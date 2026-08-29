package com.mockgps.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mockgps.R
import com.mockgps.data.MockProfile
import com.mockgps.repository.MockProfileRepository
import com.mockgps.shizuku.ShizukuHelper
import com.mockgps.util.VersionCompat
import com.mockgps.util.VersionCompat.createNotificationChannelIfNeeded
import com.mockgps.util.VersionCompat.buildNotification
import com.mockgps.util.VersionCompat.setupMockProvider
import com.mockgps.util.VersionCompat.removeMockProvider
import com.mockgps.util.VersionCompat.setMockLocation
import com.mockgps.util.VersionCompat.createBackgroundScope
import com.mockgps.util.VersionCompat.safeDelay
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class PerAppLocationService : Service() {
    private val scope = createBackgroundScope()
    private var locationManager: LocationManager? = null
    private val mockProviderName = "mock_gps_per_app_provider"
    private val activeProfiles = ConcurrentHashMap<String, PerAppState>()
    private var isRunning = false
    private val CHANNEL_ID = "per_app_location_channel"
    private var shizukuReady = false
    private var profileRepository: MockProfileRepository? = null
    private val PREFS_NAME = "mock_gps_prefs"

    data class PerAppState(
        val profile: MockProfile,
        var currentWaypointIndex: Int = 0,
        var isRouteMode: Boolean = false
    )

    override fun onCreate() {
        super.onCreate()
        Log.d("PerAppLocationService", "Service created")
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
        
        // Initialize repository
        val db = com.mockgps.data.AppDatabase.getDatabase(this)
        profileRepository = com.mockgps.repository.MockProfileRepository(db)
        
        // Check Shizuku availability
        checkShizukuAvailability()
    }

    private fun checkShizukuAvailability() {
        scope.launch {
            if (ShizukuHelper.isShizukuAvailable(this@PerAppLocationService)) {
                ShizukuHelper.requestPermission(this@PerAppLocationService, object : ShizukuHelper.ShizukuCallback {
                    override fun onShizukuReady() {
                        shizukuReady = true
                        Log.d("PerAppLocationService", "Shizuku ready - per-app spoofing enabled")
                        loadAndApplyProfiles()
                    }

                    override fun onShizukuError(error: String) {
                        Log.w("PerAppLocationService", "Shizuku error: $error")
                        shizukuReady = false
                        loadAndApplyProfiles()
                    }

                    override fun onPermissionGranted() {
                        Log.d("PerAppLocationService", "Shizuku permission granted")
                    }

                    override fun onPermissionDenied() {
                        Log.w("PerAppLocationService", "Shizuku permission denied")
                    }
                })
            } else {
                Log.w("PerAppLocationService", "Shizuku not available - per-app spoofing limited")
                shizukuReady = false
                loadAndApplyProfiles()
            }
        }
    }

    private fun loadAndApplyProfiles() {
        profileRepository?.getEnabledProfiles()?.collect { profiles ->
            profiles.forEach { profile ->
                addProfileInternal(profile)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_START_PER_APP -> {
                if (!isRunning) {
                    startPerAppSpoofing()
                }
            }
            ACTION_STOP_PER_APP -> {
                stopPerAppSpoofing()
            }
            ACTION_ADD_PROFILE -> {
                intent.getParcelableExtra<MockProfile>("profile")?.let { addProfileInternal(it) }
            }
            ACTION_REMOVE_PROFILE -> {
                intent.getStringExtra("packageName")?.let { removeProfileInternal(it) }
            }
            ACTION_UPDATE_PROFILE -> {
                intent.getParcelableExtra<MockProfile>("profile")?.let { updateProfileInternal(it) }
            }
        }
        return START_STICKY
    }

    private fun startPerAppSpoofing() {
        isRunning = true
        setupMockProvider()
        startForegroundService()
        scope.launch { updateLoop() }
        Log.d("PerAppLocationService", "Per-app spoofing started")
    }

    private fun stopPerAppSpoofing() {
        isRunning = false
        scope.cancel()
        cleanupMockProvider()
        stopForeground(true)
        stopSelf()
        Log.d("PerAppLocationService", "Per-app spoofing stopped")
    }

    private fun setupMockProvider() {
        locationManager?.let { lm ->
            setupMockProvider(lm, mockProviderName)
        }
    }

    private fun cleanupMockProvider() {
        locationManager?.let { lm ->
            removeMockProvider(lm, mockProviderName)
        }
    }

    private fun startForegroundService() {
        val intent = Intent(this, com.mockgps.ui.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = buildNotification(
            context = this,
            channelId = CHANNEL_ID,
            title = "Per-App Mock GPS Active",
            text = "Per-app location spoofing running (${activeProfiles.size} apps)",
            smallIcon = R.drawable.ic_launcher_foreground,
            contentIntent = pendingIntent,
            ongoing = true,
            priority = NotificationCompat.PRIORITY_LOW
        )

        startForeground(2, notification)
    }

    private fun createNotificationChannel() {
        createNotificationChannelIfNeeded(
            context = this,
            channelId = CHANNEL_ID,
            channelName = "Per-App Mock Location",
            importance = android.app.NotificationManager.IMPORTANCE_LOW
        )
    }

    private fun updateLoop() {
        while (isRunning) {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val updateInterval = prefs.getLong("per_app_update_interval", 1000L)
            
            activeProfiles.forEach { (packageName, state) ->
                if (state.profile.isEnabled) {
                    sendMockLocationForApp(packageName, state)
                }
            }
            safeDelay(updateInterval)
        }
    }

    private fun sendMockLocationForApp(packageName: String, state: PerAppState) {
        // Configure AppOps for this app to use our mock provider
        if (shizukuReady) {
            ShizukuHelper.setPerAppMockLocation(
                this@PerAppLocationService,
                packageName,
                state.profile.latitude,
                state.profile.longitude,
                state.profile.altitude,
                state.profile.accuracy
            )
        }

        // Update the mock location
        val location = Location(mockProviderName).apply {
            if (state.isRouteMode) {
                // Route mode handled by main service
            } else {
                latitude = state.profile.latitude
                longitude = state.profile.longitude
                altitude = state.profile.altitude ?: 0.0
            }
            speed = state.profile.speed
            bearing = state.profile.heading
            accuracy = state.profile.accuracy
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = System.nanoTime()
        }

        locationManager?.let { lm ->
            setMockLocation(lm, mockProviderName, location)
        }
        
        // Update notification with current app count
        updateNotification()
    }

    private fun updateNotification() {
        val notification = buildNotification(
            context = this,
            channelId = CHANNEL_ID,
            title = "Per-App Mock GPS Active",
            text = "Spoofing location for ${activeProfiles.size} apps",
            smallIcon = R.drawable.ic_launcher_foreground,
            ongoing = true,
            priority = NotificationCompat.PRIORITY_LOW
        )
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(2, notification)
    }

    private fun addProfileInternal(profile: MockProfile) {
        activeProfiles[profile.packageName] = PerAppState(profile)
        
        // Configure AppOps for this app
        if (shizukuReady) {
            ShizukuHelper.setAppLocationMode(this, profile.packageName, true)
            ShizukuHelper.setAppMockLocationMode(this, profile.packageName, true)
        }
        
        updateNotification()
        Log.d("PerAppLocationService", "Added profile for ${profile.packageName}")
    }

    private fun removeProfileInternal(packageName: String) {
        activeProfiles.remove(packageName)
        
        // Reset AppOps for this app
        if (shizukuReady) {
            ShizukuHelper.setAppLocationMode(this, packageName, false)
            ShizukuHelper.setAppMockLocationMode(this, packageName, false)
        }
        
        updateNotification()
        Log.d("PerAppLocationService", "Removed profile for $packageName")
    }

    private fun updateProfileInternal(profile: MockProfile) {
        activeProfiles[profile.packageName]?.let { state ->
            activeProfiles[profile.packageName] = state.copy(profile = profile)
        }
        Log.d("PerAppLocationService", "Updated profile for ${profile.packageName}")
    }

    fun addProfile(profile: MockProfile) {
        addProfileInternal(profile)
        scope.launch {
            profileRepository?.addOrUpdateProfile(profile)
        }
    }

    fun removeProfile(packageName: String) {
        removeProfileInternal(packageName)
        scope.launch {
            profileRepository?.let { repo ->
                val profiles = repo.getEnabledProfiles().first()
                profiles.find { it.packageName == packageName }?.let { profile ->
                    repo.deleteProfile(profile.id)
                }
            }
        }
    }

    fun updateProfile(profile: MockProfile) {
        updateProfileInternal(profile)
        scope.launch {
            profileRepository?.updateProfile(profile)
        }
    }

    fun setRouteMode(packageName: String, waypoints: List<com.mockgps.data.RouteWaypoint>) {
        activeProfiles[packageName]?.let { state ->
            activeProfiles[packageName] = state.copy(
                isRouteMode = true,
                currentWaypointIndex = 0
            )
        }
    }

    fun clearRouteMode(packageName: String) {
        activeProfiles[packageName]?.let { state ->
            activeProfiles[packageName] = state.copy(
                isRouteMode = false,
                currentWaypointIndex = 0
            )
        }
    }

    fun isProfileActive(packageName: String): Boolean = activeProfiles.containsKey(packageName)

    fun getActiveProfiles(): List<String> = activeProfiles.keys.toList()

    override fun onDestroy() {
        isRunning = false
        scope.cancel()
        cleanupMockProvider()
        stopForeground(true)
        super.onDestroy()
        Log.d("PerAppLocationService", "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_PER_APP = "com.mockgps.ACTION_START_PER_APP"
        const val ACTION_STOP_PER_APP = "com.mockgps.ACTION_STOP_PER_APP"
        const val ACTION_ADD_PROFILE = "com.mockgps.ACTION_ADD_PROFILE"
        const val ACTION_REMOVE_PROFILE = "com.mockgps.ACTION_REMOVE_PROFILE"
        const val ACTION_UPDATE_PROFILE = "com.mockgps.ACTION_UPDATE_PROFILE"
    }
}