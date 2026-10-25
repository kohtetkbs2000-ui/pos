# ဆိုင် POS — Android app (Offline + Online)

Capacitor ဖြင့် `www/index.html` ကို Android app အဖြစ် ထုပ်ထားပါသည်။ ဒေတာများကို Supabase တွင် သိမ်းပြီး **ဖုန်းထဲတွင်လည်း ကိုယ်ပိုင် database (IndexedDB) ရှိသဖြင့် အင်တာနက်မရှိလည်း ရောင်း/ဝယ်/စာရင်းသွင်း လုပ်နိုင်ပါသည်။**

## Offline / Online ဘယ်လိုအလုပ်လုပ်သလဲ
| အခြေအနေ | လုပ်ဆောင်ချက် |
|---|---|
| အင်တာနက်မရှိ | ဖုန်းထဲက database ကိုသာ သုံးသည်။ ပြောင်းလဲမှုတိုင်းကို "outbox" တွင် စုထားသည်။ (အပေါ်ညာထောင့်တွင် `အော့ဖ်လိုင်း · N ခု စောင့်`) |
| အင်တာနက်ပြန်ရ | **အလိုအလျောက်** outbox ကို Supabase သို့ တင်ပြီး (upload) အသစ်များကို ဒေါင်းလုပ် (download) လုပ်သည် |
| App ဖွင့်ထားစဉ် | ၁၀ စက္ကန့်တစ်ခါ စစ်၍ ၄၅ စက္ကန့်ကျော်လျှင် refresh လုပ်သည်၊ Realtime ပါ သုံးသည် |
| ပထမဆုံးအကြိမ် ဖွင့်ခြင်း | ဆိုင်ဒေတာ ဒေါင်းရန် အင်တာနက် **တစ်ကြိမ်** လိုသည် |

Settings ထဲက **"ယခု ချိန်ညှိမည်"** ခလုတ်ဖြင့် လက်ဖြင့်လည်း ချိန်ညှိနိုင်သည်။

## ငွေကိုင် (Cashier) အကောင့် ခွင့်ပြုချက်
- ငွေကိုင်သည် **အကောင့်အသစ်ဖွင့်မည်** ဖြင့် စာရင်းသွင်းပြီးနောက် Owner က **ဆက်တင် › အကောင့်များ** တွင် *ခွင့်ပြုမည်* မနှိပ်မချင်း အက်ပ်ထဲ မဝင်နိုင်ပါ။
- ခွင့်ပြုချက်စောင့်နေစဉ် စာမျက်နှာမှာ **ပြန်စစ်မည်** ခလုတ်ဖြင့် လက်ဖြင့် စစ်နိုင်သည်။

## လက်ဖြင့် Refresh
အပေါ်ညာထောင့်ရှိ ↻ ခလုတ်ကို နှိပ်လျှင် ဒေတာ upload/download ချိန်ညှိပြီး စာမျက်နှာ ပြန်ဆွဲသည် (Owner / Cashier နှစ်မျိုးလုံး)။

## ငွေရှင်းခြင်း
**ပေးငွေ** ကွက်တွင် စုစုပေါင်းထက် လျော့ရိုက်လျှင် အလိုအလျောက် **စရံငွေ** အဖြစ်ပြောင်းပြီး ကျန်ငွေကို ကြွေးသို့ ပေါင်းမည် (ဖောက်သည် ရွေးထားရမည်)။ ပြည့်အောင် ပြန်ရိုက်လျှင် ငွေပေးပြီး အဖြစ် ပြန်ဖြစ်မည်။

## APK ကို GitHub မှာ ဘယ်လို build လုပ်မလဲ
1. ဤ folder တစ်ခုလုံးကို GitHub repo အသစ်သို့ push လုပ်ပါ (`main` branch)။
2. GitHub › **Actions** › *Build Android APK* သည် အလိုအလျောက် run မည် (သို့) **Run workflow** နှိပ်ပါ။
3. ပြီးလျှင် run စာမျက်နှာအောက်ခြေ **Artifacts › shop-pos-apk** မှ `app-debug.apk` ကို ဒေါင်းပါ။
4. ဖုန်းတွင် install လုပ်ပါ ("Unknown sources" ခွင့်ပြုရန် လိုနိုင်သည်)။
5. `v1.0.0` ကဲ့သို့ tag တင်လျှင် GitHub **Releases** တွင်လည်း APK ပါလာမည်။

ကိုယ့်စက်ထဲ build လုပ်လိုလျှင်: Node 20 + JDK 17 + Android SDK ထားပြီး `npm install && npm run apk`။

## ဖိုင်များ
- `www/index.html` — app တစ်ခုလုံး (offline-first Store + sync ပါ)
- `android-overlay/` — Android သီးသန့်ဖိုင်များ (ပရင့် plugin၊ icon)
- `scripts/` — vendor (Supabase lib + font ကို app ထဲ ထည့်), android-setup
- `.github/workflows/android.yml` — APK build

## သိထားရန်
- **Supabase key** သည် `index.html` ထဲ DEFAULT အဖြစ် ပါသည်။ ၎င်းမှာ publishable key ဖြစ်ပြီး SQL policy `pos_all` က anon ကို အားလုံး ခွင့်ပြုထားသဖြင့် key ရသူ ဘယ်သူမဆို ဒေတာဖတ်/ရေးနိုင်သည်။ ပိုလုံခြုံလိုလျှင် Supabase Auth + RLS policy ပြောင်းပါ။
- ဖုန်း နှစ်လုံး offline ဖြစ်စဉ် **တူညီသော ပစ္စည်းကို** ရောင်းလျှင် stock သည် နောက်ဆုံး upload လုပ်သူ၏ တန်ဖိုးဖြင့် အစားထိုးမည် (last-write-wins)။ ဘောက်ချာနံပါတ်လည်း တူနိုင်သည်။
- Bluetooth/USB ESC-POS printer တိုက်ရိုက်ချိတ်ခြင်း (Web Bluetooth) သည် Android WebView တွင် မရပါ။ **Print** ကို Android ၏ print dialog (WiFi printer / Save as PDF) ဖြင့် သုံးပါ။
- Release (Play Store) အတွက် signed APK/AAB လိုပါက keystore ကို GitHub Secrets ထဲထည့်ပြီး workflow ကို တိုးချဲ့ရမည်။
