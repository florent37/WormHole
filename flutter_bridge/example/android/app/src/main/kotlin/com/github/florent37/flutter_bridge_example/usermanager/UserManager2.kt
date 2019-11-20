package com.github.florent37.flutter_bridge_example.usermanager

import com.github.florent37.flutterbridge.annotations.flutter.Call

interface UserManager2 {

    @Call("user")
    suspend fun getUserName(age: Int) : String
}