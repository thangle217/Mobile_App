package com.example.myapplication.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.myapplication.domain.model.UserRole
import com.example.myapplication.domain.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

/**
 * Manages user sessions locally using Android Jetpack DataStore Preferences.
 * Provides a reactive stream of the current active session and functions to persist or clear it.
 */
class SessionStore(context: Context) {
    private val dataStore = context.sessionDataStore

    /**
     * A reactive flow emitting the current [UserSession] if the user is logged in,
     * or `null` if the user is logged out or session is empty.
     */
    val session: Flow<UserSession?> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences ->
            val token = preferences[Keys.Token].orEmpty()
            if (token.isBlank()) {
                null
            } else {
                UserSession(
                    token = token,
                    role = UserRole.from(preferences[Keys.Role]),
                    displayName = preferences[Keys.DisplayName].orEmpty().ifBlank { "Người dùng" },
                    username = preferences[Keys.Username].orEmpty()
                )
            }
        }

    /**
     * Persists the given [UserSession] details into the encrypted/local DataStore.
     */
    suspend fun save(session: UserSession) {
        dataStore.edit { preferences ->
            preferences[Keys.Token] = session.token
            preferences[Keys.Role] = session.role.name
            preferences[Keys.DisplayName] = session.displayName
            preferences[Keys.Username] = session.username
        }
    }

    /**
     * Clears all session preferences, effectively logging the user out.
     */
    suspend fun clear() {
        dataStore.edit { it.clear() }
    }


    private object Keys {
        val Token = stringPreferencesKey("token")
        val Role = stringPreferencesKey("role")
        val DisplayName = stringPreferencesKey("display_name")
        val Username = stringPreferencesKey("username")
    }
}
