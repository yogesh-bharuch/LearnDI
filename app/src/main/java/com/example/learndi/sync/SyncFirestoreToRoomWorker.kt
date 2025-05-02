package com.example.learndi.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.learndi.toMap
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject


class SyncFirestoreToRoomWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    // ✅ Get current Firebase user ID
    //private val currentUserId: String = FirebaseAuth.getInstance().currentUser?.uid ?: "Unknown"

    // ✅ Initialize the repository directly
    private val repository: SyncRepository by lazy { SyncRepository.getInstance(context) }
    // ✅ Read sync parameters from SharedPreferences
    private fun getSyncParams(context: Context): Pair<Long, String> {
        val sharedPreferences = context.getSharedPreferences("SyncPrefs_LearnDi", Context.MODE_PRIVATE)
        val lastSyncTime = sharedPreferences.getLong("LAST_SYNC_TIME", 0L)
        val currentUserId = sharedPreferences.getString("CURRENT_USER_ID", "Unknown") ?: "Unknown"
        return Pair(lastSyncTime, currentUserId)
    }
    override suspend fun doWork(): Result
    {
     /*
        Log.d("FirestoreSync", "🚀 doWork started for SyncLocalToFirestoreWorker")


        return try {
            Log.d("FirestoreSync", "🔎 Fetching tasks to sync from local Room database...")

            // ✅ Use callback to handle the sync result properly
            var resultMessage = "From SyncLocalToFirestoreWorker: Sync started..."

            val callback: (String) -> Unit = { result ->
                resultMessage = result
                //Log.d("FirestoreSync", "🔥 From SyncLocalToFirestoreWorker.dowork: Sync result: $resultMessage")
            }

            // ✅ Trigger the sync with the callback
            repository.syncLocalDataToFirestore(callback)

            // ✅ Prepare WorkManager result data
            val outputData = workDataOf("SYNC_RESULT" to resultMessage)
            Log.d("FirestoreSync", "🔥 From SyncLocalToFirestoreWorker.dowork: Room → Firestore Sync completed: $resultMessage")

            Result.success(outputData)   // ✅ Return success with output data

        } catch (e: Exception) {
            Log.e("FirestoreSync", "❌ Sync failed: ${e.localizedMessage}", e)
            // ✅ Return failure with error message
            val outputData = workDataOf("SYNC_RESULT" to "🔥 From SyncLocalToFirestoreWorker.dowork: Room → Firestore Sync failed: ${e.localizedMessage}")

            Result.retry()  // 🔁 Request retry
        }

    */
        return Result.success() // remove this line when uncomment above block
    }
}
