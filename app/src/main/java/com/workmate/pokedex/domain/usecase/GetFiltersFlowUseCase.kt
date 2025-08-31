package com.workmate.pokedex.domain.usecase

import com.workmate.pokedex.domain.repository.FiltersRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// текущие фильтры из DataStore
class GetFiltersFlowUseCase @Inject constructor(
    private val repo: FiltersRepository
) {
    operator fun invoke(): Flow<List<String>> = repo.selectedTypesFlow()
}