package com.github.florent37.flutter_bridge_example.usermanager

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.delay

class UserManagerShared(val context: Context) : UserManager {

    companion object {
        const val USER = "user"
    }

    private val gson = Gson()
    private val sharedPreferences =
        context.getSharedPreferences("user_shared", Context.MODE_PRIVATE)

    suspend override fun getUser(): User? {
        delay(1000)
        return sharedPreferences.getString(USER, null)?.let {
            gson.fromJson(it, User::class.java)
        }
    }


    override suspend fun saveUser(user: User) {
        sharedPreferences.edit().putString(USER, gson.toJson(user)).apply()
    }

    override suspend fun clear() {
        sharedPreferences.edit().remove(USER).apply()
    }

}
