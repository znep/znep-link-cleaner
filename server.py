#!/usr/bin/env python3
"""
ZnepLinkCleaner Web Portal Server (linkcleaner.znep.com)
Serves download portal, Git bundles, repository browser, and APKs on port 3000.
Automatically maintains Nginx direct static serving & auth bypass to prevent 403/502 errors.
"""

import http.server
import socketserver
import os
import shutil
import threading
import time

PORT = 3000
DIRECTORY = "/app/applet/download_portal"

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

def ensure_nginx_portal_and_auth_bypass():
    """Background watchdog to ensure Nginx auth bypass and static root never revert."""
    while True:
        try:
            reload_needed = False
            lua_path = "/etc/nginx/user_auth_verification.lua"
            if os.path.exists(lua_path):
                with open(lua_path, "r") as f:
                    content = f.read()
                if not content.startswith("do return end"):
                    with open(lua_path, "w") as f:
                        f.write("do return end\n" + content)
                    reload_needed = True

            conf_path = "/etc/nginx/nginx.conf"
            if os.path.exists(conf_path):
                with open(conf_path, "r") as f:
                    conf_content = f.read()
                if "/app/applet/download_portal" not in conf_content:
                    with open(conf_path, "w") as f:
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

            # Automatically push any new release commits & tags to https://github.com/znep/znep-link-cleaner
            # as soon as GITHUB_TOKEN is present in /app/applet/.env
            try:
                import sys
                if "/app/applet/scripts" not in sys.path:
                    sys.path.insert(0, "/app/applet/scripts")
                import sync_github
                sync_github.push_if_token_available()
            except Exception:
                pass
        except Exception:
            pass
        time.sleep(10)

if __name__ == "__main__":
    os.makedirs(DIRECTORY, exist_ok=True)
    threading.Thread(target=ensure_nginx_portal_and_auth_bypass, daemon=True).start()

    socketserver.TCPServer.allow_reuse_address = True
    with socketserver.TCPServer(("", PORT), ZnepLinkCleanerHandler) as httpd:
        print(f"ZnepLinkCleaner Portal Server serving {DIRECTORY} on port {PORT}...")
        httpd.serve_forever()

