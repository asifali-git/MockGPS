package com.mockgps

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.multidex.MultiDex
import com.mockgps.data.AppDatabase
import com.mockgps.receiver.BootReceiver
import com.mockgps.service.MockLocationService
import com.mockgps.service.PerAppLocationService

class MockGPSApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize database
        AppDatabase.getDatabase(this)
        
        // Initialize osmdroid configuration
        org.osmdroid.config.Configuration.getInstance().apply {
            userAgentValue = "MockGPS/1.0"
        }
        
        // Check auto-start on app launch (if not started by boot receiver)
        checkAutoStartServices()
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    private fun checkAutoStartServices() {
        val autoStartEnabled = BootReceiver.isAutoStartEnabled(this)
        val perAppEnabled = BootReceiver.isPerAppEnabled(this)
        
        if (autoStartEnabled) {
            // Start main mock service
            val mockIntent = Intent(this, MockLocationService::class.java)
            mockIntent.action = MockLocationService.ACTION_START_MOCKING
            startForegroundServiceCompat(mockIntent)
            
            // Start per-app service if enabled
            if (perAppEnabled) {
                val perAppIntent = Intent(this, PerAppLocationService::class.java)
                perAppIntent.action = PerAppLocationService.ACTION_START_PER_APP
                startForegroundServiceCompat(perAppIntent)
            }
        }
    }
    
    private fun startForegroundServiceCompat(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
    
    companion object {
        fun startServices(context: Context) {
            val autoStartEnabled = BootReceiver.isAutoStartEnabled(context)
            val perAppEnabled = BootReceiver.isPerAppEnabled(context)
            
            if (autoStartEnabled) {
                val mockIntent = Intent(context, MockLocationService::class.java)
                mockIntent.action = MockLocationService.ACTION_START_MOCKING
                startForegroundServiceCompat(context, mockIntent)
                
                if (perAppEnabled) {
                    val perAppIntent = Intent(context, PerAppLocationService::class.java)
                    perAppIntent.action = PerAppLocationService.ACTION_START_PER_APP
                    startForegroundServiceCompat(context, perAppIntent)
                }
            }
        }
        
        private fun startForegroundServiceCompat(context: Context, intent: Intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopServices(context: Context) {
            context.stopService(Intent(context, MockLocationService::class.java))
            context.stopService(Intent(context, PerAppLocationService::class.java))
        }
    }
}