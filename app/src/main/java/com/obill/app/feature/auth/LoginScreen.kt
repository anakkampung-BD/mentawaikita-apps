package com.obill.app.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.obill.app.R
import com.obill.app.ui.components.ObillGradientButton
import com.obill.app.ui.components.TextScaleControls
import com.obill.app.ui.theme.PageBackground
import java.util.Calendar
import kotlinx.coroutines.delay

private enum class LoginAlertType { Success, Error }

@Composable
fun LoginScreen(
    state: LoginUiState,
    onLogin: (String, String) -> Unit,
    onLoginSuccess: () -> Unit,
    onAdjustText: (Float) -> Unit,
) {
    var alertType by remember { mutableStateOf<LoginAlertType?>(null) }

    LaunchedEffect(state.success) {
        if (state.success) alertType = LoginAlertType.Success
    }
    LaunchedEffect(state.error) {
        if (state.error != null) alertType = LoginAlertType.Error
    }

    LaunchedEffect(alertType) {
        if (alertType == LoginAlertType.Success) {
            delay(5000)
            alertType = null
            onLoginSuccess()
        }
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.splash_logo),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .heightIn(max = 260.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Silahkan masuk",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Filled.VisibilityOff
                            } else {
                                Icons.Filled.Visibility
                            },
                            contentDescription = if (passwordVisible) {
                                "Sembunyikan password"
                            } else {
                                "Tampilkan password"
                            },
                        )
                    }
                },
            )
            Spacer(Modifier.height(20.dp))
            if (state.loading) {
                CircularProgressIndicator()
            } else {
                ObillGradientButton(
                    text = "MASUK",
                    onClick = { onLogin(email, password) },
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = "OBill v.2 © ${Calendar.getInstance().get(Calendar.YEAR)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "PT. Anak Kampung Sejahtera",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
            )
        }
        TextScaleControls(
            onDecrease = { onAdjustText(-0.05f) },
            onIncrease = { onAdjustText(0.05f) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
        )
    }

    when (alertType) {
        LoginAlertType.Success -> {
            AlertDialog(
                onDismissRequest = { alertType = null },
                icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
                title = { Text("Login berhasil") },
                // Tidak ada text tambahan dan tombol konfirmasi sesuai permintaan.
                text = {},
                confirmButton = {},
            )
        }
        LoginAlertType.Error -> {
            AlertDialog(
                onDismissRequest = { alertType = null },
                icon = { Icon(Icons.Filled.Error, contentDescription = null) },
                title = { Text("Login gagal") },
                text = { Text("Periksa email dan password Anda, lalu coba lagi.", style = MaterialTheme.typography.bodyMedium) },
                confirmButton = {
                    TextButton(onClick = { alertType = null }) {
                        Text("OK")
                    }
                },
            )
        }
        null -> Unit
    }
}
