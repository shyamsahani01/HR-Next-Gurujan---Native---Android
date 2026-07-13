package com.example.hrnext.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.hrnext.di.AppContainer
import com.example.hrnext.ui.AppViewModelFactory
import com.example.hrnext.ui.screens.docdetail.DocDetailScreen
import com.example.hrnext.ui.screens.doclist.DocListScreen
import com.example.hrnext.ui.screens.login.LoginScreen
import com.example.hrnext.ui.screens.login.LoginViewModel
import com.example.hrnext.ui.screens.main.MainScreen
import com.example.hrnext.ui.screens.mylist.MyRecordsListScreen
import com.example.hrnext.ui.screens.splash.SplashScreen
import kotlinx.coroutines.flow.first
import java.net.URLDecoder

/** Doctypes whose desk-wide list would otherwise show every employee's records — default them to
 * "just mine" with a toggle rather than the plain, unfiltered [DocListScreen]. */
private val SELF_SCOPED_DOCTYPES = setOf("Expense Claim", "Employee Advance")

@Composable
fun HRNextNavGraph(container: AppContainer) {
    val navController = rememberNavController()
    val factory = remember { AppViewModelFactory(container) }
    val session by container.sessionManager.sessionFlow.collectAsState(initial = null)

    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen()
            LaunchedEffect(Unit) {
                val initialSession = container.sessionManager.sessionFlow.first()
                val destination = if (initialSession != null) Routes.MAIN else Routes.LOGIN
                navController.navigate(destination) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            }
        }

        composable(Routes.LOGIN) {
            val loginViewModel: LoginViewModel = viewModel(factory = factory)
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.MAIN) {
            session?.let { activeSession ->
                MainScreen(
                    container = container,
                    session = activeSession,
                    onOpenDocType = { doctype -> navController.navigate(Routes.docList(doctype)) },
                    onOpenRecord = { doctype, name -> navController.navigate(Routes.docDetail(doctype, name)) },
                    onCreateRecord = { doctype -> navController.navigate(Routes.docCreate(doctype)) },
                    onLoggedOut = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }
        }

        composable(
            route = Routes.DOC_LIST,
            arguments = listOf(navArgument("doctype") { type = NavType.StringType }),
        ) { backStackEntry ->
            val doctype = backStackEntry.arguments?.getString("doctype").orEmpty().urlDecoded()
            session?.let { activeSession ->
                if (doctype in SELF_SCOPED_DOCTYPES) {
                    MyRecordsListScreen(
                        container = container,
                        session = activeSession,
                        doctype = doctype,
                        onOpenRecord = { name -> navController.navigate(Routes.docDetail(doctype, name)) },
                        onCreateNew = { navController.navigate(Routes.docCreate(doctype)) },
                        onBack = { navController.popBackStack() },
                    )
                } else {
                    DocListScreen(
                        container = container,
                        session = activeSession,
                        doctype = doctype,
                        onOpenRecord = { name -> navController.navigate(Routes.docDetail(doctype, name)) },
                        onCreateNew = { navController.navigate(Routes.docCreate(doctype)) },
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }

        composable(
            route = Routes.DOC_DETAIL,
            arguments = listOf(
                navArgument("doctype") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val doctype = backStackEntry.arguments?.getString("doctype").orEmpty().urlDecoded()
            val name = backStackEntry.arguments?.getString("name").orEmpty().urlDecoded()
            session?.let { activeSession ->
                DocDetailScreen(
                    container = container,
                    session = activeSession,
                    doctype = doctype,
                    name = name,
                    onBack = { navController.popBackStack() },
                    onRecordCreated = { newName ->
                        navController.navigate(Routes.docDetail(doctype, newName)) {
                            popUpTo(Routes.docDetail(doctype, name)) { inclusive = true }
                        }
                    },
                )
            }
        }
    }
}

private fun String.urlDecoded(): String = URLDecoder.decode(this, "UTF-8")
