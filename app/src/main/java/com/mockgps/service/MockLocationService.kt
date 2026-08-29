package com.mockgps.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mockgps.R
import com.mockgps.ui.MainActivity
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
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class MockLocationService : Service() {
    private val scope = createBackgroundScope()
    private var locationManager: LocationManager? = null
    private var mockProviderName = "mock_gps_provider"
    private val activeProfiles = ConcurrentHashMap<String, MockProfileState>()
    private var isRunning = false
    private val CHANNEL_ID = "mock_location_channel"

    data class MockProfileState(
        val profile: com.mockgps.data.MockProfile,
        var currentIndex: Int = 0,
        var waypoints: List<com.mockgps.data.RouteWaypoint> = emptyList(),
        var isRouteMode: Boolean = false
    )

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_START_MOCKING -> {
                if (!isRunning) {
                    startMocking()
                }
            }
            ACTION_STOP_MOCKING -> {
                stopMocking()
            }
            ACTION_ADD_PROFILE -> {
                intent.getParcelableExtra<com.mockgps.data.MockProfile>("profile")?.let { addProfile(it) }
            }
            ACTION_REMOVE_PROFILE -> {
                intent.getStringExtra("packageName")?.let { removeProfile(it) }
            }
            ACTION_UPDATE_PROFILE -> {
                intent.getParcelableExtra<com.mockgps.data.MockProfile>("profile")?.let { updateProfile(it) }
            }
            else -> {
                if (!isRunning) {
                    startMocking()
                }
            }
        }
        return START_STICKY
    }

    private fun startMocking() {
        isRunning = true
        setupMockProvider()
        startForegroundService()
        scope.launch { updateLoop() }
    }

    private fun stopMocking() {
        isRunning = false
        scope.cancel()
        cleanupMockProvider()
        stopForeground(true)
        stopSelf()
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
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = buildNotification(
            context = this,
            channelId = CHANNEL_ID,
            title = "Mock GPS Active",
            text = "Mock location provider is running (${activeProfiles.size} profiles)",
            smallIcon = R.drawable.ic_launcher_foreground,
            contentIntent = pendingIntent,
            ongoing = true,
            priority = NotificationCompat.PRIORITY_LOW
        )

        startForeground(1, notification)
    }

    private fun createNotificationChannel() {
        createNotificationChannelIfNeeded(
            context = this,
            channelId = CHANNEL_ID,
            channelName = "Mock Location Service",
            importance = android.app.NotificationManager.IMPORTANCE_LOW
        )
    }

    private fun updateLoop() {
        while (isRunning) {
            activeProfiles.forEach { (packageName, state) ->
                if (state.profile.isEnabled) {
                    sendMockLocation(state)
                }
            }
            safeDelay(1000L) // Default 1 second interval
        }
    }

    private fun sendMockLocation(state: MockProfileState) {
        val location = Location(mockProviderName).apply {
            if (state.isRouteMode && state.waypoints.isNotEmpty()) {
                val wp = state.waypoints[state.currentIndex % state.waypoints.size]
                latitude = wp.latitude
                longitude = wp.longitude
                altitude = wp.altitude ?: 0.0
                state.currentIndex++
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
    }

    fun addProfile(profile: com.mockgps.data.MockProfile) {
        activeProfiles[profile.packageName] = MockProfileState(profile)
        updateNotification()
    }

    fun updateProfile(profile: com.mockgps.data.MockProfile) {
        activeProfiles[profile.packageName]?.let { state ->
            activeProfiles[profile.packageName] = state.copy(profile = profile)
        }
        updateNotification()
    }

    fun removeProfile(packageName: String) {
        activeProfiles.remove(packageName)
        updateNotification()
    }

    fun setRouteMode(packageName: String, waypoints: List<com.mockgps.data.RouteWaypoint>) {
        activeProfiles[packageName]?.let { state ->
            activeProfiles[packageName] = state.copy(
                waypoints = waypoints,
                isRouteMode = true,
                currentIndex = 0
            )
        }
    }

    fun clearRouteMode(packageName: String) {
        activeProfiles[packageName]?.let { state ->
            activeProfiles[packageName] = state.copy(
                waypoints = emptyList(),
                isRouteMode = false,
                currentIndex = 0
            )
        }
    }

    private fun updateNotification() {
        val notification = buildNotification(
            context = this,
            channelId = CHANNEL_ID,
            title = "Mock GPS Active",
            text = "Mock location provider is running (${activeProfiles.size} profiles)",
            smallIcon = R.drawable.ic_launcher_foreground,
            ongoing = true,
            priority = NotificationCompat.PRIORITY_LOW
        )
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(1, notification)
    }

    override fun onDestroy() {
        isRunning = false
        scope.cancel()
        cleanupMockProvider()
        stopForeground(true)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_MOCKING = "com.mockgps.ACTION_START_MOCKING"
        const val ACTION_STOP_MOCKING = "com.mockgps.ACTION_STOP_MOCKING"
        const val ACTION_ADD_PROFILE = "com.mockgps.ACTION_ADD_PROFILE"
        const val ACTION_REMOVE_PROFILE = "com.mockgps.ACTION_REMOVE_PROFILE"
        const val ACTION_UPDATE_PROFILE = "com.mockgps.ACTION_UPDATE_PROFILE"
    }
}