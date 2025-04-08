package com.eazydelivery.app.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import com.eazydelivery.app.data.local.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages database backup and restore operations
 */
@Singleton
class DatabaseBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securityManager: SecurityManager,
    private val errorHandler: ErrorHandler
) {
    companion object {
        private const val BACKUP_FILE_PREFIX = "eazydelivery_backup_"
        private const val BACKUP_FILE_EXTENSION = ".zip"
        private const val DATABASE_NAME = "eazydelivery_db"
        private const val BACKUP_VERSION = "1"
        private const val METADATA_FILE = "backup_metadata.json"
        private const val BUFFER_SIZE = 8192
    }

    /**
     * Creates a backup of the database
     * @param destinationUri Optional URI to save the backup to. If null, the backup is saved to the app's files directory.
     * @return The URI of the backup file, or null if the backup failed
     */
    suspend fun createBackup(destinationUri: Uri? = null): Uri? = withContext(Dispatchers.IO) {
        try {
            // Close the database to ensure all data is written
            closeDatabase()

            // Get the database file
            val databaseFile = context.getDatabasePath(DATABASE_NAME)
            if (!databaseFile.exists()) {
                Timber.e("Database file not found: ${databaseFile.absolutePath}")
                return@withContext null
            }

            // Create a timestamp for the backup file name
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val backupFileName = "$BACKUP_FILE_PREFIX$timestamp$BACKUP_FILE_EXTENSION"

            // Determine the output stream based on the destination URI
            val outputStream = if (destinationUri != null) {
                // Use the provided URI
                context.contentResolver.openOutputStream(destinationUri)
            } else {
                // Create a file in the app's files directory
                val backupDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "backups")
                if (!backupDir.exists()) {
                    backupDir.mkdirs()
                }
                val backupFile = File(backupDir, backupFileName)
                FileOutputStream(backupFile)
            }

            // Create a ZIP file containing the database and metadata
            outputStream?.use { os ->
                ZipOutputStream(os).use { zipOut ->
                    // Add the database file to the ZIP
                    addFileToZip(zipOut, databaseFile, DATABASE_NAME)

                    // Add metadata to the ZIP
                    val metadata = createBackupMetadata()
                    addStringToZip(zipOut, METADATA_FILE, metadata)
                }
            }

            // Return the URI of the backup file
            if (destinationUri != null) {
                destinationUri
            } else {
                val backupDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "backups")
                val backupFile = File(backupDir, backupFileName)
                Uri.fromFile(backupFile)
            }
        } catch (e: Exception) {
            errorHandler.handleException("DatabaseBackupManager.createBackup", e)
            null
        }
    }

    /**
     * Restores the database from a backup
     * @param backupUri The URI of the backup file
     * @return true if the restore was successful, false otherwise
     */
    suspend fun restoreBackup(backupUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            // Close the database to ensure all data is written
            closeDatabase()

            // Get the database file
            val databaseFile = context.getDatabasePath(DATABASE_NAME)
            val databaseDir = databaseFile.parentFile

            // Create a temporary directory for extraction
            val tempDir = File(context.cacheDir, "backup_temp")
            if (tempDir.exists()) {
                tempDir.deleteRecursively()
            }
            tempDir.mkdirs()

            // Extract the backup file
            val inputStream = context.contentResolver.openInputStream(backupUri)
            inputStream?.use { input ->
                ZipInputStream(input).use { zipIn ->
                    var entry: ZipEntry? = zipIn.nextEntry
                    while (entry != null) {
                        val newFile = File(tempDir, entry.name)
                        if (entry.isDirectory) {
                            newFile.mkdirs()
                        } else {
                            // Create parent directories if needed
                            newFile.parentFile?.mkdirs()

                            // Extract the file
                            FileOutputStream(newFile).use { fos ->
                                val buffer = ByteArray(BUFFER_SIZE)
                                var len: Int
                                while (zipIn.read(buffer).also { len = it } > 0) {
                                    fos.write(buffer, 0, len)
                                }
                            }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            }

            // Verify the backup metadata
            val metadataFile = File(tempDir, METADATA_FILE)
            if (!metadataFile.exists()) {
                Timber.e("Backup metadata file not found")
                return@withContext false
            }

            val metadata = metadataFile.readText()
            if (!verifyBackupMetadata(metadata)) {
                Timber.e("Invalid backup metadata")
                return@withContext false
            }

            // Copy the database file to the database directory
            val extractedDbFile = File(tempDir, DATABASE_NAME)
            if (!extractedDbFile.exists()) {
                Timber.e("Extracted database file not found")
                return@withContext false
            }

            // Delete the existing database file
            if (databaseFile.exists()) {
                databaseFile.delete()
            }

            // Copy the extracted database file to the database directory
            FileInputStream(extractedDbFile).use { input ->
                FileOutputStream(databaseFile).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var len: Int
                    while (input.read(buffer).also { len = it } > 0) {
                        output.write(buffer, 0, len)
                    }
                }
            }

            // Clean up the temporary directory
            tempDir.deleteRecursively()

            // Reopen the database
            reopenDatabase()

            true
        } catch (e: Exception) {
            errorHandler.handleException("DatabaseBackupManager.restoreBackup", e)
            false
        }
    }

    /**
     * Lists all available backups
     * @return A list of backup file URIs
     */
    suspend fun listBackups(): List<BackupInfo> = withContext(Dispatchers.IO) {
        try {
            val backupDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "backups")
            if (!backupDir.exists()) {
                return@withContext emptyList()
            }

            val backupFiles = backupDir.listFiles { file ->
                file.isFile && file.name.startsWith(BACKUP_FILE_PREFIX) && file.name.endsWith(BACKUP_FILE_EXTENSION)
            } ?: return@withContext emptyList()

            backupFiles.map { file ->
                val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                val datePart = file.name.removePrefix(BACKUP_FILE_PREFIX).removeSuffix(BACKUP_FILE_EXTENSION)
                val date = try {
                    dateFormat.parse(datePart)
                } catch (e: Exception) {
                    Date(file.lastModified())
                }

                BackupInfo(
                    uri = Uri.fromFile(file),
                    fileName = file.name,
                    creationDate = date,
                    size = file.length()
                )
            }.sortedByDescending { it.creationDate }
        } catch (e: Exception) {
            errorHandler.handleException("DatabaseBackupManager.listBackups", e)
            emptyList()
        }
    }

    /**
     * Deletes a backup file
     * @param backupUri The URI of the backup file to delete
     * @return true if the deletion was successful, false otherwise
     */
    suspend fun deleteBackup(backupUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val scheme = backupUri.scheme
            if (scheme == "file") {
                val file = File(backupUri.path ?: return@withContext false)
                if (file.exists()) {
                    file.delete()
                    return@withContext true
                }
            } else if (scheme == "content") {
                val documentFile = DocumentFile.fromSingleUri(context, backupUri)
                if (documentFile?.exists() == true) {
                    return@withContext documentFile.delete()
                }
            }
            false
        } catch (e: Exception) {
            errorHandler.handleException("DatabaseBackupManager.deleteBackup", e)
            false
        }
    }

    /**
     * Creates metadata for the backup
     * @return JSON string containing backup metadata
     */
    private fun createBackupMetadata(): String {
        return """
            {
                "version": "$BACKUP_VERSION",
                "timestamp": "${System.currentTimeMillis()}",
                "appVersion": "${context.packageManager.getPackageInfo(context.packageName, 0).versionName}",
                "databaseVersion": "4"
            }
        """.trimIndent()
    }

    /**
     * Verifies the backup metadata
     * @param metadata JSON string containing backup metadata
     * @return true if the metadata is valid, false otherwise
     */
    private fun verifyBackupMetadata(metadata: String): Boolean {
        try {
            // Simple check for now - just make sure it contains the expected fields
            return metadata.contains("version") &&
                   metadata.contains("timestamp") &&
                   metadata.contains("appVersion") &&
                   metadata.contains("databaseVersion")
        } catch (e: Exception) {
            errorHandler.handleException("DatabaseBackupManager.verifyBackupMetadata", e)
            return false
        }
    }

    /**
     * Adds a file to a ZIP output stream
     * @param zipOut The ZIP output stream
     * @param file The file to add
     * @param entryName The name of the entry in the ZIP file
     */
    @Throws(IOException::class)
    private fun addFileToZip(zipOut: ZipOutputStream, file: File, entryName: String) {
        FileInputStream(file).use { fis ->
            val zipEntry = ZipEntry(entryName)
            zipOut.putNextEntry(zipEntry)

            val buffer = ByteArray(BUFFER_SIZE)
            var len: Int
            while (fis.read(buffer).also { len = it } > 0) {
                zipOut.write(buffer, 0, len)
            }

            zipOut.closeEntry()
        }
    }

    /**
     * Adds a string to a ZIP output stream
     * @param zipOut The ZIP output stream
     * @param entryName The name of the entry in the ZIP file
     * @param content The string content to add
     */
    @Throws(IOException::class)
    private fun addStringToZip(zipOut: ZipOutputStream, entryName: String, content: String) {
        val zipEntry = ZipEntry(entryName)
        zipOut.putNextEntry(zipEntry)
        zipOut.write(content.toByteArray())
        zipOut.closeEntry()
    }

    /**
     * Closes the database to ensure all data is written
     */
    private fun closeDatabase() {
        try {
            // Use reflection to access the INSTANCE field and close the database
            val field = AppDatabase::class.java.getDeclaredField("INSTANCE")
            field.isAccessible = true
            val instance = field.get(null) as? AppDatabase
            instance?.close()
        } catch (e: Exception) {
            errorHandler.handleException("DatabaseBackupManager.closeDatabase", e)
        }
    }

    /**
     * Reopens the database after a restore
     */
    private fun reopenDatabase() {
        try {
            // Set the INSTANCE field to null to force recreation
            val field = AppDatabase::class.java.getDeclaredField("INSTANCE")
            field.isAccessible = true
            field.set(null, null)
        } catch (e: Exception) {
            errorHandler.handleException("DatabaseBackupManager.reopenDatabase", e)
        }
    }

    /**
     * Information about a backup file
     */
    data class BackupInfo(
        val uri: Uri,
        val fileName: String,
        val creationDate: Date,
        val size: Long
    )
}
