package ru.auto.mockwebserver.dsl

import okhttp3.Headers
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.anyOf
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.endsWith
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.core.AllOf

typealias RequestDefinition = RequestMatcher.Builder.() -> Unit

class RequestMatcher private constructor(
    methodMatcher: Matcher<String?>?,
    pathMatcher: Matcher<String?>?,
    queryMatcher: Matcher<String?>?,
    bodyMatcher: Matcher<String?>?,
    headersMatcher: Matcher<in Headers>?
) : AllOf<RecordedRequest>(
    listOfNotNull(
        hasProperty("method", { method }, methodMatcher),
        hasProperty("path", { requestUrl?.encodedPath }, pathMatcher),
        hasProperty("query", { requestUrl?.query }, queryMatcher),
        hasProperty("body", { body.peek().readUtf8() }, bodyMatcher),
        hasProperty("headers", { headers }, headersMatcher),
    )
) {

    @DslMarker
    annotation class RequestMarker

    @RequestMarker
    class Builder {

        private var methodMatcher: Matcher<String?>? = null
        private var pathMatcher: Matcher<String?>? = null
        private var queryMatcher: Matcher<String?>? = null
        private var bodyMatcher: Matcher<String?>? = null
        private var headersMatcher: Matcher<in Headers>? = null

        fun method(matcher: Matcher<String?>): Builder {
            require(methodMatcher == null) { "method(...) is already defined. Only one method(...) is allowed." }
            methodMatcher = matcher
            return this
        }

        fun path(matcher: Matcher<String?>): Builder {
            require(pathMatcher == null) { "path(...) is already defined. Only one path(...) is allowed." }
            pathMatcher = matcher
            return this
        }

        fun query(matcher: Matcher<String?>): Builder {
            require(queryMatcher == null) { "query(...) is already defined. Only one query(...) is allowed." }
            queryMatcher = matcher
            return this
        }

        fun body(matcher: Matcher<String?>): Builder {
            require(bodyMatcher == null) { "body(...) is already defined. Only one body(...) is allowed." }
            bodyMatcher = matcher
            return this
        }

        fun headers(matcher: Matcher<in Headers>): Builder {
            require(headersMatcher == null) { "headers(...) is already defined. Only one headers(...) is allowed." }
            headersMatcher = matcher
            return this
        }

        fun build(): RequestMatcher =
            RequestMatcher(
                methodMatcher = methodMatcher,
                pathMatcher = pathMatcher,
                queryMatcher = queryMatcher,
                bodyMatcher = bodyMatcher,
                headersMatcher = headersMatcher
            )
    }
}

fun request(request: RequestDefinition): RequestMatcher =
    RequestMatcher.Builder().apply(request).build()

fun RequestMatcher.Builder.method(method: String) =
    method(equalTo(method))

fun RequestMatcher.Builder.get() =
    method("GET")

fun RequestMatcher.Builder.post() =
    method("POST")

fun RequestMatcher.Builder.put() =
    method("PUT")

fun RequestMatcher.Builder.delete() =
    method("DELETE")

fun get(request: RequestDefinition) =
    request { get(); request() }

fun post(request: RequestDefinition) =
    request { post(); request() }

fun put(request: RequestDefinition) =
    request { put(); request() }

fun delete(request: RequestDefinition) =
    request { delete(); request() }

fun RequestMatcher.Builder.pathContains(subPath: String) =
    path(containsString(subPath))

fun RequestMatcher.Builder.pathContainsAny(vararg subPaths: String) =
    path(anyOf(subPaths.map(::containsString)))

fun RequestMatcher.Builder.pathContainsAll(vararg subPaths: String) =
    path(allOf(subPaths.map(::containsString)))

fun RequestMatcher.Builder.path(path: String) =
    path(equalTo(path))

fun RequestMatcher.Builder.pathEnd(end: String) =
    path(endsWith(end))

fun pathEnd(end: String): RequestDefinition = { pathEnd(end) }

fun pathContains(subPath: String): RequestDefinition = { pathContains(subPath) }

fun pathContainsAny(vararg subPaths: String): RequestDefinition = { pathContainsAny(*subPaths) }

fun pathContainsAll(vararg subPaths: String): RequestDefinition = { pathContainsAll(*subPaths) }

fun path(path: String): RequestDefinition = { path(path) }

fun path(matcher: Matcher<String?>): RequestDefinition = { path(matcher) }

fun RequestMatcher.Builder.query(query: List<Pair<Matcher<String>, Matcher<String?>>>) =
    query(ru.auto.mockwebserver.dsl.query(query))

fun RequestMatcher.Builder.query(vararg query: Pair<Matcher<String>, Matcher<String?>>) =
    query(query.toList())

@JvmName("queryIs")
fun RequestMatcher.Builder.query(query: List<Pair<String, String?>>) =
    query(ru.auto.mockwebserver.dsl.query(query))

@JvmName("queryIs")
fun RequestMatcher.Builder.query(vararg query: Pair<String, String?>) =
    query(query.toList())

@JvmName("queryNameIs")
fun RequestMatcher.Builder.query(query: List<Pair<String, Matcher<String?>>>) =
    query(ru.auto.mockwebserver.dsl.query(query.toList()))

@JvmName("queryNameIs")
fun RequestMatcher.Builder.query(vararg query: Pair<String, Matcher<String?>>) =
    query(ru.auto.mockwebserver.dsl.query(query.toList()))

private fun <T, R> hasProperty(name: String, getter: T.() -> R, matcher: Matcher<R>?) =
    matcher?.let { PropertyMatcher(name, getter, matcher) }

private class PropertyMatcher<T, R>(
    private val name: String,
    private val getter: (T) -> R,
    private val matcher: Matcher<R>
) : TypeSafeMatcher<T>() {

    override fun describeTo(description: Description) {
        description.appendText(name).appendText(" ").appendDescriptionOf(matcher)
    }

    override fun matchesSafely(item: T): Boolean =
        matcher.matches(getter(item))

    override fun describeMismatchSafely(item: T, mismatchDescription: Description) {
        matcher.describeMismatch(getter(item), mismatchDescription)
    }
}
