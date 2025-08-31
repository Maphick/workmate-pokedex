package com.workmate.pokedex.data.local


import androidx.room.Database
import androidx.room.RoomDatabase
import com.workmate.pokedex.data.local.entities.*


@Database(
    entities = [
        PokemonEntity::class,
        TypeEntity::class,
        PokemonTypeCrossRef::class,
        RemoteKeys::class,
        SyncState::class,
        PokemonDetailEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pokemonDao(): PokemonDao
    abstract fun remoteKeysDao(): RemoteKeysDao
    abstract fun syncStateDao(): SyncStateDao
}