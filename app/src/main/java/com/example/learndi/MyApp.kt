package com.example.learndi

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.room.Room
import androidx.work.*
import com.example.learndi.firestore.FirestoreSyncWorker
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject


/**
 * Custom Application class for initializing Firebase and setting up
 * WorkManager with Hilt support.
 */

@HiltAndroidApp
class MyApp : Application(), Configuration.Provider {

    companion object {
        lateinit var instance: MyApp
            private set

        lateinit var taskDatabase: TaskDatabase
            private set
    }

    // Inject the HiltWorkerFactory to support dependency injection in Workers
    @Inject
    lateinit var workerFactory: HiltWorkerFactory


/**
     * Provide custom WorkManager configuration to support Hilt Workers.
     */

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        Log.d("MyApp", "Initializing Room database...")

        instance = this

        // Initialize Room database
        taskDatabase = Room.databaseBuilder(
            applicationContext,
            TaskDatabase::class.java,
            "task_database"
        )
            .fallbackToDestructiveMigration()
            .build()
        Log.d("MyApp", "Room database initialized successfully.")

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        // Enable Firestore offline persistence
        val firestore = FirebaseFirestore.getInstance()

        // Schedule periodic background sync with Firestore
        //scheduleSyncWorker()
    }


/**
     * Schedule a periodic sync worker that runs every 15 minutes.
     * It only runs when the device is connected to the internet.
     */

    private fun scheduleSyncWorker() {
        Log.d("FirestoreSyncWorker", "MyApp.scheduleSyncWorker: Worker is scheduled and running.")

        val syncRequest = PeriodicWorkRequestBuilder<FirestoreSyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "taskSyncWorker",
            ExistingPeriodicWorkPolicy.KEEP, // Keeps existing work if already scheduled
            syncRequest
        )
    }
}



































/*package com.example.learndi

import android.app.Application
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

*//**
 * Custom Application class that initializes Firebase and sets up Hilt.
 *//*
@HiltAndroidApp
class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
    }
}*/
