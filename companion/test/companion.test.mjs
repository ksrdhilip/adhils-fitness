import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtempSync,rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { Journal,PAID_RESERVATION } from '../src/journal.mjs';
import { CoachService,privateAddress } from '../src/server.mjs';
import { validateRequest,validateReply } from '../src/contracts.mjs';
import { paidProvider,usageMicros } from '../src/providers.mjs';
import { childEnvironment } from '../src/codex.mjs';

const request=(extra={})=>({requestId:'r1',profileId:'p1',provider:'openai',message:'Make my workout shorter',
  profile:{name:'First',goal:'Build muscle',restrictions:'No jumping'},recentSessions:[],session:{id:'s1',revision:3,finishedAt:null,
    plan:[{exerciseId:'rdl',sets:3}],results:[{exerciseId:'rdl',setIndex:0}]},...extra});
const reply={explanation:'Reduce the remaining volume today.',proposal:{sessionId:'s1',revision:3,reason:'Less time today',changes:[{exerciseId:'rdl',sets:2}]}};
function fixture(t) {
  const dir=mkdtempSync(join(tmpdir(),'adhils-test-'));const path=join(dir,'test.sqlite');
  const journal=new Journal(path);t.after(()=>{journal.close();rmSync(dir,{recursive:true,force:true});});return {journal,path};
}
test('shared budget persists across reopen and limits different devices/profiles',t=>{
  const {journal,path}=fixture(t);
  for(let i=0;i<50;i++)journal.reserve(`device-${i%2}/profile-${i}/request`,`${i}`,i%2?'claude':'openai');
  assert.equal(journal.spent(),5_000_000);
  const second=new Journal(path);
  try {assert.throws(()=>second.reserve('new','h','openai'),/budget/);assert.equal(second.reserve('free','x','codex'),null);}finally{second.close();}
});
test('same request is cached, changed content conflicts, uncertain request cannot resend',t=>{
  const {journal}=fixture(t);
  journal.reserve('one','hash','openai');journal.complete('one',reply,1000);
  assert.deepEqual(journal.reserve('one','hash','openai'),reply);assert.equal(journal.spent(),1000);
  assert.throws(()=>journal.reserve('one','other','openai'),/reused/);
  journal.reserve('two','hash2','claude');journal.fail('two');
  assert.throws(()=>journal.reserve('two','hash2','claude'),/uncertain/);assert.equal(journal.spent(),PAID_RESERVATION+1000);
});
test('month boundary starts new allowance while prior reservations remain',t=>{
  const {journal}=fixture(t);let now=Date.UTC(2026,8,30,23,59);journal.clock=()=>now;
  journal.reserve('sept','s','openai');now=Date.UTC(2026,9,1);
  assert.equal(journal.spent(),0);assert.equal(journal.spent('2026-09'),PAID_RESERVATION);
});
test('pair invitation expires, cannot be reused, and device tokens can be revoked',t=>{
  const {journal}=fixture(t);let now=1000;journal.clock=()=>now;
  const invitation=journal.invite();const device=journal.pair(invitation.secret,'Galaxy S22');
  assert.equal(journal.authenticate(device.token).name,'Galaxy S22');
  assert.throws(()=>journal.pair(invitation.secret,'attacker'),/already used/);
  journal.revoke(device.deviceId);assert.equal(journal.authenticate(device.token),null);
  const next=journal.invite();now+=300001;assert.throws(()=>journal.pair(next.secret,'tablet'),/expired/);
});
test('coach rejects stale proposals, removed logged sets, increases and invented exercises',()=>{
  const r=request();assert.deepEqual(validateReply(reply,r),reply);
  for(const patch of [{revision:2},{changes:[{exerciseId:'rdl',sets:0}]},{changes:[{exerciseId:'rdl',sets:4}]},{changes:[{exerciseId:'invented',sets:1}]}])
    assert.throws(()=>validateReply({...reply,proposal:{...reply.proposal,...patch}},r));
  assert.throws(()=>validateReply(reply,{...r,session:null}));
});
test('provider receives selected profile only; identical request ids across profiles do not leak replies',async t=>{
  const {journal}=fixture(t);const seen=[];
  const service=new CoachService(journal,async()=>async c=>{seen.push(c);return {value:{explanation:c.profile.name,proposal:null},actualMicros:100};});
  const firstRequest=request({conversation:[{role:'coach',text:'A message belonging only to First'}]});
  const first=await service.ask({id:'device'},firstRequest);
  const second=await service.ask({id:'device'},request({profileId:'p2',profile:{name:'Second'},session:null}));
  assert.equal(first.explanation,'First');assert.equal(second.explanation,'Second');assert.equal(seen.length,2);
  assert.equal(seen[0].conversation[0].text,'A message belonging only to First');assert.deepEqual(seen[1].conversation,[]);
  await service.ask({id:'device'},firstRequest);assert.equal(seen.length,2);
});
test('concurrent submissions are bounded and invalid AI responses retain reservations',async t=>{
  const {journal}=fixture(t);let release;const wait=new Promise(r=>release=r);
  const service=new CoachService(journal,async()=>async()=>{await wait;return {value:{},actualMicros:10};});
  const pending=service.ask({id:'d'},request());
  await assert.rejects(service.ask({id:'d'},request({requestId:'r2'})),/running/);
  release();await assert.rejects(pending,/unreadable/);assert.equal(journal.spent(),PAID_RESERVATION);
});
test('missing provider configuration spends nothing',async t=>{
  const {journal}=fixture(t);const service=new CoachService(journal,async()=>{throw new Error('Configure provider');});
  await assert.rejects(service.ask({id:'d'},request()),/Configure/);assert.equal(journal.spent(),0);
});
test('paid provider uses official endpoint, bounded output, structured JSON, no tools or retries',async()=>{
  let calls=0;
  const result=await paidProvider('openai',validateRequest(request()),'example-test-key',async(url,options)=>{
    calls++;assert.equal(url,'https://api.openai.com/v1/responses');assert.equal(options.redirect,'error');
    const body=JSON.parse(options.body);assert.equal(body.max_output_tokens,4096);assert.equal(body.store,false);assert.equal(body.tools,undefined);
    assert.equal(body.text.format.type,'json_schema');
    return new Response(JSON.stringify({status:'completed',output:[{content:[{type:'output_text',text:JSON.stringify(reply)}]}],usage:{input_tokens:1000,output_tokens:100}}));
  });
  assert.equal(calls,1);assert.equal(result.actualMicros,450);
  assert.equal(usageMicros('claude',{input_tokens:1000,output_tokens:100}),1500);
  assert.equal(usageMicros('claude',{input_tokens:1000,output_tokens:100,cache_creation_input_tokens:500,cache_read_input_tokens:1000}),2600);
  assert.equal(usageMicros('openai',{}),null);
});
test('oversize input, public addresses and inherited workspace secrets are excluded',()=>{
  assert.throws(()=>validateRequest(request({message:'x'.repeat(4001)})));
  assert.equal(privateAddress('8.8.8.8'),false);assert.equal(privateAddress('192.168.1.10'),true);
  assert.equal(privateAddress('::ffff:10.0.0.8'),true);assert.equal(privateAddress('192.168.1.500'),false);
  const env=childEnvironment('C:/example');assert.equal(env.OPENAI_API_KEY,undefined);assert.equal(env.CODEX_APP_TOOLS_PIPE_PATH,undefined);
});
