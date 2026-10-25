// Creates the Android project (once) and applies our native files, name, icons and permissions.
// Usage: node scripts/android-setup.js     (run by `npm run setup` and by GitHub Actions)
const fs = require('fs'), path = require('path'), cp = require('child_process');
const root = path.join(__dirname, '..'), android = path.join(root, 'android');
const run = c => cp.execSync(c, { cwd: root, stdio: 'inherit' });

if (!fs.existsSync(android)) run('npx cap add android');

// Java sources
const javaDst = path.join(android, 'app/src/main/java/com/shoppos/app');
fs.mkdirSync(javaDst, { recursive: true });
for (const f of fs.readdirSync(path.join(root, 'android-overlay/java/com/shoppos/app')))
  fs.copyFileSync(path.join(root, 'android-overlay/java/com/shoppos/app', f), path.join(javaDst, f));

// Resources (icons, colours) - copy recursively
function copyDir(a, b) {
  fs.mkdirSync(b, { recursive: true });
  for (const e of fs.readdirSync(a, { withFileTypes: true })) {
    const s = path.join(a, e.name), d = path.join(b, e.name);
    e.isDirectory() ? copyDir(s, d) : fs.copyFileSync(s, d);
  }
}
copyDir(path.join(root, 'android-overlay/res'), path.join(android, 'app/src/main/res'));

// App name
const strings = path.join(android, 'app/src/main/res/values/strings.xml');
if (fs.existsSync(strings)) {
  let s = fs.readFileSync(strings, 'utf8');
  s = s.replace(/<string name="app_name">.*?<\/string>/, '<string name="app_name">ဆိုင် POS</string>')
       .replace(/<string name="title_activity_main">.*?<\/string>/, '<string name="title_activity_main">ဆိုင် POS</string>');
  fs.writeFileSync(strings, s);
}

// Permissions
const mf = path.join(android, 'app/src/main/AndroidManifest.xml');
let m = fs.readFileSync(mf, 'utf8');
const perms = [
  '<uses-permission android:name="android.permission.INTERNET" />',
  '<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />',
  '<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />',
  '<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />',
  '<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />',
  '<uses-feature android:name="android.hardware.bluetooth" android:required="false" />'
];
for (const p of perms) {
  const key = /android:name="([^"]+)"/.exec(p)[1];
  if (!m.includes(`android:name="${key}"`)) m = m.replace('</manifest>', `    ${p}\n</manifest>`);
}
// don't let Android back the app data up to Google (shop data / session)
m = m.replace('android:allowBackup="true"', 'android:allowBackup="false"');
fs.writeFileSync(mf, m);
console.log('android project ready');
