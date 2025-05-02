package com.example.core.firebase

import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.Query

class FirebaseConnection {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }

    /** ----------------------------- FIRESTORE ----------------------------- **/

    suspend fun getDocument(
        collectionPath: String,
        documentId: String
    ): Map<String, Any>? = try {
        val snapshot = firestore.collection(collectionPath).document(documentId).get().await()
        snapshot.data
    } catch (e: Exception) {
        Log.e("FirebaseConnection", "Error fetching document: ${e.message}", e)
        null
    }

    suspend fun getCollection(
        collectionPath: String,
        filters: Map<String, Any> = emptyMap(),
        orderBy: String? = null,
        startAfter: Any? = null
    ): List<Map<String, Any>> = try {
        var query: Query = firestore.collection(collectionPath)

        filters.forEach { (key, value) -> query = query.whereEqualTo(key, value) }
        orderBy?.let { query = query.orderBy(it) }
        startAfter?.let { query = query.startAfter(it) }

        query.get().await().documents.mapNotNull { it.data }
    } catch (e: Exception) {
        Log.e("FirebaseConnection", "Error fetching collection: ${e.message}", e)
        emptyList()
    }


    suspend fun saveDocument(
        collectionPath: String,
        data: Map<String, Any>,
        documentId: String? = null
    ): Boolean = try {
        val collectionRef = firestore.collection(collectionPath)

        if (documentId != null) {
            collectionRef.document(documentId).set(data).await()
        } else {
            collectionRef.add(data).await() // ✅ Only use on CollectionReference
        }
        true
    } catch (e: Exception) {
        Log.e("FirebaseConnection", "Error saving document: ${e.message}", e)
        false
    }


    /** ----------------------------- STORAGE ----------------------------- **/

    suspend fun uploadImage(
        folderPath: String,
        fileName: String,
        imageUri: Uri
    ): String? = try {
        val ref = storage.reference.child("$folderPath/$fileName")
        ref.putFile(imageUri).await()
        ref.downloadUrl.await().toString()
    } catch (e: Exception) {
        Log.e("FirebaseConnection", "Error uploading image: ${e.message}", e)
        null
    }

    suspend fun downloadUrlFromGsUri(gsUri: String): String? = try {
        val path = gsUri.removePrefix("gs://")
        val ref = storage.getReference(path)
        ref.downloadUrl.await().toString()
    } catch (e: Exception) {
        Log.e("FirebaseConnection", "Error converting gsUri: ${e.message}", e)
        null
    }

    suspend fun deleteFileByUrl(fileUrl: String): Boolean = try {
        val ref = storage.getReferenceFromUrl(fileUrl)
        ref.delete().await()
        true
    } catch (e: Exception) {
        Log.e("FirebaseConnection", "Error deleting file: ${e.message}", e)
        false
    }
}
