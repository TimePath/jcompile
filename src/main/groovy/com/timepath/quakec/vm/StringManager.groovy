package com.timepath.quakec.vm

import groovy.transform.CompileStatic

@CompileStatic
class StringManager {

    private LinkedHashMap<Integer, String> constant
    private int constantSize
    private ArrayList<String> temp = []
    private ArrayList<String> zone = []

    StringManager(LinkedHashMap<Integer, String> constant, int constantSize) {
        this.constant = constant
        this.constantSize = constantSize
        temp.ensureCapacity(512)
        zone.ensureCapacity(512)
    }

    String getAt(int index) {
        if (index >= 0) {
            if (index < constantSize)
                return constant[index]
            else if ((index -= constantSize) < zone.size())
                return zone[index]
        } else if ((index = ~index) < temp.size()) {
            return temp[index]
        }
        return ""
    }
}
