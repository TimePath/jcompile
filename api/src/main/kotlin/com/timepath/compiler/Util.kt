package com.timepath.compiler

import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

class DaemonThreadFactory : ThreadFactory {
    override fun newThread(r: Runnable): Thread {
        val thread = this.delegate.newThread(r)
        thread.setDaemon(true)
        return thread
    }

    val delegate = Executors.defaultThreadFactory()
}

public fun String.quote(): String = '"' + StringBuilder {
    for (c in toCharArray()) {
        when (c) {
            '\t' -> append("\\t")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '"' -> append("\\\"")
            else -> append(c)
        }
    }
}.toString() + '"'
