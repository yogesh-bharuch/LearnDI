package com.example.learndi

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learndi.firestore.FirestoreTaskService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel responsible for handling task-related business logic.
 * Synchronizes Room database operations with Firestore via FirestoreTaskService.
 */
@HiltViewModel
class TaskViewModel @Inject constructor(
    private val repository: TaskRepository,             // Local Room database repository
    private val firestoreService: FirestoreTaskService,  // Firestore sync service
    @ApplicationContext private val context: Context     // Application Context injected via Hilt
) : ViewModel() {

    // Reactive flow of tasks from the Room database
    val tasks = repository.tasks.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(), // Keeps flow active only when observed
        emptyList()                      // Default empty list
    )

    /**
     * Adds a task using an externally provided task object and map (used when you want custom fields).
     * @param taskData Map containing task fields
     * @param task Full Task object
     */
    suspend fun addTask(taskData: Map<String, Any?>, task: Task): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            var returnUri: String? = null

            try {
                // upload image
                try {
                    Log.d("Firestore", "🔄 From ViewModel.AddTask:  Uploading (resized) image...")
                    returnUri = firestoreService.uploadResizedImageAndGetUrl(Uri.parse(task.imageUri ?: ""), task.id, 500, 500)
                } catch (e: Exception) {
                    Log.e("Firestore", "❌ From ViewModel.AddTask: Upload failed: ${e.message}")
                    return@withContext Result.failure(Exception("From ViewModel.AddTask: Image upload failed ..."))
                }
                if (returnUri == null) {
                    Log.e("Firestore", "❌ From ViewModel.AddTask: Image upload failed, returning failure.")
                    return@withContext Result.failure(Exception("Image upload failed"))
                }

                // ✅ Image upload successful → Now update the task data with the new image URL
                val updatedTaskData = taskData.toMutableMap().apply {
                    Log.d("Firestore", "✅ From ViewModel.AddTask: Image uploaded successfully!")
                    put("imageUri", returnUri)
                }

                // ✅ Upload the task document
                var firestoreSuccess = false
                try {
                    Log.d("Firestore", "🔄 From ViewModel.AddTask: Uploading task document...")
                    val result = firestoreService.addTask(updatedTaskData)

                    if (result.isSuccess) {
                        Log.d("Firestore", "✅ From ViewModel.AddTask: Task document uploaded successfully!")
                        firestoreSuccess = true
                    }
                } catch (e: Exception) {
                    Log.e("Firestore", "❌ From ViewModel.AddTask: Task document upload failed: ${e.message}")
                }

                // ❌ If task document upload fails, delete the previously uploaded image to prevent orphaned files
                if (!firestoreSuccess) {
                    Log.e("Firestore", "❌ From ViewModel.AddTask: Task document upload failed, deleting uploaded image")
                    val resultImageDeleted = firestoreService.deleteImageFromStorage(task.id)
                    if (resultImageDeleted) {
                        Log.d("Firestore", "❌ From ViewModel.AddTask: deleted uploaded image as Task document upload failed")}
                    return@withContext Result.failure(Exception("Document Task upload failed"))
                }

                // ✅ Firestore task document successfully uploaded → Now insert task into local Room database
                val updatedTask = task.copy(imageUri = returnUri)
                val resultLocalDatabaseUpdated = repository.insert(updatedTask)

                // ✅ Everything succeeded → Return success
                Log.d("Firestore", "✅ Task inserted into Local Database: ${updatedTask.id}")
                return@withContext Result.success(true)
            } catch (e: Exception) {
                Log.e("Firestore", "❌ Unexpected error: ${e.message}")
                //return@withContext Result.failure(e)
                return@withContext Result.failure(Exception("Failed to insert task into local database"))
            }
        }
    }
    /*
    fun addTask1(taskData: Map<String, Any?>, task: Task) {
        viewModelScope.launch {
            // Upload and replace the image URI
            val uriToUrl = task.imageUri?.let { Uri.parse(it) } ?: Uri.EMPTY // // uriToUrl = Uri.parse("https://example.com/my-image")
            // upload image file (Image URI) + task.id (File name), with bitmap size reduced pixels in Firebase Storage.
            val returnUri = firestoreService.uploadResizedImageAndGetUrl(uriToUrl, task.id, 500, 500)
            delay(1000)

            //Image upload in firestore success than only inserts task in firestore followed by inserts in local room database
            if (returnUri != null) {
                // Update task data map with new image URI before sending to Firestore
                val updatedTaskData = taskData.toMutableMap().apply {
                    put("imageUri", returnUri)
                }
                val result = firestoreService.addTask(updatedTaskData)
                Log.d("Firestore","From ViewModel.AddTask: Uploaded (resized) image URL = $returnUri")

                    //if Task update in firestore success than only inserts in local room
                    if (result != null) {
                        Log.d("Firestore","From ViewModel.AddTask: inserted task in firestore = ${updatedTaskData["id"]}")
                        val updatedTask = task.copy(imageUri = returnUri) // Update the task locally with new imageUri "https:"
                        val resultLocalDatabaseUpdated = repository.insert(updatedTask)
                        Log.d("Firestore","From ViewModel.AddTask: inserted task in Local Database = ${updatedTask.id}")
                    } else {
                        // delete uploaded image
                        firestoreService.deleteImageFromStorage(updatedTaskData["id"].toString())
                    }

            } else {
                Log.e("Firestore", "❌ Firestore upload failed for Image & task hence not inserted in local database may be internet not available")
            }
        }
    }
    */

    /**
     * Toggles a task's completion status and updates Room + Firestore.
     *
     * @param task Task to toggle
     */
    fun toggleDone(task: Task) = viewModelScope.launch {
       // val updatedTask = task.copy(isDone = !task.isDone, firstName = "hello" + task.firstName  )
        val updatedTask = task.copy(isDone = !task.isDone)
        update(updatedTask) // Reuse update()
    }

    /**
     * Updates a task in Room and Firestore.
     *
     * @param task Task object with changes
     */
    fun update(task: Task) = viewModelScope.launch {
        val updatedTask = task.copy(syncStatus = SyncStatus.UPDATED) // Mark locally as UPDATED
        repository.update(updatedTask)

        val taskData = updatedTask.toMap() // No manual changes — keeps syncStatus as it is
        val result = firestoreService.updateTask(updatedTask.id, taskData)
        if (result) {
            Toast.makeText(context, "Task updated successfully!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Failed to update task.", Toast.LENGTH_SHORT).show()
        } // toast message

        //triggerImmediateSync(context) // (optional) for immediate sync triggering
    }

    /**
     * Deletes a task from both Room and Firestore.
     *
     * @param task Task to delete
     */
    fun delete(task: Task) = viewModelScope.launch {
        val resultDeleted = firestoreService.deleteTask(task.id)

        if (resultDeleted) {
            repository.delete(task)  // Delete locally only if Firestore delete succeeds
        } else {
            Log.d("Firestore", "Firestore delete failed, not deleting from Room")
        }
        //triggerImmediateSync(context)
    }
}
