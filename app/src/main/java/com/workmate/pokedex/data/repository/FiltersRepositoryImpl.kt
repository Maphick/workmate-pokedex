package com.workmate.pokedex.data.repository

import com.workmate.pokedex.data.datastore.FiltersDataStore
import com.workmate.pokedex.domain.repository.FiltersRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FiltersRepositoryImpl @Inject constructor(
    private val ds: FiltersDataStore
) : FiltersRepository {

    override fun selectedTypesFlow(): Flow<List<String>> = ds.selectedTypesFlow

    override suspend fun saveSelectedTypes(types: List<String>) {
        ds.saveSelectedTypes(types) // просто делегируем
    }
}
