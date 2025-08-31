package com.workmate.pokedex.data.local


import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow


@Dao
interface TypeDao {
    @Query("SELECT name FROM type ORDER BY name ASC")
    fun getAllTypeNamesFlow(): Flow<List<String>>
}