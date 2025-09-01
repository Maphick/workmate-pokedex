package com.workmate.pokedex.domain.usecase

import com.workmate.pokedex.domain.repository.PokemonRepository
import javax.inject.Inject

class ClearAllDataUseCase @Inject constructor(
    private val repo: PokemonRepository
) {
    suspend operator fun invoke() = repo.clearAllData()
}