package com.example.amate

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.amate.core.designsystem.component.AmateBottomBar
import com.example.amate.core.navigation.AmateNavHost

@Composable
fun AmateApp() {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { AmateBottomBar() },
    ) { innerPadding ->
        AmateNavHost(modifier = Modifier.padding(innerPadding))
    }
}
