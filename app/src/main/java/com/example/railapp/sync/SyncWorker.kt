package com.example.railapp.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.railapp.data.AppDatabase
import com.example.railapp.di.AppContainer
import io.github.jan.supabase.postgrest.postgrest

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val appContainer = AppContainer(applicationContext)
            val dao = appContainer.database.scanEventDao()
            val unsynced = dao.getUnsyncedEvents()

            if (unsynced.isNotEmpty()) {
                val client = appContainer.supabaseClient
                client.postgrest["scan_events"].insert(unsynced)
                dao.markAsSynced(unsynced.map { it.id })
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
