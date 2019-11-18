package com.github.florent37.flutter_bridge_example.usermanager

import com.github.florent37.flutterbridge.annotations.flutter.FlutterAnnotations.*
import kotlinx.coroutines.flow.Flow

interface UserManager {

    @Expose("getUser")
    suspend fun getUser(): Flow<User?>

    @Expose("saveUser")
    suspend fun saveUser(@Param("user") user: User)

    @Expose("clear")
    suspend fun clear()
}
