#!/usr/bin/env python3
"""دیپلوی mode=google-auth روی فانکشن موجود ai-companion (بدون ساخت فانکشن جدید).
پلن رایگان Appwrite سقف functions دارد؛ ورود native گوگل مثل study-tutor به‌صورت
mode روی ai-companion سوار می‌شود. مراحل: scopes (اجتماعی) → آپلود کد → build →
تست دود چهارگانه (گوگل‌اث + زنده‌بودن چت‌بات و study-tutor).
محیط: APPWRITE_ENDPOINT / APPWRITE_PROJECT_ID / APPWRITE_API_KEY"""
import json, os, subprocess, sys, time, urllib.request, urllib.error

EP = os.environ["APPWRITE_ENDPOINT"].rstrip("/")
PROJECT = os.environ["APPWRITE_PROJECT_ID"]
KEY = os.environ["APPWRITE_API_KEY"]
FUNC = "ai-companion"
SRC = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "ai-companion")

def api(method, path, payload=None):
    data = json.dumps(payload).encode() if payload is not None else None
    req = urllib.request.Request(EP + path, data=data, method=method, headers={
        "X-Appwrite-Project": PROJECT, "X-Appwrite-Key": KEY,
        "content-type": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            raw = r.read().decode() or "{}"
            return r.status, json.loads(raw)
    except urllib.error.HTTPError as e:
        return e.code, json.load(e)

# ۱) scopes — فقط اضافه؛ چیزی حذف نمی‌شود
code, fn = api("GET", f"/functions/{FUNC}")
if code >= 400:
    sys.exit(f"GET {FUNC} failed: {fn}")
have = set(fn.get("scopes", []))
need = {"users.read", "users.write", "sessions.write"}
if not need <= have:
    merged = sorted(have | need)
    code, r = api("PATCH", f"/functions/{FUNC}", {"scopes": merged})
    print("scopes patched:", merged, code)
    if code >= 400:
        sys.exit(f"scopes patch failed: {r}")
else:
    print("scopes OK:", sorted(have))

# ۲) tarball از ai-companion
subprocess.run(["tar", "--exclude=node_modules", "--exclude=.npm", "-czf", "/tmp/ai.tgz", "-C", os.path.abspath(SRC), "."], check=True)
boundary = "----ga" + str(int(time.time()))
with open("/tmp/ai.tgz", "rb") as f:
    tar = f.read()
parts = []
for k, v in [("entrypoint", "src/main.js"), ("commands", "npm install"), ("activate", "true")]:
    parts.append(f'--{boundary}\r\ncontent-disposition: form-data; name="{k}"\r\n\r\n{v}\r\n'.encode())
parts.append((f'--{boundary}\r\ncontent-disposition: form-data; name="code"; filename="ai.tgz"\r\n'
              f'content-type: application/gzip\r\n\r\n').encode() + tar + b"\r\n")
parts.append(f"--{boundary}--\r\n".encode())
req = urllib.request.Request(EP + f"/functions/{FUNC}/deployments", data=b"".join(parts), method="POST", headers={
    "X-Appwrite-Project": PROJECT, "X-Appwrite-Key": KEY,
    "content-type": f"multipart/form-data; boundary={boundary}"})
with urllib.request.urlopen(req, timeout=300) as r:
    dep = json.load(r)
print("deployment:", dep.get("$id"), dep.get("status"))

# ۳) صبر تا ready
for i in range(36):
    code, d = api("GET", f"/functions/{FUNC}/deployments/{dep['$id']}")
    st = d.get("status")
    print("status:", st)
    if st == "ready":
        break
    if st == "failed":
        print(d.get("errors")); sys.exit("build failed")
    time.sleep(10)
else:
    sys.exit("timeout waiting build")

# ۴) تست دود چهارگانه
def smoke(name, body, expect):
    code, ex = api("POST", f"/functions/{FUNC}/executions",
                   {"async": False, "body": json.dumps(body, ensure_ascii=False)})
    out = ex.get("responseBody") or ""
    print(f"smoke[{name}] http={code} status={ex.get('responseStatusCode')} → {out[:160]}")
    if code != 201 or expect not in out:
        sys.exit(f"smoke {name} failed")

smoke("google-no-token", {"mode": "google-auth"}, '"error":"idToken missing"')
smoke("google-fake-token", {"mode": "google-auth", "idToken": "fake.token.here"}, '"ok":false')
smoke("chatbot-alive", {}, 'empty_message')
smoke("study-tutor-alive", {"mode": "study-tutor"}, '"ok":false')
print("✅ google-auth (mode on ai-companion) deployed — همه‌ی شاخه‌ها پاسخ می‌دهند")
