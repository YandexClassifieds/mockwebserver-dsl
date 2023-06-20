package ru.auto.mockwebserver.dsl

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class RoutingObserver {

    private val onResolveListeners: MutableList<OnResolveListener> = CopyOnWriteArrayList()
    private val onShutdownListeners: MutableList<OnShutdownListener> = CopyOnWriteArrayList()
    private val _exceptions: MutableList<Throwable> = CopyOnWriteArrayList()
    val exceptions: List<Throwable> = _exceptions

    fun interface OnResolveListener {
        fun onResolve(request: RecordedRequest, response: MockResponse, route: Route)
    }

    fun interface OnShutdownListener {
        fun onShutdown()
    }

    fun addOnResolveListener(listener: OnResolveListener) {
        val listeners = onResolveListeners
        listeners.add(listener)
    }

    fun removeOnResolveListener(victim: OnResolveListener?) {
        onResolveListeners.remove(victim)
    }

    @Suppress("TooGenericExceptionCaught")
    fun dispatchOnResolve(request: RecordedRequest, response: MockResponse, route: Route) {
        val listeners = onResolveListeners
        if (listeners.size > 0) {
            for (listener in listeners) {
                try {
                    listener.onResolve(request, response, route)
                } catch (e: Throwable) {
                    _exceptions.add(e)
                }
            }
        }
    }

    fun addOnShutdownListener(listener: OnShutdownListener) {
        val listeners = onShutdownListeners
        listeners.add(listener)
    }

    fun removeOnShutdownListener(victim: OnShutdownListener?) {
        onShutdownListeners.remove(victim)
    }

    @Suppress("TooGenericExceptionCaught")
    fun dispatchOnShutdown() {
        val listeners = onShutdownListeners
        if (listeners.size > 0) {
            for (listener in listeners) {
                try {
                    listener.onShutdown()
                } catch (e: Throwable) {
                    _exceptions.add(e)
                }
            }
        }
    }

}

class RecordingRoute(
    private val route: Route
) : Route by route, RoutingObserver.OnResolveListener {

    private val _recordedRequests: MutableList<RecordedRequest> =
        Collections.synchronizedList(ArrayList<RecordedRequest>())
    val recordedRequests: List<RecordedRequest> get() = _recordedRequests

    override fun onResolve(request: RecordedRequest, response: MockResponse, route: Route) {
        if (this.route == route) {
            _recordedRequests += request
        }
    }

    override fun toString(): String = route.toString()
}

class CountingRoute(
    private val route: Route
) : Route by route, RoutingObserver.OnResolveListener {
    private val _count = AtomicInteger()
    val count: Int get() = _count.get()

    override fun onResolve(request: RecordedRequest, response: MockResponse, route: Route) {
        if (this.route == route) {
            _count.incrementAndGet()
        }
    }

    override fun toString(): String = route.toString()
}

fun Route.record(): RecordingRoute =
    RecordingRoute(this).also(routing.observer::addOnResolveListener)

fun Route.count(): CountingRoute =
    CountingRoute(this).also(routing.observer::addOnResolveListener)

fun Route.onResolve(listener: RoutingObserver.OnResolveListener): RoutingObserver.OnResolveListener {
    routing.observer.addOnResolveListener(listener)
    return listener
}

fun Route.onShutdown(listener: RoutingObserver.OnShutdownListener): RoutingObserver.OnShutdownListener {
    routing.observer.addOnShutdownListener(listener)
    return listener
}
