package com.xiaoquexing.app.data.remote

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.xiaoquexing.app.XiaoQueXingApp
import java.util.concurrent.TimeUnit

class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? XiaoQueXingApp ?: return Result.retry()
        val report = runCatching { app.container.syncEngine.syncAll(retries = 2) }.getOrElse {
            return Result.retry()
        }
        return if (report.error == null) Result.success() else Result.retry()
    }
}

object SyncWork {
    const val ONCE = "xqx_sync_once"
    const val PERIODIC = "xqx_sync_periodic"

    fun enqueueOnce(context: Context) {
        val req = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(network())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(ONCE, ExistingWorkPolicy.REPLACE, req)
    }

    fun ensurePeriodic(context: Context) {
        val req = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(network())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            req,
        )
    }

    private fun network() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
}
