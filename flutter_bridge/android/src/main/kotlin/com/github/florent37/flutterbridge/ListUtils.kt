package com.github.florent37.flutterbridge

import java.lang.ref.WeakReference

fun MutableList<WeakReference<Any>>.addSafety(value: Any){
    this.add(WeakReference(value))
}

fun MutableList<WeakReference<Any>>.forEachSafety(block: (Any) -> Unit) {
    val listIterator = this.listIterator()
    while (listIterator.hasNext()) {
        val value = listIterator.next()
        value.get()?.let { value ->
            block(value)
        } ?: run {
            listIterator.remove()
        }
    }
}