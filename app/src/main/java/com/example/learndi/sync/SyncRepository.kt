package com.example.learndi.sync

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.learndi.SyncStatus
import com.example.learndi.Task
import com.example.learndi.TaskDao
import com.example.learndi.TaskDatabase
import com.example.learndi.firestore.FirestoreTaskService
import com.example.learndi.toMap
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class SyncRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val firestoreService: FirestoreSyncTaskService
) {
    val tasks: Flow<List<Task>> = taskDao.getAll()

    // 🔄 Tasks that need to be synced (created or updated)
    fun getUnsyncedTasks() = taskDao.getUnsyncedTasks()

    suspend fun syncTasksToFirestore() {
        Log.d("SyncRepository", "🚀 Sync process started!")

        getUnsyncedTasks().collect { tasks ->
            Log.d("SyncRepository", "🔎 Found ${tasks.size} tasks to sync.")

            tasks.forEach { task ->
                Log.d("SyncRepository", "⏳ Processing task ID: ${task.id}")

                val imageUrl = task.imageUri ?: run {
                    Log.d("SyncRepository", "📤 Uploading profile image for task ID: ${task.id}")
                    firestoreService.uploadProfileImage(task.id)
                }

                Log.d("SyncRepository", "✅ Image URL: $imageUrl")

                firestoreService.uploadTask(task.copy(imageUri = imageUrl))
                Log.d("SyncRepository", "☁ Task uploaded to Firestore: ${task.id}")

                taskDao.update(task.copy(syncStatus = SyncStatus.SYNCED))
                Log.d("SyncRepository", "💾 Updated local Room DB sync status for task ID: ${task.id}")
            }
        }

        Log.d("SyncRepository", "🎉 Sync process completed successfully!")
    }
}

/*
package com.example.learndi.sync

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.learndi.SyncStatus
import com.example.learndi.Task
import com.example.learndi.TaskDao
import com.example.learndi.TaskDatabase
import com.example.learndi.firestore.FirestoreTaskService
import com.example.learndi.toMap
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val firestoreService: FirestoreSyncTaskService
) {
    val tasks: Flow<List<Task>> = taskDao.getAll()

    // 🔄 Tasks that need to be synced (created or updated)
    fun getUnsyncedTasks() = taskDao.getUnsyncedTasks()

    suspend fun syncTasksToFirestore() {
        getUnsyncedTasks().collect { tasks ->
            tasks.forEach { task ->
                val imageUrl = task.imageUri ?: firestoreService.uploadProfileImage(task.id)
                firestoreService.uploadTask(task.copy(imageUri = imageUrl))
                taskDao.update(task.copy(syncStatus = SyncStatus.SYNCED))
            }
        }
    }
}
*/
