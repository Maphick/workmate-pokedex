package com.workmate.pokedex.domain.repository


import kotlinx.coroutines.flow.Flow


interface FiltersRepository {
    fun selectedTypesFlow(): Flow<List<String>>
    suspend fun saveSelectedTypes(types: List<String>)
    suspend fun clearSelectedTypes()
    // Добавляем метод для получения текущих типов
    suspend fun getSelectedTypes(): List<String>
}