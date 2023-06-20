package ru.auto.mockwebserver.dsl

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.Matcher
import org.hamcrest.StringDescription

interface Route {
    val routing: Routing
    val description: String
    fun resolve(request: RecordedRequest): MockResponse?
}

class MatcherRoute(
    override val routing: Routing,
    override val description: String,
    private val requestMatcher: Matcher<RecordedRequest>,
    private val response: (RecordedRequest) -> MockResponse,
) : Route {

    override fun resolve(request: RecordedRequest): MockResponse? =
        response.takeIf { requestMatcher.matches(request) }?.let { response(request) }

    override fun toString(): String = StringDescription()
        .appendText("Route(description=\"")
        .appendText(description)
        .appendText("\", requestMatcher=\"")
        .appendDescriptionOf(requestMatcher)
        .appendText("\")")
        .toString()
}
