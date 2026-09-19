// تستِ واقعیِ شیمِ سیک روی DOM (jsdom) — همان HTMLهایی که در اپ رندر می‌شوند.
// اجرا:  python3 tools/seek-shim/extract_shim.py > /tmp/shim.js
//         npm i jsdom && node tools/seek-shim/test_seek_shim.js
// زمان‌های انتظار از «Books/Base-09/ریاضی/04- صوت تدریس/ryazif01*.txt».
const fs = require('fs');
const path = require('path');
const { JSDOM } = require('jsdom');

const shimRaw = fs.readFileSync('/tmp/shim.js', 'utf8');
const base = path.join(__dirname, '..', '..', 'apps/hamyar-app/src/main/assets/math/c905/');
const expect = {
  ryazif01d01: [65000, 253000, 368000, 481000, 549000, 719000, 775000, 915000, 1079000, 1430000],
  ryazif01d02: [68000, 134000, 296000, 479000, 670000, 808000, 946000, 1194000, 1632000],
  ryazif01d03: [79000, 139000, 243000, 491000, 615000, 730000, 910000, 980000, 1073000, 1350000, 1786000],
  ryazif01d04: [82000, 166000, 272000, 383000, 456000, 567000, 702000, 826000, 910000, 1114000, 1664000],
  ryazif01review: [76000, 178000, 247000, 389000, 477000, 552000, 660000, 1146000, 1267000],
};

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
for (const [name, exp] of Object.entries(expect)) {
  const html = fs.readFileSync(base + name + '.html', 'utf8');
  for (const [mode, doc, times] of [['html', html, []], ['map', html.replace(/ data-seek-ms="\d+"/g, ''), exp]]) {
    const r = run(doc, times);
    const tocOk = exp.every((ms, i) => !r.toc[i] || r.toc[i].ms === ms);
    const titleOk = r.titles.length > 0 && r.titles.every((t) => t.ms != null);
    const ok = tocOk && titleOk;
    if (!ok) fail++;
    console.log(`${ok ? '✅' : '❌'} ${name} [${mode}] toc=${r.toc.length} titles=${r.titles.length} ` +
      `tocTimes=${tocOk} titleSeek=${titleOk} sampleLabel=${r.toc[0]?.label}`);
    if (!ok) console.log('   ', JSON.stringify(r.toc.slice(0, 3)), JSON.stringify(r.titles.slice(0, 3)), 'expected:', exp.join(','));
  }
}
console.log(fail === 0 ? '\n✅ همه‌ی بررسی‌ها پاس شد' : `\n❌ ${fail} مورد ناموفق`);
process.exit(fail === 0 ? 0 : 1);
