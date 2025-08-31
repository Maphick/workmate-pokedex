package com.workmate.pokedex.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement.SpaceBetween
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.workmate.pokedex.presentation.vm.PokemonDetailViewModel
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PokemonDetailScreen(
    id: Int,
    onBack: () -> Unit,
    vm: PokemonDetailViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(id) { vm.load(id) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = when (val s = state) {
                        is PokemonDetailViewModel.UiState.Data ->
                            s.d.name.replaceFirstChar { it.uppercase() }
                        is PokemonDetailViewModel.UiState.Error -> "Ошибка"
                        PokemonDetailViewModel.UiState.Loading -> "Загрузка…"
                    }
                    Text(title)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                PokemonDetailViewModel.UiState.Loading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                is PokemonDetailViewModel.UiState.Error -> {
                    Column(
                        Modifier.align(Alignment.Center).padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(s.msg)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { vm.load(id) }) { Text("Повторить") }
                    }
                }
                is PokemonDetailViewModel.UiState.Data -> {
                    DetailContent(
                        name = s.d.name,
                        imageUrl = s.d.imageUrl,
                        types = s.d.types,
                        height = s.d.height,
                        weight = s.d.weight,
                        baseExp = s.d.baseExperience
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailContent(
    name: String,
    imageUrl: String,
    types: List<String>,
    height: Int?,
    weight: Int?,
    baseExp: Int?
) {
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(model = imageUrl, contentDescription = name, modifier = Modifier.size(200.dp))
        Spacer(Modifier.height(12.dp))
        Text(name.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))

        if (types.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                types.forEach { t ->
                    AssistChip(
                        onClick = {},
                        label = { Text(t.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        InfoRow("Height", height?.toString() ?: "-")
        InfoRow("Weight", weight?.toString() ?: "-")
        InfoRow("Base EXP", baseExp?.toString() ?: "-")
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
    Divider(Modifier.padding(vertical = 8.dp))
}
