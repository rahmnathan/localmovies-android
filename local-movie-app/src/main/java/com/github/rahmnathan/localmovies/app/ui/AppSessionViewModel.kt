package com.github.rahmnathan.localmovies.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.rahmnathan.localmovies.app.data.local.AuthState
import com.github.rahmnathan.localmovies.app.data.local.UserPreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppSessionUiState(
    val isInitialized: Boolean = false,
    val authState: AuthState = AuthState.SignedOut
)

@HiltViewModel
class AppSessionViewModel @Inject constructor(
    private val preferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppSessionUiState())
    val uiState: StateFlow<AppSessionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                preferencesDataStore.userCredentialsFlow,
                preferencesDataStore.authStateFlow
            ) { _, authState -> authState }
                .collect { authState ->
                    _uiState.update {
                        it.copy(
                            isInitialized = true,
                            authState = authState
                        )
                    }
                }
        }
    }
}
