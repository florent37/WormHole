package com.github.florent37.flutterbridge.kotlin;

import kotlin.coroutines.Continuation;
import kotlinx.coroutines.Deferred;

public class KotlinHelper {
    public static <T> Object awaitDeffered(Deferred<T> deferred, Continuation<T> continuation) {
        return DefferedWaiterKt.awaitResponse(deferred, continuation);
    }
}
