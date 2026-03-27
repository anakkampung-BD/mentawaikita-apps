package com.obill.app.data.remote

import java.io.File
import java.io.FileInputStream

/** True jika isi file mengandung signature PDF standar (toleran BOM/spasi di depan). */
fun File.isPdfContent(): Boolean {
    if (!exists() || length() < 5L) return false
    return runCatching {
        FileInputStream(this).use { input ->
            val header = ByteArray(4096)
            val read = input.read(header)
            if (read <= 0) return@use false
            val headText = String(header, 0, read, Charsets.ISO_8859_1)
            headText.contains("%PDF-")
        }
    }.getOrDefault(false)
}

/**
 * Jika server mengembalikan JSON error (bukan binary PDF), coba ambil pesan untuk user.
 */
fun File.readJsonErrorMessageIfPresent(): String? {
    if (!exists() || length() == 0L) return null
    return runCatching {
        val text = FileInputStream(this).use { input ->
            val buf = ByteArray(8192)
            val n = input.read(buf)
            if (n <= 0) return@runCatching null
            String(buf, 0, n, Charsets.UTF_8)
        }.trimStart()
        if (!text.startsWith("{")) return@runCatching null
        Regex("\"message\"\\s*:\\s*\"([^\"]*)\"").find(text)?.groupValues?.getOrNull(1)
            ?: Regex("\"message\"\\s*:\\s*'([^']*)'").find(text)?.groupValues?.getOrNull(1)
    }.getOrNull()
}
