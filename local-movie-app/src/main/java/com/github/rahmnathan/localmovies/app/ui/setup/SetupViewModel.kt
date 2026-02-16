package com.github.rahmnathan.localmovies.app.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.rahmnathan.localmovies.app.data.local.UserCredentials
import com.github.rahmnathan.localmovies.app.data.local.UserPreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupUiState(
    val username: String = "",
    val password: String = "",
    val serverUrl: String = "https://movies.nathanrahm.com",
    val authServerUrl: String = "https://login.nathanrahm.com",
    val isLoading: Boolean = false,
    val error: String? = null,
    val loginSuccess: Boolean = false
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val preferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    init {
        // Load existing credentials if any
        viewModelScope.launch {
            preferencesDataStore.userCredentialsFlow.collect { credentials ->
                _uiState.update {
                    it.copy(
                        username = credentials.username,
                        password = credentials.password,
                        serverUrl = credentials.serverUrl,
                        authServerUrl = credentials.authServerUrl
                    )
                }
            }
        }
    }

    fun onUsernameChange(username: String) {
        _uiState.update { it.copy(username = username) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    fun login() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val credentials = UserCredentials(
                    username = _uiState.value.username,
                    password = _uiState.value.password,
                    serverUrl = _uiState.value.serverUrl,
                    authServerUrl = _uiState.value.authServerUrl
                )

                preferencesDataStore.saveCredentials(credentials)

                // Note: OAuth2 validation happens when MediaApi is first used
                // For now, we assume credentials are valid
                _uiState.update {
                    it.copy(isLoading = false, loginSuccess = true)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Login failed"
                    )
                }
            }
        }
    }
}
