#!/usr/bin/env python3
"""دیپلوی فانکشن‌های سرور از CI.
۱) google-auth — ورود native گوگل (idToken → سشن). ساخته یا به‌روز می‌شود.
۲) ai-companion — اگر روی سرور نبود (حذف در نظافت)، از آرشیو بازگردانده می‌شود
   (چت‌بات/معلم خصوصی). متغیر AI_API_KEY باید در کنسول دوباره وارد شود.
محیط: APPWRITE_ENDPOINT / APPWRITE_PROJECT_ID / APPWRITE_API_KEY"""
import json, os, subprocess, sys, time, urllib.request, urllib.error

EP = os.environ["APPWRITE_ENDPOINT"].rstrip("/")
PROJECT = os.environ["APPWRITE_PROJECT_ID"]
KEY = os.environ["APPWRITE_API_KEY"]
ROOT = os.path.dirname(os.path.abspath(__file__))

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
        raw = ""
        try: raw = e.read().decode()
        except Exception: pass
        try: return e.code, json.loads(raw or "{}")
        except Exception: return e.code, {}

def deploy_function(fid, name, src_dir, scopes, extra_env=None):
    code, rt = api("GET", "/functions/runtimes")
    runtimes = sorted(r["key"] for r in rt.get("runtimes", []) if r["key"].startswith("node-"))
    RUNTIME = runtimes[-1] if runtimes else "node-20.0"
    spec = {
        "name": name, "runtime": RUNTIME, "execute": ["guests"], "events": [],
        "schedule": "", "timeout": 30, "enabled": True, "logging": True,
        "entrypoint": "src/main.js", "commands": "npm install", "scopes": scopes,
    }
    code, resp = api("POST", "/functions", dict(spec, functionId=fid))
    if code == 409:
        code, resp = api("PATCH", f"/functions/{fid}", spec)
        if code >= 400:
            sys.exit(f"PATCH {fid} failed: {resp}")
    elif code >= 400:
        sys.exit(f"POST {fid} failed: {resp}")
    if extra_env:
        api("PATCH", f"/functions/{fid}", {"vars": extra_env})
    tar = os.path.join("/tmp", fid + ".tgz")
    subprocess.run(["tar", "--exclude=node_modules", "-czf", tar, "-C", src_dir, "."], check=True)
    boundary = "----dep" + str(int(time.time()))
    with open(tar, "rb") as f:
        tgz = f.read()
    parts = []
    for k, v in [("entrypoint", "src/main.js"), ("commands", "npm install"), ("activate", "true")]:
        parts.append(f'--{boundary}\r\ncontent-disposition: form-data; name="{k}"\r\n\r\n{v}\r\n'.encode())
    parts.append((f'--{boundary}\r\ncontent-disposition: form-data; name="code"; filename="{fid}.tgz"\r\n'
                  f'content-type: application/gzip\r\n\r\n').encode() + tgz + b"\r\n")
    parts.append(f"--{boundary}--\r\n".encode())
    req = urllib.request.Request(EP + f"/functions/{fid}/deployments", data=b"".join(parts), method="POST", headers={
        "X-Appwrite-Project": PROJECT, "X-Appwrite-Key": KEY,
        "content-type": f"multipart/form-data; boundary={boundary}"})
    with urllib.request.urlopen(req, timeout=300) as r:
        dep = json.loads(r.read().decode() or "{}")
    print(f"[{fid}] deployment:", dep.get("$id"), dep.get("status"))
    for _ in range(36):
        code, d = api("GET", f"/functions/{fid}/deployments/{dep['$id']}")
        st = d.get("status")
        if st == "ready":
            print(f"[{fid}] ready ✓")
            return dep["$id"]
        if st == "failed":
            print(d.get("errors")); sys.exit(f"build {fid} failed")
        time.sleep(10)
    sys.exit(f"timeout building {fid}")

def smoke(fid, body, expect):
    code, ex = api("POST", f"/functions/{fid}/executions",
                   {"async": False, "body": json.dumps(body, ensure_ascii=False)})
    out = ex.get("responseBody") or ""
    print(f"smoke[{fid}] http={code} → {out[:140]}")
    if code != 201 or expect not in out:
        sys.exit(f"smoke {fid} failed")

# ۱) google-auth — اصلی
deploy_function("google-auth", "google-auth",
                os.path.join(ROOT), ["users.read", "users.write", "sessions.write"])
smoke("google-auth", {}, '"ok":false')

# ۲) ai-companion — اگر نبود، بازیابی (چت‌بات/معلم خصوصی)
code, fn = api("GET", "/functions/ai-companion")
if code == 404:
    print("ai-companion نیست → بازیابی از آرشیو (کلید AI را در کنسول دوباره وارد کن)")
    deploy_function("ai-companion", "study-tutor",
                    os.path.join(ROOT, "..", "ai-companion"),
                    ["users.read", "users.write", "sessions.write"])
    smoke("ai-companion", {}, '"ok":false')
else:
    print("ai-companion موجود است ✓")

print("✅ دیپلوی سرور کامل شد")
