package com.browntowndev.liftlab.core.common

import android.content.Context
import android.content.SharedPreferences
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DB_INITIALIZED
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_REST_TIME
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.REST_TIME

object SettingsManager {
    object SettingNames {
        const val DB_INITIALIZED = "database_initialized"
        const val REST_TIME = "rest_time"
        const val DEFAULT_REST_TIME = 120000L
        const val INCREMENT_AMOUNT = "increment_amount"
        const val DEFAULT_INCREMENT_AMOUNT = 5f
    }

    private const val PREFERENCES_NAME = "LiftLabPreferences"
    private lateinit var sharedPreferences: SharedPreferences

    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

        if (!sharedPreferences.getBoolean("settings_initialized", false)) {
            setDefaultSetting(DB_INITIALIZED, false)
            setDefaultSetting(REST_TIME, DEFAULT_REST_TIME)
            setDefaultSetting(INCREMENT_AMOUNT, DEFAULT_INCREMENT_AMOUNT)

            sharedPreferences.edit().putBoolean("settings_initialized", true).apply()
        }
    }

    fun <T: Any> getSetting(key: String, defaultValue: T): T {
        return when (defaultValue) {
            is String -> sharedPreferences.getString(key, defaultValue)
            is Long -> sharedPreferences.getLong(key, defaultValue)
            is Boolean -> sharedPreferences.getBoolean(key, defaultValue)
            is Float -> sharedPreferences.getFloat(key, defaultValue)
            is Int -> sharedPreferences.getInt(key, defaultValue)
            else -> throw Exception("${defaultValue::class.simpleName} cannot be stored in SharedPreferences")
        } as T
    }

    fun <T: Any> setSetting(key: String, value: T) {
        setDefaultSetting(key, value, true)
    }

    private fun <T: Any> setDefaultSetting(key: String, defaultValue: T, updateIfExists: Boolean = false) {
        if (updateIfExists || !sharedPreferences.contains(key)) {
            when (defaultValue) {
                is String -> sharedPreferences.edit().putString(key, defaultValue)
                is Long -> sharedPreferences.edit().putLong(key, defaultValue)
                is Boolean -> sharedPreferences.edit().putBoolean(key, defaultValue)
                is Float -> sharedPreferences.edit().putFloat(key, defaultValue)
                is Int -> sharedPreferences.edit().putInt(key, defaultValue)
                else -> throw Exception("${defaultValue::class.simpleName} cannot be stored in SharedPreferences")
            }.apply()
        }
    }
}
