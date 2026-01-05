package org.nv.vinylmanager.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.nv.vinylmanager.domain.model.Album
import org.nv.vinylmanager.domain.model.Track

class MusicBrainzApi(
    private val client: HttpClient = defaultClient()
) {
    companion object {
        private const val BASE_URL = "https://musicbrainz.org/ws/2"
    }

    suspend fun search(query: String): List<Album> {
        val text = client.get("$BASE_URL/release") {
            parameter("query", query)
            parameter("fmt", "json")
        }.bodyAsText()
        val json = Json.parseToJsonElement(text).jsonObject
        val releases = json["releases"]?.jsonArray.orEmpty()
        return releases.mapNotNull { element ->
            val obj = element.jsonObject
            val id = obj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val title = obj["title"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val dateString = obj["date"]?.jsonPrimitive?.content
            val artist = obj["artist-credit"]?.jsonArray?.firstOrNull()?.jsonObject?.get("name")?.jsonPrimitive?.content ?: ""
            Album(
                id = id,
                title = title,
                artist = artist,
                year = dateString?.let { LocalDate.parse(it).year },
                releaseDate = dateString?.let(LocalDate::parse),
                coverUrl = null,
                addedAt = kotlinx.datetime.Clock.System.now(),
                lastListenedAt = null,
                tracks = emptyList()
            )
        }
    }

    suspend fun fetchRelease(id: String): Album? {
        val text = client.get("$BASE_URL/release/$id") {
            parameter("fmt", "json")
            parameter("inc", "recordings+artist-credits")
        }.bodyAsText()
        val json = Json.parseToJsonElement(text).jsonObject
        val release = json["release"]?.jsonObject ?: return null
        val title = release["title"]?.jsonPrimitive?.content ?: return null
        val dateString = release["date"]?.jsonPrimitive?.content
        val artist = release["artist-credit"]?.jsonArray?.firstOrNull()?.jsonObject?.get("name")?.jsonPrimitive?.content ?: ""
        val tracks = json["media"]?.jsonArray.orEmpty().flatMap { medium ->
            medium.jsonObject["tracks"]?.jsonArray.orEmpty().mapIndexed { index, track ->
                val rec = track.jsonObject["recording"]?.jsonObject
                Track(
                    id = rec?.get("id")?.jsonPrimitive?.content ?: "${id}_$index",
                    albumId = id,
                    title = rec?.get("title")?.jsonPrimitive?.content ?: track.jsonObject["title"]?.jsonPrimitive?.content.orEmpty(),
                    position = track.jsonObject["position"]?.jsonPrimitive?.intOrNull ?: index + 1,
                    durationSeconds = rec?.get("length")?.jsonPrimitive?.intOrNull?.div(1000),
                    rating = null,
                    notes = null
                )
            }
        }
        return Album(
            id = id,
            title = title,
            artist = artist,
            year = dateString?.let { LocalDate.parse(it).year },
            releaseDate = dateString?.let(LocalDate::parse),
            coverUrl = null,
            addedAt = kotlinx.datetime.Clock.System.now(),
            lastListenedAt = null,
            tracks = tracks
        )
}

private fun defaultClient(): HttpClient = HttpClient {
    install(Logging) {
        level = LogLevel.INFO
    }
}
