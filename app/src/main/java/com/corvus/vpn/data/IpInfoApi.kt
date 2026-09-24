package com.corvus.vpn.data

import retrofit2.http.GET
import retrofit2.http.Url

interface IpInfoApi {
    @GET
    suspend fun getIpApiInfo(@Url url: String = "http://ip-api.com/json/"): IpApiInfo

    @GET
    suspend fun getIpWhoIsInfo(@Url url: String): IpWhoIsInfo

    @GET
    suspend fun getFreeIpApiInfo(@Url url: String): FreeIpApiInfo
}
