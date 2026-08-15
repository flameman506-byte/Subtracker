package com.subtracker.app

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class BillingCycle { MONTHLY, YEARLY }

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val cost: Double,
    val billingCycle: BillingCycle,
    val nextRenewalEpochDay: Long, // days since epoch, for scheduling reminders
    val reminderDaysBefore: Int = 3
)
