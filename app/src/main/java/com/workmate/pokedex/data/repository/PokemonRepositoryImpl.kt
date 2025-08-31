package com.workmate.pokedex.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.withTransaction
import com.workmate.pokedex.data.local.AppDatabase
import com.workmate.pokedex.data.local.entities.PokemonDetailEntity
import com.workmate.pokedex.data.local.entities.PokemonEntity
import com.workmate.pokedex.data.local.entities.PokemonTypeCrossRef
import com.workmate.pokedex.data.local.entities.TypeEntity
import com.workmate.pokedex.data.mapper.extractIdFromUrl
import com.workmate.pokedex.data.mapper.officialArtworkUrl
import com.workmate.pokedex.data.remote.PokeApi
import com.workmate.pokedex.domain.model.Pokemon
import com.workmate.pokedex.domain.model.PokemonDetail
import com.workmate.pokedex.domain.repository.PokemonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@OptIn(ExperimentalPagingApi::class)
class PokemonRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val api: PokeApi
) : PokemonRepository {

    /**
     * Список + поиск + фильтры типов.
     * Если типов нет — простой поиск по имени.
     * Если есть — JOIN по type + HAVING COUNT(DISTINCT) = size (см. Dao.pagingByNameAndTypes).
     */
    override fun pagingFlow(query: String?, types: List<String>): Flow<PagingData<Pokemon>> {
        val sourceFactory = if (types.isNullOrEmpty()) {
            { db.pokemonDao().pagingByName(query) }
        } else {
            { db.pokemonDao().pagingByNameAndTypes(query, types, types.size) }
        }

        return Pager(
            config = PagingConfig(pageSize = 40, prefetchDistance = 2),
            remoteMediator = PokemonRemoteMediator(db, api),
            pagingSourceFactory = sourceFactory
        ).flow.map { page -> page.map { Pokemon(it.id, it.name, it.imageUrl) } }
    }

    /**
     * Первичная инициализация кэша типов и связей pokemon_type.
     * Берём фиксированный список основных типов, тянем их из API, сохраняем type и кросс-рефы.
     */
    override suspend fun bootstrapIndex() {
        // 0) сетевые вызовы ВНЕ транзакции
        val typeNames = listOf(
            "normal","fire","water","electric","grass","ice",
            "fighting","poison","ground","flying","psychic","bug",
            "rock","ghost","dragon","dark","steel","fairy"
        )
        val typeDtos = typeNames.map { api.getType(it) } // network
        val typeEntities = typeDtos.map { TypeEntity(id = it.id, name = it.name) }

        // соберём все потенциальные кросс-рефы
        val allRefs = buildList<PokemonTypeCrossRef> {
            typeDtos.forEach { t ->
                t.pokemon.forEach { slot ->
                    val pid = extractIdFromUrl(slot.pokemon.url) // .../pokemon/35/
                    add(PokemonTypeCrossRef(pokemonId = pid, typeId = t.id))
                }
            }
        }

        db.withTransaction {
            // 1) сначала сохраним сами типы
            db.pokemonDao().upsertTypes(typeEntities)

            // 2) добавим связи только для уже существующих в БД покемонов
            if (allRefs.isNotEmpty()) {
                val distinctIds = allRefs.map { it.pokemonId }.distinct()
                val existing = mutableSetOf<Int>()

                // ВАЖНО: чанк по ~900, чтобы не превысить лимит переменных SQLite
                distinctIds.chunked(900).forEach { chunk ->
                    existing += db.pokemonDao().existingPokemonIds(chunk)
                }

                val filtered = allRefs.filter { it.pokemonId in existing }
                if (filtered.isNotEmpty()) {
                    db.pokemonDao().upsertCrossRefs(filtered)
                }
            }
        }
    }

    /** Живой список названий типов для экрана фильтров */
    override fun allTypesFlow(): Flow<List<String>> =
        db.pokemonDao().getAllTypeNamesFlow()

    /**
     * Деталка: берём из кэша, если всё есть. Иначе подгружаем,
     * дописываем detail, картинку и (при необходимости) типы и кросс-рефы.
     */
    override suspend fun getPokemonDetail(id: Int): PokemonDetail {
        val base = db.pokemonDao().getPokemonById(id)
        val cached = db.pokemonDao().getDetail(id)
        var typeNames = db.pokemonDao().getTypeNamesForPokemon(id)

        // Полностью из кэша
        if (cached != null && base != null && typeNames.isNotEmpty()) {
            return PokemonDetail(
                id = id,
                name = base.name,
                imageUrl = base.imageUrl,
                height = cached.height,
                weight = cached.weight,
                baseExperience = cached.baseExperience,
                types = typeNames
            )
        }

        // Сеть
        val dto = api.getPokemonDetail(id)

        // 1) детальные поля
        val detail = PokemonDetailEntity(
            id = dto.id,
            height = dto.height ?: 0,
            weight = dto.weight ?: 0,
            baseExperience = dto.baseExperience ?: 0
        )

        // 2) официальная картинка
        val artFromApi = dto.sprites?.other?.officialArtwork?.frontDefault
        val image = artFromApi ?: officialArtworkUrl(dto.id)

        // 3) если в БД ещё нет типов — достроим из dto.types
        val typeEntitiesToInsert = mutableListOf<TypeEntity>()
        val crossRefsToInsert = mutableListOf<PokemonTypeCrossRef>()
        if (typeNames.isEmpty()) {
            dto.types.forEach { t ->
                val typeId = extractIdFromUrl(t.type.url)      // .../type/10/
                val typeName = t.type.name.lowercase()
                typeEntitiesToInsert += TypeEntity(id = typeId, name = typeName)
                crossRefsToInsert += PokemonTypeCrossRef(pokemonId = dto.id, typeId = typeId)
            }
        }

        // 4) транзакция: сохраняем всё в БД
        db.withTransaction {
            db.pokemonDao().upsertDetail(detail)

            if (base == null) {
                db.pokemonDao().upsertAll(
                    listOf(PokemonEntity(id = dto.id, name = dto.name, imageUrl = image))
                )
            } else if (artFromApi != null && base.imageUrl != artFromApi) {
                // если API дал лучший арт — обновим
                db.pokemonDao().upsertAll(listOf(base.copy(imageUrl = artFromApi)))
            }

            if (typeEntitiesToInsert.isNotEmpty()) db.pokemonDao().upsertTypes(typeEntitiesToInsert)
            if (crossRefsToInsert.isNotEmpty()) db.pokemonDao().upsertCrossRefs(crossRefsToInsert)
        }

        // 5) перечитываем базу: типы теперь должны быть
        val finalBase = base?.copy(imageUrl = image) ?: db.pokemonDao().getPokemonById(id)!!
        typeNames = db.pokemonDao().getTypeNamesForPokemon(id)

        return PokemonDetail(
            id = id,
            name = finalBase.name,
            imageUrl = finalBase.imageUrl,
            height = detail.height,
            weight = detail.weight,
            baseExperience = detail.baseExperience,
            types = typeNames
        )
    }
}
