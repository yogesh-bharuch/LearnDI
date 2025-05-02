
package com.example.learndi

import com.example.learndi.firestore.FirestoreTaskService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    //firestoreTaskService: FirestoreTaskService
) {

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

    // 🔄 Tasks that need to be synced (created or updated)
    suspend fun getTasksToSync(): List<Task> {
        return taskDao.getTasksBySyncStatus(
            listOf(SyncStatus.NOT_SYNCED, SyncStatus.UPDATED)
        )
    }
}

/*
package com.example.learndi

import android.content.Context
import android.util.Log
import com.example.learndi.firestore.FirestoreTaskService
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val firestoreService: FirestoreTaskService,  // Firestore sync service
    @ApplicationContext private val context: Context     // Application Context injected via Hilt
) {

    // Singleton instance
    companion object {
        @Volatile
        private var instance: TaskRepository? = null

        // ✅ Add getInstance() to provide repository singleton access
        fun getInstance(context: Context, firestoreService: FirestoreTaskService): TaskRepository {
            return instance ?: synchronized(this) {
                instance ?: TaskRepository(
                    TaskDatabase.getInstance(context).taskDao(),
                    firestoreService, // Pass FirestoreTaskService instance
                    context           // Pass the Context
                ).also { instance = it }
            }
        }
    }

//    private val firestore = FirebaseFirestore.getInstance()
//    private val collectionRef = firestore.collection("LearnDI")

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

    // 🔄 Tasks that need to be synced (created or updated)
    suspend fun getTasksToSync(): List<Task> {
        return taskDao.getTasksBySyncStatus(
            listOf(SyncStatus.NOT_SYNCED, SyncStatus.UPDATED)
        )
    }


    */
/**
     * ✅ Sync local data to Firestore
     * - Handles:
     *   - Syncing unsynced members
     *   - Deleting soft-deleted members
     *   - Cleaning up references to deleted members
     *//*

    suspend fun syncLocalDataToFirestore(callback: (String) -> Unit) {
        Log.d("SyncFlow", "🔥 From Repository: syncLocalDataToFirestore called")

        val messages = mutableListOf<String>()

        try {
            // ✅ Fetch all members mark for deleted members will not fetch
            val unsyncedTask = withContext(Dispatchers.IO) {
                getTasksToSync()
            }

            */
/*//*
/ ✅ Fetch soft-deleted members
            val deletedMembers = withContext(Dispatchers.IO) {
                getMarkAsDeletedTasks()
            }*//*


            // ✅ Return if no members to sync
            //if (unsyncedMembers.isEmpty() && deletedMembers.isEmpty()) {
            if (unsyncedTask.isEmpty()) {
                val noSyncMessage = "No members to sync"
                Log.d("SyncFlow", "🔥 $noSyncMessage")
                callback(noSyncMessage)
                return
            }

            */
/*//*
/ ✅ Delete members marked as deleted
            val deleteTasks = coroutineScope {
                if (deletedMembers.isNotEmpty()) {
                    deletedMembers.map { member ->
                        async { deleteFirestoreMember(member, messages) }
                    }
                } else emptyList()
            }*//*


           */
/* // ✅ Remove references to deleted members (parent/spouse links)
            val cleanUpTasks = coroutineScope {
                if (deletedMembers.isNotEmpty()) {
                    deletedMembers.map { member ->
                        async { removeReferencesToDeletedMember(member.id, messages) }
                    }
                } else emptyList()
            }*//*


            // ✅ Sync unsynced members to Firestore
            val syncTasks = coroutineScope {
                if (unsyncedTask.isNotEmpty()) {
                    unsyncedTask.map { task ->
                        async {
                            val taskData = task.toMap()
                            firestoreService.updateTask(task.id, taskData)
                        }
                        //async { updateFirestore(task, messages) }
                    }
                } else emptyList()
            }

            // ✅ Await all operations
            //deleteTasks.awaitAll()
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
    private suspend fun updateFirestore(task: Task, messages: MutableList<String>) {

    }

}

*/