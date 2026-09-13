#!/usr/bin/env python3
"""دیپلوی تابع google-auth روی Appwrite (REST) — create/update + deployment + smoke test.
محیط: APPWRITE_ENDPOINT / APPWRITE_PROJECT_ID / APPWRITE_API_KEY"""
import json, os, subprocess, sys, time, urllib.request, urllib.error

EP = os.environ["APPWRITE_ENDPOINT"].rstrip("/")
PROJECT = os.environ["APPWRITE_PROJECT_ID"]
KEY = os.environ["APPWRITE_API_KEY"]
FUNC = "google-auth"
DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)))

def api(method, path, payload=None):
    data = json.dumps(payload).encode() if payload is not None else None
    req = urllib.request.Request(EP + path, data=data, method=method, headers={
        "X-Appwrite-Project": PROJECT, "X-Appwrite-Key": KEY,
        "content-type": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            return r.status, json.load(r)
    except urllib.error.HTTPError as e:
        return e.code, json.load(e)

# ۱) runtime: جدیدترین node
code, rt = api("GET", "/functions/runtimes")
runtimes = [r["key"] for r in rt.get("runtimes", []) if r["key"].startswith("node-")]
RUNTIME = runtimes[-1] if runtimes else "node-20.0"
print("runtime:", RUNTIME)

SPEC = {
    "functionId": FUNC, "name": FUNC, "runtime": RUNTIME,
    "execute": ["guests"], "events": [], "schedule": "", "timeout": 15,
    "enabled": True, "logging": True, "entrypoint": "src/main.js",
    "commands": "npm install", "scopes": ["users.write", "sessions.write"],
}
code, resp = api("POST", "/functions", SPEC)
if code == 409:
    print("exists → update")
    del SPEC["functionId"]
    code, resp = api("PATCH", f"/functions/{FUNC}", SPEC)
print("create/update:", code, json.dumps(resp)[:200])
if code >= 400:
    sys.exit("create/update failed")

# ۲) tarball
subprocess.run(["tar", "--exclude=node_modules", "--exclude=.npm", "-czf", "/tmp/ga.tgz", "-C", DIR, "."], check=True)
# ۳) deployment (multipart)
boundary = "----ga" + str(int(time.time()))
with open("/tmp/ga.tgz", "rb") as f:
    tar = f.read()
parts = []
for k, v in [("entrypoint", "src/main.js"), ("commands", "npm install"), ("activate", "true")]:
    parts.append(f'--{boundary}\r\ncontent-disposition: form-data; name="{k}"\r\n\r\n{v}\r\n'.encode())
parts.append((f'--{boundary}\r\ncontent-disposition: form-data; name="code"; filename="ga.tgz"\r\n'
              f'content-type: application/gzip\r\n\r\n').encode() + tar + b"\r\n")
parts.append(f"--{boundary}--\r\n".encode())
req = urllib.request.Request(EP + f"/functions/{FUNC}/deployments", data=b"".join(parts), method="POST", headers={
    "X-Appwrite-Project": PROJECT, "X-Appwrite-Key": KEY,
    "content-type": f"multipart/form-data; boundary={boundary}"})
with urllib.request.urlopen(req, timeout=300) as r:
    dep = json.load(r)
print("deployment:", dep.get("$id"), dep.get("status"))

# ۴) صبر تا ready
for i in range(36):
    code, d = api("GET", f"/functions/{FUNC}/deployments/{dep['$id']}")
    st = d.get("status")
    print("status:", st)
    if st == "ready": break
    if st == "failed":
        print(d.get("errors")); sys.exit("build failed")
    time.sleep(10)
else:
    sys.exit("timeout waiting build")

# ۵) smoke test — بدون idToken باید خطای تمیز بدهد
code, ex = api("POST", f"/functions/{FUNC}/executions", {"async": False, "body": "{}"})
print("smoke:", code, ex.get("responseStatusCode"), (ex.get("responseBody") or "")[:200])
if code == 201 and '"ok":false' in (ex.get("responseBody") or ""):
    print("✅ google-auth deployed and responsive")
else:
    sys.exit("smoke test failed")
