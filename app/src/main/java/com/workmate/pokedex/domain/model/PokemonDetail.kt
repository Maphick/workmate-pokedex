package com.workmate.pokedex.domain.model


data class PokemonDetail(
    val id: Int,
    val name: String,
    val imageUrl: String,
    val height: Int?,
    val weight: Int?,
    val baseExperience: Int?,
    val types: List<String>
)