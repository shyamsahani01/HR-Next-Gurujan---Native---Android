package com.example.hrnext.data

import com.example.hrnext.util.stringOrNull
import com.google.gson.Gson
import com.google.gson.JsonObject

class ApiException(val code: Int, rawBody: String?) : Exception(extractMessage(rawBody) ?: "Request failed (HTTP $code)")

private fun extractMessage(rawBody: String?): String? {
    if (rawBody.isNullOrBlank()) return null
    return runCatching {
        val json = Gson().fromJson(rawBody, JsonObject::class.java)
        json?.stringOrNull("message") ?: json?.stringOrNull("exception")
    }.getOrNull()
}
