package com.example.hrnext.util

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

fun JsonObject.stringOrNull(key: String): String? =
    get(key)?.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.isNotBlank() }

fun JsonObject.boolOrFalse(key: String): Boolean {
    val el = get(key) ?: return false
    if (!el.isJsonPrimitive) return false
    return runCatching { el.asInt == 1 }.getOrElse { runCatching { el.asBoolean }.getOrDefault(false) }
}

fun JsonArray?.orEmptyElements(): List<JsonElement> = this?.toList().orEmpty()

fun JsonObject.doubleOrZero(key: String): Double {
    val el = get(key) ?: return 0.0
    if (!el.isJsonPrimitive) return 0.0
    return runCatching { el.asDouble }.getOrDefault(0.0)
}
