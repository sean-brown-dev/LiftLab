package com.browntowndev.liftlab.core.data.repositories

import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.domain.repositories.SettingKey
import com.browntowndev.liftlab.core.domain.repositories.SettingsRepository
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl: SettingsRepository {
    override fun <T : Any> getSetting(
        key: SettingKey<T>
    ): T = SettingsManager.getSetting(key.name, key.defaultValue)

    override fun <T : Any> getSettingFlow(
        key: SettingKey<T>
    ): Flow<T> = SettingsManager.getSettingFlow(key.name, key.defaultValue)

    override fun <T : Any> setSetting(
        key: SettingKey<T>,
        value: T
    ) = SettingsManager.setSetting(key.name, value)
}