package com.lifuyue.kora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lifuyue.kora.navigation.KoraNavGraph
import com.lifuyue.kora.ui.theme.KoraTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KoraApp()
        }
    }
}

@Composable
private fun KoraApp() {
    KoraTheme {
        Surface(modifier = Modifier) {
            KoraNavGraph()
        }
    }
}
