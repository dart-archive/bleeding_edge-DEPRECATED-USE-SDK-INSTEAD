// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

grammar Dart;

options {
  backtrack = true;
  memoize = true;
  output = AST;
}


// -----------------------------------------------------------------
// Keyword definitions.
// -----------------------------------------------------------------
tokens {
  BREAK      = 'break';
  CASE       = 'case';
  CATCH      = 'catch';
  CONST      = 'const';
  CONTINUE   = 'continue';
  DEFAULT    = 'default';
  DO         = 'do';
  ELSE       = 'else';
  FALSE      = 'false';
  FINAL      = 'final';
  FINALLY    = 'finally';
  FOR        = 'for';
  IF         = 'if';
  IN         = 'in';
  NEW        = 'new';
  NULL       = 'null';
  RETURN     = 'return';
  SUPER      = 'super';
  SWITCH     = 'switch';
  THIS       = 'this';
  THROW      = 'throw';
  TRUE       = 'true';
  TRY        = 'try';
  VAR        = 'var';
  VOID       = 'void';
  WHILE      = 'while';

  // Pseudo-keywords that should also be valid identifiers.
  ABSTRACT   = 'abstract';
  ASSERT     = 'assert';
  CLASS      = 'class';
  EXTENDS    = 'extends';
  FACTORY    = 'factory';
  GET        = 'get';
  IMPLEMENTS = 'implements';
  IMPORT     = 'import';
  INTERFACE  = 'interface';
  IS         = 'is';
  LIBRARY    = 'library';
  NATIVE     = 'native';
  NEGATE     = 'negate';
  OPERATOR   = 'operator';
  SET        = 'set';
  SOURCE     = 'source';
  STATIC     = 'static';
  TYPEDEF    = 'typedef';
}

@header {
  package com.google.dart.antlr;
}

@lexer::header {
  package com.google.dart.antlr;
}

@lexer::members {
  public boolean hasErrors = false;

  @Override
  public String getErrorHeader(RecognitionException exception) {
    String sourceName = input.getSourceName();
    if (sourceName == null) {
      sourceName = "<unknown source>";
    }
    return sourceName + ":" + exception.line + ":"
        + (exception.charPositionInLine + 1) + ":";
  }

  @Override
  public void reportError(RecognitionException exception) {
    hasErrors = true;
    super.reportError(exception);
  }

  // Disable single token insertion and deletion, see:
  // http://www.antlr.org/wiki/display/ANTLR3/Error+reporting+and+recovery
  @Override
  protected Object recoverFromMismatchedToken(IntStream input,
                                              int ttype,
                                              BitSet follow)
      throws RecognitionException
  {
    throw new MismatchedTokenException(ttype, input);
  }

  private void error(String message) {
    hasErrors = true;
    int line = state.tokenStartLine;
    int column = state.tokenStartCharPositionInLine;
    String sourceName = input.getSourceName();
    if (sourceName == null) {
      sourceName = "<unknown source>";
    }
    emitErrorMessage(sourceName + ":" + line + ":" + (column + 1) + ": "
                     + message);
  }
}

@members {
  public boolean hasErrors = false;

  private boolean parseFunctionExpressions = true;
  private boolean setParseFunctionExpressions(boolean value) {
    boolean old = parseFunctionExpressions;
    parseFunctionExpressions = value;
    return old;
  }

  @Override
  public String getErrorHeader(RecognitionException exception) {
    String sourceName = input.getSourceName();
    if (sourceName == null) {
      sourceName = "<unknown source>";
    }
    return sourceName + ":" + exception.line + ":"
        + (exception.charPositionInLine + 1) + ":";
  }

  @Override
  public void reportError(RecognitionException exception) {
    hasErrors = true;
    super.reportError(exception);
  }

  // What to do with this method? Currently, error recovery
  // is brain-dead and we often get too many error messages just from
  // one error. This method represents one extreme solution to that
  // problem.
  // @Override
  // public void recover(IntStream input, RecognitionException re) {
  //   // Consume all input so we only see one parser error. This trick
  //   // does not work for the lexer.
  //   consumeUntil(input, Token.EOF);
  // }

  // Disable single token insertion and deletion, see:
  // http://www.antlr.org/wiki/display/ANTLR3/Error+reporting+and+recovery
  @Override
  protected Object recoverFromMismatchedToken(IntStream input,
                                              int ttype,
                                              BitSet follow)
      throws RecognitionException
  {
    throw new MismatchedTokenException(ttype, input);
  }

  private Token firstHiddenToken() {
    Token token = input.LT(1); // The next token.
    int index = token.getTokenIndex() - 1; // The previous token.
    if (index >= 0) {
      token = input.get(index);
    }
    // Skip whitespace, comments, etc.
    while ((index > 0) && (token.getChannel() == Token.HIDDEN_CHANNEL)) {
      token = input.get(--index);
    }
    return input.get(index + 1);
  }

  private void emitMessage(Token token, String message) {
    int line = token.getLine();
    int column = token.getCharPositionInLine();
    String sourceName = input.getSourceName();
    if (sourceName == null) {
      sourceName = "<unknown source>";
    }
    emitErrorMessage(sourceName + ":" + line + ":" + (column + 1) + ": "
                     + message);
  }

  private void warning(Token token, String message) {
    emitMessage(token, "warning: " + message);
  }

  private void legacy(Token token, String message) {
    warning(token, message);
  }

  private void semicolon() {
    error(null, "missing ';'");
  }

  private void error(Token token, String message) {
    if (token == null) {
      token = firstHiddenToken();
    }
    hasErrors = true;
    emitMessage(token, message);
  }
}

// -----------------------------------------------------------------
// Grammar productions.
// -----------------------------------------------------------------
compilationUnit
    : HASHBANG? directive* topLevelDefinition* EOF
    ;

directive
    : '#' identifier arguments ';'
    ;

topLevelDefinition
    : (CLASS)=> classDefinition
    | (INTERFACE)=> interfaceDefinition
    | (TYPEDEF)=> functionTypeAlias
    | functionDeclaration functionBodyOrNative
    | returnType? getOrSet identifier formalParameterList functionBodyOrNative
    | FINAL type? staticFinalDeclarationList ';'
    | constInitializedVariableDeclaration ';'
    ;

classDefinition
    : CLASS identifier typeParameters? superclass? interfaces?
      '{' classMemberDefinition* '}'
    | CLASS identifier typeParameters? interfaces? NATIVE STRING
      '{' classMemberDefinition* '}'
      { warning($start, "DartC: native can only be used in platform code"); }
    ;

typeParameter
    : identifier (EXTENDS type)?
    ;

typeParameters
    : '<' typeParameter (',' typeParameter)* '>'
    ;

superclass
    : EXTENDS type
    ;

interfaces
    : IMPLEMENTS typeList
    ;

superinterfaces
    : EXTENDS typeList
    ;

// This rule is organized in a way that may not be most readable, but
// gives the best error messages.
classMemberDefinition
    : declaration ';'
    | constructorDeclaration ';'
    | methodDeclaration functionBodyOrNative
    | CONST factoryConstructorDeclaration functionNative
    ;

functionBodyOrNative
    : NATIVE functionBody
      { warning($start, "native function with body only works on DartC"); }
    | functionNative
    | functionBody
    ;

functionNative
    : NATIVE STRING? ';'
      { warning($start, "native can only be used in platform code"); }
    ;

// A method, operator, or constructor (which all should be followed by
// a block of code).
methodDeclaration
    : factoryConstructorDeclaration
    | STATIC functionDeclaration
    | specialSignatureDefinition
    | functionDeclaration initializers?
    | namedConstructorDeclaration initializers?
    ;

// An abstract method/operator, a field, or const constructor (which
// all should be followed by a semicolon).
declaration
    : constantConstructorDeclaration (redirection | initializers)?
    | functionDeclaration redirection
    | namedConstructorDeclaration redirection
    | ABSTRACT specialSignatureDefinition
    | ABSTRACT functionDeclaration
    | STATIC FINAL type? staticFinalDeclarationList
    | STATIC? constInitializedVariableDeclaration
    ;

initializers
    : ':' superCallOrFieldInitializer (',' superCallOrFieldInitializer)*
    ;

redirection
    : ':' THIS ('.' identifier)? arguments
    ;

fieldInitializer
@init { boolean old = setParseFunctionExpressions(false); }
    : (THIS '.')? identifier '=' conditionalExpression
    ;
finally { setParseFunctionExpressions(old); }

superCallOrFieldInitializer
    : SUPER arguments
    | SUPER '.' identifier arguments
    | fieldInitializer
    ;

staticFinalDeclarationList
    : staticFinalDeclaration (',' staticFinalDeclaration)*
    ;

staticFinalDeclaration
    : identifier '=' constantExpression
    ;

interfaceDefinition
    : INTERFACE identifier typeParameters? superinterfaces?
      factorySpecification? '{' (interfaceMemberDefinition)* '}'
    ;

factorySpecification
   : FACTORY type
   ;

functionTypeAlias
    : TYPEDEF functionPrefix typeParameters? formalParameterList ';'
    ;

interfaceMemberDefinition
    : STATIC FINAL type? initializedIdentifierList ';'
    | functionDeclaration ';'
    | constantConstructorDeclaration ';'
    | namedConstructorDeclaration ';'
    | specialSignatureDefinition ';'
    | variableDeclaration ';'
    ;

factoryConstructorDeclaration
    : FACTORY qualified typeParameters? ('.' identifier)? formalParameterList
    ;

namedConstructorDeclaration
    : identifier '.' identifier formalParameterList
    ;

constructorDeclaration
    : identifier formalParameterList (redirection | initializers)?
    | namedConstructorDeclaration (redirection | initializers)?
    ;

constantConstructorDeclaration
    : CONST qualified formalParameterList
    ;

specialSignatureDefinition
    : STATIC? returnType? getOrSet identifier formalParameterList
    | returnType? OPERATOR userDefinableOperator formalParameterList
    ;

getOrSet
    : GET
    | SET
    ;

userDefinableOperator
    : multiplicativeOperator
    | additiveOperator
    | shiftOperator
    | relationalOperator
    | bitwiseOperator
    | '=='  // Disallow negative and === equality checks.
    | '~'   // Disallow ! operator.
    | NEGATE
    | '[' ']' { "[]".equals($text) }?
    | '[' ']' '=' { "[]=".equals($text) }?
    ;

prefixOperator
    : additiveOperator
    | negateOperator
    ;

postfixOperator
    : incrementOperator
    ;

negateOperator
    : '!'
    | '~'
    ;

multiplicativeOperator
    : '*'
    | '/'
    | '%'
    | '~/'
    ;

assignmentOperator
    : '='
    | '*='
    | '/='
    | '~/='
    | '%='
    | '+='
    | '-='
    | '<<='
    | '>' '>' '>' '=' { ">>>=".equals($text) }?
    | '>' '>' '=' { ">>=".equals($text) }?
    | '&='
    | '^='
    | '|='
    ;

additiveOperator
    : '+'
    | '-'
    ;

incrementOperator
    : '++'
    | '--'
    ;

shiftOperator
    : '<<'
    | '>' '>' '>' { ">>>".equals($text) }?
    | '>' '>' { ">>".equals($text) }?
    ;

relationalOperator
    : '>' '=' { ">=".equals($text) }?
    | '>'
    | '<='
    | '<'
    ;

equalityOperator
    : '=='
    | '!='
    | '==='
    | '!=='
    ;

bitwiseOperator
    : '&'
    | '^'
    | '|'
    ;

formalParameterList
    : '(' namedFormalParameters? ')'
    | '(' normalFormalParameter normalFormalParameterTail? ')'
    ;

normalFormalParameterTail
    : ',' namedFormalParameters
    | ',' normalFormalParameter normalFormalParameterTail?
    ;

normalFormalParameter
    : functionDeclaration
    | fieldFormalParameter
    | simpleFormalParameter
    ;

simpleFormalParameter
    : declaredIdentifier
    | identifier
    ;

fieldFormalParameter
   : finalVarOrType? THIS '.' identifier
   ;

namedFormalParameters
    : '[' defaultFormalParameter (',' defaultFormalParameter)* ']'
    ;

defaultFormalParameter
    : normalFormalParameter ('=' constantExpression)?
    ;

returnType
    : VOID
    | type
    ;

finalVarOrType
    : FINAL type?
    | VAR
    | type
    ;

// We have to introduce a separate rule for 'declared' identifiers to
// allow ANTLR to decide if the first identifier we encounter after
// final is a type or an identifier. Before this change, we used the
// production 'finalVarOrType identifier' in numerous places.
declaredIdentifier
    : FINAL type? identifier
    | VAR identifier
    | type identifier
    ;

identifier
    : IDENTIFIER_NO_DOLLAR
    | IDENTIFIER
    | ABSTRACT
    | ASSERT
    | CLASS
    | EXTENDS
    | FACTORY
    | GET
    | IMPLEMENTS
    | IMPORT
    | INTERFACE
    | IS
    | LIBRARY
    | NATIVE
    | NEGATE
    | OPERATOR
    | SET
    | SOURCE
    | STATIC
    | TYPEDEF
    ;

qualified
    : identifier ('.' identifier)?
    ;

type
    : qualified typeArguments?
    ;

typeArguments
    : '<' typeList '>'
    ;

typeList
    : type (',' type)*
    ;

block
    : '{' statements '}'
    ;

statements
    : statement*
    ;

statement
    : label* nonLabelledStatement
    ;

nonLabelledStatement
    : ('{')=> block // Guard to break tie with map literal.
    | initializedVariableDeclaration ';'
    | iterationStatement
    | selectionStatement
    | tryStatement
    | BREAK identifier? ';'
    | CONTINUE identifier? ';'
    | RETURN expression? ';'
    | THROW expression? ';'
    | expression? ';'
    | ASSERT '(' conditionalExpression ')' ';'
    | functionDeclaration functionBody
    ;

label
    : identifier ':'
    ;

iterationStatement
    : WHILE '(' expression ')' statement
    | DO statement WHILE '(' expression ')' ';'
    | FOR '(' forLoopParts ')' statement
    ;

forLoopParts
    : forInitializerStatement expression? ';' expressionList?
    | declaredIdentifier IN expression
    | identifier IN expression
    ;

forInitializerStatement
    : initializedVariableDeclaration ';'
    | expression? ';'
    ;

selectionStatement
    : IF '(' expression ')' statement ((ELSE)=> ELSE statement)?
    | SWITCH '(' expression ')' '{' switchCase* defaultCase? '}'
    ;

switchCase
    : label? (CASE expression ':')+ statements
    ;

defaultCase
    : label? (CASE expression ':')* DEFAULT ':' statements
    ;

tryStatement
    : TRY block (catchPart+ finallyPart? | finallyPart)
    ;

catchPart
    : CATCH '(' declaredIdentifier (',' declaredIdentifier)? ')' block
    ;

finallyPart
    : FINALLY block
    ;

variableDeclaration
    : declaredIdentifier (',' identifier)*
    ;

initializedVariableDeclaration
    : declaredIdentifier ('=' expression)? (',' initializedIdentifier)*
    ;

initializedIdentifierList
    : initializedIdentifier (',' initializedIdentifier)*
    ;

initializedIdentifier
    : identifier ('=' expression)?
    ;

constInitializedVariableDeclaration
    : declaredIdentifier ('=' constantExpression)?
      (',' constInitializedIdentifier)*
    ;

constInitializedIdentifier
    : identifier ('=' constantExpression)?
    ;

// The constant expression production is used to mark certain expressions
// as only being allowed to hold a compile-time constant. The grammar cannot
// express these restrictions (yet), so this will have to be enforced by a
// separate analysis phase.
constantExpression
    : expression
    ;

expression
    : assignableExpression assignmentOperator expression
    | conditionalExpression
    ;

expressionList
    : expression (',' expression)*
    ;

arguments
@init { boolean old = setParseFunctionExpressions(true); }
    : '(' argumentList? ')'
    ;
finally { setParseFunctionExpressions(old); }

argumentList
    : namedArgument (',' namedArgument)*
    | expressionList (',' namedArgument)*
    ;

namedArgument
    : label expression
    ;

assignableExpression
    : primary (arguments* assignableSelector)+
    | SUPER assignableSelector
    | identifier
    ;

conditionalExpression
    : logicalOrExpression ('?' expression ':' expression)?
    ;

logicalOrExpression
    : logicalAndExpression ('||' logicalAndExpression)*
    ;

logicalAndExpression
    : bitwiseOrExpression ('&&' bitwiseOrExpression)*
    ;

bitwiseOrExpression
    : bitwiseXorExpression ('|' bitwiseXorExpression)*
    | SUPER ('|' bitwiseXorExpression)+
    ;

bitwiseXorExpression
    : bitwiseAndExpression ('^' bitwiseAndExpression)*
    | SUPER ('^' bitwiseAndExpression)+
    ;

bitwiseAndExpression
    : equalityExpression ('&' equalityExpression)*
    | SUPER ('&' equalityExpression)+
    ;

equalityExpression
    : relationalExpression (equalityOperator relationalExpression)?
    | SUPER equalityOperator relationalExpression
    ;

relationalExpression
    : shiftExpression (isOperator type | relationalOperator shiftExpression)?
    | SUPER relationalOperator shiftExpression
    ;

isOperator
    : IS '!'?
    ;

shiftExpression
    : additiveExpression (shiftOperator additiveExpression)*
    | SUPER (shiftOperator additiveExpression)+
    ;

additiveExpression
    : multiplicativeExpression (additiveOperator multiplicativeExpression)*
    | SUPER (additiveOperator multiplicativeExpression)+
    ;

multiplicativeExpression
    : unaryExpression (multiplicativeOperator unaryExpression)*
    | SUPER (multiplicativeOperator unaryExpression)+
    ;

unaryExpression
    : postfixExpression
    | prefixOperator unaryExpression
    | negateOperator SUPER
    | '-' SUPER  // Invokes the NEGATE operator.
    | incrementOperator assignableExpression
    ;

postfixExpression
    : assignableExpression postfixOperator
    | primary selector*
    ;

selector
    : assignableSelector
    | arguments
    ;

assignableSelector
@init { boolean old = setParseFunctionExpressions(true); }
    : '[' expression ']'
    | '.' identifier
    ;
finally { setParseFunctionExpressions(old); }

primary
    : {!parseFunctionExpressions}?=> primaryNoFE
    | primaryFE
    ;

primaryFE
    : functionExpression
    | primaryNoFE
    ;

primaryNoFE
    : THIS
    | SUPER assignableSelector
    | literal
    | identifier
    | CONST? typeArguments? compoundLiteral
    | (NEW | CONST) type ('.' identifier)? arguments
    | expressionInParentheses
    ;

expressionInParentheses
@init { boolean old = setParseFunctionExpressions(true); }
    :'(' expression ')'
    ;
finally { setParseFunctionExpressions(old); }

literal
    : NULL
    | TRUE
    | FALSE
    | HEX_NUMBER
    | NUMBER
    | STRING
    ;

compoundLiteral
@init { boolean old = setParseFunctionExpressions(true); }
    : listLiteral
    | mapLiteral
    ;
finally { setParseFunctionExpressions(old); }

// The list literal syntax doesn't allow elided elements, unlike
// in ECMAScript. We do allow a trailing comma.
listLiteral
    : '[' (expressionList ','?)? ']'
    ;

mapLiteral
    : '{' (mapLiteralEntry (',' mapLiteralEntry)* ','?)? '}'
    ;

mapLiteralEntry
    : STRING ':' expression
    ;

functionExpression
    : (returnType? identifier)? formalParameterList functionExpressionBody
    ;

functionDeclaration
    : returnType? identifier formalParameterList
    ;

functionPrefix
    : returnType? identifier
    ;

functionBody
    : '=>' expression ';'
    | block
    ;

functionExpressionBody
    : '=>' expression
    | block
    ;

// -----------------------------------------------------------------
// Library files.
// -----------------------------------------------------------------
libraryUnit
    : libraryDefinition EOF
    ;

libraryDefinition
    : LIBRARY '{' libraryBody '}'
    ;

libraryBody
    : libraryImport? librarySource?
    ;

libraryImport
    : IMPORT '=' '[' importReferences? ']'
    ;

importReferences
    : importReference (',' importReference)* ','?
    ;

importReference
    : (IDENTIFIER ':')? STRING
    ;

librarySource
    : SOURCE '=' '[' sourceUrls? ']'
    ;

sourceUrls
    : STRING (',' STRING)* ','?
    ;


// -----------------------------------------------------------------
// Lexical tokens.
// -----------------------------------------------------------------
IDENTIFIER_NO_DOLLAR
    : IDENTIFIER_START_NO_DOLLAR IDENTIFIER_PART_NO_DOLLAR*
    ;

IDENTIFIER
    : IDENTIFIER_START IDENTIFIER_PART*
    ;

HEX_NUMBER
    : '0x' HEX_DIGIT+
    | '0X' HEX_DIGIT+
    ;

NUMBER
    : DIGIT+ NUMBER_OPT_FRACTIONAL_PART EXPONENT? NUMBER_OPT_ILLEGAL_END
    | '.' DIGIT+ EXPONENT? NUMBER_OPT_ILLEGAL_END
    ;

fragment NUMBER_OPT_FRACTIONAL_PART
    : ('.' DIGIT)=> ('.' DIGIT+)
    | // Empty fractional part.
    ;

fragment NUMBER_OPT_ILLEGAL_END
    : (IDENTIFIER_START)=> { error("numbers cannot contain identifiers"); }
    | // Empty illegal end (good!).
    ;

fragment HEX_DIGIT
    : 'a'..'f'
    | 'A'..'F'
    | DIGIT
    ;

fragment IDENTIFIER_START
    : IDENTIFIER_START_NO_DOLLAR
    | '$'
    ;

fragment IDENTIFIER_START_NO_DOLLAR
    : LETTER
    | '_'
    ;

fragment IDENTIFIER_PART_NO_DOLLAR
    : IDENTIFIER_START_NO_DOLLAR
    | DIGIT
    ;

fragment IDENTIFIER_PART
    : IDENTIFIER_START
    | DIGIT
    ;

// Bug 5408613: Should be Unicode characters.
fragment LETTER
    : 'a'..'z'
    | 'A'..'Z'
    ;

fragment DIGIT
    : '0'..'9'
    ;

fragment EXPONENT
    : ('e' | 'E') ('+' | '-')? DIGIT+
    ;

STRING
    : '@'? MULTI_LINE_STRING
    | SINGLE_LINE_STRING
    ;

fragment MULTI_LINE_STRING
options { greedy=false; }
    : '"""' .* '"""'
    | '\'\'\'' .* '\'\'\''
    ;

fragment SINGLE_LINE_STRING
    : '"' STRING_CONTENT_DQ* '"'
    | '\'' STRING_CONTENT_SQ* '\''
    | '@' '\'' (~( '\'' | NEWLINE ))* '\''
    | '@' '"' (~( '"' | NEWLINE ))* '"'
    ;

fragment STRING_CONTENT_DQ
    : ~( '\\' | '"' | NEWLINE )
    | '\\' ~( NEWLINE )
    ;

fragment STRING_CONTENT_SQ
    : ~( '\\' | '\'' | NEWLINE )
    | '\\' ~( NEWLINE )
    ;

fragment NEWLINE
    : '\n'
    | '\r'
    ;

BAD_STRING
    : UNTERMINATED_STRING NEWLINE { error("unterminated string"); }
    ;

fragment UNTERMINATED_STRING
    : '@'? '\'' (~( '\'' | NEWLINE ))*
    | '@'? '"' (~( '"' | NEWLINE ))*
    ;

HASHBANG
    : '#!' ~(NEWLINE)* (NEWLINE)?
    ;


// -----------------------------------------------------------------
// Whitespace and comments.
// -----------------------------------------------------------------
WHITESPACE
    : ('\t' | ' ' | NEWLINE)+ { $channel=HIDDEN; }
    ;

SINGLE_LINE_COMMENT
    : '//' ~(NEWLINE)* (NEWLINE)? { $channel=HIDDEN; }
    ;

MULTI_LINE_COMMENT
    : '/*' (options { greedy=false; } : .)* '*/' { $channel=HIDDEN; }
    ;
