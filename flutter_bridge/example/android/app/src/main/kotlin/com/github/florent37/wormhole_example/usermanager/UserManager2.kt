package com.github.florent37.wormhole_example.usermanager

import com.github.florent37.wormhole.annotations.flutter.Call

interface UserManager2 {

    @Call("user")
    suspend fun getUserName(age: Int) : String
}