package com.corvus.vpn.vpn

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VpnSessionManager @Inject constructor() {

    private val _remainingTime = MutableStateFlow(0)
    val remainingTime: StateFlow<Int> = _remainingTime
    val elapsedTime: StateFlow<Int> = _remainingTime

    private var timerJob: Job? = null
    private var onTimeExpiredListener: (() -> Unit)? = null

    fun startSession(initialSeconds: Int = 20 * 60, onTimeExpired: (() -> Unit)? = null) {
        timerJob?.cancel()
        _remainingTime.value = initialSeconds
        onTimeExpiredListener = onTimeExpired

        timerJob = CoroutineScope(Dispatchers.Main).launch {
            while (_remainingTime.value > 0) {
                delay(1000)
                _remainingTime.value -= 1
            }
            onTimeExpiredListener?.invoke()
        }
    }

    fun stopSession() {
        timerJob?.cancel()
        _remainingTime.value = 0
    }

    fun addTime(seconds: Int) {
        _remainingTime.value += seconds
    }

    fun formatTime(totalSeconds: Int): String {
        if (totalSeconds <= 0) return "00:00"
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}
