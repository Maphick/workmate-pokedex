package com.workmate.pokedex.data.remote

import com.workmate.pokedex.data.remote.dto.PokemonDetailDto
import com.workmate.pokedex.data.remote.dto.PokemonPageDto
import com.workmate.pokedex.data.remote.dto.TypeDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PokeApi {

    @GET("pokemon")
    suspend fun getPokemonPage(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): PokemonPageDto

    @GET("pokemon/{id}")
    suspend fun getPokemonDetail(@Path("id") id: Int): PokemonDetailDto

    @GET("type/{name}")
    suspend fun getType(@Path("name") name: String): TypeDto
}
