package com.timepath

import java.lang.invoke.MethodHandles
import java.util.logging.LogManager

object Logger {
    init {
        LogManager.getLogManager()
                .readConfiguration(javaClass.getResourceAsStream("/logging.properties"));
    }
    [suppress("NOTHING_TO_INLINE")]
    inline fun new(name: String = MethodHandles.lookup().lookupClass().getName())
            = java.util.logging.Logger.getLogger(name)
}
