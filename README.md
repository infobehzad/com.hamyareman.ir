# همیار من — اپ زهرا (Android)

نسخه‌ی **مستقل و قابل‌بیلد** اپ زهرا، جداشده از مونوریپوی ترکیبی `ZahraPadarPlatform`
(اپ پدر در آرشیو جداگانه‌ی `Padar-App-Final.zip` است و به همین بک‌اند Appwrite وصل می‌شود).

> اصل غیرقابل‌مذاکره‌ی این پروژه: **زهرا ۱۵ ساله است و در حال گذر از طلاق والدینش.**
> هر تغییری باید این اصول را حفظ کند: همراه نه ناظر، همراه نه درمانگر، شفافیتِ
> هوش‌مصنوعی‌بودنِ چت‌بات، عدم انزوا از آدم‌های واقعی، و هرگز Sync نشدنِ داده‌ی
> خصوصی (چرخه/ژورنال/چت خام) به سرور.

اسناد مرجع (حذف نشده‌اند، برای عمق بیشتر بخوانید):
- [`FIX_PROMPT.md`](FIX_PROMPT.md) — پرامپت آماده برای یک ایجنت کدنویسی جهت رفع باگ و تکمیل این اپ.
- [`AUDIT.md`](AUDIT.md) — تاریخچه‌ی کامل ماژول‌به‌ماژول (مشکل ← چرا ← راه‌حل) برای کل پلتفرم (هر دو اپ).
- [`APPWRITE.md`](APPWRITE.md) — چک‌لیست کامل تنظیمات بک‌اند Appwrite (جدول/باکت/تابع).

---

## ۱. وضعیت فعلی به‌طور خلاصه

| مورد | وضعیت |
|---|---|
| بیلد واقعی گرفته شده؟ | **خیر در این محیط** — Gradle Wrapper اضافه شده و تست‌های بک‌اند پاس هستند، اما این کانتینر Android SDK ندارد؛ workflow گیت‌هاب بیلد واقعی، تست و lint را اجرا می‌کند. |
| علت اصلی و **تأییدشده**‌ی «کامپایل نمی‌شود» | `gradlew` / `gradlew.bat` / `gradle-wrapper.jar` در آرشیو **وجود ندارند** (فقط `gradle-wrapper.properties`). دستور `./gradlew ...` بدون این فایل‌ها بلافاصله با خطای «file not found» شکست می‌خورد. رفع در بخش ۳. |
| هماهنگی نسخه‌ها | بررسی شد و مشکلی ندارد: AGP 9.4.0 / Kotlin 2.4.10 / Compose BOM 2026.08.00 در `gradle/libs.versions.toml` با `build.gradle.kts` ریشه یکی هستند. |
| کدهای Kotlin | ۱۰۰+ فایل بررسی شد؛ هیچ فایل بریده‌شده یا آکولاد نامتوازن پیدا نشد. |
| نقاط پرخطر واقعی (تأییدنشده، چون کامپایلر اجرا نشده) | امضای دقیق ۴ API در `AUDIT.md` بخش ۶-۱ فهرست شده‌اند؛ اولین جایی که باید بعد از سبزشدن بیلد چک شوند. |

جزئیات کامل تحلیل و اقدامات لازم در [`FIX_PROMPT.md`](FIX_PROMPT.md).

---

## ۲. ساختار پروژه (فقط ماژول‌های مخصوص این اپ)

```
apps/zahra-app/     کد اختصاصی اپ زهرا (۴۰+ صفحه در ۱۶ بخش UI)
shared/
  core-common        JalaliDate، Digits، LocalStore، PrivacyPolicy، Helplines، ScreenTimeTracker
  core-designsystem   تم/رنگ/تایپوگرافی + PinLockGate
  core-appwrite       کلاینت Appwrite، TablesDB، Auth، Storage، Functions، Realtime
  core-security       AppLock (PBKDF2)، Encryptor (AES-256-GCM)، بیومتریک
  core-notifications  کانال‌ها، ساعات سکوت، یادآور روزانه (AlarmManager)
  core-sync           SyncEngine + صف Outbox — مخصوص این اپ (اپ پدر ندارد)
  feature-pairing     کد ۶ رقمی پیوند با پدر
  feature-hearttoheart متن/ویس/عکس/ویدیو + آلبوم خاطرات مشترک
  feature-calls       سیگنالینگ + WebRTC صوتی/تصویری + تماس ورودی
  feature-playback    پخش کتاب صوتی در پس‌زمینه (Media3) — مخصوص این اپ
backend/              بک‌اند مشترک Appwrite (با اپ پدر مشترک — یک‌بار Deploy می‌شود)
```

وابستگی یک‌طرفه: `apps → features → core`.

---

## ۳. بیلد و اجرا

```bash
# Wrapper در ریپو قرار دارد و نیازی به نصب Gradle جداگانه نیست.
# اگر wrapper را بازسازی می‌کنید، نسخه باید 9.6.0 بماند.

cp local.properties.example local.properties
# appwrite.projectId را پر کنید (خالی هم باشد، اپ در «حالت محلی» بالا می‌آید)

./gradlew :zahra-app:assembleDebug --stacktrace
./gradlew test               # تست‌های JVM (core-common, feature-pairing)
./gradlew :zahra-app:lintDebug
```

پیش‌نیاز: **JDK 17**، Android SDK با `compileSdk 36` / `Build-Tools 36.0.0`.

---

## ۴. تنظیمات محیط (Appwrite)

مقادیر از `local.properties` یا متغیر محیطی خوانده می‌شوند (اولویت با `local.properties`):

| کلید | پیش‌فرض |
|---|---|
| `appwrite.endpoint` | `https://fra.cloud.appwrite.io/v1` |
| `appwrite.projectId` | خالی → حالت محلی (بدون سرور) |
| `appwrite.databaseId` | `main_db` |

چک‌لیست کامل جدول/باکت/تابع لازم: [`APPWRITE.md`](APPWRITE.md).
سیدکردن محتوای آموزشی/آشپزی/آزمون: `cd backend/seed && npm install && node import.js`.

---

## ۵. حریم خصوصی — خط قرمز پروژه

سه جدول هرگز به سرور Sync نمی‌شوند و در کد کلاینت اصلاً Repository ای برای
ارسالشان وجود ندارد: **چرخه (`cycle_entries`)، ژورنال (`journal_entries`)،
متن خام چت‌بات (`chat_lines`)**. این را `PrivacyPolicy` در `core-common` و
`backend/scripts/verify.js` (که هشدار می‌دهد اگر این جدول‌ها روی سرور واقعی
ساخته شده باشند) اجرا می‌کنند. هر تغییری که این مرز را کم‌رنگ می‌کند باید قبل
از پیاده‌سازی تأیید صریح بگیرد.

---

## ۶. برای رفع باگ و تکمیل پروژه

از [`FIX_PROMPT.md`](FIX_PROMPT.md) به‌عنوان پرامپت شروع برای یک ایجنت کدنویسی
(Claude Code یا مشابه) با دسترسی واقعی به JDK/Android SDK استفاده کنید — شامل
اولویت‌بندی دقیق کارهای باقی‌مانده و قوانین کاری (چه چیزی را بدون تأیید صریح
تغییر ندهید).
