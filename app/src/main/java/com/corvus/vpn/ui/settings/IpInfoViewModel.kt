package com.corvus.vpn.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.corvus.vpn.data.IpInfo
import com.corvus.vpn.data.IpRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class IpInfoUiState {
    object Idle : IpInfoUiState()
    object Loading : IpInfoUiState()
    data class Success(val ipInfo: IpInfo) : IpInfoUiState()
    data class Error(val message: String) : IpInfoUiState()
}

@HiltViewModel
class IpInfoViewModel @Inject constructor(
    private val ipRepository: IpRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<IpInfoUiState>(IpInfoUiState.Idle)
    val uiState: StateFlow<IpInfoUiState> = _uiState.asStateFlow()

    fun fetchIpInfo() {
        viewModelScope.launch {
            _uiState.value = IpInfoUiState.Loading
            ipRepository.getMyIpInfo()
                .onSuccess {
                    _uiState.value = IpInfoUiState.Success(it)
                }
                .onFailure {
                    _uiState.value = IpInfoUiState.Error(it.message ?: "Unknown error occurred")
                }
        }
    }
}
