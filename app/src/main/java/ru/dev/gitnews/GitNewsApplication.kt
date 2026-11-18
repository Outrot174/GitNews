package ru.dev.gitnews

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import ru.dev.gitnews.di.AppContainer
import ru.dev.gitnews.di.DefaultAppContainer
import java.io.File

class GitNewsApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        setupCrashHandler()
        container = DefaultAppContainer(this)
        container.releaseTrackerRepository.schedulePeriodicCheck(
            container.releaseTrackerRepository.isNotificationsEnabled()
        )
        try {
            com.yandex.mobile.ads.common.MobileAds.initialize(this) {
                Log.d("GitNewsApp", "Yandex Mobile Ads initialized")
            }
        } catch (e: Exception) {
            Log.e("GitNewsApp", "Yandex Mobile Ads initialization failed", e)
        }
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val stackTrace = Log.getStackTraceString(throwable)
                Log.e("GitNewsCrash", "FATAL EXCEPTION in thread ${thread.name}: $stackTrace")

                // Write crash log file in filesDir and cacheDir
                try {
                    val crashFile = File(filesDir, "crash_log.txt")
                    crashFile.writeText("=== GitNews Fatal Crash ===\nThread: ${thread.name}\nTime: ${System.currentTimeMillis()}\n$stackTrace\n")
                } catch (_: Exception) {}

                try {
                    val cacheCrashFile = File(cacheDir, "crash_log.txt")
                    cacheCrashFile.writeText("=== GitNews Fatal Crash ===\nThread: ${thread.name}\nTime: ${System.currentTimeMillis()}\n$stackTrace\n")
                } catch (_: Exception) {}

                // Copy top stacktrace to Android clipboard for instant diagnostics
                try {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val topError = "GitNews Fatal Crash [${throwable.javaClass.simpleName}]: ${throwable.message}\n" +
                            stackTrace.lines().take(6).joinToString("\n")
                    clipboard?.setPrimaryClip(ClipData.newPlainText("GitNews Crash Log", topError))
                } catch (_: Exception) {}
            } catch (_: Exception) {}

            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
