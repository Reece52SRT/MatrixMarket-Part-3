package com.matrixmarket.app.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Local SQLite database (via Room) that powers offline mode.
@Database(
    entities = [CachedListingEntity::class, PendingListingEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun listingDao(): ListingDao
    abstract fun pendingListingDao(): PendingListingDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "matrix_market.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
