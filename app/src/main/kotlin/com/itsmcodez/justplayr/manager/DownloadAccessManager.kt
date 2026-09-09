package com.itsmcodez.justplayr.manager

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.preferences.core.edit
import com.metrolist.music.R
import com.itsmcodez.justplayr.constant.FreemiumDownloadCountKey
import com.metrolist.music.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.lang.ref.WeakReference

data class DownloadLimitPromptState(
    val requestedDownloads: Int,
)

class DownloadAccessManager private constructor(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var pendingActivityRef: WeakReference<Activity>? = null
    private var pendingDownloadAction: (() -> Unit)? = null

    var consumedDownloads by mutableIntStateOf(0)
        private set

    var promptState by mutableStateOf<DownloadLimitPromptState?>(null)
        private set

    init {
        scope.launch {
            appContext.dataStore.data
                .map { it[FreemiumDownloadCountKey] ?: 0 }
                .distinctUntilChanged()
                .collect { consumedDownloads = it }
        }
    }

    fun requestDownloads(
        activity: Activity?,
        requestedDownloads: Int = 1,
        onApproved: () -> Unit,
    ) {
        if (requestedDownloads <= 0) {
            onApproved()
            return
        }

        if (SubscriptionManager.hasProAccess()) {
            onApproved()
            return
        }

        if (!RemoteConfigManager.getShowAds()) {
            onApproved()
            return
        }

        if (consumedDownloads + requestedDownloads <= FREE_DOWNLOAD_LIMIT) {
            updateConsumedDownloads(consumedDownloads + requestedDownloads)
            onApproved()
            return
        }

        if (activity == null) {
            Timber.tag(TAG).w("Download request blocked because no activity was available for rewarded ad flow.")
            return
        }

        pendingActivityRef = WeakReference(activity)
        pendingDownloadAction = onApproved
        promptState = DownloadLimitPromptState(requestedDownloads = requestedDownloads)
        RewardedAdManager.preload(activity)
    }

    fun tryConsumeDownloads(
        requestedDownloads: Int = 1,
    ): Boolean {
        if (requestedDownloads <= 0) return true
        if (SubscriptionManager.hasProAccess()) return true
        if (!RemoteConfigManager.getShowAds()) return true
        if (consumedDownloads + requestedDownloads > FREE_DOWNLOAD_LIMIT) return false

        updateConsumedDownloads(consumedDownloads + requestedDownloads)
        return true
    }

    fun continueWithRewardedAd() {
        val activity = pendingActivityRef?.get()
        promptState = null

        if (!RemoteConfigManager.getShowAds()) {
            pendingDownloadAction?.invoke()
            clearPendingDownload()
            return
        }

        if (activity == null) {
            clearPendingDownload()
            return
        }

        var rewardEarned = false
        RewardedAdManager.show(
            activity = activity,
            onRewardEarned = {
                rewardEarned = true
                updateConsumedDownloads(0)
                pendingDownloadAction?.invoke()
                clearPendingDownload()
            },
            onAdUnavailable = {
                clearPendingDownload()
                Toast.makeText(
                    appContext,
                    R.string.download_reward_ad_unavailable,
                    Toast.LENGTH_SHORT,
                ).show()
            },
            onAdDismissed = {
                if (!rewardEarned) {
                    clearPendingDownload()
                }
            },
        )
    }

    private fun updateConsumedDownloads(newValue: Int) {
        consumedDownloads = newValue
        scope.launch {
            appContext.dataStore.edit { preferences ->
                preferences[FreemiumDownloadCountKey] = newValue
            }
        }
    }

    private fun clearPendingDownload() {
        pendingActivityRef = null
        pendingDownloadAction = null
    }

    companion object {
        private const val TAG = "DownloadAccessManager"
        private const val FREE_DOWNLOAD_LIMIT = 2

        @Volatile
        private var instance: DownloadAccessManager? = null

        fun getInstance(context: Context): DownloadAccessManager {
            return instance ?: synchronized(this) {
                instance ?: DownloadAccessManager(context).also { instance = it }
            }
        }
    }
}

val LocalDownloadAccessManager = staticCompositionLocalOf<DownloadAccessManager> {
    error("No DownloadAccessManager provided")
}

@Composable
fun DownloadAccessProvider(
    context: Context,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalDownloadAccessManager provides DownloadAccessManager.getInstance(context.applicationContext),
        content = content,
    )
}

fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
