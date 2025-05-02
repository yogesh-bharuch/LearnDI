package com.example.learndi.firestore


import android.content.Context
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.learndi.firestore.FirestoreSyncWorker

fun triggerImmediateSync(context: Context,) {
    val request = OneTimeWorkRequestBuilder<FirestoreSyncWorker>().build()
    WorkManager.getInstance(context).enqueue(request)
}
fun triggerFirestoreSync(context: Context) {
    Log.d("FirestoreSyncWorker", "work request")
    val workRequest = OneTimeWorkRequestBuilder<FirestoreSyncWorker>().build()
    WorkManager.getInstance(context).enqueue(workRequest)
}

