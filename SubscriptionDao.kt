package com.subtracker.app

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY nextRenewalEpochDay ASC")
    fun getAll(): Flow<List<Subscription>>

    @Query("SELECT COUNT(*) FROM subscriptions")
    suspend fun count(): Int

    @Insert
    suspend fun insert(subscription: Subscription): Long

    @Update
    suspend fun update(subscription: Subscription)

    @Delete
    suspend fun delete(subscription: Subscription)
}
