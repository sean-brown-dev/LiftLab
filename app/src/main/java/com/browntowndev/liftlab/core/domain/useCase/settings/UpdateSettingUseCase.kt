package com.browntowndev.liftlab.core.domain.useCase.settings

import com.browntowndev.liftlab.core.domain.repositories.SettingKey
import com.browntowndev.liftlab.core.domain.repositories.SettingsRepository

class UpdateSettingUseCase(private val settingsRepository: SettingsRepository) {
    operator fun<T: Any> invoke(settingKey: SettingKey<T>, value: T) {
        settingsRepository.setSetting(settingKey, value)
    }
}