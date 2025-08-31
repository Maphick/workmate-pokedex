package com.workmate.pokedex.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// ВАЖНО: делегат объявляем на top-level (в файле), не внутри класса!
private val Context.filtersDataStore by preferencesDataStore(name = "filters_prefs")

@Singleton
class FiltersDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val KEY_SELECTED_TYPES = stringSetPreferencesKey("selected_types")

    /** Поток выбранных типов из DataStore (всегда в lowercase) */
    val selectedTypesFlow: Flow<List<String>> =
        context.filtersDataStore.data.map { prefs ->
            prefs[KEY_SELECTED_TYPES]?.toList()?.map { it.lowercase() }?.sorted().orEmpty()
        }

    /** Сохранить выбранные типы (приводим к lowercase) */
    suspend fun saveSelectedTypes(types: List<String>) {
        context.filtersDataStore.edit { prefs ->
            prefs[KEY_SELECTED_TYPES] = types.map { it.lowercase() }.toSet()
        }
    }
}
