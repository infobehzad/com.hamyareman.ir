#!/usr/bin/env node
/**
 * مدیریت متغیرهای AI فانکشن study-tutor (پرامپت ۰۴) + اجرای آزمایشی.
 *
 * دو حالت:
 *   node set-study-tutor-ai.js vars   (پیش‌فرض) — تضمین وجود فانکشن + ایجاد/به‌روزرسانی متغیرهای AI
 *   node set-study-tutor-ai.js smoke  — یک سوال آزمایشی اجرا می‌کند و پاسخ را نشان می‌دهد
 *
 * متغیرهای محیطی:
 *   APPWRITE_ENDPOINT / APPWRITE_PROJECT_ID / APPWRITE_API_KEY  (اسکوپ functions.write لازم)
 *   AI_API_KEY   — کلید ارائه‌دهنده؛ فقط از secret گیت‌هاب می‌آید و هرگز در لاگ چاپ نمی‌شود
 *   AI_MODEL     — اختیاری (اگر خالی باشد متغیر مدل دست نمی‌خورد)
 *   AI_ENDPOINT  — اختیاری (خالی = پیش‌فرض سازگار با OpenAI)
 *
 * نکته‌ی Appwrite: مقدار متغیرهای فانکشن بعد از ساخت خوانده نمی‌شود (مسک می‌شود)،
 * بنابراین «به‌روزرسانی شرطی» فقط بر اساس key انجام می‌شود. تغییر متغیرها با
 * deployment تازه فعال می‌شود — ورک‌فلو بعد از این اسکریپت همیشه دیپلوی می‌کند.
 */
const sdk = require('node-appwrite');

const FUNCTION_ID = process.env.STUDY_FUNCTION_ID || 'study-tutor';
const TARGET_IS_SHARED = FUNCTION_ID === 'ai-companion';
const mode = (process.argv[2] || 'vars').toLowerCase();

/** تنظیمات ساخت فانکشن — هم‌قرارداد با backend/appwrite.json و deploy-prompt-04-study-tutor.js */
const SETTINGS = {
    name: 'study-tutor',
    runtime: 'node-20.0',
    execute: ['users'],
    events: [],
    schedule: '',
    timeout: 30,
    enabled: true,
    logging: true,
    entrypoint: 'src/main.js',
    commands: 'npm install',
};

function client() {
    const ENDPOINT = process.env.APPWRITE_ENDPOINT || 'https://fra.cloud.appwrite.io/v1';
    const PROJECT_ID = process.env.APPWRITE_PROJECT_ID || '';
    const API_KEY = process.env.APPWRITE_API_KEY || '';
    if (!PROJECT_ID || !API_KEY) {
        console.error('❌ APPWRITE_PROJECT_ID و APPWRITE_API_KEY لازم است.');
        process.exit(1);
    }
    return new sdk.Functions(
        new sdk.Client().setEndpoint(ENDPOINT).setProject(PROJECT_ID).setKey(API_KEY),
    );
}

/** اگر فانکشن نبود می‌سازد (بدون دیپلوی — دیپلوی با اسکریپت اصلی بعداً می‌آید). */
async function ensureFunction(functions) {
    try {
        await functions.get({ functionId: FUNCTION_ID });
        console.log(`· فانکشن ${FUNCTION_ID} موجود است.`);
    } catch (_) {
        console.log(`➕ فانکشن ${FUNCTION_ID} ساخته می‌شود (اولین‌بار) …`);
        await functions.create({ functionId: FUNCTION_ID, ...SETTINGS });
    }
}

/** ایجاد یا به‌روزرسانی یک متغیر بر اساس key (مقدار قبلی خواندنی نیست). */
async function upsertVariable(functions, key, value) {
    if (!value) {
        console.log(`· ${key}: خالی — رد شد`);
        return;
    }
    const list = await functions.listVariables({ functionId: FUNCTION_ID });
    const found = (list.variables || []).find(v => v.key === key);
    if (found) {
        if (TARGET_IS_SHARED) {
            // فانکشن مشترک ai-companion — متغیرِ موجود را دست نمی‌زنیم (کلید همان است).
            console.log(`· ${key} از قبل روی ${FUNCTION_ID} هست — بدون تغییر`);
            return;
        }
        await functions.updateVariable({
            functionId: FUNCTION_ID, variableId: found.$id, key, value,
        });
        console.log(`♻️ ${key} به‌روز شد`);
    } else {
        await functions.createVariable({ functionId: FUNCTION_ID, key, value });
        console.log(`➕ ${key} ساخته شد`);
    }
}

async function setVars() {
    const AI_API_KEY = process.env.AI_API_KEY || '';
    const AI_MODEL = (process.env.AI_MODEL || '').trim();
    const AI_ENDPOINT = (process.env.AI_ENDPOINT || '').trim();

    if (!AI_API_KEY) {
        if (TARGET_IS_SHARED) {
            // روی فانکشن مشترک ai-companion متغیرهای AI از قبل در کنسول هست؛
            // نبودِ secret خطا نیست — همان تنظیمات کنسول استفاده می‌شود.
            console.log('· AI_API_KEY secret خالی است — متغیرهای موجود کنسولِ ai-companion دست‌نخورده می‌مانند.');
            console.log('  (اگر گام smoke شکست خورد و not_configured داد، یعنی Variables کنسول خالی است؛');
            console.log('   آن‌وقت secret گیت‌هابِ AI_API_KEY را ثبت و این ورک‌فلو را دوباره اجرا کن.)');
            return;
        }
        console.error('');
        console.error('❌ secret گیت‌هابِ AI_API_KEY خالی است.');
        console.error('   ۱) کنسول ارائه‌دهنده‌ی مدل → کلید API را بردار (همان کلید ai-companion).');
        console.error('   ۲) گیت‌هاب › Settings › Secrets and variables › Actions › New repository secret');
        console.error('      Name: AI_API_KEY   Secret: <کلید>');
        console.error('   ۳) این ورک‌فلو را دوباره اجرا کن.');
        process.exit(1);
    }

    const functions = client();
    await ensureFunction(functions);
    await upsertVariable(functions, 'AI_API_KEY', AI_API_KEY);
    await upsertVariable(functions, 'AI_MODEL', AI_MODEL);
    await upsertVariable(functions, 'AI_ENDPOINT', AI_ENDPOINT);
    console.log('✅ متغیرها ست شدند. (برای اثرگذاری، دیپلوی تازه لازم است — گام بعدی ورک‌فلو)');
}

async function smoke() {
    const functions = client();
    console.log('🧪 اجرای آزمایشی study-tutor …');
    const body = JSON.stringify({
        mode: 'study-tutor',
        lessonTitle: 'درس ۱ — معرفی مجموعه',
        sectionTitle: 'مجموعه‌ی تهی و یکتایی اعضا',
        question: 'آیا مجموعه‌ی ∅ با {∅} برابر است؟',
        correctAnswer: 'خیر — {∅} یک عضو دارد که خود ∅ است',
        studentAnswer: 'بله برابرند چون هر دو خالی‌اند',
    });
    const exec = await functions.createExecution({
        functionId: FUNCTION_ID,
        path: '/',
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body,
        async: false,
    });
    console.log(`  status=${exec.status} | http=${exec.responseStatusCode}`);
    const out = (exec.responseBody || '').slice(0, 600);
    let parsed = null;
    try { parsed = JSON.parse(exec.responseBody || 'null'); } catch (_) { /* خام نشان بده */ }
    if (parsed && parsed.ok) {
        console.log(`✅ مدل جواب داد (${parsed.model || '?'}):`);
        console.log('  «' + String(parsed.reply || '').slice(0, 300) + '»');
        console.log('🎉 study-tutor آماده است.');
    } else if (parsed && parsed.error === 'not_configured') {
        console.log('⚠️ فانکشن اجرا شد ولی می‌گوید AI تنظیم نیست — احتمالاً دیپلویِ پس از ست‌کردن متغیرها انجام نشده.');
        console.log('   ورک‌فلو را با redeploy=true دوباره اجرا کن.');
        console.log('  ' + out);
        process.exit(4);
    } else {
        console.log('  پاسخ: ' + out);
        if (parsed && parsed.error === 'upstream_error') {
            console.log('⚠️ کلید/مدل ارائه‌دهنده پذیرفته نشد — AI_API_KEY و AI_MODEL را با مقادیر ai-companion بسنج.');
        }
        process.exit(5);
    }
}

(async () => {
    if (mode === 'smoke') await smoke();
    else await setVars();
})();
