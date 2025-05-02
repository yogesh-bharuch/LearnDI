package com.example.learndi.sync

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * ✅ WorkManagerHelper manages scheduling and executing sync jobs.
 * - Chained Sync: Ensures local → Firestore sync runs before Firestore → Room sync.
 * - Immediate Sync: Runs a one-time sync with exponential backoff.
 * - Periodic Sync: Runs periodically in the background (even when the app is closed).
 * - Observation: Monitors the sync status using LiveData and logs the result.
 * - Tags and Constraints: Adds tags for easy tracking and constraints for reliability.
 */
object WorkManagerHelper {

    object WorkTags {
        const val LOCAL_TO_FIRESTORE = "LocalToFirestore_LearnDi"
        const val FIRESTORE_TO_ROOM = "FirestoreToRoom_LearnDi"
        const val CHAINED_SYNC = "ChainedSync_LearnDi"
        const val PERIODIC_LOCAL_TO_FIRESTORE = "PeriodicLocalToFirestore_LearnDi"
        const val PERIODIC_FIRESTORE_TO_ROOM = "PeriodicFirestoreToRoom_LearnDi"
    }

    object WorkNames {
        const val CHAINED_SYNC = "ChainedSync_LearnDi"
        const val PERIODIC_SYNC_LOCAL = "PeriodicSyncLocalToFirestore_LearnDi"
        const val PERIODIC_SYNC_REMOTE = "PeriodicSyncFirestoreToRoom_LearnDi"
    }

    /**
     * ✅ Store lastSyncTime and currentUserId in SharedPreferences.
     */
    private fun saveSyncParams(context: Context, lastSyncTime: Long, currentUserId: String) {
        val sharedPreferences = context.getSharedPreferences("SyncPrefsLearnDi", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putLong("LAST_SYNC_TIME", lastSyncTime)
            putString("CURRENT_USER_ID", currentUserId)
            apply()
        }
        //Log.d("FirestoreSync", "✅ From WorkManagerHelper: Saved Sync Params → lastSyncTime: $lastSyncTime, currentUserId: $currentUserId")
    }

    /**
     * ✅ Chain Sync for App Startup:
     * 1. Local → Firestore
     * 2. Firestore → Room (only after local → Firestore is complete)
     */
    //fun chainSyncOnStartup(context: Context, lastSyncTime: Long, currentUserId: String) {
    fun chainSyncOnStartup(context: Context, lastSyncTime: Long, currentUserId: String) {
        Log.d("FirestoreSync", "🔥 From WorkManagerHelper.chainSyncOnStartup")
        val workManager = WorkManager.getInstance(context)
        // ✅ Save sync parameters to SharedPreferences
        saveSyncParams(context, lastSyncTime, currentUserId)

        // 🔥 Step 1: Local → Firestore Sync
        val syncLocalToFirestoreWork = OneTimeWorkRequestBuilder<SyncLocalToFirestoreWorker>()
            .addTag(WorkTags.LOCAL_TO_FIRESTORE)   // Add tag for easy tracking
            .build()

        // 🔥 Step 2: Firestore → Room Sync
        val syncFirestoreToRoomWork = OneTimeWorkRequestBuilder<SyncFirestoreToRoomWorker>()
            .addTag(WorkTags.FIRESTORE_TO_ROOM)        // Add tag for tracking
            .build()


        // ✅ Chain Execution
        // First: Local → Firestore, Then: Firestore → Room, Enqueue the chain
        workManager.beginUniqueWork(
            WorkNames.CHAINED_SYNC,
            ExistingWorkPolicy.REPLACE,
            syncLocalToFirestoreWork
        ).then(syncFirestoreToRoomWork)
            .enqueue()
        Log.d("WorkManagerHelper", "🔥 From WorkManagerHelper.chainSyncOnStartup: (One time) Chained sync jobs enqueued on app startup \n  Sync Params: lastSyncTime: , currentUserId:  \n  With lastSyncTime= , currentUserId= ")
    }

    /**
     * ✅ Immediate One-Time Sync: Local → Firestore
     * - Triggered when user modifies data
     */
    fun immediateSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)    // Only sync with network
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncLocalToFirestoreWorker>()  // Correct Worker!
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,                   // Retry with exponential backoff
                30, TimeUnit.SECONDS                         // Retry delay of 30 seconds
            )
            .addTag("ImmediateSync_LearnDi")                         // Add tag for tracking
            .build()

        val workManager = WorkManager.getInstance(context)

        // ✅ Enqueue unique work to avoid duplication
        workManager.enqueueUniqueWork(
            "ImmediateSync_LearnDi",
            ExistingWorkPolicy.REPLACE,      // Replace existing immediate sync if it exists
            syncRequest
        )
        Log.d("WorkManagerHelper", "🔥 From WorkManagerHelper.immediateSync: Immediate sync issued local->firestore")
    }

    /**
     * ✅ Periodic Sync: Runs in the background (even if the app is closed)
     * - Syncs Local → Firestore → Room in a chain.
     */
    fun schedulePeriodicSync(
        context: Context,
        timeIntervalInMinutes: Long = 720,
        lastSyncTime: Long = 0L,
        currentUserId: String = "Yogesh"
    ) {
        val workManager = WorkManager.getInstance(context)
        // ✅ Store the sync parameters in SharedPreferences
        val currentTime = System.currentTimeMillis()
        saveSyncParams(context, currentTime, currentUserId)

        // ✅ Local → Firestore (first in chain)
        val localToFirestoreWork = PeriodicWorkRequestBuilder<SyncLocalToFirestoreWorker>(timeIntervalInMinutes, TimeUnit.MINUTES)
            .addTag(WorkTags.PERIODIC_LOCAL_TO_FIRESTORE)
            .build()

        // ✅ Firestore → Room (second in chain)
        val firestoreToRoomWork = PeriodicWorkRequestBuilder<SyncFirestoreToRoomWorker>(timeIntervalInMinutes, TimeUnit.MINUTES)
           // .setInputData(inputData)                   // Pass sync time and user ID
            .addTag(WorkTags.PERIODIC_FIRESTORE_TO_ROOM)
            .build()

        // 🔥 Schedule the workers separately (no chaining allowed with periodic work)
        workManager.enqueueUniquePeriodicWork(
            WorkTags.PERIODIC_LOCAL_TO_FIRESTORE,
            ExistingPeriodicWorkPolicy.KEEP,      // Prevent duplication
            localToFirestoreWork
        )

        workManager.enqueueUniquePeriodicWork(
            WorkTags.PERIODIC_FIRESTORE_TO_ROOM,
            ExistingPeriodicWorkPolicy.KEEP,      // Prevent duplication
            firestoreToRoomWork
        )

        Log.d("FirestoreSync", "🔥 From WorkManagerHelper.schedulePeriodicSync : chain scheduled every $timeIntervalInMinutes minutes")
    }

    /**
     * ✅ Observes the sync result and logs it.
     * - Monitors both Immediate and Periodic Sync results
     */
    fun observeSyncResult(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        onResult: (String) -> Unit   // Callback for sync result
    ) {
        val workManager = WorkManager.getInstance(context)

        // ✅ Observe immediate sync results
        workManager.getWorkInfosByTagLiveData("ImmediateSync")
            .observe(lifecycleOwner) { workInfos ->
                for (workInfo in workInfos) {
                    if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                        val resultMessage = workInfo.outputData.getString("SYNC_RESULT")
                            ?: "Immediate Sync completed"
                        Log.d("FirestoreSync", "🔥 From workManager.getWorkInfosByTagLiveData: Immediate Sync Result: $resultMessage")
                        onResult(resultMessage)

                        // ✅ Clear completed work after logging the message
                        workManager.pruneWork()    // Removes completed/cancelled work from WorkManager DB
                    }
                }
            }

        // ✅ Observe periodic sync results
        workManager.getWorkInfosByTagLiveData(WorkTags.PERIODIC_LOCAL_TO_FIRESTORE)
            .observe(lifecycleOwner) { workInfos ->
                for (workInfo in workInfos) {
                    if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                        val resultMessage = workInfo.outputData.getString("SYNC_RESULT")
                            ?: "Periodic Sync Local → Firestore completed"
                        Log.d("FirestoreSync", "🔥 Periodic Local → Firestore Sync Result: $resultMessage")
                        onResult(resultMessage)

                        // ✅ Clear completed work
                        workManager.pruneWork()
                    }
                }
            }

        workManager.getWorkInfosByTagLiveData(WorkTags.PERIODIC_FIRESTORE_TO_ROOM)
            .observe(lifecycleOwner) { workInfos ->
                for (workInfo in workInfos) {
                    if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                        val resultMessage = workInfo.outputData.getString("SYNC_RESULT")
                            ?: "Periodic Sync Firestore → Room completed"
                        Log.d("FirestoreSync", "🔥 From workManager.getWorkInfosByTagLiveData: Periodic Firestore → Room Sync Result: $resultMessage")
                        onResult(resultMessage)
                        // ✅ Clear completed work
                        workManager.pruneWork()
                    }
                }
            }
    }

}
