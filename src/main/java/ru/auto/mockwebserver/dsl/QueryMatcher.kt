package ru.auto.mockwebserver.dsl

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeDiagnosingMatcher

class QueryMatcher(
    private val matchers: List<Pair<Matcher<String>, Matcher<String?>>>,
    private val exactly: Boolean = false
) : TypeSafeDiagnosingMatcher<String>() {

    override fun describeTo(description: Description) {
        with(description) {
            appendText("a string containing query parameters ")
            appendText(matchers.toString())
            if (exactly) appendText(" exactly")
        }
    }

    override fun matchesSafely(item: String, mismatchDescription: Description): Boolean {
        val matchers = matchers.toMutableList()
        val queryNamesAndValues = item.toQueryNamesAndValues()

        fun matches(name: String, value: String): Boolean {
            if (matchers.isEmpty()) {
                if (exactly) {
                    mismatchDescription.appendNoMatch(name, value, item)
                    return false
                }
                return true
            }

            val iterator = matchers.listIterator()
            while (iterator.hasNext()) {
                val (nameMatcher, valueMatcher) = iterator.next()
                if (nameMatcher.matches(name)) {
                    if (valueMatcher.matches(value)) {
                        iterator.remove()
                        return true
                    }
                    if (valueMatcher.matches(null)) {
                        mismatchDescription.appendNotMatchedParameter(name, value, item)
                        return false
                    }
                }
            }

            if (exactly) {
                mismatchDescription.appendNotMatchedParameter(name, value, item)
                return false
            }

            return true
        }

        fun isFinished(): Boolean {
            matchers.removeIf { it.second.matches(null) }

            if (matchers.isEmpty()) {
                return true
            }

            mismatchDescription.appendNoQueryParameterMatches(matchers.toString(), item)
            return false
        }

        return queryNamesAndValues.all(::matches) && isFinished()
    }

    /**
     * Cuts this string up into alternating parameter names and values. This divides a query string
     * like `subject=math&easy&problem=5-2=3` into the list `["subject", "math", "easy", null,
     * "problem", "5-2=3"]`. Note that values may be null and may contain '=' characters.
     */
    private fun String.toQueryNamesAndValues(): List<String> {
        val result = mutableListOf<String>()
        var pos = 0
        while (pos <= length) {
            var ampersandOffset = indexOf('&', pos)
            if (ampersandOffset == -1) ampersandOffset = length

            val equalsOffset = indexOf('=', pos)
            if (equalsOffset == -1 || equalsOffset > ampersandOffset) {
                result.add(substring(pos, ampersandOffset))
                result.add("") // No value for this name.
            } else {
                result.add(substring(pos, equalsOffset))
                result.add(substring(equalsOffset + 1, ampersandOffset))
            }
            pos = ampersandOffset + 1
        }
        return result
    }

    private inline fun List<String>.all(predicate: (name: String, value: String) -> Boolean): Boolean {
        forEach { name, value -> if (!predicate(name, value)) return false }
        return true
    }

    private inline fun List<String>.forEach(action: (name: String, value: String) -> Unit) {
        for (i in indices step 2) action(this[i], this[i + 1])
    }

    private fun Description.appendNoMatch(name: String, value: String, query: String): Description  =
        appendText("no match for: ")
            .appendQueryParameter(name, value)
            .appendText(" in ").appendQuery(query)

    private fun Description.appendNotMatchedParameter(name: String, value: String, query: String): Description  =
        appendText("not matched parameter: ")
            .appendQueryParameter(name, value)
            .appendText(" in ").appendQuery(query)

    private fun Description.appendNoQueryParameterMatches(matchers: String, query: String): Description  =
        appendText("no query parameter matches: ")
            .appendText(matchers)
            .appendText(" in ").appendQuery(query)

    private fun Description.appendQueryParameter(name: String, value: String): Description =
        appendText("\"").appendText(name).appendText("\"=\"").appendText(value).appendText("\"")

    private fun Description.appendQuery(query: String): Description =
        appendText("\"").appendText(query).appendText("\"")

}

fun query(pairs: List<Pair<Matcher<String>, Matcher<String?>>>, exactly: Boolean = false) =
    QueryMatcher(pairs, exactly)

fun query(vararg pairs: Pair<Matcher<String>, Matcher<String?>>, exactly: Boolean = false) =
    query(pairs.toList(), exactly)

@JvmName("queryEqualTo")
fun query(pairs: List<Pair<String, String?>>, exactly: Boolean = false) =
    query(pairs.map { (name, value) -> equalTo(name) to equalTo(value) }, exactly)

@JvmName("queryEqualTo")
fun query(vararg pairs: Pair<String, String?>, exactly: Boolean = false) =
    query(pairs.toList(), exactly)

@JvmName("queryNameEqualTo")
fun query(pairs: List<Pair<String, Matcher<String?>>>, exactly: Boolean = false) =
    query(pairs.map { (name, value) -> equalTo(name) to value }, exactly)

@JvmName("queryNameEqualTo")
fun query(vararg pairs: Pair<String, Matcher<String?>>, exactly: Boolean = false) =
    query(pairs.toList(), exactly)
