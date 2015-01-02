package com.timepath.quakec.ast.impl

import com.timepath.quakec.ast.Type
import groovy.transform.TupleConstructor

@TupleConstructor
class DeclarationExpression extends ReferenceExpression {

    Type type
    String id

    @Override
    String getText() { "$type $id" }
}
