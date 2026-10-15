package com.viniciuscastro.ingressos.data.catalog

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Source of the event catalog. Today it is a bundled JSON file; in production it would be a REST
 * client (Retrofit) behind this same interface, with no change to the repository or the domain.
 */
interface EventCatalog {
    suspend fun fetchEvents(): List<EventDto>
}

@Serializable
data class EventDto(
    val id: String,
    val title: String,
    val description: String,
    val venue: String,
    val city: String,
    /** ISO-8601 with offset, e.g. `2026-11-14T20:00:00-03:00`. */
    val startsAt: String,
    val priceCents: Long,
    val availableTickets: Int,
)

class AssetEventCatalog(
    private val context: Context,
    private val fileName: String = "events.json",
    private val json: Json = Json { ignoreUnknownKeys = true },
) : EventCatalog {

    override suspend fun fetchEvents(): List<EventDto> = withContext(Dispatchers.IO) {
        context.assets.open(fileName).bufferedReader().use { json.decodeFromString(it.readText()) }
    }
}
