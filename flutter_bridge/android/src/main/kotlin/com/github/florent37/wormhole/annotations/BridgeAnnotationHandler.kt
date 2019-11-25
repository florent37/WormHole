package com.github.florent37.wormhole.annotations

interface BridgeAnnotationHandler {
    val fromAnnotation: Class<out Annotation>
    val toAnnotation: Class<out Annotation>
    val bindAnnotation: Class<out Annotation>
    val argumentAnnotation: Class<out Annotation>

    fun getFromAnnotationValue(fromAnnotation: Annotation) : String
    fun getToAnnotationValue(toAnnotation: Annotation) : String
    fun getBindAnnotationValue(bindAnnotation: Annotation): String
    fun getArgumentAnnotationValue(argumentAnnotation: Annotation): String
}