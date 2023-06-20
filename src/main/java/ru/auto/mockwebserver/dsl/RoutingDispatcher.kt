package ru.auto.mockwebserver.dsl

import junit.framework.AssertionFailedError
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.runners.model.MultipleFailureException
import java.net.HttpURLConnection
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class RoutingDispatcher : Dispatcher() {

    private val _routing = CompositeRouting(RoutingObserver())
    val routing: RootRouting get() = _routing

    private val exceptions: MutableList<Throwable> = CopyOnWriteArrayList()

    private var awaitTimeout: Long = 0
    private var awaitTimeUnit: TimeUnit = TimeUnit.SECONDS
    private var latch: CountDownLatch? = null
    var isAwaitElapsed = false
        private set

    @Suppress("TooGenericExceptionCaught")
    override fun dispatch(request: RecordedRequest): MockResponse {

        if (latch?.await(awaitTimeout, awaitTimeUnit) == false) {
            isAwaitElapsed = true
            latch = null
        }

        val resolvedRoute = try {
            _routing.resolve(request)
        }  catch (e: Throwable) {
            exceptions.add(e)
            null
        }
        return resolvedRoute
            ?.let { (request, response, route) ->
                routing.observer.dispatchOnResolve(request, response, route)
                response
            }
            ?: MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND).also {
                exceptions.add(AssertionFailedError(request.buildNoMockMessage()))
            }
    }

    fun after() {
        routing.observer.dispatchOnShutdown()
        MultipleFailureException.assertEmpty(exceptions + routing.observer.exceptions)
    }

    fun await(timeout: Long, unit: TimeUnit) {
        latch = CountDownLatch(1)
        awaitTimeout = timeout
        awaitTimeUnit = unit
    }

    fun unlock() {
        latch?.countDown()
        latch = null
    }

    private fun RecordedRequest.buildNoMockMessage() =
        buildString {
            append("Received no mock for the request ")
            appendLine(requestLine)
            if (body.size != 0L) {
                append(" ")
                append(body.peek().readUtf8())
            }
        }

}
