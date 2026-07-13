package com.example.hrnext.ui.screens.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hrnext.data.AuthResult
import com.example.hrnext.data.AuthRepository
import com.example.hrnext.model.Session
import kotlinx.coroutines.launch

data class LoginUiState(
    val siteUrl: String = "",
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    var uiState by mutableStateOf(LoginUiState())
        private set

    fun onSiteUrlChange(value: String) {
        uiState = uiState.copy(siteUrl = value, error = null)
    }

    fun onUsernameChange(value: String) {
        uiState = uiState.copy(username = value, error = null)
    }

    fun onPasswordChange(value: String) {
        uiState = uiState.copy(password = value, error = null)
    }

    fun login(onSuccess: (Session) -> Unit) {
        val state = uiState
        if (state.siteUrl.isBlank() || state.username.isBlank() || state.password.isBlank()) {
            uiState = state.copy(error = "Please fill in the site URL, username/email and password.")
            return
        }
        uiState = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            when (val result = authRepository.login(state.siteUrl, state.username, state.password)) {
                is AuthResult.Success -> {
                    uiState = uiState.copy(isLoading = false)
                    onSuccess(result.session)
                }
                is AuthResult.Error -> {
                    uiState = uiState.copy(isLoading = false, error = result.message)
                }
            }
        }
    }
}
