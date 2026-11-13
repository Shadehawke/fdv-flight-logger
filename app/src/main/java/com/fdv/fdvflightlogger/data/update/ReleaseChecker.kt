package com.fdv.fdvflightlogger.data.update

import com.fdv.fdvflightlogger.data.network.AppHttp
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.IOException

class UpdateException(message: String) : Exception(message)

/** A published release, as the app uses it. */
data class AppRelease(
    val version: String,        // "1.2.0", from the tag without its "v"
    val title: String,
    val notes: String,          // the release description (Markdown, shown as plain text)
    val pageUrl: String,
    val apkUrl: String?,        // null if the release has no APK attached
    val apkSize: Long?,
    val apkSha256: String?      // GitHub's checksum of the APK, used to verify the download in 6b
)

@Serializable
internal data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String? = null,
    val name: String? = null,
    val body: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GitHubAssetDto> = emptyList()
)

@Serializable
internal data class GitHubAssetDto(
    val name: String? = null,
    @SerialName("browser_download_url") val downloadUrl: String? = null,
    val size: Long? = null,
    val digest: String? = null      // "sha256:…"
)

object ReleaseChecker {

    const val REPO = "Shadehawke/fdv-flight-logger"
    private const val LATEST = "https://api.github.com/repos/$REPO/releases/latest"
    private const val RELEASES_PAGE = "https://github.com/$REPO/releases"

    @Throws(UpdateException::class)
    suspend fun latest(): AppRelease? {
        val (status, body) = try {
            val response = AppHttp.client.get(LATEST) {
                accept(ContentType.parse("application/vnd.github+json"))
                header("X-GitHub-Api-Version", "2022-11-28")
            }
            response.status.value to response.bodyAsText()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            throw UpdateException("Couldn't reach GitHub to check for updates.")
        }
        return interpret(status, body)
    }

    /** Pure, so it's unit tested with a real GitHub response. */
    internal fun interpret(status: Int, body: String): AppRelease? {
        // 404 from /releases/latest just means nothing is published yet
        if (status == 404) return null
        if (status == 403 || status == 429) {
            throw UpdateException("GitHub is limiting update checks right now. Try again in a while.")
        }
        if (status !in 200..299) throw UpdateException("Update check failed (HTTP $status).")

        val dto = try {
            AppHttp.json.decodeFromString(GitHubReleaseDto.serializer(), body)
        } catch (e: IllegalArgumentException) {
            throw UpdateException("GitHub sent a release the app couldn't read.")
        }

        // /latest already skips drafts and pre-releases; checked again so a future endpoint change can't leak one
        if (dto.draft || dto.prerelease) return null
        val tag = dto.tagName?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (AppVersion.parse(tag) == null) return null

        val apk = dto.assets.firstOrNull { asset ->
            asset.name?.endsWith(".apk", ignoreCase = true) == true && asset.downloadUrl != null
        }

        return AppRelease(
            version = tag.removePrefix("v").removePrefix("V"),
            title = dto.name?.takeIf { it.isNotBlank() } ?: tag,
            notes = dto.body?.trim().orEmpty(),
            pageUrl = dto.htmlUrl ?: RELEASES_PAGE,
            apkUrl = apk?.downloadUrl,
            apkSize = apk?.size,
            apkSha256 = apk?.digest?.removePrefix("sha256:")
        )
    }
}