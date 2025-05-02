package com.example.learndi

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import javax.inject.Singleton

//import jakarta.inject.Singleton

@Singleton
@Database(entities = [Task::class], version = 1)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        private const val NAME = "task_database"

        @Volatile
        private var INSTANCE: TaskDatabase? = null

        /**
         * Provides a singleton instance of the database.
         * Includes migration logic from versions 1 → 3.
         */
        fun getInstance(context: Context): TaskDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskDatabase::class.java,
                    NAME
                )
                    // ✅ Add migration strategies
                    //.addMigrations(MIGRATION_1_2, MIGRATION_2_3)  // Apply both migrations
                    //.fallbackToDestructiveMigration()             // Use for development
                    .build()
                INSTANCE = instance
                instance
            }
        }

    }
}


/*
* package com.example.learndi

import androidx.room.Database
import androidx.room.RoomDatabase
import javax.inject.Singleton

//import jakarta.inject.Singleton

@Singleton
@Database(entities = [Task::class], version = 1)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}

* */