package com.workmate.pokedex.presentation.ui

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

@Composable
private fun ActiveFiltersChip(types: List<String>, onClear: () -> Unit) {
    if (types.isNotEmpty()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Фильтры: ", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.width(4.dp))
            types.forEach { type ->
                FilterChip(
                    selected = true,
                    onClick = { /* Можно сделать удаление отдельного фильтра */ },
                    label = { Text(type.replaceFirstChar { it.uppercase() }) },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Удалить фильтр",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            Spacer(Modifier.weight(1f))
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
    onOpenFilters: () -> Unit,           // колбэк для перехода на экран фильтров
    onOpenDetails: (Int) -> Unit,        // колбэк для перехода на деталку с id
    vm: PokemonListViewModel = hiltViewModel() // получаем VM через Hilt
) {
    // Подписываемся на Flow<PagingData<Pokemon>> из VM и преобразуем в LazyPagingItems,
    // который «умеет» работать с Lazy списками/сетками Compose.
    val lazy: LazyPagingItems<Pokemon> = vm.paging.collectAsLazyPagingItems()

    // Хост для Snackbar’ов (покажем ошибки подгрузки и кнопку Retry).
    val snackbarHost = remember { SnackbarHostState() }

    // Состояние текста поиска; rememberSaveable — переживает повороты/процесса recreation.
    var search by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }

    // Признак «тянем сверху», берём из VM (там он меняется во время refresh’а).
    val isRefreshing by vm.isRefreshing.collectAsState()

    // Ловим ошибки загрузки из состояний Paging:
    //  - refresh — первичная загрузка/перезагрузка
    //  - append  — подгрузка следующей страницы
    val appendError = lazy.loadState.append as? LoadState.Error
    val refreshError = lazy.loadState.refresh as? LoadState.Error

    // Если появилась ошибка — показываем Snackbar с кнопкой «Повторить».
    LaunchedEffect(appendError, refreshError) {
        val err = appendError?.error ?: refreshError?.error
        if (err != null) {
            val res = snackbarHost.showSnackbar(
                message = err.localizedMessage ?: "Ошибка загрузки",
                actionLabel = "Повторить"
            )
            if (res == SnackbarResult.ActionPerformed) lazy.retry() // перезапрос текущей операции Paging
        }
    }


    // чтобы получить выбранные типы
    val selectedTypes by vm.selectedTypes.collectAsState()

    Scaffold(
        topBar = {
            // Верхняя панель с заголовком и кнопкой перехода к фильтрам
            TopAppBar(
                title = { Text("Покемоны") },
                actions = {
                    IconButton(onClick = onOpenFilters) {
                        Icon(Icons.Filled.Tune, contentDescription = "Фильтры")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) } // куда рендерить Snackbar’ы
    ) { padding ->
        // Контейнер «потяни-обнови». Показывает системный индиатор и вызывает vm.onRefresh().
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { vm.onRefresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding) // внутренние отступы от Scaffold
        ) {
            Column(Modifier.fillMaxSize()) {
                // Поисковое поле: каждое изменение сразу отправляем в VM (дебаунс можно добавить в VM).
                OutlinedTextField(
                    value = search,
                    onValueChange = {
                        search = it
                        vm.onSearch(it.text) // триггерит recompute Paging по query
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    placeholder = { Text("Поиск по имени…") }
                )

                // блок поиска по фильтрам
                ActiveFiltersChip(
                    types = selectedTypes,
                    onClear = { vm.clearFilters() }
                )


                // Реакция на состояние первичной загрузки Paging (refresh)
                when (val s = lazy.loadState.refresh) {
                    is LoadState.Loading -> {
                        // Если ещё нет элементов — показываем большой индикатор по центру;
                        // если уже что-то есть (скроллим) — рисуем сетку и снизу лоадер append.
                        if (lazy.itemCount == 0) {
                            Box(Modifier.fillMaxSize()) {
                                CircularProgressIndicator(Modifier.align(Alignment.Center))
                            }
                        } else {
                            PokemonGrid(
                                count = lazy.itemCount,
                                itemAt = { idx -> lazy[idx] },  // получение элемента по индексу (может быть null на плейсхолдерах)
                                onOpenDetails = onOpenDetails,
                                showAppendLoader = lazy.loadState.append is LoadState.Loading
                            )
                        }
                    }
                    is LoadState.Error -> {
                        // Ошибка первой загрузки: если элементов нет — показываем экран ошибки с Retry;
                        // если что-то отрисовано — оставляем сетку + лоадер/ошибка на append обрабатываем Snackbar’ом.
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
                                showAppendLoader = lazy.loadState.append is LoadState.Loading
                            )
                        }
                    }
                    is LoadState.NotLoading -> {
                        // Данные успешно подгружены: либо пусто (ничего не найдено по поиску/фильтрам),
                        // либо рисуем сетку и при необходимости нижний лоадер append.
                        if (lazy.itemCount == 0) {
                            EmptyState("Ничего не найдено")
                        } else {
                            PokemonGrid(
                                count = lazy.itemCount,
                                itemAt = { idx -> lazy[idx] },
                                onOpenDetails = onOpenDetails,
                                showAppendLoader = lazy.loadState.append is LoadState.Loading
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PokemonGrid(
    count: Int,                        // сколько элементов всего сейчас в списке
    itemAt: (Int) -> Pokemon?,         // как получить элемент по индексу (Paging может вернуть null-плейсхолдер)
    onOpenDetails: (Int) -> Unit,      // обработчик клика по карточке (переход на деталку)
    showAppendLoader: Boolean          // показывать ли нижний индикатор подгрузки следующей страницы
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),                           // ровно 2 столбца
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Основные элементы сетки (карточки покемонов).
        items(count) { index ->
            val p = itemAt(index)
            if (p != null) {
                PokemonCard(p) { onOpenDetails(p.id) }
            } else {
                // Плейсхолдер на время подгрузки элемента (можно заменить на shimmer)
                Card { Box(Modifier.height(180.dp)) }
            }
        }

        // Нижний «хвост» с индикатором, когда идёт append (подгрузка следующих страниц)
        if (showAppendLoader) {
            item(span = { GridItemSpan(maxLineSpan) }) { // на всю ширину сетки
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun PokemonCard(p: Pokemon, onClick: () -> Unit) {
    // Простая карточка с картинкой и именем; клик ведёт на деталку
    Card(onClick = onClick) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Картинка грузится Coil’ом по URL (официальный арт из репозитория PokeAPI)
            AsyncImage(
                model = p.imageUrl,
                contentDescription = p.name,
                modifier = Modifier.size(120.dp)
            )
            Spacer(Modifier.height(8.dp))
            // Имя с заглавной буквы, стиль — заголовок карточки
            Text(
                p.name.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    // Пустое состояние (ничего не найдено по текущим условиям)
    Box(Modifier.fillMaxSize()) {
        Text(text, Modifier.align(Alignment.Center))
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    // Экран ошибки с кнопкой «Повторить»
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
