package com.obill.app.data.repository

data class AppReleaseInfo(
    val latestVersion: String,
    val forceUpdate: Boolean = true,
    /**
     * Link untuk update. Jika null, aplikasi akan fallback ke `market://details?id=<packageName>`
     * lalu fallback ke `https://play.google.com/...`.
     */
    val updateUrl: String? = null,
)

interface AppUpdateRepository {
    /**
     * Cek apakah perlu update wajib berdasarkan versi aplikasi saat ini.
     * Jika tidak ada update, return `Result.success(null)`.
     */
    suspend fun checkLatestRelease(currentVersion: String): Result<AppReleaseInfo?>
}

