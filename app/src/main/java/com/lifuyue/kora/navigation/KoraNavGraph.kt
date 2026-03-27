package com.lifuyue.kora.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lifuyue.kora.R

private const val ROUTE_ONBOARDING = "onboarding"

@Composable
fun KoraNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ROUTE_ONBOARDING,
    ) {
        composable(ROUTE_ONBOARDING) {
            OnboardingScreen(
                onContinue = {
                    // M1 只提供入口骨架，后续阶段再接入真实路由跳转。
                },
            )
        }
    }
}

@Composable
private fun OnboardingScreen(onContinue: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp),
        ) {
            Text(
                text = stringResource(id = R.string.onboarding_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(id = R.string.onboarding_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onContinue) {
                Text(text = stringResource(id = R.string.onboarding_continue))
            }
        }
    }
}
