package com.corvus.vpn.vpn.model

enum class ConnectionType {
    OPENVPN,
    SING_BOX,
    PROXY
}

enum class ProtocolType {
    OPENVPN_UDP,
    OPENVPN_TCP,
    VLESS,
    VMESS,
    TROJAN,
    SHADOWSOCKS,
    HYSTERIA2,
    HTTP,
    HTTPS,
    SOCKS4,
    SOCKS5
}

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR
}

enum class EngineError {
    OPENVPN_CONFIG_INVALID,
    OPENVPN_START_FAILED,
    OPENVPN_TLS_FAILED,
    SINGBOX_CONFIG_INVALID,
    SINGBOX_START_FAILED,
    PROXY_INVALID,
    PROXY_TIMEOUT,
    PROXY_UNREACHABLE,
    ENGINE_ALREADY_RUNNING,
    VPN_PERMISSION_REQUIRED,
    UNKNOWN_ERROR
}
