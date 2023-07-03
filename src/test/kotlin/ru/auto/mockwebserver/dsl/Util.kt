package ru.auto.mockwebserver.dsl

fun asset(assetPath: String): String = WebServerRule::class.java.classLoader.getResourceAsStream(assetPath)
    ?.use { it.bufferedReader().readText() }
    ?: throw IllegalStateException("Asset is absent: $assetPath")
