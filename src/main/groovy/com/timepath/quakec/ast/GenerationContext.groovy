package com.timepath.quakec.ast

import org.jetbrains.annotations.NotNull

class GenerationContext {

    class Registry {
        int counter = 100
        Map<Integer, Object> values = [:]
        Map<Integer, String> reverse = [:]
        Map<String, Integer> lookup = [:]

        boolean has(@NotNull String name) {
            lookup.containsKey(name)
        }

        int get(@NotNull String name) {
            lookup[name] ?: 0
        }

        int put(String name, def value = null) {
            name != null ?: (name = "var$counter")
            def existing = lookup[name]
            if (existing) return existing
            def i = counter++
            values[i] = value
            return lookup[reverse[i] = name] = i
        }

        @Override
        String toString() {
            reverse.collect {
                """\$${it.key}\t${it.value}\t${values[it.key]}"""
            }.join('\n')
        }
    }

    Registry registry = new Registry()
}
