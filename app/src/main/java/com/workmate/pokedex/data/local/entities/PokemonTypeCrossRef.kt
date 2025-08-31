package com.workmate.pokedex.data.local.entities


import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index


@Entity(
    tableName = "pokemon_type",
    primaryKeys = ["pokemonId", "typeId"],
    foreignKeys = [
        ForeignKey(
            entity = PokemonEntity::class,
            parentColumns = ["id"],
            childColumns = ["pokemonId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["typeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("pokemonId"), Index("typeId")]
)
data class PokemonTypeCrossRef(
    val pokemonId: Int,
    val typeId: Int
)
