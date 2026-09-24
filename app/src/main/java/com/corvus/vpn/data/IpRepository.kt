package com.corvus.vpn.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

@Singleton
class IpRepository @Inject constructor(
    private val ipInfoApi: IpInfoApi
) {
    suspend fun getMyIpInfo(): Result<IpInfo> = withContext(Dispatchers.IO) {
        // Try 1: ip-api.com
        try {
            val res = ipInfoApi.getIpApiInfo("http://ip-api.com/json/")
            if (res.status == "success") {
                return@withContext Result.success(IpInfo(
                    query = res.query,
                    country = res.country,
                    countryCode = res.countryCode,
                    city = res.city,
                    isp = res.isp,
                    org = res.org,
                    timezone = res.timezone,
                    zip = res.zip,
                    regionName = res.regionName
                ))
            }
        } catch (e: Exception) {
            Log.e("IpRepository", "ip-api failed: ${e.message}")
        }

        // Try 2: ipwho.is (HTTPS supported)
        try {
            val res = ipInfoApi.getIpWhoIsInfo("https://ipwho.is/")
            if (res.success) {
                return@withContext Result.success(IpInfo(
                    query = res.ip,
                    country = res.country,
                    countryCode = res.country_code,
                    city = res.city,
                    isp = res.connection?.isp,
                    org = res.connection?.org,
                    timezone = res.timezone?.id,
                    zip = res.postal,
                    regionName = res.region
                ))
            }
        } catch (e: Exception) {
            Log.e("IpRepository", "ipwho.is failed: ${e.message}")
        }

        // Try 3: freeipapi.com (HTTPS supported)
        try {
            val res = ipInfoApi.getFreeIpApiInfo("https://freeipapi.com/api/json")
            return@withContext Result.success(IpInfo(
                query = res.ipAddress,
                country = res.countryName,
                countryCode = res.countryCode,
                city = res.cityName,
                isp = "N/A", // FreeIpApi doesn't provide ISP
                org = "N/A",
                timezone = res.timeZone,
                zip = res.zipCode,
                regionName = res.regionName
            ))
        } catch (e: Exception) {
            Log.e("IpRepository", "freeipapi failed: ${e.message}")
        }

        Result.failure(Exception("All IP services failed"))
    }
}
