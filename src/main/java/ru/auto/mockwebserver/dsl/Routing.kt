package ru.auto.mockwebserver.dsl

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import java.util.Collections

typealias RoutingDefinition<T> = Routing.() -> T
typealias RootRoutingDefinition<T> = RootRouting.() -> T

@DslMarker
annotation class RoutingMarker

@RoutingMarker
interface Routing {
    val observer: RoutingObserver
    fun route(route: Route): Route
    fun remove(route: Route)
    fun clear()
}

interface RootRouting : Routing {
    fun <T> stub(routingDefinition: RoutingDefinition<T>): T
    fun <T> oneOff(routingDefinition: RoutingDefinition<T>): T
}

data class ResolvedRoute(val request: RecordedRequest, val response: MockResponse, val route: Route)

open class StaticRouting(
    override val observer: RoutingObserver
) : Routing {

    protected val routeRegistry: MutableList<Route> = Collections.synchronizedList(ArrayList())

    override fun route(route: Route): Route =
        route.also { routeRegistry.add(it) }

    override fun remove(route: Route) {
        routeRegistry.remove(route)
    }

    override fun clear() {
        routeRegistry.clear()
    }

    open fun resolve(request: RecordedRequest): ResolvedRoute? {
        synchronized(routeRegistry) {
            val iterator = routeRegistry.listIterator(routeRegistry.size)
            while (iterator.hasPrevious()) {
                val route = iterator.previous()
                val response = route.resolve(request)
                if (response != null) {
                    return ResolvedRoute(request, response, route)
                }
            }
            return null
        }
    }
}

class OneOffRouting(observer: RoutingObserver) : StaticRouting(observer) {

    override fun resolve(request: RecordedRequest): ResolvedRoute? {
        synchronized(routeRegistry) {
            val iterator = routeRegistry.listIterator(routeRegistry.size)
            while (iterator.hasPrevious()) {
                val route = iterator.previous()
                val response = route.resolve(request)
                if (response != null) {
                    iterator.remove()
                    return ResolvedRoute(request, response, route)
                }
            }
            return null
        }
    }
}

class CompositeRouting(observer: RoutingObserver) : StaticRouting(observer), RootRouting {

    private val stubRouting = StaticRouting(observer)
    private val oneOffRouting = OneOffRouting(observer)

    override fun <T> stub(routingDefinition: RoutingDefinition<T>): T =
        stubRouting.routingDefinition()

    override fun <T> oneOff(routingDefinition: RoutingDefinition<T>): T =
        oneOffRouting.routingDefinition()

    override fun resolve(request: RecordedRequest): ResolvedRoute? =
        oneOffRouting.resolve(request)
            ?: super.resolve(request)
            ?: stubRouting.resolve(request)
}

fun Routing.route(description: String, request: RequestMatcher, response: (RecordedRequest) -> MockResponse): Route =
    route(MatcherRoute(routing = this, description = description, requestMatcher = request, response = response))

fun Routing.route(description: String, request: RequestMatcher, response: MockResponse): Route =
    route(description = description, request = request, response = { response })

fun Routing.route(description: String, request: RequestDefinition, response: MockResponse): Route =
    route(description = description, request = request(request), response = response)

fun Routing.get(description: String, request: RequestDefinition, response: MockResponse): Route =
    route(description = description, request = get(request), response = response)

fun Routing.get(description: String, request: RequestDefinition, response: (RecordedRequest) -> MockResponse): Route =
    route(description = description, request = get(request), response = response)

fun Routing.post(description: String, request: RequestDefinition, response: MockResponse): Route =
    route(description = description, request = post(request), response = response)

fun Routing.post(description: String, request: RequestDefinition, response: (RecordedRequest) -> MockResponse): Route =
    route(description = description, request = post(request), response = response)

fun Routing.put(description: String, request: RequestDefinition, response: MockResponse): Route =
    route(description = description, request = put(request), response = response)

fun Routing.put(description: String, request: RequestDefinition, response: (RecordedRequest) -> MockResponse): Route =
    route(description = description, request = put(request), response = response)

fun Routing.delete(description: String, request: RequestDefinition, response: MockResponse): Route =
    route(description = description, request = delete(request), response = response)
