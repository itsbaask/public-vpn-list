package com.corvus.vpn.data

import kotlinx.serialization.Serializable

@Serializable
data class IpInfo(
    val query: String,
    val country: String? = null,
    val countryCode: String? = null,
    val city: String? = null,
    val isp: String? = null,
    val org: String? = null,
    val timezone: String? = null,
    val zip: String? = null,
    val regionName: String? = null
)

@Serializable
data class IpApiInfo(
    val status: String,
    val query: String,
    val country: String? = null,
    val countryCode: String? = null,
    val city: String? = null,
    val isp: String? = null,
    val org: String? = null,
    val timezone: String? = null,
    val zip: String? = null,
    val regionName: String? = null
)

@Serializable
data class IpWhoIsInfo(
    val success: Boolean,
    val ip: String,
    val country: String? = null,
    val country_code: String? = null,
    val city: String? = null,
    val connection: IpWhoIsConnection? = null,
    val timezone: IpWhoIsTimezone? = null,
    val region: String? = null,
    val postal: String? = null
)

@Serializable
data class IpWhoIsConnection(
    val isp: String? = null,
    val org: String? = null
)

@Serializable
data class IpWhoIsTimezone(
    val id: String? = null
)

@Serializable
data class FreeIpApiInfo(
    val ipAddress: String,
    val countryName: String? = null,
    val countryCode: String? = null,
    val cityName: String? = null,
    val zipCode: String? = null,
    val timeZone: String? = null,
    val regionName: String? = null
)
