package com.itsmcodez.justplayr.manager

import android.app.Activity
import androidx.annotation.MainThread
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.metrolist.music.BuildConfig
import timber.log.Timber

object RewardedAdManager {
    private const val TAG = "RewardedAd"
    private const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    private const val AD_EXPIRATION_MILLIS = 60 * 60 * 1000L

    private var rewardedAd: RewardedAd? = null
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
            Timber.tag(TAG).d("Skipping rewarded preload because ads are disabled for this user.")
            onComplete?.invoke(false)
            return
        }

        if (adUnitId.isBlank()) {
            Timber.tag(TAG).d("Skipping rewarded preload because ad unit id is blank.")
            onComplete?.invoke(false)
            return
        }

        if (!ConsentManager.canRequestAds(activity)) {
            ConsentManager.requestConsentIfNeeded(activity) { canRequestAds ->
                if (canRequestAds) {
                    preload(activity = activity, adUnitId = adUnitId, onComplete = onComplete)
                } else {
                    Timber.tag(TAG).d("Skipping rewarded preload because consent is not available.")
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
        Timber.tag(TAG).d("Requesting rewarded ad: unitId=%s", adUnitId)

        RewardedAd.load(
            activity,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoadingAd = false
                    lastLoadTimeMillis = System.currentTimeMillis()
                    Timber.tag(TAG).d("Rewarded ad loaded: unitId=%s", adUnitId)
                    onComplete?.invoke(true)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoadingAd = false
                    lastLoadTimeMillis = 0L
                    Timber.tag(TAG).e(
                        "Rewarded ad failed to load: unitId=%s code=%s domain=%s message=%s",
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
    fun show(
        activity: Activity,
        adUnitId: String = resolvedAdUnitId(),
        onAdShown: (() -> Unit)? = null,
        onRewardEarned: (RewardItem) -> Unit,
        onAdUnavailable: (() -> Unit)? = null,
        onAdDismissed: (() -> Unit)? = null,
    ) {
        if (SubscriptionManager.shouldDisableAds()) {
            clear()
            onAdUnavailable?.invoke()
            return
        }

        if (adUnitId.isBlank()) {
            Timber.tag(TAG).d("Rewarded ad unit id is blank. Skipping show.")
            onAdUnavailable?.invoke()
            return
        }

        if (!ConsentManager.canRequestAds(activity)) {
            ConsentManager.requestConsentIfNeeded(activity) { canRequestAds ->
                if (canRequestAds) {
                    show(
                        activity = activity,
                        adUnitId = adUnitId,
                        onAdShown = onAdShown,
                        onRewardEarned = onRewardEarned,
                        onAdUnavailable = onAdUnavailable,
                        onAdDismissed = onAdDismissed,
                    )
                } else {
                    onAdUnavailable?.invoke()
                }
            }
            return
        }

        val ad = rewardedAd.takeIf { hasFreshAd() }
        if (ad == null) {
            Timber.tag(TAG).d("Rewarded ad is not ready yet.")
            preload(activity = activity, adUnitId = adUnitId)
            onAdUnavailable?.invoke()
            return
        }

        rewardedAd = null
        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    Timber.tag(TAG).d("Rewarded ad showed fullscreen content.")
                    onAdShown?.invoke()
                }

                override fun onAdDismissedFullScreenContent() {
                    Timber.tag(TAG).d("Rewarded ad dismissed.")
                    preload(activity = activity, adUnitId = adUnitId)
                    onAdDismissed?.invoke()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Timber.tag(TAG).e(
                        "Rewarded ad failed to show: code=%s domain=%s message=%s",
                        adError.code,
                        adError.domain,
                        adError.message,
                    )
                    preload(activity = activity, adUnitId = adUnitId)
                    onAdUnavailable?.invoke()
                }

                override fun onAdImpression() {
                    Timber.tag(TAG).d("Rewarded ad impression recorded.")
                }

                override fun onAdClicked() {
                    Timber.tag(TAG).d("Rewarded ad clicked.")
                }
            }

        ad.show(activity) { rewardItem ->
            Timber.tag(TAG).d(
                "User earned reward: type=%s amount=%s",
                rewardItem.type,
                rewardItem.amount,
            )
            onRewardEarned(rewardItem)
        }
    }

    @MainThread
    fun clear() {
        rewardedAd = null
        isLoadingAd = false
        lastLoadTimeMillis = 0L
    }

    private fun hasFreshAd(): Boolean {
        val hasAd = rewardedAd != null
        val isFresh = System.currentTimeMillis() - lastLoadTimeMillis < AD_EXPIRATION_MILLIS
        return hasAd && isFresh
    }

    private fun resolvedAdUnitId(): String {
        return BuildConfig.ADMOB_REWARDED_ID.ifBlank {
            if (BuildConfig.DEBUG) TEST_REWARDED_AD_UNIT_ID else ""
        }
    }
}
