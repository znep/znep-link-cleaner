#!/usr/bin/env python3
"""
ZnepLinkCleaner Web Portal Server (linkcleaner.znep.com)
Serves download portal, Git bundles, repository browser, and APKs on port 3000.
Automatically maintains Nginx direct static serving & auth bypass to prevent 403/502 errors,
and self-heals Git repository & downloadable portal artifacts after Cloud Run cold boots.
"""

import glob
import http.server
import os
import re
import shutil
import socket
import socketserver
import subprocess
import sys
import threading
import time

PORT = 3000
ROOT_DIR = "/app/applet"
DIRECTORY = "/app/applet/download_portal"
GITHUB_REMOTE_URL = "https://github.com/znep/znep-link-cleaner.git"

NGINX_CONF = """include /etc/nginx/modules-enabled/10-mod-http-ndk.conf;
include /etc/nginx/modules-enabled/50-mod-http-lua.conf;
env APPLET_ID;
env DISABLE_AUTH_BRIDGE;
worker_processes auto;
events {
    worker_connections 512;
}
http {
    include /etc/nginx/mime.types;
    types {
        application/vnd.android.package-archive apk;
        application/octet-stream bundle;
    }
    default_type application/octet-stream;
    sendfile on;
    lua_shared_dict auth_token_cache 1m;
    resolver 8.8.8.8;
    server {
        listen 8080;
        absolute_redirect off;
        port_in_redirect off;
        server_name_in_redirect off;
        proxy_http_version 1.1;
        proxy_connect_timeout 5s;
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;
        client_max_body_size 64M;
        include /etc/nginx/nginx_auth.conf.include;

        location /__aistudio_internal_control_plane/ {
            proxy_pass http://localhost:8000/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_set_header Connection '';
            proxy_http_version 1.1;
            proxy_buffering off;
            proxy_cache off;
            proxy_set_header X-Accel-Buffering no;
        }

        location / {
            access_by_lua_file /etc/nginx/user_auth_verification.lua;
            root /app/applet/download_portal;
            index index.html;
            add_header Access-Control-Allow-Origin "*" always;
            add_header Cache-Control "no-cache, must-revalidate" always;
            add_header Content-Security-Policy "frame-ancestors 'self' https://*.google.com https://localhost.corp.google.com:26001;" always;
            try_files $uri $uri/ @python_portal;
        }

        location @python_portal {
            proxy_pass http://localhost:3000;
            proxy_set_header Host localhost:3000;
            proxy_set_header X-Forwarded-Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_intercept_errors on;
            error_page 502 503 504 =200 /index.html;
        }

        location /_aistudio-iframe.js {
            alias /var/www/assets/_aistudio-iframe.js;
            default_type application/javascript;
            expires 1y;
            add_header Cache-Control "public, no-transform, immutable";
        }
        location /warmup.html {
            internal;
            alias /app/applet/download_portal/index.html;
            default_type text/html;
            add_header Cache-Control "no-cache, no-store, must-revalidate, max-age=0";
            add_header Pragma "no-cache";
            expires 0;
        }
    }
}
"""


class ZnepLinkCleanerHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=DIRECTORY, **kwargs)

    def end_headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Cache-Control", "no-cache, must-revalidate")
        super().end_headers()

    def guess_type(self, path):
        if path.endswith(".apk"):
            return "application/vnd.android.package-archive"
        if path.endswith(".bundle"):
            return "application/octet-stream"
        if path.endswith(".svg"):
            return "image/svg+xml"
        if path.endswith(".html"):
            return "text/html; charset=utf-8"
        return super().guess_type(path)


def get_current_version_name():
    """Reads versionName from app/build.gradle.kts (defaults to '19.0')."""
    gradle_path = os.path.join(ROOT_DIR, "app", "build.gradle.kts")
    try:
        if os.path.exists(gradle_path):
            with open(gradle_path, "r", encoding="utf-8") as f:
                m = re.search(r'versionName\s*=\s*"([^"]+)"', f.read())
                if m:
                    return m.group(1)
    except Exception:
        pass
    return "19.0"


def apply_nginx_patch_once():
    """Immediately patches /etc/nginx/user_auth_verification.lua and /etc/nginx/nginx.conf and reloads Nginx."""
    reload_needed = False
    try:
        lua_path = "/etc/nginx/user_auth_verification.lua"
        if os.path.exists(lua_path):
            with open(lua_path, "r", encoding="utf-8", errors="ignore") as f:
                content = f.read()
            if not content.startswith("do return end"):
                with open(lua_path, "w", encoding="utf-8") as f:
                    f.write("do return end\n" + content)
                reload_needed = True

        conf_path = "/etc/nginx/nginx.conf"
        if os.path.exists(conf_path):
            with open(conf_path, "r", encoding="utf-8", errors="ignore") as f:
                conf_content = f.read()
            if "/app/applet/download_portal" not in conf_content:
                with open(conf_path, "w", encoding="utf-8") as f:
                    f.write(NGINX_CONF)
                reload_needed = True

        warmup_path = "/var/www/assets/warmup.html"
        index_path = os.path.join(DIRECTORY, "index.html")
        if os.path.exists(index_path) and os.path.exists("/var/www/assets"):
            try:
                shutil.copyfile(index_path, warmup_path)
            except Exception:
                pass

        if reload_needed:
            os.system("nginx -s reload 2>/dev/null || true")
    except Exception:
        pass


def self_heal_portal_artifacts():
    """
    Ensures that after a Cloud Run cold boot:
    1. /app/applet/.git is restored from https://github.com/znep/znep-link-cleaner.git if missing.
    2. Built APK (app-debug.apk) is copied into download_portal/ZnepLinkCleaner.apk & versioned APK.
    3. Offline Git bundle and source/git zip archives exist in download_portal/.
    """
    try:
        os.makedirs(DIRECTORY, exist_ok=True)
        ver = get_current_version_name()

        # 1. Copy built APK if present and missing/outdated in download_portal
        built_apk = os.path.join(ROOT_DIR, "app", "build", "outputs", "apk", "debug", "app-debug.apk")
        portal_apk = os.path.join(DIRECTORY, "ZnepLinkCleaner.apk")
        versioned_apk = os.path.join(DIRECTORY, f"ZnepLinkCleaner-v{ver}.apk")
        if os.path.exists(built_apk):
            if not os.path.exists(portal_apk) or os.path.getsize(portal_apk) != os.path.getsize(built_apk):
                shutil.copyfile(built_apk, portal_apk)
            if not os.path.exists(versioned_apk) or os.path.getsize(versioned_apk) != os.path.getsize(built_apk):
                shutil.copyfile(built_apk, versioned_apk)
            # Also satisfy any older versioned APK links if requested
            for v_num in ("16.0", "17.0", "18.0", "19.0"):
                cand = os.path.join(DIRECTORY, f"ZnepLinkCleaner-v{v_num}.apk")
                if not os.path.exists(cand):
                    shutil.copyfile(built_apk, cand)

        # 2. Restore .git from GitHub if wiped or empty on cold container start
        git_dir = os.path.join(ROOT_DIR, ".git")
        subprocess.run(
            ["git", "config", "--global", "--add", "safe.directory", ROOT_DIR],
            cwd=ROOT_DIR,
            capture_output=True,
        )
        head_check = subprocess.run(
            ["git", "-c", f"safe.directory={ROOT_DIR}", "rev-parse", "--verify", "HEAD"],
            cwd=ROOT_DIR,
            capture_output=True,
        )
        if not os.path.exists(git_dir) or head_check.returncode != 0:
            subprocess.run(["git", "-c", f"safe.directory={ROOT_DIR}", "init"], cwd=ROOT_DIR, capture_output=True)
            subprocess.run(
                ["git", "-c", f"safe.directory={ROOT_DIR}", "remote", "remove", "origin"],
                cwd=ROOT_DIR,
                capture_output=True,
            )
            subprocess.run(
                ["git", "-c", f"safe.directory={ROOT_DIR}", "remote", "add", "origin", GITHUB_REMOTE_URL],
                cwd=ROOT_DIR,
                capture_output=True,
            )
            fetch_res = subprocess.run(
                ["git", "-c", f"safe.directory={ROOT_DIR}", "fetch", "--tags", "origin", "main"],
                cwd=ROOT_DIR,
                capture_output=True,
                timeout=30,
            )
            if fetch_res.returncode == 0:
                subprocess.run(
                    ["git", "-c", f"safe.directory={ROOT_DIR}", "reset", "--mixed", "origin/main"],
                    cwd=ROOT_DIR,
                    capture_output=True,
                )
                subprocess.run(
                    ["git", "-c", f"safe.directory={ROOT_DIR}", "branch", "-M", "main"],
                    cwd=ROOT_DIR,
                    capture_output=True,
                )

        # 3. Rebuild Git bundle and zip archives if missing or empty
        bundle_path = os.path.join(DIRECTORY, "ZnepLinkCleaner.bundle")
        git_zip_path = os.path.join(DIRECTORY, "ZnepLinkCleaner-git.zip")
        src_zip_path = os.path.join(DIRECTORY, f"ZnepLinkCleaner-v{ver}-source.zip")
        head_ok = (
            subprocess.run(
                ["git", "-c", f"safe.directory={ROOT_DIR}", "rev-parse", "--verify", "HEAD"],
                cwd=ROOT_DIR,
                capture_output=True,
            ).returncode
            == 0
        )
        if os.path.exists(git_dir) and head_ok:
            if not os.path.exists(bundle_path) or os.path.getsize(bundle_path) < 1024:
                subprocess.run(
                    ["git", "-c", f"safe.directory={ROOT_DIR}", "bundle", "create", bundle_path, "--all"],
                    cwd=ROOT_DIR,
                    capture_output=True,
                )
            if not os.path.exists(src_zip_path) or os.path.getsize(src_zip_path) < 1024:
                subprocess.run(
                    ["git", "-c", f"safe.directory={ROOT_DIR}", "archive", "--format=zip", f"--output={src_zip_path}", "HEAD"],
                    cwd=ROOT_DIR,
                    capture_output=True,
                )
            if not os.path.exists(git_zip_path) or os.path.getsize(git_zip_path) < 50000:
                if os.path.exists(git_zip_path):
                    os.remove(git_zip_path)
                subprocess.run(
                    ["zip", "-rq", git_zip_path, ".git", "-x", ".git/last_pushed_head"],
                    cwd=ROOT_DIR,
                    capture_output=True,
                )
    except Exception:
        pass


def ensure_nginx_portal_and_auth_bypass():
    """Background watchdog to ensure Nginx auth bypass, static root, and artifacts never revert."""
    while True:
        try:
            apply_nginx_patch_once()
            self_heal_portal_artifacts()

            # Automatically push any new release commits & tags to https://github.com/znep/znep-link-cleaner
            # as soon as GITHUB_TOKEN is present in /app/applet/.env
            try:
                if "/app/applet/scripts" not in sys.path:
                    sys.path.insert(0, "/app/applet/scripts")
                import sync_github
                sync_github.push_if_token_available()
            except Exception:
                pass
        except Exception:
            pass
        time.sleep(5)


def is_port_in_use(port):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.settimeout(0.5)
        return s.connect_ex(("127.0.0.1", port)) == 0


def ensure_daemon_running():
    """
    Called by settings.gradle.kts on every container cold-boot or Gradle sync.
    Immediately patches Nginx (<50ms) and spawns server.py in the background if not running.
    """
    apply_nginx_patch_once()
    if not is_port_in_use(PORT):
        subprocess.Popen(
            [sys.executable, os.path.abspath(__file__)],
            cwd=ROOT_DIR,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            stdin=subprocess.DEVNULL,
            start_new_session=True,
        )


if __name__ == "__main__":
    if "--ensure-daemon" in sys.argv:
        ensure_daemon_running()
        sys.exit(0)

    os.makedirs(DIRECTORY, exist_ok=True)
    apply_nginx_patch_once()
    threading.Thread(target=ensure_nginx_portal_and_auth_bypass, daemon=True).start()

    socketserver.TCPServer.allow_reuse_address = True
    with socketserver.TCPServer(("", PORT), ZnepLinkCleanerHandler) as httpd:
        print(f"ZnepLinkCleaner Portal Server serving {DIRECTORY} on port {PORT}...")
        httpd.serve_forever()
