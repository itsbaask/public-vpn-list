export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    // Global CORS headers for Android App & Web Clients
    const corsHeaders = {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, HEAD, POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type, Authorization, X-Requested-With, If-None-Match",
    };

    if (request.method === "OPTIONS") {
      return new Response(null, { headers: corsHeaders });
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

          if (!host || host === "127.0.0.1" || host === "0.0.0.0" || host === "localhost") {
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
