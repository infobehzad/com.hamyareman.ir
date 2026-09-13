/**
 * user-bootstrap — ساخت پروفایل و گذاشتن Label نقش.
 *
 * چرا لازم است؟ چون نقش کاربر (zahra/father/guest) هرگز نباید از سمت کلاینت نوشته شود.
 * اپ بعد از ورود این تابع را صدا می‌زند؛ تابع برچسب را سمت سرور می‌گذارد.
 *
 * ورودی (JSON در body): { "app": "zahra" | "father" }
 * خروجی: { "ok": true, "userId": "...", "labels": ["zahra"] }
 *
 * متغیرهای محیطی لازم (در کنسول، روی خود تابع یا پروژه):
 *   APPWRITE_FUNCTION_API_ENDPOINT / APPWRITE_FUNCTION_PROJECT_ID / APPWRITE_FUNCTION_API_KEY
 *   APPWRITE_DATABASE_ID (پیش‌فرض: main_db)
 */
const sdk = require('node-appwrite');

const DATABASE_ID = process.env.APPWRITE_DATABASE_ID || 'main_db';
const PROFILES = 'profiles';
const SETTINGS = 'user_settings';

function adminClient() {
  const client = new sdk.Client()
    .setEndpoint(process.env.APPWRITE_FUNCTION_API_ENDPOINT)
    .setProject(process.env.APPWRITE_FUNCTION_PROJECT_ID);
  // کلید سرور فقط در تابع است و هرگز داخل APK نمی‌رود.
  client.setKey(process.env.APPWRITE_FUNCTION_API_KEY);
  return client;
}

/** سازگاری با هر دو نسل SDK سرور (TablesDB جدید / Databases قدیمی). */
function tablesService(client) {
  const service = sdk.TablesDB ? new sdk.TablesDB(client) : new sdk.Databases(client);
  return {
    create: service.createRow
      ? (db, table, id, data, perms) => service.createRow(db, table, id, data, perms)
      : (db, table, id, data, perms) => service.createDocument(db, table, id, data, perms),
    list: service.listRows
      ? (db, table, queries) => service.listRows(db, table, queries)
      : (db, table, queries) => service.listDocuments(db, table, queries),
  };
}

function userIdOf(req) {
  return (
    req.userId ||
    (req.headers && (req.headers['x-appwrite-user-id'] || req.headers['X-Appwrite-User-Id'])) ||
    ''
  );
}

function parseBody(req) {
  if (!req.body) return {};
  if (typeof req.body === 'string') {
    try { return JSON.parse(req.body); } catch (e) { return {}; }
  }
  return req.body;
}

module.exports = async function (req, res) {
  const userId = userIdOf(req);
  if (!userId) {
    return res.json({ ok: false, error: 'unauthenticated' }, 401);
  }

  const body = parseBody(req);
  const app = String(body.app || 'zahra').toLowerCase() === 'father' ? 'father' : 'zahra';
  const label = app === 'father' ? 'guest' : 'zahra';
  // پدر تا وقتی کد پیوند را وارد نکند `guest` می‌ماند؛ تابع pairing او را `father` می‌کند.

  const client = adminClient();
  const users = new sdk.Users(client);
  const tables = tablesService(client);
  const Permission = sdk.Permission;
  const Role = sdk.Role;

  try {
    const perms = [
      Permission.read(Role.user(userId)),
      Permission.update(Role.user(userId)),
    ];

    await tables.create(
      DATABASE_ID,
      PROFILES,
      `profile_${userId}`,
      { userId, app, displayName: app === 'father' ? 'بابا' : 'زهرا', createdAtMs: Date.now() },
      perms,
    ).catch(() => null); // اگر پروفایل از قبل هست، مهم نیست

    await tables.create(
      DATABASE_ID,
      SETTINGS,
      `settings_${userId}`,
      { userId, weeklyOptIn: false, quietStart: 22, quietEnd: 7, themeMode: 'system' },
      perms,
    ).catch(() => null);

    await users.updateLabels(userId, [label]);

    return res.json({ ok: true, userId, labels: [label] });
  } catch (err) {
    console.error('user-bootstrap failed', err && err.message ? err.message : err);
    return res.json({ ok: false, error: 'bootstrap_failed' }, 500);
  }
};
