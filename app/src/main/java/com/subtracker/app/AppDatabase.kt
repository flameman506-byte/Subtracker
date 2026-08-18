package com.subtracker.app

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun fromBillingCycle(value: BillingCycle): String = value.name

    @TypeConverter
    fun toBillingCycle(value: String): BillingCycle = BillingCycle.valueOf(value)

    @TypeConverter
    fun fromCategory(value: Category): String = value.name

    @TypeConverter
    fun toCategory(value: String): Category = Category.valueOf(value)
}

@Database(entities = [Subscription::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subscriptionDao(): SubscriptionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "subtracker.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
