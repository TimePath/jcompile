/*
 [The "BSD licence"]
 Copyright (c) 2013 Sam Harwell
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:
 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
 3. The name of the author may not be used to endorse or promote products
    derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

/*
    TODO: GMQCC parity

    dots.qc
    enum.qc
    goto.qc
    pops.qc
    state.qc
    ternary.qc
    vec_ops.qc

    allow keywords as identifiers
*/

grammar QC;

// PARSER

//// toplevel

compilationUnit
    :   translationUnit? EOF
    ;

translationUnit
    :   externalDeclaration+
    ;

externalDeclaration
    :   functionDefinition
    |   declaration
    |   ';' // stray ;
    ;

functionDefinition
    :   declarationSpecifiers? declarator declarationList? '='? compoundStatement
    ;

declarationList
    :   declaration+
    ;

//// expressions

expression
    // flat:
    //:   assignmentExpression (',' assignmentExpression)*
    :   assignmentExpression
    |   expression ',' assignmentExpression
    ;

assignmentExpression
    :   constantExpression
    |   unaryExpression
        ( '='
        | '*='
        | '/='
        | '%='
        | '+='
        | '-='
        | '<<='
        | '>>='
        | '&='
        | '^='
        | '|='
        )
        assignmentExpression
    ;

constantExpression
    :   conditionalExpression
    ;

conditionalExpression
    :   logicalOrExpression
    |   logicalOrExpression '?' expression ':' expression
    ;

logicalOrExpression
    :   logicalAndExpression
    |   logicalOrExpression '||' logicalAndExpression
    ;

logicalAndExpression
    :   inclusiveOrExpression
    |   logicalAndExpression '&&' inclusiveOrExpression
    ;

inclusiveOrExpression
    :   exclusiveOrExpression
    |   inclusiveOrExpression '|' exclusiveOrExpression
    ;

exclusiveOrExpression
    :   andExpression
    |   exclusiveOrExpression '^' andExpression
    ;

andExpression
    :   equalityExpression
    |   andExpression '&' equalityExpression
    ;

equalityExpression
    :   relationalExpression
    |   equalityExpression ('==' | '!=') relationalExpression
    ;

relationalExpression
    :   shiftExpression
    |   relationalExpression ('<' | '>' | '<=' | '>=') shiftExpression
    ;

shiftExpression
    :   additiveExpression
    |   shiftExpression ('<<' | '>>') additiveExpression
    ;

additiveExpression
    :   multiplicativeExpression
    |   additiveExpression ('+' | '-') multiplicativeExpression
    ;

multiplicativeExpression
    :   castExpression
    |   multiplicativeExpression ('*' | '/' | '%') castExpression
    ;

castExpression
    :   unaryExpression
    |   '(' typeName ')' castExpression
    ;

unaryExpression
    :   postfixExpression
    |   ('++' | '--') unaryExpression
    |   ('+' | '-' | '~' | '!') unaryExpression
    |   ('sizeof' | '_length') unaryExpression
    |   'sizeof' '(' typeName ')'
    ;

postfixExpression
    :   primaryExpression
    |   postfixExpression '(' argumentExpressionList? ')'
    |   postfixExpression '[' expression ']'
    |   postfixExpression '.' Identifier
    |   postfixExpression '.' '(' expression ')'
    |   postfixExpression ('++' | '--')
    |   '(' typeName ')' '{' initializerList ','? '}'
    ;

argumentExpressionList
    :   assignmentExpression (',' assignmentExpression)*
    ;

primaryExpression
    :   Identifier
    |   Constant
    |   '...' // varargs access
    // FIXME: hardcoded
    |   'float'
    |   'string'
    |   StringLiteral+
    |   '(' expression ')'
    |   genericSelection
    ;

genericSelection
    :   '_Generic' '(' assignmentExpression ',' genericAssocList ')'
    ;

genericAssocList
    :   genericAssociation (',' genericAssociation)*
    ;

genericAssociation
    :   (typeName | 'default')? ':' assignmentExpression
    ;

staticAssertDeclaration
    :   '_Static_assert' '(' constantExpression ',' StringLiteral+ ')' ';'
    |   'assert' constantExpression (':' StringLiteral+)? ';'
    ;

//// declarations

declaration
    :   declarationSpecifiers initDeclaratorList? ';'
    |   staticAssertDeclaration
    ;

declarationSpecifiers
    :   declarationSpecifier+
    ;

declarationSpecifiers2
    :   declarationSpecifier+
    ;

declarationSpecifier
    :   storageClassSpecifier
    |   typeSpecifier
    |   typeQualifier
    |   functionSpecifier
    |   '[' '[' attributeList ']' ']'
    ;

storageClassSpecifier
    :   'typedef'
    |   'extern'
    |   'static'
    |   'auto'
    |   'signed'
    |   'unsigned'
    ;

typeSpecifier
    :   pointer? directTypeSpecifier
    ;

pointer
    :  '.'+
    ;

directTypeSpecifier
    :   ('void'
    |   'bool'
    |   'char'
    |   'short'
    |   'int'
    |   'float'
    |   'vector'
    |   'string'
    |   'entity') ('(' parameterTypeList ')')?
    |   structOrUnionSpecifier
    |   enumSpecifier
    |   typedefName
    ;

structOrUnionSpecifier
    :   structOrUnion Identifier? '{' structDeclarationList '}'
    |   structOrUnion Identifier
    ;

structOrUnion
    :   'struct'
    |   'union'
    ;

structDeclarationList
    :   structDeclaration+
    ;

structDeclaration
    :   specifierQualifierList structDeclaratorList? ';'
    |   staticAssertDeclaration
    ;

specifierQualifierList
    :   (typeSpecifier | typeQualifier)+
    ;

structDeclaratorList
    :   structDeclarator (',' structDeclarator)*
    ;

structDeclarator
    :   declarator
    |   declarator? ':' constantExpression
    ;

enumSpecifier
    :   'enum' Identifier? '{' enumeratorList ','? '}'
    |   'enum' Identifier
    ;

enumeratorList
    :   enumerator (',' enumerator)*
    ;

enumerator
    :   enumerationConstant ('=' constantExpression)?
    ;

enumerationConstant
    :   Identifier
    ;

typedefName
    :   Identifier
    ;

attributeList
    :   attribute (',' attribute)*
    ;

attribute
    :   ~(',' | '[' | ']') // relaxed def for "identifier or reserved word"
        ('(' argumentExpressionList? ')')?
    |   // empty
    ;

initDeclaratorList
    :   initDeclarator (',' initDeclarator)*
    ;

initDeclarator
    :   declarator ('=' initializer)?
    ;

declarator
    :   directDeclarator
    ;

directDeclarator
    :   Identifier
    |   '(' declarator ')'
    |   directDeclarator '[' assignmentExpression? ']'
    |   directDeclarator '(' parameterTypeList ')'
    ;

abstractDeclarator
    :   directAbstractDeclarator
    ;

directAbstractDeclarator
    :   '(' abstractDeclarator ')'
    |   directAbstractDeclarator '[' assignmentExpression? ']'
    |   directAbstractDeclarator '(' parameterTypeList ')'
    ;

parameterTypeList
    :   parameterList (',' parameterVarargs)?
    |   parameterVarargs
    ;

parameterList
    :   (parameterDeclaration (',' parameterDeclaration)*)?
    ;

parameterVarargs
    :   declarationSpecifiers? '...' Identifier?
    ;

parameterDeclaration
    :   declarationSpecifiers declarator
    |   declarationSpecifiers2 abstractDeclarator?
    ;

initializer
    :   assignmentExpression
    |   '{' initializerList ','? '}'
    ;

initializerList
    :   designation? initializer
    |   initializerList ',' designation? initializer
    ;

designation
    :   designatorList '='
    ;

designatorList
    :   designator+
    ;

designator
    :   '[' constantExpression ']'
    |   '.' Identifier
    ;

typeQualifier
    :   'const'
    |   'var'
    ;

functionSpecifier
    :   ('inline'
    |   '_Noreturn')
    ;

typeName
    :   specifierQualifierList abstractDeclarator?
    ;

//// statements

statement
    :   labeledStatement
    |   compoundStatement
    |   expressionStatement
    |   selectionStatement
    |   iterationStatement
    |   jumpStatement
    ;

labeledStatement
    :   Identifier ':' blockItem?
    |   ':' Identifier blockItem?
    |   'case' constantExpression ':' blockItem
    |   'default' ':' blockItem
    ;

compoundStatement
    :   '{' blockItemList? '}'
    ;

blockItemList
    :   blockItem+
    ;

blockItem
    :   declaration
    |   statement
    ;

expressionStatement
    :   expression? ';'
    ;

selectionStatement
    :   'if' 'not'? '(' expression ')' statement ('else' statement)?
    |   'switch' '(' expression ')' statement
    ;

iterationStatement
    :   'while' '(' expression ')' statement
    |   'do' statement 'while' '(' expression ')' ';'
    |   'for' '(' expression? ';' expression? ';' expression? ')' statement
    |   'for' '(' declaration expression? ';' expression? ')' statement
    ;

jumpStatement
    :   'goto' Identifier ';'
    |   'continue' ';'
    |   'break' ';'
    |   'return' expression? ';'
    ;

// LEXER

//// keywords

Auto : 'auto';
Bool : 'bool';
Break : 'break';
Case : 'case';
Char : 'char';
Const : 'const';
Continue : 'continue';
Default : 'default';
Do : 'do';
Else : 'else';
Entity : 'entity';
Enum : 'enum';
Extern : 'extern';
Float : 'float';
For : 'for';
Goto : 'goto';
If : 'if';
IfNot : 'not';
Inline : 'inline';
Int : 'int';
Return : 'return';
Short : 'short';
Signed : 'signed';
Sizeof : 'sizeof';
Static : 'static';
String : 'string';
Struct : 'struct';
Switch : 'switch';
Typedef : 'typedef';
Union : 'union';
Unsigned : 'unsigned';
Var : 'var';
Vector : 'vector';
Void : 'void';
While : 'while';

Assert : 'assert';
Generic : '_Generic';
Noreturn : '_Noreturn';
StaticAssert : '_Static_assert';

LeftParen : '(';
RightParen : ')';
LeftBracket : '[';
RightBracket : ']';
LeftBrace : '{';
RightBrace : '}';

Less : '<';
LessEqual : '<=';
Greater : '>';
GreaterEqual : '>=';
LeftShift : '<<';
RightShift : '>>';

Plus : '+';
PlusPlus : '++';
Minus : '-';
MinusMinus : '--';
Star : '*';
Div : '/';
Mod : '%';

And : '&';
Or : '|';
AndAnd : '&&';
OrOr : '||';
Caret : '^';
Not : '!';
Tilde : '~';

Question : '?';
Colon : ':';
Semi : ';';
Comma : ',';

Assign : '=';
StarAssign : '*=';
DivAssign : '/=';
ModAssign : '%=';
PlusAssign : '+=';
MinusAssign : '-=';
LeftShiftAssign : '<<=';
RightShiftAssign : '>>=';
AndAssign : '&=';
XorAssign : '^=';
OrAssign : '|=';

Equal : '==';
NotEqual : '!=';

Dot : '.';
Ellipsis : '...';
Sharp : '#';

//// fragments

fragment
Nondigit
    :   [a-zA-Z_]
    ;

fragment
NonzeroDigit
    :   [1-9]
    ;

fragment
Digit
    :   [0-9]
    ;

fragment
OctalDigit
    :   [0-7]
    ;

fragment
HexadecimalPrefix
    :   '0' [xX]
    ;

fragment
HexadecimalDigit
    :   [0-9a-fA-F]
    ;

//// identifiers

Identifier
    :   IdentifierNondigit (IdentifierNondigit | Digit)*
    ;

fragment
IdentifierNondigit
    :   Nondigit
    |   UniversalCharacterName
    ;

fragment
UniversalCharacterName
    :   '\\u' HexadecimalDigit HexadecimalDigit HexadecimalDigit HexadecimalDigit
    ;

//// constants

Constant
    :   BuiltinConstant
    |   IntegerConstant
    |   FloatingConstant
    //|   EnumerationConstant
    |   '\'' SChar '\''
    |   VectorConstant
    ;

fragment
BuiltinConstant
    : '#' (IntegerConstant | FloatingConstant)
    ;

fragment
IntegerConstant
    :   (DecimalConstant
    |   OctalConstant
    |   HexadecimalConstant) IntegerSuffix?
    ;

fragment
DecimalConstant
    :   NonzeroDigit Digit*
    ;

fragment
OctalConstant
    :   '0' OctalDigit*
    ;

fragment
HexadecimalConstant
    :   HexadecimalPrefix HexadecimalDigit+
    ;

fragment
IntegerSuffix
    :   UnsignedSuffix LongSuffix?
    |   LongSuffix UnsignedSuffix?
    ;

fragment
UnsignedSuffix
    :   [uU]
    ;

fragment
LongSuffix
    :   [lL]
    ;

fragment
FloatingConstant
    :   DecimalFloatingConstant
    |   HexadecimalFloatingConstant
    ;

fragment
DecimalFloatingConstant
    :   FractionalConstant ExponentPart? FloatingSuffix?
    |   DigitSequence ExponentPart FloatingSuffix?
    ;

fragment
HexadecimalFloatingConstant
    :   HexadecimalPrefix (HexadecimalFractionalConstant | HexadecimalDigitSequence) BinaryExponentPart FloatingSuffix?
    ;

fragment
FractionalConstant
    :   (DigitSequence? '.')? DigitSequence
    ;

fragment
ExponentPart
    :   ('e' | 'E') Sign? DigitSequence
    ;

fragment
Sign
    :   '+' | '-'
    ;

fragment
DigitSequence
    :   Digit+
    ;

fragment
HexadecimalFractionalConstant
    :   HexadecimalDigitSequence? '.' HexadecimalDigitSequence
    |   HexadecimalDigitSequence '.'
    ;

fragment
BinaryExponentPart
    :   ('p' | 'P') Sign? DigitSequence
    ;

fragment
HexadecimalDigitSequence
    :   HexadecimalDigit+
    ;

fragment
FloatingSuffix
    :   'f' | 'F'
    ;

fragment
VectorConstant
    :   '\'' Whitespace? VectorComponent Whitespace VectorComponent Whitespace VectorComponent Whitespace? '\''
    ;

fragment
VectorComponent
    :   Sign? (IntegerConstant
    |   FloatingConstant)
    ;

StringLiteral
    :   '"' SCharSequence? '"'
    ;

fragment
SCharSequence
    :   SChar+
    ;

fragment
SChar
    :   ~["\\\r\n]
    |   EscapeSequence
    ;

fragment
EscapeSequence
    :   SimpleEscapeSequence
    |   OctalEscapeSequence
    |   HexadecimalEscapeSequence
    |   UniversalCharacterName
    |   GMQCCEscapeSequence
    ;

fragment
SimpleEscapeSequence
    :   '\\' ['"?abfnrtv\\]
    ;

fragment
OctalEscapeSequence
    :   '\\' OctalDigit OctalDigit? OctalDigit?
    ;

fragment
HexadecimalEscapeSequence
    :   '\\x' HexadecimalDigit+
    ;

fragment
GMQCCEscapeSequence
    :   '\\{' 'x'? DecimalConstant+ '}'
    ;

//// ignore

LineDirective
    :   '#' Whitespace? DecimalConstant Whitespace? StringLiteral ~[\r\n]*
        -> skip
    ;

PragmaDirective
    :   '#' Whitespace? 'pragma' Whitespace ~[\r\n]*
        -> skip
    ;

Whitespace
    :   [ \t]+
        -> skip
    ;

Newline
    :   '\r'? '\n'
        -> skip
    ;

BlockComment
    :   '/*' .*? '*/'
        -> skip
    ;

LineComment
    :   '//' ~[\r\n]*
        -> skip
    ;
