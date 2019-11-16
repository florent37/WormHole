package com.github.florent37.flutterbridge.reflection

import com.github.florent37.flutterbridge.annotations.BridgeAnnotationHandler
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import kotlin.coroutines.Continuation

internal fun Method.hasBindAnnotatedMethodAnnotationWithName(
    name: String,
    annotationHandler: BridgeAnnotationHandler
): Boolean {
    return this.isAnnotationPresent(annotationHandler.bindAnnotation) && annotationHandler.getBindAnnotationValue(
        this.getAnnotation(annotationHandler.bindAnnotation)
    ) == name
}

internal fun Any.getBindAnnotatedMethodWithName(
    name: String,
    annotationHandler: BridgeAnnotationHandler
): List<Method> {
    val allMethods = mutableListOf<Method>()
    val javaClass = this.javaClass

    allMethods.addAll(javaClass.declaredMethods.filter {
        it.hasBindAnnotatedMethodAnnotationWithName(name, annotationHandler)
    })

    var superClass = javaClass.superclass
    while (superClass != null) {
        allMethods.addAll(superClass.declaredMethods.filter {
            it.hasBindAnnotatedMethodAnnotationWithName(name, annotationHandler)
        })
        superClass = superClass.superclass
    }

    javaClass.interfaces?.forEach { interf ->
        allMethods.addAll(interf.declaredMethods.filter {
            it.hasBindAnnotatedMethodAnnotationWithName(name, annotationHandler)
        })
    }

    return allMethods
}

fun Method.continuationType(): Type {
    val method = this
    return (((method.genericParameterTypes.lastOrNull()) as ParameterizedType).actualTypeArguments[0] as WildcardType).lowerBounds[0]
}

fun Method.continuationFlowType(): Type {
    val method = this
    val flowType = method.continuationType()
    return ((flowType as ParameterizedType)).actualTypeArguments[0]
}

fun Method.findReturnType(): Type {
    val method = this
    return (method.genericReturnType as ParameterizedType).actualTypeArguments[0]
}

fun Method.isSuspendFunction(): Boolean {
    val method = this
    return method.parameterTypes.lastOrNull()?.isAssignableFrom(Continuation::class.java) ?: false
}

fun Method.isSuspendFlowFunction(): Boolean {
    val method = this
    return (((method.continuationType() as? ParameterizedType)?.rawType) as? Class<*>)?.isAssignableFrom(
        kotlinx.coroutines.flow.Flow::class.java
    ) ?: false
}

fun Array<Any>.getContinuation() = this.last() as Continuation<Any?>

fun Method.isVoid(): Boolean {
    return this.returnType.name == "void"
}

fun Class<*>.parameterRequireUnwrapping() = !this.parameterDoesNotRequireUnwrapping()

fun Class<*>.parameterDoesNotRequireUnwrapping(): Boolean {
    val parameterClass = this
    return parameterClass.isPrimitive || parameterClass == String::class.java
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

fun Type.parameterDoesNotRequireUnwrapping(): Boolean {
    return false //TODO
}
