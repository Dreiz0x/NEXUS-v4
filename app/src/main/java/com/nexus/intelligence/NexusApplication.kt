package com.nexus.intelligence

import android.app.Application
import android.os.Environment
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltAndroidApp
class NexusApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupCrashHandler()
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val stacktrace = sw.toString()

                val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
                val crashLog = """
NEXUS CRASH REPORT - $timestamp
Thread: ${thread.name}
========================================
$stacktrace
========================================
""".trimIndent()

                // Intentar escribir en Downloads
                val downloadsDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )
                val crashFile = File(downloadsDir, "nexus_crash_$timestamp.txt")
                crashFile.writeText(crashLog)
            } catch (e: Exception) {
                // Si falla escribir el archivo, al menos intentamos el handler original
            }

            // Propagar al handler original para que el sistema muestre el diálogo de error
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
