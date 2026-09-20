// تستِ واقعیِ شیمِ سیک روی DOM (jsdom) — همان HTMLهایی که در اپ رندر می‌شوند.
// اجرا:  python3 tools/seek-shim/extract_shim.py > /tmp/shim.js
//         npm i jsdom && node tools/seek-shim/test_seek_shim.js
// زمان‌های انتظار از «Books/Base-09/ریاضی/04- صوت تدریس/ryazif01*.txt».
const fs = require('fs');
const path = require('path');
const { JSDOM } = require('jsdom');

const shimRaw = fs.readFileSync('/tmp/shim.js', 'utf8');
const base = path.join(__dirname, '..', '..', 'apps/hamyar-app/src/main/assets/math/c905/');
const AUD = path.join(__dirname, '..', '..', 'Books/Base-09/ریاضی/04- صوت تدریس/');

// پارسرِ بردبار: ارقامِ فارسی/عربی، فاصلهٔ اختیاری کنارِ دونقطه، ثانیهٔ ۱ یا ۲ رقمی
// («۲:۸» یعنی ۲ دقیقه و ۸ ثانیه؛ «۱۴ :۴۵» هم قبول است).
const FA = { '۰':'0','۱':'1','۲':'2','۳':'3','۴':'4','۵':'5','۶':'6','۷':'7','۸':'8','۹':'9',
             '٠':'0','١':'1','٢':'2','٣':'3','٤':'4','٥':'5','٦':'6','٧':'7','٨':'8','٩':'9' };
function parseTimingList(text) {
  const out = [];
  for (const line of String(text).split('\n')) {
    const s = line.replace(/[۰-۹٠-٩]/g, (d) => FA[d]);
    const m = s.match(/(\d{1,3})\s*:\s*(\d{1,2})(?!\d)/);
    if (m && Number(m[2]) < 60) out.push(Number(m[1]) * 60000 + Number(m[2]) * 1000);
  }
  return out;
}

// انتظارات مستقیم از فهرست‌های زمان‌بندیِ Books خوانده می‌شوند تا هرگز کهنه نشوند.
const expect = {};
if (fs.existsSync(AUD)) {
  for (const f of fs.readdirSync(AUD)) {
    if (!/^ryazif.+\.txt$/.test(f)) continue;
    const name = f.replace(/\.txt$/, '');
    if (fs.existsSync(base + name + '.html')) {
      expect[name] = parseTimingList(fs.readFileSync(path.join(AUD, f), 'utf8'));
    }
  }
}

function run(html, times) {
  const calls = [];
  const dom = new JSDOM(html, { runScripts: 'outside-only', url: 'https://local.hamyar/' });
  const w = dom.window;
  w.HamyarPlayer = { seek: (ms) => calls.push(Number(ms)) };
  w.Element.prototype.scrollIntoView = function () {};
  w.eval(shimRaw.replace(/var TIMES=\[[^\]]*\];/, `var TIMES=[${times.join(',')}];`));
  const d = w.document;
  const click = (el) => { calls.length = 0; el.dispatchEvent(new w.MouseEvent('click', { bubbles: true, cancelable: true })); return calls[0] ?? null; };
  return {
    toc: [...d.querySelectorAll('.toc a')].map((a, i) => ({ i, ms: click(a), label: a.querySelector('.t')?.textContent ?? null })),
    titles: [...d.querySelectorAll('.section-title')].map((el) => ({ id: el.id, ms: click(el) })),
  };
}

let fail = 0;
const names = Object.keys(expect);
if (names.length === 0) {
  console.log('⚠️  هیچ فهرستِ زمان‌بندی‌ای در Books پیدا نشد — تست رد شد (نه شکست).');
  process.exit(0);
}
for (const name of names) {
  const exp = expect[name];
  const html = fs.readFileSync(base + name + '.html', 'utf8');
  for (const [mode, doc, times] of [['html', html, []], ['map', html.replace(/ data-seek-ms="\d+"/g, ''), exp]]) {
    const r = run(doc, times);
    const tocOk = exp.length > 0 && exp.every((ms, i) => r.toc[i] && r.toc[i].ms === ms);
    const titleOk = r.titles.length > 0 && r.titles.every((t) => t.ms != null);
    const ok = tocOk && titleOk;
    if (!ok) fail++;
    console.log(`${ok ? '✅' : '❌'} ${name} [${mode}] toc=${r.toc.length} titles=${r.titles.length} ` +
      `tocTimes=${tocOk} titleSeek=${titleOk} sampleLabel=${r.toc[0]?.label}`);
    if (!ok) console.log('   ', JSON.stringify(r.toc.slice(0, 3)), JSON.stringify(r.titles.slice(0, 3)), 'expected:', exp.join(','));
  }
}
// درس‌هایی که HTMLشان هست ولی هنوز فهرستِ زمان ندارند: فقط «دود نکردن» بررسی می‌شود.
for (const f of fs.readdirSync(base)) {
  if (!/^ryazif.+\.html$/.test(f)) continue;
  const name = f.replace(/\.html$/, '');
  if (expect[name]) continue;
  const html = fs.readFileSync(base + f, 'utf8');
  const r = run(html, []);
  // این درس‌ها هنوز فهرستِ زمان ندارند؛ فقط «ساختار سالم» بررسی می‌شود.
  // (سه درسِ فصلِ ۳ اساساً «.toc» ندارند؛ وقتی فهرستشان آمد اینجا هم سبز می‌شود.)
  const ok = r.titles.length > 0;
  if (!ok) fail++;
  console.log(`${ok ? '🕓' : '❌'} ${name} [بدون زمان] toc=${r.toc.length} titles=${r.titles.length} — منتظرِ فهرستِ زمان`);
}
console.log(fail === 0 ? '\n✅ همه‌ی بررسی‌ها پاس شد' : `\n❌ ${fail} مورد ناموفق`);
process.exit(fail === 0 ? 0 : 1);
