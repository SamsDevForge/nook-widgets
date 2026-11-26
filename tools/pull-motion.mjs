import fs from 'node:fs';
import path from 'node:path';
import {execFileSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const env={...process.env,ANDROID_USER_HOME:path.join(root,'.tooling/android-user')};
const adb=path.join(root,'.tooling/platform-tools/adb.exe');
for(const name of ['animated-widget.png','slideshow-widget.png','video-widget.png','video-player.png','video-floating.png','sample-motion.mp4']){
  const folder=path.join(root,name.endsWith('.mp4')?'dist/previews':'dist/screenshots');fs.mkdirSync(folder,{recursive:true});
  const bytes=execFileSync(adb,['-s','emulator-5556','exec-out','run-as','com.nook.widgets','cat','files/screenshots/'+name],{env,maxBuffer:20*1024*1024});
  if(name.endsWith('.png')&&bytes.subarray(1,4).toString()!=='PNG')throw new Error('Invalid native capture: '+name);
  fs.writeFileSync(path.join(folder,name),bytes);console.log('Saved '+name);
}
