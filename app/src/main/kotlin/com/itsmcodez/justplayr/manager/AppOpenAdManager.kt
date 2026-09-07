package com.itsmcodez.justplayr.manager

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.metrolist.music.BuildConfig
import timber.log.Timber

class AppOpenAdManager(
    private val cooldownMillis: Long = DEFAULT_COOLDOWN_MILLIS,
    private val adUnitId: String = resolvedAdUnitId(),
) : DefaultLifecycleObserver {
    private var boundActivity: ComponentActivity? = null
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var lastLoadTimeMillis = 0L
    private var lastShowTimeMillis = 0L

    @MainThread
    fun bind(activity: ComponentActivity) {
        if (SubscriptionManager.shouldDisableAds()) {
            clear()
            return
        }

        if (boundActivity === activity) {
            if (!hasFreshAd()) {
                preload(activity)
            }
            return
        }

        boundActivity?.lifecycle?.removeObserver(this)
        boundActivity = activity
        activity.lifecycle.addObserver(this)
        preload(activity)
    }

    @MainThread
    fun unbind(activity: ComponentActivity) {
        if (boundActivity !== activity) return
        activity.lifecycle.removeObserver(this)
        boundActivity = null
    }

    @MainThread
    fun preload(
        activity: Activity,
        onComplete: ((Boolean) -> Unit)? = null,
    ) {
        if (SubscriptionManager.shouldDisableAds()) {
            clear()
            Timber.tag(TAG).d("Skipping app open preload because ads are disabled for this user.")
            onComplete?.invoke(false)
            return
        }

        if (adUnitId.isBlank()) {
            Timber.tag(TAG).d("Skipping app open preload because ad unit id is blank.")
            onComplete?.invoke(false)
            return
        }

        if (!ConsentManager.canRequestAds(activity)) {
            ConsentManager.requestConsentIfNeeded(activity) { canRequestAds ->
                if (canRequestAds) {
                    preload(activity = activity, onComplete = onComplete)
                } else {
                    Timber.tag(TAG).d("Skipping app open preload because consent is not available.")
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
        Timber.tag(TAG).d("Requesting app open ad: unitId=%s", adUnitId)

        AppOpenAd.load(
            activity,
            adUnitId,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    lastLoadTimeMillis = System.currentTimeMillis()
                    Timber.tag(TAG).d("App open ad loaded: unitId=%s", adUnitId)
                    onComplete?.invoke(true)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    appOpenAd = null
                    isLoadingAd = false
                    lastLoadTimeMillis = 0L
                    Timber.tag(TAG).e(
                        "App open ad failed to load: unitId=%s code=%s domain=%s message=%s",
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
    fun showIfAvailable(
        activity: Activity,
        onAdShown: (() -> Unit)? = null,
        onShowComplete: (() -> Unit)? = null,
    ): Boolean {
        if (SubscriptionManager.shouldDisableAds()) {
            clear()
            onShowComplete?.invoke()
            return false
        }

        if (isShowingAd) {
            Timber.tag(TAG).d("App open ad is already showing.")
            return false
        }

        if (adUnitId.isBlank()) {
            onShowComplete?.invoke()
            return false
        }

        if (!ConsentManager.canRequestAds(activity)) {
            ConsentManager.requestConsentIfNeeded(activity) { canRequestAds ->
                if (canRequestAds) {
                    showIfAvailable(
                        activity = activity,
                        onAdShown = onAdShown,
                        onShowComplete = onShowComplete,
                    )
                } else {
                    onShowComplete?.invoke()
                }
            }
            return false
        }

        if (!isCooldownElapsed()) {
            Timber.tag(TAG).d("Skipping app open ad because cooldown is active.")
            preload(activity)
            onShowComplete?.invoke()
            return false
        }

        val ad = appOpenAd.takeIf { hasFreshAd() }
        if (ad == null) {
            Timber.tag(TAG).d("App open ad is not ready yet.")
            preload(activity)
            onShowComplete?.invoke()
            return false
        }

        isShowingAd = true
        appOpenAd = null
        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    lastShowTimeMillis = System.currentTimeMillis()
                    Timber.tag(TAG).d("App open ad showed fullscreen content.")
                    onAdShown?.invoke()
                }

                override fun onAdDismissedFullScreenContent() {
                    Timber.tag(TAG).d("App open ad dismissed.")
                    isShowingAd = false
                    preload(activity)
                    onShowComplete?.invoke()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Timber.tag(TAG).e(
                        "App open ad failed to show: code=%s domain=%s message=%s",
                        adError.code,
                        adError.domain,
                        adError.message,
                    )
                    isShowingAd = false
                    preload(activity)
                    onShowComplete?.invoke()
                }

                override fun onAdImpression() {
                    Timber.tag(TAG).d("App open ad impression recorded.")
                }

                override fun onAdClicked() {
                    Timber.tag(TAG).d("App open ad clicked.")
                }
            }

        ad.show(activity)
        return true
    }

    @MainThread
    fun clear() {
        appOpenAd = null
        isLoadingAd = false
        isShowingAd = false
        lastLoadTimeMillis = 0L
    }

    override fun onStart(owner: LifecycleOwner) {
        val activity = boundActivity ?: return
        if (activity.isFinishing || activity.isDestroyed) return
        if (SubscriptionManager.shouldDisableAds()) {
            clear()
            return
        }
        showIfAvailable(activity)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        val activity = boundActivity ?: return
        activity.lifecycle.removeObserver(this)
        boundActivity = null
    }

    private fun hasFreshAd(): Boolean {
        val hasAd = appOpenAd != null
        val isFresh = System.currentTimeMillis() - lastLoadTimeMillis < AD_EXPIRATION_MILLIS
        return hasAd && isFresh
    }

    private fun isCooldownElapsed(): Boolean {
        return lastShowTimeMillis == 0L || System.currentTimeMillis() - lastShowTimeMillis >= cooldownMillis
    }

    companion object {
        private const val TAG = "AppOpenAd"
        private const val AD_EXPIRATION_MILLIS = 4 * 60 * 60 * 1000L
        private const val DEFAULT_COOLDOWN_MILLIS = 30_000L
        private const val TEST_APP_OPEN_AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"

        private fun resolvedAdUnitId(): String {
            return BuildConfig.ADMOB_APP_OPEN_ID.ifBlank {
                if (BuildConfig.DEBUG) TEST_APP_OPEN_AD_UNIT_ID else ""
            }
        }
    }
}
