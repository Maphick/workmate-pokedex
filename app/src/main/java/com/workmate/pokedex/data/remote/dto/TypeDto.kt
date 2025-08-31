package com.workmate.pokedex.data.remote.dto

data class TypeDto(
    val id: Int,
    val name: String,
    val pokemon: List<TypePokemonSlot>
)

data class TypePokemonSlot(
    val slot: Int? = null,
    val pokemon: NamedApiResourceDto
)
