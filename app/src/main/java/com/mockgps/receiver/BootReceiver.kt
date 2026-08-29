package com.mockgps.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import com.mockgps.service.MockLocationService
import com.mockgps.service.PerAppLocationService
import com.mockgps.util.VersionCompat
import com.mockgps.util.VersionCompat.startForegroundServiceCompat

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "MockGPS_BootReceiver"
        private const val PREFS_NAME = "mock_gps_prefs"
        private const val KEY_AUTO_START = "auto_start_enabled"
        private const val KEY_PER_APP_ENABLED = "per_app_enabled"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Received broadcast: $action")
        
        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                handleBootCompleted(context)
            }
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                handleAppUpdate(context)
            }
        }
    }

    private fun handleBootCompleted(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val autoStartEnabled = prefs.getBoolean(KEY_AUTO_START, false)
        val perAppEnabled = prefs.getBoolean(KEY_PER_APP_ENABLED, false)

        if (autoStartEnabled) {
            Log.d(TAG, "Auto-start enabled, starting services...")
            
            // Start main mock location service
            val mockIntent = Intent(context, MockLocationService::class.java)
            mockIntent.action = MockLocationService.ACTION_START_MOCKING
            startForegroundServiceCompat(context, mockIntent)

            // Start per-app location service if enabled
            if (perAppEnabled) {
                val perAppIntent = Intent(context, PerAppLocationService::class.java)
                perAppIntent.action = PerAppLocationService.ACTION_START_PER_APP
                startForegroundServiceCompat(context, perAppIntent)
            }

            // Schedule periodic check to ensure services stay alive
            scheduleServiceWatchdog(context)
        }
    }

    private fun handleAppUpdate(context: Context) {
        // Restart services after app update
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val autoStartEnabled = prefs.getBoolean(KEY_AUTO_START, false)
        
        if (autoStartEnabled) {
            val mockIntent = Intent(context, MockLocationService::class.java)
            mockIntent.action = MockLocationService.ACTION_START_MOCKING
            startForegroundServiceCompat(context, mockIntent)
        }
    }

    private fun scheduleServiceWatchdog(context: Context) {
        val alarmIntent = Intent(context, ServiceWatchdogReceiver::class.java)
        alarmIntent.action = ServiceWatchdogReceiver.ACTION_CHECK_SERVICES
        
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            0,
            alarmIntent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        val interval = 15 * 60 * 1000L // 15 minutes
        VersionCompat.setExactAlarm(
            context = context,
            alarmType = android.app.AlarmManager.RTC_WAKEUP,
            triggerAtMillis = System.currentTimeMillis() + interval,
            operation = pendingIntent
        )
    }

    companion object {
        fun setAutoStartEnabled(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_AUTO_START, enabled).apply()
        }

        fun isAutoStartEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_AUTO_START, false)
        }

        fun setPerAppEnabled(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_PER_APP_ENABLED, enabled).apply()
        }

        fun isPerAppEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_PER_APP_ENABLED, false)
        }
    }
}

class ServiceWatchdogReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_CHECK_SERVICES = "com.mockgps.ACTION_CHECK_SERVICES"
        private const val TAG = "MockGPS_Watchdog"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_CHECK_SERVICES) {
            checkAndRestartServices(context)
            // Reschedule
            BootReceiver().scheduleServiceWatchdog(context)
        }
    }

    private fun checkAndRestartServices(context: Context) {
        val prefs = context.getSharedPreferences(BootReceiver.PREFS_NAME, Context.MODE_PRIVATE)
        val autoStartEnabled = prefs.getBoolean(BootReceiver.KEY_AUTO_START, false)
        val perAppEnabled = prefs.getBoolean(BootReceiver.KEY_PER_APP_ENABLED, false)

        if (autoStartEnabled) {
            // Check if mock service is running
            if (!isServiceRunning(context, MockLocationService::class.java.name)) {
                Log.d(TAG, "MockLocationService not running, restarting...")
                val mockIntent = Intent(context, MockLocationService::class.java)
                mockIntent.action = MockLocationService.ACTION_START_MOCKING
                startForegroundServiceCompat(context, mockIntent)
            }

            // Check if per-app service is running
            if (perAppEnabled && !isServiceRunning(context, PerAppLocationService::class.java.name)) {
                Log.d(TAG, "PerAppLocationService not running, restarting...")
                val perAppIntent = Intent(context, PerAppLocationService::class.java)
                perAppIntent.action = PerAppLocationService.ACTION_START_PER_APP
                startForegroundServiceCompat(context, perAppIntent)
            }
        }
    }

    private fun isServiceRunning(context: Context, serviceName: String): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val services = activityManager.getRunningServices(Integer.MAX_VALUE)
        return services.any { it.service.className == serviceName }
    }
}