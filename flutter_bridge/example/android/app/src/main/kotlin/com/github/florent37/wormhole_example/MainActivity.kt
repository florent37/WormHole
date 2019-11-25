package com.github.florent37.wormhole_example

import android.os.Bundle
import android.util.Log
import com.github.florent37.wormhole_example.usermanager.UserManager
import com.github.florent37.wormhole_example.usermanager.UserManager2
import com.github.florent37.wormhole.expose
import com.github.florent37.wormhole.flutterRetrieve
import io.flutter.app.FlutterActivity
import io.flutter.plugins.GeneratedPluginRegistrant
import kotlinx.coroutines.*


class MainActivity : FlutterActivity(), CoroutineScope by MainScope() {

    private val userManager by lazy { UserManager(this) }
    private val userBridge2 by flutterRetrieve<UserManager2>("user2")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GeneratedPluginRegistrant.registerWith(this)

        expose("user", userManager)

        launch {
            val name = userBridge2.getUserName(18)
            Log.d("MainActivity", "$name")
        }
    }

    override fun onDestroy() {
        cancel()
        super.onDestroy()
    }
}
