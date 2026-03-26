package com.obill.app.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.obill.app.ui.components.ObillCard
import com.obill.app.ui.formatRupiahPlain

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLogout: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text("Profil seller", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        when {
            state.loading -> {
                androidx.compose.foundation.layout.Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> Text(state.error!!, color = MaterialTheme.colorScheme.error)
            state.data != null -> {
                val d = state.data!!
                ObillCard {
                    Text(d.name.orEmpty(), style = MaterialTheme.typography.labelLarge)
                    Text(d.email.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                    Text("Device: ${d.deviceId.orEmpty()}", style = MaterialTheme.typography.bodySmall)
                    d.hp?.let { Text("HP: $it", style = MaterialTheme.typography.bodySmall) }
                    d.saldo?.let {
                        Text(
                            "Saldo: ${formatRupiahPlain(it)}",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.logout(onLogout) },
                    enabled = !state.logoutLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (state.logoutLoading) "Memproses…" else "Keluar")
                }
            }
        }
    }
}
