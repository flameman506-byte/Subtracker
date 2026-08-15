package com.subtracker.app

/**
 * Central place for the freemium rules. Keeping this in one file makes it easy
 * to tune the paywall (e.g. change the free limit) without hunting through the UI code.
 */
object Entitlements {
    const val FREE_SUBSCRIPTION_LIMIT = 5

    /**
     * Call before letting the user add a new subscription.
     * Returns true if they're allowed to add one (either under the free limit,
     * or they have an active premium purchase).
     */
    fun canAddSubscription(currentCount: Int, isPremium: Boolean): Boolean {
        if (isPremium) return true
        return currentCount < FREE_SUBSCRIPTION_LIMIT
    }

    // Premium-only features — check isPremium before enabling these in the UI:
    // - spending charts / trends
    // - CSV export
    // - home-screen widget
    fun requiresPremium(feature: PremiumFeature, isPremium: Boolean): Boolean {
        return !isPremium
    }
}

enum class PremiumFeature { SPENDING_CHARTS, CSV_EXPORT, WIDGET }
