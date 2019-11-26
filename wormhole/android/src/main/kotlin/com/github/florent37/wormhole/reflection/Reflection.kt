package com.github.florent37.wormhole.reflection

import android.util.Log
import com.github.florent37.wormhole.BridgeManager
import com.github.florent37.wormhole.annotations.BridgeAnnotationHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import kotlin.coroutines.Continuation
import kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.resume

internal fun Method.hasBindAnnotatedMethodAnnotationWithName(
        name: String,
        annotationHandler: BridgeAnnotationHandler
): Boolean {
    return this.isAnnotationPresent(annotationHandler.bindAnnotation) && annotationHandler.getBindAnnotationValue(
            this.getAnnotation(annotationHandler.bindAnnotation)
    ) == name
}

internal fun Method.hasBindAnnotatedMethodAnnotationReturningFlow(
        annotationHandler: BridgeAnnotationHandler
): Boolean {
    return this.isAnnotationPresent(annotationHandler.bindAnnotation) && this.isSuspendFlowFunction()
}

internal fun Method.getBindAnnotationName(
        annotationHandler: BridgeAnnotationHandler
): String? {
    return annotations.firstOrNull { annotationHandler.bindAnnotation.isAssignableFrom(it::class.java) }?.let {
        annotationHandler.getBindAnnotationValue(it)
    }
}

private fun Any.classToLookupForReflection(): List<Class<*>> {
    val allClasses = mutableListOf<Class<*>>()
    val javaClass = this.javaClass

    allClasses.add(javaClass)

    var superClass = javaClass.superclass
    while (superClass != null) {
        allClasses.add(superClass)
        superClass = superClass.superclass
    }

    allClasses.addAll(javaClass.interfaces)

    return allClasses
}

internal fun Any.getBindAnnotatedMethodWithName(
        name: String,
        annotationHandler: BridgeAnnotationHandler
): List<Method> {
    val allMethods = mutableListOf<Method>()
    this.classToLookupForReflection().forEach { inheritedClass ->
        allMethods.addAll(inheritedClass.declaredMethods.filter {
            it.hasBindAnnotatedMethodAnnotationWithName(name, annotationHandler)
        })
    }
    return allMethods
}

internal fun Any.findBindingMethodReturningFlow(
        annotationHandler: BridgeAnnotationHandler
): List<Method> {
    val allMethods = mutableListOf<Method>()
    this.classToLookupForReflection().forEach { inheritedClass ->
        allMethods.addAll(inheritedClass.declaredMethods.filter {
            it.hasBindAnnotatedMethodAnnotationReturningFlow(annotationHandler)
        })
    }
    return allMethods
}

fun Method.continuationType(): Type? {
    val method = this
    return (((method.genericParameterTypes.lastOrNull()) as? ParameterizedType)?.actualTypeArguments?.getOrNull(0) as? WildcardType)?.lowerBounds?.getOrNull(0)
}

fun Method.continuationFlowType(): Type? {
    val method = this
    val flowType = method.continuationType()
    return ((flowType as? ParameterizedType))?.actualTypeArguments?.getOrNull(0)
}

fun Method.findReturnType(): Type {
    val method = this
    return (method.genericReturnType as ParameterizedType).actualTypeArguments[0]
}

fun Method.isSuspendFunction(): Boolean {
    val method = this
    return method.parameterTypes.lastOrNull()?.let { Continuation::class.java.isAssignableFrom(it) }
            ?: false
}

fun Method.isSuspendFlowFunction(): Boolean {
    val method = this
    val hasLastArgFlow = (((method.continuationType() as? ParameterizedType)?.rawType) as? Class<*>)?.let {
        Flow::class.java.isAssignableFrom(it)
    } ?: false
    val returnAFlow = method.returnType?.let { Flow::class.java.isAssignableFrom(it) } ?: false
    return hasLastArgFlow || returnAFlow
}

fun Array<Any>.getContinuation() = this.last() as Continuation<Any?>

fun Method.isVoid(): Boolean {
    return this.returnType.name == "void"
}

fun Class<*>.parameterRequireUnwrapping() = !this.parameterDoesNotRequireUnwrapping()

fun Class<*>.parameterDoesNotRequireUnwrapping(): Boolean {
    val parameterClass = this
    return parameterClass.isPrimitive || String::class.java.isAssignableFrom(parameterClass) || Number::class.java.isAssignableFrom(parameterClass)
}

fun Method.parametersNames(annotationHandler: BridgeAnnotationHandler): List<String> {
    val names = mutableListOf<String>()
    this.parameterAnnotations.forEach { annots ->
        annots.forEach { annot ->
            if (annotationHandler.argumentAnnotation.isInstance(annot)) {
                val name = annotationHandler.getArgumentAnnotationValue(annot)
                names.add(name)
            }
        }
    }
    return names
}

fun Type.parameterDoesNotRequireUnwrapping(): Boolean = (this as? Class<*>)?.let {
    when {
        Int::class.java.isAssignableFrom(it) -> true
        Float::class.java.isAssignableFrom(it) -> true
        Long::class.java.isAssignableFrom(it) -> true
        Double::class.java.isAssignableFrom(it) -> true
        String::class.java.isAssignableFrom(it) -> true
        else -> false
    }
} ?: false

fun Array<Any?>.addingContinuation(continuation: Continuation<Any?>): Array<Any?> {
    val newArray = arrayOfNulls<Any?>(this.size + 1)

    this.forEachIndexed { index, element ->
        newArray[index] = element
    }

    newArray[newArray.size - 1] = continuation
    return newArray
}

suspend fun Any.invokeSuspend(method: Method, params: Array<Any?>): Any? {
    val element = this
    return if (method.isSuspendFunction()) {
        suspendCancellableCoroutine { continuation ->
            //found the method
            val result = method.invoke(element, *params.addingContinuation(continuation))
            if (result != COROUTINE_SUSPENDED) {
                continuation.resume(result)
            }
            Log.d(BridgeManager.TAG, "callMethodOnObject $result")
        }
    } else {
        method.invoke(element, *params)
    }
}