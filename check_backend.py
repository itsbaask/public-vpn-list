import urllib.request
import json

base_url = "https://pub-cb24fe4df15e483d8cb39116dcff1f7a.r2.dev/"

try:
    print("Fetching manifest...")
    with urllib.request.urlopen(base_url + "v1/manifest.json") as resp:
        manifest = json.loads(resp.read().decode('utf-8'))
        print("Manifest version:", manifest.get('version'))
        print("Manifest count:", manifest.get('count'))
        print("Generated at:", manifest.get('generated_at'))

    print("\nFetching servers...")
    with urllib.request.urlopen(base_url + "v1/servers.json") as resp:
        data = json.loads(resp.read().decode('utf-8'))
        servers = data.get('servers', [])
        print(f"Total servers in backend: {len(servers)}")

        protocols = {}
        for s in servers:
            proto = s.get('protocol', 'unknown').upper()
            protocols[proto] = protocols.get(proto, 0) + 1

        print("Protocols / Types breakdown:", protocols)
        print("Sample servers:")
        for s in servers[:5]:
            print(f" - {s.get('id')}: {s.get('country_name')} ({s.get('country_code')}) | Protocol: {s.get('protocol')} | Host: {s.get('host')}:{s.get('port')}")

except Exception as e:
    print("Error connecting to backend:", e)
