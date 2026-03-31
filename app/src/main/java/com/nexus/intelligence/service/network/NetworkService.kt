package com.nexus.intelligence.service.network

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.IBinder
import com.nexus.intelligence.domain.model.NetworkDevice
import com.nexus.intelligence.domain.repository.DocumentRepository
import dagger.hilt.android.AndroidEntryPoint
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.gson.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

// ── Network Discovery Manager ────────────────────────────────────

@Singleton
class NexusNetworkManager @Inject constructor() {

    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    private val _discoveredDevices = MutableStateFlow<List<NetworkDevice>>(emptyList())
    val discoveredDevices = _discoveredDevices.asStateFlow()

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning = _isServerRunning.asStateFlow()

    companion object {
        const val SERVICE_TYPE = "_nexus._tcp."
        const val SERVICE_NAME = "NEXUS-Intelligence"
    }

    fun initialize(context: Context) {
        nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    fun registerService(port: Int) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "${SERVICE_NAME}-${Build.MODEL}"
            serviceType = SERVICE_TYPE
            setPort(port)
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                _isServerRunning.value = true
            }

            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                _isServerRunning.value = false
            }

            override fun onServiceUnregistered(info: NsdServiceInfo) {
                _isServerRunning.value = false
            }

            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {}
        }

        nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    fun startDiscovery() {
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {}

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType == SERVICE_TYPE) {
                    nsdManager?.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {}

                        override fun onServiceResolved(info: NsdServiceInfo) {
                            val device = NetworkDevice(
                                name = info.serviceName,
                                address = info.host?.hostAddress ?: "",
                                port = info.port,
                                isConnected = true
                            )
                            val current = _discoveredDevices.value.toMutableList()
                            if (current.none { it.address == device.address }) {
                                current.add(device)
                                _discoveredDevices.value = current
                            }
                        }
                    })
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                val current = _discoveredDevices.value.toMutableList()
                current.removeAll { it.name == serviceInfo.serviceName }
                _discoveredDevices.value = current
            }

            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }

        nsdManager?.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    fun stopDiscovery() {
        try {
            discoveryListener?.let { nsdManager?.stopServiceDiscovery(it) }
        } catch (e: Exception) { /* Already stopped */ }
    }

    fun unregisterService() {
        try {
            registrationListener?.let { nsdManager?.unregisterService(it) }
        } catch (e: Exception) { /* Already unregistered */ }
        _isServerRunning.value = false
    }

    fun cleanup() {
        stopDiscovery()
        unregisterService()
    }
}

// ── Local HTTP Server (Ktor CIO) ─────────────────────────────────

@Singleton
class NexusLocalServer @Inject constructor(
    private val repository: DocumentRepository,
    private val networkManager: NexusNetworkManager
) {
    private var server: ApplicationEngine? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start(port: Int = 9090) {
        if (server != null) return

        server = embeddedServer(CIO, port = port, host = "0.0.0.0") {
            install(ContentNegotiation) {
                gson {
                    setPrettyPrinting()
                }
            }

            routing {
                // Health check
                get("/health") {
                    call.respond(mapOf("status" to "online", "service" to "NEXUS Intelligence"))
                }

                // List all documents
                get("/api/documents") {
                    val documents = repository.getAllDocuments().first()
                    call.respond(documents)
                }

                // Search documents
                get("/api/search") {
                    val query = call.parameters["q"] ?: ""
                    if (query.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Query parameter 'q' is required"))
                        return@get
                    }
                    val results = repository.textSearch(query)
                    call.respond(results)
                }

                // Get document by ID
                get("/api/documents/{id}") {
                    val id = call.parameters["id"]?.toLongOrNull()
                    if (id == null) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid document ID"))
                        return@get
                    }
                    val doc = repository.getDocumentById(id)
                    if (doc != null) {
                        call.respond(doc)
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Document not found"))
                    }
                }

                // Download file
                get("/api/files/{id}") {
                    val id = call.parameters["id"]?.toLongOrNull()
                    if (id == null) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
                        return@get
                    }
                    val doc = repository.getDocumentById(id)
                    if (doc != null) {
                        val file = File(doc.filePath)
                        if (file.exists()) {
                            call.respondFile(file)
                        } else {
                            call.respond(HttpStatusCode.NotFound, mapOf("error" to "File not found on disk"))
                        }
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Document not found"))
                    }
                }

                // Stats
                get("/api/stats") {
                    val count = repository.getDocumentCount().first()
                    call.respond(mapOf(
                        "totalDocuments" to count,
                        "deviceName" to Build.MODEL
                    ))
                }
            }
        }

        scope.launch {
            server?.start(wait = false)
            networkManager.registerService(port)
        }
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
        networkManager.unregisterService()
    }

    fun isRunning(): Boolean = server != null
}

// ── Network Discovery Service (Android Service) ──────────────────

@AndroidEntryPoint
class NetworkDiscoveryService : Service() {

    @Inject
    lateinit var networkManager: NexusNetworkManager

    @Inject
    lateinit var localServer: NexusLocalServer

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        networkManager.initialize(this)
        networkManager.startDiscovery()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        networkManager.cleanup()
        localServer.stop()
    }
}
