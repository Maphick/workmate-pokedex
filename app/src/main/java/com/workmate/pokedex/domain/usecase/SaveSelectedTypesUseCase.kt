package com.workmate.pokedex.domain.usecase

import com.workmate.pokedex.domain.repository.FiltersRepository
import javax.inject.Inject

class SaveSelectedTypesUseCase @Inject constructor(
    private val repo: FiltersRepository
) {
    suspend operator fun invoke(types: List<String>) = repo.saveSelectedTypes(types)
}