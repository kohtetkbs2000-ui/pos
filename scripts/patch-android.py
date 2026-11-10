#!/usr/bin/env python3
"""Run after `npx cap add android`: copies the custom Java plugins and patches the manifest."""
import pathlib, re, shutil, sys

PKG_PATH = 'com/shoppos/app'
root = pathlib.Path('android')
if not root.exists():
    sys.exit('android/ folder not found - run `npx cap add android` first')

# 1) Java sources (MainActivity.java overwrites the generated one)
dst = root / 'app/src/main/java' / PKG_PATH
dst.mkdir(parents=True, exist_ok=True)
for f in pathlib.Path('native').glob('*.java'):
    shutil.copy(f, dst / f.name)
    print('copied', f.name)

# 2) AndroidManifest.xml
mf = root / 'app/src/main/AndroidManifest.xml'
s = mf.read_text(encoding='utf-8')

extra = '''
    <!-- Bluetooth receipt printers (classic SPP) -->
    <uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-feature android:name="android.hardware.bluetooth" android:required="false" />
'''
if 'BLUETOOTH_CONNECT' not in s:
    s = s.replace('</manifest>', extra + '</manifest>')

# keyboard should resize the page (app is built for this)
if 'windowSoftInputMode' not in s:
    s = re.sub(r'<activity\b', '<activity android:windowSoftInputMode="adjustResize"', s, count=1)

mf.write_text(s, encoding='utf-8')
print('manifest patched')
