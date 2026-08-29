package com.mockgps.shizuku

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.util.Log
import com.mockgps.util.VersionCompat
import moe.shizuku.client.ShizukuClient
import moe.shizuku.manager.ShizukuManager
import moe.shizuku.os.ProcessCompat

object ShizukuHelper {
    private const val TAG = "MockGPS_Shizuku"
    private const val SHIZUKU_PERMISSION = "moe.shizuku.permission.API_V23"
    
    // AppOps constants - use string constants for maximum compatibility
    private const val OP_COARSE_LOCATION = "android:coarse_location"
    private const val OP_FINE_LOCATION = "android:fine_location"
    private const val OP_GPS = "android:gps"
    private const val OP_MOCK_LOCATION = "android:mock_location"
    
    // AppOps modes
    private const val MODE_ALLOWED = AppOpsManager.MODE_ALLOWED
    private const val MODE_IGNORED = AppOpsManager.MODE_IGNORED
    private const val MODE_ERRORED = AppOpsManager.MODE_ERRORED
    private const val MODE_DEFAULT = AppOpsManager.MODE_DEFAULT

    interface ShizukuCallback {
        fun onShizukuReady()
        fun onShizukuError(error: String)
        fun onPermissionGranted()
        fun onPermissionDenied()
    }

    fun isShizukuAvailable(context: Context): Boolean {
        return try {
            val pm = context.packageManager
            val info = pm.getPackageInfo("moe.shizuku.privileged.api", 0)
            info != null
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun isShizukuInstalled(context: Context): Boolean {
        return try {
            val pm = context.packageManager
            pm.getPackageInfo("moe.shizuku.manager", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun checkSelfPermission(context: Context): Int {
        return context.checkSelfPermission(SHIZUKU_PERMISSION)
    }

    fun requestPermission(context: Context, callback: ShizukuCallback) {
        // Shizuku works on Android 7.0+ (API 24)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ use ShizukuManager
                ShizukuManager.requestPermission(context) { granted ->
                    if (granted) {
                        callback.onPermissionGranted()
                        initializeShizuku(context, callback)
                    } else {
                        callback.onPermissionDenied()
                        callback.onShizukuError("Shizuku permission denied")
                    }
                }
            } else {
                // Android 7-10
                initializeShizuku(context, callback)
            }
        } else {
            callback.onShizukuError("Shizuku requires Android 7.0+ (API 24)")
        }
    }

    private fun initializeShizuku(context: Context, callback: ShizukuCallback) {
        if (ShizukuClient.isStarted()) {
            callback.onShizukuReady()
            return
        }

        ShizukuClient.start(context)
        
        // Wait for Shizuku to be ready
        val thread = Thread {
            var attempts = 0
            while (!ShizukuClient.isStarted() && attempts < 100) {
                Thread.sleep(100)
                attempts++
            }
            
            if (ShizukuClient.isStarted()) {
                // Check if we have the required permissions
                if (hasRequiredPermissions()) {
                    callback.onShizukuReady()
                } else {
                    callback.onShizukuError("Shizuku started but missing required permissions")
                }
            } else {
                callback.onShizukuError("Shizuku failed to start after timeout")
            }
        }
        thread.start()
    }

    private fun hasRequiredPermissions(): Boolean {
        // Check if we can use AppOps
        return try {
            val binder = ShizukuClient.getSystemService(Context.APP_OPS_SERVICE) as android.os.IBinder
            binder != null
        } catch (e: Exception) {
            false
        }
    }

    fun setAppLocationMode(packageName: String, allow: Boolean): Boolean {
        return if (ShizukuClient.isStarted()) {
            try {
                val appOps = getAppOpsService()
                val uid = getPackageUid(packageName)
                if (uid == -1) return false
                
                val mode = if (allow) MODE_ALLOWED else MODE_IGNORED
                
                // Use VersionCompat for maximum compatibility
                VersionCompat.setAppOpsMode(appOps, OP_COARSE_LOCATION, uid, packageName, mode)
                VersionCompat.setAppOpsMode(appOps, OP_FINE_LOCATION, uid, packageName, mode)
                VersionCompat.setAppOpsMode(appOps, OP_GPS, uid, packageName, mode)
                
                Log.d(TAG, "Set location mode for $packageName to ${if (allow) "ALLOWED" else "IGNORED"}")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set app location mode", e)
                false
            }
        } else {
            false
        }
    }

    fun setAppMockLocationMode(packageName: String, allow: Boolean): Boolean {
        return if (ShizukuClient.isStarted()) {
            try {
                val appOps = getAppOpsService()
                val uid = getPackageUid(packageName)
                if (uid == -1) return false
                
                val mode = if (allow) MODE_ALLOWED else MODE_IGNORED
                VersionCompat.setAppOpsMode(appOps, OP_MOCK_LOCATION, uid, packageName, mode)
                
                Log.d(TAG, "Set mock location mode for $packageName to ${if (allow) "ALLOWED" else "IGNORED"}")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set app mock location mode", e)
                false
            }
        } else {
            false
        }
    }

    fun getAppLocationMode(packageName: String): Int {
        return if (ShizukuClient.isStarted()) {
            try {
                val appOps = getAppOpsService()
                val uid = getPackageUid(packageName)
                if (uid == -1) return MODE_DEFAULT
                
                val coarseMode = VersionCompat.checkLocationAppOps(appOps, uid, packageName)
                
                return coarseMode
            } catch (e: Exception) {
                MODE_DEFAULT
            }
        } else {
            MODE_DEFAULT
        }
    }

    fun getAppMockLocationMode(packageName: String): Int {
        return if (ShizukuClient.isStarted()) {
            try {
                val appOps = getAppOpsService()
                val uid = getPackageUid(packageName)
                if (uid == -1) return MODE_DEFAULT
                
                return VersionCompat.checkMockLocationAppOps(appOps, uid, packageName)
            } catch (e: Exception) {
                MODE_DEFAULT
            }
        } else {
            MODE_DEFAULT
        }
    }

    fun setPerAppMockLocation(
        packageName: String,
        latitude: Double,
        longitude: Double,
        altitude: Double? = null,
        accuracy: Float = 10f
    ): Boolean {
        // Configure AppOps for per-app mock location
        // Works on Android 7.0+ with Shizuku
        return if (ShizukuClient.isStarted()) {
            try {
                val appOps = getAppOpsService()
                val uid = getPackageUid(packageName)
                if (uid == -1) return false
                
                // Allow mock location for this app
                VersionCompat.setAppOpsMode(appOps, OP_MOCK_LOCATION, uid, packageName, MODE_ALLOWED)
                // Allow location access
                VersionCompat.setAppOpsMode(appOps, OP_COARSE_LOCATION, uid, packageName, MODE_ALLOWED)
                VersionCompat.setAppOpsMode(appOps, OP_FINE_LOCATION, uid, packageName, MODE_ALLOWED)
                
                Log.d(TAG, "Configured per-app mock location for $packageName")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set per-app mock location", e)
                false
            }
        } else {
            false
        }
    }

    private fun getAppOpsService(): AppOpsManager {
        val binder = ShizukuClient.getSystemService(Context.APP_OPS_SERVICE) as android.os.IBinder
        return AppOpsManager.wrap(binder)
    }

    private fun getPackageUid(packageName: String): Int {
        return try {
            VersionCompat.getPackageUid(ShizukuClient.getContext() ?: 
                throw IllegalStateException("Shizuku context not available"), packageName)
        } catch (e: Exception) {
            -1
        }
    }

    fun getInstalledApps(context: Context): List<AppInfo> {
        val apps = mutableListOf<AppInfo>()
        if (ShizukuClient.isStarted()) {
            try {
                val pm = ShizukuClient.getSystemService(Context.PACKAGE_SERVICE) as android.content.pm.IPackageManager
                val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA, ProcessCompat.myUserId())
                
                for (pkg in packages) {
                    if ((pkg.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                        apps.add(AppInfo(
                            packageName = pkg.packageName,
                            label = pkg.applicationInfo.loadLabel(context.packageManager).toString(),
                            icon = pkg.applicationInfo.loadIcon(context.packageManager),
                            uid = pkg.applicationInfo.uid
                        ))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get installed apps via Shizuku", e)
            }
        }
        return apps
    }

    data class AppInfo(
        val packageName: String,
        val label: String,
        val icon: android.graphics.drawable.Drawable?,
        val uid: Int
    )
}