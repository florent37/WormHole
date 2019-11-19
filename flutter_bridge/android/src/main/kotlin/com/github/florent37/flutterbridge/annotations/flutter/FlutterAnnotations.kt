package com.github.florent37.flutterbridge.annotations.flutter

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.RetentionPolicy.RUNTIME

@Target(
        AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.PROPERTY_SETTER
)
@Retention(RUNTIME)
annotation class Expose(val value: String = "")

@Target(
        AnnotationTarget.VALUE_PARAMETER
)
@Retention(RetentionPolicy.RUNTIME)
annotation class Param(val value: String = "")

@Target(
        AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.PROPERTY_SETTER
)
@Retention(RUNTIME)
annotation class Call(val value: String = "")

@Target(
        AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.PROPERTY_SETTER
)
@Retention(RUNTIME)
annotation class Wait(
        val value: String = ""
)