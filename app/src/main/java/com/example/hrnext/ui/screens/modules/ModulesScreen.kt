package com.example.hrnext.ui.screens.modules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hrnext.di.AppContainer
import com.example.hrnext.model.ModuleItem
import com.example.hrnext.model.ModuleSection
import com.example.hrnext.model.Session
import com.example.hrnext.ui.AppViewModelFactory
import com.example.hrnext.ui.components.iconForDoctype
import com.example.hrnext.ui.theme.accentColorFor

/** The generic "browse every HR/Payroll doctype" grid — everything Home doesn't have a dedicated,
 * personalized surface for lives here instead. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModulesScreen(
    container: AppContainer,
    session: Session,
    onOpenDocType: (String) -> Unit,
) {
    val factory = remember(session.siteUrl) { AppViewModelFactory(container, siteUrl = session.siteUrl) }
    val viewModel: ModulesViewModel = viewModel(key = "modules:${session.siteUrl}", factory = factory)
    val state = viewModel.uiState

    Scaffold(
        topBar = { TopAppBar(title = { Text("More", fontWeight = FontWeight.SemiBold) }) },
    ) { padding ->
        Surface(modifier = Modifier.fillMaxSize().padding(padding)) {
            PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = viewModel::load,
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    state.error != null && state.sections.isEmpty() -> ModulesErrorState(state.error, viewModel::load)
                    state.isLoading && state.sections.isEmpty() -> ModulesLoadingState()
                    state.sections.isEmpty() -> ModulesEmptyState()
                    else -> ModulesContent(sections = state.sections, onOpenDocType = onOpenDocType)
                }
            }
        }
    }
}

@Composable
private fun ModulesContent(sections: List<ModuleSection>, onOpenDocType: (String) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        // Keyed by index rather than title: Frappe workspaces can legitimately contribute two
        // card-break sections with the same label (e.g. two "Reports" groups), and a duplicate
        // key crashes LazyColumn the moment that item scrolls into view.
        itemsIndexed(sections, key = { index, section -> "$index:${section.title}" }) { _, section ->
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(width = 5.dp, height = 18.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(accentColorFor(section.title).solid),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        section.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    section.items.forEach { item ->
                        ModuleCard(item = item, onClick = { onOpenDocType(item.doctype) })
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun ModuleCard(item: ModuleItem, onClick: () -> Unit) {
    val accent = accentColorFor(item.doctype)
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.width(152.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(accent.container),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    iconForDoctype(item.doctype),
                    contentDescription = null,
                    tint = accent.onContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                item.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ModulesLoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ModulesEmptyState() {
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
                    Icons.Filled.FolderOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "No HR modules found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Check that your account has access to HR/Payroll doctypes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ModulesErrorState(message: String, onRetry: () -> Unit) {
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
                    Icons.Filled.WifiOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Couldn't load your HR modules",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Button(onClick = onRetry, shape = RoundedCornerShape(50)) { Text("Retry") }
        }
    }
}
