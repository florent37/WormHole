package com.github.florent37.wormhole

import android.util.Log
import com.github.florent37.wormhole.annotations.BridgeAnnotationHandler
import com.github.florent37.wormhole.reflection.isSuspendFunction
import com.github.florent37.wormhole.reflection.parameterDoesNotRequireUnwrapping
import com.github.florent37.wormhole.reflection.parametersNames
import java.lang.reflect.Method
import java.lang.reflect.Type

class ArgumentTransformer(val jsonSerialisation: JsonSerialisation) {
    fun transformArgToSend(element: Any?): Any? {
        return element?.let {
            return if (it::class.java.parameterDoesNotRequireUnwrapping()) {
                it
            } else {
                jsonSerialisation.serialize(it)
            }
        }
    }

    fun createArgumentsToCallMethod(method: Method, receivedArg: Any?, annotationHandler: BridgeAnnotationHandler): Array<Any?> {
        val suspendMethod = method.isSuspendFunction()
        val suspendArgCount = if (suspendMethod) 1 else 0

        //handle params
        val params: Array<Any?>

        val realParameterCount = method.parameterTypes.size - suspendArgCount;
        params = arrayOfNulls(realParameterCount) //for suspend & arg, 1 arg + continuation

        if (receivedArg == null && (realParameterCount == 0)) {
            //nothing to do here
        } else if (receivedArg != null) {
            //1 param -> do not need to transform to map
            if (realParameterCount == 1) {
                val parameterClass = method.parameterTypes[0]
                val genericParameterType = method.genericParameterTypes[0]

                params[0] = this.transformeReceived(receivedArg, genericParameterType, parameterClass)
            } else if (receivedArg is Map<*, *>) {  //multiple params -> json to map then call
                val map = receivedArg as Map<String, *>
                val parameterAnnotations = method.parameterAnnotations

                //TODO handle only FlutterParameter
                if (parameterAnnotations.size != method.genericParameterTypes.size) {
                    throw BridgeError("to handle multiple params for ${method.name}, please annotate your arguments with $ ")
                }

                val parameterName = method.parametersNames(annotationHandler)

                for (index in 0 until realParameterCount) {
                    val thisParameterName = parameterName[index]
                    val parameter = map[thisParameterName]
                    val parameterClass = method.parameterTypes[index]
                    val genericParameterType = method.genericParameterTypes[index]

                    params[index] = this.transformeReceived(parameter, genericParameterType, parameterClass)
                }
            }

        } else {
            Log.e(BridgeManager.TAG, "binds methods can only have 0 or 1 argument (String/json)")
        }
        
        return params
    }

    fun transformeReceived(
        element: Any?,
        actualTypeArgumentType: Type? = null,
        actualParameterClass: Class<*>?
    ): Any? {
        try {
            return element?.let { parameter ->
                when {
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
                    actualParameterClass != null && actualParameterClass.parameterDoesNotRequireUnwrapping() -> {
                        Log.e("ArgumentTransformer", "element class parameterDoesNotRequireUnwrapping")
                        when {
                            Int::class.java.isAssignableFrom(actualParameterClass) -> (parameter as Number).toInt()
                            Long::class.java.isAssignableFrom(actualParameterClass) -> (parameter as Number).toLong()
                            Float::class.java.isAssignableFrom(actualParameterClass) -> (parameter as Number).toFloat()
                            Double::class.java.isAssignableFrom(actualParameterClass) -> (parameter as Number).toDouble()
                            String::class.java.isAssignableFrom(actualParameterClass) -> parameter
                            else -> parameter
                        }
                    }
                    actualTypeArgumentType != null && actualTypeArgumentType.parameterDoesNotRequireUnwrapping() -> {
                        Log.e("ArgumentTransformer", "element type parameterDoesNotRequireUnwrapping")
                        (actualTypeArgumentType as? Class<*>)?.let {
                            when {
                                Int::class.java.isAssignableFrom(it) -> (parameter as Number).toInt()
                                Long::class.java.isAssignableFrom(it) -> (parameter as Number).toLong()
                                Float::class.java.isAssignableFrom(it) -> (parameter as Number).toFloat()
                                Double::class.java.isAssignableFrom(it) -> (parameter as Number).toDouble()
                                String::class.java.isAssignableFrom(it) -> parameter
                                else -> parameter
                            }
                        }
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