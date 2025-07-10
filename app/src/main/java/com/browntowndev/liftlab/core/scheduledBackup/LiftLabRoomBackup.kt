package com.browntowndev.liftlab.core.scheduledBackup

import android.app.Activity

import android.content.Context
import android.content.Intent
import android.net.Uri // Added import
import android.util.Log
import androidx.room.RoomDatabase
import java.io.*
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.google.common.io.Files.copy
import javax.crypto.BadPaddingException
import javax.crypto.spec.GCMParameterSpec

class LiftLabRoomBackup(
    val context: Context,
    private val roomDatabase: RoomDatabase,
    private val backupFile: File,
) {

    companion object {
        private var TAG = "debug_RoomBackup"
        private lateinit var TEMP_BACKUP_PATH: File
        private lateinit var TEMP_BACKUP_FILE: File
        private lateinit var DATABASE_FILE: File

        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_KEY_ALIAS = "LiftLabBackupAesKey"

        val MIME_TYPES_FOR_BACKUP = arrayOf("application/octet-stream", "*/*")
        private const val GCM_IV_LENGTH = 12 // GCM recommended IV length is 12 bytes


        private fun getOrCreateSecretKey(): SecretKey {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

            if (keyStore.containsAlias(AES_KEY_ALIAS)) {
                return (keyStore.getEntry(AES_KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
            }

            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val keySpec = KeyGenParameterSpec.Builder(
                AES_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(keySpec)
            return keyGenerator.generateKey()
        }

        fun readFile(file: File): ByteArray {
            val fileContents = file.readBytes()
            val inputBuffer = BufferedInputStream(FileInputStream(file))
            inputBuffer.read(fileContents)
            inputBuffer.close()
            return fileContents
        }
    }

    private lateinit var dbName: String
    private var enableLogDebug: Boolean = false
    private var onCompleteListener: OnCompleteListener? = null
    private var restartIntent: Intent? = null

    fun onCompleteListener(
        listener: (success: Boolean, message: String, exitCode: Int) -> Unit
    ): LiftLabRoomBackup {
        this.onCompleteListener =
            object : OnCompleteListener {
                override fun onComplete(success: Boolean, message: String, exitCode: Int) {
                    listener(success, message, exitCode)
                }
            }

        return this
    }

    fun restartApp(restartIntent: Intent): LiftLabRoomBackup {
        this.restartIntent = restartIntent
        restartApp()
        return this
    }

    private fun restartApp() {
        restartIntent!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(restartIntent)
        if (context is Activity) {
            context.finish()
        }
        Runtime.getRuntime().exit(0)
    }

    private fun initRoomBackup(): Boolean {
        dbName = roomDatabase.openHelper.databaseName!!
        DATABASE_FILE = File(context.getDatabasePath(dbName).toURI())
        TEMP_BACKUP_PATH = File("${context.filesDir}/databasebackup-temp/")
        TEMP_BACKUP_FILE = File("$TEMP_BACKUP_PATH/tempbackup.sqlite3")

        if (enableLogDebug) {
            Log.d(TAG, "DatabaseName: $dbName")
            Log.d(TAG, "Database Location: $DATABASE_FILE")
            Log.d(TAG, "backupLocationCustomFile: $backupFile")
        }
        return true
    }

    fun backup() {
        if (enableLogDebug) Log.d(TAG, "Starting Backup ...")
        val success = initRoomBackup()
        if (!success) return

        if (enableLogDebug) Log.d(TAG, "backupFilename: ${backupFile.name}")

        doBackup()
    }

    /**
     * Initializes the restore process.
     * The calling Activity is responsible for launching the file picker
     * and then calling `handleSelectedFileToRestore` with the chosen URI.
     */
    fun prepareRestore(): Boolean {
        if (enableLogDebug) Log.d(TAG, "Preparing for Restore ...")
        val success = initRoomBackup()
        if (!success) {
            onCompleteListener?.onComplete(false, "Initialization for restore failed", OnCompleteListener.EXIT_CODE_ERROR)
            return false
        }
        // No longer launching intent here
        return true
    }

    /**
     * Call this method after the user has selected a file URI using the Activity Result API.
     */
    fun handleSelectedFileToRestore(uri: Uri) {
        if (enableLogDebug) Log.d(TAG, "File selected for restore: $uri")
        try {
            if (!uri.toString().endsWith(".aes")) {
                onCompleteListener?.onComplete(false, "File must be an .aes file.", OnCompleteListener.EXIT_CODE_ERROR)
                return
            }
            doRestore(uri)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing selected file for restore: ${e.message}", e)
            onCompleteListener?.onComplete(false, "Error processing selected file", OnCompleteListener.EXIT_CODE_ERROR)
        }
    }

    private fun doBackup() {
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

    private fun doRestore(sourceUri: Uri) {
        roomDatabase.close()

        if (!TEMP_BACKUP_PATH.exists()) {
            TEMP_BACKUP_PATH.mkdirs()
        }

        if (TEMP_BACKUP_FILE.exists()) {
            TEMP_BACKUP_FILE.delete()
        }

        try {
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(TEMP_BACKUP_FILE).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: run {
                Log.e(TAG, "Failed to open input stream from URI: $sourceUri")
                onCompleteListener?.onComplete(false, "Failed to read selected file", OnCompleteListener.EXIT_CODE_ERROR)
                return
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error copying selected file to temp backup location: ${e.message}", e)
            onCompleteListener?.onComplete(false, "Error preparing file for restore", OnCompleteListener.EXIT_CODE_ERROR)
            TEMP_BACKUP_FILE.delete()
        } catch (e: IOException) {
            Log.e(TAG, "Error writing decrypted data to database file: ${e.message}", e)
            onCompleteListener?.onComplete(false, "Error writing restored data", OnCompleteListener.EXIT_CODE_ERROR)
            TEMP_BACKUP_FILE.delete()
            return
        }

        try {
            val decryptedBytes = decryptBackup() ?: return // TEMP_BACKUP_FILE is used and deleted by decryptBackup
            val bos = BufferedOutputStream(FileOutputStream(DATABASE_FILE, false))
            bos.write(decryptedBytes)
            bos.flush()
            bos.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error writing decrypted data to database file: ${e.message}", e)
            onCompleteListener?.onComplete(false, "Error writing restored data", OnCompleteListener.EXIT_CODE_ERROR)
            return
        }

        if (enableLogDebug)
            Log.d(TAG, "Restored from $TEMP_BACKUP_FILE (original URI source was $sourceUri)")

        onCompleteListener?.onComplete(true, "success", OnCompleteListener.EXIT_CODE_SUCCESS)
    }

    private fun encryptBackup(): ByteArray? {
        try {
            copy(DATABASE_FILE, TEMP_BACKUP_FILE)

            val fileData = readFile(TEMP_BACKUP_FILE)

            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")

            val iv = ByteArray(GCM_IV_LENGTH)
            java.security.SecureRandom().nextBytes(iv)
            val gcmParameterSpec = GCMParameterSpec(128, iv) // 128 bit auth tag length
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec)
            val encrypted = cipher.doFinal(fileData)

            return iv + encrypted
        } catch (e: Exception) {
            if (enableLogDebug) Log.d(TAG, "error during encryption: ${e.message}")
            onCompleteListener?.onComplete(
                false,
                "error during encryption",
                OnCompleteListener.EXIT_CODE_ERROR_ENCRYPTION_ERROR
            )
            return null
        } finally {
            TEMP_BACKUP_FILE.delete()
        }
    }

    private fun decryptBackup(): ByteArray? {
        try {
            val fileData = readFile(TEMP_BACKUP_FILE) // Reads from TEMP_BACKUP_FILE

            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")

            val iv = fileData.copyOfRange(0, GCM_IV_LENGTH)
            val encryptedData = fileData.copyOfRange(GCM_IV_LENGTH, fileData.size)

            val gcmParameterSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec)
            val decryptedBytes = cipher.doFinal(encryptedData)

            return decryptedBytes
        } catch (e: BadPaddingException) {
            Log.e(TAG, "error during decryption (wrong password): ${e.message}")
            onCompleteListener?.onComplete(
                false,
                "error during decryption",
                OnCompleteListener.EXIT_CODE_ERROR_WRONG_DECRYPTION_PASSWORD
            )
            return null
        } catch (e: Exception) {
            Log.e(TAG, "error during decryption: ${e.message}")
            onCompleteListener?.onComplete(
                false,
                "error during decryption",
                OnCompleteListener.EXIT_CODE_ERROR_DECRYPTION_ERROR
            )
            return null
        } finally {
            TEMP_BACKUP_FILE.delete()
        }
    }
}
