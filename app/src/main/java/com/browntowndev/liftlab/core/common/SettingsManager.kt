package com.browntowndev.liftlab.core.common

import android.content.Context
import android.content.SharedPreferences
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DB_INITIALIZED
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_REST_TIME
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_USE_ALL_WORKOUT_DATA
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.REST_TIME
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object SettingsManager {
    object SettingNames {
        const val DB_INITIALIZED = "database_initialized"
        const val REST_TIME = "rest_time"
        const val USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS = "useAllWorkoutDataForRecommendations"
        const val ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION = "onlyUseResultsForLiftsInSamePosition"
        const val INCREMENT_AMOUNT = "increment_amount"
        const val DEFAULT_REST_TIME = 120000L
        const val DEFAULT_INCREMENT_AMOUNT = 5f
        const val DEFAULT_USE_ALL_WORKOUT_DATA = false
        const val DEFAULT_ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION = true
    }

    private const val PREFERENCES_NAME = "LiftLabPreferences"
    private lateinit var sharedPreferences: SharedPreferences

    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

        if (!sharedPreferences.getBoolean("settings_initialized", false)) {
            setDefaultSetting(DB_INITIALIZED, false)
            setDefaultSetting(REST_TIME, DEFAULT_REST_TIME)
            setDefaultSetting(INCREMENT_AMOUNT, DEFAULT_INCREMENT_AMOUNT)
            setDefaultSetting(
                USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS,
                DEFAULT_USE_ALL_WORKOUT_DATA
            )
            setDefaultSetting(ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION, DEFAULT_ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION)

            sharedPreferences.edit().putBoolean("settings_initialized", true).apply()
        }
    }

    fun initialize(sharedPreferences: SharedPreferences) {
        this.sharedPreferences = sharedPreferences
    }

    fun <T : Any> getSetting(key: String, defaultValue: T): T {
        return when (defaultValue) {
            is String -> sharedPreferences.getString(key, defaultValue)
            is Long -> sharedPreferences.getLong(key, defaultValue)
            is Boolean -> sharedPreferences.getBoolean(key, defaultValue)
            is Float -> sharedPreferences.getFloat(key, defaultValue)
            is Int -> sharedPreferences.getInt(key, defaultValue)
            else -> throw Exception("${defaultValue::class.simpleName} cannot be stored in SharedPreferences")
        } as T
    }

    fun<T: Any> getSettingFlow(key: String, defaultValue: T): Flow<T> {
        return callbackFlow {
            trySend(getSetting(key, defaultValue)).isSuccess

            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
                if (changedKey == key) {
                    trySend(getSetting(key, defaultValue)).isSuccess
                }
            }

            sharedPreferences.registerOnSharedPreferenceChangeListener(listener)

            awaitClose {
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(
                    listener
                )
            }
        }
    }

    fun <T : Any> setSetting(key: String, value: T) {
        setDefaultSetting(key, value, true)
    }

    private fun <T : Any> setDefaultSetting(
        key: String,
        defaultValue: T,
        updateIfExists: Boolean = false
    ) {
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
