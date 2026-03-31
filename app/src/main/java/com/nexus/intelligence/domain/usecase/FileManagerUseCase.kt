package com.nexus.intelligence.domain.usecase

import com.nexus.intelligence.data.local.dao.DocumentDao
import com.nexus.intelligence.domain.model.ApplyResult
import com.nexus.intelligence.domain.model.FileCluster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileManagerUseCase @Inject constructor(
    private val documentDao: DocumentDao,
    private val organizerUseCase: FileOrganizerUseCase
) {

    // ── Aplicar organización completa ────────────────────────────────

    suspend fun applyOrganization(
        approvedClusters: List<FileCluster>,
        masterFolderPath: String
    ): ApplyResult = withContext(Dispatchers.IO) {
        var moved = 0
        var renamed = 0
        val failed = mutableListOf<String>()

        val masterFolder = File(masterFolderPath)
        if (!masterFolder.exists()) {
            masterFolder.mkdirs()
        }

        for (cluster in approvedClusters) {
            val clusterFolder = File(masterFolder, cluster.folderName)
            if (!clusterFolder.exists()) clusterFolder.mkdirs()

            for (doc in cluster.documents) {
                try {
                    val sourceFile = File(doc.filePath)
                    if (!sourceFile.exists()) {
                        failed.add("No encontrado: ${doc.fileName}")
                        continue
                    }

                    // Determinar nombre final del archivo
                    val finalName = if (organizerUseCase.isGenericName(doc.fileName)) {
                        val ext = doc.fileName.substringAfterLast(".", "")
                        val baseName = "${cluster.folderName}_${doc.id}"
                        if (ext.isNotEmpty()) "$baseName.$ext" else baseName
                    } else {
                        doc.fileName
                    }

                    val wasRenamed = finalName != doc.fileName
                    val destFile = resolveConflict(File(clusterFolder, finalName))

                    // Mover archivo en disco
                    val success = sourceFile.renameTo(destFile)
                        || copyAndDelete(sourceFile, destFile)

                    if (success) {
                        // Actualizar ruta en la DB
                        val entity = documentDao.getDocumentByPath(doc.filePath)
                        if (entity != null) {
                            documentDao.updateDocument(
                                entity.copy(
                                    filePath = destFile.absolutePath,
                                    fileName = destFile.name,
                                    parentDirectory = destFile.parent ?: ""
                                )
                            )
                        }
                        moved++
                        if (wasRenamed) renamed++
                    } else {
                        failed.add("Error moviendo: ${doc.fileName}")
                    }
                } catch (e: Exception) {
                    failed.add("${doc.fileName}: ${e.message}")
                }
            }
        }

        ApplyResult(moved = moved, renamed = renamed, failed = failed.size, failedFiles = failed)
    }

    // ── Crear carpeta manualmente ────────────────────────────────────

    suspend fun createFolder(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            File(path).mkdirs()
        } catch (e: Exception) {
            false
        }
    }

    // ── Mover archivo individual ─────────────────────────────────────

    suspend fun moveFile(docId: Long, destinationFolder: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val entity = documentDao.getDocumentById(docId) ?: return@withContext false
            val source = File(entity.filePath)
            if (!source.exists()) return@withContext false

            val destFolder = File(destinationFolder)
            if (!destFolder.exists()) destFolder.mkdirs()

            val dest = resolveConflict(File(destFolder, entity.fileName))
            val success = source.renameTo(dest) || copyAndDelete(source, dest)

            if (success) {
                documentDao.updateDocument(
                    entity.copy(
                        filePath = dest.absolutePath,
                        parentDirectory = dest.parent ?: ""
                    )
                )
            }
            success
        } catch (e: Exception) {
            false
        }
    }

    // ── Renombrar archivo ────────────────────────────────────────────

    suspend fun renameFile(docId: Long, newName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val entity = documentDao.getDocumentById(docId) ?: return@withContext false
            val source = File(entity.filePath)
            if (!source.exists()) return@withContext false

            // Preservar extensión si el nuevo nombre no la incluye
            val ext = entity.fileName.substringAfterLast(".", "")
            val finalName = if (ext.isNotEmpty() && !newName.endsWith(".$ext")) {
                "$newName.$ext"
            } else {
                newName
            }

            val dest = File(source.parent ?: "", sanitizeFileName(finalName))
            if (dest.exists()) return@withContext false // No sobrescribir sin confirmar

            val success = source.renameTo(dest)
            if (success) {
                documentDao.updateDocument(
                    entity.copy(
                        filePath = dest.absolutePath,
                        fileName = dest.name
                    )
                )
            }
            success
        } catch (e: Exception) {
            false
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────

    // Si ya existe un archivo con ese nombre, agrega _2, _3, etc.
    private fun resolveConflict(file: File): File {
        if (!file.exists()) return file

        val base = file.nameWithoutExtension
        val ext = file.extension
        var counter = 2
        var candidate: File

        do {
            candidate = File(
                file.parent ?: "",
                if (ext.isNotEmpty()) "${base}_$counter.$ext" else "${base}_$counter"
            )
            counter++
        } while (candidate.exists() && counter < 100)

        return candidate
    }

    // Fallback para sistemas de archivos que no soportan rename cross-partition
    private fun copyAndDelete(source: File, dest: File): Boolean {
        return try {
            source.copyTo(dest, overwrite = false)
            source.delete()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[/\\\\:*?\"<>|]"), "_").trim()
    }
}
