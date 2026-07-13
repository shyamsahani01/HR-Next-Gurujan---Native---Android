package com.example.hrnext.ui.screens.doclist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hrnext.di.AppContainer
import com.example.hrnext.model.DocTypeMeta
import com.example.hrnext.model.Session
import com.example.hrnext.ui.AppViewModelFactory
import com.example.hrnext.ui.components.formatFieldValue
import com.example.hrnext.ui.theme.accentColorFor
import com.google.gson.JsonObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocListScreen(
    container: AppContainer,
    session: Session,
    doctype: String,
    onOpenRecord: (String) -> Unit,
    onCreateNew: () -> Unit,
    onBack: (() -> Unit)? = null,
    title: String = doctype,
    employeeFilter: String? = null,
    allowCreate: Boolean = true,
    headerContent: (@Composable () -> Unit)? = null,
    topBarActions: (@Composable () -> Unit)? = null,
) {
    val factory = remember(session.siteUrl, doctype, employeeFilter) {
        AppViewModelFactory(container, siteUrl = session.siteUrl, doctype = doctype, employeeFilter = employeeFilter)
    }
    val viewModel: DocListViewModel = viewModel(key = "doclist:${session.siteUrl}:$doctype:${employeeFilter.orEmpty()}", factory = factory)
    val state = viewModel.uiState
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible >= state.items.size - 4
        }
    }
    LaunchedEffect(shouldLoadMore, state.items.size) {
        if (shouldLoadMore) viewModel.loadNextPage()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = { topBarActions?.invoke() },
            )
        },
        floatingActionButton = {
            if (allowCreate) {
                ExtendedFloatingActionButton(
                    onClick = onCreateNew,
                    icon = { Icon(Icons.Filled.Add, null) },
                    text = { Text("New") },
                    shape = RoundedCornerShape(50),
                )
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            headerContent?.invoke()
            SearchField(value = state.searchText, onValueChange = viewModel::onSearchChange)

            PullToRefreshBox(
                isRefreshing = state.isLoadingPage && state.items.isEmpty(),
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    state.error != null && state.items.isEmpty() -> ErrorBox(state.error)
                    state.items.isEmpty() && !state.isLoadingPage && !state.isLoadingMeta -> EmptyBox()
                    else -> LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(state.items, key = { index, doc -> doc.get("name")?.asString ?: index }) { _, doc ->
                            RecordCard(
                                doctype = doctype,
                                doc = doc,
                                meta = state.meta,
                                onClick = { onOpenRecord(doc.get("name")?.asString.orEmpty()) },
                            )
                        }
                        if (state.isLoadingPage && state.items.isNotEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("Search") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(50),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun RecordCard(doctype: String, doc: JsonObject, meta: DocTypeMeta?, onClick: () -> Unit) {
    val name = doc.get("name")?.asString.orEmpty()
    val titleField = meta?.titleField
    val titleValue = titleField?.let { doc.get(it)?.takeIf { v -> v.isJsonPrimitive }?.asString }
    val title = titleValue ?: name

    val subtitleFields = (meta?.listViewFields.orEmpty())
        .filter { it.fieldname != "name" && it.fieldname != titleField }
        .take(3)

    val accent = accentColorFor(doctype)

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accent.container),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    title.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent.onContainer,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (titleValue != null && titleValue != name) {
                    Text(name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (subtitleFields.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Column {
                        subtitleFields.forEach { field ->
                            Row {
                                Text(
                                    "${field.label}: ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    formatFieldValue(doc, field),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorBox(message: String) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(32.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun EmptyBox() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.SearchOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "No records found",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Try adjusting your search or check back later.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
