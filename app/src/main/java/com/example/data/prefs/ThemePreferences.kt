package com.example.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.StoreThemeOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.storeDataStore by preferencesDataStore(name = "turbo_store_prefs")

class ThemePreferences(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_color_key")

    val themeFlow: Flow<StoreThemeOption> = context.storeDataStore.data.map { prefs ->
        StoreThemeOption.fromKey(prefs[themeKey] ?: StoreThemeOption.EMERALD.key)
    }

    suspend fun saveTheme(option: StoreThemeOption) {
        context.storeDataStore.edit { prefs ->
            prefs[themeKey] = option.key
        }
    }
}
