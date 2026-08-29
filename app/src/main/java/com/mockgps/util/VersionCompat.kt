package com.mockgps.util

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Version compatibility utilities for Android API 21 (5.0) through future versions.
 * Handles all API differences dynamically at runtime.
 */
object VersionCompat {
    
    // Minimum SDK we support
    const val MIN_SDK = 21 // Android 5.0 Lollipop
    
    // Current compile target
    const val TARGET_SDK = 34 // Android 14
    
    /**
     * Check if running on specific API level or higher
     */
    @Suppress("UNUSED_PARAMETER")
    inline fun isAtLeast(apiLevel: Int): Boolean = Build.VERSION.SDK_INT >= apiLevel
    
    /**
     * Start foreground service with compatibility across all versions
     */
    fun startForegroundServiceCompat(context: Context, intent: Intent): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            true
        } catch (e: Exception) {
            // Fallback for restricted backgrounds
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    val pendingIntent = PendingIntent.getService(
                        context, 0, intent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    // Try with WorkManager as fallback
                    false
                } catch (e2: Exception) {
                    false
                }
            } else {
                context.startService(intent)
                true
            }
        }
    }
    
    /**
     * Stop service with compatibility
     */
    fun stopServiceCompat(context: Context, intent: Intent): Boolean {
        return try {
            context.stopService(intent)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Create notification channel (API 26+)
     */
    fun createNotificationChannelIfNeeded(
        context: Context,
        channelId: String,
        channelName: String,
        importance: Int = NotificationManager.IMPORTANCE_LOW
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(channelId, channelName, importance).apply {
                    description = "Mock GPS Service Channel"
                    enableLights(false)
                    enableVibration(false)
                    setShowBadge(false)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }
    
    /**
     * Build notification with max compatibility
     */
    fun buildNotification(
        context: Context,
        channelId: String,
        title: String,
        text: String,
        smallIcon: Int,
        contentIntent: PendingIntent? = null,
        ongoing: Boolean = true,
        priority: Int = NotificationCompat.PRIORITY_LOW
    ): Notification {
        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(smallIcon)
            .setOngoing(ongoing)
            .setPriority(priority)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setColorized(false)
            .setOnlyAlertOnce(true)
        
        contentIntent?.let { builder.setContentIntent(it) }
        
        // API 26+ requires channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannelIfNeeded(context, channelId, "Mock GPS Service")
        }
        
        return builder.build()
    }
    
    /**
     * Check mock location permission with full compatibility
     */
    fun hasMockLocationPermission(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_MOCK_LOCATION,
                    android.os.Process.myUid(),
                    context.packageName
                ) == AppOpsManager.MODE_ALLOWED
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                ContextCompat.checkSelfPermission(
                    context, 
                    android.Manifest.permission.ACCESS_MOCK_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            }
            else -> {
                // Pre-Marshmallow: mock location permission granted at install
                true
            }
        }
    }
    
    /**
     * Request mock location permission
     */
    fun requestMockLocationPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_OPS_MODES)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
    
    /**
     * Check if ignoring battery optimizations
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return pm.isIgnoringBatteryOptimizations(context.packageName)
        }
        return true // Pre-Marshmallow: no battery optimization
    }
    
    /**
     * Request ignore battery optimizations
     */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
    
    /**
     * Get exact alarm permission (API 31+)
     */
    fun hasExactAlarmPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            return alarmManager.canScheduleExactAlarms()
        }
        return true // Pre-Android 12: no exact alarm restriction
    }
    
    /**
     * Request exact alarm permission
     */
    fun requestExactAlarmPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
    
    /**
     * Set exact alarm with compatibility
     */
    fun setExactAlarm(
        context: Context,
        alarmType: Int,
        triggerAtMillis: Long,
        operation: PendingIntent
    ): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        
        return try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    if (hasExactAlarmPermission(context)) {
                        alarmManager.setExactAndAllowWhileIdle(alarmType, triggerAtMillis, operation)
                    } else {
                        alarmManager.setExact(alarmType, triggerAtMillis, operation)
                    }
                    true
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                    alarmManager.setExact(alarmType, triggerAtMillis, operation)
                    true
                }
                else -> {
                    alarmManager.set(alarmType, triggerAtMillis, operation)
                    true
                }
            }
        } catch (e: SecurityException) {
            // Fallback to inexact alarm
            try {
                alarmManager.set(alarmType, triggerAtMillis, operation)
                true
            } catch (e2: Exception) {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Cancel alarm with compatibility
     */
    fun cancelAlarm(context: Context, operation: PendingIntent): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        return try {
            alarmManager.cancel(operation)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check location permission with compatibility
     */
    fun hasLocationPermissions(context: Context): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocation = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
        return fineLocation == PackageManager.PERMISSION_GRANTED || 
               coarseLocation == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check background location permission (API 29+)
     */
    fun hasBackgroundLocationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true // Pre-Android 10: no separate background permission
    }
    
    /**
     * Setup mock location provider with compatibility
     */
    @SuppressLint("MissingPermission")
    fun setupMockProvider(
        locationManager: LocationManager,
        providerName: String
    ): Boolean {
        return try {
            if (locationManager.getProvider(providerName) != null) {
                locationManager.removeTestProvider(providerName)
            }
            
            val requiresNetwork = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1
            val requiresSatellite = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1
            val requiresCell = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1
            val hasMonetaryCost = false
            val supportsAltitude = true
            val supportsSpeed = true
            val supportsBearing = true
            val powerUsage = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 ? 0 : 1
            val accuracy = 5
            
            locationManager.addTestProvider(
                providerName,
                requiresNetwork,
                requiresSatellite,
                requiresCell,
                hasMonetaryCost,
                supportsAltitude,
                supportsSpeed,
                supportsBearing,
                powerUsage,
                accuracy
            )
            
            locationManager.setTestProviderEnabled(providerName, true)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                locationManager.setTestProviderStatus(
                    providerName, 
                    LocationManager.STATUS_AVAILABLE, 
                    null, 
                    System.currentTimeMillis()
                )
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Remove mock provider with compatibility
     */
    fun removeMockProvider(locationManager: LocationManager, providerName: String): Boolean {
        return try {
            if (locationManager.getProvider(providerName) != null) {
                locationManager.removeTestProvider(providerName)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Set mock location with compatibility
     */
    fun setMockLocation(
        locationManager: LocationManager,
        providerName: String,
        location: Location
    ): Boolean {
        return try {
            locationManager.setTestProviderLocation(providerName, location)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Enable/disable test provider
     */
    fun setTestProviderEnabled(
        locationManager: LocationManager,
        providerName: String,
        enabled: Boolean
    ): Boolean {
        return try {
            locationManager.setTestProviderEnabled(providerName, enabled)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get AppOpsManager with compatibility
     */
    fun getAppOpsManager(context: Context): AppOpsManager? {
        return try {
            context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Check AppOps mode for location
     */
    fun checkLocationAppOps(appOps: AppOpsManager, uid: Int, packageName: String): Int {
        val coarse = try { appOps.checkOpNoThrow(AppOpsManager.OP_COARSE_LOCATION, uid, packageName) } catch (e: Exception) { AppOpsManager.MODE_DEFAULT }
        val fine = try { appOps.checkOpNoThrow(AppOpsManager.OP_FINE_LOCATION, uid, packageName) } catch (e: Exception) { AppOpsManager.MODE_DEFAULT }
        return if (coarse == AppOpsManager.MODE_ALLOWED || fine == AppOpsManager.MODE_ALLOWED) AppOpsManager.MODE_ALLOWED else AppOpsManager.MODE_IGNORED
    }
    
    /**
     * Check mock location AppOps
     */
    fun checkMockLocationAppOps(appOps: AppOpsManager, uid: Int, packageName: String): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                appOps.checkOpNoThrow(AppOpsManager.OPSTR_MOCK_LOCATION, uid, packageName)
            } else {
                AppOpsManager.MODE_DEFAULT
            }
        } catch (e: Exception) {
            AppOpsManager.MODE_DEFAULT
        }
    }
    
    /**
     * Set AppOps mode with compatibility
     */
    fun setAppOpsMode(
        appOps: AppOpsManager,
        op: String,
        uid: Int,
        packageName: String,
        mode: Int
    ): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && op == AppOpsManager.OPSTR_MOCK_LOCATION) {
                // Use reflection for OPSTR_MOCK_LOCATION on older versions
                val method = AppOpsManager::class.java.getMethod("setMode", String::class.java, Int::class.java, String::class.java, Int::class.java)
                method.invoke(appOps, op, uid, packageName, mode)
            } else {
                appOps.setMode(op, uid, packageName, mode)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get package UID with compatibility
     */
    fun getPackageUid(context: Context, packageName: String): Int {
        return try {
            context.packageManager.getPackageUid(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            -1
        }
    }
    
    /**
     * Check if app is system app
     */
    fun isSystemApp(context: Context, packageName: String): Boolean {
        return try {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0 || (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    /**
     * Safe coroutine scope for background work
     */
    fun createBackgroundScope(): CoroutineScope {
        return CoroutineScope(Dispatchers.IO + Job())
    }
    
    /**
     * Safe delay with compatibility
     */
    suspend fun safeDelay(millis: Long) {
        try {
            kotlinx.coroutines.delay(millis)
        } catch (e: Exception) {
            Thread.sleep(millis)
        }
    }
    
    /**
     * Get device info for debugging
     */
    fun getDeviceInfo(): String {
        return "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}), " +
               "Device: ${Build.MANUFACTURER} ${Build.MODEL}, " +
               "Brand: ${Build.BRAND}, " +
               "Board: ${Build.BOARD}, " +
               "Hardware: ${Build.HARDWARE}, " +
               "Fingerprint: ${Build.FINGERPRINT}"
    }
    
    /**
     * Check if running on emulator
     */
    fun isEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic")
            || Build.FINGERPRINT.startsWith("unknown")
            || Build.MODEL.contains("google_sdk")
            || Build.MODEL.contains("Emulator")
            || Build.MODEL.contains("Android SDK built for x86")
            || Build.MANUFACTURER.contains("Genymotion")
            || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
            || Build.PRODUCT.contains("sdk")
    }
    
    /**
     * Get available memory
     */
    fun getAvailableMemory(context: Context): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.availMem
    }
    
    /**
     * Check if device has low memory
     */
    fun isLowMemoryDevice(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            activityManager.isLowRamDevice()
        } else {
            getAvailableMemory(context) < 512 * 1024 * 1024 // 512MB
        }
    }
}

/**
 * Safe wrapper for LocationManager operations
 */
class SafeLocationManager(private val context: Context) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    
    fun getLastKnownLocation(provider: String): Location? {
        return try {
            if (VersionCompat.hasLocationPermissions(context)) {
                locationManager.getLastKnownLocation(provider)
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    fun requestLocationUpdates(
        provider: String,
        minTime: Long,
        minDistance: Float,
        listener: android.location.LocationListener
    ): Boolean {
        return try {
            if (VersionCompat.hasLocationPermissions(context)) {
                locationManager.requestLocationUpdates(provider, minTime, minDistance, listener)
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }
    
    fun removeUpdates(listener: android.location.LocationListener): Boolean {
        return try {
            locationManager.removeUpdates(listener)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun getProvider(name: String) = try { locationManager.getProvider(name) } catch (e: Exception) { null }
    
    fun allProviders = try { locationManager.allProviders } catch (e: Exception) { emptyList() }
    
    val isLocationEnabled: Boolean
        get() = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                locationManager.isLocationEnabled
            } else {
                val mode = Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE)
                mode != Settings.Secure.LOCATION_MODE_OFF
            }
        } catch (e: Exception) {
            false
        }
}

/**
 * Safe wrapper for NotificationManager operations
 */
class SafeNotificationManager(private val context: Context) {
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    fun notify(id: Int, notification: Notification): Boolean {
        return try {
            manager.notify(id, notification)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun cancel(id: Int): Boolean {
        return try {
            manager.cancel(id)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun cancelAll(): Boolean {
        return try {
            manager.cancelAll()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun areNotificationsEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            manager.areNotificationsEnabled()
        } else {
            true // Assume enabled on older versions
        }
    }
}

/**
 * Safe wrapper for AlarmManager operations
 */
class SafeAlarmManager(private val context: Context) {
    private val manager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
    
    fun setExactAndAllowWhileIdle(type: Int, triggerAtMillis: Long, operation: PendingIntent): Boolean {
        return VersionCompat.setExactAlarm(context, type, triggerAtMillis, operation)
    }
    
    fun setExact(type: Int, triggerAtMillis: Long, operation: PendingIntent): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                manager.setExact(type, triggerAtMillis, operation)
            } else {
                manager.set(type, triggerAtMillis, operation)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun set(type: Int, triggerAtMillis: Long, operation: PendingIntent): Boolean {
        return try {
            manager.set(type, triggerAtMillis, operation)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun cancel(operation: PendingIntent): Boolean {
        return VersionCompat.cancelAlarm(context, operation)
    }
}