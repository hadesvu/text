package com.advtechgrp.commends.settings

import android.annotation.SuppressLint
import android.content.ContentProviderClient
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.advtechgrp.commends.events.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

data class ApplicationInformation(
    val name: String, val versionCode: Long, val versionName: String
) {
    fun displayVersion(): String {
        return String.format("%s (%d)", versionName, versionCode)
    }
}

interface SettingsLocalDatasource {
    suspend fun value(name: String): String?
    suspend fun store(name: String, value: String)
}

interface CommonSettingsLocalDataSource {
    suspend fun deviceToken(): String
    suspend fun textBooksBaseUrl(): String
    suspend fun baseUrl():String
    suspend fun applicationInformation(): ApplicationInformation
    suspend fun isSharedOrAssignedDevice(): Boolean
    suspend fun serial(): String
    suspend fun workstationName(): String
    suspend fun mediaDeviceId(fromSerialNumber: String): Int
    suspend fun model(): String
    suspend fun systemVersion(): String
    suspend fun locale(): String
    suspend fun registerNumber(): String
    fun formatAsUserName(
        registerNumber: String?, serialNumber: String, mediaDeviceId: Int
    ): String
}

class SettingsContentProviderLocalDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatcherIO: CoroutineDispatcher = Dispatchers.IO
) : SettingsLocalDatasource {

    private val _authority = "com.advtechgrp.session.settings.provider"
    private val _tableName = "settings"
    private val _contentUriBase = Uri.parse("content://$_authority")
    private val _contentUri = Uri.withAppendedPath(_contentUriBase, _tableName)

    override suspend fun value(name: String): String? = withContext(dispatcherIO) {
        var v: String? = null
        context.contentResolver.query(
            _contentUri, arrayOf("value"), " name = ? ", arrayOf(name), null
        ).use { c ->
            if (c != null) {
                if (c.moveToFirst()) {
                    v = c.getString(0)
                }
            }
        }
        return@withContext v
    }

    override suspend fun store(name: String, value: String): Unit = withContext(dispatcherIO) {
        val setting = ContentValues()
        setting.put("name", name)
        setting.put("value", value)
        requireNotNull(
            context.contentResolver.insert(
                _contentUri, setting
            )
        )
    }
}

class CommonSettingsLocalDataSourceImpl @Inject constructor(
    private val settingsLocalDatasource: SettingsLocalDatasource,
    private val context: Context,
    private val dispatcherIO: CoroutineDispatcher = Dispatchers.IO
) : CommonSettingsLocalDataSource {

    override suspend fun deviceToken(): String = withContext(dispatcherIO) {
        return@withContext settingsLocalDatasource.value("deviceToken")!!
    }

    override suspend fun textBooksBaseUrl() = withContext(dispatcherIO) {
        return@withContext settingsLocalDatasource.value("baseUrlTextbooks")!!
    }

    override suspend fun baseUrl(): String = withContext(dispatcherIO) {
        return@withContext settingsLocalDatasource.value("baseUrl")!!
    }

    override suspend fun applicationInformation(): ApplicationInformation =
        withContext(dispatcherIO) {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName, PackageManager.PackageInfoFlags.of(0L)
                )
            } else {
                context.packageManager.getPackageInfo(
                    context.packageName, 0
                )
            }
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION") packageInfo.versionCode.toLong()
            }
            return@withContext ApplicationInformation(
                name = packageInfo.packageName,
                versionCode = code,
                versionName = packageInfo.versionName
            )
        }

    override suspend fun isSharedOrAssignedDevice(): Boolean {
        val contentResolver = context.contentResolver
        val path = Uri.withAppendedPath(
            Uri.parse("content://com.advtechgrp.device.settings.provider"), "settings"
        )
        val client: ContentProviderClient? = contentResolver.acquireContentProviderClient(path)
        if (client != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                client.close()
            }
            return true
        }
        return false
    }

    @SuppressLint("HardwareIds", "MissingPermission")
    @Suppress("DEPRECATION")
    override suspend fun serial(): String = withContext(dispatcherIO) {
        if (BuildConfig.DEBUG && Build.PRODUCT.startsWith("sdk_")) {
            return@withContext "unknown"
        }
        return@withContext if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            try {
                Build.getSerial()
            } catch (e: SecurityException) {
                "unknown"
            }
        } else {
            Build.SERIAL
        }
    }

    @SuppressLint("HardwareIds")
    override suspend fun workstationName(): String = withContext(dispatcherIO) {
        return@withContext "a-" + Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ANDROID_ID
        )
    }

    override suspend fun mediaDeviceId(fromSerialNumber: String): Int = withContext(dispatcherIO) {
        val storedValue = settingsLocalDatasource.value("mediaDeviceId")
        if (storedValue == null) {
            require(fromSerialNumber.length >= 5)
            return@withContext when (fromSerialNumber.substring(0, 3)) {
                "346", "446" -> 6
                "708" -> 9
                "710" -> 13
                "508" -> 10
                else -> when (fromSerialNumber.substring(0, 5)) {
                    "70617" -> 8
                    "70619" -> 11
                    else -> {
                        throw IllegalArgumentException("could not parse mediaDeviceId from $fromSerialNumber")
                    }
                }
            }
        } else {
            return@withContext storedValue.toInt()
        }
    }

    override suspend fun model(): String {
        return Build.MODEL
    }

    override suspend fun systemVersion(): String {
        return Build.VERSION.RELEASE
    }

    override suspend fun locale(): String {
        return Locale.getDefault().toString()
    }

    override suspend fun registerNumber(): String = withContext(dispatcherIO) {
        return@withContext settingsLocalDatasource.value("registerNumber")!!
    }

    override fun formatAsUserName(
        registerNumber: String?,
        serialNumber: String,
        mediaDeviceId: Int
    ): String {
        return registerNumber ?: "$mediaDeviceId:$serialNumber"
    }
}