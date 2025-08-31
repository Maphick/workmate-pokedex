package com.workmate.pokedex.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.workmate.pokedex.data.local.AppDatabase
import com.workmate.pokedex.data.local.entities.PokemonEntity
import com.workmate.pokedex.data.local.entities.RemoteKeys
import com.workmate.pokedex.data.mapper.extractIdFromUrl
import com.workmate.pokedex.data.mapper.officialArtworkUrl
import com.workmate.pokedex.data.remote.PokeApi

@OptIn(ExperimentalPagingApi::class)
class PokemonRemoteMediator(
    private val db: AppDatabase,
    private val api: PokeApi
) : RemoteMediator<Int, PokemonEntity>() {

    override suspend fun initialize(): InitializeAction =
        InitializeAction.LAUNCH_INITIAL_REFRESH

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PokemonEntity>
    ): MediatorResult = try {
        val pageSize = state.config.pageSize

        val offset = when (loadType) {
            LoadType.REFRESH -> 0
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> {
                val last = state.lastItemOrNull()
                    ?: return MediatorResult.Success(endOfPaginationReached = true)
                val key = db.remoteKeysDao().remoteKeysByPokemonId(last.id)
                    ?: return MediatorResult.Success(endOfPaginationReached = true)
                key.nextKey ?: return MediatorResult.Success(endOfPaginationReached = true)
            }
        }

        val page = api.getPokemonPage(limit = pageSize, offset = offset)
        val entities = page.results.map {
            val id = extractIdFromUrl(it.url)
            PokemonEntity(id = id, name = it.name, imageUrl = officialArtworkUrl(id))
        }

        val end = page.next == null
        val prevKey = if (offset == 0) null else (offset - pageSize).coerceAtLeast(0)
        val nextKey = if (end) null else offset + pageSize

        db.withTransaction {
            if (loadType == LoadType.REFRESH) {
                db.pokemonDao().clearPokemon()
                db.remoteKeysDao().clearRemoteKeys()
                db.pokemonDao().clearCrossRefs()
            }
            db.pokemonDao().upsertAll(entities)
            val keys = entities.map { RemoteKeys(pokemonId = it.id, prevKey = prevKey, nextKey = nextKey) }
            db.remoteKeysDao().insertAll(keys)
        }

        MediatorResult.Success(endOfPaginationReached = end)
    } catch (e: Exception) {
        MediatorResult.Error(e)
    }
}
