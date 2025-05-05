package com.example.learndi.firestore

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.learndi.Task
import com.example.learndi.toMap
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A service class responsible for handling Firestore and Firebase Storage operations
 * related to Task entities.
 */
@Singleton
class FirestoreTaskService @Inject constructor(
    private val appContext: Context
) {

    // Firebase instances
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // Reference to the Firestore collection
    private val taskCollection = firestore.collection("LearnDI")

    /**
     * Adds a new task to the Firestore collection.
     *
     * @param task The task to be added.
     */
    suspend fun addTask(taskData: Map<String, Any?>): Result<Boolean> {
        return try {
            // Add the task data directly from the map
            taskCollection.document(taskData["id"] as? String ?: "BadData Passed")
                .set(taskData)  // Store the task as a Map in Firestore
            Log.d("Firestore", "✅ From FirestoreTaskServivice.addtask: Task added to Firestore: ${taskData["id"]}")
            Result.success(true)
        } catch (e: Exception) {
            Log.e("Firestore", "❌ From FirestoreTaskServivice.addtask: Failed to add task: ${e.message}")
            Result.success(false)
        }
    }

    suspend fun updateTask(taskId: String, taskData: Map<String, Any?>): Boolean {
        return try {
            taskCollection.document(taskId)
                .update(taskData) // Firestore 'update' (only updates provided fields)
                .await()          // Important: suspend until operation completes
            Log.d("Firestore", "✅ From FirestoreTaskServivice.addtask: Task updated in Firestore: $taskId")
            true  // ✅ Success
        } catch (e: Exception) {
            Log.e("Firestore", "❌ From FirestoreTaskServivice.addtask: Failed to update task: ${e.message}")
            false // ❌ Failure
        }
    }


    /**
     * Uploads an image from a URI to Firebase Storage and returns the download URL.
     *
     * @param uri URI of the image to upload.
     * @param uuid Unique identifier to name the file in storage.
     * @return Download URL of the uploaded image or null on failure.
     */
    suspend fun uploadImageAndGetUrl(uri: Uri, uuid: String): String? {
        return try {
            val imageRef = storage.reference.child("task_images/$uuid")
            imageRef.putFile(uri).await()
            val downloadUrl = imageRef.downloadUrl.await()
            downloadUrl.toString()
        } catch (e: Exception) {
            Log.e("FirestoreTaskService", "❌ Image upload failed: ${e.message}")
            null
        }
    }

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
    suspend fun uploadResizedImageAndGetUrl(uri: Uri, uuid: String, maxWidth: Int, maxHeight: Int): String? {
        return try {
            // Convert URI to InputStream
            val inputStream = appContext.contentResolver.openInputStream(uri)
                ?: throw Exception("Cannot open URI")

            // Decode the InputStream into a Bitmap
            val originalBitmap = BitmapFactory.decodeStream(inputStream)

            // Maintain aspect ratio while resizing
            val aspectRatio = originalBitmap.width.toDouble() / originalBitmap.height
            val newWidth = if (aspectRatio > 1) maxWidth else (maxHeight * aspectRatio).toInt()
            val newHeight = if (aspectRatio > 1) (maxWidth / aspectRatio).toInt() else maxHeight

            // Resize bitmap
            val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)

            // Convert Bitmap to InputStream
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            val resizedInputStream = ByteArrayInputStream(outputStream.toByteArray())

            // Upload resized image
            val imageRef = storage.reference.child("task_images/$uuid")
            imageRef.putStream(resizedInputStream).await()
            val downloadUrl = imageRef.downloadUrl.await()

            downloadUrl.toString()
        } catch (e: Exception) {
            Log.e("FirestoreTaskService", "❌ From FirestoreTaskServivice.uploadResizedImageAndGetUrl: Resized image upload failed: ${e.message}")
            null
        }
    }

    /**
     * Deletes a task and optionally its associated image from Firestore and Storage.
     *
     * @param id The task ID.
     * @param uuid Optional image ID, defaults to task ID.
     * @return True if deletion succeeded, false otherwise.
     */
    suspend fun deleteTask(id: String, uuid: String = ""): Boolean {
        return try {
            // Delete associated image (if exists)
            val imageDeleted = deleteImageFromStorage(id)

            // Delete Firestore document
            val snapshot = taskCollection.whereEqualTo("id", id).get().await()
            for (doc in snapshot.documents) {
                taskCollection.document(doc.id).delete().await()
            }

            Log.d("FirestoreTaskService", "✅ Task deleted: $id, Image deleted: $imageDeleted")
            true
        } catch (e: Exception) {
            Log.e("FirestoreTaskService", "❌ Failed to delete task: ${e.message}")
            false
        }
    }

    /**
     * Deletes an image from Firebase Storage.
     *
     * @param uuid Image UUID (not used directly here).
     * @param id The ID used as image filename.
     * @return True if image deleted, false if it fails or doesn't exist.
     */
    suspend fun deleteImageFromStorage(id: String): Boolean {
        return try {
            val imageRef = storage.reference.child("task_images/$id")
            imageRef.delete().await()
            Log.d("FirestoreTaskService", "🗑️ Image deleted: $id")
            true
        } catch (e: Exception) {
            Log.e("FirestoreTaskService", "❌ Image deletion failed: ${e.message}")
            false
        }
    }

    /**
     * Updates a task in Firestore using its `id` as document reference.
     *
     * @param task The task object with updated fields.
     */
    suspend fun updateTask(task: Task) {
        try {
            taskCollection.document(task.id).update(task.toMap())
            Log.d("Firestore", "🔄 Task updated: ${task.id}")
        } catch (e: Exception) {
            Log.e("Firestore", "❌ Failed to update task: ${e.message}")
        }
    }
}

