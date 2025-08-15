package com.browntowndev.liftlab.core.domain.repositories

import com.browntowndev.liftlab.core.domain.common.SettingKey
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun <T : Any> getSetting(key: SettingKey<T>): T
    fun <T: Any> getSettingFlow(key: SettingKey<T>): Flow<T>
    fun <T : Any> setSetting(key: SettingKey<T>, value: T)
}