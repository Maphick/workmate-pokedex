package com.workmate.pokedex.data.repository

import com.workmate.pokedex.data.datastore.FiltersDataStore
import com.workmate.pokedex.domain.repository.FiltersRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FiltersRepositoryImpl @Inject constructor(
    private val ds: FiltersDataStore
) : FiltersRepository {

    override fun selectedTypesFlow(): Flow<List<String>> = ds.selectedTypesFlow

    override suspend fun saveSelectedTypes(types: List<String>) {
        println("Saving selected types: $types")
        ds.saveSelectedTypes(types)
    }

    override suspend fun clearSelectedTypes() {
        println("Clearing selected types")
        ds.saveSelectedTypes(emptyList())
    }

    override suspend fun getSelectedTypes(): List<String> {
        return ds.selectedTypesFlow.first()
    }
}