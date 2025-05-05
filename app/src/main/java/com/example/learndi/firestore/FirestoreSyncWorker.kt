package com.example.learndi.firestore

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.learndi.SyncStatus
import com.example.learndi.TaskRepository
import com.example.learndi.toMap
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class FirestoreSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: TaskRepository,
    private val firestoreService: FirestoreTaskService
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("FirestoreSyncWorker", "Running worker...") // Log added here
        return try {
            val tasksToSync = repository.getTasksToSync()
            tasksToSync.forEach { task ->
                if (task.deleted) {
                    Log.d("FirestoreSyncWorker", "Deleting task: ${task.id}")
                    firestoreService.deleteTask(task.id)
                    repository.delete(task) // Actually remove it from Room
                } else {
                    val taskData = task.toMap()
                    when (task.syncStatus) {
                        SyncStatus.NOT_SYNCED -> {
                            Log.d("FirestoreSyncWorker", "Adding task: ${task.id}")
                            // Convert Task to map and pass it to addTask

                            val result = firestoreService.addTask(taskData)

                        }
                        SyncStatus.UPDATED -> {
                            Log.d("FirestoreSyncWorker", "Updating task: ${task.id}")
                            firestoreService.updateTask(task.id, taskData)
                        }
                        else -> { /* no-op */ }
                    }
                    repository.update(task.copy(syncStatus = SyncStatus.SYNCED))
                }
            }
            return Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Failed to sync tasks", e)
            return Result.retry()
        }
    }
}
