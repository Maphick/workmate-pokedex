package com.workmate.pokedex.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.workmate.pokedex.presentation.vm.FiltersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersScreen(onBack: () -> Unit, vm: FiltersViewModel = hiltViewModel()) {
    val all by vm.allTypes.collectAsState()
    val selected by vm.selected.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Фильтры") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    TextButton(onClick = { vm.apply(); onBack() }) { Text("Применить") }
                }
            )
        }
    ) { padding ->
        if (all.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                Text("Типы ещё не загружены", modifier = Modifier.padding(16.dp))
            }
        } else {
            LazyColumn(Modifier.padding(padding)) {
                items(all) { t ->
                    val checked = t in selected
                    ListItem(
                        headlineContent = { Text(t.replaceFirstChar { it.uppercase() }) },
                        trailingContent = {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { vm.toggle(t) }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { vm.toggle(t) }
                    )
                    Divider()
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
