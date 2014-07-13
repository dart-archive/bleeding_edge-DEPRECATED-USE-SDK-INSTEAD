/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.engine.ast;

import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.utilities.dart.ParameterKind;
import com.google.dart.engine.utilities.translation.DartOmit;
import com.google.dart.engine.utilities.translation.DartOptional;

import static com.google.dart.engine.scanner.TokenFactory.tokenFromKeyword;
import static com.google.dart.engine.scanner.TokenFactory.tokenFromString;
import static com.google.dart.engine.scanner.TokenFactory.tokenFromType;
import static com.google.dart.engine.scanner.TokenFactory.tokenFromTypeAndString;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * The class {@code AstFactory} defines utility methods that can be used to create AST nodes. The
 * nodes that are created are complete in the sense that all of the tokens that would have been
 * associated with the nodes by a parser are also created, but the token stream is not constructed.
 * None of the nodes are resolved.
 * <p>
 * The general pattern is for the name of the factory method to be the same as the name of the class
 * of AST node being created. There are two notable exceptions. The first is for methods creating
 * nodes that are part of a cascade expression. These methods are all prefixed with 'cascaded'. The
 * second is places where a shorter name seemed unambiguous and easier to read, such as using
 * 'identifier' rather than 'prefixedIdentifier', or 'integer' rather than 'integerLiteral'.
 */
public final class AstFactory {
  public static AdjacentStrings adjacentStrings(StringLiteral... strings) {
    return new AdjacentStrings(list(strings));
  }

  public static Annotation annotation(Identifier name) {
    return new Annotation(tokenFromType(TokenType.AT), name, null, null, null);
  }

  public static Annotation annotation(Identifier name, SimpleIdentifier constructorName,
      ArgumentList arguments) {
    return new Annotation(
        tokenFromType(TokenType.AT),
        name,
        tokenFromType(TokenType.PERIOD),
        constructorName,
        arguments);
  }

  public static ArgumentList argumentList(Expression... arguments) {
    return new ArgumentList(
        tokenFromType(TokenType.OPEN_PAREN),
        list(arguments),
        tokenFromType(TokenType.CLOSE_PAREN));
  }

  public static AsExpression asExpression(Expression expression, TypeName type) {
    return new AsExpression(expression, tokenFromKeyword(Keyword.AS), type);
  }

  public static AssertStatement assertStatement(Expression condition) {
    return new AssertStatement(
        tokenFromKeyword(Keyword.ASSERT),
        tokenFromType(TokenType.OPEN_PAREN),
        condition,
        tokenFromType(TokenType.CLOSE_PAREN),
        tokenFromType(TokenType.SEMICOLON));
  }

  public static AssignmentExpression assignmentExpression(Expression leftHandSide,
      TokenType operator, Expression rightHandSide) {
    return new AssignmentExpression(leftHandSide, tokenFromType(operator), rightHandSide);
  }

  public static BlockFunctionBody asyncBlockFunctionBody(Statement... statements) {
    return new BlockFunctionBody(
        tokenFromTypeAndString(TokenType.IDENTIFIER, "async"),
        null,
        block(statements));
  }

  public static ExpressionFunctionBody asyncExpressionFunctionBody(Expression expression) {
    return new ExpressionFunctionBody(
        tokenFromTypeAndString(TokenType.IDENTIFIER, "async"),
        tokenFromType(TokenType.FUNCTION),
        expression,
        tokenFromType(TokenType.SEMICOLON));
  }

  public static BlockFunctionBody asyncGeneratorBlockFunctionBody(Statement... statements) {
    return new BlockFunctionBody(
        tokenFromTypeAndString(TokenType.IDENTIFIER, "async"),
        tokenFromType(TokenType.STAR),
        block(statements));
  }

  public static AwaitExpression awaitExpression(Expression expression) {
    return new AwaitExpression(
        tokenFromTypeAndString(TokenType.IDENTIFIER, "await"),
        expression,
        tokenFromType(TokenType.SEMICOLON));
  }

  public static BinaryExpression binaryExpression(Expression leftOperand, TokenType operator,
      Expression rightOperand) {
    return new BinaryExpression(leftOperand, tokenFromType(operator), rightOperand);
  }

  public static Block block(Statement... statements) {
    return new Block(
        tokenFromType(TokenType.OPEN_CURLY_BRACKET),
        list(statements),
        tokenFromType(TokenType.CLOSE_CURLY_BRACKET));
  }

  public static BlockFunctionBody blockFunctionBody(Block block) {
    return new BlockFunctionBody(null, null, block);
  }

  public static BlockFunctionBody blockFunctionBody(Statement... statements) {
    return new BlockFunctionBody(null, null, block(statements));
  }

  public static BooleanLiteral booleanLiteral(boolean value) {
    return new BooleanLiteral(value ? tokenFromKeyword(Keyword.TRUE)
        : tokenFromKeyword(Keyword.FALSE), value);
  }

  public static BreakStatement breakStatement() {
    return new BreakStatement(
        tokenFromKeyword(Keyword.BREAK),
        null,
        tokenFromType(TokenType.SEMICOLON));
  }

  public static BreakStatement breakStatement(String label) {
    return new BreakStatement(
        tokenFromKeyword(Keyword.BREAK),
        identifier(label),
        tokenFromType(TokenType.SEMICOLON));
  }

  public static IndexExpression cascadedIndexExpression(Expression index) {
    return new IndexExpression(
        tokenFromType(TokenType.PERIOD_PERIOD),
        tokenFromType(TokenType.OPEN_SQUARE_BRACKET),
        index,
        tokenFromType(TokenType.CLOSE_SQUARE_BRACKET));
  }

  public static MethodInvocation cascadedMethodInvocation(String methodName,
      Expression... arguments) {
    return new MethodInvocation(
        null,
        tokenFromType(TokenType.PERIOD_PERIOD),
        identifier(methodName),
        argumentList(arguments));
  }

  public static PropertyAccess cascadedPropertyAccess(String propertyName) {
    return new PropertyAccess(
        null,
        tokenFromType(TokenType.PERIOD_PERIOD),
        identifier(propertyName));
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

  public static CatchClause catchClause(TypeName exceptionType, Statement... statements) {
    return catchClause(exceptionType, null, null, statements);
  }

  public static CatchClause catchClause(TypeName exceptionType, String exceptionParameter,
      Statement... statements) {
    return catchClause(exceptionType, exceptionParameter, null, statements);
  }

  public static CatchClause catchClause(TypeName exceptionType, String exceptionParameter,
      String stackTraceParameter, Statement... statements) {
    return new CatchClause(
        exceptionType == null ? null : tokenFromTypeAndString(TokenType.IDENTIFIER, "on"),
        exceptionType,
        exceptionParameter == null ? null : tokenFromKeyword(Keyword.CATCH),
        exceptionParameter == null ? null : tokenFromType(TokenType.OPEN_PAREN),
        exceptionParameter == null ? null : identifier(exceptionParameter),
        stackTraceParameter == null ? null : tokenFromType(TokenType.COMMA),
        stackTraceParameter == null ? null : identifier(stackTraceParameter),
        exceptionParameter == null ? null : tokenFromType(TokenType.CLOSE_PAREN),
        block(statements));
  }

  public static ClassDeclaration classDeclaration(Keyword abstractKeyword, String name,
      TypeParameterList typeParameters, ExtendsClause extendsClause, WithClause withClause,
      ImplementsClause implementsClause, ClassMember... members) {
    return new ClassDeclaration(
        null,
        null,
        abstractKeyword == null ? null : tokenFromKeyword(abstractKeyword),
        tokenFromKeyword(Keyword.CLASS),
        identifier(name),
        typeParameters,
        extendsClause,
        withClause,
        implementsClause,
        tokenFromType(TokenType.OPEN_CURLY_BRACKET),
        list(members),
        tokenFromType(TokenType.CLOSE_CURLY_BRACKET));
  }

  public static ClassTypeAlias classTypeAlias(String name, TypeParameterList typeParameters,
      Keyword abstractKeyword, TypeName superclass, WithClause withClause,
      ImplementsClause implementsClause) {
    return new ClassTypeAlias(
        null,
        null,
        tokenFromKeyword(Keyword.CLASS),
        identifier(name),
        typeParameters,
        tokenFromType(TokenType.EQ),
        abstractKeyword == null ? null : tokenFromKeyword(abstractKeyword),
        superclass,
        withClause,
        implementsClause,
        tokenFromType(TokenType.SEMICOLON));
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
        tokenFromType(TokenType.EOF),
        scriptTag == null ? null : scriptTag(scriptTag),
        directives == null ? new ArrayList<Directive>() : directives,
        declarations == null ? new ArrayList<CompilationUnitMember>() : declarations,
        tokenFromType(TokenType.EOF));
  }

  public static ConditionalExpression conditionalExpression(Expression condition,
      Expression thenExpression, Expression elseExpression) {
    return new ConditionalExpression(
        condition,
        tokenFromType(TokenType.QUESTION),
        thenExpression,
        tokenFromType(TokenType.COLON),
        elseExpression);
  }

  public static ConstructorDeclaration constructorDeclaration(Identifier returnType, String name,
      FormalParameterList parameters, List<ConstructorInitializer> initializers) {
    return new ConstructorDeclaration(
        null,
        null,
        tokenFromKeyword(Keyword.EXTERNAL),
        null,
        null,
        returnType,
        name == null ? null : tokenFromType(TokenType.PERIOD),
        name == null ? null : identifier(name),
        parameters,
        initializers == null || initializers.isEmpty() ? null : tokenFromType(TokenType.PERIOD),
        initializers == null ? new ArrayList<ConstructorInitializer>() : initializers,
        null,
        emptyFunctionBody());
  }

  public static ConstructorDeclaration constructorDeclaration(Keyword constKeyword,
      Keyword factoryKeyword, Identifier returnType, String name, FormalParameterList parameters,
      List<ConstructorInitializer> initializers, FunctionBody body) {
    return new ConstructorDeclaration(
        null,
        null,
        null,
        constKeyword == null ? null : tokenFromKeyword(constKeyword),
        factoryKeyword == null ? null : tokenFromKeyword(factoryKeyword),
        returnType,
        name == null ? null : tokenFromType(TokenType.PERIOD),
        name == null ? null : identifier(name),
        parameters,
        initializers == null || initializers.isEmpty() ? null : tokenFromType(TokenType.PERIOD),
        initializers == null ? new ArrayList<ConstructorInitializer>() : initializers,
        null,
        body);
  }

  public static ConstructorFieldInitializer constructorFieldInitializer(boolean prefixedWithThis,
      String fieldName, Expression expression) {
    return new ConstructorFieldInitializer(
        prefixedWithThis ? tokenFromKeyword(Keyword.THIS) : null,
        prefixedWithThis ? tokenFromType(TokenType.PERIOD) : null,
        identifier(fieldName),
        tokenFromType(TokenType.EQ),
        expression);
  }

  public static ConstructorName constructorName(TypeName type, String name) {
    return new ConstructorName(
        type,
        name == null ? null : tokenFromType(TokenType.PERIOD),
        name == null ? null : identifier(name));
  }

  @DartOmit
  public static ContinueStatement continueStatement() {
    return continueStatement(null);
  }

  public static ContinueStatement continueStatement(@DartOptional String label) {
    SimpleIdentifier labelNode = label == null ? null : identifier(label);
    return new ContinueStatement(
        tokenFromKeyword(Keyword.CONTINUE),
        labelNode,
        tokenFromType(TokenType.SEMICOLON));
  }

  public static DeclaredIdentifier declaredIdentifier(Keyword keyword, String identifier) {
    return declaredIdentifier(keyword, null, identifier);
  }

  public static DeclaredIdentifier declaredIdentifier(Keyword keyword, TypeName type,
      String identifier) {
    return new DeclaredIdentifier(
        null,
        null,
        keyword == null ? null : tokenFromKeyword(keyword),
        type,
        identifier(identifier));
  }

  public static DeclaredIdentifier declaredIdentifier(String identifier) {
    return declaredIdentifier(null, null, identifier);
  }

  public static DeclaredIdentifier declaredIdentifier(TypeName type, String identifier) {
    return declaredIdentifier(null, type, identifier);
  }

  public static DoStatement doStatement(Statement body, Expression condition) {
    return new DoStatement(
        tokenFromKeyword(Keyword.DO),
        body,
        tokenFromKeyword(Keyword.WHILE),
        tokenFromType(TokenType.OPEN_PAREN),
        condition,
        tokenFromType(TokenType.CLOSE_PAREN),
        tokenFromType(TokenType.SEMICOLON));
  }

  public static DoubleLiteral doubleLiteral(double value) {
    return new DoubleLiteral(tokenFromString(Double.toString(value)), value);
  }

  public static EmptyFunctionBody emptyFunctionBody() {
    return new EmptyFunctionBody(tokenFromType(TokenType.SEMICOLON));
  }

  public static EmptyStatement emptyStatement() {
    return new EmptyStatement(tokenFromType(TokenType.SEMICOLON));
  }

  public static EnumDeclaration enumDeclaration(SimpleIdentifier name,
      EnumConstantDeclaration... constants) {
    return new EnumDeclaration(
        null,
        null,
        tokenFromKeyword(Keyword.ENUM),
        name,
        tokenFromType(TokenType.OPEN_CURLY_BRACKET),
        list(constants),
        tokenFromType(TokenType.CLOSE_CURLY_BRACKET));
  }

  public static EnumDeclaration enumDeclaration(String name, String... constantNames) {
    int count = constantNames.length;
    EnumConstantDeclaration[] constants = new EnumConstantDeclaration[count];
    for (int i = 0; i < count; i++) {
      constants[i] = new EnumConstantDeclaration(null, null, identifier(constantNames[i]));
    }
    return enumDeclaration(identifier(name), constants);
  }

  public static ExportDirective exportDirective(List<Annotation> metadata, String uri,
      Combinator... combinators) {
    return new ExportDirective(
        null,
        metadata,
        tokenFromKeyword(Keyword.EXPORT),
        string(uri),
        list(combinators),
        tokenFromType(TokenType.SEMICOLON));
  }

  public static ExportDirective exportDirective(String uri, Combinator... combinators) {
    return exportDirective(new ArrayList<Annotation>(), uri, combinators);
  }

  public static ExpressionFunctionBody expressionFunctionBody(Expression expression) {
    return new ExpressionFunctionBody(
        null,
        tokenFromType(TokenType.FUNCTION),
        expression,
        tokenFromType(TokenType.SEMICOLON));
  }

  public static ExpressionStatement expressionStatement(Expression expression) {
    return new ExpressionStatement(expression, tokenFromType(TokenType.SEMICOLON));
  }

  public static ExtendsClause extendsClause(TypeName type) {
    return new ExtendsClause(tokenFromKeyword(Keyword.EXTENDS), type);
  }

  public static FieldDeclaration fieldDeclaration(boolean isStatic, Keyword keyword, TypeName type,
      VariableDeclaration... variables) {
    return new FieldDeclaration(
        null,
        null,
        isStatic ? tokenFromKeyword(Keyword.STATIC) : null,
        variableDeclarationList(keyword, type, variables),
        tokenFromType(TokenType.SEMICOLON));
  }

  public static FieldDeclaration fieldDeclaration(boolean isStatic, Keyword keyword,
      VariableDeclaration... variables) {
    return fieldDeclaration(isStatic, keyword, null, variables);
  }

  @DartOmit
  public static FieldFormalParameter fieldFormalParameter(Keyword keyword, TypeName type,
      String identifier) {
    return fieldFormalParameter(keyword, type, identifier, null);
  }

  public static FieldFormalParameter fieldFormalParameter(Keyword keyword, TypeName type,
      String identifier, @DartOptional FormalParameterList parameterList) {
    return new FieldFormalParameter(
        null,
        null,
        keyword == null ? null : tokenFromKeyword(keyword),
        type,
        tokenFromKeyword(Keyword.THIS),
        tokenFromType(TokenType.PERIOD),
        identifier(identifier),
        parameterList);
  }

  public static FieldFormalParameter fieldFormalParameter(String identifier) {
    return fieldFormalParameter(null, null, identifier);
  }

  public static ForEachStatement forEachStatement(DeclaredIdentifier loopVariable,
      Expression iterator, Statement body) {
    return new ForEachStatement(
        null,
        tokenFromKeyword(Keyword.FOR),
        tokenFromType(TokenType.OPEN_PAREN),
        loopVariable,
        tokenFromKeyword(Keyword.IN),
        iterator,
        tokenFromType(TokenType.CLOSE_PAREN),
        body);
  }

  public static ForEachStatement forEachStatement(SimpleIdentifier identifier, Expression iterator,
      Statement body) {
    return new ForEachStatement(
        null,
        tokenFromKeyword(Keyword.FOR),
        tokenFromType(TokenType.OPEN_PAREN),
        identifier,
        tokenFromKeyword(Keyword.IN),
        iterator,
        tokenFromType(TokenType.CLOSE_PAREN),
        body);
  }

  public static FormalParameterList formalParameterList(FormalParameter... parameters) {
    return new FormalParameterList(
        tokenFromType(TokenType.OPEN_PAREN),
        list(parameters),
        null,
        null,
        tokenFromType(TokenType.CLOSE_PAREN));
  }

  public static ForStatement forStatement(Expression initialization, Expression condition,
      List<Expression> updaters, Statement body) {
    return new ForStatement(
        tokenFromKeyword(Keyword.FOR),
        tokenFromType(TokenType.OPEN_PAREN),
        null,
        initialization,
        tokenFromType(TokenType.SEMICOLON),
        condition,
        tokenFromType(TokenType.SEMICOLON),
        updaters,
        tokenFromType(TokenType.CLOSE_PAREN),
        body);
  }

  public static ForStatement forStatement(VariableDeclarationList variableList,
      Expression condition, List<Expression> updaters, Statement body) {
    return new ForStatement(
        tokenFromKeyword(Keyword.FOR),
        tokenFromType(TokenType.OPEN_PAREN),
        variableList,
        null,
        tokenFromType(TokenType.SEMICOLON),
        condition,
        tokenFromType(TokenType.SEMICOLON),
        updaters,
        tokenFromType(TokenType.CLOSE_PAREN),
        body);
  }

  public static FunctionDeclaration functionDeclaration(TypeName type, Keyword keyword,
      String name, FunctionExpression functionExpression) {
    return new FunctionDeclaration(null, null, null, type, keyword == null ? null
        : tokenFromKeyword(keyword), identifier(name), functionExpression);
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

  public static FunctionTypedFormalParameter functionTypedFormalParameter(TypeName returnType,
      String identifier, FormalParameter... parameters) {
    return new FunctionTypedFormalParameter(
        null,
        null,
        returnType,
        identifier(identifier),
        formalParameterList(parameters));
  }

  public static HideCombinator hideCombinator(SimpleIdentifier... identifiers) {
    return new HideCombinator(tokenFromString("hide"), list(identifiers));
  }

  public static HideCombinator hideCombinator(String... identifiers) {
    ArrayList<SimpleIdentifier> identifierList = new ArrayList<SimpleIdentifier>();
    for (String identifier : identifiers) {
      identifierList.add(identifier(identifier));
    }
    return new HideCombinator(tokenFromString("hide"), identifierList);
  }

  public static PrefixedIdentifier identifier(SimpleIdentifier prefix, SimpleIdentifier identifier) {
    return new PrefixedIdentifier(prefix, tokenFromType(TokenType.PERIOD), identifier);
  }

  public static SimpleIdentifier identifier(String lexeme) {
    return new SimpleIdentifier(tokenFromTypeAndString(TokenType.IDENTIFIER, lexeme));
  }

  public static PrefixedIdentifier identifier(String prefix, SimpleIdentifier identifier) {
    return new PrefixedIdentifier(identifier(prefix), tokenFromType(TokenType.PERIOD), identifier);
  }

  public static PrefixedIdentifier identifier(String prefix, String identifier) {
    return new PrefixedIdentifier(
        identifier(prefix),
        tokenFromType(TokenType.PERIOD),
        identifier(identifier));
  }

  public static IfStatement ifStatement(Expression condition, Statement thenStatement) {
    return ifStatement(condition, thenStatement, null);
  }

  public static IfStatement ifStatement(Expression condition, Statement thenStatement,
      Statement elseStatement) {
    return new IfStatement(
        tokenFromKeyword(Keyword.IF),
        tokenFromType(TokenType.OPEN_PAREN),
        condition,
        tokenFromType(TokenType.CLOSE_PAREN),
        thenStatement,
        elseStatement == null ? null : tokenFromKeyword(Keyword.ELSE),
        elseStatement);
  }

  public static ImplementsClause implementsClause(TypeName... types) {
    return new ImplementsClause(tokenFromKeyword(Keyword.IMPLEMENTS), list(types));
  }

  public static ImportDirective importDirective(List<Annotation> metadata, String uri,
      boolean isDeferred, String prefix, Combinator... combinators) {
    return new ImportDirective(
        null,
        metadata,
        tokenFromKeyword(Keyword.IMPORT),
        string(uri),
        !isDeferred ? null : tokenFromKeyword(Keyword.DEFERRED),
        prefix == null ? null : tokenFromKeyword(Keyword.AS),
        prefix == null ? null : identifier(prefix),
        list(combinators),
        tokenFromType(TokenType.SEMICOLON));
  }

  public static ImportDirective importDirective(String uri, boolean isDeferred, String prefix,
      Combinator... combinators) {
    return importDirective(new ArrayList<Annotation>(), uri, isDeferred, prefix, combinators);
  }

  public static ImportDirective importDirective(String uri, String prefix,
      Combinator... combinators) {
    return importDirective(new ArrayList<Annotation>(), uri, false, prefix, combinators);
  }

  public static IndexExpression indexExpression(Expression array, Expression index) {
    return new IndexExpression(
        array,
        tokenFromType(TokenType.OPEN_SQUARE_BRACKET),
        index,
        tokenFromType(TokenType.CLOSE_SQUARE_BRACKET));
  }

  public static InstanceCreationExpression instanceCreationExpression(Keyword keyword,
      ConstructorName name, Expression... arguments) {
    return new InstanceCreationExpression(
        keyword == null ? null : tokenFromKeyword(keyword),
        name,
        argumentList(arguments));
  }

  public static InstanceCreationExpression instanceCreationExpression(Keyword keyword,
      TypeName type, Expression... arguments) {
    return instanceCreationExpression(keyword, type, null, arguments);
  }

  public static InstanceCreationExpression instanceCreationExpression(Keyword keyword,
      TypeName type, String identifier, Expression... arguments) {
    return instanceCreationExpression(
        keyword,
        new ConstructorName(
            type,
            identifier == null ? null : tokenFromType(TokenType.PERIOD),
            identifier == null ? null : identifier(identifier)),
        arguments);
  }

  public static IntegerLiteral integer(long value) {
    return new IntegerLiteral(
        tokenFromTypeAndString(TokenType.INT, Long.toString(value)),
        BigInteger.valueOf(value));
  }

  public static InterpolationExpression interpolationExpression(Expression expression) {
    return new InterpolationExpression(
        tokenFromType(TokenType.STRING_INTERPOLATION_EXPRESSION),
        expression,
        tokenFromType(TokenType.CLOSE_CURLY_BRACKET));
  }

  public static InterpolationExpression interpolationExpression(String identifier) {
    return new InterpolationExpression(
        tokenFromType(TokenType.STRING_INTERPOLATION_IDENTIFIER),
        identifier(identifier),
        null);
  }

  public static InterpolationString interpolationString(String contents, String value) {
    return new InterpolationString(tokenFromString(contents), value);
  }

  public static IsExpression isExpression(Expression expression, boolean negated, TypeName type) {
    return new IsExpression(expression, tokenFromKeyword(Keyword.IS), negated
        ? tokenFromType(TokenType.BANG) : null, type);
  }

  public static Label label(SimpleIdentifier label) {
    return new Label(label, tokenFromType(TokenType.COLON));
  }

  public static Label label(String label) {
    return label(identifier(label));
  }

  public static LabeledStatement labeledStatement(List<Label> labels, Statement statement) {
    return new LabeledStatement(labels, statement);
  }

  public static LibraryDirective libraryDirective(List<Annotation> metadata,
      LibraryIdentifier libraryName) {
    return new LibraryDirective(
        null,
        metadata,
        tokenFromKeyword(Keyword.LIBRARY),
        libraryName,
        tokenFromType(TokenType.SEMICOLON));
  }

  public static LibraryDirective libraryDirective(String libraryName) {
    return libraryDirective(new ArrayList<Annotation>(), libraryIdentifier(libraryName));
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
        keyword == null ? null : tokenFromKeyword(keyword),
        typeArguments,
        tokenFromType(TokenType.OPEN_SQUARE_BRACKET),
        list(elements),
        tokenFromType(TokenType.CLOSE_SQUARE_BRACKET));
  }

  public static MapLiteral mapLiteral(Keyword keyword, TypeArgumentList typeArguments,
      MapLiteralEntry... entries) {
    return new MapLiteral(
        keyword == null ? null : tokenFromKeyword(keyword),
        typeArguments,
        tokenFromType(TokenType.OPEN_CURLY_BRACKET),
        list(entries),
        tokenFromType(TokenType.CLOSE_CURLY_BRACKET));
  }

  public static MapLiteral mapLiteral(MapLiteralEntry... entries) {
    return mapLiteral(null, null, entries);
  }

  public static MapLiteralEntry mapLiteralEntry(String key, Expression value) {
    return new MapLiteralEntry(string(key), tokenFromType(TokenType.COLON), value);
  }

  public static MethodDeclaration methodDeclaration(Keyword modifier, TypeName returnType,
      Keyword property, Keyword operator, SimpleIdentifier name, FormalParameterList parameters) {
    return new MethodDeclaration(
        null,
        null,
        tokenFromKeyword(Keyword.EXTERNAL),
        modifier == null ? null : tokenFromKeyword(modifier),
        returnType,
        property == null ? null : tokenFromKeyword(property),
        operator == null ? null : tokenFromKeyword(operator),
        name,
        parameters,
        emptyFunctionBody());
  }

  public static MethodDeclaration methodDeclaration(Keyword modifier, TypeName returnType,
      Keyword property, Keyword operator, SimpleIdentifier name, FormalParameterList parameters,
      FunctionBody body) {
    return new MethodDeclaration(
        null,
        null,
        null,
        modifier == null ? null : tokenFromKeyword(modifier),
        returnType,
        property == null ? null : tokenFromKeyword(property),
        operator == null ? null : tokenFromKeyword(operator),
        name,
        parameters,
        body);
  }

  public static MethodInvocation methodInvocation(Expression target, String methodName,
      Expression... arguments) {
    return new MethodInvocation(
        target,
        target == null ? null : tokenFromType(TokenType.PERIOD),
        identifier(methodName),
        argumentList(arguments));
  }

  public static MethodInvocation methodInvocation(String methodName, Expression... arguments) {
    return methodInvocation(null, methodName, arguments);
  }

  public static NamedExpression namedExpression(Label label, Expression expression) {
    return new NamedExpression(label, expression);
  }

  public static NamedExpression namedExpression(String label, Expression expression) {
    return namedExpression(label(label), expression);
  }

  public static DefaultFormalParameter namedFormalParameter(NormalFormalParameter parameter,
      Expression expression) {
    return new DefaultFormalParameter(parameter, ParameterKind.NAMED, expression == null ? null
        : tokenFromType(TokenType.COLON), expression);
  }

  public static NativeClause nativeClause(String nativeCode) {
    return new NativeClause(tokenFromString("native"), string(nativeCode));
  }

  public static NativeFunctionBody nativeFunctionBody(String nativeMethodName) {
    return new NativeFunctionBody(
        tokenFromString("native"),
        string(nativeMethodName),
        tokenFromType(TokenType.SEMICOLON));
  }

  public static NullLiteral nullLiteral() {
    return new NullLiteral(tokenFromKeyword(Keyword.NULL));
  }

  public static ParenthesizedExpression parenthesizedExpression(Expression expression) {
    return new ParenthesizedExpression(
        tokenFromType(TokenType.OPEN_PAREN),
        expression,
        tokenFromType(TokenType.CLOSE_PAREN));
  }

  public static PartDirective partDirective(List<Annotation> metadata, String url) {
    return new PartDirective(
        null,
        metadata,
        tokenFromKeyword(Keyword.PART),
        string(url),
        tokenFromType(TokenType.SEMICOLON));
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
        tokenFromKeyword(Keyword.PART),
        tokenFromString("of"),
        libraryName,
        tokenFromType(TokenType.SEMICOLON));
  }

  public static DefaultFormalParameter positionalFormalParameter(NormalFormalParameter parameter,
      Expression expression) {
    return new DefaultFormalParameter(parameter, ParameterKind.POSITIONAL, expression == null
        ? null : tokenFromType(TokenType.EQ), expression);
  }

  public static PostfixExpression postfixExpression(Expression expression, TokenType operator) {
    return new PostfixExpression(expression, tokenFromType(operator));
  }

  public static PrefixExpression prefixExpression(TokenType operator, Expression expression) {
    return new PrefixExpression(tokenFromType(operator), expression);
  }

  public static PropertyAccess propertyAccess(Expression target, SimpleIdentifier propertyName) {
    return new PropertyAccess(target, tokenFromType(TokenType.PERIOD), propertyName);
  }

  public static PropertyAccess propertyAccess(Expression target, String propertyName) {
    return new PropertyAccess(target, tokenFromType(TokenType.PERIOD), identifier(propertyName));
  }

  public static RedirectingConstructorInvocation redirectingConstructorInvocation(
      Expression... arguments) {
    return redirectingConstructorInvocation(null, arguments);
  }

  public static RedirectingConstructorInvocation redirectingConstructorInvocation(
      String constructorName, Expression... arguments) {
    return new RedirectingConstructorInvocation(
        tokenFromKeyword(Keyword.THIS),
        constructorName == null ? null : tokenFromType(TokenType.PERIOD),
        constructorName == null ? null : identifier(constructorName),
        argumentList(arguments));
  }

  public static RethrowExpression rethrowExpression() {
    return new RethrowExpression(tokenFromKeyword(Keyword.RETHROW));
  }

  public static ReturnStatement returnStatement() {
    return returnStatement(null);
  }

  public static ReturnStatement returnStatement(Expression expression) {
    return new ReturnStatement(
        tokenFromKeyword(Keyword.RETURN),
        expression,
        tokenFromType(TokenType.SEMICOLON));
  }

  public static ScriptTag scriptTag(String scriptTag) {
    return new ScriptTag(tokenFromString(scriptTag));
  }

  public static ShowCombinator showCombinator(SimpleIdentifier... identifiers) {
    return new ShowCombinator(tokenFromString("show"), list(identifiers));
  }

  public static ShowCombinator showCombinator(String... identifiers) {
    ArrayList<SimpleIdentifier> identifierList = new ArrayList<SimpleIdentifier>();
    for (String identifier : identifiers) {
      identifierList.add(identifier(identifier));
    }
    return new ShowCombinator(tokenFromString("show"), identifierList);
  }

  public static SimpleFormalParameter simpleFormalParameter(Keyword keyword, String parameterName) {
    return simpleFormalParameter(keyword, null, parameterName);
  }

  public static SimpleFormalParameter simpleFormalParameter(Keyword keyword, TypeName type,
      String parameterName) {
    return new SimpleFormalParameter(
        null,
        null,
        keyword == null ? null : tokenFromKeyword(keyword),
        type,
        identifier(parameterName));
  }

  public static SimpleFormalParameter simpleFormalParameter(String parameterName) {
    return simpleFormalParameter(null, null, parameterName);
  }

  public static SimpleFormalParameter simpleFormalParameter(TypeName type, String parameterName) {
    return simpleFormalParameter(null, type, parameterName);
  }

  public static StringInterpolation string(InterpolationElement... elements) {
    return new StringInterpolation(list(elements));
  }

  public static SimpleStringLiteral string(String content) {
    return new SimpleStringLiteral(tokenFromString("'" + content + "'"), content);
  }

  public static SuperConstructorInvocation superConstructorInvocation(Expression... arguments) {
    return superConstructorInvocation(null, arguments);
  }

  public static SuperConstructorInvocation superConstructorInvocation(String name,
      Expression... arguments) {
    return new SuperConstructorInvocation(
        tokenFromKeyword(Keyword.SUPER),
        name == null ? null : tokenFromType(TokenType.PERIOD),
        name == null ? null : identifier(name),
        argumentList(arguments));
  }

  public static SuperExpression superExpression() {
    return new SuperExpression(tokenFromKeyword(Keyword.SUPER));
  }

  public static SwitchCase switchCase(Expression expression, Statement... statements) {
    return switchCase(new ArrayList<Label>(), expression, statements);
  }

  public static SwitchCase switchCase(List<Label> labels, Expression expression,
      Statement... statements) {
    return new SwitchCase(
        labels,
        tokenFromKeyword(Keyword.CASE),
        expression,
        tokenFromType(TokenType.COLON),
        list(statements));
  }

  public static SwitchDefault switchDefault(List<Label> labels, Statement... statements) {
    return new SwitchDefault(
        labels,
        tokenFromKeyword(Keyword.DEFAULT),
        tokenFromType(TokenType.COLON),
        list(statements));
  }

  public static SwitchDefault switchDefault(Statement... statements) {
    return switchDefault(new ArrayList<Label>(), statements);
  }

  public static SwitchStatement switchStatement(Expression expression, SwitchMember... members) {
    return new SwitchStatement(
        tokenFromKeyword(Keyword.SWITCH),
        tokenFromType(TokenType.OPEN_PAREN),
        expression,
        tokenFromType(TokenType.CLOSE_PAREN),
        tokenFromType(TokenType.OPEN_CURLY_BRACKET),
        list(members),
        tokenFromType(TokenType.CLOSE_CURLY_BRACKET));
  }

  public static SymbolLiteral symbolLiteral(String... components) {
    ArrayList<Token> identifierList = new ArrayList<Token>();
    for (String component : components) {
      identifierList.add(tokenFromTypeAndString(TokenType.IDENTIFIER, component));
    }
    return new SymbolLiteral(
        tokenFromType(TokenType.HASH),
        identifierList.toArray(new Token[identifierList.size()]));
  }

  public static BlockFunctionBody syncBlockFunctionBody(Statement... statements) {
    return new BlockFunctionBody(
        tokenFromTypeAndString(TokenType.IDENTIFIER, "sync"),
        null,
        block(statements));
  }

  public static BlockFunctionBody syncGeneratorBlockFunctionBody(Statement... statements) {
    return new BlockFunctionBody(
        tokenFromTypeAndString(TokenType.IDENTIFIER, "sync"),
        tokenFromType(TokenType.STAR),
        block(statements));
  }

  public static ThisExpression thisExpression() {
    return new ThisExpression(tokenFromKeyword(Keyword.THIS));
  }

  public static ThrowExpression throwExpression() {
    return throwExpression(null);
  }

  public static ThrowExpression throwExpression(Expression expression) {
    return new ThrowExpression(tokenFromKeyword(Keyword.THROW), expression);
  }

  public static TopLevelVariableDeclaration topLevelVariableDeclaration(Keyword keyword,
      TypeName type, VariableDeclaration... variables) {
    return new TopLevelVariableDeclaration(null, null, variableDeclarationList(
        keyword,
        type,
        variables), tokenFromType(TokenType.SEMICOLON));
  }

  public static TopLevelVariableDeclaration topLevelVariableDeclaration(Keyword keyword,
      VariableDeclaration... variables) {
    return new TopLevelVariableDeclaration(null, null, variableDeclarationList(
        keyword,
        null,
        variables), tokenFromType(TokenType.SEMICOLON));
  }

  public static TryStatement tryStatement(Block body, Block finallyClause) {
    return tryStatement(body, new ArrayList<CatchClause>(), finallyClause);
  }

  public static TryStatement tryStatement(Block body, CatchClause... catchClauses) {
    return tryStatement(body, list(catchClauses), null);
  }

  public static TryStatement tryStatement(Block body, List<CatchClause> catchClauses,
      Block finallyClause) {
    return new TryStatement(
        tokenFromKeyword(Keyword.TRY),
        body,
        catchClauses,
        finallyClause == null ? null : tokenFromKeyword(Keyword.FINALLY),
        finallyClause);
  }

  public static FunctionTypeAlias typeAlias(TypeName returnType, String name,
      TypeParameterList typeParameters, FormalParameterList parameters) {
    return new FunctionTypeAlias(
        null,
        null,
        tokenFromKeyword(Keyword.TYPEDEF),
        returnType,
        identifier(name),
        typeParameters,
        parameters,
        tokenFromType(TokenType.SEMICOLON));
  }

  public static TypeArgumentList typeArgumentList(TypeName... typeNames) {
    return new TypeArgumentList(
        tokenFromType(TokenType.LT),
        list(typeNames),
        tokenFromType(TokenType.GT));
  }

  /**
   * Create a type name whose name has been resolved to the given element and whose type has been
   * resolved to the type of the given element.
   * <p>
   * <b>Note:</b> This method does not correctly handle class elements that have type parameters.
   * 
   * @param element the element defining the type represented by the type name
   * @return the type name that was created
   */
  public static TypeName typeName(ClassElement element, TypeName... arguments) {
    SimpleIdentifier name = identifier(element.getName());
    name.setStaticElement(element);
    TypeName typeName = typeName(name, arguments);
    typeName.setType(element.getType());
    return typeName;
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

  public static TypeParameter typeParameter(String name) {
    return new TypeParameter(null, null, identifier(name), null, null);
  }

  public static TypeParameter typeParameter(String name, TypeName bound) {
    return new TypeParameter(null, null, identifier(name), tokenFromKeyword(Keyword.EXTENDS), bound);
  }

  public static TypeParameterList typeParameterList(String... typeNames) {
    ArrayList<TypeParameter> typeParameters = new ArrayList<TypeParameter>();
    for (String typeName : typeNames) {
      typeParameters.add(typeParameter(typeName));
    }
    return new TypeParameterList(
        tokenFromType(TokenType.LT),
        typeParameters,
        tokenFromType(TokenType.GT));
  }

  public static VariableDeclaration variableDeclaration(String name) {
    return new VariableDeclaration(null, null, identifier(name), null, null);
  }

  public static VariableDeclaration variableDeclaration(String name, Expression initializer) {
    return new VariableDeclaration(
        null,
        null,
        identifier(name),
        tokenFromType(TokenType.EQ),
        initializer);
  }

  public static VariableDeclarationList variableDeclarationList(Keyword keyword, TypeName type,
      VariableDeclaration... variables) {
    return new VariableDeclarationList(null, null, keyword == null ? null
        : tokenFromKeyword(keyword), type, list(variables));
  }

  public static VariableDeclarationList variableDeclarationList(Keyword keyword,
      VariableDeclaration... variables) {
    return variableDeclarationList(keyword, null, variables);
  }

  public static VariableDeclarationStatement variableDeclarationStatement(Keyword keyword,
      TypeName type, VariableDeclaration... variables) {
    return new VariableDeclarationStatement(
        variableDeclarationList(keyword, type, variables),
        tokenFromType(TokenType.SEMICOLON));
  }

  public static VariableDeclarationStatement variableDeclarationStatement(Keyword keyword,
      VariableDeclaration... variables) {
    return variableDeclarationStatement(keyword, null, variables);
  }

  public static WhileStatement whileStatement(Expression condition, Statement body) {
    return new WhileStatement(
        tokenFromKeyword(Keyword.WHILE),
        tokenFromType(TokenType.OPEN_PAREN),
        condition,
        tokenFromType(TokenType.CLOSE_PAREN),
        body);
  }

  public static WithClause withClause(TypeName... types) {
    return new WithClause(tokenFromKeyword(Keyword.WITH), list(types));
  }

  public static YieldStatement yieldEachStatement(Expression expression) {
    return new YieldStatement(
        tokenFromTypeAndString(TokenType.IDENTIFIER, "yield"),
        tokenFromType(TokenType.STAR),
        expression,
        tokenFromType(TokenType.SEMICOLON));
  }

  public static YieldStatement yieldStatement(Expression expression) {
    return new YieldStatement(
        tokenFromTypeAndString(TokenType.IDENTIFIER, "yield"),
        null,
        expression,
        tokenFromType(TokenType.SEMICOLON));
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private AstFactory() {
  }
}
