import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync,existsSync,mkdtempSync,rmSync } from 'node:fs';
import { join } from 'node:path';
import { tmpdir } from 'node:os';
import { request } from 'node:https';
import { X509Certificate } from 'node:crypto';
import { fileURLToPath } from 'node:url';
import { Journal } from '../src/journal.mjs';
import { fitnessServer,CoachService } from '../src/server.mjs';

const state=fileURLToPath(new URL('../.state/',import.meta.url));
test('real TLS pairing, authentication, origin rejection and revocation', {skip:!existsSync(join(state,'server-cert.pem'))}, async t=>{
  const cert=readFileSync(join(state,'server-cert.pem')),key=readFileSync(join(state,'server-key.pem'));
  const fingerprint=new X509Certificate(cert).fingerprint256;
  const dir=mkdtempSync(join(tmpdir(),'adhils-https-'));const journal=new Journal(join(dir,'test.sqlite'));
  const service=new CoachService(journal,async()=>async()=>({value:{explanation:'Test answer',proposal:null},actualMicros:0}));
  const server=fitnessServer({key,cert,journal,service});
  await new Promise(r=>server.listen(0,'127.0.0.1',r));
  t.after(async()=>{await new Promise(r=>server.close(r));journal.close();rmSync(dir,{recursive:true,force:true});});
  const call=(path,body,token,origin)=>new Promise((resolve,reject)=>{
    const req=request({hostname:'127.0.0.1',port:server.address().port,path,method:body?'POST':'GET',ca:cert,
      checkServerIdentity:(_host,peer)=>peer.fingerprint256===fingerprint?undefined:new Error('Certificate identity changed'),
      headers:{'Content-Type':'application/json',...(token?{Authorization:`Bearer ${token}`} : {}),...(origin?{Origin:origin}: {})}},res=>{
        const chunks=[];res.on('data',c=>chunks.push(c));res.on('end',()=>resolve({status:res.statusCode,body:JSON.parse(Buffer.concat(chunks))}));
      });req.on('error',reject);req.end(body?JSON.stringify(body):undefined);
  });
  assert.equal((await call('/v1/status')).status,401);
  const invite=journal.invite();
  assert.equal((await call('/v1/pair',{secret:invite.secret,deviceName:'Browser'},null,'https://example.com')).status,403);
  const pair=await call('/v1/pair',{secret:invite.secret,deviceName:'S22 test'});
  assert.equal(pair.status,200);assert.ok(pair.body.token);
  assert.equal((await call('/v1/pair',{secret:invite.secret,deviceName:'replay'})).status,401);
  const status=await call('/v1/status',null,pair.body.token);
  assert.equal(status.status,200);assert.equal(status.body.limitUsd,5);
  journal.revoke(pair.body.deviceId);
  assert.equal((await call('/v1/status',null,pair.body.token)).status,401);
});
