package com.timepath.compiler.backend.q1vm

import com.timepath.compiler.ir.Instruction

data class CompilerOptions(
        /**
         * Start allocating from this offset
         */
        val userStorageStart: Int = Instruction.OFS_PARAM(8).i
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
        // TODO: types per fold, contiguous for vectors
        , val scopeFolding: Boolean = false
        // TODO: types per fold
        , val mergeConstants: Boolean = false
        /**
         * Disabling this makes each function use its own set of locals, which leaks a lot of space
         */
        , val overlapLocals: Boolean = true
        /**
         * Support vec_x as vec.x
         */
        , val legacyVectors: Boolean = true
        /**
         * Support `if not (expr)`
         */
        , val ifNot: Boolean = false
        /**
         * Allows the following without special syntax:
         *
         * .float field;
         * ent.field
         *
         * This violates entity classes.
         *
         * Prefer:
         * ent.(field)
         */
        , val legacyPointerToMember: Boolean = false
        /**
         * Try 'missing' symbols as entity fields
         *
         * globally:
         * entityclass entity {
         *      .float f;
         * }
         *
         * locally:
         * .float fld = f; // comes from entity::
         */
        , val legacyFieldNamespace: Boolean = true
)
