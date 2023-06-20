MockWebServer DSL
=================
# Documentation

---

- [Mock requests](#mock-requests)
- [Request matchers](#request-matchers)
- [Mock request for only one call](#mock-request-for-only-one-call)
- [Adding stubs](#adding-stubs)
- [Request asserts](#request-asserts)
- [Synchronize your test with requests](#synchronize-your-test-with-requests)
- [Download](#download)

---

### Mock requests
You can add mock for response by two ways:
1. While defining ```WebServerRule```:
    ```kotlin
    @get:Rule
    val webServerRule = WebServerRule {
        // your mock function
        getDraft()
    }
    ```
2. Inside test method using rule instance:
    ```kotlin
   @Test
   fun testSomething() {
       webServerRule.routing {
           // your mock function
           getDraft()
       }
       // test body
   }
    ```
Every routing block can contain any amount of mocks. Every ```routing``` block has effect immediately after definition. So, you can define mocks during test execution. By this reason, you can override mock from previous ```routing``` sections. Just define new mock with the same ```requestDefinition``` and different response.

Let's see how mock looks like:
```kotlin
get(
    description = "get feed, page $page",
    requestDefinition = {
        pathContains("feed")
        query(StringContains("page=$page"))
    },
    response = response {
        setBody(asset(assetPath))
    }
)
```
Above example can be divided in next logical blocks:
1. ```get(...)``` — this is method of request. The library supports **GET**, **POST**, **PUT** and **DELETE** methods. Use suitable method from the library
2. ```description``` — description of mock. Used in ```toString()``` method
3. ```requestDefinition``` — here you define condition for mock usage. Every http request is checked by this condition. See next section to learn more about matchers
4. ```response``` — builder for [MockResponse](https://github.com/square/okhttp/blob/master/mockwebserver/src/main/kotlin/mockwebserver3/MockResponse.kt). Here you define resulting response

### Request matchers

In previous section, we figured out that mock definition contains ```requestDefinition``` block where we define condition when mock is used. In other words, this is ```RequestMatcher``` definition.
Definition of ```RequestMatcher``` includes next matcher's extensions:
1. ```method(String)``` — **GET**, **POST**, **PUT** or **DELETE**.
Usually method matcher applied when you use top-level function with the same name:
    ```kotlin
    webServerRule.routing {
        post(...)
    }
    ```
2. ```path(Matcher<String?>)``` — matcher of encoded path.
Use library matcher or write your own one's:
    ```kotlin
    // library matcher
    post(
        "POST user cards listing",
        pathEnd("user/cards")
        // ...
    )
    // custom matcher
   get(
        "get user card with id «$id»",
        path(
            CoreMatchers.containsString("user/card/$id")
        )
    )
    ```
3. ```query(Matcher<String?>)``` — same logic as for path matcher, but input for matcher is encoded query string (```key=value&foo=bar```). For easy use library provides overridden methods, where input for matcher is ```Pair<String, String?>```:
    ```kotlin
    get(
        description = "get active offers for user with id $id",
        requestDefinition = {
            pathContains("/user/$id/offers")
            query("status" to "active")
        },
        // ...
   )
    ```
    Also, you can use custom matchers.
4. ```body(Matcher<String?>``` — matcher for body, where full body appeared as ```String?```

Often a good ```RequestMatcher``` definition is combination of matchers mentioned above:
```kotlin
post(
    description = "post event",
    requestDefinition = {
        pathContains("events/log")
        query("userId" to $userId)
        body(StringContains(eventName))
    },
    response = successResponse()
)
```
Also, to be mentioned that order of request matching is the same as we defined matchers above:

First, ```RequestMatcher``` matchers by method, then, by path, then, by query and finally by body.

### Mock request for only one call

In above examples mocks registered once used every time when request happened. But sometimes you need to use registered mock only once. For example, if your testing case is to check that request was called one time. For this case library provides ```OneOffRouting```. Register mock here, and it is used first and only once:
```kotlin
webServerRule.routing { oneOff { postSearchOffers() } }
```

### Adding stubs

Usually large apps make a lot of side requests that you don't want test exactly in particular test case. Background requests can be cause of test failure. So, you can mock them using ```stub``` function:
```kotlin
WebServerRule {
    stub { getCallsStats() }
    // ...
}
```

Often there is a group of side requests that you want to mock in every test case. According to DRY principle you could make an extension which would register all stub mocks:
```kotlin
fun WebServerRule.routeStubs() {
    stub { defaultStubRoutingDefinition(this) }
}

val defaultStubRoutingDefinition: RoutingDefinition<Unit> = {
    stubGetStory()
    stubGetStories()
    // ..
}

```

Stub mocks are used as last mocks.

### Request asserts

Another usage of this DSL is testing network layer using the range of assertions.

Example:

```kotlin
getFeed().assert {
    // here is used the same DSL as for definition of the route
    pathContains("personal/feed")
    query("key" to "value")
}
```

This code fails the test if assert is not succeed. For example if request `personal/feed` doesn't contain query `key=value`.

There are an examples of available assert methods:

1. `Route.assertCalled(count: Int)` — assert request was called exactly `count` times
2. `Route.assert(mode: AssertionMode, requestDefinition: RequestDefinition)` — assert request with given `AssertionMode` by given `RequestDefinition`. It means that method can be used with the same DSL as `Route` definition (see above code example). Another parameter is `AssertionMode`. This mode is used to define behavior of matching the requests: once assert was registered, it starts to record the history of requests. Then, assert looks for requests matches by given matcher from the history. By default, it looks for only first match and succeed, but you can change this behavior using method `times(count: Int, exactly: Boolean)` or defining your own `AssertionMode`.
3. `Route.assert(matcher: Matcher<RecordedRequest>, mode: AssertionMode)` — assert request with your own `Matcher`
4. `Route.baseAssert(matcher: Matcher<RecordingRoute>)` — define your own assert method using this base implementation

There is the list of available `AssertionMode`:
1. `times(count: Int, exactly: Boolean = true)` — assert that count of matches requests equals `count`. `exactly = true` means that all requests in the history must match to assert
2. `last()` — assert that last request in the history matches to assert
3. `index(index: Int)` — assert that request in history with index `index` matches to assert

Notice that all asserts run in the end of the test regardless where you put them.
If you require to listen the history immediately after every request happened, you can use these methods:
1. `Route.record()` — starts to store the history of the requests
    ```kotlin
    val getFeedRoute = webServerRule.routing { getFeed().record() }
    // this assert is called immediately
    getFeedRoute.assert(...)
    ```
2. `Route.count()` — starts to count the requests
    ```kotlin
    val getFeedRoute = webServerRule.routing { getFeed().count() }
    // this assert is called immediately
    getFeedRoute.assertCalled(1)
    ```

### Synchronize your test with requests

While writing UI-tests, we should remember about synchronization of running the test and running the application.

There are two issues which you can face writing UI-tests:

**1. Request performed earlier than the mock specified**

To fix it you can use method `await()` in `WebServerRule`.

Example:
```kotlin
@Test
fun testSomethingWithoutSync() {
   webServerRule.routing {
       getRequestThatStartsBeforeInitialization()
   }
   // here you get "404 NotFound" because request performed earlier mock specifying
}

@Test
fun testSomethingWithSync() {
   // hold requests before unlock
   webServerRule.await()
   // specifying mock
   webServerRule.routing {
       getRequestThatStartsBeforeInitialization()
   }
   // unlock requests
   webServerRule.unlock()
   // here you get exactly your mock
}
```

**2. Assert called earlier than request performed**

This issue should not be happened by default, but if you use immediately asserts (using `record()` or `count()`), you face it.
To fix this issue you can use some techniques of sync as [IdlingResource](https://developer.android.com/training/testing/espresso/idling-resource).

For example:

```kotlin
val getFeedRoute = webServerRule.routing { getFeed().record() }
Espresso.onIdle {
    getFeedRoute.assert(...)
}
```

### Download

```kotlin
androidTestImplementation("com.yandex.classifieds:mockwebserver-dsl:1.0.0")
```

Learn more on [GitHub]()
