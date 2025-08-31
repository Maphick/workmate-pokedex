package com.workmate.pokedex.domain.usecase


import com.workmate.pokedex.domain.model.PokemonDetail
import com.workmate.pokedex.domain.repository.PokemonRepository
import javax.inject.Inject


// детальная карточка (кеш + сеть).
class GetPokemonDetailUseCase @Inject constructor(
    private val repo: PokemonRepository
) {
    suspend operator fun invoke(id: Int): PokemonDetail = repo.getPokemonDetail(id)
}