package com.obill.app.data.repository

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Checker update versi aplikasi dari endpoint remote.
 *
 * Format response yang didukung:
 * 1) JSON:
 *    { "latest_version":"2.0.2", "force_update":true, "update_url":"https://..." }
 *    atau key alternatif: version / latestVersion / forceUpdate / url
 * 2) Plain text:
 *    2.0.2
 */
class RemoteAppUpdateRepository(
    private val updateCheckUrl: String,
    private val updateCheckToken: String,
    private val fetchText: suspend (String, Map<String, String>) -> Result<String>,
) : AppUpdateRepository {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun checkLatestRelease(currentVersion: String): Result<AppReleaseInfo?> {
        if (updateCheckUrl.isBlank()) return Result.success(null)

        val headers = buildMap {
            if (updateCheckToken.isNotBlank()) {
                put("PRIVATE-TOKEN", updateCheckToken)
            }
        }
        return fetchText(updateCheckUrl, headers).mapCatching { body ->
            if (body.isBlank()) return@mapCatching null

            val parsed = parseAsJson(body) ?: parseAsPlainText(body)
            val latestRaw = parsed?.latestVersion ?: return@mapCatching null
            val latest = latestRaw.trim()
            val current = currentVersion.trim()

            // Jika versi sama persis dengan versi di repository, kembalikan info untuk UI.
            if (latest == current) {
                return@mapCatching AppReleaseInfo(
                    latestVersion = latest,
                    forceUpdate = false,
                    updateUrl = parsed.updateUrl ?: fallbackRepoApkUrl(),
                )
            }

            if (!isNewerVersion(latest, current)) {
                null
            } else {
                // Jika field force_update tidak ada, default wajib update.
                AppReleaseInfo(
                    latestVersion = latest,
                    forceUpdate = parsed.forceUpdate,
                    updateUrl = parsed.updateUrl ?: fallbackRepoApkUrl(),
                )
            }
        }
    }

    private data class Parsed(
        val latestVersion: String,
        val forceUpdate: Boolean,
        val updateUrl: String?,
    )

    private fun parseAsJson(body: String): Parsed? = runCatching {
        val root = json.parseToJsonElement(body).jsonObject
        val latest = root.string("latest_version")
            ?: root.string("latestVersion")
            ?: root.string("version")
            ?: return null

        val force = root.bool("force_update")
            ?: root.bool("forceUpdate")
            ?: true

        val url = root.string("update_url")
            ?: root.string("updateUrl")
            ?: root.string("url")

        Parsed(latestVersion = latest, forceUpdate = force, updateUrl = url)
    }.getOrNull()

    private fun parseAsPlainText(body: String): Parsed? {
        val latest = body.lineSequence().firstOrNull()?.trim().orEmpty()
        if (latest.isBlank()) return null
        return Parsed(latestVersion = latest, forceUpdate = true, updateUrl = null)
    }

    private fun JsonObject.string(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }

    private fun JsonObject.bool(key: String): Boolean? =
        this[key]?.jsonPrimitive?.booleanOrNull

    /**
     * Fallback URL download APK dari repository GitLab jika response update tidak mengirim update_url.
     * Contoh transform:
     * .../updates%2Flatest-version.txt/raw?ref=main -> .../updates%2Fapp-release.apk/raw?ref=main
     */
    private fun fallbackRepoApkUrl(): String? {
        val source = updateCheckUrl.trim()
        if (source.isBlank()) return null

        val replaced = source.replace("updates%2Flatest-version.txt", "updates%2Fapp-release.apk")
        if (replaced == source) return null

        return appendPrivateTokenQuery(replaced, updateCheckToken)
    }

    private fun appendPrivateTokenQuery(url: String, token: String): String {
        if (token.isBlank()) return url
        if (url.contains("private_token=")) return url
        val separator = if (url.contains("?")) "&" else "?"
        return "$url${separator}private_token=$token"
    }
}

