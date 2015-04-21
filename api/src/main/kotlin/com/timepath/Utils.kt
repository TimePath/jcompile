package com.timepath

import java.lang.invoke.MethodHandles
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.logging.Level
import java.util.logging.LogManager

class DaemonThreadFactory : ThreadFactory {
    private val delegate = Executors.defaultThreadFactory()
    override fun newThread(r: Runnable): Thread = with(delegate.newThread(r)) {
        setDaemon(true)
        this
    }
}

public inline fun time<T>(logger: Logger, name: String, action: () -> T): T {
    val start = System.currentTimeMillis()
    val ret = action()
    logger.info { "$name: ${(System.currentTimeMillis() - start).toDouble() / 1000} seconds" }
    return ret
}

public class Logger(public val logger: java.util.logging.Logger) {
    companion object {
        init {
            LogManager.getLogManager().readConfiguration(javaClass.getResourceAsStream("/logging.properties"));
        }

        suppress("NOTHING_TO_INLINE")
        inline fun new(name: String = MethodHandles.lookup().lookupClass().getName())
                = Logger(java.util.logging.Logger.getLogger(name))
    }

    public inline fun log(level: Level, msg: () -> String): Unit = if (logger.isLoggable(level)) logger.log(level, msg())
    public inline fun finest(msg: () -> String): Unit = log(Level.FINEST, msg)
    public inline fun finer(msg: () -> String): Unit = log(Level.FINER, msg)
    public inline fun fine(msg: () -> String): Unit = log(Level.FINE, msg)
    public inline fun config(msg: () -> String): Unit = log(Level.CONFIG, msg)
    public inline fun info(msg: () -> String): Unit = log(Level.INFO, msg)
    public inline fun warning(msg: () -> String): Unit = log(Level.WARNING, msg)
    public inline fun severe(msg: () -> String): Unit = log(Level.SEVERE, msg)
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
