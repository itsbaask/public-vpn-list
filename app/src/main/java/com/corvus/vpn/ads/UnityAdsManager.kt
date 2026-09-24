package com.corvus.vpn.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnityAdsManager @Inject constructor() {

    companion object {
        const val GAME_ID = "6191257"
        const val TEST_MODE = false
        const val INTERSTITIAL_ID = "Interstitial_Android"
        const val REWARDED_ID = "Rewarded_Android"
        const val BANNER_ID = "Banner_Android"
        private const val TAG = "UnityAdsManager"
    }

    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
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
    }

    fun loadRewardedAd(context: Context) {
        UnityAds.load(REWARDED_ID, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String?) {
                Log.d(TAG, "Rewarded Ad Loaded: $placementId")
            }

            override fun onUnityAdsFailedToLoad(placementId: String?, error: UnityAds.UnityAdsLoadError?, message: String?) {
                Log.e(TAG, "Rewarded Ad Failed to Load: $message")
            }
        })
    }

    fun loadInterstitialAd(context: Context) {
        UnityAds.load(INTERSTITIAL_ID, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String?) {
                Log.d(TAG, "Interstitial Ad Loaded: $placementId")
            }

            override fun onUnityAdsFailedToLoad(placementId: String?, error: UnityAds.UnityAdsLoadError?, message: String?) {
                Log.e(TAG, "Interstitial Ad Failed to Load: $message")
            }
        })
    }

    fun showRewardedAd(activity: Activity, onRewardGranted: (Int) -> Unit) {
        UnityAds.show(activity, REWARDED_ID, object : IUnityAdsShowListener {
            override fun onUnityAdsShowComplete(placementId: String?, state: UnityAds.UnityAdsShowCompletionState?) {
                Log.d(TAG, "Rewarded Ad Completed: $placementId state=$state")
                loadRewardedAd(activity)
                onRewardGranted(1)
            }

            override fun onUnityAdsShowFailure(placementId: String?, error: UnityAds.UnityAdsShowError?, message: String?) {
                Log.e(TAG, "Rewarded Ad Show Failure: $message")
                loadRewardedAd(activity)
                // Fallback reward grant so user experience is not blocked
                onRewardGranted(1)
            }

            override fun onUnityAdsShowStart(placementId: String?) {}
            override fun onUnityAdsShowClick(placementId: String?) {}
        })
    }

    fun showInterstitialAd(activity: Activity) {
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
    }

    fun createBannerView(activity: Activity): android.view.View {
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
    }
}
