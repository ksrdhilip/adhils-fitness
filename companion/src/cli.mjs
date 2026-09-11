import { readFileSync,writeFileSync,existsSync,mkdirSync } from 'node:fs';
import { join,resolve,dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { networkInterfaces } from 'node:os';
import { X509Certificate } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import { Journal } from './journal.mjs';
import { check } from './contracts.mjs';
import { readCredential,paidProvider } from './providers.mjs';
import { CodexClient } from './codex.mjs';
import { CoachService,fitnessServer,privateAddress } from './server.mjs';

const root=resolve(dirname(fileURLToPath(import.meta.url)),'..');
const state=join(root,'.state');mkdirSync(state,{recursive:true});
const args=process.argv.slice(2),command=args[0]??'help';
const option=name=>{const i=args.indexOf(name);return i>=0?args[i+1]:undefined;};
const escape=s=>s.replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;').replaceAll('"','&quot;');
const codex=new CodexClient(root,option('--codex')??'codex.exe');

async function main() {
  if(command==='setup') {
    check(process.platform==='win32','This companion setup currently supports Windows only');
    const openssl=option('--openssl')??'C:\\Program Files\\Git\\usr\\bin\\openssl.exe';
    check(existsSync(openssl),'Use an installed official OpenSSL executable, supplied with Git for Windows, via --openssl.');
    execFileSync('powershell.exe',['-NoProfile','-NonInteractive','-File',join(root,'scripts','protect-state.ps1')],{windowsHide:true,stdio:'pipe'});
    // Files stay inside this project. No installers, remote scripts, firewall changes or startup services.
    const key=join(state,'server-key.pem'),cert=join(state,'server-cert.pem');
    if(!existsSync(key) && !existsSync(cert)) {
      execFileSync(openssl,['req','-x509','-newkey','rsa:2048','-sha256','-nodes','-keyout',key,'-out',cert,
        '-days','365','-subj','/CN=ADhils Fitness Companion'],{windowsHide:true,stdio:['ignore','ignore','pipe']});
    }
    check(existsSync(key) && existsSync(cert),'TLS identity is incomplete. Restore the matching key and certificate before continuing.');
    console.log('TLS identity is ready. Sign in with: node src/cli.mjs login');return;
  }
  if(command==='login') {
    try {
      if(await codex.account()) {console.log('ChatGPT is already connected for this companion.');return;}
      const login=await codex.login();
      console.log('Open the official sign-in URL in your browser and complete login:');
      console.log(login.authUrl);
      console.log('Waiting for login (up to 5 minutes)…');
      const deadline=Date.now()+300000;
      while(Date.now()<deadline) {
        await new Promise(r=>setTimeout(r,3000));
        if(await codex.account()) {console.log('ChatGPT connected. Your subscription limits apply.');return;}
      }
      throw new Error('Login timed out. Run the login command again when ready.');
    } finally {codex.close();}
  }
  if(command==='account') {try {console.log(await codex.account()?'ChatGPT connected':'ChatGPT login required');}finally{codex.close();}return;}
  const journal=new Journal(join(state,'companion.sqlite'));
  if(command==='devices') {console.table(journal.devices());journal.close();return;}
  if(command==='revoke') {check(args[1],'Supply a device ID from the devices command');journal.revoke(args[1]);journal.close();console.log('Device access revoked.');return;}
  if(command==='status') {console.log(JSON.stringify({month:journal.month(),spentAndReservedUsd:journal.spent()/1e6,limitUsd:5}));journal.close();return;}
  if(command==='serve') {
    const host=option('--host')??'127.0.0.1',port=Number(option('--port')??'8787');
    const local=Object.values(networkInterfaces()).flat().filter(Boolean).map(x=>x.address);
    check(privateAddress(host) && local.includes(host),'Choose a private IP address assigned to this PC using --host.');
    check(Number.isInteger(port)&&port>=1024&&port<=65535,'Choose a port from 1024 to 65535');
    check(existsSync(join(state,'server-key.pem')),'Run setup first.');
    const key=readFileSync(join(state,'server-key.pem')),cert=readFileSync(join(state,'server-cert.pem'));
    const certificate=new X509Certificate(cert);
    check(Date.parse(certificate.validTo)>Date.now(),'TLS certificate expired. Create a new identity and pair devices again.');
    const prepare=async provider=>{
      if(provider==='codex') {check(await codex.account(),'Run the companion login command on your PC first.',503);return context=>codex.coach(context);}
      const credential=await readCredential(root,provider);return context=>paidProvider(provider,context,credential);
    };
    const service=new CoachService(journal,prepare);
    const server=fitnessServer({key,cert,journal,service});
    await new Promise((res,rej)=>{server.once('error',rej);server.listen(port,host,res);});
    const invitation={version:1,url:`https://${host}:${port}`,fingerprint:certificate.fingerprint256.replaceAll(':','').toLowerCase(),secret:journal.invite().secret};
    const {default:QRCode}=await import('qrcode');
    const qr=await QRCode.toDataURL(JSON.stringify(invitation),{width:480,margin:3,errorCorrectionLevel:'M'});
    const html=`<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width"><title>Pair ADhils Fitness</title><style>body{background:#101614;color:#f2f6f3;font:18px system-ui;max-width:680px;margin:40px auto;padding:20px}h1{color:#b4f5d5}img{max-width:100%;border-radius:18px}textarea{width:100%;height:150px;background:#1b2420;color:inherit}p{line-height:1.5}</style><h1>Connect your training companion</h1><p>On your S22 or Tab A9+, open Settings → Scan PC pairing QR. This invitation works once and expires in five minutes. Restart the companion to issue a new invitation.</p><img alt="One-use pairing QR" src="${qr}"><p>Or copy this invitation into Settings → Paste invitation.</p><textarea readonly>${escape(JSON.stringify(invitation))}</textarea><p>Keep this page private. Both devices must be on your trusted network.</p></html>`;
    writeFileSync(join(state,'pairing.html'),html);
    console.log(`Companion listening on ${invitation.url}\nOpen ${join(state,'pairing.html')} to pair. Press Ctrl+C to stop.`);
    if(host==='127.0.0.1') console.log('Loopback mode: for a phone, restart with --host followed by your PC private Wi-Fi IPv4 address.');
    const stop=()=>{server.close();codex.close();journal.close();process.exit(0);};
    process.on('SIGINT',stop);process.on('SIGTERM',stop);return;
  }
  journal.close();
  console.log('ADhils companion: setup | login | account | serve [--host PRIVATE_IP] [--port 8787] | status | devices | revoke DEVICE_ID');
}
main().catch(e=>{codex.close();console.error(e.status?e.message:(e.message??'Companion failed'));process.exitCode=1;});
