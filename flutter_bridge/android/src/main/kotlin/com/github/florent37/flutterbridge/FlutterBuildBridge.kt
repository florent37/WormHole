package com.github.florent37.flutterbridge

import io.flutter.app.FlutterActivity
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.view.FlutterView
import kotlin.reflect.KProperty

inline fun <reified T> BinaryMessenger.buildBridge(channelName: String) : T {
    return BridgeManager.findOrCreate(this, channelName).buildBridge()
}

inline fun <B : BinaryMessenger> BinaryMessenger.expose(channelName: String, annotatedElement: Any) {
    BridgeManager.findOrCreate(this, channelName).expose(annotatedElement)
}

inline fun <reified T> FlutterView.Provider.buildBridge(channelName: String) : T {
    return BridgeManager.findOrCreate(this.flutterView, channelName).buildBridge()
}

inline fun FlutterView.Provider.expose(channelName: String, annotatedElement: Any) {
    BridgeManager.findOrCreate(this.flutterView, channelName).expose(annotatedElement)
}

/*
class FlutterBridge<T>(val initValue : T) {
    operator fun getValue(owner: Any, property: KProperty<*>): T {
        return initValue
    }
}
*/

inline fun <reified T> FlutterActivity.flutterBridge(channelName: String) = lazy { buildBridge<T>(channelName) }
inline fun <reified T> BinaryMessenger.flutterBridge(channelName: String) = lazy { buildBridge<T>(channelName) }
