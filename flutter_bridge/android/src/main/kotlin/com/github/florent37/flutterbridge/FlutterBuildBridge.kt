package com.github.florent37.flutterbridge

import io.flutter.plugin.common.BinaryMessenger
import io.flutter.view.FlutterView

inline fun <reified T, B : BinaryMessenger> B.buildBridge(channelName: String) : T {
    return BridgeManager.findOrCreate(this, channelName).buildBridge()
}

inline fun <B : BinaryMessenger> B.expose(channelName: String, annotatedElement: Any) {
    BridgeManager.findOrCreate(this, channelName).expose(annotatedElement)
}

inline fun <reified T> FlutterView.Provider.buildBridge(channelName: String) : T {
    return BridgeManager.findOrCreate(this.flutterView, channelName).buildBridge()
}

inline fun FlutterView.Provider.expose(channelName: String, annotatedElement: Any) {
    BridgeManager.findOrCreate(this.flutterView, channelName).expose(annotatedElement)
}