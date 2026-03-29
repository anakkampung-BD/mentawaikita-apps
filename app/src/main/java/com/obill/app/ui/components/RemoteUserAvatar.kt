package com.obill.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Avatar dari [DiceBear](https://www.dicebear.com/) — lisensi CC0 / bebas dipakai (lihat situs).
 * [seed] menentukan bentuk avatar (tetap untuk pengguna yang sama bila seed sama).
 */
fun diceBearAvatarUrl(seed: String, sizePx: Int = 128): String {
    val s = seed.trim().ifBlank { "obill-user" }
    val encoded = URLEncoder.encode(s, StandardCharsets.UTF_8.name())
    return "https://api.dicebear.com/7.x/avataaars/png?seed=$encoded&size=$sizePx"
}

/**
 * Avatar lingkaran memuat gambar dari internet; jika gagal, menampilkan huruf awal dari [fallbackInitialSource].
 *
 * @param isOnDarkBackground jika true (mis. kartu gradient dashboard), placeholder/error memakai overlay terang + teks putih.
 */
@Composable
fun RemoteUserAvatar(
    seed: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    contentDescription: String? = "Avatar pengguna",
    fallbackInitialSource: String = seed,
    isOnDarkBackground: Boolean = false,
) {
    val context = LocalContext.current
    val url = remember(seed) { diceBearAvatarUrl(seed) }
    val initial = fallbackInitialSource
        .trim()
        .firstOrNull { !it.isWhitespace() }
        ?.uppercaseChar()
        ?.toString()
        ?: "?"

    val placeholderBg = if (isOnDarkBackground) {
        Color.White.copy(alpha = 0.28f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val placeholderFg = if (isOnDarkBackground) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentScale = ContentScale.Crop,
        loading = {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(placeholderBg),
            )
        },
        error = {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(placeholderBg),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleMedium,
                    color = placeholderFg,
                )
            }
        },
    )
}
