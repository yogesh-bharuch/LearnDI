package com.example.learndi.sync

import com.example.learndi.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class FirestoreSyncTaskService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    suspend fun uploadProfileImage(id: String): String? {
        val imageRef = storage.reference.child("profile_images/$id.jpg")
        return try {
            imageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun uploadTask(task: Task) {
        firestore.collection("tasks").document(task.id).set(task).await()
    }
}