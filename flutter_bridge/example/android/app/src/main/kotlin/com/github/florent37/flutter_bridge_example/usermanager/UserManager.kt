package com.github.florent37.flutter_bridge_example.usermanager

import android.content.Context
import com.github.florent37.flutterbridge.annotations.flutter.Expose
import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

@ExperimentalCoroutinesApi
@FlowPreview
class UserManager(val context: Context) {

    companion object {
        const val USER = "user"
    }

    private val gson = Gson()
    private val sharedPreferences = context.getSharedPreferences("user_shared", Context.MODE_PRIVATE)
    private val userChannel = ConflatedBroadcastChannel<User?>()

    init {
        updateUser()
    }

    private fun updateUser() {
        val currentUser = sharedPreferences.getString(USER, null)?.let {
            gson.fromJson(it, User::class.java)
        }
        userChannel.offer(currentUser)
    }

    @Expose("getUser")
    fun getUser(): Flow<User?> = userChannel.asFlow()

    @Expose("saveUser")
    fun saveUser(user: User) {
        sharedPreferences.edit().putString(USER, gson.toJson(user)).apply()
        updateUser()
    }

    @Expose("clear")
    fun clear() {
        sharedPreferences.edit().remove(USER).apply()
        updateUser()
    }

}
