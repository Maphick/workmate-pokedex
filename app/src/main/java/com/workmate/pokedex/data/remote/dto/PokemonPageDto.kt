package com.workmate.pokedex.data.remote.dto

data class NamedApiResourceDto(
    val name: String,
    val url: String
)

data class PokemonPageDto(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<NamedApiResourceDto>
)
