package com.example.learndi

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "task_table")
data class Task(
    val title: String,
    val isDone: Boolean = false,
    val firstName: String? = null,                      // Member's first name
    val middleName: String? = null,                    // Middle name (nullable)
    val lastName: String? = null,                       // Last name
    val town: String? = null,                           // Town
    val shortName: String? = null,                      // Generated short name
    var isAlive: Boolean = true,                // New field: true by default
    val childNumber: Int? = 1,                      // Child number (nullable)
    val comment: String? = null,                       // Comment (nullable)
    var imageUri: String? = null,               // Image URI (nullable)

    // ✅ Sync and metadata fields
    val syncStatus: SyncStatus = SyncStatus.NOT_SYNCED,  // Sync tracking status
    var deleted: Boolean = false,               // Local delete flag
    val createdAt: Long = System.currentTimeMillis(),    // Creation timestamp
    var createdBy: String? = null,              // Creator ID (nullable)
    var updatedAt: Long = System.currentTimeMillis(),                   // 🔥 New field: For Firestore → Room sync

    // ✅ ID fields at the end (Firestore consistency)
    @PrimaryKey val id: String = UUID.randomUUID().toString(),  // Unique ID
    var parentID: String? = null,               // Parent ID (nullable)
    var spouseID: String? = null                // Spouse ID (nullable)
)

enum class SyncStatus {
    NOT_SYNCED, UPDATED, SYNCED
}

fun Task.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "title" to title,
    "isDone" to isDone,
    "firstName" to firstName,
    "middleName" to middleName,
    "lastName" to lastName,
    "town" to town,
    "shortName" to shortName,
    "isAlive" to isAlive,
    "childNumber" to childNumber,
    "comment" to comment,
    "imageUri" to imageUri,
    //"syncStatus" to syncStatus.name,
    //"deleted" to deleted,
    "createdAt" to createdAt,
    "createdBy" to createdBy,
    "updatedAt" to updatedAt,
    "parentID" to parentID,
    "spouseID" to spouseID
)

