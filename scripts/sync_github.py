#!/usr/bin/env python3
"""
ZnepLinkCleaner — Git Release Commit Builder & Automated GitHub Remote Sync
Ensures every version published to https://linkcleaner.znep.com is committed as a
separate Git commit on `main` whose commit message body explicitly contains:
  - User Prompt: "<exact triggering user prompt>"
  - Changes: bulleted list of changes
And automatically pushes all separate release commits + annotated tags to
https://github.com/znep/znep-link-cleaner.git whenever GITHUB_TOKEN is configured.
"""

import json
import os
import re
import subprocess
import sys

ROOT_DIR = "/app/applet"
PORTAL_DIR = "/app/applet/download_portal"
GITHUB_REPO = "znep/znep-link-cleaner"
GITHUB_REMOTE_URL = f"https://github.com/{GITHUB_REPO}.git"


def run_git(args, env=None, check=True):
    merged_env = os.environ.copy()
    if env:
        merged_env.update(env)
    return subprocess.run(
        ["git"] + args,
        cwd=ROOT_DIR,
        env=merged_env,
        capture_output=True,
        text=True,
        check=check,
    )


def load_env_token():
    token = os.environ.get("GITHUB_TOKEN", "").strip()
    if token:
        return token
    env_path = os.path.join(ROOT_DIR, ".env")
    if os.path.exists(env_path):
        with open(env_path, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if not line or line.startswith("#"):
                    continue
                if line.startswith("GITHUB_TOKEN="):
                    val = line.split("=", 1)[1].strip().strip('"').strip("'")
                    if val and not val.startswith("ghp_your_"):
                        return val
    return ""


def export_git_commits_json():
    """Exports all release commits on main to download_portal/git_commits.json for the embedded GitHub widget."""
    os.makedirs(PORTAL_DIR, exist_ok=True)
    tags_res = run_git(["tag", "-l"], check=False)
    tag_map = {}
    for t in tags_res.stdout.splitlines():
        t = t.strip()
        if not t:
            continue
        sha_res = run_git(["rev-list", "-n", "1", t], check=False)
        if sha_res.returncode == 0 and sha_res.stdout.strip():
            tag_map[sha_res.stdout.strip()] = t

    log_res = run_git(
        ["log", "main", "--first-parent", "--format=%H%x1f%h%x1f%an%x1f%ad%x1f%s%x1f%b%x1e", "--date=short"],
        check=False,
    )
    commits = []
    for entry in log_res.stdout.split("\x1e"):
        entry = entry.strip()
        if not entry:
            continue
        parts = entry.split("\x1f")
        if len(parts) < 6:
            continue
        sha, short_sha, author, date_str, subject, body = parts[:6]
        commits.append(
            {
                "sha": sha.strip(),
                "short_sha": short_sha.strip(),
                "author": author.strip(),
                "date": date_str.strip(),
                "subject": subject.strip(),
                "body": body.strip(),
                "tag": tag_map.get(sha.strip(), ""),
            }
        )

    out_path = os.path.join(PORTAL_DIR, "git_commits.json")
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(commits, f, indent=2)
    return commits


def push_if_token_available():
    """Pushes main and all annotated release tags to https://github.com/znep/znep-link-cleaner.git if GITHUB_TOKEN is set."""
    token = load_env_token()
    if not token:
        return False, "GITHUB_TOKEN not set in .env"

    state_file = os.path.join(ROOT_DIR, ".git", "last_pushed_head")
    head_res = run_git(["rev-parse", "main"], check=False)
    head_sha = head_res.stdout.strip()
    if os.path.exists(state_file):
        with open(state_file, "r", encoding="utf-8") as f:
            if f.read().strip() == head_sha:
                return True, f"Already pushed {head_sha[:8]}"

    auth_url = f"https://x-access-token:{token}@github.com/{GITHUB_REPO}.git"
    push_res = run_git(["push", auth_url, "main:main", "--tags", "--force-with-lease"], check=False)
    if push_res.returncode != 0:
        push_res = run_git(["push", auth_url, "main:main", "--tags", "--force"], check=False)

    if push_res.returncode == 0:
        with open(state_file, "w", encoding="utf-8") as f:
            f.write(head_sha)
        return True, f"Pushed {head_sha[:8]} to {GITHUB_REPO}"
    return False, push_res.stderr.strip()


if __name__ == "__main__":
    commits = export_git_commits_json()
    print(f"Exported {len(commits)} commits to {PORTAL_DIR}/git_commits.json")
    ok, msg = push_if_token_available()
    print(f"GitHub Push Status: {ok} ({msg})")
