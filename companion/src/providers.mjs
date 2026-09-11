import { execFile } from 'node:child_process';
import { promisify } from 'node:util';
import { existsSync } from 'node:fs';
import { join } from 'node:path';
import { instructions, replySchema, check } from './contracts.mjs';
const run=promisify(execFile);
const prices={openai:{input:.25,output:2},claude:{input:1,output:5}}; // USD per million; reviewed 2026-09-10

export function usageMicros(provider,usage) {
  const input=usage?.input_tokens,output=usage?.output_tokens;
  if(!Number.isSafeInteger(input) || input<0 || !Number.isSafeInteger(output) || output<0) return null;
  const p=prices[provider];
  const created=usage.cache_creation_input_tokens??0,read=usage.cache_read_input_tokens??0;
  if(!Number.isSafeInteger(created)||created<0||!Number.isSafeInteger(read)||read<0)return null;
  // Claude reports cache tokens separately. Use the longer-TTL write rate
  // conservatively if caching is introduced by the provider in the future.
  return Math.ceil(input*p.input+output*p.output+(provider==='claude'?(created*2+read*.1)*p.input:0));
}
export async function readCredential(root,name) {
  check(['openai','claude'].includes(name),'Unknown credential');
  check(existsSync(join(root,'.state',`${name}.credential`)),`Configure the ${name} API key on the PC first.`,503);
  const {stdout}=await run('powershell.exe',['-NoProfile','-NonInteractive','-File',join(root,'scripts','credential.ps1'),'-Action','Read','-Name',name],
    {windowsHide:true,timeout:10000,maxBuffer:8192});
  check(stdout.trim().length>10,'Could not unlock API key. Run companion as the Windows user who saved it.',503);
  return stdout.trim();
}
async function boundedJson(response) {
  let size=0;const chunks=[];
  for await(const chunk of response.body) {size+=chunk.length;check(size<=262144,'AI response too large',502);chunks.push(chunk);}
  return JSON.parse(Buffer.concat(chunks).toString('utf8'));
}
export async function paidProvider(provider,context,key,fetcher=fetch) {
  check(['openai','claude'].includes(provider),'Unknown paid provider');
  const payload=JSON.stringify(context);
  check(Buffer.byteLength(payload)<=24000,'AI context too large');
  const openai=provider==='openai';
  const url=openai?'https://api.openai.com/v1/responses':'https://api.anthropic.com/v1/messages';
  const body=openai?{
    model:'gpt-5-mini',instructions,input:payload,max_output_tokens:4096,reasoning:{effort:'low'},store:false,
    text:{format:{type:'json_schema',name:'fitness_coach',strict:true,schema:replySchema}}
  }:{model:'claude-haiku-4-5-20251001',max_tokens:4096,system:instructions,messages:[{role:'user',content:payload}],
    output_config:{format:{type:'json_schema',schema:replySchema}}};
  const response=await fetcher(url,{method:'POST',redirect:'error',signal:AbortSignal.timeout(105000),
    headers:openai?{'Content-Type':'application/json','Authorization':`Bearer ${key}`}:
      {'Content-Type':'application/json','x-api-key':key,'anthropic-version':'2023-06-01'},body:JSON.stringify(body)});
  // Do not echo provider bodies: they can contain request content or sensitive metadata.
  check(response.ok,`AI provider returned HTTP ${response.status}. Check the PC account configuration. This request will not retry automatically.`,502);
  const data=await boundedJson(response);
  const text=openai?data.output?.flatMap(x=>x.content??[]).filter(x=>x.type==='output_text').map(x=>x.text).join(''):
    data.content?.filter(x=>x.type==='text').map(x=>x.text).join('');
  check(openai?data.status==='completed':data.stop_reason==='end_turn','AI response was incomplete. No changes were applied.',502);
  check(typeof text==='string' && text.length>0,'The AI returned no answer',502);
  return {value:JSON.parse(text),actualMicros:usageMicros(provider,data.usage)};
}
