package com.workmate.pokedex.presentation.vm


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workmate.pokedex.domain.model.PokemonDetail
import com.workmate.pokedex.domain.usecase.GetPokemonDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class PokemonDetailViewModel @Inject constructor(
    private val getDetail: GetPokemonDetailUseCase
) : ViewModel() {
    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state


    sealed interface UiState { object Loading: UiState; data class Data(val d: PokemonDetail): UiState; data class Error(val msg: String): UiState }


    fun load(id: Int) = viewModelScope.launch {
        _state.value = UiState.Loading
        try { _state.value = UiState.Data(getDetail(id)) } catch (e: Exception) { _state.value = UiState.Error(e.localizedMessage ?: "Ошибка") }
    }
}