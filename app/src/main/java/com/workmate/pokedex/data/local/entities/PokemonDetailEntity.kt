package com.workmate.pokedex.data.local.entities


import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "pokemon_detail")
data class PokemonDetailEntity(
    @PrimaryKey val id: Int,
    val height: Int?,
    val weight: Int?,
    val baseExperience: Int?
)