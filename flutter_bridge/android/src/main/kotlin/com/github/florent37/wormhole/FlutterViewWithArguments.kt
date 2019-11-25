package com.github.florent37.wormhole

import android.view.View
import android.view.ViewGroup
import io.flutter.view.FlutterView
import kotlinx.coroutines.*

data class FlutterEvent(
    val method: String,
    val argument: Any?
)

suspend fun FlutterView.waitFirstFrame() = suspendCancellableCoroutine<Boolean> { continuation ->
    val flutterView = this
    flutterView.addFirstFrameListener(object : FlutterView.FirstFrameListener {
        override fun onFirstFrame() {
            continuation.resume(true, onCancellation = {
                flutterView.removeFirstFrameListener(this)
            })
        }
    })
}

suspend fun FlutterView.addInto(
    viewGroup: ViewGroup,
    width: Int = ViewGroup.LayoutParams.MATCH_PARENT,
    height: Int = ViewGroup.LayoutParams.MATCH_PARENT
) : FlutterView {
    viewGroup.visibility = View.INVISIBLE
    viewGroup.addView(this, width, height)
    waitFirstFrame()
    viewGroup.visibility = View.VISIBLE
    return this
}

suspend fun FlutterView.invoke(
    identifier: String,
    methodName: String,
    params: Any
) {
    BridgeManager.findOrCreate(this, identifier).invoke(methodName, params)
}