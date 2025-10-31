package com.dp.jumpster.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [JumpRecord::class, JumpEntry::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun jumpRecordDao(): JumpRecordDao
    abstract fun jumpEntryDao(): JumpEntryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jumpster_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
