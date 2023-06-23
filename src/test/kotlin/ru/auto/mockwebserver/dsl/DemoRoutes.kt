package ru.auto.mockwebserver.dsl

fun Routing.getFeed(page: Int, assetPath: String) = get(
    "get feed, page $page",
    {
        pathContains("feed")
        query("page" to page.toString())
    },
    response {
        setBody(asset(assetPath))
    }
)

fun Routing.postOffer(id: String) = post(
    "post offer, id $id",
    {
        pathContains("offer")
    },
    response {
        setBody(asset("offer/saveSuccess.json"))
    }
)
