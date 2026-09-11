import { createServer } from 'node:https';
import { check,digest,validateRequest,validateReply } from './contracts.mjs';
import { MONTHLY_LIMIT } from './journal.mjs';

export function privateAddress(ip) {
  if(typeof ip!=='string') return false;
  const value=ip.replace(/^::ffff:/,'');
  if(value==='::1') return true;
  const parts=value.split('.').map(Number);
  return parts.length===4 && parts.every(x=>Number.isInteger(x) && x>=0 && x<=255) &&
    (parts[0]===127 || parts[0]===10 || (parts[0]===192 && parts[1]===168) || (parts[0]===172 && parts[1]>=16 && parts[1]<=31));
}
async function readJson(req) {
  check(req.headers['content-type']?.split(';')[0]==='application/json','Send application/json',415);
  let size=0;const chunks=[];
  for await(const chunk of req) {size+=chunk.length;check(size<=128*1024,'Request too large',413);chunks.push(chunk);}
  try {return JSON.parse(Buffer.concat(chunks).toString('utf8'));} catch {throw Object.assign(new Error('Invalid JSON'),{status:400});}
}
export class CoachService {
  constructor(journal,prepare) {this.journal=journal;this.prepare=prepare;this.active=false;}
  async ask(device,request) {
    const context=validateRequest(request);
    // Keep IDs unambiguous even when callers supply punctuation.
    const key=JSON.stringify([device.id,request.profileId,request.requestId]);
    const hash=digest(JSON.stringify(request));
    check(!this.active,'Another coach request is running. Try again shortly.',429);
    this.active=true;
    let reserved=false;
    try {
      // Configuration errors are detected before reserving or contacting a paid provider.
      const send=await this.prepare(request.provider);
      const cached=this.journal.reserve(key,hash,request.provider);
      if(cached) return cached;
      reserved=true;
      const result=await send(context);
      const reply={...validateReply(result.value,context),provider:request.provider,
        usageUsd:result.actualMicros==null?null:result.actualMicros/1_000_000};
      this.journal.complete(key,reply,result.actualMicros);return reply;
    } catch(e) {if(reserved)this.journal.fail(key);throw e;}
    finally {this.active=false;}
  }
}
export function fitnessServer({key,cert,journal,service}) {
  const rate=new Map();
  const server=createServer({key,cert,minVersion:'TLSv1.2'},async(req,res)=>{
    const json=(status,body)=>{
      res.writeHead(status,{'Content-Type':'application/json','Cache-Control':'no-store','X-Content-Type-Options':'nosniff'});res.end(JSON.stringify(body));
    };
    try {
      check(privateAddress(req.socket.remoteAddress),'Private network required',403);
      check(!req.headers.origin,'Browser requests are not supported',403);
      const addr=req.socket.remoteAddress;const now=Date.now();
      const bucket=rate.get(addr)??{at:now,n:0};if(now-bucket.at>60000){bucket.at=now;bucket.n=0;}bucket.n++;rate.set(addr,bucket);
      if(rate.size>1000) for(const [ip,b] of rate)if(now-b.at>60000)rate.delete(ip);
      check(bucket.n<=60,'Too many requests. Wait a minute.',429);
      if(req.url==='/v1/pair' && req.method==='POST') {
        const body=await readJson(req);json(200,journal.pair(body.secret,body.deviceName));return;
      }
      const device=journal.authenticate(req.headers.authorization?.replace(/^Bearer /,''));
      check(device,'Pair this device with your PC first',401);
      if(req.url==='/v1/status' && req.method==='GET') {
        json(200,{version:1,month:journal.month(),spentUsd:journal.spent()/1_000_000,limitUsd:MONTHLY_LIMIT/1_000_000,
          usageIncludesReservations:true});return;
      }
      if(req.url==='/v1/coach' && req.method==='POST') {json(200,await service.ask(device,await readJson(req)));return;}
      json(404,{error:'Unknown endpoint'});
    } catch(e) {if(!res.destroyed)json(e.status??500,{error:e.status?e.message:'Companion could not complete the request. Check the PC connection and configuration.'});}
  });
  server.requestTimeout=15000;server.headersTimeout=10000;server.keepAliveTimeout=5000;
  return server;
}
