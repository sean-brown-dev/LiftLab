package com.browntowndev.liftlab.core.scheduledBackup

import de.raphaelebner.roomdatabasebackup.core.AESEncryptionHelper
import de.raphaelebner.roomdatabasebackup.core.OnCompleteListener

import android.content.Context
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

class LiftLabRoomBackup(
    private val context: Context,
    private val roomDatabase: RoomDatabase,
    private val backupFile: File,
) {

    companion object {
        private var TAG = "debug_RoomBackup"
        private lateinit var INTERNAL_BACKUP_PATH: File
        private lateinit var TEMP_BACKUP_PATH: File
        private lateinit var TEMP_BACKUP_FILE: File
        private lateinit var EXTERNAL_BACKUP_PATH: File
        private lateinit var DATABASE_FILE: File

        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_KEY_ALIAS = "LiftLabBackupAesKey"

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
    }

    private lateinit var dbName: String
    private var enableLogDebug: Boolean = false
    private var onCompleteListener: OnCompleteListener? = null

    private fun initRoomBackup(): Boolean {
        dbName = roomDatabase.openHelper.databaseName!!
        INTERNAL_BACKUP_PATH = File("${context.filesDir}/databasebackup/")
        TEMP_BACKUP_PATH = File("${context.filesDir}/databasebackup-temp/")
        TEMP_BACKUP_FILE = File("$TEMP_BACKUP_PATH/tempbackup.sqlite3")
        EXTERNAL_BACKUP_PATH = File(context.getExternalFilesDir("backup")!!.toURI())
        DATABASE_FILE = File(context.getDatabasePath(dbName).toURI())

        try {
            INTERNAL_BACKUP_PATH.mkdirs()
            TEMP_BACKUP_PATH.mkdirs()
        } catch (_: FileAlreadyExistsException) {
        } catch (_: IOException) {
        }

        if (enableLogDebug) {
            Log.d(TAG, "DatabaseName: $dbName")
            Log.d(TAG, "Database Location: $DATABASE_FILE")
            Log.d(TAG, "INTERNAL_BACKUP_PATH: $INTERNAL_BACKUP_PATH")
            Log.d(TAG, "EXTERNAL_BACKUP_PATH: $EXTERNAL_BACKUP_PATH")
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

    private fun encryptBackup(): ByteArray? {
        try {
            copy(DATABASE_FILE, TEMP_BACKUP_FILE)

            val encryptDecryptBackup = AESEncryptionHelper()
            val fileData = encryptDecryptBackup.readFile(TEMP_BACKUP_FILE)

            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv
            val encrypted = cipher.doFinal(fileData)
            TEMP_BACKUP_FILE.delete()

            return iv + encrypted // Prepend IV to the ciphertext
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
