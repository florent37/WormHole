package com.github.florent37.wormhole.annotations.flutter

import com.github.florent37.wormhole.annotations.BridgeAnnotationHandler

class FlutterBridgeAnnotationHandler : BridgeAnnotationHandler {

    override val fromAnnotation = Wait::class.java
    override val toAnnotation = Call::class.java
    override val bindAnnotation = Expose::class.java
    override val argumentAnnotation = Param::class.java

    override fun getFromAnnotationValue(fromAnnotation: Annotation) = (fromAnnotation as? Wait)?.value ?: ""
    override fun getToAnnotationValue(toAnnotation: Annotation) = (toAnnotation as? Call)?.value ?: ""
    override fun getBindAnnotationValue(bindAnnotation: Annotation) = (bindAnnotation as? Expose)?.value ?: ""
    override fun getArgumentAnnotationValue(argumentAnnotation: Annotation) = (argumentAnnotation as? Param)?.value ?: ""
}
