package com.subtracker.app

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Wraps Google Play Billing for the premium subscription.
 *
 * Setup steps (do these in the Play Console, not in code):
 * 1. Create the app listing in Google Play Console.
 * 2. Under Monetize > Products > Subscriptions, create a subscription product
 *    with id "premium_monthly" and "premium_yearly" (base plans/offers as needed).
 * 3. Set your prices there (e.g. $2.99/mo, $19.99/yr).
 */
class BillingManager(context: Context) {
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { handlePurchase(it) }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    fun startConnection(onReady: () -> Unit) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryExistingPurchases()
                    onReady()
                }
            }
            override fun onBillingServiceDisconnected() {
                // Retry logic recommended here for production.
            }
        })
    }

    fun launchPurchaseFlow(activity: Activity, productId: String) {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
                productDetailsList.isNotEmpty()
            ) {
                val productDetails = productDetailsList[0]
                val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
                if (offerToken != null) {
                    val flowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(
                            listOf(
                                BillingFlowParams.ProductDetailsParams.newBuilder()
                                    .setProductDetails(productDetails)
                                    .setOfferToken(offerToken)
                                    .build()
                            )
                        ).build()
                    billingClient.launchBillingFlow(activity, flowParams)
                }
            }
        }
    }

    private fun queryExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        billingClient.queryPurchasesAsync(params) { _, purchases ->
            _isPremium.value = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val ackParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(ackParams) { }
            }
            _isPremium.value = true
        }
    }
}
