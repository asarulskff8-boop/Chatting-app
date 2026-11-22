package com.example.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "chatting_app_prefs")

data class UserPreferences(
    val keepMeSignedIn: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val languageCode: String = "English",
    val biometricLockEnabled: Boolean = false,
    val chatWallpaperIndex: Int = 0,
    val lastSignedInEmail: String = ""
)

class AppPreferences(private val context: Context) {

    private object Keys {
        val KEEP_ME_SIGNED_IN = booleanPreferencesKey("keep_me_signed_in")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
        val CHAT_WALLPAPER = intPreferencesKey("chat_wallpaper")
        val LAST_EMAIL = stringPreferencesKey("last_email")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            keepMeSignedIn = prefs[Keys.KEEP_ME_SIGNED_IN] ?: true,
            themeMode = ThemeMode.fromKey(prefs[Keys.THEME_MODE]),
            languageCode = prefs[Keys.LANGUAGE_CODE] ?: "English",
            biometricLockEnabled = prefs[Keys.BIOMETRIC_LOCK] ?: false,
            chatWallpaperIndex = prefs[Keys.CHAT_WALLPAPER] ?: 0,
            lastSignedInEmail = prefs[Keys.LAST_EMAIL] ?: ""
        )
    }

    suspend fun setKeepMeSignedIn(enabled: Boolean) {
        context.dataStore.edit { it[Keys.KEEP_ME_SIGNED_IN] = enabled }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.key }
    }

    suspend fun setLanguageCode(language: String) {
        context.dataStore.edit { it[Keys.LANGUAGE_CODE] = language }
    }

    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_LOCK] = enabled }
    }

    suspend fun setChatWallpaperIndex(index: Int) {
        context.dataStore.edit { it[Keys.CHAT_WALLPAPER] = index }
    }

    suspend fun setLastSignedInEmail(email: String) {
        context.dataStore.edit { it[Keys.LAST_EMAIL] = email }
    }
}
