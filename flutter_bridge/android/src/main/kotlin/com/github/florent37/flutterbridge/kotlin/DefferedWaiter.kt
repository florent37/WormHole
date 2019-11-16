package com.github.florent37.flutterbridge.kotlin

import kotlinx.coroutines.Deferred

suspend fun <T : Any> awaitResponse(deferred: Deferred<T>): T {
    return deferred.await()
}