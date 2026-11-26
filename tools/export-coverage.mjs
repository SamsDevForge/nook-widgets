import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const catalog=JSON.parse(fs.readFileSync(path.join(root,'dist/stock-catalog.json'),'utf8'));
const lines=['# Nook 0.10.0 stock-family coverage','',
  'These are independently drawn Android counterparts and useful equivalents. This inventory describes the current Nook catalog; it is not an audited list of every stock widget from every platform release. Original availability varies by OS, region and installed apps.','',
  'Android/Nook families use supported local data or native actions. Connected RSS and market families need their selected sources. Manual logs use your entries. Private vendor services use manual cards or app shortcuts, with a source label on the widget. They do not expose private vendor accounts.','',
  'Liquid Glass is available throughout the catalog in clear or tinted mode. Its transparency slider is 0–100%. Blur/refraction uses an individually selected static wallpaper image; the launcher wallpaper is not sampled live.',''];
for(const [brand,families] of Object.entries(catalog)){
  const count=families.reduce((n,f)=>n+f.layouts.length,0);
  lines.push(`## ${brand}`,'',`${families.length} named family entries · ${count} layout/size presets. Small, Medium and Large are preferred footprints; your Android launcher determines placement. Counts include size choices and repeated categories across designs. Each row below corresponds to an addable catalog family.`,'','| Family | Source | Layout/size choices |','| --- | --- | ---: |');
  for(const f of families)lines.push(`| ${f.family.replaceAll('|','/')} | ${f.source} | ${f.layouts.length} |`);
  lines.push('');
}
lines.push('The complete named variants and source details are recorded in [stock-catalog.json](stock-catalog.json). The catalog is exported by the native stock integration suite.','');
fs.writeFileSync(path.join(root,'dist/stock-library-coverage.md'),lines.join('\n'));
console.log('Exported readable stock-family coverage.');
