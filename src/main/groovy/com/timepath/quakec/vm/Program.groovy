package com.timepath.quakec.vm

import groovy.transform.CompileStatic

@CompileStatic
class Program {

    private Loader data

    Program(Loader data) {
        this.data = data
    }

    void exec(String needle = 'main') {
        Function fn = data.functions.find {
            it.name == needle
        }
        int fp = fn.firstStatement
        while (1) {
            fp = data.statements.get(fp).exec(fp)
        }
    }

    public static void main(String[] args) {
        def data = "${System.properties["user.home"]}/IdeaProjects/xonotic/data/xonotic-data.pk3dir"
        new Program(new Loader("${data}/progs.dat" as File)).exec()
    }

}