import { spawn } from 'node:child_process';
import { createInterface } from 'node:readline';
import { mkdirSync } from 'node:fs';
import { join } from 'node:path';
import { check, instructions, replySchema } from './contracts.mjs';

// Only OS/runtime variables are inherited. No workspace tokens, API keys or app connector pipes.
export function childEnvironment(root) {
  const env={};
  for(const name of ['SystemRoot','WINDIR','COMSPEC','PATH','PATHEXT','USERPROFILE','LOCALAPPDATA','APPDATA','TEMP','TMP']) {
    const found=Object.keys(process.env).find(k=>k.toUpperCase()===name.toUpperCase());
    if(found) env[name]=process.env[found];
  }
  env.CODEX_HOME=join(root,'.state','codex-home');
  return env;
}
export class CodexClient {
  constructor(root,executable='codex.exe') {
    this.root=root;this.executable=executable;this.next=1;this.pending=new Map();this.listeners=new Set();
  }
  async start() {
    if(this.ready) return this.ready;
    this.ready=this.initialize();
    try {await this.ready;} catch(e) {this.close();throw e;}
  }
  async initialize() {
    const cwd=join(this.root,'.state','coach-workspace');mkdirSync(cwd,{recursive:true});
    mkdirSync(join(this.root,'.state','codex-home'),{recursive:true});
    const args=['app-server','--stdio','-c','sandbox_mode="read-only"','-c','approval_policy="never"',
      '-c','web_search="disabled"','-c','features.shell_tool=false','-c','features.unified_exec=false',
      '-c','features.apply_patch_freeform=false','-c','features.apps=false','-c','features.plugins=false',
      '-c','features.multi_agent=false','-c','features.js_repl=false','-c','features.view_image=false',
      '-c','features.skip_host_skill_discovery=true','-c','mcp_servers={}'];
    const child=spawn(this.executable,args,{cwd,env:childEnvironment(this.root),windowsHide:true,shell:false,stdio:['pipe','pipe','pipe']});
    this.child=child;this.cwd=cwd;
    // Drain diagnostics without logging account data or prompts.
    child.stderr.on('data',()=>{});
    child.on('error',()=>this.fail(new Error('Could not start the installed Codex CLI. Check its executable path.')));
    child.on('exit',()=>{this.fail(new Error('Codex companion process stopped.'));this.ready=null;});
    const lines=createInterface({input:child.stdout});
    lines.on('line',line=>{
      if(line.length>1_000_000) {this.close();return;}
      let event;try {event=JSON.parse(line);} catch {return;}
      if(event.id!==undefined && event.method) {
        // No computer actions, credential refresh from callers, or approval grants.
        this.send({id:event.id,error:{code:-32601,message:'Fitness coach does not support tool execution or approval requests'}});return;
      }
      if(event.id!==undefined) {
        const p=this.pending.get(event.id);if(!p) return;
        this.pending.delete(event.id);clearTimeout(p.timer);
        event.error?p.reject(new Error('Codex could not complete the request. Check companion login and account access.')):p.resolve(event.result);
      } else for(const listener of this.listeners) listener(event);
    });
    await this.call('initialize',{clientInfo:{name:'adhils_fitness',title:'ADhils Fitness',version:'0.1.0'},capabilities:{experimentalApi:true}});
    this.send({method:'initialized',params:{}});
  }
  send(value) {check(this.child && !this.child.killed,'Codex is not running',503);this.child.stdin.write(JSON.stringify(value)+'\n');}
  call(method,params,timeout=15000) {
    return new Promise((resolve,reject)=>{
      const id=this.next++;
      const timer=setTimeout(()=>{this.pending.delete(id);reject(new Error(`Codex ${method} timed out`));},timeout);
      this.pending.set(id,{resolve,reject,timer});
      try {this.send({id,method,params});} catch(e) {clearTimeout(timer);this.pending.delete(id);reject(e);}
    });
  }
  fail(error) {for(const p of this.pending.values()) {clearTimeout(p.timer);p.reject(error);}this.pending.clear();}
  close() {this.fail(new Error('Codex connection closed'));this.child?.kill();this.child=null;this.ready=null;}
  async account() {await this.start();const result=await this.call('account/read',{refreshToken:false});return result.account?.type==='chatgpt';}
  async login() {
    await this.start();return this.call('account/login/start',{type:'chatgpt'});
  }
  async coach(context) {
    check(await this.account(),'Sign in with ChatGPT using the PC companion login command first.',503);
    const started=await this.call('thread/start',{cwd:this.cwd,sandbox:'readOnly',approvalPolicy:'never',ephemeral:true,
      baseInstructions:instructions,developerInstructions:'Return only the requested fitness JSON. Never invoke a tool.',
      environments:[],dynamicTools:[],selectedCapabilityRoots:[]});
    const threadId=started.thread.id;
    return new Promise((resolve,reject)=>{
      let result='';let turnId=null;let settled=false;
      const finish=(err,value)=>{if(settled)return;settled=true;clearTimeout(timer);this.listeners.delete(listener);err?reject(err):resolve(value);};
      const listener=event=>{
        if(event.params?.threadId!==threadId) return;
        if(event.method==='item/completed' && event.params.item?.type==='agentMessage') result=event.params.item.text;
        if(event.method==='turn/completed') {
          if(event.params.turn.status!=='completed') finish(new Error('ChatGPT could not complete the answer. No workout changes were applied.'));
          else try {finish(null,{value:JSON.parse(result),actualMicros:0});} catch {finish(new Error('ChatGPT returned an unreadable answer.'));}
        }
      };
      const timer=setTimeout(()=>{
        if(turnId) this.call('turn/interrupt',{threadId,turnId}).catch(()=>{});
        finish(new Error('ChatGPT timed out. Try again when your PC and account are available.'));
      },105000);
      this.listeners.add(listener);
      this.call('turn/start',{threadId,input:[{type:'text',text:JSON.stringify(context),text_elements:[]}],
        environments:[],sandboxPolicy:{type:'readOnly',networkAccess:false},approvalPolicy:'never',effort:'low',outputSchema:replySchema})
        .then(x=>{turnId=x.turn.id;}).catch(e=>finish(e));
    });
  }
}
