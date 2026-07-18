package app.szuflada.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.szuflada.data.db.ItemEntity
import java.time.LocalDate

@Composable
fun SzufladaRoot(viewModel: HomeViewModel = hiltViewModel()) {
    var showAdd by rememberSaveable { mutableStateOf(false) }
    if (showAdd) {
        AddItemScreen(
            viewModel = viewModel,
            onClose = { showAdd = false },
        )
    } else {
        HomeScreen(
            viewModel = viewModel,
            onAddClick = { showAdd = true },
        )
    }
}

@Composable
private fun HomeScreen(viewModel: HomeViewModel, onAddClick: () -> Unit) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri -> uri?.let(viewModel::exportTo) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Filled.Add, contentDescription = "Dodaj dokument")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Szuflada",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(vertical = 12.dp).weight(1f),
                )
                IconButton(
                    onClick = {
                        exportLauncher.launch("szuflada-export-${LocalDate.now()}.zip")
                    },
                ) {
                    Icon(Icons.Filled.Share, contentDescription = "Eksport wszystkich danych (ZIP)")
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Szukaj (tytuł, sklep, tekst paragonu)") },
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            if (items.isEmpty()) {
                Text(
                    if (query.isBlank()) {
                        "Pusta szuflada. Dodaj pierwszy paragon przyciskiem +."
                    } else {
                        "Brak wyników dla „$query”."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 24.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items, key = { it.itemId }) { item ->
                        ItemRow(item)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemRow(item: ItemEntity) {
    val details = buildList {
        item.merchant?.let { add(it) }
        item.amountGrosze?.let { add("%d,%02d %s".format(it / 100, it % 100, item.currency ?: "zł")) }
        item.purchaseDate?.let { add(LocalDate.ofEpochDay(it).toString()) }
    }.joinToString(" · ")

    ListItem(
        headlineContent = { Text(item.title) },
        supportingContent = { if (details.isNotEmpty()) Text(details) },
    )
}

private val AddItemFormSaver = listSaver<AddItemForm, String>(
    save = { listOf(it.title, it.merchant, it.amount, it.purchaseDate, it.warrantyUntil, it.ocrText) },
    restore = { AddItemForm(it[0], it[1], it[2], it[3], it[4], it[5]) },
)

@Composable
private fun AddItemScreen(viewModel: HomeViewModel, onClose: () -> Unit) {
    var form by rememberSaveable(stateSaver = AddItemFormSaver) { mutableStateOf(AddItemForm()) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Nowy paragon", style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                value = form.ocrText,
                onValueChange = { form = form.copy(ocrText = it) },
                modifier = Modifier.fillMaxWidth().height(140.dp),
                label = { Text("Tekst paragonu (wklej — docelowo: skan aparatem)") },
            )
            OutlinedButton(
                onClick = { form = viewModel.fillFromOcr(form) },
                enabled = form.ocrText.isNotBlank(),
            ) {
                Text("Uzupełnij pola z tekstu")
            }

            OutlinedTextField(
                value = form.title,
                onValueChange = { form = form.copy(title = it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Tytuł *") },
                singleLine = true,
            )
            OutlinedTextField(
                value = form.merchant,
                onValueChange = { form = form.copy(merchant = it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Sklep") },
                singleLine = true,
            )
            OutlinedTextField(
                value = form.amount,
                onValueChange = { form = form.copy(amount = it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Kwota (np. 49,99)") },
                singleLine = true,
            )
            OutlinedTextField(
                value = form.purchaseDate,
                onValueChange = { form = form.copy(purchaseDate = it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Data zakupu (RRRR-MM-DD lub D.M.RRRR)") },
                singleLine = true,
            )
            OutlinedTextField(
                value = form.warrantyUntil,
                onValueChange = { form = form.copy(warrantyUntil = it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Gwarancja do (RRRR-MM-DD)") },
                singleLine = true,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onClose) { Text("Anuluj") }
                Button(
                    onClick = { viewModel.saveItem(form) { onClose() } },
                    enabled = form.title.isNotBlank(),
                ) {
                    Text("Zapisz")
                }
            }
        }
    }
}
