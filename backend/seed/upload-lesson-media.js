#!/usr/bin/env node
/**
 * آپلود رسانه‌ی واقعی درس‌ها به باکت wellness-media (پرامپت ۰۱ — عملیاتی).
 *
 * چه می‌کند؟
 *   ۱) همه‌ی فایل‌های رسانه‌ی «واقعی» (غیرصفر) با قرارداد
 *      `<Cxxx>-<Lnn>-<V|A><NN>.mp4|mp3` یا `<Cxxx>-<E-فصل>-<Lnn>-...` را از
 *      wellness-references/School-books-9 پیدا می‌کند.
 *   ۲) هر فایل را با fileId = نام کامل فایل (مثلاً C905-E01-L01-V01.mp4) در باکت
 *      می‌گذارد — چون پلیر با expandChapters از روی نام، فصل‌های V01..V03 را می‌سازد
 *      و URL را هم از همین fileId می‌بندد. اگر از قبل بود → skip (idempotent).
 *   ۳) با --seed-lessons: برای هر درسی که V01/A01 آن در باکت موجود است، سطر
 *      `lessons` می‌سازد/به‌روز می‌کند (videoUrl/audioUrl + hasVideo/hasAudio).
 *      درسی که رسانه‌اش هنوز آپلود نشده دست‌نخورده می‌ماند (لینک شکسته نمی‌سازیم).
 *
 * placeholderهای صفر بایتی آپلود نمی‌شوند — فقط شمرده می‌شوند.
 *
 * اجرا:        node upload-lesson-media.js [--dry-run] [--seed-lessons] [--book C905]
 * متغیرها:     APPWRITE_ENDPOINT / APPWRITE_PROJECT_ID / APPWRITE_API_KEY / APPWRITE_BUCKET_ID
 * اسکوپ لازم:  storage.write (+ files.read برای چک وجود) و با --seed-lessons: tables.write
 */
const sdk = require('node-appwrite');
const { InputFile } = require('node-appwrite/file');
const fs = require('fs');
const path = require('path');

const dryRun = process.argv.includes('--dry-run');
const seedLessons = process.argv.includes('--seed-lessons');
const bookFilter = (() => {
    const i = process.argv.indexOf('--book');
    return i > -1 ? process.argv[i + 1] : null;
})();

const ENDPOINT = (process.env.APPWRITE_ENDPOINT || 'https://fra.cloud.appwrite.io/v1').replace(/\/+$/, '');
const PROJECT_ID = process.env.APPWRITE_PROJECT_ID || '';
const API_KEY = (process.env.APPWRITE_API_KEY || '').trim();
const BUCKET = process.env.APPWRITE_BUCKET_ID || '6aa1eaae00303400117b';
const DB = process.env.APPWRITE_DATABASE_ID || 'ZahraDB';
const ROOT = path.join(__dirname, '..', '..', 'wellness-references', 'School-books-9');

const MEDIA_RE = /^(C\d+)-((?:E\d+-)?(?:L|R)\d+)-([VA])(\d{2})\.(mp4|mp3)$/;

if (!PROJECT_ID || !API_KEY) {
    console.error('❌ APPWRITE_PROJECT_ID و APPWRITE_API_KEY لازم است.');
    process.exit(1);
}

const client = new sdk.Client().setEndpoint(ENDPOINT).setProject(PROJECT_ID).setKey(API_KEY);
const storage = new sdk.Storage(client);
const tables = new sdk.TablesDB(client);

function viewUrl(fileId) {
    return `${ENDPOINT}/storage/buckets/${BUCKET}/files/${fileId}/view?project=${PROJECT_ID}`;
}

/** همه‌ی رسانه‌های واقعی (غیرصفر) + شمار placeholderها */
function collect() {
    const real = [];
    let placeholders = 0;
    const walk = (dir) => {
        let entries = [];
        try { entries = fs.readdirSync(dir, { withFileTypes: true }); } catch (_) { return; }
        for (const e of entries) {
            const full = path.join(dir, e.name);
            if (e.isDirectory()) walk(full);
            else if (MEDIA_RE.test(e.name)) {
                const size = fs.statSync(full).size;
                if (size === 0) { placeholders++; continue; }
                const m = e.name.match(MEDIA_RE);
                const lessonDir = path.dirname(path.dirname(full)); // <درس>/media/<فایل>
                real.push({
                    full,
                    name: e.name,
                    size,
                    bookCode: m[1],
                    lessonCode: m[2],
                    kind: m[3],
                    num: m[4],
                    lessonTitle: lessonDir.split(path.sep).pop().replace(/^[A-Za-z0-9-]+\s*-\s*/, ''),
                    bookDir: path.dirname(lessonDir).split(path.sep).pop().replace(/\s*\(C\d+\)$/, ''),
                });
            }
        }
    };
    walk(ROOT);
    return { real, placeholders };
}

async function fileExists(fileId) {
    try { await storage.getFile({ bucketId: BUCKET, fileId }); return true; }
    catch (e) {
        if (String(e && e.code) === '404') return false;
        throw e;
    }
}

/** خطای اسکوپ → پیام راهنمای دقیق */
function dieOnScope(e, what) {
    const msg = String(e && e.message || e);
    if (String(e && e.code) === '401' || /scope/i.test(msg)) {
        console.error(`\n❌ کلید API اجازه‌ی ${what} ندارد (401/scope).`);
        console.error('   کنسول Appwrite › Overview › Integrations › API Keys ← کلید را ویرایش/بساز و');
        console.error('   اسکوپ storage.write (و files.read) — و برای سید، tables.write — را اضافه کن.');
        console.error('   سپس secret گیت‌هاب APPWRITE_API_KEY را با همان کلید به‌روز کن.');
        process.exit(2);
    }
    throw e;
}

async function upload() {
    const { real, placeholders } = collect();
    const filtered = bookFilter ? real.filter(f => f.bookCode === bookFilter) : real;
    const uniq = new Map();
    for (const f of filtered) if (!uniq.has(f.name)) uniq.set(f.name, f); // نام = fileId یکتا

    console.log(`📁 رسانه‌ی واقعی: ${uniq.size} فایل (${bookFilter ? 'فقط ' + bookFilter : 'همه‌ی کتاب‌ها'})`);
    console.log(`   placeholder صفر بایتی (آپلود نمی‌شوند): ${placeholders}`);
    if (uniq.size === 0) {
        console.log('');
        console.log('⚠️ هیچ فایل واقعی‌ای در ریپو نیست — فقط placeholder. دو راه:');
        console.log('   الف) فایل‌های واقعی را جای placeholderها بگذار و به ریپو پوش کن، بعد این ورک‌فلو را اجرا کن.');
        console.log('   ب) روی سیستم خودت که فایل‌ها هست، همین اسکریپت را محلی اجرا کن:');
        console.log('      APPWRITE_PROJECT_ID=... APPWRITE_API_KEY=... node backend/seed/upload-lesson-media.js --seed-lessons');
        return { uploaded: 0, skipped: 0, byLesson: new Map() };
    }

    let uploaded = 0, skipped = 0, failed = 0;
    const byLesson = new Map(); // `${bookCode}|${lessonCode}` → {hasVideo,hasAudio,title,bookDir}
    let n = 0;
    for (const f of uniq.values()) {
        n++;
        const prefix = `  [${n}/${uniq.size}] ${f.name}`;
        try {
            if (await fileExists(f.name)) { console.log(`${prefix} · هست (skip)`); skipped++; }
            else if (dryRun) { console.log(`${prefix} [dry] آپلود (${(f.size / 1048576).toFixed(1)} MB)`); }
            else {
                await storage.createFile({
                    bucketId: BUCKET,
                    fileId: f.name,
                    file: InputFile.fromPath(f.full, f.name),
                });
                console.log(`${prefix} ✅ (${(f.size / 1048576).toFixed(1)} MB)`);
            }
            if (!dryRun || true) {
                const key = `${f.bookCode}|${f.lessonCode}`;
                const g = byLesson.get(key) || { hasVideo: false, hasAudio: false, title: f.lessonTitle, bookDir: f.bookDir, bookCode: f.bookCode, lessonCode: f.lessonCode };
                if (f.kind === 'V') g.hasVideo = true; else g.hasAudio = true;
                byLesson.set(key, g);
            }
            uploaded++;
        } catch (e) {
            failed++;
            console.log(`${prefix} ❌ ${e && e.message || e}`);
            dieOnScope(e, 'آپلود (storage.write)');
        }
    }
    console.log(`\n📊 آپلود: ${uploaded} | از قبل بود: ${skipped} | خطا: ${failed}`);
    return { uploaded, skipped, byLesson };
}

/** سید جدول lessons برای درس‌هایی که رسانه‌شان در باکت موجود است */
async function seedLessonsRows(byLesson) {
    const keys = [...byLesson.keys()];
    if (keys.length === 0) {
        console.log('\n(سید lessons: درسی با رسانه‌ی موجود پیدا نشد — رد شد)');
        return;
    }
    console.log(`\n🌱 سید lessons برای ${keys.length} درس …`);
    let created = 0, updated = 0, failed = 0;
    for (const key of keys) {
        const { hasVideo, hasAudio, title, bookDir, bookCode, lessonCode } = byLesson.get(key);
        const rowId = `lesson-${bookCode}-${lessonCode}`;
        const data = {
            title: title || `${bookDir} — ${lessonCode}`,
            subject: bookDir || '',
            grade: 9,
            body: '',
            bookCode,
            bookTitleFa: bookDir || '',
            lessonTitleFa: title || lessonCode,
            lessonNumber: 0, // پایین مرتب می‌شود
            hasVideo, hasAudio,
            videoUrl: hasVideo ? viewUrl(`${bookCode}-${lessonCode}-V01.mp4`) : '',
            audioUrl: hasAudio ? viewUrl(`${bookCode}-${lessonCode}-A01.mp3`) : '',
            chapterMarkers: '[]',
        };
        try {
            let exists = true;
            try { await tables.getRow({ databaseId: DB, tableId: 'lessons', rowId }); }
            catch (_) { exists = false; }
            if (dryRun) { console.log(`  [dry] ${exists ? 'update' : 'create'} ${rowId} (V:${hasVideo ? '✓' : '—'} A:${hasAudio ? '✓' : '—'})`); continue; }
            if (exists) { await tables.updateRow({ databaseId: DB, tableId: 'lessons', rowId, data }); updated++; }
            else { await tables.createRow({ databaseId: DB, tableId: 'lessons', rowId, data }); created++; }
            console.log(`  ✅ ${rowId} (V:${hasVideo ? '✓' : '—'} A:${hasAudio ? '✓' : '—'})`);
        } catch (e) {
            failed++;
            console.log(`  ❌ ${rowId}: ${e && e.message || e}`);
            dieOnScope(e, 'نوشتن جدول lessons (tables.write)');
        }
    }
    console.log(`📊 سید: ساخته ${created} | به‌روز ${updated} | خطا ${failed}`);
}

(async () => {
    console.log('آپلود رسانه‌ی درس‌ها به باکت wellness-media (پرامپت ۰۱ — عملیاتی)');
    console.log(`endpoint: ${ENDPOINT} | project: ${PROJECT_ID} | bucket: ${BUCKET}${dryRun ? ' | [DRY-RUN]' : ''}`);
    const { byLesson } = await upload();
    if (seedLessons) await seedLessonsRows(byLesson);
    console.log('\nتمام شد.');
})();
