package com.nexus.intelligence.service.indexing

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.FileObserver
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.nexus.intelligence.R
import com.nexus.intelligence.domain.usecase.IndexDocumentsUseCase
import com.nexus.intelligence.domain.usecase.ManageSettingsUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class IndexingService : Service() {

    @Inject
    lateinit var indexDocumentsUseCase: IndexDocumentsUseCase

    @Inject
    lateinit var manageSettingsUseCase: ManageSettingsUseCase

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val fileObservers = mutableListOf<FileObserver>()

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "nexus_indexing"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_SCAN = "com.nexus.intelligence.START_SCAN"
        const val ACTION_STOP = "com.nexus.intelligence.STOP_SERVICE"
        const val EXTRA_DIRECTORIES = "directories"

        fun startScan(context: Context, directories: List<String>) {
            val intent = Intent(context, IndexingService::class.java).apply {
                action = ACTION_START_SCAN
                putStringArrayListExtra(EXTRA_DIRECTORIES, ArrayList(directories))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, IndexingService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SCAN -> {
                val directories = intent.getStringArrayListExtra(EXTRA_DIRECTORIES)
                // Sin directories en el intent = no escanear nada automáticamente
                // El usuario debe configurar carpetas explícitamente en Settings
                if (directories.isNullOrEmpty()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                startForegroundNotification()
                startIndexing(directories)
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startIndexing(directories: List<String>) {
        serviceScope.launch {
            try {
                launch {
                    indexDocumentsUseCase.progress.collectLatest { progress ->
                        updateNotification(
                            "Escaneando: ${progress.currentFile}",
                            "${progress.processedFiles}/${progress.totalFiles}"
                        )
                    }
                }

                val totalIndexed = indexDocumentsUseCase.fullScan(directories)

                updateNotification(
                    "Escaneo completo",
                    "$totalIndexed documentos indexados"
                )

                setupFileWatchers(directories)
                delay(2000)

            } catch (e: Exception) {
                updateNotification("Error en escaneo", e.message ?: "Error desconocido")
            }
        }
    }

    private fun setupFileWatchers(directories: List<String>) {
        fileObservers.forEach { it.stopWatching() }
        fileObservers.clear()

        for (dirPath in directories) {
            try {
                val observer = object : FileObserver(File(dirPath), CREATE or MODIFY or DELETE or MOVED_FROM or MOVED_TO) {
                    override fun onEvent(event: Int, path: String?) {
                        if (path == null) return
                        val file = File(dirPath, path)

                        serviceScope.launch {
                            when (event) {
                                CREATE, MODIFY, MOVED_TO -> {
                                    if (file.isFile &&
                                        file.extension.lowercase() in com.nexus.intelligence.data.parser.DocumentParser.SUPPORTED_EXTENSIONS &&
                                        !file.name.startsWith(".")
                                    ) {
                                        indexDocumentsUseCase.indexSingleFile(file)
                                    }
                                }
                                DELETE, MOVED_FROM -> {
                                    // La limpieza se hace en el próximo fullScan
                                }
                            }
                        }
                    }
                }
                observer.startWatching()
                fileObservers.add(observer)
            } catch (e: Exception) {
                // Carpeta sin permisos, ignorar
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_indexing),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Progreso de indexado de documentos"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundNotification() {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_indexing_title))
            .setContentText(getString(R.string.notification_indexing_text))
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(title: String, text: String) {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        fileObservers.forEach { it.stopWatching() }
        fileObservers.clear()
        serviceScope.cancel()
    }
}

// ── Boot Receiver ────────────────────────────────────────────────

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // NO escanear root del storage en boot
            // El IndexingService sin directories en el intent se detiene solo
            // Si el usuario quiere scan en boot, debe venir de las carpetas configuradas
            // Eso se maneja desde el ViewModel al arrancar la app, no aquí
        }
    }
}
