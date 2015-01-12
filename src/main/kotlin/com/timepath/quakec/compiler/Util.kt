package com.timepath.quakec.compiler

import java.util.concurrent.ThreadFactory
import java.util.concurrent.Executors

class DaemonThreadFactory : ThreadFactory {
    override fun newThread(r: Runnable): Thread {
        val thread = this.delegate.newThread(r)
        thread.setDaemon(true)
        return thread
    }

    val delegate = Executors.defaultThreadFactory()
}
