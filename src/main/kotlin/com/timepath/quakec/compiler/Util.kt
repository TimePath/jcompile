package com.timepath.quakec.compiler

import java.text.SimpleDateFormat
import java.util.concurrent.ThreadFactory
import java.util.concurrent.Executors
import java.util.Date

fun Date.format(pattern: String) = SimpleDateFormat(pattern).format(this)

class DaemonThreadFactory : ThreadFactory {
    override fun newThread(r: Runnable): Thread {
        val thread = this.delegate.newThread(r)
        thread.setDaemon(true)
        return thread
    }

    val delegate = Executors.defaultThreadFactory()
}
