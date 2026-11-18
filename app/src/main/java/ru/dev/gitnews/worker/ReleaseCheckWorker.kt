package ru.dev.gitnews.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ru.dev.gitnews.GitNewsApplication
import ru.dev.gitnews.MainActivity
import ru.dev.gitnews.R

class ReleaseCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? GitNewsApplication ?: return Result.success()
        val container = app.container

        if (!container.secureTokenStorage.isReleaseNotificationsEnabled()) {
            Log.d(TAG, "Release notifications disabled by user preference")
            return Result.success()
        }

        return try {
            val updates = container.releaseTrackerRepository.checkNewReleases()
            if (updates.isNotEmpty()) {
                createNotificationChannelIfNeeded()
                updates.forEach { event ->
                    showReleaseNotification(event.repoId, event.fullName, event.newTag, event.releaseName)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "ReleaseCheckWorker execution failed", e)
            Result.retry()
        }
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Релизы репозиториев",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Уведомления о новых версиях и релизах в отслеживаемых репозиториях"
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun showReleaseNotification(
        repoId: Long,
        fullName: String,
        newTag: String,
        releaseName: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted")
                return
            }
        }

        val parts = fullName.split('/')
        val owner = parts.getOrNull(0) ?: ""
        val repo = parts.getOrNull(1) ?: fullName

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("repo_owner", owner)
            putExtra("repo_name", repo)
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            repoId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = fullName
        val content = "Новый релиз $newTag" + if (releaseName.isNotBlank() && releaseName != newTag) ": $releaseName" else ""

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext).notify(repoId.toInt(), notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while posting notification", e)
        }
    }

    companion object {
        private const val TAG = "ReleaseCheckWorker"
        const val CHANNEL_ID = "gitnews_releases_channel"
    }
}
