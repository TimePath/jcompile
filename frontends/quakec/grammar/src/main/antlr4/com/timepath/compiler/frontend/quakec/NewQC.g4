grammar NewQC;
compilationUnit
    :    scopeGlobal* EOF
    ;
id
    :    Identifier
    |    KW_var
    |    KW_inline
    |    KW_break
    ;
attribType
    :    KW_const
    ;
attribFunc
    :    '[' '[' attribute=expr ']' ']'
    |    kw=KW_var
    |    kw=KW_inline
    ;
attribVar
    :    '[' '[' attribute=expr ']' ']'
    |    kw=KW_typedef
    |    kw=KW_static
    |    kw=KW_noref
    |    kw=KW_var
    |    kw=KW_local
    ;
block
    :    '{' scopeBlock* '}'
    ;
scopeBlock
    :    ';'
    |    declVar
    |    stmt
    ;
scopeGlobal
    :    ';'
    |    declEntity
    |    declEnum
    |    declFunc
    |    declVar
    ;
declFunc
    :    attribFunc* type id functypeParams ('='? block)?
    |    attribFunc* type id functypeParams '=' exprAssign
    |    attribFunc* type id '='? block
    ;
declVar
    :    declVar_ ';'
    ;
declVar_
    :    attribVar* type declVar__ (',' declVar__)*
    ;
declVar__
    :    id (('=' exprAssign)?
    |    '[' exprAssign ']' ('=' '{' exprAssign (',' exprAssign)* ','? '}')?)
    ;
declEntity
    :    KW_entityclass id (':' id)? '{' scopeEntity* '}'
    ;
scopeEntity
    :    ';'
    |    declVar
    |    declFunc
    ;
declEnum
    :    KW_enum (':' id)? '{' declEnum_ (',' declEnum_)* '}'
    ;
declEnum_
    :    id ('=' exprAssign)?
    ;
functypeParams
    :    '(' (functypeParam (',' functypeParam)*)? ')'
    ;
functypeParam
    :    type id?
    |    '...'
    ;
type
    :    attribType* type_
    ;
type_
    :    typeName
    |    type_ functypeParams
    |    typePtr type_
    ;
typePtr
    :    '.'
    |    '...'
    ;
typeName
    :    Identifier
    |    KW_auto
    |    KW_var
    |    KW_void
    |    KW_bool
    |    KW_char
    |    KW_short
    |    KW_int
    |    KW_float
    |    KW_vector
    |    KW_string
    |    KW_entity
    ;
expr
    :    exprAssign
    |    expr ',' exprAssign
    ;
exprAssign
    :    exprConst
    |    lvalue ('='
    |    '|='
    |    '^='
    |    '&='
    |    '&~='
    |    '<<='
    |    '>>='
    |    '>>>='
    |    '+='
    |    '-='
    |    '*='
    |    '/='
    |    '%='
    |    '><='
    |    '**=') expr
    ;
lvalue
    :    id
    |    KW_return
    |    lvalue '.' lvalue
    |    lvalue '.' '(' lvalue ')'
    |    lvalue '(' (exprAssign (',' exprAssign)*)? ')' '.' lvalue
    |    lvalue '[' expr ']' ('.' lvalue)?
    |    '(' lvalue ')'
    |    '(' expr ')'
    ;
exprConst
    :    exprCond
    ;
exprCond
    :    exprLogicalOr
    |    exprCond '?' expr ':' expr
    |    KW_if '(' expr ')' expr KW_else expr
    |    KW_switch '(' expr ')' block
    ;
exprLogicalOr
    :    exprLogicalXor
    |    exprLogicalOr '||' exprLogicalXor
    ;
exprLogicalXor
    :    exprLogicalAnd
    |    exprLogicalXor '^^' exprLogicalAnd
    ;
exprLogicalAnd
    :    exprInclusiveOr
    |    exprLogicalAnd '&&' exprInclusiveOr
    ;
exprInclusiveOr
    :    exprExclusiveOr
    |    exprInclusiveOr '|' exprExclusiveOr
    ;
exprExclusiveOr
    :    exprAnd
    |    exprExclusiveOr '^' exprAnd
    ;
exprAnd
    :    exprEqual
    |    exprAnd '&' exprEqual
    ;
exprEqual
    :    exprCompare
    |    exprEqual ('=='
    |    '!=') exprCompare
    ;
exprCompare
    :    exprShift
    |    exprCompare ('<'
    |    '<='
    |    '<=>'
    |    '>='
    |    '>') exprShift
    ;
exprShift
    :    exprAdd
    |    exprShift ('<<'
    |    '>>'
    |    '>>>') exprAdd
    ;
exprAdd
    :    exprMul
    |    exprAdd ('+'
    |    '-') exprMul
    ;
exprMul
    :    exprExp
    |    exprMul ('*'
    |    '/'
    |    '%'
    |    '><') exprExp
    ;
exprExp
    :    exprUnary
    |    exprUnary '**' exprExp
    ;
exprUnary
    :    ('++'
    |    '--') exprUnary
    |    ('+'
    |    '-') exprUnary
    |    exprPostfix
    |    ('!'
    |    '~') exprUnary
    |    '(' type ')' exprUnary
    |    KW_sizeof exprUnary
    ;
exprPostfix
    :    exprPrimary
    |    exprPostfix ('++'
    |    '--')
    ;
exprPrimary
    :    literal
    |    '(' expr ')'
    |    '...' '(' exprAssign ',' type ')'
    |    exprPrimary '.' id
    |    exprPrimary '[' expr ']'
    |    exprPrimary ('.*' expr
    |    '.' '(' expr ')')
    |    exprPrimary '(' (exprAssign (',' exprAssign)*)? ')'
    ;
literal
    :    id
    |    Number
    |    Character
    |    Vector
    |    String
    |    '[' exprAssign (',' exprAssign)* ']'
    |    '(' block ')'
    ;
stmt
    :    block
    |    ';'
    |    exprAssign ';'
    |    KW_switch '(' expr ')' block
    |    KW_do stmt KW_while '(' expr ')' ';'
    |    KW_goto id ';'
    |    KW_break id? ';'
    |    KW_continue id? ';'
    |    KW_return expr? ';'
    |    (id ':'
    |    ':' id) stmt?
    |    KW_case expr ':' stmt
    |    KW_default ':' stmt
    |    KW_if '(' expr ')' stmt
    |    KW_if '(' expr ')' stmt_noif KW_else stmt
    |    KW_while '(' expr ')' stmt
    |    KW_for '(' (';'
    |    declVar
    |    expr ';') expr? ';' expr? ')' stmt
    ;
stmt_noif
    :    block
    |    ';'
    |    exprAssign ';'
    |    KW_switch '(' expr ')' block
    |    KW_do stmt KW_while '(' expr ')' ';'
    |    KW_goto id ';'
    |    KW_break id? ';'
    |    KW_continue id? ';'
    |    KW_return expr? ';'
    |    (id ':'
    |    ':' id) stmt_noif?
    |    KW_case expr ':' stmt_noif
    |    KW_default ':' stmt_noif
    |    KW_if '(' expr ')' stmt_noif KW_else stmt_noif
    |    KW_while '(' expr ')' stmt_noif
    |    KW_for '(' (';'
    |    declVar
    |    expr ';') expr? ';' expr? ')' stmt_noif
    ;
KW_break
    :    'break'
    ;
KW_goto
    :    'goto'
    ;
KW_typedef
    :    'typedef'
    ;
KW_for
    :    'for'
    ;
KW_default
    :    'default'
    ;
KW_noref
    :    'noref'
    ;
KW_sizeof
    :    'sizeof'
    ;
KW_bool
    :    'bool'
    ;
KW_while
    :    'while'
    ;
KW_continue
    :    'continue'
    ;
KW_if
    :    'if'
    ;
KW_switch
    :    'switch'
    ;
KW_char
    :    'char'
    ;
KW_void
    :    'void'
    ;
KW_string
    :    'string'
    ;
KW_local
    :    'local'
    ;
KW_entityclass
    :    'entityclass'
    ;
KW_var
    :    'var'
    ;
KW_enum
    :    'enum'
    ;
KW_const
    :    'const'
    ;
KW_vector
    :    'vector'
    ;
KW_float
    :    'float'
    ;
KW_int
    :    'int'
    ;
KW_static
    :    'static'
    ;
KW_short
    :    'short'
    ;
KW_case
    :    'case'
    ;
KW_auto
    :    'auto'
    ;
KW_return
    :    'return'
    ;
KW_entity
    :    'entity'
    ;
KW_do
    :    'do'
    ;
KW_pragma
    :    'pragma'
    ;
KW_inline
    :    'inline'
    ;
KW_else
    :    'else'
    ;
Identifier
    :    IdentifierFragment ((NumberFragment
    |    IdentifierFragment))*
    ;
fragment
IdentifierFragment
    :    'a' .. 'z'
    |    'A' .. 'Z'
    |    '_'
    |    '::'
    ;
Number
    :    '#'? (NumberFragment* '.')? NumberFragment+ 'f'?
    |    '0' 'x' HexDigit+
    ;
fragment
NumberFragment
    :    '0' .. '9'
    ;
fragment
HexDigit
    :    NumberFragment
    |    'a' .. 'f'
    |    'A' .. 'F'
    ;
String
    :    (((Whitespace
    |    Newline))* '"' StringFragment* '"')+
    ;
fragment
StringFragment
    :    ~["\\\r\n]
    |    EscapeSequence
    ;
Character
    :    '\'' CharacterFragment '\''
    ;
fragment
CharacterFragment
    :    ~['\\\r\n]
    |    EscapeSequence
    ;
Vector
    :    '\'' Whitespace* (SignedNumber Whitespace*)* '\''
    ;
fragment
SignedNumber
    :    (('+'
    |    '-'))? Number
    ;
fragment
EscapeSequence
    :    '\\' ['"?abfnrtv\\]
    |    '\\x' HexDigit
    |    '\\{' 'x'? Number '}'
    ;
Whitespace
    :    [ \t]+ -> channel(1)
    ;
Newline
    :    ('\n'
    |    '\r' '\n'?) -> channel(1)
    ;
LineDirective
    :    ('#'
    |    '#line') Whitespace* Number Whitespace+ String Whitespace* ~[\r\n]* -> channel(2)
    ;
PragmaDirective
    :    '#' Whitespace* KW_pragma Whitespace* ~[\r\n]* -> channel(2)
    ;
CommentLine
    :    '//' ~[\r\n]* -> channel(3)
    ;
CommentDoc
    :    '/**' .*? '*/' -> channel(3)
    ;
CommentBlock
    :    '/*' .*? '*/' -> channel(3)
    ;
