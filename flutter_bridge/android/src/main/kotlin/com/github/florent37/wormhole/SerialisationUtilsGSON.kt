package com.github.florent37.wormhole

import com.github.florent37.wormhole.gson.CustomizedObjectTypeAdapter
import com.google.gson.*
import com.google.gson.reflect.TypeToken

import java.lang.reflect.Type

interface JsonSerialisation {

    fun serialize(obj: Any): Map<String, *>

    fun deserialize(className: Class<*>, map:  Map<String, *>): Any

    fun deserialize(type: Type, map:  Map<String, *>): Any

    //fun deserialize(type: Type, map: Map<*, *>): Any

    fun deserialize(type: Type, collection: Collection<*>): Any
}

object SerialisationUtilsGSON : JsonSerialisation {

    /**
     * this adapter prevent integer to be parsed to double
     * @see CustomizedObjectTypeAdapter
     */
    val adapter = CustomizedObjectTypeAdapter()
    private var gson = GsonBuilder()
            .registerTypeAdapter(Map::class.java, adapter)
            .registerTypeAdapter(List::class.java, adapter)
            .setPrettyPrinting().create()

    private fun Any.asJsonString() : String {
        return gson.toJson(this)
    }

    override fun serialize(obj: Any): Map<String, *> {
        val jsonString =  gson.toJson(obj)
        return gson.fromJson(jsonString, Map::class.java) as Map<String, *>
    }

    override fun deserialize(className: Class<*>, map:  Map<String, *>): Any {
        //1. convert the map to string
        val jsonString = map.asJsonString()
        //2. convert deserialize the string
        return gson.fromJson<Any>(jsonString, className)
    }

    override fun deserialize(type: Type, map: Map<String, *>): Any {
        //1. convert the map to string
        val jsonString = map.asJsonString()
        //2. convert deserialize the string
        return gson.fromJson<Any>(jsonString, TypeToken.get(type).type)
    }

    override fun deserialize(type: Type, collection: Collection<*>): Any {
        //1. convert the map to string
        val jsonString = collection.asJsonString()
        //2. convert deserialize the string
        return gson.fromJson<Any>(jsonString, TypeToken.get(type).type)
    }
}