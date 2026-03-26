package com.obill.app.feature.dashboard

import android.content.Intent
import android.content.ActivityNotFoundException
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.obill.app.data.remote.PenjualanItemDto
import com.obill.app.BuildConfig
import com.obill.app.ui.components.ObillCard
import com.obill.app.ui.components.ObillGradientButton
import com.obill.app.ui.formatRupiahPlain
import com.obill.app.ui.theme.BlueBrand
import com.obill.app.ui.theme.OrangeBrand
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

/** Hijau stabilo / highlighter untuk indikator sync sukses */
private val StabiloGreen = Color(0xFFCCFF00)

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onOpenSale: () -> Unit,
    onOpenQuota: () -> Unit,
    onOpenReport: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(state.receiptUri) {
        val uri = state.receiptUri ?: return@LaunchedEffect
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            ContextCompat.startActivity(context, Intent.createChooser(intent, "Buka bukti transaksi PDF"), null)
            viewModel.clearReceiptUri()
        } catch (_: ActivityNotFoundException) {
            viewModel.setReceiptError("Tidak ada aplikasi pembaca PDF di perangkat ini.")
            viewModel.clearReceiptUri()
        } catch (_: Exception) {
            viewModel.setReceiptError("File PDF tidak dapat ditampilkan.")
            viewModel.clearReceiptUri()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    var refreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = {
            refreshing = true
            viewModel.load()
        },
    )
    LaunchedEffect(state.loading) {
        if (!state.loading) refreshing = false
    }

    val lastCheckOneLine = remember(state.lastSyncAtMillis) {
        val millis = state.lastSyncAtMillis ?: return@remember "-"
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("id", "ID")).format(Date(millis))
    }

    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            nowMillis = System.currentTimeMillis()
        }
    }
    val nowText = remember(nowMillis) {
        val sdf = SimpleDateFormat("EEEE, dd MMM yyyy HH:mm:ss", Locale("id", "ID"))
        sdf.format(Date(nowMillis))
    }

    val profileText = state.sellerProfile?.name?.takeIf { it.isNotBlank() }
        ?: state.sellerProfile?.email?.takeIf { it.isNotBlank() }
        ?: "-"

    val saldoText = state.sellerProfile?.saldo?.let { formatRupiahPlain(it) } ?: "-"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(OrangeBrand, BlueBrand),
                            ),
                        )
                        .padding(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            UserAvatar(displayName = profileText)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    profileText,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    "Versi ${BuildConfig.VERSION_NAME}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (state.isUpToDate) {
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "Up to date",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White,
                                        )
                                    }
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "Saldo",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f),
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                saldoText,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    InfoTile(
                        title = "Waktu",
                        value = nowText,
                        accent = OrangeBrand,
                        modifier = Modifier.weight(1f),
                    )
                    SyncStatusCard(
                        isLoading = state.loading,
                        isOffline = state.error != null,
                        lastCheckText = lastCheckOneLine,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                Text("Quick Action", style = MaterialTheme.typography.labelLarge)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ObillGradientButton(
                        text = "Penjualan",
                        onClick = onOpenSale,
                        icon = Icons.Filled.AddShoppingCart,
                        modifier = Modifier.weight(1f),
                        fillWidth = false,
                    )
                    ObillGradientButton(
                        text = "Cek kuota",
                        onClick = onOpenQuota,
                        icon = Icons.Filled.ReceiptLong,
                        modifier = Modifier.weight(1f),
                        fillWidth = false,
                    )
                    ObillGradientButton(
                        text = "Laporan",
                        onClick = onOpenReport,
                        icon = Icons.Filled.Assessment,
                        modifier = Modifier.weight(1f),
                        fillWidth = false,
                    )
                }
            }

            when {
                state.loading && state.data == null -> {
                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                state.error != null && state.data == null -> {
                    item {
                        Text(
                            "Gagal memuat dashboard.",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                state.data != null -> {
                    val d = state.data!!
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            StatCard(
                                title = "Penjualan hari ini",
                                value = "${d.jmlPenjualanHariIni} trx",
                                accent = OrangeBrand,
                                modifier = Modifier.weight(1f),
                            )
                            StatCard(
                                title = "Komisi hari ini",
                                value = formatRupiahPlain(d.komisiHariIni),
                                accent = BlueBrand,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    item {
                        StatCard(
                            title = "Komisi bulan",
                            value = formatRupiahPlain(d.komisiBulan),
                            accent = OrangeBrand,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        Text(
                            "Transaksi terbaru",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                            ),
                        )
                    }
                    if (state.receiptLoading) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            }
                        }
                    }
                    state.receiptError?.let { err ->
                        item {
                            Text(
                                err,
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    items(d.penjualan) { p ->
                        TransactionRow(
                            p = p,
                            enabled = !state.receiptLoading,
                            onClick = {
                                val idInt = p.id.toIntOrNull()
                                if (idInt != null) {
                                    viewModel.downloadReceiptPdf(idInt)
                                }
                            },
                        )
                    }
                }
            }
        }

        PullRefreshIndicator(
            refreshing = refreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun SyncStatusCard(
    isLoading: Boolean,
    isOffline: Boolean,
    lastCheckText: String,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "syncBlink")
    val blink by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blinkAlpha",
    )
    val dotColor = when {
        isLoading -> BlueBrand
        isOffline -> Color(0xFFFF1744)
        else -> StabiloGreen
    }
    ObillCard(modifier = modifier) {
        Column {
            when {
                isLoading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Menyinkronkan…",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                                .alpha(blink),
                        )
                        Text(
                            if (isOffline) "Offline" else "Online",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Last check : $lastCheckText",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun UserAvatar(displayName: String) {
    val initial = displayName
        .trim()
        .firstOrNull { !it.isWhitespace() }
        ?.uppercaseChar()
        ?.toString()
        ?: "?"
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.28f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initial,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
        )
    }
}

@Composable
private fun InfoTile(
    title: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    showProgress: Boolean = false,
    subtitle: String? = null,
) {
    ObillCard(modifier = modifier) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(accent, RoundedCornerShape(99.dp)),
                )
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))
            if (showProgress) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(value, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Text(
                    value,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 3,
                )
            }
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    ObillCard(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(accent),
        )
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun TransactionRow(
    p: PenjualanItemDto,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val price = p.sellingPrice?.toDoubleOrNull() ?: 0.0
    val priceLabel = formatRupiahPlain(price).replace("Rp ", "Rp. ")
    ObillCard(
        modifier = Modifier.clickable(
            enabled = enabled,
            onClick = onClick,
        ),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "No. ${p.id}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    p.buyer?.trim().orEmpty().ifBlank { "-" },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    textAlign = TextAlign.End,
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    p.tanggal.orEmpty().ifBlank { "-" },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    maxLines = 2,
                )
                Text(
                    priceLabel,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                Text(
                    p.kodeVoucher.orEmpty().ifBlank { "-" },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
