import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const adb = path.join(root, '.tooling/platform-tools/adb.exe');
const output = path.join(root, 'dist/screenshots');
fs.mkdirSync(output, {recursive:true});
const env = {...process.env, ANDROID_USER_HOME:path.join(root,'.tooling/android-user')};
for (const name of ['today','gallery','configure-notes','configure-timers','glass-gallery','glass-frosted-notes','glass-smoked-notes','glass-frosted-timers','glass-smoked-timers','glass-comparison','transparency-slider','temporary-note-editor','saved-notes','universal-menu','universal-timers','design-languages','design-editor','design-fine-tune','samsung-library','replica-nothing','replica-apple','replica-macos','replica-pixel','replica-samsung','liquid-glass','music-layouts','music-widget','music-now-playing','creative-players-buds']) {
  const bytes = execFileSync(adb,['-s','emulator-5556','exec-out','run-as','com.nook.widgets','cat',`files/screenshots/${name}.png`],{env,maxBuffer:20*1024*1024});
  if(bytes.subarray(1,4).toString()!=='PNG') throw new Error(`Invalid screenshot: ${name}`);
  fs.writeFileSync(path.join(output,`${name}.png`),bytes);
  console.log(`Saved ${name}.png`);
}
fs.writeFileSync(path.join(root,'dist/stock-catalog.json'),execFileSync(adb,['-s','emulator-5556','exec-out','run-as','com.nook.widgets','cat','files/screenshots/stock-catalog.json'],{env,maxBuffer:2*1024*1024}));
