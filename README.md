MockWebServer DSL
=================

**MockWebServer DSL** is a Kotlin library for testing Android apps.
The library provides powerful DSL for [MockWebServer](https://github.com/square/okhttp/tree/master/mockwebserver). So, you can mock your http-client easy and concise.

### Getting started

Imagine if you want to test the feed screen in your app. Your app gets feed data from server by the **REST API**:

```GET https://myamazingapp.com/api/v1/feed?page=1```

Inside your test class define rule:

```kotlin
@get:Rule
val webServerRule = WebServerRule {
    getFeed(page = 1, assetPath = "feed/page_1.json")
}
```

Where ```getFeed()``` is definition of mock:

```kotlin
fun Routing.getFeed(page: Int, assetPath: String) = get(
    "get feed, page $page",
    {
        pathContains("feed")
        query(StringContains("page=$page"))
    },
    response {
        setBody(asset(assetPath))
    }
)
```

After that, your app gets successful response with body from ```feed/page_1.json``` file.

### Features 🚀
- Add any type (**GET**, **POST**, **PUT**, **DELETE**) and any amount of requests' mocks in single or multiple definition blocks
```kotlin
webServerRule.routing {
    getFeed(page = 1, assetPath = "feed/page_1.json")
    getFeed(page = 2, assetPath = "feed/page_2.json")
    postOffer(id = "abc-123")
}
```

- Override mocks if you need to change response in any execution point of your test
```kotlin
// simple feed mock
webServerRule.routing {
    getFeed(page = 1, assetPath = "feed/page_1.json")
}

// some routine...

// response changed from this point
webServerRule.routing {
    getFeed(page = 1, assetPath = "feed/page_1_viewed.json")
}
```

- Use different strategies of routing:
  - **CompositeRouting** — mock every request if suitable routing is registered
  - **OneOffRouting** — one routing for one shoot. if there are two the same requests only first is mocked
  - **StubRouting** — mock all background requests which are not important for testing

  More details in [docs](#documentation-).


- Test your data layer with assertions
```kotlin
webServerRule.routing {
    getDraft().assertCalled()
    getOffer().assertBody(equalsTo(getOfferBody()))
}
```

---

More features and full documentation you can find [here](docs/index.md).

### Documentation 📖
[Documentation](docs/index.md)

### Download
```kotlin
androidTestImplementation("com.yandex.classifieds:mockwebserver-dsl:1.0.0")
```

### Contributing 🤝
We welcome contributions to this project!

---

[Contributing policy](CONTRIBUTING.md)

### License 📄
[License](LICENSE)
