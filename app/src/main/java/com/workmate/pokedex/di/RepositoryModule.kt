package com.workmate.pokedex.di


import com.workmate.pokedex.data.repository.FiltersRepositoryImpl
import com.workmate.pokedex.data.repository.PokemonRepositoryImpl
import com.workmate.pokedex.domain.repository.FiltersRepository
import com.workmate.pokedex.domain.repository.PokemonRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindPokemonRepository(impl: PokemonRepositoryImpl): PokemonRepository


    @Binds @Singleton
    abstract fun bindFiltersRepository(impl: FiltersRepositoryImpl): FiltersRepository
}