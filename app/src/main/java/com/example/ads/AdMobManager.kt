package com.example.ads

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.R
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages Google AdMob Banner, Interstitial, and Rewarded ads while remaining
 * 100% non-blocking when the player is playing offline.
 */
class AdMobManager(private val context: Context) {

    private val tag = "AdMobManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var isInitialized = false

    private val _isOnline = MutableStateFlow(checkCurrentlyOnline())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isInterstitialReady = MutableStateFlow(false)
    val isInterstitialReady: StateFlow<Boolean> = _isInterstitialReady.asStateFlow()

    private val _isRewardedReady = MutableStateFlow(false)
    val isRewardedReady: StateFlow<Boolean> = _isRewardedReady.asStateFlow()

    val bannerAdUnitId: String by lazy { context.getString(R.string.admob_banner_unit_id) }
    val interstitialAdUnitId: String by lazy { context.getString(R.string.admob_interstitial_unit_id) }
    val rewardedAdUnitId: String by lazy { context.getString(R.string.admob_rewarded_unit_id) }
    val isUsingTestIds: Boolean by lazy {
        context.getString(R.string.admob_app_id).contains("3940256099942544")
    }

    init {
        registerConnectivityListener()
    }

    fun initialize() {
        if (isInitialized) return
        isInitialized = true
        scope.launch(Dispatchers.IO) {
            runCatching {
                MobileAds.initialize(context) {
                    scope.launch(Dispatchers.Main) {
                        if (_isOnline.value) {
                            preloadInterstitial()
                            preloadRewarded()
                        }
                    }
                }
            }.onFailure { err ->
                Log.w(tag, "AdMob initialize skipped/deferred: ${err.message}")
            }
        }
    }

    private fun checkCurrentlyOnline(): Boolean {
        return runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }.getOrDefault(false)
    }

    private fun registerConnectivityListener() {
        runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isOnline.value = true
                    scope.launch(Dispatchers.Main) {
                        if (interstitialAd == null) preloadInterstitial()
                        if (rewardedAd == null) preloadRewarded()
                    }
                }

                override fun onLost(network: Network) {
                    _isOnline.value = checkCurrentlyOnline()
                }
            })
        }
    }

    fun preloadInterstitial() {
        if (!_isOnline.value || interstitialAd != null) return
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            interstitialAdUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    _isInterstitialReady.value = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    _isInterstitialReady.value = false
                    Log.d(tag, "Interstitial load deferred: ${error.message}")
                }
            }
        )
    }

    fun preloadRewarded() {
        if (!_isOnline.value || rewardedAd != null) return
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            rewardedAdUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    _isRewardedReady.value = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    _isRewardedReady.value = false
                    Log.d(tag, "Rewarded load deferred: ${error.message}")
                }
            }
        )
    }

    /**
     * Shows an interstitial ad if loaded and online; otherwise invokes [onComplete] immediately
     * so offline gameplay is never blocked.
     */
    fun showInterstitialIfReady(activity: Activity?, onComplete: () -> Unit) {
        val ad = interstitialAd
        if (activity == null || ad == null || !_isOnline.value) {
            onComplete()
            preloadInterstitial()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                _isInterstitialReady.value = false
                preloadInterstitial()
                onComplete()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                _isInterstitialReady.value = false
                onComplete()
            }
        }
        ad.show(activity)
    }

    /**
     * Shows a rewarded video ad and triggers [onRewardEarned] when the user completes watching.
     */
    fun showRewardedAd(
        activity: Activity?,
        onRewardEarned: () -> Unit,
        onUnavailable: () -> Unit
    ) {
        val ad = rewardedAd
        if (activity == null || ad == null || !_isOnline.value) {
            onUnavailable()
            if (_isOnline.value) preloadRewarded()
            return
        }
        var rewardGranted = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                _isRewardedReady.value = false
                preloadRewarded()
                if (rewardGranted) {
                    onRewardEarned()
                }
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                _isRewardedReady.value = false
                onUnavailable()
            }
        }
        ad.show(activity) {
            rewardGranted = true
        }
    }
}
