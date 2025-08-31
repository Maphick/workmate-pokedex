package com.workmate.pokedex.data.remote.dto

import com.squareup.moshi.Json

data class PokemonDetailDto(
    val id: Int,
    val name: String,
    @Json(name = "base_experience") val baseExperience: Int?,
    val height: Int?,
    val weight: Int?,
    val types: List<PokemonTypeDto> = emptyList(),
    val sprites: PokemonSpritesDto? = null
)

data class PokemonTypeDto(
    val slot: Int,
    val type: NamedApiResourceDto
)

data class PokemonSpritesDto(val other: OtherSpritesDto? = null)
data class OtherSpritesDto(@Json(name = "official-artwork") val officialArtwork: OfficialArtworkDto? = null)
data class OfficialArtworkDto(@Json(name = "front_default") val frontDefault: String?)
