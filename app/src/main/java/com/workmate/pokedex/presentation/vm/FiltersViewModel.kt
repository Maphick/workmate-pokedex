package com.workmate.pokedex.presentation.vm


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workmate.pokedex.domain.usecase.GetFiltersFlowUseCase
import com.workmate.pokedex.domain.usecase.SaveFiltersUseCase
import com.workmate.pokedex.data.local.AppDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class FiltersViewModel @Inject constructor(
    private val db: AppDatabase,
    private val getSelected: GetFiltersFlowUseCase,
    private val saveSelected: SaveFiltersUseCase
) : ViewModel() {

    val allTypes: StateFlow<List<String>> = db.pokemonDao().getAllTypeNamesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selected = MutableStateFlow<List<String>>(emptyList())
    val selected: StateFlow<List<String>> = _selected
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { getSelected().collect { _selected.value = it } }
    }

    fun toggle(type: String) {
        val set = _selected.value.toMutableSet()
        if (!set.add(type)) set.remove(type)
        _selected.value = set.sorted()
    }

    fun apply() = viewModelScope.launch { saveSelected(_selected.value) }
}
