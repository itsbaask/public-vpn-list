export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    // Common CORS headers for Android App & Web Clients
    const corsHeaders = {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, HEAD, POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type, Authorization",
    };

    if (request.method === "OPTIONS") {
      return new Response(null, { headers: corsHeaders });
    }

    // 1. App endpoint: GET /servers or GET /v1/servers.json
    if (request.method === "GET" && (url.pathname === "/servers" || url.pathname === "/v1/servers.json")) {
      let data = null;

      // Check KV namespace first
      if (env.VPN_DATA) {
        data = await env.VPN_DATA.get("SERVER_LIST");
      }

      // Check R2 bucket fallback
      if (!data && env.PUBLICVPN_BUCKET) {
        const object = await env.PUBLICVPN_BUCKET.get("v1/servers.json");
        if (object) {
          data = await object.text();
        }
      }

      if (!data) {
        return new Response(JSON.stringify({
          updated_at: new Date().toISOString(),
          servers: []
        }), {
          status: 200,
          headers: {
            "Content-Type": "application/json",
            ...corsHeaders
          }
        });
      }

      return new Response(data, {
        status: 200,
        headers: {
          "Content-Type": "application/json; charset=utf-8",
          "Cache-Control": "public, max-age=300, s-maxage=300, stale-while-revalidate=3600",
          ...corsHeaders
        }
      });
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
