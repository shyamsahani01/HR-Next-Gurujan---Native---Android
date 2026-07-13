package com.example.hrnext

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.hrnext.ui.navigation.HRNextNavGraph
import com.example.hrnext.ui.theme.HRNextTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as HRNextApp).container
        setContent {
            val themeMode by container.sessionManager.themeModeFlow.collectAsState(initial = "system")
            val darkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            HRNextTheme(darkTheme = darkTheme) {
                HRNextNavGraph(container = container)
            }
        }
    }
}
