package com.workmate.pokedex.data.local

import androidx.room.*
import com.workmate.pokedex.data.local.entities.SyncState

@Dao
interface SyncStateDao {
    @Query("SELECT * FROM sync_state WHERE id = 1")
    suspend fun get(): SyncState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: SyncState)

    @Query("DELETE FROM sync_state")
    suspend fun clear()
}
