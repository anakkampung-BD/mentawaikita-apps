package com.obill.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.obill.app.AppContainer
import com.obill.app.navigation.ObillNavHost
import com.obill.app.ui.theme.ObillTheme

@Composable
fun ObillApp(container: AppContainer) {
    var textScale by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(Unit) {
        container.tokenStore.textScale.collect { textScale = it }
    }
    ObillTheme(textScale = textScale) {
        ObillNavHost(container = container)
    }
}
