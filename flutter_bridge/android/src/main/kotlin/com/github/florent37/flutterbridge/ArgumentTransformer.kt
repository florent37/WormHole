package com.github.florent37.flutterbridge

import android.util.Log
import com.github.florent37.flutterbridge.reflection.parameterDoesNotRequireUnwrapping
import java.lang.reflect.Type

class ArgumentTransformer(val jsonSerialisation: JsonSerialisation) {
    fun transformArgToSend(element: Any?): Any? {
        return element?.let { element ->
            return if (element::class.java.parameterDoesNotRequireUnwrapping()) {
                element
            } else {
                jsonSerialisation.serialize(element)
            }
        }
    }

    fun transformeReceived(
        element: Any?,
        actualTypeArgumentType: Type? = null,
        actualParameterClass: Class<*>?
    ): Any? {
        try {
            return element?.let { parameter ->
                when {
                    actualParameterClass != null && actualParameterClass.parameterDoesNotRequireUnwrapping() -> {
                        Log.e("ArgumentTransformer", "element class parameterDoesNotRequireUnwrapping")
                        when {
                            Int::class.java.isAssignableFrom(actualParameterClass) -> (parameter as Number).toInt()
                            Float::class.java.isAssignableFrom(actualParameterClass) -> (parameter as Number).toFloat()
                            Double::class.java.isAssignableFrom(actualParameterClass) -> (parameter as Number).toDouble()
                            else -> parameter
                        }
                    }
                    actualTypeArgumentType != null && actualTypeArgumentType.parameterDoesNotRequireUnwrapping() -> {
                        Log.e("ArgumentTransformer", "element type parameterDoesNotRequireUnwrapping")
                        when {
                            Int::class.java.isAssignableFrom(actualParameterClass) -> (parameter as Number).toInt()
                            Float::class.java.isAssignableFrom(actualParameterClass) -> (parameter as Number).toFloat()
                            Double::class.java.isAssignableFrom(actualParameterClass) -> (parameter as Number).toDouble()
                            else -> parameter
                        }
                    }
                    actualTypeArgumentType != null && parameter is Map<*, *> -> {
                        Log.e("ArgumentTransformer", "element is Map")
                        jsonSerialisation.deserialize(
                                actualTypeArgumentType,
                                parameter as Map<String, *>
                        )
                    }
                    actualTypeArgumentType != null && parameter is Collection<*> -> {
                        Log.e("ArgumentTransformer", "element is Collection")
                        jsonSerialisation.deserialize(
                                actualTypeArgumentType,
                                parameter
                        )
                    }
                    else -> {
                        Log.e("ArgumentTransformer", "element is not one of above")
                        null
                    }
                }
            }
        } catch (t: Throwable){
            Log.e("ArgumentTransformer", "cannot transform $element", t)
            return null
        }
    }
}