package com.example.learndi

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM task_table where deleted = 0")
    //fun getAllTasksLiveData(): LiveData<List<Task>>
    fun getAll(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("SELECT * FROM task_table WHERE syncStatus IN (:statuses)")
    suspend fun getTasksBySyncStatus(statuses: List<SyncStatus>): List<Task>

    @Query("SELECT * FROM task_table WHERE deleted = 1")
    suspend fun getMarkAsDeletedTasks(): List<Task>

    @Query("SELECT * FROM task_table WHERE syncStatus = :status")
    fun getUnsyncedTasks(status: SyncStatus = SyncStatus.NOT_SYNCED): Flow<List<Task>>




}
