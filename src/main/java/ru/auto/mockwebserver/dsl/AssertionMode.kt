package ru.auto.mockwebserver.dsl

import org.hamcrest.Description
import org.hamcrest.Matcher

interface AssertionMode<T> {
    fun describeTo(matcher: Matcher<T>, description: Description)
    fun matches(
        matcher: Matcher<T>,
        recordedItems: List<T>,
        mismatchDescription: Description
    ): Boolean
}

fun <T> times(count: Int, exactly: Boolean = true) = object : AssertionMode<T> {

    override fun describeTo(matcher: Matcher<T>, description: Description) {
        description
            .appendText("containing ")
            .apply { if (exactly) appendText("exactly ") }
            .appendValue(count)
            .appendText(" item(s) that is ")
            .appendDescriptionOf(matcher)
    }

    override fun matches(
        matcher: Matcher<T>,
        recordedItems: List<T>,
        mismatchDescription: Description
    ): Boolean {
        val notMatched: List<Pair<Int, T>> = recordedItems.filterMatched(matcher)
        val matchedCount = recordedItems.size - notMatched.size
        if (count != matchedCount || (exactly && notMatched.isNotEmpty())) {
            mismatchDescription
                .appendText("contained ")
                .appendValue(matchedCount)
                .appendText(" matched item(s)")

            if (notMatched.isNotEmpty()) {
                mismatchDescription.appendText("\nNot matched items(s) found: ")
                notMatched.forEach { (index, request) ->
                    mismatchDescription.appendText("\n[").appendText(index.toString()).appendText("] ")
                    matcher.describeMismatch(request, mismatchDescription)
                }
            }
            return false
        }

        return true
    }

    private fun List<T>.filterMatched(matcher: Matcher<T>) =
        mapIndexedNotNull { index, request -> if (!matcher.matches(request)) index to request else null }
}

@Suppress("StringLiteralDuplication")
fun <T> index(index: Int) = object : AssertionMode<T> {

    override fun describeTo(matcher: Matcher<T>, description: Description) {
        description
            .appendText("containing item[")
            .appendText(index.toString())
            .appendText( "] that is ")
            .appendDescriptionOf(matcher)
    }

    override fun matches(
        matcher: Matcher<T>,
        recordedItems: List<T>,
        mismatchDescription: Description
    ): Boolean {
        if (recordedItems.isEmpty()) {
            mismatchDescription.appendText("was empty")
            return false
        }
        if (index !in recordedItems.indices) {
            mismatchDescription.appendText("index not in ").appendText(recordedItems.indices.toString())
            return false
        }
        if (!matcher.matches(recordedItems[index])) {
            matcher.describeMismatch(recordedItems[index], mismatchDescription)
            return false
        }
        return true
    }
}

@Suppress("StringLiteralDuplication")
fun <T> last() = object : AssertionMode<T> {

    override fun describeTo(matcher: Matcher<T>, description: Description) {
        description
            .appendText("containing last item that is ")
            .appendDescriptionOf(matcher)
    }

    override fun matches(
        matcher: Matcher<T>,
        recordedItems: List<T>,
        mismatchDescription: Description
    ): Boolean {
        if (recordedItems.isEmpty()) {
            mismatchDescription.appendText("was empty")
            return false
        }
        if (!matcher.matches(recordedItems.last())) {
            matcher.describeMismatch(recordedItems.last(), mismatchDescription)
            return false
        }
        return true
    }
}
