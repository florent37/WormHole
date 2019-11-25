package com.github.florent37.wormhole_example

import android.os.Bundle
import android.util.Log
import com.github.florent37.wormhole_example.usermanager.UserManager
import com.github.florent37.wormhole_example.usermanager.UserManager2
import com.github.florent37.wormhole.expose
import com.github.florent37.wormhole.retrieve
import io.flutter.app.FlutterActivity
import io.flutter.plugins.GeneratedPluginRegistrant
import kotlinx.coroutines.*


class MainActivity : FlutterActivity(), CoroutineScope by MainScope() {

    private val userManager by lazy { UserManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GeneratedPluginRegistrant.registerWith(this)

        expose("user", userManager)
    }

    override fun onDestroy() {
        cancel()
        super.onDestroy()
    }
}
