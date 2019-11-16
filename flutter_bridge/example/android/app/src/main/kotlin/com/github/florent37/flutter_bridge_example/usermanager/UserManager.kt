package com.github.florent37.flutter_bridge_example.usermanager

import com.github.florent37.flutterbridge.annotations.flutter.FlutterAnnotations.*

interface UserManager {

    @Expose("getUser")
    suspend fun getUser(): User?

    @Expose("saveUser")
    suspend fun saveUser(@Param("user") user: User)

    @Expose("clear")
    suspend fun clear()
}
