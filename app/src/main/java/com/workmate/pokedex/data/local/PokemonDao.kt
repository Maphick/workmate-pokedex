package com.workmate.pokedex.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import com.workmate.pokedex.data.local.entities.PokemonDetailEntity
import com.workmate.pokedex.data.local.entities.PokemonEntity
import com.workmate.pokedex.data.local.entities.PokemonTypeCrossRef
import com.workmate.pokedex.data.local.entities.TypeEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для списка покемонов, их типов и кеша деталей.
 *
 * Таблицы:
 * - pokemon(id, name, imageUrl)
 * - type(id, name)
 * - pokemon_type(pokemonId, typeId)
 * - pokemon_detail(id, height, weight, baseExperience)
 */
@Dao
interface PokemonDao {

    @Query("SELECT COUNT(*) FROM type")
    suspend fun getTypesCount(): Int

    @Query("SELECT COUNT(*) FROM pokemon_type")
    suspend fun getCrossRefsCount(): Int

    @Query("DELETE FROM type")
    suspend fun clearTypes()


    // метод для подсчета покемонов
    @Query("SELECT COUNT(*) FROM pokemon")
    suspend fun getPokemonCount(): Int

    @Query("SELECT id FROM pokemon WHERE id IN (:ids)")
    suspend fun existingPokemonIds(ids: List<Int>): List<Int>

    // ---- LIST + SEARCH/PAGING ----

    // PokemonDao.kt - два отдельных метода
    /** Фильтрация по ЛЮБОМУ из выбранных типов (OR) */
    @RewriteQueriesToDropUnusedColumns
    @Query("""
    SELECT DISTINCT p.*
    FROM pokemon p
    JOIN pokemon_type pt ON pt.pokemonId = p.id
    JOIN type t ON t.id = pt.typeId
    WHERE (:query IS NULL OR :query = '' OR p.name LIKE '%' || :query || '%')
      AND t.name IN (:types)
    ORDER BY p.id ASC
""")
    fun pagingByNameAndAnyTypes(
        query: String?,
        types: List<String>
    ): PagingSource<Int, PokemonEntity>

    /** Фильтрация по ВСЕМ выбранным типам (AND) */
    @RewriteQueriesToDropUnusedColumns
    @Query("""
    SELECT p.*
    FROM pokemon p
    JOIN pokemon_type pt ON pt.pokemonId = p.id
    JOIN type t ON t.id = pt.typeId
    WHERE (:query IS NULL OR :query = '' OR p.name LIKE '%' || :query || '%')
      AND t.name IN (:types)
    GROUP BY p.id
    HAVING COUNT(DISTINCT t.name) = :typeCount
    ORDER BY p.id ASC
""")
    fun pagingByNameAndAllTypes(
        query: String?,
        types: List<String>,
        typeCount: Int
    ): PagingSource<Int, PokemonEntity>

    /** Поиск по имени без фильтров типов */
    @Query("""
        SELECT * FROM pokemon
        WHERE (:query IS NULL OR :query = '' OR name LIKE '%' || :query || '%')
        ORDER BY id ASC
    """)
    fun pagingByName(query: String?): PagingSource<Int, PokemonEntity>

    /** Поиск по имени + отбор по ВСЕМ выбранным типам */
    @RewriteQueriesToDropUnusedColumns
    @Query("""
    SELECT DISTINCT p.*
    FROM pokemon p
    JOIN pokemon_type pt ON pt.pokemonId = p.id
    JOIN type t ON t.id = pt.typeId
    WHERE (:query IS NULL OR :query = '' OR p.name LIKE '%' || :query || '%')
      AND t.name IN (:types)
    ORDER BY p.id ASC
""")
    fun pagingByNameAndTypes(
        query: String?,
        types: List<String>
    ): PagingSource<Int, PokemonEntity>

    // ---- DETAIL / TYPES ----

    /** Имена типов покемона (для экрана деталей) */
    @Query("""
        SELECT t.name
        FROM type t
        JOIN pokemon_type pt ON pt.typeId = t.id
        WHERE pt.pokemonId = :pokemonId
        ORDER BY t.name ASC
    """)
    suspend fun getTypeNamesForPokemon(pokemonId: Int): List<String>

    /** Вытащить TypeEntity по именам (для связей без лишних сетевых запросов) */
    @Query("SELECT * FROM type WHERE name IN (:names)")
    suspend fun getTypesByNames(names: List<String>): List<TypeEntity>


    // ---- TYPES LIST (для экрана фильтров) ----
    @Query("SELECT name FROM type ORDER BY name ASC")
    fun getAllTypeNamesFlow(): Flow<List<String>>

    // ---- BASE ENTITIES READ ----
    @Query("SELECT * FROM pokemon WHERE id = :id LIMIT 1")
    suspend fun getPokemonById(id: Int): PokemonEntity?

    @Query("SELECT * FROM pokemon_detail WHERE id = :id LIMIT 1")
    suspend fun getDetail(id: Int): PokemonDetailEntity?

    // ---- UPSERT DETAIL ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDetail(detail: PokemonDetailEntity)


    // ---- UPSERTы как и были ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(pokemons: List<PokemonEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTypes(types: List<TypeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCrossRefs(refs: List<PokemonTypeCrossRef>)

    // --- CLEAR (для REFRESH) ---
    @Query("DELETE FROM pokemon")
    suspend fun clearPokemon()

    @Query("DELETE FROM pokemon_type")
    suspend fun clearCrossRefs()



    @Query("""
    INSERT OR REPLACE INTO pokemon_type(pokemonId, typeId)
    SELECT :pokemonId, :typeId
    WHERE EXISTS(SELECT 1 FROM pokemon WHERE id = :pokemonId)
    """)
    suspend fun insertCrossRefIfPokemonExists(pokemonId: Int, typeId: Int)


    // (опционально, если где-то чистятся детали)
    // @Query("DELETE FROM pokemon_detail")
    // suspend fun clearDetails()

}
