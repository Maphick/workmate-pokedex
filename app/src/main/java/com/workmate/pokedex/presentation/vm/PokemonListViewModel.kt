package com.workmate.pokedex.presentation.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.workmate.pokedex.domain.model.Pokemon
import com.workmate.pokedex.domain.usecase.ClearFiltersUseCase
import com.workmate.pokedex.domain.usecase.GetFiltersFlowUseCase
import com.workmate.pokedex.domain.usecase.GetPagedPokemonUseCase
import com.workmate.pokedex.domain.usecase.RefreshBootstrapUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class PokemonListViewModel @Inject constructor(
    private val getPaged: GetPagedPokemonUseCase,
    private val refresh: RefreshBootstrapUseCase,
    private val clearFilters: ClearFiltersUseCase,
    getFilters: GetFiltersFlowUseCase
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val selectedTypesFlow = getFilters()
    private val _selectedTypes = MutableStateFlow<List<String>>(emptyList())
    val selectedTypes: StateFlow<List<String>> = _selectedTypes.asStateFlow()
    val isRefreshing = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            // СНАЧАЛА загружаем bootstrap данные
            refresh()

            // ПОТОМ подписываемся на изменения фильтров
            selectedTypesFlow.collect { types ->
                println("Selected types updated: $types")
                _selectedTypes.value = types
            }
        }
    }

    // при фильтрации покемонов: t - кол-во выбранных типов
    val paging: Flow<PagingData<Pokemon>> =
        combine(query, _selectedTypes) { q, t ->
            println("Combine: query='$q', types=$t")
            q to t
        }
            .flatMapLatest { (q, t) ->
                println("FlatMapLatest: query='$q', types=$t")
                // Принудительно инвалидируем предыдущий PagingSource
                getPaged(q, t)
            }
            .cachedIn(viewModelScope)

    fun onSearch(text: String) { query.value = text }

    fun clearFilters() = viewModelScope.launch {
        // Здесь нужно очистить фильтры в репозитории/DataStore
        // Для этого добавлен соответствующий метод в FiltersRepository
        // СТАЛО:

        clearFilters.invoke() // Вызываем use case

    }

    fun onRefresh() = viewModelScope.launch {
        isRefreshing.value = true
        try {
            refresh()
        } finally { isRefreshing.value = false }
    }

}