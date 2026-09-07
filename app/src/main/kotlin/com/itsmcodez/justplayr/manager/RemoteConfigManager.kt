package com.itsmcodez.justplayr.manager

import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.metrolist.music.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

data class RemoteConfigFlags(
    val enableDiscoverHomepage: Boolean = true,
    val enablePaywall: Boolean = true,
    val enablePlayrAi: Boolean = true,
    val enableWideSearch: Boolean = true,
    val showAds: Boolean = true,
)

/**
 * Manager responsible for handling Firebase Remote Config.
 *
 * This object provides a centralized way to initialize, fetch, and access
 * remote configuration parameters. It also supports real-time updates.
 */
object RemoteConfigManager {
    private object Keys {
        const val ENABLE_DISCOVER_HOMEPAGE = "enable_discover_homepage"
        const val ENABLE_PAYWALL = "enable_paywall"
        const val ENABLE_PLAYR_AI = "enable_playr_ai"
        const val ENABLE_WIDE_SEARCH = "enable_wide_search"
        const val SHOW_ADS = "show_ads"
    }

    private val remoteConfig by lazy {
        Firebase.remoteConfig
    }

    private val _flags = MutableStateFlow(RemoteConfigFlags())
    val flags: StateFlow<RemoteConfigFlags> = _flags.asStateFlow()

    /**
     * Initializes Remote Config settings and default values.
     *
     * @param isDebug If true, sets the minimum fetch interval to 0 for immediate updates during development.
     */
    fun init(isDebug: Boolean) {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = if (isDebug) 0 else 43200 // 12-hour interval for production
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
            .addOnCompleteListener {
                updateFlags()
            }
    }

    /**
     * Fetches and activates the latest configuration from Firebase.
     *
     * @param onComplete Callback invoked with the success status of the fetch operation.
     */
    fun fetch(onComplete: (Boolean) -> Unit = {}) {
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                updateFlags()
                onComplete(task.isSuccessful)
            }
    }

    /**
     * Sets up a listener for real-time configuration updates.
     *
     * Automatically activates updates and triggers the provided callback.
     *
     * @param onRelevantUpdate Callback invoked when a config update is successfully activated.
     */
    fun startRealtimeUpdates(onRelevantUpdate: () -> Unit) {
        remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                Timber.tag("RemoteConfig").d("Updated keys: ${configUpdate.updatedKeys}")

                remoteConfig.activate().addOnCompleteListener { task ->
                    updateFlags()
                    if (task.isSuccessful) {
                        onRelevantUpdate()
                    }
                }
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                Timber.tag("RemoteConfig").w(error, "Config update error: ${error.code}")
            }
        })
    }

    private fun updateFlags() {
        _flags.value = RemoteConfigFlags(
            enableDiscoverHomepage = remoteConfig.getBoolean(Keys.ENABLE_DISCOVER_HOMEPAGE),
            enablePaywall = remoteConfig.getBoolean(Keys.ENABLE_PAYWALL),
            enablePlayrAi = remoteConfig.getBoolean(Keys.ENABLE_PLAYR_AI),
            enableWideSearch = remoteConfig.getBoolean(Keys.ENABLE_WIDE_SEARCH),
            showAds = remoteConfig.getBoolean(Keys.SHOW_ADS),
        )
    }

    /** @return true if the discover section on the homepage is enabled. */
    fun getEnableDiscoverHomepage() = flags.value.enableDiscoverHomepage

    /** @return true if the paywall is enabled. */
    fun getEnablePaywall() = flags.value.enablePaywall

    /** @return true if the Playr AI feature is enabled. */
    fun getEnablePlayrAi() = flags.value.enablePlayrAi

    /** @return true if wide search (searching across more providers) is enabled. */
    fun getEnableWideSearch() = flags.value.enableWideSearch

    /** @return true if ads should be displayed in the app. */
    fun getShowAds() = flags.value.showAds
}
