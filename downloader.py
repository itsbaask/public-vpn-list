#!/usr/bin/env python3
import argparse
import concurrent.futures
import json
import logging
import os
import re
import signal
import sys
import threading
import time
from datetime import datetime
from pathlib import Path
from typing import Dict, Any, List, Optional, Set
from urllib.parse import urljoin

import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry
from bs4 import BeautifulSoup

# --- Configuration (Monster Edition v12.5 - Fixed Download Engine) ---
CONFIG = {
    "PROJECT_NAME": "public-vpn-list",
    "BASE_URL": "https://publicvpnlist.com",
    "MAX_FILE_SIZE_MB": 24.5,
}

# Silence underlying logs for a clean Dashboard
logging.getLogger("urllib3").setLevel(logging.CRITICAL)

def minify_ovpn(data: str) -> str:
    """Config compressor: Removes comments and extra whitespace"""
    return "\n".join([l.strip() for l in data.splitlines() if l.strip() and not l.strip().startswith(('#', ';'))])

class MonsterDashboard:
    def __init__(self, total):
        self.total = total
        self.done = 0
        self.ok = 0
        self.fail = 0
        self.lock = threading.Lock()
        self.start_time = time.time()

    def log_progress(self, success=True):
        with self.lock:
            self.done += 1
            if success: self.ok += 1
            else: self.fail += 1
            pct = (self.done / self.total * 100) if self.total > 0 else 0
            elapsed = time.time() - self.start_time
            sys.stdout.write(f"\r🚀 MONSTER SYNC: [{self.done}/{self.total}] {pct:.1f}% | ✅ Success: {self.ok} | ❌ Fail: {self.fail} | Speed: {self.done/(elapsed+0.1):.1f} s/s")
            sys.stdout.flush()

class SovereignMonster:
    def __init__(self, args):
        self.args = args
        self.db_path = Path("data/database.json")
        self.output_file = Path("web_dist/servers.json")
        self.db = {}
        self.catalog_meta = {}
        self.stop_requested = False

        self.session = requests.Session()
        retries = Retry(total=5, backoff_factor=2, status_forcelist=[429, 500, 502, 503, 504])
        self.session.mount("https://", HTTPAdapter(max_retries=retries, pool_connections=50, pool_maxsize=100))
        self.session.headers.update({
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        })

        signal.signal(signal.SIGINT, self._abort)
        self._init_fs()

    def _abort(self, s, f): self.stop_requested = True

    def _init_fs(self):
        for p in ["data", "web_dist"]: Path(p).mkdir(parents=True, exist_ok=True)
        if self.db_path.exists():
            try:
                with open(self.db_path, "r", encoding="utf-8") as f: self.db = json.load(f)
            except: pass

    def discover_recursive_sitemaps(self, url=None) -> Set[str]:
        target = url or f"{CONFIG['BASE_URL']}/sitemap.xml"
        ids = set()
        try:
            res = self.session.get(target, timeout=25)
            if res.status_code != 200: return ids
            soup = BeautifulSoup(res.text, "xml")
            for sm in soup.find_all("sitemap"):
                loc = sm.find("loc")
                if loc: ids.update(self.discover_recursive_sitemaps(loc.text.strip()))
            for loc in soup.find_all("loc"):
                if m := re.search(r'/server/(\d+)', loc.text): ids.add(m.group(1))
        except: pass
        return ids

    def scrape_catalog_meta(self):
        """Scrapes metadata from first pages of catalog"""
        for p in range(1, 6):
            if self.stop_requested: break
            try:
                res = self.session.get(f"{CONFIG['BASE_URL']}/?page={p}&page_size=50&protocol=openvpn", timeout=20)
                soup = BeautifulSoup(res.text, "html.parser")
                rows = soup.find_all("tr")[1:]
                for row in rows:
                    cols = row.find_all("td")
                    link = row.find("a", href=re.compile(r'/server/(\d+)'))
                    if link and len(cols) > 0:
                        sid = re.search(r'/server/(\d+)', link['href']).group(1)
                        country = cols[0].get_text(strip=True) or (cols[0].find("img")["alt"] if cols[0].find("img") else "Unknown")
                        self.catalog_meta[sid] = country
            except: break

    def download_server(self, sid: str, dash: MonsterDashboard):
        """EXACT download logic from your proven working version"""
        try:
            # 1. Fetch Download Page
            dl_res = self.session.get(f"{CONFIG['BASE_URL']}/download/{sid}/", timeout=25)
            if not dl_res or dl_res.status_code != 200:
                dash.log_progress(False)
                return None

            # 2. Extract country
            country = self.catalog_meta.get(sid, "Unknown")
            soup = BeautifulSoup(dl_res.text, "html.parser")
            if country == "Unknown":
                img = soup.find("img", src=re.compile(r'/flags/'))
                if img: country = img.get("alt", "Unknown")

            # 3. Find link using BeautifulSoup (Proven Method)
            link_node = soup.find('a', href=re.compile(r'\.ovpn|/get/|/file/', re.I))
            if not link_node:
                dash.log_progress(False)
                return None

            dl_url = urljoin(CONFIG["BASE_URL"], link_node['href'])

            # 4. Download Config
            f_res = self.session.get(dl_url, timeout=25)
            if f_res and "client" in f_res.text:
                dash.log_progress(True)
                return {
                    "id": sid, "c": country, "cfg": minify_ovpn(f_res.text),
                    "ts": int(time.time()), "st": "on"
                }
        except: pass
        dash.log_progress(False)
        return None

    def run(self):
        print("🔍 Scanning Monster Sitemaps (Deep Discovery)...")
        all_online = self.discover_recursive_sitemaps()
        self.scrape_catalog_meta()

        if not all_online:
            print("❌ No servers found.")
            return

        to_capture = [sid for sid in all_online if sid not in self.db or self.db[sid].get("st") != "on"]
        print(f"🎯 Found {len(all_online)} servers. Fetching {len(to_capture)} new configs...")

        dash = MonsterDashboard(len(to_capture))

        with concurrent.futures.ThreadPoolExecutor(max_workers=self.args.workers) as executor:
            futures = {executor.submit(self.download_server, sid, dash): sid for sid in to_capture}
            for f in concurrent.futures.as_completed(futures):
                if self.stop_requested: break
                res = f.result()
                if res: self.db[res["id"]] = res

        self._deploy()

    def _deploy(self):
        with open(self.db_path, "w", encoding="utf-8") as f: json.dump(self.db, f)
        active = [v for v in self.db.values() if v.get("st") == "on"]
        output = {"updated": int(time.time()), "total": len(active), "servers": active}
        json_str = json.dumps(output, separators=(',', ':'))

        if len(json_str.encode('utf-8')) > (CONFIG["MAX_FILE_SIZE_MB"] * 1024 * 1024):
            active.sort(key=lambda x: x['ts'], reverse=True)
            while len(json.dumps({"updated": output["updated"], "total": len(active), "servers": active}, separators=(',', ':')).encode('utf-8')) > (CONFIG["MAX_FILE_SIZE_MB"] * 1024 * 1024):
                active.pop()
            json_str = json.dumps({"updated": output["updated"], "total": len(active), "servers": active}, separators=(',', ':'))

        with open(self.output_file, "w", encoding="utf-8") as f: f.write(json_str)
        print(f"\n✅ SYNC COMPLETE | Servers: {len(active)} | Size: {len(json_str.encode('utf-8'))/1048576:.2f} MB")

if __name__ == "__main__":
    p = argparse.ArgumentParser()
    p.add_argument("--workers", type=int, default=10)
    p.add_argument("--max-pages", type=int, default=100)
    p.add_argument("--yes", action="store_true")
    SovereignMonster(p.parse_args()).run()
