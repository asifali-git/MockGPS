package com.mockgps.shizuku

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.mockgps.util.VersionCompat

object ShizukuHelper {
    private const val TAG = "MockGPS_Shizuku"
    private const val SHIZUKU_PERMISSION_REQUEST_CODE = 1001

    interface ShizukuCallback {
        fun onShizukuReady()
        fun onShizukuError(error: String)
        fun onPermissionGranted()
        fun onPermissionDenied()
    }

    fun isShizukuAvailable(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("moe.shizuku.manager", 0) != null
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun isShizukuInstalled(context: Context): Boolean {
        return isShizukuAvailable(context)
    }

    fun checkSelfPermission(context: Context): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                context.checkSelfPermission("moe.shizuku.permission.API_V23")
            } else {
                PackageManager.PERMISSION_GRANTED
            }
        } catch (e: Exception) {
            PackageManager.PERMISSION_DENIED
        }
    }

    fun requestPermission(context: Context, callback: ShizukuCallback) {
        try {
            if (!isShizukuAvailable(context)) {
                callback.onShizukuError("Shizuku not installed. Please install Shizuku manager app.")
                return
            }

            val granted = checkSelfPermission(context) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                callback.onPermissionGranted()
                callback.onShizukuReady()
                return
            }

            callback.onShizukuError("Shizuku permission not granted. Please open Shizuku manager and grant permission.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request Shizuku permission", e)
            callback.onShizukuError("Shizuku error: ${e.message}")
        }
    }

    fun setAppLocationMode(context: Context, packageName: String, allow: Boolean): Boolean {
        return try {
            val uid = getUid(context, packageName)
            if (uid == -1) return false

            val mode = if (allow) "allow" else "deny"
            val ops = listOf("coarse_location", "fine_location", "gps")

            ops.forEach { op ->
                execAppOps(uid, op, mode)
            }

            Log.d(TAG, "Set location mode for $packageName to $mode")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set app location mode", e)
            false
        }
    }

    fun setAppMockLocationMode(context: Context, packageName: String, allow: Boolean): Boolean {
        return try {
            val uid = getUid(context, packageName)
            if (uid == -1) return false

            val mode = if (allow) "allow" else "deny"
            execAppOps(uid, "mock_location", mode)

            Log.d(TAG, "Set mock location mode for $packageName to $mode")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set app mock location mode", e)
            false
        }
    }

    fun setPerAppMockLocation(context: Context, packageName: String, lat: Double, lng: Double, alt: Double? = null, acc: Float = 10f): Boolean {
        return try {
            val uid = getUid(context, packageName)
            if (uid == -1) return false

            execAppOps(uid, "mock_location", "allow")
            execAppOps(uid, "coarse_location", "allow")
            execAppOps(uid, "fine_location", "allow")

            Log.d(TAG, "Configured per-app mock location for $packageName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set per-app mock location", e)
            false
        }
    }

    private fun execAppOps(uid: Int, op: String, mode: String) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "appops set $uid $op $mode"))
            process.waitFor()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to exec appops: $op $mode for uid $uid")
        }
    }

    private fun getUid(context: Context, packageName: String): Int {
        return VersionCompat.getPackageUid(context, packageName)
    }

    fun getInstalledApps(context: Context): List<AppInfo> {
        val apps = mutableListOf<AppInfo>()
        try {
            val packages = context.packageManager.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
            for (app in packages) {
                if ((app.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) {
                    apps.add(
                        AppInfo(
                            packageName = app.packageName,
                            label = context.packageManager.getApplicationLabel(app).toString(),
                            icon = context.packageManager.getApplicationIcon(app)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "getInstalledApps failed", e)
        }
        return apps
    }

    data class AppInfo(val packageName: String, val label: String, val icon: android.graphics.drawable.Drawable?)
}
