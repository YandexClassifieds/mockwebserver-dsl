package ru.auto.mockwebserver.dsl

import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.TypeSafeDiagnosingMatcher
import org.hamcrest.TypeSafeMatcher

fun Route.assertCalled(count: Int): Route =
    assertCalled(equalTo(count))

fun Route.assertCalled(matcher: Matcher<Int>): Route =
    baseAssertCalled(RequestCountMatcher(matcher))

fun Route.baseAssertCalled(matcher: Matcher<CountingRoute>): Route =
    assert(Route::count, matcher)

fun Route.assert(mode: AssertionMode<RecordedRequest> = times(1), requestDefinition: RequestDefinition): Route =
    assert(request(requestDefinition), mode)

fun Route.assert(matcher: Matcher<RecordedRequest>, mode: AssertionMode<RecordedRequest> = times(1)): Route =
    baseAssert(RecordedRequestsMatcher(matcher, mode))

fun Route.baseAssert(matcher: Matcher<RecordingRoute>): Route =
    assert(Route::record, matcher)

private inline fun <reified T : Route> Route.assert(
    action: (Route) -> T,
    matcher: Matcher<T>,
): Route {
    when (this) {
        is T -> {
            assertThat(this.toString(), this, matcher)
        }
        else -> {
            val route = action(this)
            onShutdown { assertThat(route.toString(), route, matcher) }
        }
    }
    return this
}

class RequestCountMatcher(
    private val countMatcher: Matcher<Int>
) : TypeSafeMatcher<CountingRoute>() {

    override fun describeTo(description: Description) {
        description
            .appendText("should be called ")
            .appendDescriptionOf(countMatcher)
            .appendText(" times")
    }

    override fun matchesSafely(item: CountingRoute): Boolean =
        countMatcher.matches(item.count)

    override fun describeMismatchSafely(item: CountingRoute, mismatchDescription: Description) {
        countMatcher.describeMismatch(item.count, mismatchDescription)
    }
}

class RecordedRequestsMatcher(
    private val matcher: Matcher<RecordedRequest>,
    private val assertionMode: AssertionMode<RecordedRequest>
) : TypeSafeDiagnosingMatcher<RecordingRoute>() {

    override fun describeTo(description: Description) {
        description.appendText("recorded requests should ")
        assertionMode.describeTo(matcher, description)
    }

    override fun matchesSafely(item: RecordingRoute, mismatchDescription: Description): Boolean {
        val items = item.recordedRequests
        val matches = assertionMode.matches(matcher, items, mismatchDescription)
        if (!matches) {
            with(mismatchDescription) {
                appendText("\nFound ").appendText(items.size.toString()).appendText(" request(s)")
                if (items.isNotEmpty()) {
                    appendText(": ")
                    items.forEachIndexed { index, recordedRequest ->
                        appendText("\n[").appendText(index.toString()).appendText("] ")
                        appendText(recordedRequest.requestLine)
                        if (recordedRequest.body.size != 0L) {
                            appendText(" ")
                            appendText(recordedRequest.body.peek().readUtf8())
                        }
                    }
                }
            }
        }
        return matches
    }
}
