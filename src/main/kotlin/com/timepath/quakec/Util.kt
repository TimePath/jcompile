package com.timepath.quakec

import java.util.concurrent.ThreadFactory
import java.util.concurrent.Executors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.ArrayList

fun String.times(n: Int) = this.repeat(n)

fun Date.format(pattern: String) = SimpleDateFormat(pattern).format(this)

class DaemonThreadFactory : ThreadFactory {
    override fun newThread(r: Runnable): Thread {
        val thread = this.delegate.newThread(r)
        thread.setDaemon(true)
        return thread
    }

    val delegate = Executors.defaultThreadFactory()
}
