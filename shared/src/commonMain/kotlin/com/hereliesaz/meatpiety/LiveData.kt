package com.hereliesaz.meatpiety

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Client for the MeatPiety worker (`worker/`), which holds API keys, scrapes
 * where there is no API, and caches everything for a day. Every call degrades
 * to null so the calculator keeps working offline on its built-in figures.
 */
internal const val WORKER_URL = "https://meatpiety.hereliesaz.workers.dev"

internal object LiveData {
    private val client by lazy { HttpClient() }

    private suspend fun getJson(path: String, vararg params: Pair<String, String>): JsonObject? = runCatching {
        val response = client.get(WORKER_URL + path) { params.forEach { (k, v) -> parameter(k, v) } }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        if (response.status.isSuccess()) body else body.also { throw LookupError(it["error"]?.jsonPrimitive?.content) }
    }.getOrElse { if (it is LookupError || it is kotlinx.coroutines.CancellationException) throw it else null }

    private class LookupError(message: String?) : Exception(message)

    /** Live U.S. retail prices in USD per species unit, keyed by [Species.name]. */
    suspend fun prices(): Map<String, Double> =
        getJson("/market")?.get("prices")?.jsonObject
            ?.mapNotNull { (k, v) -> v.jsonPrimitive.doubleOrNull?.let { k to it } }?.toMap()
            ?: emptyMap()

    /** Exchange rate into the device's currency, or the visitor's country's currency when unknown. */
    suspend fun money(): Money? {
        val code = deviceCurrencyCode()
        val body = if (code != null) getJson("/fx", "currency" to code) else getJson("/fx")
        val currency = body?.get("currency")?.jsonPrimitive?.content ?: return null
        val rate = body["perUsd"]?.jsonPrimitive?.doubleOrNull ?: return null
        return Money(currency, rate)
    }

    suspend fun followers(platform: Platform, handle: String): FollowerLookup = try {
        val body = getJson("/followers", "platform" to platform.name.lowercase(), "handle" to handle)
        body?.get("followers")?.jsonPrimitive?.longOrNull?.let { FollowerLookup.Found(it) }
            ?: FollowerLookup.Failed("couldn't reach the lookup service")
    } catch (e: LookupError) {
        FollowerLookup.Failed(e.message ?: "couldn't read that account")
    }
}
