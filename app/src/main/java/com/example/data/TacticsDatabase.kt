package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MatchHistory::class, SavedGame::class], version = 1, exportSchema = false)
abstract class TacticsDatabase : RoomDatabase() {
    abstract fun tacticsDao(): TacticsDao

    companion object {
        @Volatile
        private var INSTANCE: TacticsDatabase? = null

        fun getDatabase(context: Context): TacticsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TacticsDatabase::class.java,
                    "tactics_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
