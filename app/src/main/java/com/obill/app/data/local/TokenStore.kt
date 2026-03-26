package com.obill.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "obill_prefs")

class TokenStore(private val context: Context) {

    private val tokenKey = stringPreferencesKey("auth_token")
    private val textScaleKey = floatPreferencesKey("text_scale")

    val token: Flow<String?> = context.dataStore.data.map { it[tokenKey] }

    val textScale: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[textScaleKey] ?: 1f
    }

    suspend fun setToken(token: String) {
        context.dataStore.edit { it[tokenKey] = token }
    }

    suspend fun clearToken() {
        context.dataStore.edit { it.remove(tokenKey) }
    }

    suspend fun setTextScale(scale: Float) {
        context.dataStore.edit { it[textScaleKey] = scale.coerceIn(0.85f, 1.35f) }
    }
}
