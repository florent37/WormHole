package com.github.florent37.wormhole

import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar

class WormHolePlugin: MethodCallHandler {
  override fun onMethodCall(p0: MethodCall, p1: Result) {
    //nothing to do here
  }

  companion object {
    @JvmStatic
    fun registerWith(registrar: Registrar) {
        /* nothing to do here
      val channel = MethodChannel(registrar.messenger(), "flutter_bridge")
      channel.setMethodCallHandler(FlutterBridgePlugin())
         */
    }
  }
}