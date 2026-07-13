package com.example.hrnext.network

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

private data class StoredCookie(
    val name: String,
    val value: String,
    val domain: String,
    val path: String,
    val expiresAt: Long,
    val secure: Boolean,
    val httpOnly: Boolean,
    val hostOnly: Boolean,
)

/**
 * OkHttp [CookieJar] backed by DataStore so the Frappe session cookie (`sid`) survives
 * app restarts and the user doesn't have to log back in on every launch.
 */
class PersistentCookieJar(
    private val dataStore: DataStore<Preferences>,
    private val scope: CoroutineScope,
) : CookieJar {

    private val cookieStoreKey = stringPreferencesKey("cookie_store_json")
    private val gson = Gson()
    private val cache = ConcurrentHashMap<String, List<Cookie>>()

    init {
        val raw = runBlocking { dataStore.data.first()[cookieStoreKey] }
        if (!raw.isNullOrBlank()) {
            val type = object : TypeToken<Map<String, List<StoredCookie>>>() {}.type
            val stored: Map<String, List<StoredCookie>> = runCatching { gson.fromJson<Map<String, List<StoredCookie>>>(raw, type) }.getOrDefault(emptyMap())
            stored.forEach { (host, cookies) ->
                cache[host] = cookies.map { it.toCookie() }
            }
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        val existing = cache[url.host].orEmpty().associateBy { it.name }.toMutableMap()
        cookies.forEach { existing[it.name] = it }
        cache[url.host] = existing.values.toList()
        persist()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        return cache[url.host].orEmpty().filter { it.expiresAt > now }
    }

    fun clear() {
        cache.clear()
        persist()
    }

    private fun persist() {
        val snapshot = cache.mapValues { (_, cookies) -> cookies.map { it.toStored() } }
        val json = gson.toJson(snapshot)
        scope.launch {
            dataStore.edit { it[cookieStoreKey] = json }
        }
    }

    private fun Cookie.toStored() = StoredCookie(name, value, domain, path, expiresAt, secure, httpOnly, hostOnly)

    private fun StoredCookie.toCookie(): Cookie = Cookie.Builder()
        .name(name)
        .value(value)
        .let { if (hostOnly) it.hostOnlyDomain(domain) else it.domain(domain) }
        .path(path)
        .expiresAt(expiresAt)
        .let { if (secure) it.secure() else it }
        .let { if (httpOnly) it.httpOnly() else it }
        .build()
}
