package com.workmate.pokedex.domain.usecase

import com.workmate.pokedex.domain.repository.FiltersRepository
import javax.inject.Inject

class GetSelectedTypesUseCase @Inject constructor(
    private val repo: FiltersRepository
) {
    suspend operator fun invoke(): List<String> = repo.getSelectedTypes()
}