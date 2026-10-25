// Copies the offline copies of Supabase JS + fonts from node_modules into www/.
// Runs automatically in GitHub Actions (npm run vendor).
const fs = require('fs'), path = require('path');
const root = path.join(__dirname, '..'), nm = path.join(root, 'node_modules'), www = path.join(root, 'www');
fs.mkdirSync(path.join(www, 'vendor'), { recursive: true });
fs.mkdirSync(path.join(www, 'fonts'), { recursive: true });

// 1) Supabase UMD bundle
const sb = path.join(nm, '@supabase/supabase-js/dist/umd/supabase.js');
const sbMin = path.join(nm, '@supabase/supabase-js/dist/umd/supabase.min.js');
const src = fs.existsSync(sbMin) ? sbMin : sb;
if (!fs.existsSync(src)) { console.error('supabase-js UMD bundle not found in node_modules'); process.exit(1); }
fs.copyFileSync(src, path.join(www, 'vendor/supabase.min.js'));
console.log('vendor/supabase.min.js <-', path.relative(root, src));

// 2) Fonts (Myanmar + Latin) -> www/fonts/fonts.css
let css = '';
function addFamily(pkg, family, subset, weights) {
  const dir = path.join(nm, '@fontsource', pkg, 'files');
  if (!fs.existsSync(dir)) { console.warn('font package missing:', pkg); return; }
  for (const w of weights) {
    const f = `${pkg}-${subset}-${w}-normal.woff2`;
    if (!fs.existsSync(path.join(dir, f))) { console.warn('missing font file', f); continue; }
    fs.copyFileSync(path.join(dir, f), path.join(www, 'fonts', f));
    css += `@font-face{font-family:'${family}';font-style:normal;font-display:swap;font-weight:${w};src:url('${f}') format('woff2')}\n`;
  }
}
addFamily('noto-sans-myanmar', 'Noto Sans Myanmar', 'myanmar', [400, 500, 600, 700]);
addFamily('plus-jakarta-sans', 'Plus Jakarta Sans', 'latin', [500, 600, 700, 800]);
fs.writeFileSync(path.join(www, 'fonts/fonts.css'), css || '/* fonts unavailable - system fonts are used */\n');
console.log('fonts/fonts.css written');
