package com.itsmcodez.justplayr.manager

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.purchaseWith
import com.revenuecat.purchases.restorePurchasesWith
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

@Immutable
data class SubscriptionUiState(
    val customerInfo: CustomerInfo? = null,
    val offerings: Offerings? = null,
    val isCustomerInfoLoading: Boolean = false,
    val isOfferingsLoading: Boolean = false,
    val lastError: PurchasesError? = null,
) {
    val isLoading: Boolean = isCustomerInfoLoading || isOfferingsLoading
    val isPaywallAvailable: Boolean = offerings != null && (
        offerings.current != null ||
        offerings.getOffering(SubscriptionManager.OFFERING_TEST_DEFAULT) != null ||
        offerings.getOffering(SubscriptionManager.OFFERING_JUSTPLAYR_DEFAULT) != null
    )
}

class SubscriptionManager private constructor(
    context: Context,
) {
    private var hasRegisteredCustomerInfoListener = false

    var uiState by mutableStateOf(SubscriptionUiState())
        private set

    val subscriptionUiState: SubscriptionUiState
        get() = uiState

    val customerInfo: CustomerInfo?
        get() = uiState.customerInfo

    val offerings: Offerings?
        get() = uiState.offerings

    val isConfigured: Boolean
        get() = Purchases.isConfigured

    val isPro: Boolean
        get() = hasProEntitlement(uiState.customerInfo)

    init {
        if (!isConfigured) {
            Timber.tag(TAG).w("RevenueCat is not configured. Subscription features are disabled.")
        } else {
            registerCustomerInfoListenerIfNeeded()
            refreshAll()
        }
    }

    fun refreshAll() {
        refreshCustomerInfo()
        refreshOfferings()
    }

    fun refreshCustomerInfo() {
        if (!isConfigured) return

        uiState = uiState.copy(isCustomerInfoLoading = true, lastError = null)
        Purchases.sharedInstance.getCustomerInfoWith(
            onError = { error ->
                Timber.tag(TAG).e("Error fetching customer info: ${error.message}")
                uiState = uiState.copy(
                    isCustomerInfoLoading = false,
                    lastError = error,
                )
            },
            onSuccess = { info ->
                updateCustomerInfo(info)
                uiState = uiState.copy(
                    isCustomerInfoLoading = false,
                    lastError = null,
                )
            },
        )
    }

    fun refreshOfferings() {
        if (!isConfigured) return

        uiState = uiState.copy(isOfferingsLoading = true, lastError = null)
        Purchases.sharedInstance.getOfferingsWith(
            onError = { error ->
                Timber.tag(TAG).e("Error fetching offerings: ${error.message}")
                uiState = uiState.copy(
                    isOfferingsLoading = false,
                    lastError = error,
                )
            },
            onSuccess = { newOfferings ->
                uiState = uiState.copy(
                    offerings = newOfferings,
                    isOfferingsLoading = false,
                    lastError = null,
                )
            },
        )
    }

    fun purchase(
        activity: Activity,
        packageToPurchase: Package,
        onResult: (Boolean, PurchasesError?) -> Unit,
    ) {
        if (!isConfigured) {
            onResult(false, null)
            return
        }

        Purchases.sharedInstance.purchaseWith(
            PurchaseParams.Builder(activity, packageToPurchase).build(),
            onError = { error, userCancelled ->
                if (userCancelled) {
                    Timber.tag(TAG).d("User cancelled purchase.")
                    onResult(false, null)
                } else {
                    Timber.tag(TAG).e("Purchase error: ${error.message}")
                    uiState = uiState.copy(lastError = error)
                    onResult(false, error)
                }
            },
            onSuccess = { _, info ->
                updateCustomerInfo(info)
                onResult(hasProEntitlement(info), null)
            },
        )
    }

    fun restorePurchases(onResult: (Boolean, PurchasesError?) -> Unit) {
        if (!isConfigured) {
            onResult(false, null)
            return
        }

        Purchases.sharedInstance.restorePurchasesWith(
            onError = { error ->
                Timber.tag(TAG).e("Restore error: ${error.message}")
                uiState = uiState.copy(lastError = error)
                onResult(false, error)
            },
            onSuccess = { info ->
                updateCustomerInfo(info)
                onResult(hasProEntitlement(info), null)
            },
        )
    }

    private fun registerCustomerInfoListenerIfNeeded() {
        if (hasRegisteredCustomerInfoListener) return
        Purchases.sharedInstance.updatedCustomerInfoListener = { info ->
            updateCustomerInfo(info)
        }
        hasRegisteredCustomerInfoListener = true
    }

    private fun updateCustomerInfo(info: CustomerInfo?) {
        proAccess.set(hasProEntitlement(info))
        hasResolvedCustomerStatus.set(true)
        uiState = uiState.copy(customerInfo = info)
    }

    companion object {
        private const val TAG = "SubscriptionManager"
        const val PRO_ENTITLEMENT_ID = "JustPlayr Pro"
        const val OFFERING_TEST_DEFAULT = "default"
        const val OFFERING_JUSTPLAYR_DEFAULT = "justplayr_default"

        @Volatile
        private var instance: SubscriptionManager? = null

        private val proAccess = AtomicBoolean(false)
        private val hasResolvedCustomerStatus = AtomicBoolean(false)

        fun getInstance(context: Context): SubscriptionManager {
            return instance ?: synchronized(this) {
                instance ?: SubscriptionManager(context).also { instance = it }
            }
        }

        fun hasProAccess(): Boolean = proAccess.get()

        fun shouldDisableAds(): Boolean =
            !RemoteConfigManager.getShowAds() || !hasResolvedCustomerStatus.get() || hasProAccess()

        private fun hasProEntitlement(info: CustomerInfo?): Boolean {
            return info?.entitlements?.get(PRO_ENTITLEMENT_ID)?.isActive == true
        }
    }
}

val LocalSubscriptionManager = staticCompositionLocalOf<SubscriptionManager> {
    error("No SubscriptionManager provided")
}

@Composable
fun SubscriptionProvider(
    context: Context,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalSubscriptionManager provides SubscriptionManager.getInstance(context.applicationContext),
        content = content,
    )
}
