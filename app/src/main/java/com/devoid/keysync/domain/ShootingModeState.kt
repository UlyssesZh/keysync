package com.devoid.keysync.domain

sealed class ShootingModeState {
    data object Enabled: ShootingModeState()
    data class Disabled(val temporary:Boolean): ShootingModeState()//shooting mode might be temporary disabled when cancellable button is triggered
}