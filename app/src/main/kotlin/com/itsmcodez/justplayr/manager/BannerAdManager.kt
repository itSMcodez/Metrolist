package com.itsmcodez.justplayr.manager

import android.content.Context
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.metrolist.music.BuildConfig
import timber.log.Timber

object BannerAdManager {
    fun canShowBannerAd(
        context: Context,
        adUnitId: String = BuildConfig.ADMOB_BANNER_ID,
    ): Boolean {
        return !SubscriptionManager.shouldDisableAds() &&
            adUnitId.isNotBlank() &&
            ConsentManager.canRequestAds(context)
    }

    fun requestConsentIfNeeded(
        context: Context,
        adUnitId: String = BuildConfig.ADMOB_BANNER_ID,
        onComplete: (Boolean) -> Unit,
    ) {
        if (SubscriptionManager.shouldDisableAds() || adUnitId.isBlank()) {
            onComplete(false)
            return
        }

        ConsentManager.requestConsentIfNeeded(context) { canRequestAds ->
            onComplete(canRequestAds && adUnitId.isNotBlank())
        }
    }

    fun getAdaptiveAdSize(
        context: Context,
        widthDp: Int,
    ): AdSize {
        val safeWidthDp = widthDp.coerceAtLeast(320)
        @Suppress("DEPRECATION")
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, safeWidthDp)
    }

    fun createBannerAdView(
        context: Context,
        adUnitId: String = BuildConfig.ADMOB_BANNER_ID,
        adSize: AdSize,
        adListener: AdListener? = null,
    ): AdView {
        return AdView(context).apply {
            if (SubscriptionManager.shouldDisableAds() || adUnitId.isBlank()) {
                Timber.tag("BannerAd").d("Skipping banner request because ads are disabled for this user.")
                return@apply
            }

            setAdSize(adSize)
            this.adUnitId = adUnitId
            this.adListener =
                object : AdListener() {
                    override fun onAdLoaded() {
                        Timber.tag("BannerAd").d("Banner loaded: unitId=%s size=%sx%s", adUnitId, adSize.width, adSize.height)
                        adListener?.onAdLoaded()
                    }

                    override fun onAdImpression() {
                        Timber.tag("BannerAd").d("Banner impression recorded: unitId=%s", adUnitId)
                        adListener?.onAdImpression()
                    }

                    override fun onAdClicked() {
                        Timber.tag("BannerAd").d("Banner clicked: unitId=%s", adUnitId)
                        adListener?.onAdClicked()
                    }

                    override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                        Timber.tag("BannerAd").e(
                            "Banner failed to load: unitId=%s code=%s domain=%s message=%s",
                            adUnitId,
                            error.code,
                            error.domain,
                            error.message,
                        )
                        adListener?.onAdFailedToLoad(error)
                    }
                }

            Timber.tag("BannerAd").d("Requesting banner: unitId=%s size=%sx%s", adUnitId, adSize.width, adSize.height)
            loadAd(AdRequest.Builder().build())
        }
    }

    fun destroyBannerAdView(adView: AdView?) {
        adView?.destroy()
    }
}
