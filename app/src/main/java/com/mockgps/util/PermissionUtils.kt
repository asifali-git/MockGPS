package com.mockgps.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

object PermissionUtils {
    val LOCATION_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    val BACKGROUND_LOCATION_PERMISSION = Manifest.permission.ACCESS_BACKGROUND_LOCATION
    val MOCK_LOCATION_PERMISSION = "android.permission.MOCK_LOCATION"

    fun hasLocationPermissions(context: Context): Boolean {
        return VersionCompat.hasLocationPermissions(context)
    }

    fun hasBackgroundLocationPermission(context: Context): Boolean {
        return VersionCompat.hasBackgroundLocationPermission(context)
    }

    fun hasMockLocationPermission(context: Context): Boolean {
        return VersionCompat.hasMockLocationPermission(context)
    }

    fun requestLocationPermissions(activity: Activity, requestCode: Int) {
        activity.requestPermissions(LOCATION_PERMISSIONS, requestCode)
    }

    fun requestBackgroundLocationPermission(activity: Activity, requestCode: Int) {
        activity.requestPermissions(arrayOf(BACKGROUND_LOCATION_PERMISSION), requestCode)
    }

    fun openMockLocationSettings(context: Context) {
        VersionCompat.requestMockLocationPermission(context)
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openDeveloperOptions(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}