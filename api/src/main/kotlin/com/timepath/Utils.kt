package com.timepath

import java.lang.invoke.MethodHandles
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.logging.LogManager

class DaemonThreadFactory : ThreadFactory {
    private val delegate = Executors.defaultThreadFactory()
    override fun newThread(r: Runnable): Thread = with(delegate.newThread(r)) {
        setDaemon(true)
        this
    }
}

public inline fun time<T>(logger: java.util.logging.Logger, name: String, action: () -> T): T {
    val start = System.currentTimeMillis()
    val ret = action()
    logger.info("$name: ${(System.currentTimeMillis() - start).toDouble() / 1000} seconds")
    return ret
}

public object Logger {
    init {
        LogManager.getLogManager()
                .readConfiguration(javaClass.getResourceAsStream("/logging.properties"));
    }

    [suppress("NOTHING_TO_INLINE")]
    inline fun new(name: String = MethodHandles.lookup().lookupClass().getName())
            = java.util.logging.Logger.getLogger(name)
}

public fun String.quote(): String = "\"${StringBuilder {
    for (c in this@quote) {
        when (c) {
            '\t' -> append("\\t")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '"' -> append("\\\"")
            else -> append(c)
        }
    }
}}\""
