@file:OptIn(ExperimentalMaterial3Api::class)

package com.physiocare.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.physiocare.app.data.AppRepository
import com.physiocare.app.data.Category
import com.physiocare.app.data.ExerciseLibrary
import com.physiocare.app.ui.components.ExerciseRow
import com.physiocare.app.ui.components.ScreenHeader

@Composable
fun LibraryScreen(repo: AppRepository, onCategory: (Category) -> Unit, onOpen: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = ExerciseLibrary.search(query)

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Text("Exercises", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "${ExerciseLibrary.all.size} exercises. Pick the area you want to work on.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                placeholder = { Text("Search, e.g. knee, balance, grip", style = MaterialTheme.typography.bodyLarge) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, contentDescription = "Clear search") }
                    }
                },
                shape = RoundedCornerShape(18.dp),
            )
        }
        if (query.isNotBlank()) {
            if (results.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "No exercises match “$query”. Try a body part such as “shoulder” or “ankle”.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            items(results, key = { it.id }, span = { GridItemSpan(maxLineSpan) }) { ex ->
                ExerciseRow(ex, repo.targetFor(ex), onClick = { onOpen(ex.id) })
            }
        } else {
            items(Category.values().toList(), key = { it.name }) { c ->
                Card(
                    onClick = { onCategory(c) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(c.emoji, style = MaterialTheme.typography.displayMedium)
                        Text(c.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${ExerciseLibrary.byCategory(c).size} exercises",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryScreen(category: Category, repo: AppRepository, onBack: () -> Unit, onOpen: (String) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader("${category.emoji}  ${category.title}", onBack, subtitle = category.blurb) }
        items(ExerciseLibrary.byCategory(category), key = { it.id }) { ex ->
            Column(Modifier.padding(horizontal = 16.dp)) {
                ExerciseRow(ex, repo.targetFor(ex), onClick = { onOpen(ex.id) })
            }
        }
    }
}
