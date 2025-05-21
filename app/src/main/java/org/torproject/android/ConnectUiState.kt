package org.torproject.android

sealed class ConnectUiState {
    object NoInternet : ConnectUiState()
    object Off : ConnectUiState()
    object Starting : ConnectUiState()
    object On : ConnectUiState()
    object Stopping : ConnectUiState()
}
