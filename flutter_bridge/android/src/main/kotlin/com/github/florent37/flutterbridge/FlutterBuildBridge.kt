package com.github.florent37.flutterbridge

import io.flutter.app.FlutterActivity
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.view.FlutterView

inline fun <reified T> BinaryMessenger.retrieve(channelName: String) : T {
    return BridgeManager.findOrCreate(this, channelName).buildBridge()
}

inline fun BinaryMessenger.expose(channelName: String, annotatedElement: Any) {
    BridgeManager.findOrCreate(this, channelName).expose(annotatedElement)
}

inline fun <reified T> FlutterView.Provider.retrieve(channelName: String) : T {
    return BridgeManager.findOrCreate(this.flutterView, channelName).buildBridge()
}

inline fun FlutterView.Provider.expose(channelName: String, annotatedElement: Any) {
    BridgeManager.findOrCreate(this.flutterView, channelName).expose(annotatedElement)
}

inline fun <T : Any> T.exposeTo(channelName: String, flutterView: FlutterView) : T {
    if(this !is FlutterView && this !is BinaryMessenger) {
        BridgeManager.findOrCreate(flutterView, channelName).expose(this)
    }
    return this
}

inline fun <T : Any> T.exposeTo(channelName: String, flutterViewProvider: FlutterView.Provider) : T {
    return this.exposeTo(channelName, flutterViewProvider.flutterView)
}

inline fun <T : Any> T.exposeTo(channelName: String, binaryMessenger: BinaryMessenger) : T {
    if(this !is FlutterView && this !is BinaryMessenger) {
        BridgeManager.findOrCreate(binaryMessenger, channelName).expose(this)
    }
    return this
}

inline fun <reified T> FlutterActivity.flutterRetrieve(channelName: String) = lazy { retrieve<T>(channelName) }
inline fun <reified T> BinaryMessenger.flutterRetrieve(channelName: String) = lazy { retrieve<T>(channelName) }
