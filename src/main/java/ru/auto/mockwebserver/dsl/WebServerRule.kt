package ru.auto.mockwebserver.dsl

import okhttp3.mockwebserver.MockWebServer
import org.junit.rules.ExternalResource
import java.net.InetAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

open class WebServerRule(
    private val inetAddress: InetAddress = InetAddress.getByName("localhost"),
    private val webPort: Int = 8080,
    initRouting: RootRoutingDefinition<Unit>? = null,
) : ExternalResource() {

    val webServer: MockWebServer = MockWebServer()
    val dispatcher = RoutingDispatcher()

    private var awaitTimeout: Long = DEFAULT_AWAIT_TIMEOUT
    private var awaitTimeUnit: TimeUnit = TimeUnit.SECONDS
    private var latch: CountDownLatch? = null
    private var isAwaitElapsed = false

    init {
        routing { initRouting?.invoke(this) }
    }

    fun <T> routing(routingDefinition: RootRoutingDefinition<T>): T =
        dispatcher.routing.routingDefinition()

    fun await(timeout: Long = DEFAULT_AWAIT_TIMEOUT, unit: TimeUnit = TimeUnit.SECONDS) = apply {
        latch = CountDownLatch(1)
        awaitTimeout = timeout
        awaitTimeUnit = unit
        dispatcher.await(timeout, unit)
    }

    fun unlock() {
        latch?.countDown()
        latch = null
        dispatcher.unlock()
    }

    override fun before() {
        startWebServer()
    }

    override fun after() {
        shutdownWebServer()
    }

    open fun startWebServer() {
        webServer.dispatcher = dispatcher
        webServer.start(inetAddress, webPort)
    }

    open fun shutdownWebServer() {
        webServer.shutdown()
        dispatcher.after()
        checkAwaitElapsed()
    }

    private fun checkAwaitElapsed() {
        if (isAwaitElapsed || dispatcher.isAwaitElapsed) error("MockWebServer waiting time elapsed before unlock called")
    }

    companion object {
        private const val DEFAULT_AWAIT_TIMEOUT = 20L
    }
}
