package com.workmate.pokedex.data.mapper


fun extractIdFromUrl(url: String): Int =
    url.trimEnd('/').substringAfterLast('/').toInt()

fun officialArtworkUrl(id: Int): String =
    "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"

