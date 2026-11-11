package com.fdv.fdvflightlogger.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** One HTTP client and JSON configuration shared by every API the app talks to. */
object AppHttp {

    val json = Json {
        // FDV or SimBrief can add new fields without breaking older versions of the app
        ignoreUnknownKeys = true
        // A field that's missing from the response is treated the same as null
        explicitNulls = false
        // An unexpected null for a field with a default falls back to that default instead of crashing
        coerceInputValues = true
    }

    val client: HttpClient by lazy {
        HttpClient(OkHttp) {
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 15_000
                requestTimeoutMillis = 20_000
            }
            install(ContentNegotiation) {
                json(json)
            }
            // Non-2xx responses come back as normal responses, so each API decides what an error means
            expectSuccess = false
        }
    }
}