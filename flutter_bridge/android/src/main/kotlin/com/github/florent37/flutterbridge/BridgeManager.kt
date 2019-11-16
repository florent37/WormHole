package com.github.florent37.flutterbridge

import android.util.Log
import com.github.florent37.flutterbridge.annotations.BridgeAnnotationHandler
import com.github.florent37.flutterbridge.annotations.flutter.FlutterBridgeAnnotationHandler
import com.github.florent37.flutterbridge.deferred.WaitingCall
import com.github.florent37.flutterbridge.deferred.awaitWithContinuation
import com.github.florent37.flutterbridge.reflection.*
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import java.lang.ref.WeakReference
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.lang.reflect.Type
import kotlin.coroutines.suspendCoroutine


@Suppress("UNCHECKED_CAST")
@FlowPreview
class BridgeManager(
    private val channel: MethodChannel,
    val annotationHandler: BridgeAnnotationHandler = FlutterBridgeAnnotationHandler(),
    val jsonSerialisation: JsonSerialisation = SerialisationUtilsGSON
) {

    companion object {
        const val TAG = "BridgeManager"

        private val bridgesManagers = mutableMapOf<String, WeakReference<BridgeManager>>()

        fun findOrCreate(binaryMessenger: BinaryMessenger, channelName: String): BridgeManager {
            val channel = bridgesManagers[channelName]?.get()
            return when {
                channel != null -> channel
                else -> {
                    val newManager = BridgeManager(binaryMessenger, channelName)
                    bridgesManagers[channelName] = WeakReference(newManager)
                    newManager
                }
            }
        }
    }

    private constructor(binaryMessenger: BinaryMessenger, identifier: String) : this(
        MethodChannel(
            binaryMessenger,
            identifier
        )
    )

    private val coroutineJob = SupervisorJob()
    private val coroutineViewScope = CoroutineScope(context = coroutineJob + Dispatchers.Main)
    private val argumentTransformer = ArgumentTransformer(jsonSerialisation)

    init {
        channel.setMethodCallHandler { methodCall, result ->
            onMethodCalled(methodCall.method, methodCall.arguments, result)
        }
    }

    private var bridges = mutableListOf<WeakReference<Any>>()
    private val waitingCalls = mutableMapOf<String, MutableList<WaitingCall<*>>>()

    val output = Channel<FlutterEvent>()

    private fun onMethodCalled(
        name: String,
        arg: Any?,
        result: MethodChannel.Result
    ) {

        Log.d(TAG, "onMethodCalled $name $arg")

        waitingCalls[name]?.forEach {
            it.complete(arg)
        }
        //clear those methods calls
        waitingCalls.remove(name)

        coroutineViewScope.launch {
            //refacto from activity.lifecycle
            output.send(
                FlutterEvent(
                    method = name,
                    argument = arg
                )
            )
        }

        bridges.forEachSafety {
            it.callMethodOnObject(methodName = name, arg = arg, channelResult = result)
        }
    }

    fun <T> buildBridge(protocol: Class<T>): T {
        val proxy = Proxy.newProxyInstance(
            protocol.classLoader,
            arrayOf<Class<*>>(protocol),
            GeneratedProxyInvocationHandler()
        )
        return proxy as T
    }

    inline fun <reified T> buildBridge(): T {
        return buildBridge(T::class.java)
    }

    /**
     * Binds @Expose instance call with flutter methods
     */
    fun expose(annotatedElement: Any) {
        bridges.addSafety(annotatedElement)
    }

    private fun sendMessage(method: String, arg: Any) {
        val transformArgToSend = argumentTransformer.transformArgToSend(arg)
        channel.invokeMethod(method, transformArgToSend)
    }

    private fun sendMessage(method: String, arg: Any?, result: (Any?, Throwable?) -> Unit) {
        val transformArgToSend = argumentTransformer.transformArgToSend(arg)
        channel.invokeMethod(method, transformArgToSend, object : MethodChannel.Result {
            override fun notImplemented() {
                result(null, NotImplementedError())
            }

            override fun error(p0: String?, p1: String?, p2: Any?) {
                result(null, BridgeError("$p0 $p1, $p2"))
            }

            override fun success(p0: Any?) {
                result(p0, null)
            }
        })
    }

    private fun <T> waitMethodCallAsync(returnType: Type, methodName: String): Deferred<T> {
        val deferred = CompletableDeferred<T>()

        deferred.invokeOnCompletion {
            if (deferred.isCancelled) {
                waitingCalls.remove(methodName)
            }
        }

        var waitingCallsForName = waitingCalls[methodName]
        if (waitingCallsForName == null) {
            waitingCallsForName = mutableListOf()
            waitingCalls[methodName] = waitingCallsForName
        }
        waitingCallsForName.add(
            WaitingCall(
                methodName,
                returnType,
                argumentTransformer,
                deferred
            )
        )

        return deferred
    }

    operator fun invoke(methodName: String, params: Any) {
        sendMessage(methodName, params)
    }

    fun close() {
        coroutineViewScope.cancel()
    }

    @FlowPreview
    private inner class GeneratedProxyInvocationHandler : InvocationHandler {

        fun handleFrom(method: Method, args: Array<Any>?): Any? {
            var methodName =
                annotationHandler.getFromAnnotationValue(method.getAnnotation(annotationHandler.fromAnnotation))
            if (methodName.isBlank()) {
                methodName = method.name
            }

            return when {
                method.isSuspendFunction() -> {
                    if (method.isSuspendFlowFunction()) {
                        val actualTypeArgument = method.continuationFlowType()

                        return output
                            .consumeAsFlow()
                            .filter { it.method == methodName }
                            .map {
                                argumentTransformer.transformeReceived(it, actualTypeArgument, null)
                            }
                    } else {
                        val continuation = args!!.getContinuation()
                        val actualTypeArgument = method.continuationType()
                        val deferred = waitMethodCallAsync<Any>(actualTypeArgument, methodName)
                        deferred.awaitWithContinuation(continuation)
                    }
                }
                method.returnType == Deferred::class.java -> {
                    val actualTypeArgument = method.findReturnType()
                    waitMethodCallAsync<Any>(actualTypeArgument, methodName)
                }
                else -> throw BridgeError("FromFlutter on a bridge must return a suspend / Deferred")
            }
        }

        fun handleTo(method: Method, args: Array<Any>?): Any? {
            var methodName =
                annotationHandler.getToAnnotationValue(method.getAnnotation(annotationHandler.toAnnotation))
            if (methodName.isBlank()) {
                methodName = method.name
            }

            when {
                method.isSuspendFunction() -> {
                    val continuation = args!!.getContinuation()

                    val actualTypeArgument = method.continuationType()

                    //method without arg, but continuation
                    if (method.parameterTypes.size == 1) { //1 because of continuation
                        return sendMessageAndReturnDeffered(
                            methodName,
                            emptyArray(),
                            actualTypeArgument
                        ).awaitWithContinuation(continuation)
                    } else if (method.parameterTypes.size == 2) { //2 because of continuation
                        return sendMessageAndReturnDeffered(
                            methodName,
                            args,
                            actualTypeArgument
                        ).awaitWithContinuation(continuation)
                    } else {
                        throw BridgeError("ToFlutter only Works with 1 arg")
                    }
                }
                method.returnType == Deferred::class.java -> {
                    val actualTypeArgument = method.findReturnType()

                    //for now, only for 1 arg
                    if (method.parameterTypes.size == 1) {
                        return sendMessageAndReturnDeffered(methodName, args, actualTypeArgument)
                    } else {
                        throw BridgeError("ToFlutter only works with 1 arg")
                    }
                }
                else -> { //don't return something
                    //for now, only for 1 arg
                    if (method.parameterTypes.size == 1) {
                        sendMessage(methodName, args!![0])
                    } else {
                        throw BridgeError("ToFlutter only works with 1 arg")
                    }
                    return null
                }
            }
        }

        @Throws(Throwable::class)
        override fun invoke(proxy: Any, method: Method, args: Array<Any>?): Any? {

            // If the method is a method from Object then defer to normal invocation.
            if (method.declaringClass == Any::class.java) {
                return method.invoke(this, *(args!!))
            }

            return when {
                method.isAnnotationPresent(annotationHandler.fromAnnotation) -> handleFrom(
                    method,
                    args
                )
                method.isAnnotationPresent(annotationHandler.toAnnotation) -> handleTo(
                    method,
                    args
                )
                else -> null
            }
        }

    }

    private fun sendMessageAndReturnDeffered(
        methodName: String,
        args: Array<Any>?,
        actualTypeArgument: Type
    ): CompletableDeferred<Any?> {
        val completableDeferred = CompletableDeferred<Any?>()
        val arg = args?.getOrNull(0)
        sendMessage(methodName, arg) { result, error ->
            if (error != null) {
                completableDeferred.completeExceptionally(error)
            } else {
                completableDeferred.complete(argumentTransformer.transformeReceived(result, actualTypeArgument, null))
            }
        }
        return completableDeferred
    }

    /**
     * Used for bind / expose annotations
     */
    private fun Any.callMethodOnObject(
        methodName: String,
        arg: Any?,
        channelResult: MethodChannel.Result
    ) {
        val element = this

        val methodsList = element.getBindAnnotatedMethodWithName(methodName, annotationHandler)
        for (method in methodsList) {
            try {
                /* only works with 0 or 1 arg (string)*/

                val suspendMethod = method.isSuspendFunction()
                val suspendArgCount = if (suspendMethod) 1 else 0

                //handle params
                val params: Array<Any?>

                val realParameterCount = method.parameterTypes.size - suspendArgCount;
                params = arrayOfNulls(realParameterCount) //for suspend & arg, 1 arg + continuation

                if (arg == null && (realParameterCount == 0)) {
                    //nothing to do here
                } else if (arg != null) {
                    //1 param -> do not need to transform to map
                    if (realParameterCount == 1) {
                        val parameterClass = method.parameterTypes[0]
                        val genericParameterType = method.genericParameterTypes[0]

                        params[0] = argumentTransformer.transformeReceived(arg, genericParameterType, parameterClass)
                    } else if (arg is Map<*, *>) {  //multiple params -> json to map then call
                        val map = arg as Map<String, *>
                        val parameterAnnotations = method.parameterAnnotations

                        //TODO handle only FlutterParameter
                        if (parameterAnnotations.size != method.genericParameterTypes.size) {
                            throw BridgeError("to handle multiple params for $methodName, please annotate your arguments with $ ")
                        }

                        val parameterName = method.parametersNames(annotationHandler)

                        for (index in 0 until realParameterCount) {
                            val thisParameterName = parameterName[index]
                            val parameter = map[thisParameterName]
                            val parameterClass = method.parameterTypes[index]
                            val genericParameterType = method.genericParameterTypes[index]

                            params[index] = argumentTransformer.transformeReceived(parameter, genericParameterType, parameterClass)
                        }
                    }

                } else {
                    Log.e(TAG, "binds methods can only have 0 or 1 argument (String/json)")
                    continue
                }

                //handle return
                if (suspendMethod) {
                    coroutineViewScope.launch {
                        var handledDirectlyByMethod = false
                        val methodResult = suspendCoroutine<Any?> { continuation ->
                            //add the continuation
                            val paramsWithContinuation = arrayOfNulls<Any>(params.size + 1)

                            params.forEachIndexed { index, element ->
                                paramsWithContinuation[index] = element
                            }
                            paramsWithContinuation[paramsWithContinuation.size - 1] = continuation

                            Log.d(TAG, "onMethodCalled invoke suspend method on ${element::class.java} $params")

                            //found the method
                            val result = method.invoke(element, *paramsWithContinuation)
                            if(result != kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED) {
                                Log.d(TAG, "onMethodCalled invoked on ${element::class.java} $result")

                                Log.d(TAG, "handleCallMethodOnObjectReturn")
                                handleCallMethodOnObjectReturn(
                                        element = element,
                                        method = method,
                                        methodResult = result,
                                        channelResult = channelResult
                                )

                                handledDirectlyByMethod = true
                                 //close
                            }
                        }

                        if(!handledDirectlyByMethod) {
                            Log.d(TAG, "!handledDirectlyByMethod handleCallMethodOnObjectReturn")

                            handleCallMethodOnObjectReturn(
                                    element = element,
                                    method = method,
                                    methodResult = methodResult,
                                    channelResult = channelResult
                            )
                        }

                    }
                } else {
                    val methodResult = method.invoke(element, *params)
                    handleCallMethodOnObjectReturn(
                        element = element,
                        method = method,
                        methodResult = methodResult,
                        channelResult = channelResult
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "callMethodOnObject error", e)
            }

        }
    }

    private fun handleCallMethodOnObjectReturn(
        element: Any,
        method: Method,
        methodResult: Any?,
        channelResult: MethodChannel.Result
    ) {
        when {
            method.isVoid() -> return
            else -> { //simple object
                val valueToSend = argumentTransformer.transformArgToSend(methodResult)
                Log.d(TAG, "valueToSend : $valueToSend")
                channelResult.success(valueToSend)
            }
        }
    }

    /*
    inline fun <reified T> readOutput(methodName: String): Flow<T> =
        output
            .consumeAsFlow()
            .filter { it.method == methodName }
            .map {
                val json = it.argument as String
                jsonSerialisation.deserialize(T::class.java, json) as T
            }

    suspend inline fun <reified T> waitOutput(methodName: String): T =
        output
            .consumeAsFlow()
            .filter { it.method == methodName }
            .map {
                val json = it.argument as String
                jsonSerialisation.deserialize(T::class.java, json) as T
            }
            .first()
*/
}