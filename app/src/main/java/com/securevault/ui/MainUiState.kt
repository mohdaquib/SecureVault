package com.securevault.ui

import com.securevault.domain.model.Note

data class MainUiState(
    val notes: List<Note> = emptyList(),
    val securityStatus: SecurityStatus = SecurityStatus.Loading,
)

sealed class SecurityStatus {
    object Loading : SecurityStatus()

    object Secure : SecurityStatus()

    object Blocked : SecurityStatus()
}
