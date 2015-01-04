package com.timepath.quakec.ast

trait Expression : Statement {
    /**
     * Used in constant folding
     *
     * @return A constant or null if it could change at runtime
     */
    fun evaluate(): Value? = null

    /**
     * Used in constant folding
     *
     * @return true if constant folding is forbidden for this node (not descendants)
     */
    fun hasSideEffects(): Boolean = false
}
