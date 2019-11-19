package com.github.florent37.flutter_bridge_example

import android.os.Bundle
import android.util.Log
import com.github.florent37.flutter_bridge_example.usermanager.UserManager
import com.github.florent37.flutter_bridge_example.usermanager.UserManager2
import com.github.florent37.flutterbridge.buildBridge
import com.github.florent37.flutterbridge.expose
import com.github.florent37.flutterbridge.flutterBridge
import io.flutter.app.FlutterActivity
import io.flutter.plugins.GeneratedPluginRegistrant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch



class MainActivity : FlutterActivity(), CoroutineScope by MainScope() {

    private val userManager by lazy { UserManager(this) }
    private val userBridge2 by flutterBridge<UserManager2>("user2")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GeneratedPluginRegistrant.registerWith(this)

        expose("user", userManager)

        launch {
            val name = userBridge2.getUserName()
            Log.d("MainActivity", "$name")
        }
    }

    override fun onDestroy() {
        cancel()
        super.onDestroy()
    }
}
