package com.workmate.pokedex.domain.repository

import androidx.paging.PagingData
import com.workmate.pokedex.domain.model.PokemonDetail
import com.workmate.pokedex.domain.model.Pokemon
import kotlinx.coroutines.flow.Flow

interface PokemonRepository {
    /** Поток PagingData для списка (с учётом query + выбранных типов) */
    fun pagingFlow(query: String?, types: List<String>): Flow<PagingData<Pokemon>>

    /** Первичная индексация/обновление кеша (имена + типы) */
    suspend fun bootstrapIndex()
    // очистка таблиц
    suspend fun clearAllData()

    /** Все доступные типы (для экрана фильтров) */
    fun allTypesFlow(): Flow<List<String>>

    /** Детальная инфа по покемону (с офлайн-кэшем) */
    suspend fun getPokemonDetail(id: Int): PokemonDetail
}
