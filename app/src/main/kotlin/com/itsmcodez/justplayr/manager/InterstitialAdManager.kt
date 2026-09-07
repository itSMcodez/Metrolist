package com.itsmcodez.justplayr.manager

import android.app.Activity
import androidx.annotation.MainThread
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.metrolist.music.BuildConfig
import timber.log.Timber

object InterstitialAdManager {
    private const val TAG = "InterstitialAd"
    private const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val AD_EXPIRATION_MILLIS = 60 * 60 * 1000L

    private var interstitialAd: InterstitialAd? = null
    private var isLoadingAd = false
    private var lastLoadTimeMillis = 0L

    @MainThread
    fun preload(
        activity: Activity,
        adUnitId: String = resolvedAdUnitId(),
        onComplete: ((Boolean) -> Unit)? = null,
    ) {
        if (SubscriptionManager.shouldDisableAds()) {
            clear()
            Timber.tag(TAG).d("Skipping preload because ads are disabled for this user.")
            onComplete?.invoke(false)
            return
        }

        if (adUnitId.isBlank()) {
            Timber.tag(TAG).d("Skipping preload because interstitial ad unit id is blank.")
            onComplete?.invoke(false)
            return
        }

        if (!ConsentManager.canRequestAds(activity)) {
            ConsentManager.requestConsentIfNeeded(activity) { canRequestAds ->
                if (canRequestAds) {
                    preload(activity = activity, adUnitId = adUnitId, onComplete = onComplete)
                } else {
                    Timber.tag(TAG).d("Skipping preload because consent is not available.")
                    onComplete?.invoke(false)
                }
            }
            return
        }

        if (hasFreshAd()) {
            onComplete?.invoke(true)
            return
        }

        if (isLoadingAd) {
            onComplete?.invoke(false)
            return
        }

        isLoadingAd = true
        Timber.tag(TAG).d("Requesting interstitial: unitId=%s", adUnitId)

        InterstitialAd.load(
            activity,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoadingAd = false
                    lastLoadTimeMillis = System.currentTimeMillis()
                    Timber.tag(TAG).d("Interstitial loaded: unitId=%s", adUnitId)
                    onComplete?.invoke(true)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoadingAd = false
                    lastLoadTimeMillis = 0L
                    Timber.tag(TAG).e(
                        "Interstitial failed to load: unitId=%s code=%s domain=%s message=%s",
                        adUnitId,
                        error.code,
                        error.domain,
                        error.message,
                    )
                    onComplete?.invoke(false)
                }
            },
        )
    }

    @MainThread
    fun showAdOnAction(
        activity: Activity,
        adUnitId: String = resolvedAdUnitId(),
        onAction: () -> Unit,
    ) {
        if (SubscriptionManager.shouldDisableAds()) {
            clear()
            onAction()
            return
        }

        if (adUnitId.isBlank()) {
            onAction()
            return
        }

        if (!ConsentManager.canRequestAds(activity)) {
            ConsentManager.requestConsentIfNeeded(activity) { canRequestAds ->
                if (canRequestAds) {
                    showAdOnAction(activity = activity, adUnitId = adUnitId, onAction = onAction)
                } else {
                    onAction()
                }
            }
            return
        }

        val ad = interstitialAd.takeIf { hasFreshAd() }
        if (ad == null) {
            Timber.tag(TAG).d("Interstitial not ready yet. Continuing action without ad.")
            preload(activity = activity, adUnitId = adUnitId)
            onAction()
            return
        }

        var actionHandled = false
        fun continueAction() {
            if (actionHandled) return
            actionHandled = true
            onAction()
        }

        interstitialAd = null
        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Timber.tag(TAG).d("Interstitial dismissed.")
                    preload(activity = activity, adUnitId = adUnitId)
                    continueAction()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Timber.tag(TAG).e(
                        "Interstitial failed to show: code=%s domain=%s message=%s",
                        adError.code,
                        adError.domain,
                        adError.message,
                    )
                    preload(activity = activity, adUnitId = adUnitId)
                    continueAction()
                }

                override fun onAdShowedFullScreenContent() {
                    Timber.tag(TAG).d("Interstitial showed fullscreen content.")
                }

                override fun onAdImpression() {
                    Timber.tag(TAG).d("Interstitial impression recorded.")
                }

                override fun onAdClicked() {
                    Timber.tag(TAG).d("Interstitial clicked.")
                }
            }

        ad.show(activity)
    }

    @MainThread
    fun clear() {
        interstitialAd = null
        isLoadingAd = false
        lastLoadTimeMillis = 0L
    }

    private fun hasFreshAd(): Boolean {
        val hasAd = interstitialAd != null
        val isFresh = System.currentTimeMillis() - lastLoadTimeMillis < AD_EXPIRATION_MILLIS
        return hasAd && isFresh
    }

    private fun resolvedAdUnitId(): String {
        return BuildConfig.ADMOB_INTERSTITIAL_ID.ifBlank {
            if (BuildConfig.DEBUG) TEST_INTERSTITIAL_AD_UNIT_ID else ""
        }
    }
}
