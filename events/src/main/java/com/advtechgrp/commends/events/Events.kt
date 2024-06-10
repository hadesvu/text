package com.advtechgrp.commends.events

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import com.advtechgrp.commends.settings.CommonSettingsLocalDataSourceImpl
import com.advtechgrp.commends.settings.SettingsContentProviderLocalDataSource
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

interface EventsLocalDataSource {
    suspend fun insert(name: String, data: String, correlationId: String)
    suspend fun insert(name: String, data: Any)
    fun insertEvent(name: String, data: String, correlationId: String)
    fun insertEvent(name: String, data: Any)
}

class EventsLocalDataSourceImpl @Inject constructor(
    private val context: Context,
    private val dispatcherIO: CoroutineDispatcher = Dispatchers.IO
) : EventsLocalDataSource {

    private val _authority = "com.advtechgrp.operations.events.provider"
    private val _tableName = "events"
    private val _contentUriBase = Uri.parse("content://$_authority")
    private val _contentUri = Uri.withAppendedPath(_contentUriBase, _tableName)
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val commonSettings = CommonSettingsLocalDataSourceImpl(
        SettingsContentProviderLocalDataSource(context, dispatcherIO),
        context,
        dispatcherIO
    )

    object Columns {
        const val UNIQUE_ID = "uniqueId"
        const val NAME = "name"
        const val TIMESTAMP = "timestamp"
        const val USER = "user"
        const val WORKSTATION = "workstation"
        const val DATA = "data"
        const val CORRELATION_ID = "correlationId"
        const val APPLICATION_VERSION = "applicationVersion"
        const val APPLICATION_NAME = "applicationName"
    }

    override suspend fun insert(name: String, data: String, correlationId: String) {
        withContext(dispatcherIO) {
            // The events content provider might not be available
            // This can happen if the code to insert events has been
            // deployed but the operations app where the provider
            // resides has not been updated yet.
            // Or more likely you are running on an emulator
            val client = context.contentResolver.acquireContentProviderClient(_contentUri)
                ?: return@withContext
            if (Build.VERSION.SDK_INT >= 24) {
                //Added in API level 24
                client.close()
            }


            val applicationInformation = commonSettings.applicationInformation()

            val eventValues = ContentValues()
            eventValues.put(Columns.NAME, name)
            eventValues.put(Columns.UNIQUE_ID, UUID.randomUUID().toString())
            eventValues.put(Columns.TIMESTAMP, DateTime.now().millis)
            eventValues.put(Columns.DATA, data)
            eventValues.put(
                Columns.USER, commonSettings.formatAsUserName(
                    commonSettings.registerNumber(),
                    commonSettings.serial(),
                    commonSettings.mediaDeviceId(commonSettings.serial()),
                )
            )
            eventValues.put(Columns.WORKSTATION, commonSettings.workstationName())
            eventValues.put(Columns.CORRELATION_ID, correlationId)
            eventValues.put(
                Columns.APPLICATION_VERSION, applicationInformation.displayVersion()
            )
            eventValues.put(
                Columns.APPLICATION_NAME, applicationInformation.name
            )

            context.contentResolver.insert(_contentUri, eventValues)
        }
    }

    override suspend fun insert(name: String, data: Any) = withContext(dispatcherIO) {
        try {
            val gson = GsonBuilder().create()
            val json = gson.toJson(data)
            insert(name = name, data = json, UUID.randomUUID().toString())
            Timber.i(name)
            Timber.i(json)
        } catch (t: Throwable) {
            Timber.e(t, "Error inserting event")
        }
    }

    override fun insertEvent(name: String, data: String, correlationId: String) {
        executor.execute {
            runBlocking {
                insert(name, data, correlationId)
            }
        }
    }

    override fun insertEvent(name: String, data: Any) {
        executor.execute {
            runBlocking {
                insert(name, data)
            }
        }
    }
}