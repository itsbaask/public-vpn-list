package com.corvus.vpn.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "servers")
@Serializable
data class ServerEntity(
    @PrimaryKey val id: String,
    val protocol: String = "OPENVPN",
    val engine: String = "OPENVPN",
    val transportSecurity: String? = null,
    val name: String = "VPN Server",
    val countryCode: String = "UN",
    val countryName: String = "Unknown",
    val flag: String = "🌐",
    val ping: Int? = null,
    val speed: Long? = null,
    val score: Long? = null,
    val ovpnConfig: String? = null,
    val configUri: String? = null,
    val source: String = "aggregator",
    val tier: String = "free",
    val host: String = "",
    val port: Int = 1194,
    val transport: String = "udp"
)
