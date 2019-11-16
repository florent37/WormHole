package com.github.florent37.flutterbridge.deferred

import android.util.Log
import com.github.florent37.flutterbridge.ArgumentTransformer
import com.github.florent37.flutterbridge.kotlin.KotlinHelper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import java.lang.reflect.Type
import kotlin.coroutines.Continuation

class WaitingCall<T>(
    val methodName: String,
    val returnType: Type,
    val argumentTransformer: ArgumentTransformer,
    val completable: CompletableDeferred<T>
) {
    fun complete(arg: Any?) {
        if (completable.isActive) {
            try {
                completable.complete(
                    argumentTransformer.transformeReceived(
                        arg,
                        returnType,
                        null
                    ) as T
                )
            } catch (t: Throwable) {
                Log.e("WaitingCall", t.message, t)
            }
        }
    }
}

/**
 * Useful to handle Java Reflection & Coroutine
 */
fun Deferred<Any?>.awaitWithContinuation(continuation: Continuation<Any?>): Any? {
    return KotlinHelper.awaitDeffered(this, continuation)
}