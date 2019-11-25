package com.github.florent37.wormhole.kotlin

import kotlinx.coroutines.Deferred

suspend fun <T : Any> awaitResponse(deferred: Deferred<T>): T {
    return deferred.await()
}