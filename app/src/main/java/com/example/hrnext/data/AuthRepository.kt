package com.example.hrnext.data

import com.example.hrnext.di.AppContainer
import com.example.hrnext.model.Session
import com.example.hrnext.network.RetrofitProvider
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.IOException

sealed interface AuthResult {
    data class Success(val session: Session) : AuthResult
    data class Error(val message: String) : AuthResult
}

class AuthRepository(private val container: AppContainer) {

    suspend fun login(siteUrlInput: String, usr: String, pwd: String): AuthResult {
        return try {
            val api = container.apiFor(siteUrlInput)
            val response = api.login(usr.trim(), pwd)
            if (!response.isSuccessful) {
                return AuthResult.Error(
                    extractErrorMessage(response.errorBody()?.string())
                        ?: "Login failed (HTTP ${response.code()}). Check your username and password.",
                )
            }
            val body = response.body()
            val message = body?.get("message")?.takeIf { it.isJsonPrimitive }?.asString
            if (message != "Logged In") {
                return AuthResult.Error(message ?: "Login failed. Please check your credentials.")
            }
            val fullName = body.get("full_name")?.takeIf { it.isJsonPrimitive }?.asString ?: usr
            val session = Session(
                siteUrl = RetrofitProvider.normalizeSiteUrl(siteUrlInput).trimEnd('/'),
                username = usr.trim(),
                fullName = fullName,
                userImage = null,
            )
            container.sessionManager.saveSession(session)
            AuthResult.Success(session)
        } catch (e: IOException) {
            AuthResult.Error("Could not reach the site. Check the URL and your network connection.")
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Unexpected error while logging in.")
        }
    }

    suspend fun logout(siteUrl: String) {
        runCatching { container.apiFor(siteUrl).logout() }
        container.logout()
    }

    private fun extractErrorMessage(rawBody: String?): String? {
        if (rawBody.isNullOrBlank()) return null
        return runCatching {
            val json = Gson().fromJson(rawBody, JsonObject::class.java)
            json?.get("message")?.takeIf { it.isJsonPrimitive }?.asString
                ?: json?.get("_server_messages")?.takeIf { it.isJsonPrimitive }?.asString
                    ?.let { messages ->
                        Gson().fromJson(messages, Array<String>::class.java)?.firstOrNull()
                    }
        }.getOrNull()
    }
}
