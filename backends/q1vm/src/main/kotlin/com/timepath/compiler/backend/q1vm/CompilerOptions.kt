package com.timepath.compiler.backend.q1vm

data class CompilerOptions(
        /**
         * Start allocating from this offset
         */
        var userStorageStart: Int = 100
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
        // TODO: types per fold
        , val scopeFolding: Boolean = true
        // TODO: types per fold
        , val mergeConstants: Boolean = true
        /**
         * Disabling this makes each function use its own set of locals, which leaks a lot of space
         */
        , val overlapLocals: Boolean = true
        /**
         * Support vec_x as vec.x
         */
        , val legacyVectors: Boolean = true
)
