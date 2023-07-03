package ru.auto.mockwebserver.dsl

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.function.Executable
import org.junit.jupiter.api.function.ThrowingConsumer

class DemoTest {

    @get:Rule
    val webServerRule = WebServerRule {
        getFeed(page = 1, assetPath = "feed/page_1.json")
    }

    @Test
    fun testFeedFirst() {
        check(
            request("/feed?page=1"),
            { response -> assertTrue(response.isSuccessful, "Status") },
            { response -> assertEquals(response.body?.string(), asset("feed/page_1.json"), "body") },
        )
    }

    @Test
    fun testDifferentRequestTypes() {
        webServerRule.routing {
            getFeed(page = 1, assetPath = "feed/page_1.json")
            getFeed(page = 2, assetPath = "feed/page_2.json")
            postOffer(id = "abc-123")
        }
        check(
            request("/offer").post("{}".toRequestBody(APPLICATION_JSON)),
            { response -> assertTrue(response.isSuccessful, "Status") },
        )
    }

    @Test
    fun testOverrideMocks() {
        // simple feed mock
        webServerRule.routing {
            getFeed(page = 1, assetPath = "feed/page_1.json")
        }
        check(
            request("/feed?page=1"),
            { response -> assertTrue(response.isSuccessful, "Status") },
            { response -> assertEquals(response.body?.string(), asset("feed/page_1.json"), "body") },
        )

        // some routine...

        // response changed from this point
        webServerRule.routing {
            getFeed(page = 1, assetPath = "feed/page_1_viewed.json")
        }
        check(
            request("/feed?page=1"),
            { response -> assertTrue(response.isSuccessful, "Status") },
            { response -> assertEquals(response.body?.string(), asset("feed/page_1_viewed.json"), "body") },
        )
    }

    private fun request(link: String) = Request.Builder()
        .url(mockServerUrl.resolve(link)!!)
        .header("User-Agent", "DemoTest.kt")

    @Suppress("HttpUrlsUsage")
    private val mockServerUrl: HttpUrl by lazy {
        "http://${webServerRule.webServer.hostName}:${webServerRule.webServer.port}/".toHttpUrl()
    }

    private fun check(request: Request.Builder, vararg checks: ThrowingConsumer<Response>) = client
        .newCall(request.build()).execute()
        .use { response ->
            assertAll(
                checks.asList().map { Executable { it.accept(response) } }
            )
        }

    companion object {
        private val client: OkHttpClient by lazy { OkHttpClient() }
        private val APPLICATION_JSON = "application/json; charset=utf-8".toMediaType()
    }
}
