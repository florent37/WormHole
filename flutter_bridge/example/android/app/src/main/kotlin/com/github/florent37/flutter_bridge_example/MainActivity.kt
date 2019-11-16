package com.github.florent37.flutter_bridge_example

import android.os.Bundle
import com.github.florent37.flutter_bridge_example.usermanager.UserManager
import com.github.florent37.flutter_bridge_example.usermanager.UserManagerShared
import com.github.florent37.flutterbridge.expose

import io.flutter.app.FlutterActivity
import io.flutter.plugins.GeneratedPluginRegistrant

class MainActivity: FlutterActivity() {

  private val userManager : UserManager by lazy { UserManagerShared(this) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    GeneratedPluginRegistrant.registerWith(this)

    expose("user", userManager)
  }
}
