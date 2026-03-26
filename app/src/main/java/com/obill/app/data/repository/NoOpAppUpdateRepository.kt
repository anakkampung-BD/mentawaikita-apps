package com.obill.app.data.repository

/**
 * Stub sementara.
 *
 * Nanti setelah kamu kirim API update + format response,
 * implementasinya akan menggantikan class ini.
 */
class NoOpAppUpdateRepository : AppUpdateRepository {
    override suspend fun checkLatestRelease(currentVersion: String): Result<AppReleaseInfo?> =
        Result.success(null)
}

