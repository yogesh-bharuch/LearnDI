package com.example.learndi.hiltsync

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.learndi.TaskRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await

@HiltWorker
class SyncWorker1 @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repo: TaskRepository,
    private val storage: FirebaseStorage,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val tasks = repo.getTasksToSync()
        tasks.forEach { task ->
            val imageUrl = task.imageUri?.let { uri ->
                val fileRef = storage.reference.child("task_images/${task.id}.jpg")
                fileRef.putFile(Uri.parse(uri)).await()
                fileRef.downloadUrl.await().toString()
            }

            val taskData = hashMapOf(
                "id" to task.id,
                "title" to task.title,
                "imageUrl" to imageUrl
            )

            firestore.collection("LearnDI").document(task.id).set(taskData)
        }

        return Result.success()
    }
}
