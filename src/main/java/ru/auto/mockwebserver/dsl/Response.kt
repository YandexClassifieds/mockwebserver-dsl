package ru.auto.mockwebserver.dsl

import okhttp3.mockwebserver.MockResponse

fun response(responseDefinition: MockResponse.() -> Unit) =
    MockResponse().apply(responseDefinition)
