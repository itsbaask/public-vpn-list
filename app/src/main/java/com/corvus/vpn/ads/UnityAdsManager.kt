package com.corvus.vpn.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.metadata.PlayerMetaData
import com.corvus.vpn.data.AppConfig
import com.corvus.vpn.data.AccountRepository
import com.corvus.vpn.data.UnityRewardClaimRequest
import com.corvus.vpn.data.VpnApi
import com.corvus.vpn.vpn.VpnSessionManager
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class UnityAdsManager @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val accountRepository: AccountRepository,
    private val vpnApi: VpnApi,
    private val sessionManager: VpnSessionManager
) {

    companion object {
        const val GAME_ID = AppConfig.UNITY_GAME_ID
        const val TEST_MODE = AppConfig.UNITY_TEST_MODE
        const val INTERSTITIAL_ID = "Interstitial_Android"
        const val REWARDED_ID = "Rewarded_Android"
        const val BANNER_ID = "Banner_Android"
        private const val TAG = "UnityAdsManager"
    }

    private var isInitialized = false
    private var lastInterstitialAt = 0L
    private val interstitialCooldownMs = 3 * 60 * 1000L
    private val installationId: String = context.getSharedPreferences("corvus_ads", Context.MODE_PRIVATE)
        .run {
            getString("installation_id", null) ?: UUID.randomUUID().toString().also {
                edit().putString("installation_id", it).apply()
            }
        }
    private val rewardScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            UnityAds.initialize(context, GAME_ID, TEST_MODE, object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    isInitialized = true
                    Log.d(TAG, "Unity Ads Initialized Successfully! Game ID: $GAME_ID")
                    loadRewardedAd(context)
                    loadInterstitialAd(context)
                }

                override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError?, message: String?) {
                    Log.e(TAG, "Unity Ads Initialization Failed: $message")
                }
            })
        } catch (e: Throwable) {
            Log.e(TAG, "Error initializing Unity Ads: ${e.message}")
        }
    }

    fun loadRewardedAd(context: Context) {
        try {
            UnityAds.load(REWARDED_ID, object : IUnityAdsLoadListener {
                override fun onUnityAdsAdLoaded(placementId: String?) {
                    Log.d(TAG, "Rewarded Ad Loaded: $placementId")
                }

                override fun onUnityAdsFailedToLoad(placementId: String?, error: UnityAds.UnityAdsLoadError?, message: String?) {
                    Log.e(TAG, "Rewarded Ad Failed to Load: $message")
                }
            })
        } catch (e: Throwable) {
            Log.e(TAG, "Error in loadRewardedAd: ${e.message}")
        }
    }

    fun loadInterstitialAd(context: Context) {
        try {
            UnityAds.load(INTERSTITIAL_ID, object : IUnityAdsLoadListener {
                override fun onUnityAdsAdLoaded(placementId: String?) {
                    Log.d(TAG, "Interstitial Ad Loaded: $placementId")
                }

                override fun onUnityAdsFailedToLoad(placementId: String?, error: UnityAds.UnityAdsLoadError?, message: String?) {
                    Log.e(TAG, "Interstitial Ad Failed to Load: $message")
                }
            })
        } catch (e: Throwable) {
            Log.e(TAG, "Error in loadInterstitialAd: ${e.message}")
        }
    }

    /**
     * Legacy signature retained for callers. It deliberately does not grant a local reward;
     * the server-side Unity callback is the only authorization source for reward time.
     */
    @Suppress("UNUSED_PARAMETER")
    fun showRewardedAd(activity: Activity, onRewardGranted: (Int) -> Unit) {
        showRewardedAd(activity, accountRepository.userId, onAdCompleted = {})
    }

    /**
     * Shows a rewarded ad after registering sid with Unity. sid is user/installation + nonce;
     * the Worker verifies Unity's HMAC callback and records the OID for replay protection.
     */
    fun showRewardedAd(activity: Activity, accountId: String?, onAdCompleted: () -> Unit = {}) {
        try {
            if (!isInitialized || !UnityAds.isInitialized) return
            val principal = accountId?.takeIf { it.isNotBlank() } ?: "device:$installationId"
            val sid = "$principal|${UUID.randomUUID()}"
            val playerMetaData = PlayerMetaData(activity)
            playerMetaData.setServerId(sid)
            playerMetaData.commit()
            UnityAds.show(activity, REWARDED_ID, object : IUnityAdsShowListener {
                override fun onUnityAdsShowComplete(placementId: String?, state: UnityAds.UnityAdsShowCompletionState?) {
                    Log.d(TAG, "Rewarded Ad Completed: $placementId state=$state sid=$sid")
                    loadRewardedAd(activity)
                    if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                        rewardScope.launch(Dispatchers.Main) {
                            try {
                                onAdCompleted()
                            } catch (e: Exception) {
                                Log.e(TAG, "Error executing onAdCompleted callback", e)
                            }
                        }
                        claimVerifiedReward(sid, {})
                    }
                }

                override fun onUnityAdsShowFailure(placementId: String?, error: UnityAds.UnityAdsShowError?, message: String?) {
                    Log.e(TAG, "Rewarded Ad Show Failure: $message")
                    loadRewardedAd(activity)
                }

                override fun onUnityAdsShowStart(placementId: String?) {}
                override fun onUnityAdsShowClick(placementId: String?) {}
            })
        } catch (e: Throwable) {
            Log.e(TAG, "Error in showRewardedAd: ${e.message}")
        }
    }


    private fun claimVerifiedReward(sid: String, onGranted: () -> Unit) {
        rewardScope.launch {
            repeat(10) { attempt ->
                try {
                    val response = vpnApi.claimUnityReward(UnityRewardClaimRequest(sid))
                    if (response.isSuccessful && response.body()?.status == "granted") {
                        val seconds = response.body()?.granted_seconds ?: 0
                        if (seconds > 0) {
                            withContext(Dispatchers.Main) {
                                sessionManager.addTime(seconds)
                                onGranted()
                            }
                        }
                        return@launch
                    }
                    if (response.code() == 409) return@launch
                } catch (error: Exception) {
                    Log.w(TAG, "Unity reward claim attempt ${attempt + 1} failed: ${error.message}")
                }
                delay(2_000L)
            }
            Log.w(TAG, "Unity reward callback was not available before claim timeout")
        }
    }

    fun showInterstitialAd(activity: Activity) {
        try {
            val now = System.currentTimeMillis()
            if (!isInitialized || now - lastInterstitialAt < interstitialCooldownMs || !UnityAds.isInitialized) return
            lastInterstitialAt = now
            UnityAds.show(activity, INTERSTITIAL_ID, object : IUnityAdsShowListener {
                override fun onUnityAdsShowComplete(placementId: String?, state: UnityAds.UnityAdsShowCompletionState?) {
                    loadInterstitialAd(activity)
                }

                override fun onUnityAdsShowFailure(placementId: String?, error: UnityAds.UnityAdsShowError?, message: String?) {
                    loadInterstitialAd(activity)
                }

                override fun onUnityAdsShowStart(placementId: String?) {}
                override fun onUnityAdsShowClick(placementId: String?) {}
            })
        } catch (e: Throwable) {
            Log.e(TAG, "Error in showInterstitialAd: ${e.message}")
        }
    }

    fun createBannerView(activity: Activity): android.view.View {
        try {
            if (!isInitialized) {
                return android.view.View(activity)
            }
            val bannerView = com.unity3d.services.banners.BannerView(
                activity,
                BANNER_ID,
                com.unity3d.services.banners.UnityBannerSize(320, 50)
            )
            bannerView.listener = object : com.unity3d.services.banners.BannerView.IListener {
                override fun onBannerLoaded(bannerAdView: com.unity3d.services.banners.BannerView?) {
                    Log.d(TAG, "Unity Banner Ad Loaded Successfully!")
                }

                override fun onBannerFailedToLoad(
                    bannerAdView: com.unity3d.services.banners.BannerView?,
                    errorInfo: com.unity3d.services.banners.BannerErrorInfo?
                ) {
                    Log.e(TAG, "Unity Banner Ad Failed to Load: ${errorInfo?.errorMessage}")
                }

                override fun onBannerClick(bannerAdView: com.unity3d.services.banners.BannerView?) {}
                override fun onBannerShown(bannerAdView: com.unity3d.services.banners.BannerView?) {}
                override fun onBannerLeftApplication(bannerAdView: com.unity3d.services.banners.BannerView?) {}
            }
            bannerView.load()
            return bannerView
        } catch (e: Throwable) {
            Log.e(TAG, "Error in createBannerView: ${e.message}")
            return android.view.View(activity)
        }
    }
}
