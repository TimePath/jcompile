package com.timepath.compiler.backends.q1vm

data class CompilerOptions(
        /**
         * Start allocating from this offset
         */
        var userStorageStart: Int = 100,
        /**
         * Reuse references from previous deeper scopes, but not between functions as that may not be safe
         *
         * Example:
         *
         * {
         *      float x;
         * }
         * float y; // re-use x
         *
         */
        // TODO: give generated functions locals
        // TODO: types per fold
        val scopeFolding: Boolean = true,
        // TODO: types per fold
        val mergeConstants: Boolean = true

)
