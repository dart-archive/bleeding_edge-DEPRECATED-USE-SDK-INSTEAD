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

import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.TokenType;

import static com.google.dart.engine.scanner.TokenFactory.token;

import java.util.ArrayList;
import java.util.List;

/**
 * The class {@code ASTFactory} defines utility methods that can be used to create AST nodes. The
 * nodes that are created are complete in the sense that all of the tokens that would have been
 * associated with the nodes by a parser are also created, but the token stream is not constructed.
 */
public final class ASTFactory {
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

  public static ArgumentDefinitionTest argumentDefinitionTest(String identifier) {
    return new ArgumentDefinitionTest(token(TokenType.QUESTION), identifier(identifier));
  }

  public static ArgumentList argumentList(Expression... arguments) {
    return new ArgumentList(
        token(TokenType.OPEN_PAREN),
        list(arguments),
        token(TokenType.CLOSE_PAREN));
  }

  public static ArrayAccess arrayAccess(Expression array, Expression index) {
    return new ArrayAccess(
        array,
        token(TokenType.OPEN_SQUARE_BRACKET),
        index,
        token(TokenType.CLOSE_SQUARE_BRACKET));
  }

  public static AssignmentExpression assignmentExpression(Expression leftHandSide,
      TokenType operator, Expression rightHandSide) {
    return new AssignmentExpression(leftHandSide, token(operator), rightHandSide);
  }

  public static BinaryExpression binaryExpression(Expression leftOperand, TokenType operator,
      Expression rightOperand) {
    return new BinaryExpression(leftOperand, token(operator), rightOperand);
  }

  public static Block block(Statement... statements) {
    return new Block(
        token(TokenType.OPEN_CURLY_BRACKET),
        list(statements),
        token(TokenType.CLOSE_CURLY_BRACKET));
  }

  public static BlockFunctionBody blockFunctionBody(Statement... statements) {
    return new BlockFunctionBody(block(statements));
  }

  public static BooleanLiteral booleanLiteral(boolean value) {
    return new BooleanLiteral(value ? token(Keyword.TRUE) : token(Keyword.FALSE), value);
  }

  public static BreakStatement breakStatement() {
    return new BreakStatement(token(Keyword.BREAK), null, token(TokenType.SEMICOLON));
  }

  public static BreakStatement breakStatement(String label) {
    return new BreakStatement(token(Keyword.BREAK), identifier(label), token(TokenType.SEMICOLON));
  }

  public static ArrayAccess cascadedArrayAccess(Expression index) {
    return new ArrayAccess(
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
        exceptionType == null ? null : token(TokenType.IDENTIFIER, "on"),
        exceptionType,
        exceptionParameter == null ? null : token(Keyword.CATCH),
        exceptionParameter == null ? null : token(TokenType.OPEN_PAREN),
        identifier(exceptionParameter),
        stackTraceParameter == null ? null : token(TokenType.COMMA),
        stackTraceParameter == null ? null : identifier(stackTraceParameter),
        exceptionParameter == null ? null : token(TokenType.CLOSE_PAREN),
        block(statements));
  }

  public static ClassDeclaration classDeclaration(Keyword abstractKeyword, String name,
      TypeParameterList typeParameters, ExtendsClause extendsClause,
      ImplementsClause implementsClause, ClassMember... members) {
    return new ClassDeclaration(
        null,
        null,
        abstractKeyword == null ? null : token(abstractKeyword),
        token(Keyword.CLASS),
        identifier(name),
        typeParameters,
        extendsClause,
        implementsClause,
        token(TokenType.OPEN_CURLY_BRACKET),
        list(members),
        token(TokenType.CLOSE_CURLY_BRACKET));
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
    return new CompilationUnit(scriptTag == null ? null : scriptTag(scriptTag), directives == null
        ? new ArrayList<Directive>() : directives, declarations == null
        ? new ArrayList<CompilationUnitMember>() : declarations);
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

  public static ConstructorDeclaration constructorDeclaration(Keyword keyword,
      Identifier returnType, String name, FormalParameterList parameters,
      List<ConstructorInitializer> initializers) {
    return new ConstructorDeclaration(null, null, token(Keyword.EXTERNAL), keyword == null ? null
        : token(keyword), returnType, name == null ? null : token(TokenType.PERIOD), name == null
        ? null : identifier(name), parameters, initializers == null || initializers.isEmpty()
        ? null : token(TokenType.PERIOD), initializers == null
        ? new ArrayList<ConstructorInitializer>() : initializers, emptyFunctionBody());
  }

  public static ConstructorDeclaration constructorDeclaration(Keyword keyword,
      Identifier returnType, String name, FormalParameterList parameters,
      List<ConstructorInitializer> initializers, FunctionBody body) {
    return new ConstructorDeclaration(
        null,
        null,
        null,
        keyword == null ? null : token(keyword),
        returnType,
        name == null ? null : token(TokenType.PERIOD),
        name == null ? null : identifier(name),
        parameters,
        initializers == null || initializers.isEmpty() ? null : token(TokenType.PERIOD),
        initializers == null ? new ArrayList<ConstructorInitializer>() : initializers,
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

  public static ContinueStatement continueStatement() {
    return new ContinueStatement(token(Keyword.CONTINUE), null, token(TokenType.SEMICOLON));
  }

  public static ContinueStatement continueStatement(String label) {
    return new ContinueStatement(
        token(Keyword.CONTINUE),
        identifier(label),
        token(TokenType.SEMICOLON));
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

  public static ExportDirective exportDirective(List<Annotation> metadata, String uri,
      List<Combinator> combinators) {
    return new ExportDirective(
        null,
        metadata,
        token(Keyword.EXPORT),
        string(uri),
        combinators,
        token(TokenType.SEMICOLON));
  }

  public static ExportDirective exportDirective(String uri, List<Combinator> combinators) {
    return exportDirective(new ArrayList<Annotation>(), uri, combinators);
  }

  public static ExpressionFunctionBody expressionFunctionBody(Expression expression) {
    return new ExpressionFunctionBody(
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
    return new FieldDeclaration(
        null,
        null,
        isStatic ? token(Keyword.STATIC) : null,
        variableDeclarationList(keyword, type, variables),
        token(TokenType.SEMICOLON));
  }

  public static FieldDeclaration fieldDeclaration(boolean isStatic, Keyword keyword,
      VariableDeclaration... variables) {
    return fieldDeclaration(isStatic, keyword, null, variables);
  }

  public static FieldFormalParameter fieldFormalParameter(Keyword keyword, TypeName type,
      String identifier) {
    return new FieldFormalParameter(
        null,
        null,
        keyword == null ? null : token(keyword),
        type,
        token(Keyword.THIS),
        token(TokenType.PERIOD),
        identifier(identifier));
  }

  public static ForEachStatement forEachStatement(SimpleFormalParameter loopParameter,
      Expression iterator, Statement body) {
    return new ForEachStatement(
        token(Keyword.FOR),
        token(TokenType.OPEN_PAREN),
        loopParameter,
        token(Keyword.IN),
        iterator,
        token(TokenType.CLOSE_PAREN),
        body);
  }

  public static FormalParameterList formalParameterList(FormalParameter... parameters) {
    return new FormalParameterList(
        token(TokenType.OPEN_PAREN),
        list(parameters),
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

  public static FunctionDeclaration functionDeclaration(Keyword keyword,
      FunctionExpression functionExpression) {
    return new FunctionDeclaration(
        null,
        null,
        null,
        keyword == null ? null : token(keyword),
        functionExpression);
  }

  public static FunctionDeclarationStatement functionDeclarationStatement(Keyword keyword,
      FunctionExpression functionExpression) {
    return new FunctionDeclarationStatement(functionDeclaration(keyword, functionExpression));
  }

  public static FunctionExpression functionExpression(String name) {
    return new FunctionExpression(
        null,
        identifier(name),
        formalParameterList(),
        blockFunctionBody());
  }

  public static FunctionExpression functionExpression(TypeName type, String name,
      FormalParameterList parameters, FunctionBody body) {
    return new FunctionExpression(type, identifier(name), parameters, body);
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

  public static SimpleIdentifier identifier(String lexeme) {
    return new SimpleIdentifier(token(TokenType.IDENTIFIER, lexeme));
  }

  public static PrefixedIdentifier identifier(String prefix, String identifier) {
    return new PrefixedIdentifier(
        identifier(prefix),
        token(TokenType.PERIOD),
        identifier(identifier));
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

  public static ImplementsClause implementsClause(TypeName... types) {
    return new ImplementsClause(token(Keyword.IMPLEMENTS), list(types));
  }

  public static ImportDirective importDirective(List<Annotation> metadata, String uri,
      String prefix, List<Combinator> combinators) {
    return new ImportDirective(
        null,
        metadata,
        token(Keyword.IMPORT),
        string(uri),
        prefix == null ? null : token(Keyword.AS),
        prefix == null ? null : identifier(prefix),
        combinators,
        token(TokenType.SEMICOLON));
  }

  public static ImportDirective importDirective(String uri, String prefix,
      List<Combinator> combinators) {
    return importDirective(new ArrayList<Annotation>(), uri, prefix, combinators);
  }

  public static HideCombinator importHideCombinator(SimpleIdentifier... identifiers) {
    return new HideCombinator(token("hide"), list(identifiers));
  }

  public static ShowCombinator importShowCombinator(SimpleIdentifier... identifiers) {
    return new ShowCombinator(token("show"), list(identifiers));
  }

  public static InstanceCreationExpression instanceCreationExpression(Keyword keyword,
      TypeName type, Expression... arguments) {
    return instanceCreationExpression(keyword, type, null, arguments);
  }

  public static InstanceCreationExpression instanceCreationExpression(Keyword keyword,
      TypeName type, String identifier, Expression... arguments) {
    return new InstanceCreationExpression(
        keyword == null ? null : token(keyword),
        type,
        identifier == null ? null : token(TokenType.PERIOD),
        identifier == null ? null : identifier(identifier),
        argumentList(arguments));
  }

  public static IntegerLiteral integer(long value) {
    return new IntegerLiteral(token(TokenType.INT, Long.toString(value)), value);
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

  public static Label label(String label) {
    return new Label(identifier(label), token(TokenType.COLON));
  }

  public static LabeledStatement labeledStatement(List<Label> labels, Statement statement) {
    return new LabeledStatement(labels, statement);
  }

  public static LibraryDirective libraryDirective(List<Annotation> metadata, String libraryName) {
    return new LibraryDirective(
        null,
        metadata,
        token(Keyword.LIBRARY),
        identifier(libraryName),
        token(TokenType.SEMICOLON));
  }

  public static LibraryDirective libraryDirective(String libraryName) {
    return libraryDirective(new ArrayList<Annotation>(), libraryName);
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

  public static MethodDeclaration methodDeclaration(Keyword modifier, TypeName returnType,
      Keyword property, Keyword operator, Identifier name, FormalParameterList parameters) {
    return new MethodDeclaration(null, null, token(Keyword.EXTERNAL), modifier == null ? null
        : token(modifier), returnType, property == null ? null : token(property), operator == null
        ? null : token(operator), name, parameters, emptyFunctionBody());
  }

  public static MethodDeclaration methodDeclaration(Keyword modifier, TypeName returnType,
      Keyword property, Keyword operator, Identifier name, FormalParameterList parameters,
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

  public static MethodInvocation methodInvocation(Expression target, String methodName,
      Expression... arguments) {
    return new MethodInvocation(
        target,
        target == null ? null : token(TokenType.PERIOD),
        identifier(methodName),
        argumentList(arguments));
  }

  public static MethodInvocation methodInvocation(String methodName, Expression... arguments) {
    return methodInvocation(null, methodName, arguments);
  }

  public static NamedExpression namedExpression(String label, Expression expression) {
    return new NamedExpression(label(label), expression);
  }

  public static DefaultFormalParameter namedFormalParameter(NormalFormalParameter parameter,
      Expression expression) {
    return new DefaultFormalParameter(parameter, true, token(TokenType.COLON), expression);
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

  public static PartOfDirective partOfDirective(List<Annotation> metadata,
      SimpleIdentifier libraryName) {
    return new PartOfDirective(
        null,
        metadata,
        token(Keyword.PART),
        token("of"),
        libraryName,
        token(TokenType.SEMICOLON));
  }

  public static PartOfDirective partOfDirective(SimpleIdentifier libraryName) {
    return partOfDirective(new ArrayList<Annotation>(), libraryName);
  }

  public static DefaultFormalParameter positionalFormalParameter(NormalFormalParameter parameter,
      Expression expression) {
    return new DefaultFormalParameter(parameter, false, token(TokenType.EQ), expression);
  }

  public static PostfixExpression postfixExpression(Expression expression, TokenType operator) {
    return new PostfixExpression(expression, token(operator));
  }

  public static PrefixExpression prefixExpression(TokenType operator, Expression expression) {
    return new PrefixExpression(token(operator), expression);
  }

  public static PropertyAccess propertyAccess(Expression target, String propertyName) {
    return new PropertyAccess(target, token(TokenType.PERIOD), identifier(propertyName));
  }

  public static RedirectingConstructorInvocation redirectingConstructorInvocation(
      Expression... arguments) {
    return redirectingConstructorInvocation(null, arguments);
  }

  public static RedirectingConstructorInvocation redirectingConstructorInvocation(
      String constructorName, Expression... arguments) {
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
      String parameterName) {
    return new SimpleFormalParameter(
        null,
        null,
        keyword == null ? null : token(keyword),
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
    return new SimpleStringLiteral(token("'" + content + "'"), content);
  }

  public static SuperConstructorInvocation superConstructorInvocation(Expression... arguments) {
    return superConstructorInvocation(null, arguments);
  }

  public static SuperConstructorInvocation superConstructorInvocation(String name,
      Expression... arguments) {
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

  public static TypeAlias typeAlias(TypeName returnType, String name,
      TypeParameterList typeParameters, FormalParameterList parameters) {
    return new TypeAlias(
        null,
        null,
        token(Keyword.TYPEDEF),
        returnType,
        identifier(name),
        typeParameters,
        parameters,
        token(TokenType.SEMICOLON));
  }

  public static TypeArgumentList typeArgumentList(TypeName... typeNames) {
    return new TypeArgumentList(token(TokenType.LT), list(typeNames), token(TokenType.GT));
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
    return new TypeParameter(null, null, identifier(name), token(Keyword.EXTENDS), bound);
  }

  public static TypeParameterList typeParameterList(String... typeNames) {
    ArrayList<TypeParameter> typeParameters = new ArrayList<TypeParameter>();
    for (String typeName : typeNames) {
      typeParameters.add(typeParameter(typeName));
    }
    return new TypeParameterList(token(TokenType.LT), typeParameters, token(TokenType.GT));
  }

  public static VariableDeclaration variableDeclaration(String name) {
    return new VariableDeclaration(null, null, identifier(name), null, null);
  }

  public static VariableDeclaration variableDeclaration(String name, Expression initializer) {
    return new VariableDeclaration(null, null, identifier(name), token(TokenType.EQ), initializer);
  }

  public static VariableDeclarationList variableDeclarationList(Keyword keyword, TypeName type,
      VariableDeclaration... variables) {
    return new VariableDeclarationList(
        keyword == null ? null : token(keyword),
        type,
        list(variables));
  }

  public static VariableDeclarationList variableDeclarationList(Keyword keyword,
      VariableDeclaration... variables) {
    return variableDeclarationList(keyword, null, variables);
  }

  public static VariableDeclarationStatement variableDeclarationStatement(Keyword keyword,
      TypeName type, VariableDeclaration... variables) {
    return new VariableDeclarationStatement(
        variableDeclarationList(keyword, type, variables),
        token(TokenType.SEMICOLON));
  }

  public static VariableDeclarationStatement variableDeclarationStatement(Keyword keyword,
      VariableDeclaration... variables) {
    return variableDeclarationStatement(keyword, null, variables);
  }

  public static WhileStatement whileStatement(Expression condition, Statement body) {
    return new WhileStatement(
        token(Keyword.WHILE),
        token(TokenType.OPEN_PAREN),
        condition,
        token(TokenType.CLOSE_PAREN),
        body);
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private ASTFactory() {
  }
}
