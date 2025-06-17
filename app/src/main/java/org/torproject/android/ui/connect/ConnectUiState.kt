package org.torproject.android.ui.connect

sealed class ConnectUiState {
    object NoInternet : ConnectUiState()
    object Off : ConnectUiState()
    object Starting : ConnectUiState()
    object On : ConnectUiState()
    object Stopping : ConnectUiState()
}
