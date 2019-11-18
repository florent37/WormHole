package com.github.florent37.flutter_bridge_example.usermanager

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.internal.ChannelFlow

@ExperimentalCoroutinesApi
@FlowPreview
class UserManagerShared(val context: Context) : UserManager {

    companion object {
        const val USER = "user"
    }


    private val gson = Gson()
    private val sharedPreferences = context.getSharedPreferences("user_shared", Context.MODE_PRIVATE)
    private val userChannel = ConflatedBroadcastChannel<User?>()

    init {
        updateUser()
    }

    private fun updateUser(){
        val currentUser = sharedPreferences.getString(USER, null)?.let {
            gson.fromJson(it, User::class.java)
        }
        userChannel.offer(currentUser)
    }

    override suspend fun getUser(): Flow<User?> = userChannel.asFlow()

    override suspend fun saveUser(user: User) {
        sharedPreferences.edit().putString(USER, gson.toJson(user)).apply()
        updateUser()
    }

    override suspend fun clear() {
        sharedPreferences.edit().remove(USER).apply()
        updateUser()
    }

}
