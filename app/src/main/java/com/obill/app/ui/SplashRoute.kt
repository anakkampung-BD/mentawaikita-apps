package com.obill.app.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.obill.app.AppContainer
import com.obill.app.BuildConfig
import com.obill.app.R
import com.obill.app.data.repository.AppReleaseInfo
import com.obill.app.ui.components.TextScaleControls
import com.obill.app.ui.theme.SplashBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Satu layar: logo, tagline, progress sinkron API.
 * [pingServer] wajib sukses sebelum lanjut — jika gagal (mis. offline), progress tidak dipaksakan 100%.
 */
@Composable
fun SplashRoute(
    container: AppContainer,
    onDone: (hasToken: Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var progress by remember { mutableFloatStateOf(0f) }
    var syncError by remember { mutableStateOf<String?>(null) }
    var retryNonce by remember { mutableIntStateOf(0) }
    var forceUpdateInfo by remember { mutableStateOf<AppReleaseInfo?>(null) }

    fun openUpdate(url: String?) {
        val packageName = context.packageName
        val marketUrl = url ?: "market://details?id=$packageName"
        val playUrl = "https://play.google.com/store/apps/details?id=$packageName"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(marketUrl))
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(playUrl)))
        }
    }

    LaunchedEffect(retryNonce) {
        syncError = null
        forceUpdateInfo = null
        try {
            progress = 0.05f
            container.authRepository.hydrateTokenHolder()
            progress = 0.18f

            val pingResult = container.pingServer()
            if (pingResult.isFailure) {
                progress = 0.22f
                syncError =
                    "Tidak dapat terhubung ke server. Periksa koneksi internet lalu coba lagi."
                return@LaunchedEffect
            }
            progress = 0.42f

            // Cek update wajib sebelum masuk ke login/dashboard.
            val currentVersion = BuildConfig.VERSION_NAME
            val release = container.appUpdateRepository
                .checkLatestRelease(currentVersion)
                .getOrNull()
            if (release?.forceUpdate == true) {
                forceUpdateInfo = release
                progress = 1f
                openUpdate(release.updateUrl)
                return@LaunchedEffect
            }

            val hadToken = container.tokenStore.token.first() != null
            if (hadToken) {
                progress = 0.55f
                val profileOk = container.sellerRepository.profile().isSuccess
                if (!profileOk) {
                    container.authRepository.clearSessionLocalOnly()
                }
                progress = 0.92f
            } else {
                progress = 0.88f
            }

            progress = 1f
            delay(350)

            val stillLoggedIn = container.tokenStore.token.first() != null
            onDone(stillLoggedIn)
        } catch (e: Exception) {
            progress = 0.22f
            syncError = e.message?.takeIf { it.isNotBlank() }
                ?: "Terjadi kesalahan saat sinkronisasi."
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SplashBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .padding(bottom = 24.dp),
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
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Let's route the world!",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(8.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Versi ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
            )
            syncError?.let { err ->
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { retryNonce++ }) {
                    Text("Coba lagi")
                }
            }
        }

        TextScaleControls(
            onDecrease = {
                scope.launch {
                    val cur = container.tokenStore.textScale.first()
                    container.tokenStore.setTextScale(cur - 0.05f)
                }
            },
            onIncrease = {
                scope.launch {
                    val cur = container.tokenStore.textScale.first()
                    container.tokenStore.setTextScale(cur + 0.05f)
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
        )

        forceUpdateInfo?.let { info ->
            AlertDialog(
                onDismissRequest = { /* wajib update */ },
                title = { Text("Update Diperlukan") },
                text = {
                    val scroll = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .heightIn(max = 360.dp)
                            .verticalScroll(scroll),
                    ) {
                        Text(
                            "Versi terbaru: ${info.latestVersion}. Update wajib untuk melanjutkan.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (!info.releaseNotes.isNullOrBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = info.releaseNotes.trim(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { openUpdate(info.updateUrl) }) {
                        Text("Update Sekarang")
                    }
                },
            )
        }
    }
}
