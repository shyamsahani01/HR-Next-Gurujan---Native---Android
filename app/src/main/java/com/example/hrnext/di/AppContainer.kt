package com.example.hrnext.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.example.hrnext.network.FrappeApi
import com.example.hrnext.network.PersistentCookieJar
import com.example.hrnext.network.RetrofitProvider
import com.example.hrnext.network.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

private val Context.dataStore by preferencesDataStore(name = "hrnext_prefs")

/** Small hand-rolled dependency container; the app is simple enough that Hilt isn't worth the build overhead. */
class AppContainer(context: Context) {

    private val scope = CoroutineScope(SupervisorJob())
    private val cookieJar = PersistentCookieJar(context.dataStore, scope)
    val sessionManager = SessionManager(context.dataStore)

    private var cachedSiteUrl: String? = null
    private var cachedApi: FrappeApi? = null

    /** Builds (and caches) the [FrappeApi] pointed at [siteUrl]. Cheap to call repeatedly. */
    fun apiFor(siteUrl: String): FrappeApi {
        val normalized = RetrofitProvider.normalizeSiteUrl(siteUrl)
        val current = cachedApi
        if (current != null && cachedSiteUrl == normalized) return current
        val built = RetrofitProvider.build(siteUrl, cookieJar)
        cachedSiteUrl = normalized
        cachedApi = built
        return built
    }

    suspend fun logout() {
        cookieJar.clear()
        sessionManager.clear()
        cachedApi = null
        cachedSiteUrl = null
    }
}
