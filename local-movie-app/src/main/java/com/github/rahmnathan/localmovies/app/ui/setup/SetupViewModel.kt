package com.github.rahmnathan.localmovies.app.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.rahmnathan.localmovies.app.auth.AuthSessionManager
import com.github.rahmnathan.localmovies.app.data.local.UserPreferencesDataStore
import com.github.rahmnathan.localmovies.app.data.repository.MediaRepository
import com.github.rahmnathan.localmovies.app.data.repository.Result
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
    val sessionMessage: String? = null,
    val error: String? = null,
    val loginSuccess: Boolean = false
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val preferencesDataStore: UserPreferencesDataStore,
    private val authSessionManager: AuthSessionManager,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                preferencesDataStore.userCredentialsFlow,
                preferencesDataStore.authMessageFlow
            ) { credentials, authMessage ->
                credentials to authMessage
            }.collect { (credentials, authMessage) ->
                _uiState.update {
                    it.copy(
                        username = credentials.username,
                        serverUrl = credentials.serverUrl,
                        authServerUrl = credentials.authServerUrl,
                        sessionMessage = authMessage
                    )
                }
            }
        }
    }

    fun onUsernameChange(username: String) {
        _uiState.update { it.copy(username = username) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, error = null) }
    }

    fun login() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                authSessionManager.login(
                    username = _uiState.value.username,
                    password = _uiState.value.password,
                    serverUrl = _uiState.value.serverUrl,
                    authServerUrl = _uiState.value.authServerUrl
                )

                when (val result = mediaRepository.getMediaList(page = 0, size = 1).first { it !is Result.Loading }) {
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                password = "",
                                loginSuccess = true
                            )
                        }
                    }
                    is Result.Error -> {
                        preferencesDataStore.clearSession()
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                password = "",
                                error = loginFailureMessage(result.exception)
                            )
                        }
                    }
                    is Result.Loading -> Unit
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

    private fun loginFailureMessage(exception: Throwable): String {
        val message = exception.message.orEmpty()
        return if ("401" in message || "Unauthorized" in message) {
            "Login failed: the server rejected the new session."
        } else {
            message.ifBlank { "Login failed" }
        }
    }
}
