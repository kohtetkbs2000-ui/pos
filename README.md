# ဆိုင် POS — Android APK (GitHub Actions)

ဒီ repo ကို GitHub ပေါ်တင်ရုံနဲ့ APK ကို အလိုအလျောက် build လုပ်ပေးပါမယ်။ ကွန်ပျူတာမှာ Android Studio မလိုပါ။

## APK ထုတ်နည်း
1. GitHub မှာ repository အသစ်ဖွင့်ပါ (Private လည်းရ)။
2. ဒီ zip ကို ဖြေပြီး **ဖိုင်အားလုံးကို** upload လုပ်ပါ။ `.github` folder ပါရမယ် (ဖုန်းထဲက "Upload files" က hidden folder ကို မယူရင် ကွန်ပျူတာကနေ တင်ပါ သို့မဟုတ် `git push` သုံးပါ)။
3. repo ရဲ့ **Actions** tab → **Build Android APK** → **Run workflow** နှိပ်ပါ (`main` branch ကို push လုပ်တိုင်းလည်း အလိုအလျောက် run ပါမယ်)။
4. ၅–၁၀ မိနစ်ခန့်နေရင် run ပြီးပါမယ်။ run ကိုဖွင့် → အောက်ဆုံး **Artifacts → shoppos-apk** ကို download လုပ်ပါ။
5. zip ဖြေပြီး `shoppos.apk` ကို ဖုန်းမှာ install လုပ်ပါ ("မသိသော app တင်ခွင့်" ဖွင့်ပေးရပါမယ်)။

### Release link ရချင်ရင်
```
git tag v1.0
git push origin v1.0
```
Repo ရဲ့ **Releases** မှာ `shoppos.apk` ပေါ်လာပါမယ်။

## App ပြင်ရင် (ဥပမာ အော်ဒါအခြေအနေ၊ ခြင်းတောင်း)
`www/index.html` ကို အသစ်နဲ့ အစားထိုးပြီး push လုပ်ပါ။ APK အသစ် ပြန် build ပါမယ်။

## ပါဝင်တာတွေ
- `www/index.html` — POS app (အခု ပြင်ပြီးသား version)
- `native/NativePrintPlugin.java` — ဘောက်ချာ / Label ကို Android print dialog နဲ့ ပရင့်ထုတ် (A4 / 80mm / Label / PDF)
- `native/BtPrinterPlugin.java` — Bluetooth (SPP) thermal printer တိုက်ရိုက်ပို့
- `@capacitor/filesystem`, `@capacitor/share` — CSV / Backup / ပုံ ဖိုင်များ share
- `scripts/patch-android.py` — Bluetooth permission နဲ့ plugin တွေကို build ထဲ ထည့်ပေး

## မှတ်ချက်
- Bluetooth printer ကို Android **Settings > Bluetooth** မှာ အရင် pair လုပ်ပါ၊ ပြီးမှ app ထဲ Printer ချိတ်ဆက်ရာမှာ ရွေးပါ။
- ဒါက **debug APK** ဖြစ်ပြီး install သုံးလို့ရပါတယ်။ Google Play တင်မယ်ဆိုရင် signed AAB လိုပါတယ်။
- Data တွေက ဖုန်းထဲမှာ သိမ်းပါတယ် (Supabase ချိတ်ထားရင် cloud နဲ့လည်း sync)။ App ကို uninstall လုပ်ရင် local data ပျောက်မှာမို့ Backup ဖိုင် ထုတ်ထားပါ။
- Icon က Capacitor မူလ icon ဖြစ်ပါတယ်။ ပြောင်းချင်ရင် `@capacitor/assets` သုံးနိုင်ပါတယ်။
