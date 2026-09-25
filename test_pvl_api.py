import urllib.request
import json

access_key = "pvlk_eb8cc33936641d9492cf0a2740c81412"
protocols = ["openvpn", "vless", "vmess", "shadowsocks", "trojan", "hysteria2"]

for proto in protocols:
    url = f"https://publicvpnlist.com/api/v1/servers?protocol={proto}&status=online&per_page=10"
    req = urllib.request.Request(url, headers={
        "Authorization": f"Bearer {access_key}",
        "Accept": "application/json",
        "User-Agent": "PublicVPNList-Cloud-Sync/1.0"
    })
    try:
        with urllib.request.urlopen(req) as resp:
            data = json.loads(resp.read().decode('utf-8'))
            print(f"Protocol {proto}: success, count:", len(data.get("data", [])))
    except Exception as e:
        print(f"Protocol {proto}: failed -> {e}")
