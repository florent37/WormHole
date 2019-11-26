package com.github.florent37.wormhole_example

import android.os.Bundle
import android.util.Log
import com.github.florent37.wormhole_example.usermanager.UserManager
import com.github.florent37.wormhole.expose
import com.github.florent37.wormhole.retrieve
import io.flutter.app.FlutterActivity
import io.flutter.plugins.GeneratedPluginRegistrant
import kotlinx.coroutines.*


class MainActivity : FlutterActivity() {

    private val userManager by lazy { UserManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GeneratedPluginRegistrant.registerWith(this)

        /**
         * Expose the user manager to be accessible to Flutter via a WormHole
         */
        expose("user", userManager)
    }
}
