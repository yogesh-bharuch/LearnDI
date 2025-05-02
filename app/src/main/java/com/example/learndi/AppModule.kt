package com.example.learndi

import android.content.Context
import androidx.room.Room
import com.example.learndi.firestore.FirestoreTaskService
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides app-wide singleton dependencies such as
 * Room database, DAO, Repository, Firestore, and Firebase Storage.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Room Database

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): TaskDatabase {
        return Room.databaseBuilder(
            context,
            TaskDatabase::class.java,
            "task_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideTaskDao(
        db: TaskDatabase
    ): TaskDao = db.taskDao()

    @Provides
    @Singleton
    fun provideRepository(
        taskDao: TaskDao,
        //firestoreTaskService: FirestoreTaskService
        ): TaskRepository {
        return TaskRepository(taskDao)
        //return TaskRepository(taskDao, firestoreTaskService)
    }

    // Firebase

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirestoreTaskService(
        @ApplicationContext context: Context
    ): FirestoreTaskService {
        return FirestoreTaskService(context)
    }
}


/*
package com.example.learndi

import android.content.Context
import androidx.room.Room
import com.example.learndi.firestore.FirestoreTaskService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

*/
/**
 * Hilt module that provides app-wide singleton dependencies such as
 * Room database, DAO, Repository, and Firestore service.
 *//*

@Module
@InstallIn(SingletonComponent::class) // Install this module in the Application scope
object AppModule {

    */
/**
     * Provides a singleton instance of TaskRepository.
     *//*

    @Provides
    @Singleton
    fun provideRepository(taskDao: TaskDao): TaskRepository {
        return TaskRepository(taskDao)
    }

    */
/**
     * Provides a singleton instance of Room TaskDatabase.
     *//*

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): TaskDatabase {
        return Room.databaseBuilder(
            context,
            TaskDatabase::class.java,
            "task_database"
        ).build()
    }

    */
/**
     * Provides a singleton instance of TaskDao from the database.
     *//*

    @Provides
    @Singleton
    fun provideTaskDao(
        db: TaskDatabase
    ): TaskDao = db.taskDao()

    */
/**
     * Provides a singleton instance of FirestoreTaskService.
     *//*

    @Provides
    @Singleton
    fun provideFirestoreTaskService(
        @ApplicationContext context: Context
    ): FirestoreTaskService {
        return FirestoreTaskService(context)
    }


}
*/
