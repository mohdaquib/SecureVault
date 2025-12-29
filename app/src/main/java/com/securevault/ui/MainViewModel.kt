package com.securevault.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevault.core.network.NetworkResult
import com.securevault.core.network.SecurityHealthChecker
import com.securevault.domain.usecase.CreateNoteUseCase
import com.securevault.domain.usecase.GetNotesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getNotesUseCase: GetNotesUseCase,
    private val createNoteUseCase: CreateNoteUseCase,
    private val securityHealthChecker: SecurityHealthChecker
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: Flow<MainUiState> = _uiState

    init {
        observeNotes()
        checkSecurity()
    }

    private fun observeNotes() {
        viewModelScope.launch {
            getNotesUseCase().collectLatest { notes ->
                _uiState.value = _uiState.value.copy(notes = notes)
            }
        }
    }

    private fun checkSecurity() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(securityStatus = SecurityStatus.Loading)
            when(securityHealthChecker.check()) {
                is NetworkResult.Success ->
                    _uiState.value = _uiState.value.copy(securityStatus = SecurityStatus.Secure)
                is NetworkResult.Failure ->
                    _uiState.value = _uiState.value.copy(securityStatus = SecurityStatus.Blocked)
            }
        }
    }

    fun addSampleNote() {
        viewModelScope.launch {
            createNoteUseCase(
                title = "Secure Note",
                content = "Stored encrypted using SQLCipher + Keystore"
            )
        }
    }
}