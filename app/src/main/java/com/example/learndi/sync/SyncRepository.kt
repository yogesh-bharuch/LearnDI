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

class SyncRepository (private val taskDao: TaskDao) {
    // Singleton instance
    companion object {
        @Volatile
        private var instance: SyncRepository? = null

        // ✅ Add getInstance() to provide repository singleton access
        fun getInstance(context: Context): SyncRepository {
            return instance ?: synchronized(this) {
                instance ?: SyncRepository(
                    TaskDatabase.getInstance(context).taskDao()
                ).also { instance = it }
            }
        }
    }

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val collectionRef = firestore.collection("LearnDI")

    val tasks: Flow<List<Task>> = taskDao.getAll()

    suspend fun insert(task: Task) {
        taskDao.insert(task)
    }

    suspend fun update(task: Task) {
        taskDao.update(task)
    }

    suspend fun delete(task: Task) {
        taskDao.delete(task)
    }

    suspend fun getMarkAsDeletedTasks(): List<Task> {
        return taskDao.getMarkAsDeletedTasks()
    }

    // 🔄 Tasks that need to be synced (created or updated)
    suspend fun getTasksToSync(): List<Task> {
        return taskDao.getTasksBySyncStatus(
            listOf(SyncStatus.NOT_SYNCED, SyncStatus.UPDATED)
        )
    }
    suspend fun syncLocalDataToFirestore(callback: (String) -> Unit) {
        Log.d("FirestoreSync", "🔥 From SyncRepository.syncLocalDataToFirestore called")

        val messages = mutableListOf<String>()

        try {
            // ✅ Fetch all members mark for deleted members will not fetch
            val unsyncedTasks = withContext(Dispatchers.IO) {
                getTasksToSync()
            }

            // ✅ Fetch soft-deleted members
            val deletedTasks = withContext(Dispatchers.IO) {
                getMarkAsDeletedTasks()
            }

            // ✅ Return if no members to sync
            if (unsyncedTasks.isEmpty() && deletedTasks.isEmpty()) {
                val noSyncMessage = "No members to sync"
                Log.d("FirestoreSync", "🔥 From SyncRepository.syncLocalDataToFirestore $noSyncMessage")
                callback(noSyncMessage)
                return
            }

            // ✅ Delete members marked as deleted
            val deleteTasks = coroutineScope {
                if (deletedTasks.isNotEmpty()) {
                    deletedTasks.map { task ->
                        async { deleteTaskInFirestore(task, messages) }
                    }
                } else emptyList()
            }

           /* // ✅ Remove references to deleted members (parent/spouse links)
            val cleanUpTasks = coroutineScope {
                if (deletedTasks.isNotEmpty()) {
                    deletedTasks.map { member ->
                        async { removeReferencesToDeletedMember(member.id, messages) }
                    }
                } else emptyList()
            }*/

            // ✅ Sync unsynced members to Firestore
            val syncTasks = coroutineScope {
                if (unsyncedTasks.isNotEmpty()) {
                    unsyncedTasks.map { task ->
                        async { updateTaskInFirestore(task, messages) }
                    }
                } else emptyList()
            }

            // ✅ Await all operations
            deleteTasks.awaitAll()
            //cleanUpTasks.awaitAll()
            syncTasks.awaitAll()

            // ✅ Prepare final result message
            val resultMessage = messages.joinToString("\n").ifEmpty { "No members to sync" }
            Log.d("SyncFlow", "🔥 Final Sync Message: $resultMessage")

            // ✅ Trigger the callback with the result
            callback(resultMessage)

        } catch (e: Exception) {
            Log.e("SyncFlow", "❌ Error during sync operation: ${e.message}", e)
            callback("❌ Error during sync: ${e.message}")
        }
    }

    /**
     * ✅ Check if the document exists (documentSnapshot.exists()):
     * If exists, it performs update().
     *If not found, it uses set(), creating a new Firestore entry.
     *Keeps logs and messages clear on whether a task was updated or newly created.
     * Updates the local Room sync status and `updatedAt` timestamp upon successful Firestore sync.
     */

    suspend fun updateTaskInFirestore(task: Task, messages: MutableList<String>): Boolean {
        val currentTime = System.currentTimeMillis()
        var updatedTask = task.copy(updatedAt = currentTime)
        val taskData = updatedTask.toMap()

        return try {
            val documentRef = collectionRef.document(task.id)

            // ✅ Check if document exists first
            val documentSnapshot = documentRef.get().await()

            if (documentSnapshot.exists()) {
                // ✅ Task exists → Update it
                documentRef.update(taskData).await()
                Log.d("Firestore", "✅ From SyncRepository.updateTaskInFirestore: Task updated in Firestore: ${task.id}")
                messages.add("✅ From SyncRepository.updateTaskInFirestore: ${task.firstName} ${task.lastName}")
            } else {
                // 🚀 Task does NOT exist → Create new
                documentRef.set(taskData).await()
                Log.d("Firestore", "🚀 From SyncRepository.updateTaskInFirestore: New task created in Firestore: ${task.id}")
                messages.add("🚀 From SyncRepository.updateTaskInFirestore: Firestore document created: ${task.firstName} ${task.lastName}")
            }

            // ✅ Update local Room sync status and timestamp
            withContext(Dispatchers.IO) {
                updatedTask = task.copy(syncStatus = SyncStatus.SYNCED)
                update(updatedTask)
            }

            true  // ✅ Success
        } catch (e: Exception) {
            Log.e("Firestore", "❌ From SyncRepository.updateTaskInFirestore: Failed to update task: ${e.message}")
            messages.add("❌ From SyncRepository.updateTaskInFirestore: Firestore update failed for ${task.firstName}")
            false // ❌ Failure
        }
    }
    /*suspend fun updateTaskInFirestore1(task: Task, messages: MutableList<String>): Boolean {
        val currentTime = System.currentTimeMillis()
        var updatedTask = task.copy(
            updatedAt = currentTime
        )
        val taskData = updatedTask.toMap()

        return try {
            collectionRef.document(task.id)
                .update(taskData) // Firestore 'update'
                .await()          // Important: suspend until operation completes
            Log.d("Firestore", "✅ From SyncRepository.updateTaskInFirestore: Task updated in Firestore: ${task.id}")
            //Log.d("FirestoreRepo", "✅ Updated in Firestore: ${threeGen.firstName}")
            messages.add("✅ Firestore: ${task.firstName} ${task.lastName}")
            // ✅ Update local Room sync status and timestamp
            withContext(Dispatchers.IO) {
                updatedTask = task.copy(
                    syncStatus = SyncStatus.SYNCED,
                )
                update(updatedTask)
            }

            true  // ✅ Success
        } catch (e: Exception) {
            Log.e("Firestore", "❌ From SyncRepository.updateTaskInFirestore: Failed to update task: ${e.message}")
            false // ❌ Failure
        }
    }*/

    /**
     * Resizes an image from a URI, uploads it to Firebase Storage,
     * and returns its download URL.
     *
     * @param uri Image URI.
     * @param uuid File name for the image in Firebase Storage.
     * @param maxWidth Maximum allowed width.
     * @param maxHeight Maximum allowed height.
     * @return Download URL of the resized image, or null if upload fails.
     */
/*    suspend fun uploadResizedImageAndGetUrl(uri: Uri, uuid: String, maxWidth: Int, maxHeight: Int): String? {
        return try {
            // ✅ Convert URI to InputStream
            context.contentResolver.openInputStream(uri)?.use { inputStream ->

                // ✅ Decode the InputStream into a Bitmap
                val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: throw Exception("Invalid Bitmap")

                // ✅ Maintain aspect ratio while resizing
                val aspectRatio = originalBitmap.width.toDouble() / originalBitmap.height
                val newWidth = if (aspectRatio > 1) maxWidth else (maxHeight * aspectRatio).toInt()
                val newHeight = if (aspectRatio > 1) (maxWidth / aspectRatio).toInt() else maxHeight

                // ✅ Resize the bitmap
                val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)

                // ✅ Convert Bitmap to InputStream
                ByteArrayOutputStream().use { outputStream ->
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    ByteArrayInputStream(outputStream.toByteArray()).use { resizedInputStream ->

                        // ✅ Upload resized image to Firebase Storage
                        val imageRef = storage.reference.child("task_images/$uuid")
                        imageRef.putStream(resizedInputStream).await()

                        // ✅ Retrieve download URL
                        val downloadUrl = imageRef.downloadUrl.await()
                        Log.d("FirestoreTaskService", "✅ Image uploaded successfully: $downloadUrl")
                        return downloadUrl.toString()
                    }
                }
            }
            throw Exception("Failed to open URI InputStream")
        } catch (e: Exception) {
            Log.e("FirestoreTaskService", "❌ Image upload failed: ${e.message}")
            null
        }
    }*/

    /**
     * ✅ If the image exists → Deletes the image first, then deletes the task.
     * If the image does not exist → Logs that it's missing, but still deletes the task.
     */
    private suspend fun deleteTaskInFirestore(task: Task, messages: MutableList<String>) {
        try {
            // Delete associated image (if exists)
            val imageDeleted = deleteImageFromStorage(task.id)
            //If the image does not exist → Logs that it's missing
            if (imageDeleted) {
                Log.d("FirestoreSync", "🔥 From SyncRepository.deleteTaskInFirestore: Image file Deleted in Firestore storage: ${task.id} name: ${task.firstName} ${task.lastName}")
                messages.add("🔥 From SyncRepository.deleteTaskInFirestore: Image file Deleted: ${task.firstName} ${task.lastName}")
            } else {
                Log.d("FirestoreSync", "🔥 Image file not found in Firestore storage: ${task.firstName} ${task.lastName}")
                messages.add("🔥 From SyncRepository.deleteTaskInFirestore: Image file not found: ${task.firstName} ${task.lastName}")
            }

            // ✅ Firestore delete (Always delete the task, regardless of image deletion result)
            collectionRef.document(task.id).delete().await()
            Log.d("FirestoreSync", "✅ From SyncRepository.deleteTaskInFirestore: Task deleted from Firestore: ${task.id}")

            // ✅ Local Room delete
            taskDao.delete(task)
            Log.d("FirestoreSync", "✅ From SyncRepository.deleteTaskInFirestore: Task deleted from Local Room: ${task.id}")

            messages.add("✅ From SyncRepository.deleteTaskInFirestore: Task deleted: ${task.firstName} ${task.lastName}")

        } catch (e: Exception) {
            Log.e("FirestoreSync", "❌ From SyncRepository.deleteTaskInFirestore: Failed to delete task: ${e.message}", e)
            messages.add("❌ From SyncRepository.deleteTaskInFirestore: Failed to delete task: ${task.firstName} ${task.lastName}")
        }
    }

    suspend fun deleteImageFromStorage(id: String): Boolean {
        return try {
            val imageRef = storage.reference.child("task_images/$id")

            // Check if the file exists
            imageRef.metadata.await()

            // If metadata fetch is successful, delete the file
            imageRef.delete().await()
            Log.d("FirestoreSync", "🗑️ From SyncRepository.deleteImageFromStorage: Image deleted: $id")
            true
        } catch (e: Exception) {
            Log.e("FirestoreSync", "❌ From SyncRepository.deleteImageFromStorage: Image deletion failed: ${e.message}")
            false
        }
    }
    /**
     * ✅ Removes references to deleted members
     */
    private suspend fun removeReferencesToDeletedMember(deletedMemberId: String, messages: MutableList<String>) {
        try {
            Log.d("FirestoreRepo", "🔥 Removing references for ID: $deletedMemberId")

            val batch = firestore.batch()

            // ✅ Clear parentID references
            val parentQuery = collectionRef.whereEqualTo("parentID", deletedMemberId).get().await()
            parentQuery.documents.forEach { doc ->
                batch.update(doc.reference, "parentID", null)
            }

            // ✅ Clear spouseID references
            val spouseQuery = collectionRef.whereEqualTo("spouseID", deletedMemberId).get().await()
            spouseQuery.documents.forEach { doc ->
                batch.update(doc.reference, "spouseID", null)
            }

            // ✅ Commit batch updates
            batch.commit().await()
            messages.add("✅ References removed for ID: $deletedMemberId")
            Log.d("FirestoreRepo", "✅ References removed for ID: $deletedMemberId")

        } catch (e: Exception) {
            Log.e("FirestoreRepo", "❌ Failed to remove references: ${e.message}", e)
            messages.add("❌ Failed: $deletedMemberId")
        }
    }

}
