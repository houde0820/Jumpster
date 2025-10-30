package com.dp.jumpster.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [JumpRecord::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun jumpRecordDao(): JumpRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jumpster_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
