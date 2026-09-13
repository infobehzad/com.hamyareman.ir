/**
 * google-auth — ورود استاندارد گوگل روی اندروید (Credential Manager).
 *
 * ورودی: { "idToken": "...", "nonce": "..." }
 * کار:
 *  ۱) صحت idToken با Google tokeninfo (audience/nonce/email_verified)؛
 *  ۲) پیدا/ساخت کاربر Appwrite با شناسه‌ی قطعی از sub گوگل؛
 *  ۳) ساخت سشن با createJWT (کلید تابع؛ هیچ کلیدی در اپ نیست).
 * خروجی: { ok: true, userId, secret } — اپ با آن createSession می‌زند.
 */
const sdk = require('node-appwrite');
const crypto = require('crypto');

const AUD = process.env.GOOGLE_WEB_CLIENT_ID ||
  '347554951220-gfvsc84d437nsl6jsg6u06aur5c0fsiu.apps.googleusercontent.com';

module.exports = async ({ req, res, log, error }) => {
  try {
    const body = req.bodyJson && typeof req.bodyJson === 'object'
      ? req.bodyJson
      : (typeof req.body === 'string' ? JSON.parse(req.body || '{}') : (req.body || {}));
    const idToken = String(body.idToken || '');
    const nonce = String(body.nonce || '');
    if (!idToken) return res.json({ ok: false, error: 'idToken missing' });

    const r = await fetch('https://oauth2.googleapis.com/tokeninfo?id_token=' + encodeURIComponent(idToken));
    const info = await r.json();
    if (!info || !info.sub) {
      error('tokeninfo rejected the token');
      return res.json({ ok: false, error: 'توکن گوگل نامعتبر است.' });
    }
    if (info.aud !== AUD) {
      error('aud mismatch: ' + info.aud);
      return res.json({ ok: false, error: 'توکن برای این اپ صادر نشده است.' });
    }
    if (String(info.email_verified) !== 'true') {
      return res.json({ ok: false, error: 'ایمیل گوگل تأیید نشده است.' });
    }
    if (nonce && info.nonce && String(info.nonce) !== nonce) {
      return res.json({ ok: false, error: 'nonce mismatch' });
    }

    const client = new sdk.Client()
      .setEndpoint(process.env.APPWRITE_FUNCTION_API_ENDPOINT)
      .setProject(process.env.APPWRITE_FUNCTION_PROJECT_ID)
      .setKey(process.env.APPWRITE_FUNCTION_API_KEY);
    const users = new sdk.Users(client);
    const account = new sdk.Account(client);

    // شناسه‌ی قطعی از sub — دوباره ورود، همان کاربر.
    const userId = 'g' + crypto.createHash('sha256').update('google:' + info.sub).digest('hex').slice(0, 34);
    try {
      await users.get(userId);
      log('existing user ' + userId);
    } catch (e) {
      await users.create({ userId, email: info.email, name: info.name || '', prefs: { provider: 'google', googleSub: info.sub } });
      log('created user ' + userId);
    }

    const jwt = await account.createJWT({ userId });
    return res.json({ ok: true, userId: jwt.userId, secret: jwt.secret });
  } catch (e) {
    error('google-auth failed: ' + (e && e.message || e));
    return res.json({ ok: false, error: 'ورود ناموفق: ' + (e && e.message || e) });
  }
};
