package com.timepath.quakec.ast

trait Expression extends Statement {

    /**
     * Used in constant folding
     * @return A constant or null if it could change at runtime
     */
    Value evaluate() { null }

    /**
     * Used in constant folding
     * @return true if constant folding is forbidden for this node (not descendants)
     */
    boolean hasSideEffects() { false }

}