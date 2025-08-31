package com.workmate.pokedex.presentation.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.workmate.pokedex.domain.model.Pokemon
import com.workmate.pokedex.domain.usecase.GetFiltersFlowUseCase
import com.workmate.pokedex.domain.usecase.GetPagedPokemonUseCase
import com.workmate.pokedex.domain.usecase.RefreshBootstrapUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

// PokemonListViewModel.kt
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class PokemonListViewModel @Inject constructor(
    private val getPaged: GetPagedPokemonUseCase,
    private val refresh: RefreshBootstrapUseCase,
    getFilters: GetFiltersFlowUseCase
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val selectedTypes = getFilters() // Flow<List<String>>
    val isRefreshing = MutableStateFlow(false)

    init {
        // разово индексируем имена и типы (если ещё не)
        viewModelScope.launch { refresh() }
    }

    val paging: Flow<PagingData<Pokemon>> =
        combine(query, selectedTypes) { q, t -> q to t }
            .flatMapLatest { (q, t) -> getPaged(q, t) }
            .cachedIn(viewModelScope)

    fun onSearch(text: String) { query.value = text }
    fun onRefresh() = viewModelScope.launch {
        isRefreshing.value = true
        try { refresh() } finally { isRefreshing.value = false }
    }
}
