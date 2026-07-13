package com.example.hrnext.network

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.hrnext.model.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Persists which site/user is currently logged in (cookies themselves live in [PersistentCookieJar]). */
class SessionManager(private val dataStore: DataStore<Preferences>) {

    data class CheckinState(val isCheckedIn: Boolean, val sinceMillis: Long?)

    private object Keys {
        val SITE_URL = stringPreferencesKey("site_url")
        val USERNAME = stringPreferencesKey("username")
        val FULL_NAME = stringPreferencesKey("full_name")
        val USER_IMAGE = stringPreferencesKey("user_image")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val EMPLOYEE_ID = stringPreferencesKey("employee_id")
        val CHECKED_IN = booleanPreferencesKey("checked_in")
        val CHECKIN_SINCE = longPreferencesKey("checkin_since")
    }

    val sessionFlow: Flow<Session?> = dataStore.data.map { prefs ->
        val siteUrl = prefs[Keys.SITE_URL]
        val username = prefs[Keys.USERNAME]
        if (siteUrl.isNullOrBlank() || username.isNullOrBlank()) {
            null
        } else {
            Session(
                siteUrl = siteUrl,
                username = username,
                fullName = prefs[Keys.FULL_NAME] ?: username,
                userImage = prefs[Keys.USER_IMAGE],
                employeeId = prefs[Keys.EMPLOYEE_ID],
            )
        }
    }

    /** One of "system", "light", "dark". */
    val themeModeFlow: Flow<String> = dataStore.data.map { it[Keys.THEME_MODE] ?: "system" }

    /** Whether the current employee is checked in right now, persisted so it survives process death;
     * always reconciled against server truth on Home load rather than trusted blindly. */
    val checkinStateFlow: Flow<CheckinState> = dataStore.data.map { prefs ->
        CheckinState(isCheckedIn = prefs[Keys.CHECKED_IN] ?: false, sinceMillis = prefs[Keys.CHECKIN_SINCE])
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun saveSession(session: Session) {
        dataStore.edit { prefs ->
            prefs[Keys.SITE_URL] = session.siteUrl
            prefs[Keys.USERNAME] = session.username
            prefs[Keys.FULL_NAME] = session.fullName
            if (session.userImage != null) {
                prefs[Keys.USER_IMAGE] = session.userImage
            } else {
                prefs.remove(Keys.USER_IMAGE)
            }
        }
    }

    suspend fun saveEmployeeId(id: String) {
        dataStore.edit { prefs -> prefs[Keys.EMPLOYEE_ID] = id }
    }

    suspend fun setCheckinState(isCheckedIn: Boolean, sinceMillis: Long?) {
        dataStore.edit { prefs ->
            prefs[Keys.CHECKED_IN] = isCheckedIn
            if (sinceMillis != null) prefs[Keys.CHECKIN_SINCE] = sinceMillis else prefs.remove(Keys.CHECKIN_SINCE)
        }
    }

    /** Clears the logged-in session only; user preferences like [themeModeFlow] survive logout. */
    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.SITE_URL)
            prefs.remove(Keys.USERNAME)
            prefs.remove(Keys.FULL_NAME)
            prefs.remove(Keys.USER_IMAGE)
            prefs.remove(Keys.EMPLOYEE_ID)
            prefs.remove(Keys.CHECKED_IN)
            prefs.remove(Keys.CHECKIN_SINCE)
        }
    }
}
