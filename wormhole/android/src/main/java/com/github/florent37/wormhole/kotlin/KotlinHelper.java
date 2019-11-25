package com.github.florent37.wormhole.kotlin;

import kotlin.coroutines.Continuation;
import kotlinx.coroutines.Deferred;

public class KotlinHelper {
    public static <T> Object awaitDeffered(Deferred<T> deferred, Continuation<T> continuation) {
        return DefferedWaiterKt.awaitResponse(deferred, continuation);
    }
}
