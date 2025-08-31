package com.workmate.pokedex.domain.usecase

import com.workmate.pokedex.domain.repository.FiltersRepository
import javax.inject.Inject

// сохранить выбранные типы.

class SaveFiltersUseCase @Inject constructor(
    private val repo: FiltersRepository
) {
    suspend operator fun invoke(types: List<String>) {
        repo.saveSelectedTypes(types.map { it.lowercase() })
    }
}
