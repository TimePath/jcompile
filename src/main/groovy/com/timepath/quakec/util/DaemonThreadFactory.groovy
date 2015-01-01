package com.timepath.quakec.util

import groovy.transform.CompileStatic

import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

@CompileStatic
class DaemonThreadFactory implements ThreadFactory {

    private ThreadFactory delegate = Executors.defaultThreadFactory()

    @Override
    Thread newThread(Runnable r) {
        this.delegate.newThread(r).with {
            daemon = true
            it
        }
    }
}