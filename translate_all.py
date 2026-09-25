import os
import xml.etree.ElementTree as ET

translations = {
    "ar": {
        "free_premium_label": "بريميوم مجاني",
        "feature_priority_support": "دعم فني 24/7",
        "openvpn_mode": "OpenVPN",
        "wireguard_mode": "WireGuard",
        "vip_tier": "VIP",
        "pro_title": "Corvus Pro",
        "app_name": "Corvus VPN"
    },
    "de": {
        "free_premium_label": "Kostenloses Premium",
        "feature_priority_support": "24/7 Priority-Support",
        "openvpn_mode": "OpenVPN",
        "wireguard_mode": "WireGuard",
        "vip_tier": "VIP",
        "pro_title": "Corvus Pro",
        "app_name": "Corvus VPN",
        "ping_label": "Ping",
        "download_speed": "Download",
        "upload_speed": "Upload",
        "version_label": "Version",
        "copyright_text": "© 2026 Corvus Inc.",
        "all_filter": "Alle",
        "encrypted_line": "Verschlüsselte Leitung",
        "limited_label": "Begrenzt",
        "free_nodes_label": "Kostenlose Knoten",
        "standard_label": "Standard",
        "continent_europe": "Europa",
        "continent_asia": "Asien",
        "continent_america": "Amerika",
        "continent_africa": "Afrika",
        "continent_oceania": "Ozeanien",
        "continent_other": "Andere",
        "feature_speed": "Geschwindigkeit",
        "feature_locations": "Standorte",
        "feature_global_servers": "Globale Server",
        "feature_no_ads": "Keine Werbung",
        "feature_dedicated_lines": "VIP-Standleitungen"
    },
    "es": {
        "free_premium_label": "Prémium Gratis",
        "feature_priority_support": "Soporte Prioritario 24/7",
        "openvpn_mode": "OpenVPN",
        "wireguard_mode": "WireGuard",
        "vip_tier": "VIP",
        "pro_title": "Corvus Pro",
        "app_name": "Corvus VPN",
        "isp": "ISP",
        "ping_label": "Ping",
        "copyright_text": "© 2026 Corvus Inc.",
        "popular_badge": "POPULAR",
        "all_filter": "Todos",
        "encrypted_line": "Línea Cifrada",
        "limited_label": "Limitado",
        "free_nodes_label": "Nodos Grátis",
        "standard_label": "Estándar",
        "continent_europe": "Europa",
        "continent_asia": "Asia",
        "continent_america": "América",
        "continent_africa": "África",
        "continent_oceania": "Oceanía",
        "continent_other": "Otros",
        "feature_speed": "Velocidad",
        "feature_locations": "Ubicaciones",
        "feature_global_servers": "Servidores Globales",
        "feature_no_ads": "Sin Anuncios",
        "feature_dedicated_lines": "Líneas VIP Dedicadas"
    },
    "fr": {
        "free_premium_label": "Premium Gratuit",
        "feature_priority_support": "Support Prioritaire 24/7",
        "openvpn_mode": "OpenVPN",
        "wireguard_mode": "WireGuard",
        "vip_tier": "VIP",
        "pro_title": "Corvus Pro",
        "app_name": "Corvus VPN",
        "version_label": "Version",
        "ping_label": "Ping",
        "copyright_text": "© 2026 Corvus Inc.",
        "all_filter": "Tous",
        "encrypted_line": "Ligne Chiffrée",
        "limited_label": "Limité",
        "free_nodes_label": "Nœuds Gratuits",
        "standard_label": "Standard",
        "continent_europe": "Europe",
        "continent_asia": "Asie",
        "continent_america": "Amérique",
        "continent_africa": "Afrique",
        "continent_oceania": "Océanie",
        "continent_other": "Autre",
        "feature_speed": "Vitesse",
        "feature_locations": "Emplacements",
        "feature_global_servers": "Serveurs Mondiaux",
        "feature_no_ads": "Sans Publicité",
        "feature_dedicated_lines": "Lignes VIP Dédiées"
    },
    "ja": {
        "free_premium_label": "無料プレミアム",
        "feature_priority_support": "24/7 優先サポート",
        "openvpn_mode": "OpenVPN",
        "wireguard_mode": "WireGuard",
        "vip_tier": "VIP",
        "pro_title": "Corvus Pro",
        "app_name": "Corvus VPN",
        "ping_label": "Ping",
        "copyright_text": "© 2026 Corvus Inc."
    },
    "pt": {
        "free_premium_label": "Premium Grátis",
        "feature_priority_support": "Suporte Prioritário 24/7",
        "openvpn_mode": "OpenVPN",
        "wireguard_mode": "WireGuard",
        "vip_tier": "VIP",
        "pro_title": "Corvus Pro",
        "app_name": "Corvus VPN",
        "ping_label": "Ping",
        "download_speed": "Download",
        "upload_speed": "Upload",
        "copyright_text": "© 2026 Corvus Inc.",
        "popular_badge": "POPULAR",
        "continent_oceania": "Oceania"
    },
    "ru": {
        "free_premium_label": "Бесплатный Премиум",
        "feature_priority_support": "Поддержка 24/7",
        "openvpn_mode": "OpenVPN",
        "wireguard_mode": "WireGuard",
        "vip_tier": "VIP",
        "pro_title": "Corvus Pro",
        "app_name": "Corvus VPN",
        "copyright_text": "© 2026 Corvus Inc."
    },
    "tr": {
        "free_premium_label": "Ücretsiz Premium",
        "feature_priority_support": "7/24 Öncelikli Destek",
        "openvpn_mode": "OpenVPN",
        "wireguard_mode": "WireGuard",
        "vip_tier": "VIP",
        "pro_title": "Corvus Pro",
        "app_name": "Corvus VPN",
        "ping_label": "Ping",
        "copyright_text": "© 2026 Corvus Inc."
    },
    "zh": {
        "free_premium_label": "免费高级版",
        "feature_priority_support": "7x24 小时优先客服",
        "openvpn_mode": "OpenVPN",
        "wireguard_mode": "WireGuard",
        "vip_tier": "VIP",
        "pro_title": "Corvus Pro",
        "app_name": "Corvus VPN",
        "copyright_text": "© 2026 Corvus Inc."
    }
}

res_dir = r"C:\Download\VPN\VPN\app\src\main\res"

for lang, mapping in translations.items():
    path = os.path.join(res_dir, f"values-{lang}", "strings.xml")
    if os.path.exists(path):
        tree = ET.parse(path)
        root = tree.getroot()
        updated = 0
        for elem in root.findall('string'):
            name = elem.attrib.get('name')
            if name in mapping:
                elem.text = mapping[name]
                updated += 1
        tree.write(path, encoding="utf-8", xml_declaration=True)
        print(f"Updated {lang}: {updated} keys translated.")

print("Translation update completed!")
