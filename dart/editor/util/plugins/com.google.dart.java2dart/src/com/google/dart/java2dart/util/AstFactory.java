/*
 * Copyright (c) 2013, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.java2dart.util;

import com.google.dart.engine.ast.*;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.StringToken;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.utilities.dart.ParameterKind;

import static com.google.dart.java2dart.util.TokenFactory.token;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * The class {@code AstFactory} defines utility methods that can be used to create AST nodes. The
 * nodes that are created are complete in the sense that all of the tokens that would have been
 * associated with the nodes by a parser are also created, but the token stream is not constructed.
 */
public final class AstFactory {
  public static AdjacentStrings adjacentStrings(StringLiteral... strings) {
    return new AdjacentStrings(list(strings));
  }

  public static Annotation annotation(Identifier name) {
    return new Annotation(token(TokenType.AT), name, null, null, null);
  }

  public static Annotation annotation(Identifier name, SimpleIdentifier constructorName,
      ArgumentList arguments) {
    return new Annotation(
        token(TokenType.AT),
        name,
        token(TokenType.PERIOD),
        constructorName,
        arguments);
  }

  public static ArgumentList argumentList(Expression... arguments) {
    return argumentList(list(arguments));
  }

  public static ArgumentList argumentList(List<Expression> arguments) {
    return new ArgumentList(token(TokenType.OPEN_PAREN), arguments, token(TokenType.CLOSE_PAREN));
  }

  public static AsExpression asExpression(Expression expression, TypeName type) {
    return new AsExpression(expression, token(Keyword.AS), type);
  }

  public static AssertStatement assertStatement(Expression condition) {
    return new AssertStatement(
        token(Keyword.ASSERT),
        token(TokenType.OPEN_PAREN),
        condition,
        token(TokenType.CLOSE_PAREN),
        token(TokenType.SEMICOLON));
  }

  public static AssignmentExpression assignmentExpression(Expression leftHandSide,
      TokenType operator, Expression rightHandSide) {
    return new AssignmentExpression(leftHandSide, token(operator), rightHandSide);
  }

  public static ExpressionStatement assignmentStatement(String leftHandIdentifier,
      String rightHandIdentifier) {
    return expressionStatement(assignmentExpression(
        identifier(leftHandIdentifier),
        TokenType.EQ,
        identifier(rightHandIdentifier)));
  }

  public static BinaryExpression binaryExpression(Expression leftOperand, TokenType operator,
      Expression rightOperand) {
    return new BinaryExpression(leftOperand, token(operator), rightOperand);
  }

  public static Block block(List<Statement> statements) {
    return new Block(
        token(TokenType.OPEN_CURLY_BRACKET),
        statements,
        token(TokenType.CLOSE_CURLY_BRACKET));
  }

  public static Block block(Statement... statements) {
    return block(list(statements));
  }

  public static BlockFunctionBody blockFunctionBody(Block block) {
    return new BlockFunctionBody(null, null, block);
  }

  public static BlockFunctionBody blockFunctionBody(List<Statement> statements) {
    return new BlockFunctionBody(null, null, block(statements));
  }

  public static BlockFunctionBody blockFunctionBody(Statement... statements) {
    return new BlockFunctionBody(null, null, block(statements));
  }

  public static BooleanLiteral booleanLiteral(boolean value) {
    return new BooleanLiteral(value ? token(Keyword.TRUE) : token(Keyword.FALSE), value);
  }

  public static BreakStatement breakStatement() {
    return new BreakStatement(token(Keyword.BREAK), null, token(TokenType.SEMICOLON));
  }

  public static BreakStatement breakStatement(SimpleIdentifier label) {
    return new BreakStatement(token(Keyword.BREAK), label, token(TokenType.SEMICOLON));
  }

  public static BreakStatement breakStatement(String label) {
    return breakStatement(identifier(label));
  }

  public static IndexExpression cascadedIndexExpression(Expression index) {
    return new IndexExpression(
        token(TokenType.PERIOD_PERIOD),
        token(TokenType.OPEN_SQUARE_BRACKET),
        index,
        token(TokenType.CLOSE_SQUARE_BRACKET));
  }

  public static MethodInvocation cascadedMethodInvocation(String methodName,
      Expression... arguments) {
    return new MethodInvocation(
        null,
        token(TokenType.PERIOD_PERIOD),
        identifier(methodName),
        argumentList(arguments));
  }

  public static PropertyAccess cascadedPropertyAccess(String propertyName) {
    return new PropertyAccess(null, token(TokenType.PERIOD_PERIOD), identifier(propertyName));
  }

  public static CascadeExpression cascadeExpression(Expression target,
      Expression... cascadeSections) {
    return new CascadeExpression(target, list(cascadeSections));
  }

  public static CatchClause catchClause(String exceptionParameter, Statement... statements) {
    return catchClause(null, exceptionParameter, null, statements);
  }

  public static CatchClause catchClause(String exceptionParameter, String stackTraceParameter,
      Statement... statements) {
    return catchClause(null, exceptionParameter, stackTraceParameter, statements);
  }

  public static CatchClause catchClause(TypeName exceptionType,
      SimpleIdentifier exceptionParameter, SimpleIdentifier stackTraceParameter, Block block) {
    return new CatchClause(
        exceptionType == null ? null : token(TokenType.IDENTIFIER, "on"),
        exceptionType,
        exceptionParameter == null ? null : token(Keyword.CATCH),
        exceptionParameter == null ? null : token(TokenType.OPEN_PAREN),
        exceptionParameter,
        stackTraceParameter == null ? null : token(TokenType.COMMA),
        stackTraceParameter,
        exceptionParameter == null ? null : token(TokenType.CLOSE_PAREN),
        block);
  }

  public static CatchClause catchClause(TypeName exceptionType, Statement... statements) {
    return catchClause(exceptionType, null, null, statements);
  }

  public static CatchClause catchClause(TypeName exceptionType, String exceptionParameter,
      Statement... statements) {
    return catchClause(exceptionType, exceptionParameter, null, statements);
  }

  public static CatchClause catchClause(TypeName exceptionType, String exceptionParameter,
      String stackTraceParameter, Statement... statements) {
    return catchClause(exceptionType, exceptionParameter, stackTraceParameter, block(statements));
  }

  public static ClassDeclaration classDeclaration(Comment comment, SimpleIdentifier name,
      ExtendsClause extendsClause, WithClause withClause, ImplementsClause implementsClause,
      List<ClassMember> members) {
    return new ClassDeclaration(
        comment,
        null,
        null,
        token(Keyword.CLASS),
        name,
        null,
        extendsClause,
        withClause,
        implementsClause,
        token(TokenType.OPEN_CURLY_BRACKET),
        members,
        token(TokenType.CLOSE_CURLY_BRACKET));
  }

  public static ClassDeclaration classDeclaration(Keyword abstractKeyword, String name,
      TypeParameterList typeParameters, ExtendsClause extendsClause, WithClause withClause,
      ImplementsClause implementsClause, ClassMember... members) {
    return classDeclaration(
        abstractKeyword,
        name,
        typeParameters,
        extendsClause,
        withClause,
        implementsClause,
        list(members));
  }

  public static ClassDeclaration classDeclaration(Keyword abstractKeyword, String name,
      TypeParameterList typeParameters, ExtendsClause extendsClause, WithClause withClause,
      ImplementsClause implementsClause, List<ClassMember> members) {
    return new ClassDeclaration(
        null,
        null,
        abstractKeyword == null ? null : token(abstractKeyword),
        token(Keyword.CLASS),
        identifier(name),
        typeParameters,
        extendsClause,
        withClause,
        implementsClause,
        token(TokenType.OPEN_CURLY_BRACKET),
        members,
        token(TokenType.CLOSE_CURLY_BRACKET));
  }

  public static ClassTypeAlias classTypeAlias(String name, TypeParameterList typeParameters,
      Keyword abstractKeyword, TypeName superclass, WithClause withClause,
      ImplementsClause implementsClause) {
    return new ClassTypeAlias(
        null,
        null,
        token(Keyword.TYPEDEF),
        identifier(name),
        typeParameters,
        token(TokenType.EQ),
        abstractKeyword == null ? null : token(abstractKeyword),
        superclass,
        withClause,
        implementsClause,
        token(TokenType.SEMICOLON));
  }

  public static CompilationUnit compilationUnit() {
    return compilationUnit(null, null, null);
  }

  public static CompilationUnit compilationUnit(CompilationUnitMember... declarations) {
    return compilationUnit(null, null, list(declarations));
  }

  public static CompilationUnit compilationUnit(Directive... directives) {
    return compilationUnit(null, list(directives), null);
  }

  public static CompilationUnit compilationUnit(List<Directive> directives,
      List<CompilationUnitMember> declarations) {
    return compilationUnit(null, directives, declarations);
  }

  public static CompilationUnit compilationUnit(String scriptTag) {
    return compilationUnit(scriptTag, null, null);
  }

  public static CompilationUnit compilationUnit(String scriptTag,
      CompilationUnitMember... declarations) {
    return compilationUnit(scriptTag, null, list(declarations));
  }

  public static CompilationUnit compilationUnit(String scriptTag, Directive... directives) {
    return compilationUnit(scriptTag, list(directives), null);
  }

  public static CompilationUnit compilationUnit(String scriptTag, List<Directive> directives,
      List<CompilationUnitMember> declarations) {
    return new CompilationUnit(
        token(TokenType.EOF),
        scriptTag == null ? null : scriptTag(scriptTag),
        directives == null ? new ArrayList<Directive>() : directives,
        declarations == null ? new ArrayList<CompilationUnitMember>() : declarations,
        token(TokenType.EOF));
  }

  public static ConditionalExpression conditionalExpression(Expression condition,
      Expression thenExpression, Expression elseExpression) {
    return new ConditionalExpression(
        condition,
        token(TokenType.QUESTION),
        thenExpression,
        token(TokenType.COLON),
        elseExpression);
  }

  public static ConstructorDeclaration constructorDeclaration(Comment comment,
      Identifier returnType, SimpleIdentifier name, FormalParameterList parameters,
      List<ConstructorInitializer> initializers, FunctionBody body) {
    return new ConstructorDeclaration(//
        comment,
        null,
        null,
        null,
        null,
        returnType,
        name == null ? null : token(TokenType.PERIOD),
        name,
        parameters,
        initializers == null || initializers.isEmpty() ? null : token(TokenType.PERIOD),
        initializers == null ? new ArrayList<ConstructorInitializer>() : initializers,
        null,
        body != null ? body : emptyFunctionBody());
  }

  public static ConstructorDeclaration constructorDeclaration(Identifier returnType, String name,
      FormalParameterList parameters, List<ConstructorInitializer> initializers) {
    return new ConstructorDeclaration(
        null,
        null,
        null,
        null,
        null,
        returnType,
        name == null ? null : token(TokenType.PERIOD),
        name == null ? null : identifier(name),
        parameters,
        initializers == null || initializers.isEmpty() ? null : token(TokenType.PERIOD),
        initializers == null ? new ArrayList<ConstructorInitializer>() : initializers,
        null,
        emptyFunctionBody());
  }

  public static ConstructorDeclaration constructorDeclaration(Identifier returnType, String name,
      FormalParameterList parameters, List<ConstructorInitializer> initializers, FunctionBody body) {
    return constructorDeclaration(null, null, returnType, name, parameters, initializers, body);
  }

  public static ConstructorDeclaration constructorDeclaration(Keyword constKeyword,
      Keyword factoryKeyword, Identifier returnType, String name, FormalParameterList parameters,
      List<ConstructorInitializer> initializers, FunctionBody body) {
    return new ConstructorDeclaration(
        null,
        null,
        null,
        constKeyword == null ? null : token(constKeyword),
        factoryKeyword == null ? null : token(factoryKeyword),
        returnType,
        name == null ? null : token(TokenType.PERIOD),
        name == null ? null : identifier(name),
        parameters,
        initializers == null || initializers.isEmpty() ? null : token(TokenType.PERIOD),
        initializers == null ? new ArrayList<ConstructorInitializer>() : initializers,
        null,
        body);
  }

  public static ConstructorFieldInitializer constructorFieldInitializer(boolean prefixedWithThis,
      String fieldName, Expression expression) {
    return new ConstructorFieldInitializer(
        prefixedWithThis ? token(Keyword.THIS) : null,
        prefixedWithThis ? token(TokenType.PERIOD) : null,
        identifier(fieldName),
        token(TokenType.EQ),
        expression);
  }

  public static ConstructorName constructorName(TypeName type, String name) {
    return new ConstructorName(type, name == null ? null : token(TokenType.PERIOD), name == null
        ? null : identifier(name));
  }

  public static ContinueStatement continueStatement() {
    return new ContinueStatement(token(Keyword.CONTINUE), null, token(TokenType.SEMICOLON));
  }

  public static ContinueStatement continueStatement(String label) {
    return new ContinueStatement(
        token(Keyword.CONTINUE),
        identifier(label),
        token(TokenType.SEMICOLON));
  }

  public static DeclaredIdentifier declaredIdentifier(Keyword keyword, String identifier) {
    return declaredIdentifier(keyword, null, identifier);
  }

  public static DeclaredIdentifier declaredIdentifier(Keyword keyword, TypeName type,
      SimpleIdentifier identifier) {
    return new DeclaredIdentifier(
        null,
        null,
        keyword == null ? null : token(keyword),
        type,
        identifier);
  }

  public static DeclaredIdentifier declaredIdentifier(Keyword keyword, TypeName type,
      String identifier) {
    return declaredIdentifier(keyword, type, identifier(identifier));
  }

  public static DeclaredIdentifier declaredIdentifier(String identifier) {
    return declaredIdentifier(null, null, identifier);
  }

  public static DeclaredIdentifier declaredIdentifier(TypeName type, SimpleIdentifier identifier) {
    return declaredIdentifier(null, type, identifier);
  }

  public static DeclaredIdentifier declaredIdentifier(TypeName type, String identifier) {
    return declaredIdentifier(null, type, identifier);
  }

  public static DoStatement doStatement(Statement body, Expression condition) {
    return new DoStatement(
        token(Keyword.DO),
        body,
        token(Keyword.WHILE),
        token(TokenType.OPEN_PAREN),
        condition,
        token(TokenType.CLOSE_PAREN),
        token(TokenType.SEMICOLON));
  }

  public static DoubleLiteral doubleLiteral(double value) {
    return new DoubleLiteral(token(Double.toString(value)), value);
  }

  public static EmptyFunctionBody emptyFunctionBody() {
    return new EmptyFunctionBody(token(TokenType.SEMICOLON));
  }

  public static EmptyStatement emptyStatement() {
    return new EmptyStatement(token(TokenType.SEMICOLON));
  }

  public static Comment eolDocComment(String commentText) {
    return Comment.createDocumentationComment(new Token[] {new StringToken(
        TokenType.SINGLE_LINE_COMMENT,
        commentText,
        0)});
  }

  public static ExportDirective exportDirective(List<Annotation> metadata, String uri,
      Combinator... combinators) {
    return new ExportDirective(
        null,
        metadata,
        token(Keyword.EXPORT),
        string(uri),
        list(combinators),
        token(TokenType.SEMICOLON));
  }

  public static ExportDirective exportDirective(String uri, Combinator... combinators) {
    return exportDirective(new ArrayList<Annotation>(), uri, combinators);
  }

  public static ExpressionFunctionBody expressionFunctionBody(Expression expression) {
    return new ExpressionFunctionBody(
        null,
        token(TokenType.FUNCTION),
        expression,
        token(TokenType.SEMICOLON));
  }

  public static ExpressionStatement expressionStatement(Expression expression) {
    return new ExpressionStatement(expression, token(TokenType.SEMICOLON));
  }

  public static ExtendsClause extendsClause(TypeName type) {
    return new ExtendsClause(token(Keyword.EXTENDS), type);
  }

  public static FieldDeclaration fieldDeclaration(boolean isStatic, Keyword keyword, TypeName type,
      VariableDeclaration... variables) {
    return fieldDeclaration(null, isStatic, keyword, type, list(variables));
  }

  public static FieldDeclaration fieldDeclaration(boolean isStatic, Keyword keyword,
      VariableDeclaration... variables) {
    return fieldDeclaration(isStatic, keyword, null, variables);
  }

  public static FieldDeclaration fieldDeclaration(boolean isStatic, TypeName type,
      VariableDeclaration... variables) {
    return fieldDeclaration(null, isStatic, null, type, list(variables));
  }

  public static FieldDeclaration fieldDeclaration(Comment comment, boolean isStatic,
      Keyword keyword, TypeName type, List<VariableDeclaration> variables) {
    return new FieldDeclaration(
        comment,
        null,
        isStatic ? token(Keyword.STATIC) : null,
        variableDeclarationList(keyword, type, variables),
        token(TokenType.SEMICOLON));
  }

  public static FieldDeclaration fieldDeclaration(Comment comment, boolean isStatic,
      Keyword keyword, TypeName type, VariableDeclaration... variables) {
    return fieldDeclaration(comment, isStatic, keyword, type, list(variables));
  }

  public static FieldDeclaration fieldDeclaration(Comment comment, boolean isStatic,
      VariableDeclarationList variableDeclarationList) {
    return new FieldDeclaration(
        comment,
        null,
        isStatic ? token(Keyword.STATIC) : null,
        variableDeclarationList,
        token(TokenType.SEMICOLON));
  }

  public static FieldDeclaration fieldDeclaration(TypeName typeName,
      VariableDeclaration... variables) {
    return fieldDeclaration(false, null, typeName, variables);
  }

  public static FieldFormalParameter fieldFormalParameter(Keyword keyword, TypeName type,
      SimpleIdentifier identifier) {
    return new FieldFormalParameter(
        null,
        null,
        keyword == null ? null : token(keyword),
        type,
        token(Keyword.THIS),
        token(TokenType.PERIOD),
        identifier,
        null);
  }

  public static FieldFormalParameter fieldFormalParameter(Keyword keyword, TypeName type,
      String identifier) {
    return fieldFormalParameter(keyword, type, identifier(identifier));
  }

  public static FieldFormalParameter fieldFormalParameter(Keyword keyword, TypeName type,
      String identifier, FormalParameterList parameterList) {
    return new FieldFormalParameter(
        null,
        null,
        keyword == null ? null : token(keyword),
        type,
        token(Keyword.THIS),
        token(TokenType.PERIOD),
        identifier(identifier),
        parameterList);
  }

  public static ForEachStatement forEachStatement(DeclaredIdentifier loopParameter,
      Expression iterator, Statement body) {
    return new ForEachStatement(
        null,
        token(Keyword.FOR),
        token(TokenType.OPEN_PAREN),
        loopParameter,
        token(Keyword.IN),
        iterator,
        token(TokenType.CLOSE_PAREN),
        body);
  }

  public static FormalParameterList formalParameterList(FormalParameter... parameters) {
    return formalParameterList(list(parameters));
  }

  public static FormalParameterList formalParameterList(List<FormalParameter> parameters) {
    return new FormalParameterList(
        token(TokenType.OPEN_PAREN),
        parameters,
        null,
        null,
        token(TokenType.CLOSE_PAREN));
  }

  public static ForStatement forStatement(Expression initialization, Expression condition,
      List<Expression> updaters, Statement body) {
    return new ForStatement(
        token(Keyword.FOR),
        token(TokenType.OPEN_PAREN),
        null,
        initialization,
        token(TokenType.SEMICOLON),
        condition,
        token(TokenType.SEMICOLON),
        updaters,
        token(TokenType.CLOSE_PAREN),
        body);
  }

  public static ForStatement forStatement(VariableDeclarationList variableList,
      Expression condition, List<Expression> updaters, Statement body) {
    return new ForStatement(
        token(Keyword.FOR),
        token(TokenType.OPEN_PAREN),
        variableList,
        null,
        token(TokenType.SEMICOLON),
        condition,
        token(TokenType.SEMICOLON),
        updaters,
        token(TokenType.CLOSE_PAREN),
        body);
  }

  public static FunctionDeclaration functionDeclaration(TypeName type, Keyword keyword,
      String name, FunctionExpression functionExpression) {
    return new FunctionDeclaration(
        null,
        null,
        null,
        type,
        keyword == null ? null : token(keyword),
        identifier(name),
        functionExpression);
  }

  public static FunctionDeclarationStatement functionDeclarationStatement(TypeName type,
      Keyword keyword, String name, FunctionExpression functionExpression) {
    return new FunctionDeclarationStatement(functionDeclaration(
        type,
        keyword,
        name,
        functionExpression));
  }

  public static FunctionExpression functionExpression() {
    return new FunctionExpression(formalParameterList(), blockFunctionBody());
  }

  public static FunctionExpression functionExpression(FormalParameterList parameters,
      FunctionBody body) {
    return new FunctionExpression(parameters, body);
  }

  public static FunctionExpressionInvocation functionExpressionInvocation(Expression function,
      Expression... arguments) {
    return new FunctionExpressionInvocation(function, argumentList(arguments));
  }

  public static FunctionExpressionInvocation functionExpressionInvocation(Expression function,
      List<Expression> arguments) {
    return new FunctionExpressionInvocation(function, argumentList(arguments));
  }

  public static FunctionTypedFormalParameter functionTypedFormalParameter(TypeName returnType,
      String identifier, FormalParameter... parameters) {
    return new FunctionTypedFormalParameter(
        null,
        null,
        returnType,
        identifier(identifier),
        formalParameterList(parameters));
  }

  public static PrefixedIdentifier identifier(SimpleIdentifier prefix, SimpleIdentifier identifier) {
    return new PrefixedIdentifier(prefix, token(TokenType.PERIOD), identifier);
  }

  public static SimpleIdentifier identifier(String lexeme) {
    return lexeme != null ? new SimpleIdentifier(token(TokenType.IDENTIFIER, lexeme)) : null;
  }

  public static PrefixedIdentifier identifier(String prefix, SimpleIdentifier identifier) {
    return identifier(identifier(prefix), identifier);
  }

  public static PrefixedIdentifier identifier(String prefix, String identifier) {
    return identifier(identifier(prefix), identifier(identifier));
  }

  public static List<SimpleIdentifier> identifiers(String... names) {
    ArrayList<SimpleIdentifier> identifiers = new ArrayList<SimpleIdentifier>();
    for (String component : names) {
      identifiers.add(identifier(component));
    }
    return identifiers;
  }

  public static IfStatement ifStatement(Expression condition, Statement thenStatement) {
    return ifStatement(condition, thenStatement, null);
  }

  public static IfStatement ifStatement(Expression condition, Statement thenStatement,
      Statement elseStatement) {
    return new IfStatement(
        token(Keyword.IF),
        token(TokenType.OPEN_PAREN),
        condition,
        token(TokenType.CLOSE_PAREN),
        thenStatement,
        elseStatement == null ? null : token(Keyword.ELSE),
        elseStatement);
  }

  public static ImplementsClause implementsClause(List<TypeName> types) {
    return new ImplementsClause(token(Keyword.IMPLEMENTS), types);
  }

  public static ImplementsClause implementsClause(TypeName... types) {
    return implementsClause(list(types));
  }

  public static ImportDirective importDirective(List<Annotation> metadata, String uri,
      String prefix, Combinator... combinators) {
    return new ImportDirective(
        null,
        metadata,
        token(Keyword.IMPORT),
        string(uri),
        null,
        prefix == null ? null : token(Keyword.AS),
        prefix == null ? null : identifier(prefix),
        list(combinators),
        token(TokenType.SEMICOLON));
  }

  public static ImportDirective importDirective(String uri, String prefix,
      Combinator... combinators) {
    return importDirective(new ArrayList<Annotation>(), uri, prefix, combinators);
  }

  public static HideCombinator importHideCombinator(SimpleIdentifier... identifiers) {
    return new HideCombinator(token("hide"), list(identifiers));
  }

  public static HideCombinator importHideCombinator(String... identifiers) {
    return new HideCombinator(token("hide"), identifiers(identifiers));
  }

  public static ShowCombinator importShowCombinator(SimpleIdentifier... identifiers) {
    return new ShowCombinator(token("show"), list(identifiers));
  }

  public static ShowCombinator importShowCombinator(String... identifiers) {
    return new ShowCombinator(token("show"), identifiers(identifiers));
  }

  public static IndexExpression indexExpression(Expression array, Expression index) {
    return new IndexExpression(
        array,
        token(TokenType.OPEN_SQUARE_BRACKET),
        index,
        token(TokenType.CLOSE_SQUARE_BRACKET));
  }

  public static InstanceCreationExpression instanceCreationExpression(Keyword keyword,
      TypeName type, Expression... arguments) {
    return instanceCreationExpression(keyword, type, null, arguments);
  }

  public static InstanceCreationExpression instanceCreationExpression(Keyword keyword,
      TypeName type, List<Expression> arguments) {
    return instanceCreationExpression(keyword, type, null, arguments);
  }

  public static InstanceCreationExpression instanceCreationExpression(Keyword keyword,
      TypeName type, String identifier, Expression... arguments) {
    return instanceCreationExpression(keyword, type, identifier, list(arguments));
  }

  public static InstanceCreationExpression instanceCreationExpression(Keyword keyword,
      TypeName type, String identifier, List<Expression> arguments) {
    return new InstanceCreationExpression(
        keyword == null ? null : token(keyword),
        new ConstructorName(
            type,
            identifier == null ? null : token(TokenType.PERIOD),
            identifier == null ? null : identifier(identifier)), argumentList(arguments));
  }

  public static IntegerLiteral integer(long value) {
    return new IntegerLiteral(token(TokenType.INT, Long.toString(value)), BigInteger.valueOf(value));
  }

  public static IntegerLiteral integerHex(long value) {
    String hexString = "0x" + Long.toHexString(value).toUpperCase();
    return new IntegerLiteral(token(TokenType.INT, hexString), BigInteger.valueOf(value));
  }

  public static InterpolationExpression interpolationExpression(Expression expression) {
    return new InterpolationExpression(
        token(TokenType.STRING_INTERPOLATION_EXPRESSION),
        expression,
        token(TokenType.CLOSE_CURLY_BRACKET));
  }

  public static InterpolationExpression interpolationExpression(String identifier) {
    return new InterpolationExpression(
        token(TokenType.STRING_INTERPOLATION_IDENTIFIER),
        identifier(identifier),
        null);
  }

  public static InterpolationString interpolationString(String contents, String value) {
    return new InterpolationString(token(contents), value);
  }

  public static IsExpression isExpression(Expression expression, boolean negated, TypeName type) {
    return new IsExpression(
        expression,
        token(Keyword.IS),
        negated ? token(TokenType.BANG) : null,
        type);
  }

  public static Label label(SimpleIdentifier label) {
    return new Label(label, token(TokenType.COLON));
  }

  public static Label label(String label) {
    return new Label(identifier(label), token(TokenType.COLON));
  }

  public static LabeledStatement labeledStatement(List<Label> labels, Statement statement) {
    return new LabeledStatement(labels, statement);
  }

  public static LibraryDirective libraryDirective(List<Annotation> metadata,
      LibraryIdentifier libraryName) {
    return new LibraryDirective(
        null,
        metadata,
        token(Keyword.LIBRARY),
        libraryName,
        token(TokenType.SEMICOLON));
  }

  public static LibraryDirective libraryDirective(String... libraryNameComponents) {
    return libraryDirective(new ArrayList<Annotation>(), libraryIdentifier(libraryNameComponents));
  }

  public static LibraryIdentifier libraryIdentifier(SimpleIdentifier... components) {
    return new LibraryIdentifier(list(components));
  }

  public static LibraryIdentifier libraryIdentifier(String... components) {
    ArrayList<SimpleIdentifier> componentList = new ArrayList<SimpleIdentifier>();
    for (String component : components) {
      componentList.add(identifier(component));
    }
    return new LibraryIdentifier(componentList);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static <E> ArrayList<E> list(E... elements) {
    ArrayList<E> elementList = new ArrayList();
    for (E element : elements) {
      elementList.add(element);
    }
    return elementList;
  }

  public static ListLiteral listLiteral(Expression... elements) {
    return listLiteral(null, null, elements);
  }

  public static ListLiteral listLiteral(Keyword keyword, TypeArgumentList typeArguments,
      Expression... elements) {
    return new ListLiteral(
        keyword == null ? null : token(keyword),
        null,
        token(TokenType.OPEN_SQUARE_BRACKET),
        list(elements),
        token(TokenType.CLOSE_SQUARE_BRACKET));
  }

  public static ListLiteral listLiteral(Keyword keyword, TypeArgumentList typeArguments,
      List<Expression> elements) {
    return new ListLiteral(
        keyword == null ? null : token(keyword),
        typeArguments,
        token(TokenType.OPEN_SQUARE_BRACKET),
        elements,
        token(TokenType.CLOSE_SQUARE_BRACKET));
  }

  public static ListLiteral listLiteral(List<Expression> elements) {
    return listLiteral(null, null, elements);
  }

  public static TypeName listType(TypeName elementType, int dimensions) {
    TypeName listType = elementType;
    for (int i = 0; i < dimensions; i++) {
      listType = typeName(identifier("List"), listType);
    }
    return listType;
  }

  public static MapLiteral mapLiteral(Keyword keyword, TypeArgumentList typeArguments,
      MapLiteralEntry... entries) {
    return new MapLiteral(
        keyword == null ? null : token(keyword),
        typeArguments,
        token(TokenType.OPEN_CURLY_BRACKET),
        list(entries),
        token(TokenType.CLOSE_CURLY_BRACKET));
  }

  public static MapLiteral mapLiteral(MapLiteralEntry... entries) {
    return mapLiteral(null, null, entries);
  }

  public static MapLiteralEntry mapLiteralEntry(String key, Expression value) {
    return new MapLiteralEntry(string(key), token(TokenType.COLON), value);
  }

  public static MethodDeclaration methodDeclaration(Comment comment, boolean isStatic,
      TypeName returnType, SimpleIdentifier name, FormalParameterList parameters, FunctionBody body) {
    return new MethodDeclaration(
        comment,
        null,
        null,
        isStatic ? token(Keyword.STATIC) : null,
        returnType,
        null,
        null,
        name,
        parameters,
        body);
  }

  public static MethodDeclaration methodDeclaration(Keyword modifier, TypeName returnType,
      Keyword property, Keyword operator, SimpleIdentifier name, FormalParameterList parameters) {
    return new MethodDeclaration(null, null, token(Keyword.EXTERNAL), modifier == null ? null
        : token(modifier), returnType, property == null ? null : token(property), operator == null
        ? null : token(operator), name, parameters, emptyFunctionBody());
  }

  public static MethodDeclaration methodDeclaration(Keyword modifier, TypeName returnType,
      Keyword property, Keyword operator, SimpleIdentifier name, FormalParameterList parameters,
      FunctionBody body) {
    return new MethodDeclaration(
        null,
        null,
        null,
        modifier == null ? null : token(modifier),
        returnType,
        property == null ? null : token(property),
        operator == null ? null : token(operator),
        name,
        parameters,
        body);
  }

  public static MethodDeclaration methodDeclaration(TypeName returnType, SimpleIdentifier name,
      FormalParameterList parameters, FunctionBody body) {
    return methodDeclaration(null, returnType, null, null, name, parameters, body);
  }

  public static MethodInvocation methodInvocation(Expression target, SimpleIdentifier methodName,
      Expression... arguments) {
    return methodInvocation(target, methodName, list(arguments));
  }

  public static MethodInvocation methodInvocation(Expression target, SimpleIdentifier methodName,
      List<Expression> arguments) {
    return new MethodInvocation(
        target,
        target == null ? null : token(TokenType.PERIOD),
        methodName,
        argumentList(arguments));
  }

  public static MethodInvocation methodInvocation(Expression target, String methodName,
      Expression... arguments) {
    return methodInvocation(target, methodName, list(arguments));
  }

  public static MethodInvocation methodInvocation(Expression target, String methodName,
      List<Expression> arguments) {
    return methodInvocation(target, identifier(methodName), arguments);
  }

  public static MethodInvocation methodInvocation(SimpleIdentifier methodName,
      List<Expression> arguments) {
    return methodInvocation(null, methodName, arguments);
  }

  public static MethodInvocation methodInvocation(String methodName, Expression... arguments) {
    return methodInvocation(null, methodName, arguments);
  }

  public static MethodInvocation methodInvocation(String methodName, List<Expression> arguments) {
    return methodInvocation(null, methodName, arguments);
  }

  public static NamedExpression namedExpression(String label, Expression expression) {
    return new NamedExpression(label(label), expression);
  }

  public static DefaultFormalParameter namedFormalParameter(NormalFormalParameter parameter,
      Expression expression) {
    return new DefaultFormalParameter(parameter, ParameterKind.NAMED, expression == null ? null
        : token(TokenType.COLON), expression);
  }

  public static NullLiteral nullLiteral() {
    return new NullLiteral(token(Keyword.NULL));
  }

  public static ParenthesizedExpression parenthesizedExpression(Expression expression) {
    return new ParenthesizedExpression(
        token(TokenType.OPEN_PAREN),
        expression,
        token(TokenType.CLOSE_PAREN));
  }

  public static PartDirective partDirective(List<Annotation> metadata, String url) {
    return new PartDirective(
        null,
        metadata,
        token(Keyword.PART),
        string(url),
        token(TokenType.SEMICOLON));
  }

  public static PartDirective partDirective(String url) {
    return partDirective(new ArrayList<Annotation>(), url);
  }

  public static PartOfDirective partOfDirective(LibraryIdentifier libraryName) {
    return partOfDirective(new ArrayList<Annotation>(), libraryName);
  }

  public static PartOfDirective partOfDirective(List<Annotation> metadata,
      LibraryIdentifier libraryName) {
    return new PartOfDirective(
        null,
        metadata,
        token(Keyword.PART),
        token("of"),
        libraryName,
        token(TokenType.SEMICOLON));
  }

  public static DefaultFormalParameter positionalFormalParameter(NormalFormalParameter parameter,
      Expression expression) {
    return new DefaultFormalParameter(parameter, ParameterKind.POSITIONAL, expression == null
        ? null : token(TokenType.EQ), expression);
  }

  public static PostfixExpression postfixExpression(Expression expression, TokenType operator) {
    return new PostfixExpression(expression, token(operator));
  }

  public static PrefixExpression prefixExpression(TokenType operator, Expression expression) {
    return new PrefixExpression(token(operator), expression);
  }

  public static PropertyAccess propertyAccess(Expression target, SimpleIdentifier propertyName) {
    return new PropertyAccess(target, token(TokenType.PERIOD), propertyName);
  }

  public static PropertyAccess propertyAccess(Expression target, String propertyName) {
    return propertyAccess(target, identifier(propertyName));
  }

  public static RedirectingConstructorInvocation redirectingConstructorInvocation(
      Expression... arguments) {
    return redirectingConstructorInvocation(null, arguments);
  }

  public static RedirectingConstructorInvocation redirectingConstructorInvocation(
      String constructorName, Expression... arguments) {
    return redirectingConstructorInvocation(constructorName, list(arguments));
  }

  public static RedirectingConstructorInvocation redirectingConstructorInvocation(
      String constructorName, List<Expression> arguments) {
    return new RedirectingConstructorInvocation(
        token(Keyword.THIS),
        constructorName == null ? null : token(TokenType.PERIOD),
        constructorName == null ? null : identifier(constructorName),
        argumentList(arguments));
  }

  public static ReturnStatement returnStatement() {
    return returnStatement(null);
  }

  public static ReturnStatement returnStatement(Expression expression) {
    return new ReturnStatement(token(Keyword.RETURN), expression, token(TokenType.SEMICOLON));
  }

  public static ScriptTag scriptTag(String scriptTag) {
    return new ScriptTag(token(scriptTag));
  }

  public static SimpleFormalParameter simpleFormalParameter(Keyword keyword, String parameterName) {
    return simpleFormalParameter(keyword, null, parameterName);
  }

  public static SimpleFormalParameter simpleFormalParameter(Keyword keyword, TypeName type,
      SimpleIdentifier parameterName) {
    return new SimpleFormalParameter(
        null,
        null,
        keyword == null ? null : token(keyword),
        type,
        parameterName);
  }

  public static SimpleFormalParameter simpleFormalParameter(Keyword keyword, TypeName type,
      String parameterName) {
    return simpleFormalParameter(keyword, type, identifier(parameterName));
  }

  public static SimpleFormalParameter simpleFormalParameter(String parameterName) {
    return simpleFormalParameter(null, null, parameterName);
  }

  public static SimpleFormalParameter simpleFormalParameter(TypeName type,
      SimpleIdentifier parameterName) {
    return simpleFormalParameter(null, type, parameterName);
  }

  public static SimpleFormalParameter simpleFormalParameter(TypeName type, String parameterName) {
    return simpleFormalParameter(null, type, parameterName);
  }

  public static StringInterpolation string(InterpolationElement... elements) {
    return string(list(elements));
  }

  public static StringInterpolation string(List<InterpolationElement> elements) {
    return new StringInterpolation(elements);
  }

  public static SimpleStringLiteral string(String content) {
    return new SimpleStringLiteral(token("'" + content + "'"), content);
  }

  public static SuperConstructorInvocation superConstructorInvocation(Expression... arguments) {
    return superConstructorInvocation(null, arguments);
  }

  public static SuperConstructorInvocation superConstructorInvocation(List<Expression> arguments) {
    return superConstructorInvocation(null, arguments);
  }

  public static SuperConstructorInvocation superConstructorInvocation(String name,
      Expression... arguments) {
    return superConstructorInvocation(name, list(arguments));
  }

  public static SuperConstructorInvocation superConstructorInvocation(String name,
      List<Expression> arguments) {
    return new SuperConstructorInvocation(token(Keyword.SUPER), name == null ? null
        : token(TokenType.PERIOD), name == null ? null : identifier(name), argumentList(arguments));
  }

  public static SuperExpression superExpression() {
    return new SuperExpression(token(Keyword.SUPER));
  }

  public static SwitchCase switchCase(Expression expression, Statement... statements) {
    return switchCase(new ArrayList<Label>(), expression, statements);
  }

  public static SwitchCase switchCase(List<Label> labels, Expression expression,
      Statement... statements) {
    return new SwitchCase(
        labels,
        token(Keyword.CASE),
        expression,
        token(TokenType.COLON),
        list(statements));
  }

  public static SwitchDefault switchDefault(List<Label> labels, Statement... statements) {
    return new SwitchDefault(
        labels,
        token(Keyword.DEFAULT),
        token(TokenType.COLON),
        list(statements));
  }

  public static SwitchDefault switchDefault(Statement... statements) {
    return switchDefault(new ArrayList<Label>(), statements);
  }

  public static SwitchStatement switchStatement(Expression expression, SwitchMember... members) {
    return new SwitchStatement(
        token(Keyword.SWITCH),
        token(TokenType.OPEN_PAREN),
        expression,
        token(TokenType.CLOSE_PAREN),
        token(TokenType.OPEN_CURLY_BRACKET),
        list(members),
        token(TokenType.CLOSE_CURLY_BRACKET));
  }

  public static ThisExpression thisExpression() {
    return new ThisExpression(token(Keyword.THIS));
  }

  public static ThrowExpression throwExpression() {
    return throwExpression(null);
  }

  public static ThrowExpression throwExpression(Expression expression) {
    return new ThrowExpression(token(Keyword.THROW), expression);
  }

  public static TopLevelVariableDeclaration topLevelVariableDeclaration(Keyword keyword,
      TypeName type, VariableDeclaration... variables) {
    return new TopLevelVariableDeclaration(null, null, variableDeclarationList(
        keyword,
        type,
        variables), token(TokenType.SEMICOLON));
  }

  public static TopLevelVariableDeclaration topLevelVariableDeclaration(Keyword keyword,
      VariableDeclaration... variables) {
    return new TopLevelVariableDeclaration(null, null, variableDeclarationList(
        keyword,
        null,
        variables), token(TokenType.SEMICOLON));
  }

  public static TryStatement tryStatement(Block body, Block finallyClause) {
    return tryStatement(body, new ArrayList<CatchClause>(), finallyClause);
  }

  public static TryStatement tryStatement(Block body, CatchClause... catchClauses) {
    return tryStatement(body, list(catchClauses), null);
  }

  public static TryStatement tryStatement(Block body, List<CatchClause> catchClauses,
      Block finallyClause) {
    return new TryStatement(token(Keyword.TRY), body, catchClauses, finallyClause == null ? null
        : token(Keyword.FINALLY), finallyClause);
  }

  public static FunctionTypeAlias typeAlias(TypeName returnType, String name,
      TypeParameterList typeParameters, FormalParameterList parameters) {
    return new FunctionTypeAlias(
        null,
        null,
        token(Keyword.TYPEDEF),
        returnType,
        identifier(name),
        typeParameters,
        parameters,
        token(TokenType.SEMICOLON));
  }

  public static TypeArgumentList typeArgumentList(List<TypeName> typeNames) {
    return new TypeArgumentList(token(TokenType.LT), typeNames, token(TokenType.GT));
  }

  public static TypeArgumentList typeArgumentList(TypeName... typeNames) {
    return typeArgumentList(list(typeNames));
  }

  public static TypeName typeName(Identifier name, List<TypeName> arguments) {
    if (arguments == null || arguments.isEmpty()) {
      return new TypeName(name, null);
    }
    return new TypeName(name, typeArgumentList(arguments));
  }

  public static TypeName typeName(Identifier name, TypeName... arguments) {
    if (arguments.length == 0) {
      return new TypeName(name, null);
    }
    return new TypeName(name, typeArgumentList(arguments));
  }

  public static TypeName typeName(String name, TypeName... arguments) {
    if (arguments.length == 0) {
      return new TypeName(identifier(name), null);
    }
    return new TypeName(identifier(name), typeArgumentList(arguments));
  }

  public static TypeParameter typeParameter(SimpleIdentifier name, TypeName bound) {
    return new TypeParameter(null, null, name, token(Keyword.EXTENDS), bound);
  }

  public static TypeParameter typeParameter(String name) {
    return new TypeParameter(null, null, identifier(name), null, null);
  }

  public static TypeParameter typeParameter(String name, TypeName bound) {
    return typeParameter(identifier(name), bound);
  }

  public static TypeParameterList typeParameterList(List<TypeParameter> typeParameters) {
    return new TypeParameterList(token(TokenType.LT), typeParameters, token(TokenType.GT));
  }

  public static TypeParameterList typeParameterList(String... typeNames) {
    ArrayList<TypeParameter> typeParameters = new ArrayList<TypeParameter>();
    for (String typeName : typeNames) {
      typeParameters.add(typeParameter(typeName));
    }
    return typeParameterList(typeParameters);
  }

  public static VariableDeclaration variableDeclaration(SimpleIdentifier name) {
    return variableDeclaration(name, null);
  }

  public static VariableDeclaration variableDeclaration(SimpleIdentifier name,
      Expression initializer) {
    return new VariableDeclaration(null, null, name, token(TokenType.EQ), initializer);
  }

  public static VariableDeclaration variableDeclaration(String name) {
    return new VariableDeclaration(null, null, identifier(name), null, null);
  }

  public static VariableDeclaration variableDeclaration(String name, Expression initializer) {
    return variableDeclaration(identifier(name), initializer);
  }

  public static VariableDeclarationList variableDeclarationList(Keyword keyword, TypeName type,
      List<VariableDeclaration> variables) {
    return new VariableDeclarationList(
        null,
        null,
        keyword == null ? null : token(keyword),
        type,
        variables);
  }

  public static VariableDeclarationList variableDeclarationList(Keyword keyword, TypeName type,
      VariableDeclaration... variables) {
    return variableDeclarationList(keyword, type, list(variables));
  }

  public static VariableDeclarationList variableDeclarationList(Keyword keyword,
      VariableDeclaration... variables) {
    return variableDeclarationList(keyword, null, variables);
  }

  public static VariableDeclarationStatement variableDeclarationStatement(Keyword keyword,
      TypeName type, VariableDeclaration... variables) {
    return variableDeclarationStatement(variableDeclarationList(keyword, type, variables));
  }

  public static VariableDeclarationStatement variableDeclarationStatement(Keyword keyword,
      VariableDeclaration... variables) {
    return variableDeclarationStatement(keyword, null, variables);
  }

  public static VariableDeclarationStatement variableDeclarationStatement(
      VariableDeclarationList variableDeclarationList) {
    return new VariableDeclarationStatement(variableDeclarationList, token(TokenType.SEMICOLON));
  }

  public static WhileStatement whileStatement(Expression condition, Statement body) {
    return new WhileStatement(
        token(Keyword.WHILE),
        token(TokenType.OPEN_PAREN),
        condition,
        token(TokenType.CLOSE_PAREN),
        body);
  }

  public static WithClause withClause(TypeName... types) {
    return new WithClause(token(Keyword.WITH), list(types));
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private AstFactory() {
  }
}
