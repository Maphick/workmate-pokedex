package com.workmate.pokedex.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.workmate.pokedex.data.local.entities.RemoteKeys

@Dao
interface RemoteKeysDao {
    @Query("SELECT * FROM remote_keys WHERE pokemonId = :id")
    suspend fun remoteKeysByPokemonId(id: Int): RemoteKeys?

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertAll(keys: List<RemoteKeys>)

    @Query("DELETE FROM remote_keys")
    suspend fun clearRemoteKeys()
}