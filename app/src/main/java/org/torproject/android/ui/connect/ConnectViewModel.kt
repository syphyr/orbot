package org.torproject.android.ui.connect

import android.content.Context

import androidx.lifecycle.ViewModel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

import org.torproject.android.core.NetworkUtils.isNetworkAvailable
import org.torproject.android.service.OrbotConstants

class ConnectViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<ConnectUiState>(ConnectUiState.Off)
    val uiState: StateFlow<ConnectUiState> = _uiState

    fun updateState(context: Context, status: String?) {
        val newState = when {
            !isNetworkAvailable(context) -> ConnectUiState.NoInternet
            status == OrbotConstants.STATUS_STARTING -> ConnectUiState.Starting
            status == OrbotConstants.STATUS_ON -> ConnectUiState.On
            status == OrbotConstants.STATUS_STOPPING -> ConnectUiState.Stopping
            else -> ConnectUiState.Off
        }
        _uiState.value = newState
    }
}
