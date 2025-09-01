package com.workmate.pokedex.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.workmate.pokedex.domain.model.Pokemon
import com.workmate.pokedex.presentation.vm.PokemonListViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveFiltersChip(
    types: List<String>,
    onRemoveType: (String) -> Unit,
    onClear: () -> Unit
) {
    if (types.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Чипы занимают всё доступное место и переносятся на новые строки
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Фильтры:",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )

                types.forEach { type ->
                    FilterChip(
                        selected = true,
                        onClick = { /* no-op */ },
                        label = { Text(type.replaceFirstChar { it.uppercase() }) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Удалить фильтр",
                                modifier = Modifier
                                    .padding(start = 2.dp)
                                    .clickable { onRemoveType(type) }
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.padding(start = 8.dp))

            // Кнопка всегда остаётся справа и не уезжает
            TextButton(onClick = onClear) {
                Text("Очистить")
            }
        }
        Divider()
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonListScreen(
    onOpenFilters: () -> Unit,
    onOpenDetails: (Int) -> Unit,
    vm: PokemonListViewModel = hiltViewModel()
) {
    val lazy: LazyPagingItems<Pokemon> = vm.paging.collectAsLazyPagingItems()
    val snackbarHost = remember { SnackbarHostState() }
    var search by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }

    // --- FIX #1: Привязка pull-to-refresh к loadState.refresh, только когда список не пуст ---
    // Это убирает «двойной» лоадер (центр + индикатор refresh) и мигания на коротких списках.
    val isRefreshingFromLoadState by remember(lazy.loadState) {
        mutableStateOf(lazy.loadState.refresh is LoadState.Loading && lazy.itemCount > 0)
    }

    // Выбранные типы для чипов (как было)
    val selectedTypes by vm.selectedTypes.collectAsState()

    // Ошибки загрузки
    val appendError = lazy.loadState.append as? LoadState.Error
    val refreshError = lazy.loadState.refresh as? LoadState.Error

    // --- FIX #2: Не показывать snackbar, пока идёт refresh ---
    LaunchedEffect(appendError, refreshError, lazy.loadState.refresh) {
        if (lazy.loadState.refresh is LoadState.Loading) return@LaunchedEffect
        val err = appendError?.error ?: refreshError?.error
        if (err != null) {
            val res = snackbarHost.showSnackbar(
                message = err.localizedMessage ?: "Ошибка загрузки",
                actionLabel = "Повторить"
            )
            if (res == SnackbarResult.ActionPerformed) lazy.retry()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Покемоны") },
                actions = {
                    IconButton(onClick = onOpenFilters) {
                        Icon(Icons.Filled.Tune, contentDescription = "Фильтры")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        PullToRefreshBox(
            // --- FIX #1 применяется здесь ---
            isRefreshing = isRefreshingFromLoadState,
            onRefresh = { lazy.refresh() }, // рефрешим сам Paging (можно оставить vm.onRefresh() если он вызывает refresh())
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value = search,
                    onValueChange = {
                        search = it
                        vm.onSearch(it.text)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    placeholder = { Text("Поиск по имени…") }
                )

                ActiveFiltersChip(
                    types = selectedTypes,
                    onRemoveType = { vm.removeFilterType(it) },
                    onClear = { vm.clearFilters() }
                )

                // Основное состояние по refresh
                when (val s = lazy.loadState.refresh) {
                    is LoadState.Loading -> {
                        // Если элементов ещё нет — показываем один(!) центр-лоадер.
                        // Во время refresh нижний append-лоадер не рисуем (см. FIX #3 ниже в PokemonGrid).
                        if (lazy.itemCount == 0) {
                            Box(Modifier.fillMaxSize()) {
                                CircularProgressIndicator(Modifier.align(Alignment.Center))
                            }
                        } else {
                            PokemonGrid(
                                count = lazy.itemCount,
                                itemAt = { idx -> lazy[idx] },
                                onOpenDetails = onOpenDetails,
                                // --- FIX #3: Не показываем append-лоадер во время refresh ---
                                showAppendLoader = (lazy.loadState.append is LoadState.Loading) &&
                                        (lazy.loadState.refresh !is LoadState.Loading)
                            )
                        }
                    }
                    is LoadState.Error -> {
                        if (lazy.itemCount == 0) {
                            ErrorState(
                                message = s.error.localizedMessage ?: "Не удалось загрузить данные",
                                onRetry = { lazy.retry() }
                            )
                        } else {
                            PokemonGrid(
                                count = lazy.itemCount,
                                itemAt = { idx -> lazy[idx] },
                                onOpenDetails = onOpenDetails,
                                showAppendLoader = (lazy.loadState.append is LoadState.Loading) &&
                                        (lazy.loadState.refresh !is LoadState.Loading)
                            )
                        }
                    }
                    is LoadState.NotLoading -> {
                        if (lazy.itemCount == 0) {
                            EmptyState("Ничего не найдено")
                        } else {
                            PokemonGrid(
                                count = lazy.itemCount,
                                itemAt = { idx -> lazy[idx] },
                                onOpenDetails = onOpenDetails,
                                showAppendLoader = (lazy.loadState.append is LoadState.Loading)
                            )
                        }
                    }
                }
            }
        }
    }
}

private val LocalPagingItems =
    staticCompositionLocalOf<LazyPagingItems<Pokemon>?> { null }

@Composable
private fun PokemonGrid(
    count: Int,
    itemAt: (Int) -> Pokemon?,         // как у тебя было
    onOpenDetails: (Int) -> Unit,
    showAppendLoader: Boolean
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Ключи делаем просто по индексу. Это безопасно и не упадёт при смене запроса.
        items(
            count = count,
            key = { index -> "pokemon-$index" }
        ) { index ->
            // Безопасно получаем элемент: try/catch от IndexOutOfBounds
            val p = try {
                itemAt(index)
            } catch (_: IndexOutOfBoundsException) {
                null
            }

            if (p != null) {
                PokemonCard(p) { onOpenDetails(p.id) }
            } else {
                Card { Box(Modifier.height(180.dp)) }
            }
        }

        if (showAppendLoader) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) { CircularProgressIndicator() }
            }
        }
    }
}



@Composable
private fun PokemonCard(p: Pokemon, onClick: () -> Unit) {
    Card(onClick = onClick) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = p.imageUrl,
                contentDescription = p.name,
                modifier = Modifier.size(120.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                p.name.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(Modifier.fillMaxSize()) {
        Text(text, Modifier.align(Alignment.Center))
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(message)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onRetry) { Text("Повторить") }
        }
    }
}
