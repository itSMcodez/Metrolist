package com.itsmcodez.justplayr.manager

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.metrolist.music.BuildConfig
import timber.log.Timber

object ConsentManager {
    private var isConsentRequestInFlight = false
    private val pendingCallbacks = mutableListOf<(Boolean) -> Unit>()

    fun canRequestAds(context: Context): Boolean {
        return UserMessagingPlatform.getConsentInformation(context).canRequestAds()
    }

    fun requestConsentIfNeeded(
        context: Context,
        onComplete: (Boolean) -> Unit,
    ) {
        if (canRequestAds(context)) {
            onComplete(true)
            return
        }

        val activity = context.findActivity()
        if (activity == null) {
            onComplete(false)
            return
        }

        pendingCallbacks += onComplete
        if (isConsentRequestInFlight) return

        isConsentRequestInFlight = true

        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        val consentRequestParameters =
            ConsentRequestParameters.Builder()
                .apply {
                    createDebugSettings(activity)?.let(::setConsentDebugSettings)
                }.build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            consentRequestParameters,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Timber.w("Consent form dismissed with error: %s", formError.message)
                    }
                    completePendingCallbacks(consentInformation.canRequestAds())
                }
            },
            { requestConsentError ->
                Timber.w("Consent info update failed: %s", requestConsentError.message)
                completePendingCallbacks(consentInformation.canRequestAds())
            },
        )
    }

    fun isPrivacyOptionsRequired(context: Context): Boolean {
        return UserMessagingPlatform
            .getConsentInformation(context)
            .privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    private fun completePendingCallbacks(canRequestAds: Boolean) {
        isConsentRequestInFlight = false
        val callbacks = pendingCallbacks.toList()
        pendingCallbacks.clear()
        callbacks.forEach { callback ->
            callback(canRequestAds)
        }
    }

    private fun createDebugSettings(context: Context): ConsentDebugSettings? {
        if (!BuildConfig.DEBUG) return null

        val testDeviceHashedId = BuildConfig.UMP_TEST_DEVICE_HASHED_ID.trim()
        val debugGeography = debugGeographyFrom(BuildConfig.UMP_DEBUG_GEOGRAPHY)

        if (testDeviceHashedId.isEmpty() && debugGeography == ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_DISABLED) {
            return null
        }

        return ConsentDebugSettings.Builder(context).apply {
            if (testDeviceHashedId.isNotEmpty()) {
                addTestDeviceHashedId(testDeviceHashedId)
            }
            setDebugGeography(debugGeography)
        }.build()
    }

    private fun debugGeographyFrom(value: String): Int {
        return when (value.trim().uppercase()) {
            "EEA" -> ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA
            "REGULATED_US_STATE" -> ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_REGULATED_US_STATE
            "OTHER" -> ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_OTHER
            else -> ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_DISABLED
        }
    }

    private tailrec fun Context.findActivity(): Activity? {
        return when (this) {
            is Activity -> this
            is ContextWrapper -> baseContext.findActivity()
            else -> null
        }
    }
}
