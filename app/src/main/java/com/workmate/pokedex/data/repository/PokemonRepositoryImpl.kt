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


    // очистка таблиц
    override suspend fun clearAllData() {
        println("Clearing all data...")
        try {
            db.withTransaction {
                // 1. Очищаем связи
                db.pokemonDao().clearCrossRefs()
                println("Cleared cross-refs")

                // 2. Очищаем покемонов
                db.pokemonDao().clearPokemon()
                println("Cleared pokemons")

                // 3. Очищаем типы
                db.pokemonDao().clearTypes()
                println("Cleared types")

                // 4. Очищаем ключи пагинации
                db.remoteKeysDao().clearRemoteKeys()
                println("Cleared remote keys")

                // 5. Очищаем детали (если нужно)
                // db.pokemonDao().clearDetails()
            }
            println("All data cleared successfully")
        } catch (e: Exception) {
            println("Error clearing data: ${e.message}")
            throw e
        }
    }

    /**
     * Список + поиск + фильтры типов.
     * Если типов нет — простой поиск по имени.
     * Если есть — JOIN по type + HAVING COUNT(DISTINCT) = size (см. Dao.pagingByNameAndTypes).
     */

    override fun pagingFlow(query: String?, types: List<String>): Flow<PagingData<Pokemon>> {
        val sourceFactory = if (types.isNullOrEmpty()) {
            { db.pokemonDao().pagingByName(query) }
        } else {
            // любой из методов в зависимости от нужной логики:
            { db.pokemonDao().pagingByNameAndAnyTypes(query, types) } // ЛЮБОЙ тип
            // ИЛИ:
            // { db.pokemonDao().pagingByNameAndAllTypes(query, types, types.size) } // ВСЕ типы
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
    //  это кастомный метод инициализации, предназначенный для первичной загрузки и кэширования
    //  данных о типах покемонов и их связях с покемонами в локальной базе данных
    override suspend fun bootstrapIndex() {
        println("=== BOOTSTRAP INDEX STARTED ===")

        // Проверяем, нужно ли загружать типы
        val existingTypesCount = db.pokemonDao().getTypesCount()
        val existingRefsCount = db.pokemonDao().getCrossRefsCount()

        if (existingTypesCount >= 18 && existingRefsCount > 0) {
            println("Bootstrap already completed, skipping")
            return
        }

        // Загружаем только недостающие данные
        val typeNames = listOf(
            "normal","fire","water","electric","grass","ice",
            "fighting","poison","ground","flying","psychic","bug",
            "rock","ghost","dragon","dark","steel","fairy"
        )

        println("Loading types and relationships...")

        val typeDtos = typeNames.mapNotNull { typeName ->
            try {
                api.getType(typeName)
            } catch (e: Exception) {
                println("Failed to load type: $typeName")
                null
            }
        }

        val typeEntities = typeDtos.map { TypeEntity(id = it.id, name = it.name) }

        // Фильтруем связи для существующих покемонов
        val allRefs = buildList<PokemonTypeCrossRef> {
            typeDtos.forEach { typeDto ->
                typeDto.pokemon.forEach { pokemonSlot ->
                    val pokemonId = extractIdFromUrl(pokemonSlot.pokemon.url)
                    // Проверяем существование покемона
                    if (db.pokemonDao().getPokemonById(pokemonId) != null) {
                        add(PokemonTypeCrossRef(pokemonId = pokemonId, typeId = typeDto.id))
                    }
                }
            }
        }

        db.withTransaction {
            // Добавляем только новые типы и связи
            db.pokemonDao().upsertTypes(typeEntities)
            if (allRefs.isNotEmpty()) {
                db.pokemonDao().upsertCrossRefs(allRefs)
            }
        }

        println("=== BOOTSTRAP INDEX COMPLETED ===")
    }

    // метод для проверки загрузки покемонов
    private suspend fun ensurePokemonDataLoaded() {
        val count = db.pokemonDao().getPokemonCount()
        println("Pokemon in DB: $count")

        if (count == 0) {
            println("Loading initial pokemon data...")
            loadInitialPokemonData()
        }
    }

    private suspend fun loadInitialPokemonData() {
        // Загружаем первую страницу покемонов
        val page = api.getPokemonPage(limit = 100, offset = 0)
        val entities = page.results.map {
            val id = extractIdFromUrl(it.url)
            PokemonEntity(id = id, name = it.name, imageUrl = officialArtworkUrl(id))
        }

        db.withTransaction {
            db.pokemonDao().upsertAll(entities)
        }
        println("Loaded ${entities.size} pokemons")
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
