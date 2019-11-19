package com.github.florent37.flutterbridge

import android.util.Log
import androidx.annotation.VisibleForTesting
import com.github.florent37.flutterbridge.annotations.BridgeAnnotationHandler
import com.github.florent37.flutterbridge.annotations.flutter.FlutterBridgeAnnotationHandler
import com.github.florent37.flutterbridge.deferred.WaitingCall
import com.github.florent37.flutterbridge.deferred.awaitWithContinuation
import com.github.florent37.flutterbridge.reflection.*
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.lang.ref.WeakReference
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.lang.reflect.Type


@Suppress("UNCHECKED_CAST")
@FlowPreview
class BridgeManager(
        private val channelName: String,
        private val binaryMessenger: BinaryMessenger,
        private val channel: MethodChannel,
        private val annotationHandler: BridgeAnnotationHandler = FlutterBridgeAnnotationHandler(),
        private val jsonSerialisation: JsonSerialisation = SerialisationUtilsGSON
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
            identifier,
            binaryMessenger,
            MethodChannel(
                    binaryMessenger,
                    identifier
            )
    )

    private val coroutineJob = SupervisorJob()
    private val coroutineViewScope = CoroutineScope(context = coroutineJob + Dispatchers.Main)
    private val argumentTransformer = ArgumentTransformer(jsonSerialisation)
    private val eventChannels = mutableMapOf<String, EventChannel>()

    init {
        channel.setMethodCallHandler { methodCall, result ->
            onMethodCalled(methodCall.method, methodCall.arguments, result)
        }
    }

    private var bridges = mutableListOf<WeakReference<Any>>()
    private val waitingCalls = mutableMapOf<String, MutableList<WaitingCall<*>>>()

    val output = Channel<FlutterEvent>()

    @VisibleForTesting
    internal fun onMethodCalled(
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
        handleBindingFlows(annotatedElement)
    }

    private fun handleBindingFlows(annotatedElement: Any) {
        val bindingMethodReturningFlow = annotatedElement.findBindingMethodReturningFlow(annotationHandler)
        bindingMethodReturningFlow.forEach { method ->
            val channelName = method.getBindAnnotationName(annotationHandler) ?: method.name
            handleBindingChannel(annotatedElement, method, channelName)
        }
    }

    private fun handleBindingChannel(annotatedElement: Any, method: Method, name: String) {
        //1. create the EventChannel
        val eventChannelName = "$channelName/$name"
        val eventChannel = eventChannels.getOrPut(eventChannelName) {
            EventChannel(binaryMessenger, eventChannelName)
        }
        //2. register with the method
        eventChannel.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arg: Any?, eventSink: EventChannel.EventSink?) {
                //handle params
                val params = argumentTransformer.createArgumentsToCallMethod(
                        method = method,
                        annotationHandler = annotationHandler,
                        receivedArg = arg
                )
                coroutineViewScope.launch {
                    val methodResult = annotatedElement.invokeSuspend(method, params)
                    if (methodResult is Flow<*>) {
                        Log.d(TAG, "Flow/handleBindingChannel $methodResult")
                        methodResult.collect {
                            val valueToSend = argumentTransformer.transformArgToSend(it)
                            Log.d(TAG, "Flow/sendValue : $valueToSend")
                            eventSink?.success(valueToSend)
                        }
                    }
                }
            }

            override fun onCancel(arg: Any?) {

            }
        })
    }

    private fun sendMessage(method: String, arg: Any) {
        val transformArgToSend = argumentTransformer.transformArgToSend(arg)
        channel.invokeMethod(method, transformArgToSend)
    }

    private fun sendMessage(method: String, arg: Any?, result: (Any?, Throwable?) -> Unit) {
        val transformArgToSend = argumentTransformer.transformArgToSend(arg)
        channel.invokeMethod(method, transformArgToSend, object : MethodChannel.Result {
            override fun notImplemented() {
                Log.e(TAG, "$method is not implemented")
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
                        method.continuationType()?.let { actualTypeArgument ->
                            val deferred = waitMethodCallAsync<Any>(actualTypeArgument, methodName)
                            deferred.awaitWithContinuation(continuation)
                        }

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
                                actualTypeArgument!!
                        ).awaitWithContinuation(continuation)
                    } else if (method.parameterTypes.size == 2) { //2 because of continuation
                        return sendMessageAndReturnDeffered(
                                methodName,
                                args,
                                actualTypeArgument!!
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
                val transformed = argumentTransformer.transformeReceived(result, actualTypeArgument, null)
                completableDeferred.complete(transformed)
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
            if (!method.isSuspendFlowFunction()) { //Flow is handle by `handleBindingsFlow`
                val params = try {
                    //handle params
                    argumentTransformer.createArgumentsToCallMethod(
                            method = method,
                            annotationHandler = annotationHandler,
                            receivedArg = arg
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "callMethodOnObject error while parsing parameters", e)
                    continue
                }

                coroutineViewScope.launch {
                    //handle return
                    try {
                        val methodResult = element.invokeSuspend(method, params)
                        Log.d(TAG, "callMethodOnObject $methodResult")
                        handleCallMethodOnObjectReturn(
                                element = element,
                                method = method,
                                methodResult = methodResult,
                                channelResult = channelResult
                        )

                    } catch (e: Exception) {
                        Log.e(TAG, "callMethodOnObject error while invokating ${method.name}", e)
                    }
                }
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
