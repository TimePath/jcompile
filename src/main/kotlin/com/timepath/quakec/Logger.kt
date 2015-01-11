package com.timepath.quakec

import java.util.logging.Logger
import java.lang.invoke.MethodHandles
import java.util.logging.LogManager

object Logging {
    {
        LogManager.getLogManager().readConfiguration(javaClass.getResourceAsStream("/logging.properties"));
    }
    [suppress("NOTHING_TO_INLINE")]
    inline fun new(name: String = MethodHandles.lookup().lookupClass().getName()) = Logger.getLogger(name)
}
