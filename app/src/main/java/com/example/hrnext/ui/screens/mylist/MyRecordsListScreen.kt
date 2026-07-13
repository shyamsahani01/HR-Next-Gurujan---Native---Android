package com.example.hrnext.ui.screens.mylist

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hrnext.di.AppContainer
import com.example.hrnext.model.Session
import com.example.hrnext.ui.AppViewModelFactory
import com.example.hrnext.ui.screens.doclist.DocListScreen

/** Wraps [DocListScreen] for doctypes that should default to "just mine" (Expense Claim, Employee
 * Advance): a top-bar toggle lets the user switch to every record their account can read. */
@Composable
fun MyRecordsListScreen(
    container: AppContainer,
    session: Session,
    doctype: String,
    title: String = doctype,
    onOpenRecord: (String) -> Unit,
    onCreateNew: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val factory = remember(session.siteUrl) { AppViewModelFactory(container, siteUrl = session.siteUrl, session = session) }
    val viewModel: MyRecordsViewModel = viewModel(
        key = "mylist:${session.siteUrl}:${session.username}:$doctype",
        factory = factory,
    )
    val state = viewModel.uiState

    DocListScreen(
        container = container,
        session = session,
        doctype = doctype,
        title = title,
        onBack = onBack,
        onOpenRecord = onOpenRecord,
        onCreateNew = onCreateNew,
        employeeFilter = if (state.showMineOnly) state.employeeId else null,
        topBarActions = {
            IconButton(onClick = viewModel::toggleShowMineOnly) {
                AnimatedContent(
                    targetState = state.showMineOnly,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "mineOnlyToggle",
                ) { showMineOnly ->
                    Icon(
                        if (showMineOnly) Icons.Filled.Person else Icons.Filled.Groups,
                        contentDescription = if (showMineOnly) "Showing my records — tap to view everyone's" else "Showing everyone's records — tap to view mine",
                        tint = if (showMineOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}
