package com.workmate.pokedex.domain.usecase

import com.workmate.pokedex.domain.repository.PokemonRepository
import javax.inject.Inject

// первичная индексация БД (имена/типы) для офлайна.
class RefreshBootstrapUseCase @Inject constructor(
    private val repo: PokemonRepository
) {
    suspend operator fun invoke() = repo.bootstrapIndex()
}
