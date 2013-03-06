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
package com.google.dart.engine.internal.verifier;

import com.google.dart.engine.ast.ArgumentDefinitionTest;
import com.google.dart.engine.ast.AssertStatement;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassTypeAlias;
import com.google.dart.engine.ast.ConditionalExpression;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.ConstructorName;
import com.google.dart.engine.ast.DoStatement;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.IfStatement;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.ReturnStatement;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SwitchStatement;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.TypeParameter;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.WhileStatement;
import com.google.dart.engine.ast.visitor.RecursiveASTVisitor;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.TypeVariableElement;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.StaticTypeWarningCode;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.internal.error.ErrorReporter;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

/**
 * Instances of the class {@code ErrorVerifier} traverse an AST structure looking for additional
 * errors and warnings not covered by the parser and resolver.
 */
public class ErrorVerifier extends RecursiveASTVisitor<Void> {
  /**
   * The error reporter by which errors will be reported.
   */
  private ErrorReporter errorReporter;

  /**
   * The current library that is being analyzed.
   */
  private LibraryElement currentLibrary;

  /**
   * The type representing the type 'dynamic'.
   */
  private Type dynamicType;

  /**
   * The object providing access to the types defined by the language.
   */
  private TypeProvider typeProvider;

  /**
   * The method or function that we are currently visiting, or {@code null} if we are not inside a
   * method or function.
   */
  private ExecutableElement currentFunction;

  public ErrorVerifier(ErrorReporter errorReporter, LibraryElement currentLibrary,
      TypeProvider typeProvider) {
    this.errorReporter = errorReporter;
    this.currentLibrary = currentLibrary;
    this.typeProvider = typeProvider;
    dynamicType = typeProvider.getDynamicType();
  }

  @Override
  public Void visitArgumentDefinitionTest(ArgumentDefinitionTest node) {
    SimpleIdentifier identifier = node.getIdentifier();
    Element element = identifier.getElement();
    if (element != null && !(element instanceof ParameterElement)) {
      errorReporter.reportError(
          CompileTimeErrorCode.ARGUMENT_DEFINITION_TEST_NON_PARAMETER,
          identifier,
          identifier.getName());
    }
    return super.visitArgumentDefinitionTest(node);
  }

  @Override
  public Void visitAssertStatement(AssertStatement node) {
    Expression expression = node.getCondition();
    Type type = getType(expression);
    if (type instanceof InterfaceType) {
      if (!type.isAssignableTo(typeProvider.getBoolType())) {
        errorReporter.reportError(StaticTypeWarningCode.NON_BOOL_EXPRESSION, expression);
      }
    } else if (type instanceof FunctionType) {
      FunctionType functionType = (FunctionType) type;
      if (functionType.getTypeArguments().length == 0
          && !functionType.getReturnType().isAssignableTo(typeProvider.getBoolType())) {
        errorReporter.reportError(StaticTypeWarningCode.NON_BOOL_EXPRESSION, expression);
      }
    }
    return super.visitAssertStatement(node);
  }

  @Override
  public Void visitAssignmentExpression(AssignmentExpression node) {
    Expression lhs = node.getLeftHandSide();
    Expression rhs = node.getRightHandSide();
    Type leftType = getType(lhs);
    Type rightType = getType(rhs);
    if (!rightType.isAssignableTo(leftType)) {
      errorReporter.reportError(
          StaticTypeWarningCode.INVALID_ASSIGNMENT,
          rhs,
          leftType.getName(),
          rightType.getName());
    }
    return super.visitAssignmentExpression(node);
  }

  @Override
  public Void visitClassDeclaration(ClassDeclaration node) {
    checkForBuiltInIdentifierAsName(
        node.getName(),
        CompileTimeErrorCode.BUILT_IN_IDENTIFIER_AS_TYPE_NAME);
    return super.visitClassDeclaration(node);
  }

  @Override
  public Void visitClassTypeAlias(ClassTypeAlias node) {
    checkForBuiltInIdentifierAsName(
        node.getName(),
        CompileTimeErrorCode.BUILT_IN_IDENTIFIER_AS_TYPEDEF_NAME);
    return super.visitClassTypeAlias(node);
  }

  @Override
  public Void visitConditionalExpression(ConditionalExpression node) {
    checkForNonBoolCondition(node.getCondition());
    return super.visitConditionalExpression(node);
  }

  @Override
  public Void visitConstructorDeclaration(ConstructorDeclaration node) {
    ExecutableElement previousFunction = currentFunction;
    try {
      currentFunction = node.getElement();
      return super.visitConstructorDeclaration(node);
    } finally {
      currentFunction = previousFunction;
    }
  }

  @Override
  public Void visitDoStatement(DoStatement node) {
    checkForNonBoolCondition(node.getCondition());
    return super.visitDoStatement(node);
  }

  @Override
  public Void visitFunctionDeclaration(FunctionDeclaration node) {
    ExecutableElement previousFunction = currentFunction;
    try {
      currentFunction = node.getElement();
      return super.visitFunctionDeclaration(node);
    } finally {
      currentFunction = previousFunction;
    }
  }

  @Override
  public Void visitFunctionExpression(FunctionExpression node) {
    ExecutableElement previousFunction = currentFunction;
    try {
      currentFunction = node.getElement();
      return super.visitFunctionExpression(node);
    } finally {
      currentFunction = previousFunction;
    }
  }

  @Override
  public Void visitFunctionTypeAlias(FunctionTypeAlias node) {
    checkForBuiltInIdentifierAsName(
        node.getName(),
        CompileTimeErrorCode.BUILT_IN_IDENTIFIER_AS_TYPEDEF_NAME);
    return super.visitFunctionTypeAlias(node);
  }

  @Override
  public Void visitIfStatement(IfStatement node) {
    checkForNonBoolCondition(node.getCondition());
    return super.visitIfStatement(node);
  }

  @Override
  public Void visitInstanceCreationExpression(InstanceCreationExpression node) {
    ConstructorName constructorName = node.getConstructorName();
    TypeName typeName = constructorName.getType();
    Type createdType = typeName.getType();
    if (createdType instanceof InterfaceType) {
      if (((InterfaceType) createdType).getElement().isAbstract()) {
        // CONST_WITH_ABSTRACT_CLASS & NEW_WITH_ABSTRACT_CLASS
        ConstructorElement element = node.getElement();
        if (element != null && !element.isFactory()) {
          if (((KeywordToken) node.getKeyword()).getKeyword() == Keyword.CONST) {
            errorReporter.reportError(StaticWarningCode.CONST_WITH_ABSTRACT_CLASS, typeName);
          } else {
            errorReporter.reportError(StaticWarningCode.NEW_WITH_ABSTRACT_CLASS, typeName);
          }
        }
      }
      // TODO(jwren) Should this be an else-if or if block? (Should we provide as many errors
      // as possible, or try to be as concise as possible?)
      if (typeName.getTypeArguments() != null) {
        // TYPE_ARGUMENT_NOT_MATCHING_BOUNDS
        ConstructorElement constructorElement = constructorName.getElement();
        if (constructorElement != null) {
          NodeList<TypeName> typeNameArgList = typeName.getTypeArguments().getArguments();
          TypeVariableElement[] boundingElts = constructorElement.getEnclosingElement().getTypeVariables();
          // Loop through only all of the elements of the shorter of our two arrays. (Note: This
          // will only happen these tokens have the WRONG_NUMBER_OF_TYPE_ARGUMENTS error code too.)
          int loopThroughIndex = Math.min(typeNameArgList.size(), boundingElts.length);
          for (int i = 0; i < loopThroughIndex; i++) {
            TypeName argTypeName = typeNameArgList.get(i);
            Type argType = argTypeName.getType();
            Type boundType = boundingElts[i].getBound();
            if (argType != null && boundType != null) {
              if (!argType.isSubtypeOf(boundType)) {
                errorReporter.reportError(
                    StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS,
                    argTypeName,
                    argTypeName.getName(),
                    boundingElts[i].getName());
              }
            }
          }
        }
      }
    } else {
      errorReporter.reportError(CompileTimeErrorCode.NON_CONSTANT_MAP_KEY, typeName);
    }
    return super.visitInstanceCreationExpression(node);
  }

  @Override
  public Void visitMethodDeclaration(MethodDeclaration node) {
    ExecutableElement previousFunction = currentFunction;
    try {
      currentFunction = node.getElement();
      return super.visitMethodDeclaration(node);
    } finally {
      currentFunction = previousFunction;
    }
  }

  @Override
  public Void visitReturnStatement(ReturnStatement node) {
    FunctionType functionType = currentFunction == null ? null : currentFunction.getType();
    Type expectedReturnType = functionType == null ? null : functionType.getReturnType();
    Expression returnExpression = node.getExpression();
    if (expectedReturnType != null && !expectedReturnType.isVoid() && returnExpression != null) {
      Type actualReturnType = getType(returnExpression);
      if (!actualReturnType.isAssignableTo(expectedReturnType)) {
        errorReporter.reportError(
            StaticTypeWarningCode.RETURN_OF_INVALID_TYPE,
            returnExpression,
            actualReturnType.getName(),
            expectedReturnType.getName());
      }
    }
    return super.visitReturnStatement(node);
  }

  @Override
  public Void visitSwitchStatement(SwitchStatement node) {
    Expression expression = node.getExpression();
    Type type = expression.getStaticType();
    // if the type is int or String, exit this check quickly
    if (type != null && !type.equals(typeProvider.getIntType())
        && !type.equals(typeProvider.getStringType())) {
      Element element = type.getElement();
      if (element instanceof ClassElement) {
        ClassElement classElement = (ClassElement) element;
        MethodElement method = classElement.lookUpMethod("==", currentLibrary);
        if (method != null
            && !method.getEnclosingElement().getType().equals(typeProvider.getObjectType())) {
          errorReporter.reportError(
              CompileTimeErrorCode.CASE_EXPRESSION_TYPE_IMPLEMENTS_EQUALS,
              expression,
              element.getName());
        }
      }
    }
    return super.visitSwitchStatement(node);
  }

  @Override
  public Void visitTypeParameter(TypeParameter node) {
    checkForBuiltInIdentifierAsName(
        node.getName(),
        CompileTimeErrorCode.BUILT_IN_IDENTIFIER_AS_TYPE_VARIABLE_NAME);
    return super.visitTypeParameter(node);
  }

  @Override
  public Void visitVariableDeclarationList(VariableDeclarationList node) {
    TypeName typeName = node.getType();
    if (typeName != null) {
      Identifier identifier = typeName.getName();
      if (identifier instanceof SimpleIdentifier) {
        SimpleIdentifier simpleIdentifier = (SimpleIdentifier) identifier;
        Token token = simpleIdentifier.getToken();
        if (token.getType() == TokenType.KEYWORD) {
          if (((KeywordToken) token).getKeyword() != Keyword.DYNAMIC) {
            errorReporter.reportError(
                CompileTimeErrorCode.BUILT_IN_IDENTIFIER_AS_TYPE,
                identifier,
                identifier.getName());
          }
        }
      }
    }
    return super.visitVariableDeclarationList(node);
  }

  @Override
  public Void visitWhileStatement(WhileStatement node) {
    checkForNonBoolCondition(node.getCondition());
    return super.visitWhileStatement(node);
  }

  /**
   * This verifies that the passed identifier is not a keyword, and generates the passed error code
   * on the identifier if it is a keyword.
   * 
   * @param identifier the identifier to check to ensure that it is not a keyword
   * @param errorCode if the passed identifier is a keyword then this error code is created on the
   *          identifier, the error code will be one of
   *          {@link CompileTimeErrorCode#BUILT_IN_IDENTIFIER_AS_TYPE_NAME},
   *          {@link CompileTimeErrorCode#BUILT_IN_IDENTIFIER_AS_TYPE_VARIABLE_NAME} or
   *          {@link CompileTimeErrorCode#BUILT_IN_IDENTIFIER_AS_TYPEDEF_NAME}
   */
  private void checkForBuiltInIdentifierAsName(SimpleIdentifier identifier, ErrorCode errorCode) {
    Token token = identifier.getToken();
    if (token.getType() == TokenType.KEYWORD) {
      errorReporter.reportError(errorCode, identifier, identifier.getName());
    }
  }

  /**
   * Checks to ensure that the expressions that need to be of type bool, are. Otherwise an error is
   * reported on the expression.
   * 
   * @see StaticTypeWarningCode#NON_BOOL_CONDITION
   * @param condition the conditional expression to test
   */
  private void checkForNonBoolCondition(Expression condition) {
    Type conditionType = getType(condition);
    if (conditionType != null && !conditionType.isAssignableTo(typeProvider.getBoolType())) {
      errorReporter.reportError(StaticTypeWarningCode.NON_BOOL_CONDITION, condition);
    }
  }

  /**
   * Return the type of the given expression that is to be used for type analysis.
   * 
   * @param expression the expression whose type is to be returned
   * @return the type of the given expression
   */
  private Type getType(Expression expression) {
    Type type = expression.getStaticType();
    return type == null ? dynamicType : type;
  }

}
