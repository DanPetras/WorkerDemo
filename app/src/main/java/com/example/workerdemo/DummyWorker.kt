package com.example.workerdemo

import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.await
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class DummyWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    companion object {
        private const val TAG = "DummyWorker"
        private const val WORK_ID = "dummy_work"
        private const val NOTIFICATION_CHANNEL = "dummy_channel"
        private const val NOTIFICATION_ID = 1

        fun createChannels(context: Context) {
            NotificationManagerCompat.from(context).createNotificationChannelsCompat(listOf(
                NotificationChannelCompat.Builder(NOTIFICATION_CHANNEL, NotificationManager.IMPORTANCE_LOW)
                    .setName("Dummy channel")
                    .build(),
            ))
        }

        suspend fun enqueue(context: Context) {
            Log.d(TAG, "Adding dummy worker")
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_ID, ExistingWorkPolicy.APPEND_OR_REPLACE, OneTimeWorkRequest.Builder(DummyWorker::class)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setConstraints(Constraints.Builder().setRequiredNetwork(context).build())
                    .build())
                .await()
        }

        suspend fun cancelAll(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_ID).await()
        }

        private fun Constraints.Builder.setRequiredNetwork(context: Context): Constraints.Builder {
            val networkRequest = NetworkRequest.Builder().run {
                addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED)
                build()
            }
            val networkType = NetworkType.UNMETERED
            return setRequiredNetworkRequest(networkRequest, networkType)
        }
    }

    private var notificationBuilder: NotificationCompat.Builder? = null

    override suspend fun doWork(): Result {
        try {
            // Simulate some work
            repeat(5) {
                delay(2.seconds)
                val progress = (it + 1) / 5f
                Log.i(TAG, "Progress: $progress")
                safeSetForegroundProgress(progress)
            }
            delay(1.seconds)
        } catch (e: Exception) {
            Log.e(TAG, "Error in DummyWorker", e)
        }
        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return getForegroundInfo(0f)
    }

    private fun getForegroundInfo(progress: Float): ForegroundInfo {
        if (notificationBuilder == null) {
            notificationBuilder = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_sync)
                .setSubText("Dummy Worker")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
        }
        return ForegroundInfo(NOTIFICATION_ID, notificationBuilder!!
            .setContentTitle("Dummy Worker")
            .setProgress(100, (progress * 100).toInt(), false)
            .build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    private suspend fun safeSetForegroundProgress(progress: Float) {
        setProgress(Data.Builder().putFloat("progress", progress).build())
        try {
            setForeground(getForegroundInfo(progress))
        } catch (e: IllegalStateException) {
            // Might occur when not able to run in the foreground at this point.
            Log.w(TAG, "Failed to setForeground", e)
        }
    }
}
