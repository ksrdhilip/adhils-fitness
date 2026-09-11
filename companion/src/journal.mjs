import { DatabaseSync } from 'node:sqlite';
import { randomBytes, randomUUID, timingSafeEqual } from 'node:crypto';
import { check, digest } from './contracts.mjs';

export const MONTHLY_LIMIT=5_000_000; // integer micro-USD
export const PAID_RESERVATION=100_000; // $0.10, above max supported request cost
export class Journal {
  constructor(path,clock=()=>Date.now()) {
    this.clock=clock;this.db=new DatabaseSync(path);
    this.db.exec(`PRAGMA journal_mode=WAL; PRAGMA busy_timeout=5000;
      CREATE TABLE IF NOT EXISTS requests (key TEXT PRIMARY KEY, hash TEXT NOT NULL, month TEXT NOT NULL, provider TEXT NOT NULL,
        amount INTEGER NOT NULL, state TEXT NOT NULL, response TEXT, created INTEGER NOT NULL);
      CREATE TABLE IF NOT EXISTS devices (id TEXT PRIMARY KEY,name TEXT NOT NULL,token_hash TEXT UNIQUE NOT NULL,created INTEGER NOT NULL,revoked INTEGER NOT NULL DEFAULT 0);
      CREATE TABLE IF NOT EXISTS invitations (hash TEXT PRIMARY KEY,expires INTEGER NOT NULL,used INTEGER NOT NULL DEFAULT 0);`);
  }
  month() {return new Date(this.clock()).toISOString().slice(0,7);}
  spent(month=this.month()) {return this.db.prepare('SELECT COALESCE(SUM(amount),0) AS total FROM requests WHERE month=?').get(month).total;}
  reserve(key,hash,provider) {
    this.db.exec('BEGIN IMMEDIATE');
    try {
      const old=this.db.prepare('SELECT * FROM requests WHERE key=?').get(key);
      if(old) {
        check(old.hash===hash,'Request ID was reused with different data',409);
        check(old.state==='done','This request is pending or its outcome is uncertain. It will not be charged or sent again automatically.',409);
        this.db.exec('COMMIT');return JSON.parse(old.response);
      }
      const amount=provider==='codex'?0:PAID_RESERVATION;
      check(this.spent()+amount<=MONTHLY_LIMIT,'The shared $5 monthly API budget is exhausted. Use ChatGPT mode or wait until next month.',402);
      this.db.prepare('INSERT INTO requests VALUES (?,?,?,?,?,?,NULL,?)').run(key,hash,this.month(),provider,amount,'pending',this.clock());
      this.db.exec('COMMIT');return null;
    } catch(e) {this.db.exec('ROLLBACK');throw e;}
  }
  complete(key,response,actualMicros) {
    const row=this.db.prepare('SELECT amount FROM requests WHERE key=? AND state=?').get(key,'pending');
    check(row,'Request not pending',409);
    // Missing/invalid usage keeps the full reservation; never silently releases a possible charge.
    const amount=Number.isSafeInteger(actualMicros) && actualMicros>=0 ? actualMicros : row.amount;
    this.db.prepare('UPDATE requests SET state=?,response=?,amount=? WHERE key=?').run('done',JSON.stringify(response),amount,key);
  }
  fail(key) {this.db.prepare("UPDATE requests SET state='uncertain' WHERE key=? AND state='pending'").run(key);}
  invite() {
    const secret=randomBytes(32).toString('base64url');const expires=this.clock()+5*60_000;
    this.db.prepare('DELETE FROM invitations').run();
    this.db.prepare('INSERT INTO invitations VALUES (?,?,0)').run(digest(secret),expires);
    return {secret,expires};
  }
  pair(secret,name) {
    check(typeof secret==='string' && secret.length<=128 && typeof name==='string' && name.length<=80,'Invalid pairing request');
    const token=randomBytes(32).toString('base64url');const id=randomUUID();
    this.db.exec('BEGIN IMMEDIATE');
    try {
      const used=this.db.prepare('UPDATE invitations SET used=1 WHERE hash=? AND used=0 AND expires>?').run(digest(secret),this.clock());
      check(used.changes===1,'Invitation expired or already used. Generate a new invitation on the PC.',401);
      this.db.prepare('INSERT INTO devices VALUES (?,?,?,?,0)').run(id,name,digest(token),this.clock());
      this.db.exec('COMMIT');return {token,deviceId:id};
    } catch(e) {this.db.exec('ROLLBACK');throw e;}
  }
  authenticate(token) {
    if(typeof token!=='string' || token.length>128) return null;
    return this.db.prepare('SELECT id,name FROM devices WHERE token_hash=? AND revoked=0').get(digest(token))??null;
  }
  devices() {return this.db.prepare('SELECT id,name,created,revoked FROM devices').all();}
  revoke(id) {this.db.prepare('UPDATE devices SET revoked=1 WHERE id=?').run(id);}
  close() {this.db.close();}
}
