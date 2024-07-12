package com.browntowndev.liftlab.core.scheduledBackup

import de.raphaelebner.roomdatabasebackup.core.AESEncryptionHelper
import de.raphaelebner.roomdatabasebackup.core.AESEncryptionManager
import de.raphaelebner.roomdatabasebackup.core.OnCompleteListener

import android.Manifest.permission.*
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.room.RoomDatabase
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.common.io.Files.copy
import java.io.*
import java.util.*

/**
 * MIT License
 *
 * Copyright (c) 2024 Raphael Ebner
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
class LiftLabRoomBackup(
    private val context: Context,
    private val roomDatabase: RoomDatabase,
    private val backupFile: File,
    private val encryptionKey: String,
) {

    companion object {
        private const val SHARED_PREFS = "de.raphaelebner.roomdatabasebackup"
        private var TAG = "debug_RoomBackup"
        private lateinit var INTERNAL_BACKUP_PATH: File
        private lateinit var TEMP_BACKUP_PATH: File
        private lateinit var TEMP_BACKUP_FILE: File
        private lateinit var EXTERNAL_BACKUP_PATH: File
        private lateinit var DATABASE_FILE: File
    }

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var dbName: String

    private var enableLogDebug: Boolean = false
    private var onCompleteListener: OnCompleteListener? = null

    fun enableDebugLogging(enable: Boolean): LiftLabRoomBackup {
        this.enableLogDebug = enable
        return this
    }

    /** Init vars, and return true if no error occurred */
    private fun initRoomBackup(): Boolean {
        // Create or retrieve the Master Key for encryption/decryption
        val masterKeyAlias =
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

        // Initialize/open an instance of EncryptedSharedPreferences
        // Encryption key is stored in plain text in an EncryptedSharedPreferences --> it is saved
        // encrypted
        sharedPreferences =
            EncryptedSharedPreferences.create(
                context,
                SHARED_PREFS,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

        dbName = roomDatabase.openHelper.databaseName!!
        INTERNAL_BACKUP_PATH = File("${context.filesDir}/databasebackup/")
        TEMP_BACKUP_PATH = File("${context.filesDir}/databasebackup-temp/")
        TEMP_BACKUP_FILE = File("$TEMP_BACKUP_PATH/tempbackup.sqlite3")
        EXTERNAL_BACKUP_PATH = File(context.getExternalFilesDir("backup")!!.toURI())
        DATABASE_FILE = File(context.getDatabasePath(dbName).toURI())

        // Create internal and temp backup directory if does not exist
        try {
            INTERNAL_BACKUP_PATH.mkdirs()
            TEMP_BACKUP_PATH.mkdirs()
        } catch (_: FileAlreadyExistsException) {} catch (_: IOException) {}

        if (enableLogDebug) {
            Log.d(TAG, "DatabaseName: $dbName")
            Log.d(TAG, "Database Location: $DATABASE_FILE")
            Log.d(TAG, "INTERNAL_BACKUP_PATH: $INTERNAL_BACKUP_PATH")
            Log.d(TAG, "EXTERNAL_BACKUP_PATH: $EXTERNAL_BACKUP_PATH")
            Log.d(TAG, "backupLocationCustomFile: $backupFile")
        }
        return true
    }

    /**
     * Start Backup process, and set onComplete Listener to success, if no error occurred, else
     * onComplete Listener success is false and error message is passed
     *
     * if custom storage ist selected, the [openBackupfileCreator] will be launched
     */
    fun backup() {
        if (enableLogDebug) Log.d(TAG, "Starting Backup ...")
        val success = initRoomBackup()
        if (!success) return

        // Create name for backup file, if no custom name is set: Database name + currentTime +
        // .sqlite3
        if (enableLogDebug) Log.d(TAG, "backupFilename: $backupFile.name")

        doBackup()
    }

    /**
     * This method will do the backup action
     *
     * @param destination File
     */
    private fun doBackup() {
        // Close the database
        roomDatabase.close()

        val encryptedBytes = encryptBackup() ?: return
        val bos = BufferedOutputStream(FileOutputStream(backupFile, false))
        bos.write(encryptedBytes)
        bos.flush()
        bos.close()

        if (enableLogDebug)
            Log.d(TAG, "Backup done, saved to $backupFile")

        onCompleteListener?.onComplete(true, "success", OnCompleteListener.EXIT_CODE_SUCCESS)
    }

    /**
     * Encrypts the current Database and return it's content as ByteArray. The original Database is
     * not encrypted only a current copy of this database
     *
     * @return encrypted backup as ByteArray
     */
    private fun encryptBackup(): ByteArray? {
        try {
            // Copy database you want to backup to temp directory
            copy(DATABASE_FILE, TEMP_BACKUP_FILE)

            // encrypt temp file, and save it to backup location
            val encryptDecryptBackup = AESEncryptionHelper()
            val fileData = encryptDecryptBackup.readFile(TEMP_BACKUP_FILE)

            val aesEncryptionManager = AESEncryptionManager()
            val encryptedBytes =
                aesEncryptionManager.encryptData(sharedPreferences, encryptionKey, fileData)

            // Delete temp file
            TEMP_BACKUP_FILE.delete()

            return encryptedBytes
        } catch (e: Exception) {
            if (enableLogDebug) Log.d(TAG, "error during encryption: ${e.message}")
            onCompleteListener?.onComplete(
                false,
                "error during encryption",
                OnCompleteListener.EXIT_CODE_ERROR_ENCRYPTION_ERROR
            )
            return null
        }
    }
}
