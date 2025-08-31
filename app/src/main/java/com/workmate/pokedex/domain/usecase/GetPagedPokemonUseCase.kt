package com.workmate.pokedex.domain.usecase

import androidx.paging.PagingData
import com.workmate.pokedex.domain.model.Pokemon
import com.workmate.pokedex.domain.repository.PokemonRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// отдаёт поток для списка (репозиторий + Paging).
class GetPagedPokemonUseCase @Inject constructor(
    private val repo: PokemonRepository
) {
    operator fun invoke(query: String?, types: List<String>): Flow<PagingData<Pokemon>> =
        repo.pagingFlow(query, types)
}
