grammar Kotlin;

// FIXME:
// val x = "//comment"
// val x = "'apos"
// val x = "#include"
// val x = "~"
// val x = "&"
// val x = "|"
// val x = "^"
// val x = "`grave"

file
  : preamble toplevelObject* EOF
  ;
script
  : preamble expression* EOF
  ;
preamble
  : fileAnnotations
    packageHeader?
    kimport*
  ;
fileAnnotations
  : fileAnnotation*
  ;
fileAnnotation
  : '[' 'file' ':' annotationEntry+ ']'
  ;
packageHeader
  : ('package' packageName)
  ;
  packageName
    : simpleName ('.' simpleName)*
    ;
kimport
  : 'import' packageName ('.' '*' | 'as' simpleName)?
  ;
toplevelObject
  : class
  | object
  | function
  | property
  ;
class
  : modifiers ('class' | 'trait') simpleName
      typeParameters?
      (modifiers ('(' functionParameters ')'))?
      (':' annotations delegationSpecifiers)?
      typeConstraints
      (classBody | enumClassBody)?
  ;
classBody
  : '{' memberDeclaration* '}'
  ;
delegationSpecifier
  : constructorInvocation
  | userType
  | explicitDelegation
  ;
  delegationSpecifiers
    : delegationSpecifier (',' delegationSpecifier)*
    ;
explicitDelegation
  : userType 'by' expression
  ;
typeParameters
  : '<' typeParameter (',' typeParameter)* '>'
  ;
typeParameter
  : modifiers simpleName (':' userType)?
  ;
typeConstraints
  : ('where' typeConstraint (',' typeConstraint)*)?
  ;
typeConstraint
  : annotations simpleName ':' type
  ;
memberDeclaration
  : companionObject
  | object
  | function
  | property
  | class
  | anonymousInitializer
  | secondaryConstructor
  ;
anonymousInitializer
  : 'init' block
  ;
companionObject
  : modifiers 'companion' 'object' simpleName? objectLiteral
  ;
  objectLiteral
    : (':' delegationSpecifiers)? classBody
    ;
valueParameters
  : '(' functionParameters? ')'
  ;
  functionParameters
    : functionParameter (',' functionParameter)*
    ;
functionParameter
  : modifiers ('val' | 'var')? parameter ('=' expression)?
  ;
initializer
  : annotations constructorInvocation
  ;
block
  : '{' statements '}'
  ;
  statements
    : statement*
    ;
function
  : modifiers 'fun' typeParameters?
      (type memberAccessOperation)?
      simpleName
      typeParameters? valueParameters (':' type)?
      typeConstraints
      functionBody?
  ;
functionBody
  : block
  | '=' expression
  ;
variableDeclarationEntry
  : simpleName (':' type)?
  ;
multipleVariableDeclarations
  : '(' variableDeclarationEntry (',' variableDeclarationEntry)* ')'
  ;
property
  : modifiers ('val' | 'var')
      typeParameters? (type '.' | annotations)?
      (multipleVariableDeclarations | variableDeclarationEntry)
      typeConstraints
      (('by' | '=') expression)?
      (getter setter | setter getter | getter | setter)?
  ;
getter
  : modifiers 'get' ('(' ')' (':' type)? functionBody)?
  ;
setter
  : modifiers 'set' ('(' modifiers (simpleName | parameter) ')' functionBody)?
  ;
parameter
  : simpleName ':' type
  ;
object
  : modifiers 'object' simpleName (':' delegationSpecifiers)? classBody?
  ;
secondaryConstructor
  : modifiers 'constructor' valueParameters (':' constructorDelegationCall)? block?
  ;
constructorDelegationCall
  : ('this' | 'super') valueArguments
  ;
enumClassBody
  : '{' (enumEntry | memberDeclaration)* '}'
  ;
enumEntry
  : modifiers simpleName (':' initializers)? classBody?
  ;
  initializers
    : initializer (',' initializer)*
    ;
type
  : annotations type_
  ;
type_
  : (typeDescriptor (memberAccessOperation functionType)? | functionType)
  ;
typeDescriptor
  : '(' type_ ')'
  | userType
  | 'dynamic'
  | typeDescriptor '?' //#nullableType
  ;

userType
  : simpleUserType ('.' simpleUserType)*
  ;
simpleUserType
  : simpleName (
    '<' (
      (varianceAnnotation? type | '*') (',' (varianceAnnotation? type | '*'))*
    ) '>'
  )?
  ;
functionType
  : '(' ((parameter | modifiers type) (',' (parameter | modifiers type))*)? ')' '->' type?
  ;
if
  : 'if' '(' expression ')' expression ('else' expression)?
  ;
try
  : 'try' block catchBlock* finallyBlock?
  ;
catchBlock
  : 'catch' '(' annotations simpleName ':' userType ')' block
  ;
finallyBlock
  : 'finally' block
  ;
loop
  : 'for' '(' annotations (multipleVariableDeclarations | variableDeclarationEntry) 'in' expression ')' expression #for
  | 'while' '(' expression ')' expression #while
  | 'do' expression 'while' '(' expression ')' #doWhile
  ;

expression
  : disjunction (assignmentOperator disjunction)*
  ;
  assignmentOperator
    : '='
    | '+=' | '-=' | '*=' | '/=' | '%='
    ;
disjunction
  : conjunction ('||' conjunction)*
  ;
conjunction
  : equalityComparison ('&&' equalityComparison)*
  ;
equalityComparison
  : comparison (equalityOperation comparison)*
  ;
  equalityOperation
    : '!=' | '=='
    ;
comparison
  : namedInfix (comparisonOperation namedInfix)*
  ;
  comparisonOperation
    : '<' | '>' | '>=' | '<='
    ;
namedInfix
  : elvisExpression (inOperation elvisExpression)*
  | elvisExpression (isOperation type)?
  ;
  inOperation
    : 'in' | '!in'
    ;
  isOperation
    : 'is' | '!is'
    ;
elvisExpression
  : infixFunctionCall ('?:' infixFunctionCall)*
  ;
infixFunctionCall
  : rangeExpression (/* TODO: require no SEMI. Ugh, SEMI everywhere... */ simpleName rangeExpression)*
  ;
rangeExpression
  : additiveExpression ('..' additiveExpression)*
  ;
additiveExpression
  : multiplicativeExpression (additiveOperation multiplicativeExpression)*
  ;
  additiveOperation
    : '+' | '-'
    ;
multiplicativeExpression
  : typeRHS (multiplicativeOperation typeRHS)*
  ;
  multiplicativeOperation
    : '*' | '/' | '%'
    ;
typeRHS
  : prefixUnaryExpression (typeOperation type)*
  ;
  typeOperation
    : 'as' | 'as?' | ':'
    ;
prefixUnaryExpression
  : prefixUnaryOperation* postfixUnaryExpression
  ;
  prefixUnaryOperation
      : '-' | '+'
      | '++' | '--'
      | '!'
      | annotation
      | label
      ;
      label
        : '@' simpleName
        ;
postfixUnaryExpression
  : (callableReference | atomicExpression) postfixUnaryOperation*
  ;
  postfixUnaryOperation
    : '++' | '--' | '!!'
    | callSuffix
    | arrayAccess
    | memberAccessOperation postfixUnaryExpression
    ;
    callSuffix
      : typeArguments? (label? functionLiteral)
      | typeArguments? valueArguments (label? functionLiteral)?
      ;
    arrayAccess
      : '[' expression (',' expression)* ']'
      ;
    memberAccessOperation
      : '.' | '?.'
      ;
callableReference
  : userType? '::' simpleName
  ;
atomicExpression
  : '(' expression ')'
  | literalConstant
  | functionLiteral
  | 'this' label?
  | 'super' ('<' type '>')? label?
  | if
  | when
  | try
  | 'object' objectLiteral
  | jump
  | loop
  | simpleName
  | '$' simpleName //#FieldName
  | 'package'
  ;
  literalConstant
    : 'true' | 'false'
    | stringTemplate
    | NoEscapeString
    | IntegerLiteral
    | CharacterLiteral
    | FloatingPointLiteral
    | 'null'
    ;
    stringTemplate // FIXME: allow spaces
      : '"' stringTemplateElement* '"'
      ;
      stringTemplateElement
        : '$' (simpleName | 'this')
        | EscapeSequence
        | longTemplate
        | ~'"'
        ;
        longTemplate
          : '${' expression '}'
          ;
declaration
  : function
  | property
  | class
  ;
statement
  : declaration
  | expression
  ;
typeArguments
  : '<' type (',' type)* '>'
  ;
valueArguments
  : '(' (valueArgument (',' valueArgument)*)? ')'
  ;
valueArgument
  : (simpleName '=')? '*'? expression
  ;
jump
  : 'throw' expression
  | 'return' label? expression?
  | 'continue' label?
  | 'break' label?
  ;
functionLiteral
  : '{' (functionLiteralArgs '->')? statement* '}'
  | 'fun' (type memberAccessOperation)? valueParameters (':' type)? block
  ;
  functionLiteralArgs
    : functionLiteralArg (',' functionLiteralArg)*
    ;
    functionLiteralArg
      : modifiers simpleName (':' type)?
      ;
constructorInvocation
  : userType callSuffix
  ;
when
  : 'when' ('(' (modifiers 'val' simpleName '=')? expression ')')? '{'
        whenEntry*
    '}'
  ;
whenEntry
  : whenCondition (',' whenCondition)* '->' expression
  | 'else' '->' expression
  ;
whenCondition
  : expression
  | ('in' | '!in') expression
  | ('is' | '!is') type
  ;

modifiers
  : modifier*
  ;
  modifier
    : classModifier
    | accessModifier
    | varianceAnnotation
    | memberModifier
    | annotation+
    ;
    classModifier
      : 'abstract'
      | 'final'
      | 'enum'
      | 'open'
      | 'attribute'
      ;
    memberModifier
      : 'override'
      | 'open'
      | 'final'
      | 'abstract'
      ;
    accessModifier
      : 'private'
      | 'protected'
      | 'public'
      | 'internal'
      ;
    varianceAnnotation
      : 'in'
      | 'out'
      ;

annotations
  : annotation*
  ;
annotation
  : '[' annotationEntry+ ']'
  | annotationEntry
  ;
  annotationEntry
    : SimpleName ('.' SimpleName)* typeArguments? valueArguments?
    ;

// LEXER

IntegerLiteral
  : DecimalIntegerLiteral
  | HexIntegerLiteral
  | BinaryIntegerLiteral
  ;
  fragment
  IntegerTypeSuffix
    : [L]
    ;
  fragment
  DecimalIntegerLiteral
    : DecimalNumeral IntegerTypeSuffix?
    ;
    fragment
    DecimalNumeral
      : '0'
      | NonZeroDigit (Digits? | Underscores Digits)
      ;
      fragment
      Underscores
        : '_'+
        ;
      fragment
      Digits
        : Digit (DigitOrUnderscore* Digit)?
        ;
        fragment
        Digit
          : '0'
          | NonZeroDigit
          ;
          fragment
          NonZeroDigit
            : [1-9]
            ;
        fragment
        DigitOrUnderscore
          : Digit
          | '_'
          ;
  fragment
  HexIntegerLiteral
    : HexNumeral IntegerTypeSuffix?
    ;
    fragment
    HexNumeral
      : '0' [xX] HexDigits
      ;
      fragment
      HexDigits
        : HexDigit (HexDigitOrUnderscore* HexDigit)?
        ;
        fragment
        HexDigit
          : [0-9a-fA-F]
          ;
        fragment
        HexDigitOrUnderscore
          : HexDigit
          | '_'
          ;
  fragment
  BinaryIntegerLiteral
    : BinaryNumeral IntegerTypeSuffix?
    ;
    fragment
    BinaryNumeral
      : '0' [bB] BinaryDigits
      ;
      fragment
      BinaryDigits
        : BinaryDigit (BinaryDigitOrUnderscore* BinaryDigit)?
        ;
        fragment
        BinaryDigit
          : [01]
          ;
        fragment
        BinaryDigitOrUnderscore
          : BinaryDigit
          | '_'
          ;

FloatingPointLiteral
  : DecimalFloatingPointLiteral
  | HexadecimalFloatingPointLiteral
  ;
  fragment
  FloatTypeSuffix
    : [fFdD]
    ;
  fragment
  DecimalFloatingPointLiteral
    : Digits ('.' Digits)? ExponentPart? FloatTypeSuffix?
    | '.' Digits ExponentPart? FloatTypeSuffix?
    | Digits ExponentPart FloatTypeSuffix?
    | Digits FloatTypeSuffix
    ;
    fragment
    ExponentPart
      : ExponentIndicator SignedInteger
      ;
      fragment
      ExponentIndicator
        : [eE]
        ;
      fragment
      SignedInteger
        : Sign? Digits
        ;
        fragment
        Sign
          : [+-]
          ;
  fragment
  HexadecimalFloatingPointLiteral
    : HexSignificand BinaryExponent FloatTypeSuffix?
    ;
    fragment
    HexSignificand
      : HexNumeral '.'?
      | '0' [xX] HexDigits? '.' HexDigits
      ;
    fragment
    BinaryExponent
      : BinaryExponentIndicator SignedInteger
      ;
      fragment
      BinaryExponentIndicator
        :   [pP]
        ;

CharacterLiteral
  : '\'' (~['\\]
  |	EscapeSequence) '\''
  ;

NoEscapeString
  : '"""' .*? '"""'
  ;
fragment
RegularStringPart
  : ~[\\"$\n]
  ;
EscapeSequence
  : UnicodeEscapeSequence | RegularEscapeSequence
  ;
  fragment
  UnicodeEscapeSequence
    : '\\u' HexDigit HexDigit HexDigit HexDigit
    ;
  fragment
  RegularEscapeSequence
    : '\\' ~[\n]
    ;

simpleName
  : SimpleName
  | 'get' | 'set' | 'out' | 'file' | 'init' | 'attribute'
  ;
SimpleName
  : JavaIdentifier
  | '`' (JavaLetterOrDigit | ' ')+ '`'
  ;
  fragment
  JavaIdentifier
    : JavaLetter JavaLetterOrDigit*
    ;
    fragment
    JavaLetter
      : [a-zA-Z_] // these are the "java letters" below 0xFF
      | // covers all characters above 0xFF which are not a surrogate
        ~[\u0000-\u00FF\uD800-\uDBFF]
        {Character.isJavaIdentifierStart(_input.LA(-1))}?
      | // covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
        [\uD800-\uDBFF] [\uDC00-\uDFFF]
        {Character.isJavaIdentifierStart(Character.toCodePoint((char)_input.LA(-2), (char)_input.LA(-1)))}?
      ;
    fragment
    JavaLetterOrDigit
      : [a-zA-Z0-9$_] // these are the "java letters or digits" below 0xFF
      | // covers all characters above 0xFF which are not a surrogate
        ~[\u0000-\u00FF\uD800-\uDBFF]
        {Character.isJavaIdentifierPart(_input.LA(-1))}?
      | // covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
        [\uD800-\uDBFF] [\uDC00-\uDFFF]
        {Character.isJavaIdentifierPart(Character.toCodePoint((char)_input.LA(-2), (char)_input.LA(-1)))}?
      ;

SEMI
  : [;\n]+ -> skip
  ;

WS
  : [ \t]+ -> skip
  ;

Newline
  : '\r'? '\n' -> skip
  ;

BlockComment
  : '/*' (BlockComment|.)*? '*/' -> skip
  ;

LineComment
  : '//' ~[\r\n]* -> skip
  ;
