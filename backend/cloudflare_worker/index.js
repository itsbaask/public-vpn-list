function unityCanonicalParams(params) {
  return Object.entries(params)
    .filter(([key]) => key !== "hmac")
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([key, value]) => `${key}=${value ?? ""}`)
    .join(",");
}

function md5Bytes(input) {
  const bytes = input instanceof Uint8Array ? input : new TextEncoder().encode(input);
  const bitLength = bytes.length * 8;
  const paddedLength = (((bytes.length + 8) >> 6) + 1) * 64;
  const padded = new Uint8Array(paddedLength);
  padded.set(bytes);
  padded[bytes.length] = 0x80;
  const view = new DataView(padded.buffer);
  view.setUint32(paddedLength - 8, bitLength >>> 0, true);
  view.setUint32(paddedLength - 4, Math.floor(bitLength / 0x100000000), true);
  const rotate = (value, amount) => (value << amount) | (value >>> (32 - amount));
  const add = (a, b) => (a + b) | 0;
  const shifts = [7,12,17,22,7,12,17,22,7,12,17,22,7,12,17,22,5,9,14,20,5,9,14,20,5,9,14,20,5,9,14,20,4,11,16,23,4,11,16,23,4,11,16,23,4,11,16,23,6,10,15,21,6,10,15,21,6,10,15,21,6,10,15,21];
  const constants = Array.from({ length: 64 }, (_, i) => Math.floor(Math.abs(Math.sin(i + 1)) * 0x100000000));
  let a0 = 0x67452301; let b0 = 0xefcdab89; let c0 = 0x98badcfe; let d0 = 0x10325476;
  for (let offset = 0; offset < padded.length; offset += 64) {
    const words = new Uint32Array(16);
    for (let i = 0; i < 16; i += 1) words[i] = view.getUint32(offset + i * 4, true);
    let a = a0; let b = b0; let c = c0; let d = d0;
    for (let i = 0; i < 64; i += 1) {
      let f; let g;
      if (i < 16) { f = (b & c) | (~b & d); g = i; }
      else if (i < 32) { f = (d & b) | (~d & c); g = (5 * i + 1) % 16; }
      else if (i < 48) { f = b ^ c ^ d; g = (3 * i + 5) % 16; }
      else { f = c ^ (b | ~d); g = (7 * i) % 16; }
      const rotated = rotate(add(a, add(f, add(constants[i], words[g]))), shifts[i]);
      const oldD = d; d = c; c = b; b = add(b, rotated); a = oldD;
    }
    a0 = add(a0, a); b0 = add(b0, b); c0 = add(c0, c); d0 = add(d0, d);
  }
  const output = new Uint8Array(16); const result = new DataView(output.buffer);
  result.setUint32(0, a0 >>> 0, true); result.setUint32(4, b0 >>> 0, true); result.setUint32(8, c0 >>> 0, true); result.setUint32(12, d0 >>> 0, true);
  return output;
}

function bytesToHex(bytes) {
  return Array.from(bytes, (byte) => byte.toString(16).padStart(2, "0")).join("");
}

function unitySignature(params, secret) {
  let key = new TextEncoder().encode(secret);
  if (key.length > 64) key = md5Bytes(key);
  const inner = new Uint8Array(64); const outer = new Uint8Array(64);
  inner.fill(0x36); outer.fill(0x5c);
  for (let i = 0; i < key.length; i += 1) { inner[i] ^= key[i]; outer[i] ^= key[i]; }
  const message = new TextEncoder().encode(unityCanonicalParams(params));
  const innerMessage = new Uint8Array(64 + message.length); innerMessage.set(inner); innerMessage.set(message, 64);
  const innerHash = md5Bytes(innerMessage);
  const outerMessage = new Uint8Array(64 + innerHash.length); outerMessage.set(outer); outerMessage.set(innerHash, 64);
  return bytesToHex(md5Bytes(outerMessage));
}

function constantTimeEqual(left, right) {
  if (!left || !right || left.length !== right.length) return false;
  let diff = 0;
  for (let index = 0; index < left.length; index += 1) diff |= left.charCodeAt(index) ^ right.charCodeAt(index);
  return diff === 0;
}

const worker = {
  async fetch(request, env) {
    const url = new URL(request.url);
    const supportedProtocols = new Set([
      "openvpn", "vless", "vmess", "trojan", "shadowsocks", "hysteria2", "hy2", "tuic", "wireguard"
    ]);

    // Global CORS headers for Android App & Web Clients
    const corsHeaders = {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, HEAD, POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type, Authorization, X-Requested-With, If-None-Match",
    };

    if (request.method === "OPTIONS") {
      return new Response(null, { headers: corsHeaders });
    }

    // Unity Ads S2S redeem callback. The secret is a Worker secret, never an APK value.
    if (request.method === "GET" && url.pathname === "/s2s/unity/reward") {
      const params = Object.fromEntries(url.searchParams.entries());
      const { sid, oid, hmac } = params;
      const secret = env.UNITY_S2S_SECRET;
      if (!secret) {
        return new Response("S2S not configured", { status: 503, headers: corsHeaders });
      }
      if (!sid || !oid || !hmac || sid.length > 512 || oid.length > 256 || hmac.length !== 32) {
        return new Response("Missing or invalid callback parameters", { status: 400, headers: corsHeaders });
      }
      const expected = unitySignature(params, secret);
      if (!constantTimeEqual(hmac.toLowerCase(), expected.toLowerCase())) {
        return new Response("Invalid signature", { status: 403, headers: corsHeaders });
      }
      if (!env.VPN_DATA) {
        return new Response("Reward store unavailable", { status: 503, headers: corsHeaders });
      }
      const rewardKey = `UNITY_REWARD:${oid}`;
      const alreadyRedeemed = await env.VPN_DATA.get(rewardKey);
      if (alreadyRedeemed) {
        return new Response("Duplicate order", { status: 409, headers: corsHeaders });
      }
      const separator = sid.indexOf("|");
      const userId = separator > 0 ? sid.slice(0, separator) : sid;
      const nonce = separator > 0 ? sid.slice(separator + 1) : "";
      const reward = { user_id: userId, nonce, sid, oid, granted_seconds: 20 * 60, redeemed_at: new Date().toISOString() };
      await env.VPN_DATA.put(rewardKey, JSON.stringify(reward), { expirationTtl: 60 * 60 * 24 * 30 });
      await env.VPN_DATA.put(`UNITY_REWARD_SID:${encodeURIComponent(sid)}`, JSON.stringify(reward), { expirationTtl: 60 * 60 * 24 * 30 });
      return new Response("1", { status: 200, headers: corsHeaders });
    }

    // App claims one verified Unity reward using the sid registered before the ad.
    if (request.method === "POST" && url.pathname === "/s2s/unity/claim") {
      if (!env.VPN_DATA) return new Response("Reward store unavailable", { status: 503, headers: corsHeaders });
      let body;
      try { body = await request.json(); } catch (_) { return new Response("Invalid JSON", { status: 400, headers: corsHeaders }); }
      const sid = typeof body?.sid === "string" ? body.sid.trim() : "";
      if (!sid || sid.length > 512) return new Response("Missing sid", { status: 400, headers: corsHeaders });
      const rewardKey = `UNITY_REWARD_SID:${encodeURIComponent(sid)}`;
      const reward = await env.VPN_DATA.get(rewardKey, "json");
      if (!reward) return new Response(JSON.stringify({ status: "pending" }), { status: 202, headers: { "Content-Type": "application/json", ...corsHeaders } });
      const claimKey = `UNITY_REWARD_CLAIMED:${reward.oid}`;
      if (await env.VPN_DATA.get(claimKey)) return new Response("Reward already claimed", { status: 409, headers: corsHeaders });
      await env.VPN_DATA.put(claimKey, JSON.stringify({ sid, oid: reward.oid, claimed_at: new Date().toISOString() }), { expirationTtl: 60 * 60 * 24 * 30 });
      return new Response(JSON.stringify({ status: "granted", oid: reward.oid, granted_seconds: reward.granted_seconds || 20 * 60 }), { status: 200, headers: { "Content-Type": "application/json", ...corsHeaders } });
    }

    // Device registration & session limit check endpoint (3 devices for Premium, 10 for Ultra Business)
    if (request.method === "POST" && url.pathname === "/v1/auth/device/register") {
      if (!env.VPN_DATA) return new Response("Store unavailable", { status: 503, headers: corsHeaders });
      let body;
      try { body = await request.json(); } catch (_) { return new Response("Invalid JSON", { status: 400, headers: corsHeaders }); }
      const userId = typeof body?.user_id === "string" ? body.user_id.trim() : "";
      const deviceId = typeof body?.device_id === "string" ? body.device_id.trim() : "";
      const planTier = typeof body?.tier === "string" ? body.tier.trim().toLowerCase() : "free";

      if (!userId || !deviceId) return new Response("Missing user_id or device_id", { status: 400, headers: corsHeaders });

      let maxDevices = 1;
      if (planTier === "ultra_business" || planTier === "secondary" || planTier === "ultra") {
        maxDevices = 10;
      } else if (planTier === "premium" || planTier === "pro" || planTier === "vip") {
        maxDevices = 3;
      }

      const devicesKey = `USER_DEVICES:${userId}`;
      let devices = await env.VPN_DATA.get(devicesKey, "json") || [];
      if (!Array.isArray(devices)) devices = [];

      const now = new Date().toISOString();
      const existingIndex = devices.findIndex(d => d.device_id === deviceId);

      if (existingIndex >= 0) {
        devices[existingIndex].last_seen = now;
      } else {
        if (devices.length >= maxDevices) {
          devices.sort((a, b) => new Date(a.last_seen || 0) - new Date(b.last_seen || 0));
          devices.shift();
        }
        devices.push({ device_id: deviceId, registered_at: now, last_seen: now });
      }

      await env.VPN_DATA.put(devicesKey, JSON.stringify(devices), { expirationTtl: 60 * 60 * 24 * 90 });

      return new Response(JSON.stringify({ status: "allowed", active_devices: devices.length, max_devices: maxDevices }), {
        status: 200,
        headers: { "Content-Type": "application/json", ...corsHeaders }
      });
    }

    // 1. App endpoint: GET /servers or GET /v1/servers.json
    if (request.method === "GET" && (url.pathname === "/servers" || url.pathname === "/v1/servers.json")) {
      let rawData = null;

      // Check KV namespace first
      if (env.VPN_DATA) {
        rawData = await env.VPN_DATA.get("SERVER_LIST");
      }

      // Check R2 bucket fallback
      if (!rawData && env.PUBLICVPN_BUCKET) {
        const object = await env.PUBLICVPN_BUCKET.get("v1/servers.json");
        if (object) {
          rawData = await object.text();
        }
      }

      if (!rawData) {
        return new Response(JSON.stringify({
          error: "server_list_unavailable",
          message: "Server list is being synchronized by the cloud sync engine.",
          updated_at: new Date().toISOString()
        }), {
          status: 503,
          headers: {
            "Content-Type": "application/json; charset=utf-8",
            "Retry-After": "60",
            ...corsHeaders
          }
        });
      }

      try {
        let container = JSON.parse(rawData);
        let servers = container.servers || (Array.isArray(container) ? container : []);

        // Edge Filtering
        const countryFilter = url.searchParams.get("country") || url.searchParams.get("country_code");
        const tierFilter = url.searchParams.get("tier");
        const protocolFilter = url.searchParams.get("protocol");
        const searchQuery = url.searchParams.get("search");
        const limitParam = parseInt(url.searchParams.get("limit") || "0", 10);
        const sortParam = url.searchParams.get("sort");

        // Edge Deduplication Engine
        const seenKeys = new Set();
        let deduped = [];

        for (const s of servers) {
          const host = (s.host || s.exit_ip || "").trim().toLowerCase();
          const port = s.port || 1194;
          const proto = (s.protocol || "openvpn").trim().toLowerCase();
          const key = `${proto}:${host}:${port}`;

          if (!supportedProtocols.has(proto) || !host || host === "127.0.0.1" || host === "0.0.0.0" || host === "localhost") {
            continue;
          }

          if (seenKeys.has(key)) {
            continue;
          }
          seenKeys.add(key);

          // Apply filters
          if (countryFilter && (s.country_code || "").toUpperCase() !== countryFilter.toUpperCase()) {
            continue;
          }
          if (tierFilter && (s.tier || "free").toLowerCase() !== tierFilter.toLowerCase()) {
            continue;
          }
          if (protocolFilter && (s.protocol || "").toLowerCase() !== protocolFilter.toLowerCase()) {
            continue;
          }
          if (searchQuery) {
            const q = searchQuery.toLowerCase();
            const matchName = (s.name || "").toLowerCase().includes(q);
            const matchCountry = (s.country_name || "").toLowerCase().includes(q);
            const matchHost = (s.host || "").toLowerCase().includes(q);
            if (!matchName && !matchCountry && !matchHost) {
              continue;
            }
          }

          deduped.push(s);
        }

        // Dynamic Edge Sorting
        if (sortParam === "speed") {
          deduped.sort((a, b) => (b.speed_mbps || 0) - (a.speed_mbps || 0));
        } else if (sortParam === "ping" || sortParam === "latency") {
          deduped.sort((a, b) => (a.latency_ms || 999) - (b.latency_ms || 999));
        } else if (sortParam === "score") {
          deduped.sort((a, b) => (b.network_score || 0) - (a.network_score || 0));
        }

        // Apply Limit
        if (limitParam > 0 && deduped.length > limitParam) {
          deduped = deduped.slice(0, limitParam);
        }

        container.count = deduped.length;
        container.servers = deduped;

        const responsePayload = JSON.stringify(container);

        return new Response(responsePayload, {
          status: 200,
          headers: {
            "Content-Type": "application/json; charset=utf-8",
            "Cache-Control": "public, max-age=180, s-maxage=180, stale-while-revalidate=3600",
            ...corsHeaders
          }
        });
      } catch (e) {
        // Fallback to raw data if JSON parsing error
        return new Response(rawData, {
          status: 200,
          headers: {
            "Content-Type": "application/json; charset=utf-8",
            "Cache-Control": "public, max-age=300",
            ...corsHeaders
          }
        });
      }
    }

    // 2. Profile endpoint: GET /profiles/:id or GET /v1/profiles/:id.ovpn
    if (request.method === "GET" && (url.pathname.startsWith("/profiles/") || url.pathname.startsWith("/v1/profiles/"))) {
      const parts = url.pathname.split("/");
      const filename = parts[parts.length - 1];
      const serverId = filename.replace(".ovpn", "");

      if (env.PUBLICVPN_BUCKET) {
        const profileObj = await env.PUBLICVPN_BUCKET.get(`v1/profiles/${serverId}.ovpn`);
        if (profileObj) {
          const body = await profileObj.arrayBuffer();
          return new Response(body, {
            status: 200,
            headers: {
              "Content-Type": "application/x-openvpn-profile",
              "Content-Disposition": `attachment; filename="${serverId}.ovpn"`,
              "Cache-Control": "public, max-age=86400, s-maxage=604800",
              ...corsHeaders
            }
          });
        }
      }

      return new Response("Profile Not Found", { status: 404, headers: corsHeaders });
    }

    // 3. Upload endpoint: POST /upload (Automated GitHub Actions Worker)
    if (request.method === "POST" && url.pathname === "/upload") {
      const authHeader = request.headers.get("Authorization");
      if (authHeader !== `Bearer ${env.API_SECRET}`) {
        return new Response("Unauthorized", { status: 401, headers: corsHeaders });
      }

      try {
        const body = await request.json();
        const servers = Array.isArray(body?.servers) ? body.servers : [];
        if (!servers.length || servers.some((s) => !supportedProtocols.has(String(s?.protocol || "").toLowerCase()))) {
          return new Response("Invalid or unsupported server protocol", { status: 422, headers: corsHeaders });
        }
        if (env.VPN_DATA) {
          await env.VPN_DATA.put("SERVER_LIST", JSON.stringify(body));
        }
        return new Response("Servers Updated Successfully", { status: 200, headers: corsHeaders });
      } catch (e) {
        return new Response("Invalid JSON Payload", { status: 400, headers: corsHeaders });
      }
    }

    return new Response("PublicVPNList Enterprise Cloudflare Backend Active", {
      status: 200,
      headers: { "Content-Type": "text/plain", ...corsHeaders }
    });
  }
};

export default worker;
