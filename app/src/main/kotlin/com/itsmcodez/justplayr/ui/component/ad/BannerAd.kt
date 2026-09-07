package com.itsmcodez.justplayr.ui.component.ad

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.metrolist.music.BuildConfig
import com.itsmcodez.justplayr.manager.BannerAdManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.itsmcodez.justplayr.manager.LocalSubscriptionManager
import com.itsmcodez.justplayr.manager.RemoteConfigManager

@Composable
fun BannerAd(
    modifier: Modifier = Modifier,
    adUnitId: String = BuildConfig.ADMOB_BANNER_ID,
    adSize: AdSize? = null,
    adListener: AdListener? = null,
) {
    val context = LocalContext.current
    val subscriptionManager = LocalSubscriptionManager.current
    val isPro = subscriptionManager.isPro
    val remoteConfigFlags by RemoteConfigManager.flags.collectAsStateWithLifecycle()
    var canShowAd by remember(context, adUnitId, remoteConfigFlags.showAds) {
        mutableStateOf(BannerAdManager.canShowBannerAd(context, adUnitId))
    }

    LaunchedEffect(context, adUnitId, isPro, remoteConfigFlags.showAds) {
        if (isPro || !remoteConfigFlags.showAds) {
            canShowAd = false
            return@LaunchedEffect
        }

        BannerAdManager.requestConsentIfNeeded(context, adUnitId) { granted ->
            canShowAd = granted
        }
    }

    if (isPro) return
    if (!remoteConfigFlags.showAds) return
    if (!canShowAd) return

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val availableWidthDp = remember(maxWidth) { maxWidth.value.toInt().coerceAtLeast(320) }
        val resolvedAdSize = remember(adSize, availableWidthDp, context) {
            adSize ?: BannerAdManager.getAdaptiveAdSize(
                context = context,
                widthDp = availableWidthDp,
            )
        }
        val adHeight = remember(resolvedAdSize) { resolvedAdSize.height.dp }

        val adView = remember(adUnitId, resolvedAdSize, adListener) {
            BannerAdManager.createBannerAdView(
                context = context,
                adUnitId = adUnitId,
                adSize = resolvedAdSize,
                adListener = adListener,
            )
        }

        DisposableEffect(adView) {
            onDispose {
                BannerAdManager.destroyBannerAdView(adView)
            }
        }

        AndroidView(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(adHeight),
            factory = { adView },
        )
    }
}

@Composable
fun InlineBannerAd(
    modifier: Modifier = Modifier,
    adUnitId: String = BuildConfig.ADMOB_BANNER_ID,
    adListener: AdListener? = null,
) {
    BannerAd(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        adUnitId = adUnitId,
        adListener = adListener,
    )
}
