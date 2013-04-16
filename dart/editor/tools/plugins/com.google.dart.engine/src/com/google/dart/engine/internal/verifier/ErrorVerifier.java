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

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.ArgumentDefinitionTest;
import com.google.dart.engine.ast.AssertStatement;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.CatchClause;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassMember;
import com.google.dart.engine.ast.ClassTypeAlias;
import com.google.dart.engine.ast.ConditionalExpression;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.ConstructorFieldInitializer;
import com.google.dart.engine.ast.ConstructorInitializer;
import com.google.dart.engine.ast.ConstructorName;
import com.google.dart.engine.ast.DefaultFormalParameter;
import com.google.dart.engine.ast.DoStatement;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ExtendsClause;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.FieldFormalParameter;
import com.google.dart.engine.ast.FormalParameter;
import com.google.dart.engine.ast.FormalParameterList;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.IfStatement;
import com.google.dart.engine.ast.ImplementsClause;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.NativeFunctionBody;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.NormalFormalParameter;
import com.google.dart.engine.ast.RethrowExpression;
import com.google.dart.engine.ast.ReturnStatement;
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SwitchStatement;
import com.google.dart.engine.ast.ThrowExpression;
import com.google.dart.engine.ast.TopLevelVariableDeclaration;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.TypeParameter;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.VariableDeclarationStatement;
import com.google.dart.engine.ast.WhileStatement;
import com.google.dart.engine.ast.visitor.RecursiveASTVisitor;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.TypeVariableElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.StaticTypeWarningCode;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.internal.element.FieldFormalParameterElementImpl;
import com.google.dart.engine.internal.error.ErrorReporter;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.parser.ParserErrorCode;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

import java.util.HashMap;

/**
 * Instances of the class {@code ErrorVerifier} traverse an AST structure looking for additional
 * errors and warnings not covered by the parser and resolver.
 * 
 * @coverage dart.engine.resolver
 */
public class ErrorVerifier extends RecursiveASTVisitor<Void> {
  /**
   * This enum holds one of four states of a field initialization state through a constructor
   * signature, not initialized, initialized in the field declaration, initialized in the field
   * formal, and finally, initialized in the initializers list.
   */
  private enum INIT_STATE {
    NOT_INIT,
    INIT_IN_DECLARATION,
    INIT_IN_FIELD_FORMAL,
    INIT_IN_DEFAULT_VALUE,
    INIT_IN_INITIALIZERS
  }

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
   * This is set to {@code true} iff the visitor is currently visiting children nodes of a
   * {@link ConstructorDeclaration} and the constructor is 'const'.
   * 
   * @see #visitConstructorDeclaration(ConstructorDeclaration)
   */
  private boolean isEnclosingConstructorConst;

  /**
   * This is set to {@code true} iff the visitor is currently visiting children nodes of a
   * {@link CatchClause}.
   * 
   * @see #visitCatchClause(CatchClause)
   */
  private boolean isInCatchClause;

  /**
   * This is set to {@code true} iff the visitor is currently visiting code in the SDK.
   */
  private boolean isInSystemLibrary;

  /**
   * The method or function that we are currently visiting, or {@code null} if we are not inside a
   * method or function.
   */
  private ExecutableElement enclosingFunction;

  /**
   * This map is initialized when visiting the contents of a class declaration. If the visitor is
   * not in an enclosing class declaration, then the map is set to {@code null}.
   * <p>
   * When set the map maps the set of {@link FieldElement}s in the class to an
   * {@link INIT_STATE#NOT_INIT} or {@link INIT_STATE#INIT_IN_DECLARATION}. <code>checkFor*</code>
   * methods, specifically {@link #checkForAllFinalInitializedErrorCodes(ConstructorDeclaration)},
   * can make a copy of the map to compute error code states. <code>checkFor*</code> methods should
   * only ever make a copy, or read from this map after it has been set in
   * {@link #visitClassDeclaration(ClassDeclaration)}.
   * 
   * @see #visitClassDeclaration(ClassDeclaration)
   * @see #checkForAllFinalInitializedErrorCodes(ConstructorDeclaration)
   */
  private HashMap<FieldElement, INIT_STATE> initialFieldElementsMap;

  /**
   * A list of types used by the {@link CompileTimeErrorCode#EXTENDS_DISALLOWED_CLASS} and
   * {@link CompileTimeErrorCode#IMPLEMENTS_DISALLOWED_CLASS} error codes.
   */
  private final InterfaceType[] DISALLOWED_TYPES_TO_EXTEND_OR_IMPLEMENT;

  public ErrorVerifier(ErrorReporter errorReporter, LibraryElement currentLibrary,
      TypeProvider typeProvider) {
    this.errorReporter = errorReporter;
    this.currentLibrary = currentLibrary;
    this.isInSystemLibrary = currentLibrary.getSource().isInSystemLibrary();
    this.typeProvider = typeProvider;
    isEnclosingConstructorConst = false;
    isInCatchClause = false;
    dynamicType = typeProvider.getDynamicType();
    DISALLOWED_TYPES_TO_EXTEND_OR_IMPLEMENT = new InterfaceType[] {
        typeProvider.getNumType(), typeProvider.getIntType(), typeProvider.getDoubleType(),
        typeProvider.getBoolType(), typeProvider.getStringType()};
  }

  @Override
  public Void visitArgumentDefinitionTest(ArgumentDefinitionTest node) {
    checkForArgumentDefinitionTestNonParameter(node);
    return super.visitArgumentDefinitionTest(node);
  }

  @Override
  public Void visitAssertStatement(AssertStatement node) {
    checkForNonBoolExpression(node);
    return super.visitAssertStatement(node);
  }

  @Override
  public Void visitAssignmentExpression(AssignmentExpression node) {
    checkForInvalidAssignment(node);
    return super.visitAssignmentExpression(node);
  }

  @Override
  public Void visitCatchClause(CatchClause node) {
    boolean previousIsInCatchClause = isInCatchClause;
    try {
      isInCatchClause = true;
      return super.visitCatchClause(node);
    } finally {
      isInCatchClause = previousIsInCatchClause;
    }
  }

  @Override
  public Void visitClassDeclaration(ClassDeclaration node) {
    try {
      checkForBuiltInIdentifierAsName(
          node.getName(),
          CompileTimeErrorCode.BUILT_IN_IDENTIFIER_AS_TYPE_NAME);
      // initialize initialFieldElementsMap
      ClassElement classElement = node.getElement();
      if (classElement != null) {
        FieldElement[] fieldElements = classElement.getFields();
        initialFieldElementsMap = new HashMap<FieldElement, INIT_STATE>(fieldElements.length);
        for (FieldElement fieldElement : fieldElements) {
          if (!fieldElement.isSynthetic()) {
            initialFieldElementsMap.put(fieldElement, fieldElement.getInitializer() == null
                ? INIT_STATE.NOT_INIT : INIT_STATE.INIT_IN_DECLARATION);
          }
        }
      }
      checkForFinalNotInitialized(node);
      return super.visitClassDeclaration(node);
    } finally {
      initialFieldElementsMap = null;
    }
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
    ExecutableElement outerFunction = enclosingFunction;
    try {
      enclosingFunction = node.getElement();
      isEnclosingConstructorConst = node.getConstKeyword() != null;
      checkForConstConstructorWithNonFinalField(node);
      checkForConflictingConstructorNameAndMember(node);
      checkForAllFinalInitializedErrorCodes(node);
      return super.visitConstructorDeclaration(node);
    } finally {
      isEnclosingConstructorConst = false;
      enclosingFunction = outerFunction;
    }
  }

  @Override
  public Void visitDoStatement(DoStatement node) {
    checkForNonBoolCondition(node.getCondition());
    return super.visitDoStatement(node);
  }

  @Override
  public Void visitExtendsClause(ExtendsClause node) {
    checkForExtendsDisallowedClass(node);
    return super.visitExtendsClause(node);
  }

  @Override
  public Void visitFieldFormalParameter(FieldFormalParameter node) {
    checkForConstFormalParameter(node);
    checkForFieldInitializerOutsideConstructor(node);
    return super.visitFieldFormalParameter(node);
  }

  @Override
  public Void visitFunctionDeclaration(FunctionDeclaration node) {
    ExecutableElement outerFunction = enclosingFunction;
    try {
      enclosingFunction = node.getElement();
      return super.visitFunctionDeclaration(node);
    } finally {
      enclosingFunction = outerFunction;
    }
  }

  @Override
  public Void visitFunctionExpression(FunctionExpression node) {
    ExecutableElement outerFunction = enclosingFunction;
    try {
      enclosingFunction = node.getElement();
      return super.visitFunctionExpression(node);
    } finally {
      enclosingFunction = outerFunction;
    }
  }

  @Override
  public Void visitFunctionTypeAlias(FunctionTypeAlias node) {
    checkForBuiltInIdentifierAsName(
        node.getName(),
        CompileTimeErrorCode.BUILT_IN_IDENTIFIER_AS_TYPEDEF_NAME);
    checkForDefaultValueInFunctionTypeAlias(node);
    return super.visitFunctionTypeAlias(node);
  }

  @Override
  public Void visitIfStatement(IfStatement node) {
    checkForNonBoolCondition(node.getCondition());
    return super.visitIfStatement(node);
  }

  @Override
  public Void visitImplementsClause(ImplementsClause node) {
    checkForImplementsDisallowedClass(node);
    return super.visitImplementsClause(node);
  }

  @Override
  public Void visitInstanceCreationExpression(InstanceCreationExpression node) {
    ConstructorName constructorName = node.getConstructorName();
    TypeName typeName = constructorName.getType();
    Type type = typeName.getType();
    if (type instanceof InterfaceType) {
      InterfaceType interfaceType = (InterfaceType) type;
      checkForConstWithNonConst(node);
      checkForConstOrNewWithAbstractClass(node, typeName, interfaceType);
      // TODO(jwren) Email Luke to make this determination: Should we always call all checks, if not,
      // which order should they be called in?
      // (Should we provide as many errors as possible, or try to be as concise as possible?)
      checkForTypeArgumentNotMatchingBounds(node, constructorName.getElement(), typeName);
    }
    return super.visitInstanceCreationExpression(node);
  }

  @Override
  public Void visitMethodDeclaration(MethodDeclaration node) {
    ExecutableElement previousFunction = enclosingFunction;
    try {
      enclosingFunction = node.getElement();
      return super.visitMethodDeclaration(node);
    } finally {
      enclosingFunction = previousFunction;
    }
  }

  @Override
  public Void visitNativeFunctionBody(NativeFunctionBody node) {
    checkForNativeFunctionBodyInNonSDKCode(node);
    return super.visitNativeFunctionBody(node);
  }

  @Override
  public Void visitRethrowExpression(RethrowExpression node) {
    checkForRethrowOutsideCatch(node);
    return super.visitRethrowExpression(node);
  }

  @Override
  public Void visitReturnStatement(ReturnStatement node) {
    checkForReturnOfInvalidType(node);
    return super.visitReturnStatement(node);
  }

  @Override
  public Void visitSimpleFormalParameter(SimpleFormalParameter node) {
    checkForConstFormalParameter(node);
    return super.visitSimpleFormalParameter(node);
  }

  @Override
  public Void visitSwitchStatement(SwitchStatement node) {
    checkForCaseExpressionTypeImplementsEquals(node);
    return super.visitSwitchStatement(node);
  }

  @Override
  public Void visitThrowExpression(ThrowExpression node) {
    checkForConstEvalThrowsException(node);
    return super.visitThrowExpression(node);
  }

  @Override
  public Void visitTopLevelVariableDeclaration(TopLevelVariableDeclaration node) {
    checkForFinalNotInitialized(node.getVariables());
    return super.visitTopLevelVariableDeclaration(node);
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
    checkForBuiltInIdentifierAsName(node);
    return super.visitVariableDeclarationList(node);
  }

  @Override
  public Void visitVariableDeclarationStatement(VariableDeclarationStatement node) {
    checkForFinalNotInitialized(node.getVariables());
    return super.visitVariableDeclarationStatement(node);
  }

  @Override
  public Void visitWhileStatement(WhileStatement node) {
    checkForNonBoolCondition(node.getCondition());
    return super.visitWhileStatement(node);
  }

  /**
   * This verifies that the passed constructor declaration does not violate any of the error codes
   * relating to the initialization of fields in the enclosing class.
   * 
   * @param node the {@link ConstructorDeclaration} to evaluate
   * @return return {@code true} if and only if an error code is generated on the passed node
   * @see #initialFieldElementsMap
   * @see CompileTimeErrorCode#FINAL_INITIALIZED_IN_DECLARATION_AND_CONSTRUCTOR
   * @see CompileTimeErrorCode#FINAL_INITIALIZED_MULTIPLE_TIMES
   */
  private boolean checkForAllFinalInitializedErrorCodes(ConstructorDeclaration node) {
    if (node.getFactoryKeyword() != null || node.getRedirectedConstructor() != null
        || node.getExternalKeyword() != null) {
      return false;
    }
    boolean foundError = false;
    HashMap<FieldElement, INIT_STATE> fieldElementsMap = new HashMap<FieldElement, INIT_STATE>(
        initialFieldElementsMap);

    // Visit all of the field formal parameters
    NodeList<FormalParameter> formalParameters = node.getParameters().getParameters();
    for (FormalParameter formalParameter : formalParameters) {
      FormalParameter parameter = formalParameter;
      if (parameter instanceof DefaultFormalParameter) {
        parameter = ((DefaultFormalParameter) parameter).getParameter();
      }
      if (parameter instanceof FieldFormalParameter) {
        FieldElement fieldElement = ((FieldFormalParameterElementImpl) parameter.getElement()).getField();
        INIT_STATE state = fieldElementsMap.get(fieldElement);
        if (state == INIT_STATE.NOT_INIT) {
          fieldElementsMap.put(fieldElement, INIT_STATE.INIT_IN_FIELD_FORMAL);
        } else if (state == INIT_STATE.INIT_IN_DECLARATION) {
          if (fieldElement.isFinal() || fieldElement.isConst()) {
            errorReporter.reportError(
                CompileTimeErrorCode.FINAL_INITIALIZED_IN_DECLARATION_AND_CONSTRUCTOR,
                formalParameter.getIdentifier(),
                fieldElement.getName());
            foundError = true;
          }
        } else if (state == INIT_STATE.INIT_IN_FIELD_FORMAL) {
          if (fieldElement.isFinal() || fieldElement.isConst()) {
            errorReporter.reportError(
                CompileTimeErrorCode.FINAL_INITIALIZED_MULTIPLE_TIMES,
                formalParameter.getIdentifier(),
                fieldElement.getName());
            foundError = true;
          }
        }
      }
    }

    // Visit all of the initializers
    NodeList<ConstructorInitializer> initializers = node.getInitializers();
    for (ConstructorInitializer constructorInitializer : initializers) {
      if (constructorInitializer instanceof ConstructorFieldInitializer) {
        ConstructorFieldInitializer constructorFieldInitializer = (ConstructorFieldInitializer) constructorInitializer;
        SimpleIdentifier fieldName = constructorFieldInitializer.getFieldName();
        Element element = fieldName.getElement();
        if (element instanceof FieldElement) {
          FieldElement fieldElement = (FieldElement) element;
          INIT_STATE state = fieldElementsMap.get(fieldElement);
          if (state == INIT_STATE.NOT_INIT) {
            fieldElementsMap.put(fieldElement, INIT_STATE.INIT_IN_INITIALIZERS);
          } else if (state == INIT_STATE.INIT_IN_DECLARATION) {
            if (fieldElement.isFinal() || fieldElement.isConst()) {
              errorReporter.reportError(
                  CompileTimeErrorCode.FIELD_INITIALIZED_IN_INITIALIZER_AND_DECLARATION,
                  fieldName);
              foundError = true;
            }
          } else if (state == INIT_STATE.INIT_IN_FIELD_FORMAL) {
            errorReporter.reportError(
                CompileTimeErrorCode.FIELD_INITIALIZED_IN_PARAMETER_AND_INITIALIZER,
                fieldName);
            foundError = true;
          } else if (state == INIT_STATE.INIT_IN_INITIALIZERS) {
            errorReporter.reportError(
                CompileTimeErrorCode.FIELD_INITIALIZED_BY_MULTIPLE_INITIALIZERS,
                fieldName,
                fieldElement.getName());
            foundError = true;
          }
//          else if (variableElement instanceof TopLevelVariableElement) {
          // TODO(jwren) Report error, constructor initializer variable is a top level element
          // (Either here or in ElementResolver#visitFieldFormalParameter)
//          }
        }
//        else {
        // TODO(jwren) Do we need to consider this branch?
//        }
      }
    }

    // Before we do the final check for FINAL_NOT_INITIALIZED, first we loop through all of the
    // parameters that have default values to set INIT_IN_DEFAULT_VALUE onto the FieldElement in our
    // fieldElementsMap.
//    for (FormalParameter formalParameter : formalParameters) {
//      if (formalParameter instanceof DefaultFormalParameter) {
//        DefaultFormalParameter defaultFormalParameter = (DefaultFormalParameter) formalParameter;
//        if (defaultFormalParameter.getDefaultValue() != null) {
//          // TODO(jwren) Need associated field element:
//          //fieldElementsMap.put(??, INIT_STATE.INIT_IN_DEFAULT_VALUE);
//        }
//      }
//    }

    // Visit all of the states in the map to ensure that none were never initialized
    // TODO(jwren) revisit this block- lots of false positives are generated by the SDK Analysis test
    // Specifically, need Dart language question answered concerning formal function parameters
    // i.e., code like "(..., int this.f(..), ...)".
    // See test at CompileTimeErrorCodeTest.test_finalNotInitialized_inConstructor()
//    Set<Entry<FieldElement, INIT_STATE>> set = fieldElementsMap.entrySet();
//    for (Entry<FieldElement, INIT_STATE> entry : set) {
//      if (entry.getValue() == INIT_STATE.NOT_INIT) {
//        FieldElement fieldElement = entry.getKey();
//        if (fieldElement.isFinal() || fieldElement.isConst()) {
//          errorReporter.reportError(
//              CompileTimeErrorCode.FINAL_NOT_INITIALIZED,
//              node.getReturnType(),
//              fieldElement.getName());
//          foundError = true;
//        }
//      }
//    }
    return foundError;
  }

  /**
   * This verifies that the passed argument definition test identifier is a parameter.
   * 
   * @param node the {@link ArgumentDefinitionTest} to evaluate
   * @return return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#ARGUMENT_DEFINITION_TEST_NON_PARAMETER
   */
  private boolean checkForArgumentDefinitionTestNonParameter(ArgumentDefinitionTest node) {
    SimpleIdentifier identifier = node.getIdentifier();
    Element element = identifier.getElement();
    if (element != null && !(element instanceof ParameterElement)) {
      errorReporter.reportError(
          CompileTimeErrorCode.ARGUMENT_DEFINITION_TEST_NON_PARAMETER,
          identifier,
          identifier.getName());
      return true;
    }
    return false;
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
   * @return return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#BUILT_IN_IDENTIFIER_AS_TYPE_NAME
   * @see CompileTimeErrorCode#BUILT_IN_IDENTIFIER_AS_TYPE_VARIABLE_NAME
   * @see CompileTimeErrorCode#BUILT_IN_IDENTIFIER_AS_TYPEDEF_NAME
   */
  private boolean checkForBuiltInIdentifierAsName(SimpleIdentifier identifier, ErrorCode errorCode) {
    Token token = identifier.getToken();
    if (token.getType() == TokenType.KEYWORD) {
      errorReporter.reportError(errorCode, identifier, identifier.getName());
      return true;
    }
    return false;
  }

  /**
   * This verifies that the passed variable declaration list does not have a built-in identifier.
   * 
   * @param node the variable declaration list to check
   * @return return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#BUILT_IN_IDENTIFIER_AS_TYPE
   */
  private boolean checkForBuiltInIdentifierAsName(VariableDeclarationList node) {
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
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * This verifies that the passed switch statement does not have a case expression with the
   * operator '==' overridden.
   * 
   * @param node the switch statement to evaluate
   * @return return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#CASE_EXPRESSION_TYPE_IMPLEMENTS_EQUALS
   */
  private boolean checkForCaseExpressionTypeImplementsEquals(SwitchStatement node) {
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
          return true;
        }
      }
    }
    return false;
  }

  // TODO(jwren) replace this method with a generic "conflicting" error code evaluation
  private boolean checkForConflictingConstructorNameAndMember(ConstructorDeclaration node) {
    ConstructorElement constructorElement = node.getElement();
    SimpleIdentifier constructorName = node.getName();
    if (constructorName != null && constructorElement != null && !constructorName.isSynthetic()) {
      String name = constructorName.getName();
      ClassElement classElement = constructorElement.getEnclosingElement();
      FieldElement[] fields = classElement.getFields();
      for (FieldElement field : fields) {
        if (field.getName().equals(name)) {
          errorReporter.reportError(
              CompileTimeErrorCode.CONFLICTING_CONSTRUCTOR_NAME_AND_FIELD,
              node,
              name);
          return true;
        }
      }
      MethodElement[] methods = classElement.getMethods();
      for (MethodElement method : methods) {
        if (method.getName().equals(name)) {
          errorReporter.reportError(
              CompileTimeErrorCode.CONFLICTING_CONSTRUCTOR_NAME_AND_METHOD,
              node,
              name);
          return true;
        }
      }
    }
    return false;
  }

  /**
   * This verifies that the passed constructor declaration is not 'const' if it has a non-final
   * instance variable.
   * 
   * @param node the instance creation expression to evaluate
   * @return return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#CONST_CONSTRUCTOR_WITH_NON_FINAL_FIELD
   */
  private boolean checkForConstConstructorWithNonFinalField(ConstructorDeclaration node) {
    if (!isEnclosingConstructorConst) {
      return false;
    }
    ConstructorElement constructorElement = node.getElement();
    if (constructorElement != null) {
      ClassElement classElement = constructorElement.getEnclosingElement();
      FieldElement[] elements = classElement.getFields();
      for (FieldElement field : elements) {
        if (!field.isFinal() && !field.isConst() && !field.isStatic() && !field.isSynthetic()) {
          errorReporter.reportError(
              CompileTimeErrorCode.CONST_CONSTRUCTOR_WITH_NON_FINAL_FIELD,
              node);
          return true;
        }
      }
    }
    return false;
  }

  /**
   * This verifies that the passed throw expression is not enclosed in a 'const' constructor
   * declaration.
   * 
   * @param node the throw expression expression to evaluate
   * @return return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#CONST_EVAL_THROWS_EXCEPTION
   */
  private boolean checkForConstEvalThrowsException(ThrowExpression node) {
    if (isEnclosingConstructorConst) {
      errorReporter.reportError(CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION, node);
      return true;
    }
    return false;
  }

  /**
   * This verifies that the passed normal formal parameter is not 'const'.
   * 
   * @param node the normal formal parameter to evaluate
   * @return return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#CONST_FORMAL_PARAMETER
   */
  private boolean checkForConstFormalParameter(NormalFormalParameter node) {
    if (node.isConst()) {
      errorReporter.reportError(CompileTimeErrorCode.CONST_FORMAL_PARAMETER, node);
      return true;
    }
    return false;
  }

  /**
   * This verifies that the passed instance creation expression is not being invoked on an abstract
   * class.
   * 
   * @param node the instance creation expression to evaluate
   * @param typeName the {@link TypeName} of the {@link ConstructorName} from the
   *          {@link InstanceCreationExpression}, this is the AST node that the error is attached to
   * @param type the type being constructed with this {@link InstanceCreationExpression}
   * @return return {@code true} if and only if an error code is generated on the passed node
   * @see StaticWarningCode#CONST_WITH_ABSTRACT_CLASS
   * @see StaticWarningCode#NEW_WITH_ABSTRACT_CLASS
   */
  private boolean checkForConstOrNewWithAbstractClass(InstanceCreationExpression node,
      TypeName typeName, InterfaceType type) {
    if (type.getElement().isAbstract()) {
      ConstructorElement element = node.getElement();
      if (element != null && !element.isFactory()) {
        if (((KeywordToken) node.getKeyword()).getKeyword() == Keyword.CONST) {
          errorReporter.reportError(StaticWarningCode.CONST_WITH_ABSTRACT_CLASS, typeName);
        } else {
          errorReporter.reportError(StaticWarningCode.NEW_WITH_ABSTRACT_CLASS, typeName);
        }
        return true;
      }
    }
    return false;
  }

  /**
   * This verifies that if the passed instance creation expression is 'const', then it is not being
   * invoked on a constructor that is not 'const'.
   * 
   * @param node the instance creation expression to evaluate
   * @return return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#CONST_WITH_NON_CONST
   */
  private boolean checkForConstWithNonConst(InstanceCreationExpression node) {
    ConstructorElement constructorElement = node.getElement();
    if (node.isConst() && constructorElement != null && !constructorElement.isConst()) {
      errorReporter.reportError(CompileTimeErrorCode.CONST_WITH_NON_CONST, node);
      return true;
    }
    return false;
  }

  /**
   * This verifies that there are no default parameters in the passed function type alias.
   * 
   * @param node the function type alias to evaluate
   * @return return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#DEFAULT_VALUE_IN_FUNCTION_TYPE_ALIAS
   */
  private boolean checkForDefaultValueInFunctionTypeAlias(FunctionTypeAlias node) {
    boolean result = false;
    FormalParameterList formalParameterList = node.getParameters();
    NodeList<FormalParameter> parameters = formalParameterList.getParameters();
    for (FormalParameter formalParameter : parameters) {
      if (formalParameter instanceof DefaultFormalParameter) {
        DefaultFormalParameter defaultFormalParameter = (DefaultFormalParameter) formalParameter;
        if (defaultFormalParameter.getDefaultValue() != null) {
          errorReporter.reportError(CompileTimeErrorCode.DEFAULT_VALUE_IN_FUNCTION_TYPE_ALIAS, node);
          result = true;
        }
      }
    }
    return result;
  }

  /**
   * This verifies that the passed extends clause does not extend classes such as num or String.
   * 
   * @param node the extends clause to test
   * @return return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#EXTENDS_DISALLOWED_CLASS
   */
  private boolean checkForExtendsDisallowedClass(ExtendsClause extendsClause) {
    return checkForExtendsOrImplementsDisallowedClass(
        extendsClause.getSuperclass(),
        CompileTimeErrorCode.EXTENDS_DISALLOWED_CLASS);
  }

  /**
   * This verifies that the passed type name does not extend or implement classes such as 'num' or
   * 'String'.
   * 
   * @param node the type name to test
   * @return return {@code true} if and only if an error code is generated on the passed node
   * @see #checkForExtendsDisallowedClass(ExtendsClause)
   * @see #checkForImplementsDisallowedClass(ImplementsClause)
   * @see CompileTimeErrorCode#EXTENDS_DISALLOWED_CLASS
   * @see CompileTimeErrorCode#IMPLEMENTS_DISALLOWED_CLASS
   */
  private boolean checkForExtendsOrImplementsDisallowedClass(TypeName typeName, ErrorCode errorCode) {
    if (typeName.isSynthetic()) {
      return false;
    }
    Type superType = typeName.getType();
    for (InterfaceType disallowedType : DISALLOWED_TYPES_TO_EXTEND_OR_IMPLEMENT) {
      if (superType != null && superType.equals(disallowedType)) {
        // if the violating type happens to be 'num', we need to rule out the case where the
        // enclosing class is 'int' or 'double'
        if (superType.equals(typeProvider.getNumType())) {
          ASTNode grandParent = typeName.getParent().getParent();
          // Note: this is a corner case that won't happen often, so adding a field currentClass
          // (see currentFunction) to ErrorVerifier isn't worth if for this case, but if the field
          // currentClass is added, then this message should become a todo to not lookup the
          // grandparent node
          if (grandParent instanceof ClassDeclaration) {
            ClassElement classElement = ((ClassDeclaration) grandParent).getElement();
            Type classType = classElement.getType();
            if (classType != null
                && (classType.equals(typeProvider.getIntType()) || classType.equals(typeProvider.getDoubleType()))) {
              return false;
            }
          }
        }
        // otherwise, report the error
        errorReporter.reportError(errorCode, typeName, disallowedType.getName());
        return true;
      }
    }
    return false;
  }

  /**
   * This verifies that the passed field formal parameter is in a constructor declaration.
   * 
   * @param node the field formal parameter to test
   * @return return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#FIELD_INITIALIZER_OUTSIDE_CONSTRUCTOR
   */
  private boolean checkForFieldInitializerOutsideConstructor(FieldFormalParameter node) {
    ASTNode parent = node.getParent();
    if (parent != null) {
      ASTNode grandparent = parent.getParent();
      // If this is not an error case, then parent is a FormalParameterList and the grandparent is a
      // ConstructorDeclaration, or the parent is a DefaultFormalParameter and grandparent is a
      // FormalParameter [with ConstructorDeclaration being its parent],
      if (grandparent != null && !(grandparent instanceof ConstructorDeclaration)
          && !(grandparent.getParent() instanceof ConstructorDeclaration)) {
        errorReporter.reportError(CompileTimeErrorCode.FIELD_INITIALIZER_OUTSIDE_CONSTRUCTOR, node);
        return true;
      }
    }
    return false;
  }

  /**
   * This verifies that final fields that are declared, without any constructors in the enclosing
   * class, are initialized. Cases in which there is at least one constructor are handled at the end
   * of {@link #checkForAllFinalInitializedErrorCodes(ConstructorDeclaration)}.
   * 
   * @param node the class declaration to test
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#FINAL_NOT_INITIALIZED
   */
  private boolean checkForFinalNotInitialized(ClassDeclaration node) {
    NodeList<ClassMember> classMembers = node.getMembers();
    for (ClassMember classMember : classMembers) {
      if (classMember instanceof ConstructorDeclaration) {
        return false;
      }
    }
    boolean foundError = false;
    for (ClassMember classMember : classMembers) {
      if (classMember instanceof FieldDeclaration) {
        FieldDeclaration field = (FieldDeclaration) classMember;
        foundError = foundError | checkForFinalNotInitialized(field.getFields());
      }
    }
    return foundError;
  }

  /**
   * This verifies that the passed variable declaration list has only initialized variables if the
   * list is final or const. This method is called by
   * {@link #checkForFinalNotInitialized(ClassDeclaration)},
   * {@link #visitTopLevelVariableDeclaration(TopLevelVariableDeclaration)} and
   * {@link #visitVariableDeclarationStatement(VariableDeclarationStatement)}.
   * 
   * @param node the class declaration to test
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#FINAL_NOT_INITIALIZED
   */
  private boolean checkForFinalNotInitialized(VariableDeclarationList node) {
    boolean foundError = false;
    if (!node.isSynthetic() && (node.isConst() || node.isFinal())) {
      NodeList<VariableDeclaration> variables = node.getVariables();
      for (VariableDeclaration variable : variables) {
        if (variable.getInitializer() == null) {
          errorReporter.reportError(
              CompileTimeErrorCode.FINAL_NOT_INITIALIZED,
              variable,
              variable.getName().getName());
          foundError = true;
        }
      }
    }
    return foundError;
  }

  /**
   * This verifies that the passed implements clause does not implement classes such as 'num' or
   * 'String'.
   * 
   * @param node the implements clause to test
   * @return return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#IMPLEMENTS_DISALLOWED_CLASS
   */
  private boolean checkForImplementsDisallowedClass(ImplementsClause implementsClause) {
    boolean foundError = false;
    for (TypeName type : implementsClause.getInterfaces()) {
      foundError = foundError
          | checkForExtendsOrImplementsDisallowedClass(
              type,
              CompileTimeErrorCode.IMPLEMENTS_DISALLOWED_CLASS);
    }
    return foundError;
  }

  /**
   * This verifies that the passed assignment expression represents a valid assignment.
   * 
   * @param node the assignment expression to evaluate
   * @return return {@code true} if and only if an error code is generated on the passed node
   * @see StaticTypeWarningCode#INVALID_ASSIGNMENT
   */
  private boolean checkForInvalidAssignment(AssignmentExpression node) {
    Expression lhs = node.getLeftHandSide();
    Expression rhs = node.getRightHandSide();
    VariableElement leftElement = getVariableElement(lhs);
    Type leftType = (leftElement == null) ? getType(lhs) : leftElement.getType();
    Type rightType = getType(rhs);
    if (!rightType.isAssignableTo(leftType)) {
      errorReporter.reportError(
          StaticTypeWarningCode.INVALID_ASSIGNMENT,
          rhs,
          rightType.getName(),
          leftType.getName());
      return true;
    }
    return false;
  }

  /**
   * Checks to ensure that native function bodies can only in SDK code.
   * 
   * @param node the native function body to test
   * @return return {@code true} if and only if an error code is generated on the passed node
   * @see ParserErrorCode#NATIVE_FUNCTION_BODY_IN_NON_SDK_CODE
   */
  private boolean checkForNativeFunctionBodyInNonSDKCode(NativeFunctionBody node) {
    if (!isInSystemLibrary) {
      errorReporter.reportError(ParserErrorCode.NATIVE_FUNCTION_BODY_IN_NON_SDK_CODE, node);
      return true;
    }
    return false;
  }

  /**
   * Checks to ensure that the expressions that need to be of type bool, are. Otherwise an error is
   * reported on the expression.
   * 
   * @param condition the conditional expression to test
   * @return return {@code true} if and only if an error code is generated on the passed node
   * @see StaticTypeWarningCode#NON_BOOL_CONDITION
   */
  private boolean checkForNonBoolCondition(Expression condition) {
    Type conditionType = getType(condition);
    if (conditionType != null && !conditionType.isAssignableTo(typeProvider.getBoolType())) {
      errorReporter.reportError(StaticTypeWarningCode.NON_BOOL_CONDITION, condition);
      return true;
    }
    return false;
  }

  /**
   * This verifies that the passed assert statement has either a 'bool' or '() -> bool' input.
   * 
   * @param node the assert statement to evaluate
   * @return return {@code true} if and only if an error code is generated on the passed node
   * @see StaticTypeWarningCode#NON_BOOL_EXPRESSION
   */
  private boolean checkForNonBoolExpression(AssertStatement node) {
    Expression expression = node.getCondition();
    Type type = getType(expression);
    if (type instanceof InterfaceType) {
      if (!type.isAssignableTo(typeProvider.getBoolType())) {
        errorReporter.reportError(StaticTypeWarningCode.NON_BOOL_EXPRESSION, expression);
        return true;
      }
    } else if (type instanceof FunctionType) {
      FunctionType functionType = (FunctionType) type;
      if (functionType.getTypeArguments().length == 0
          && !functionType.getReturnType().isAssignableTo(typeProvider.getBoolType())) {
        errorReporter.reportError(StaticTypeWarningCode.NON_BOOL_EXPRESSION, expression);
        return true;
      }
    }
    return false;
  }

  /**
   * This checks that the rethrow is inside of a catch clause.
   * 
   * @param node the rethrow expression to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#RETHROW_OUTSIDE_CATCH
   */
  private boolean checkForRethrowOutsideCatch(RethrowExpression node) {
    if (!isInCatchClause) {
      errorReporter.reportError(CompileTimeErrorCode.RETHROW_OUTSIDE_CATCH, node);
      return true;
    }
    return false;
  }

  /**
   * This checks that the return type matches the type of the declared return type in the enclosing
   * method or function.
   * 
   * @param node the return statement to evaluate
   * @return return {@code true} if and only if an error code is generated on the passed node
   * @see StaticTypeWarningCode#RETURN_OF_INVALID_TYPE
   */
  private boolean checkForReturnOfInvalidType(ReturnStatement node) {
    FunctionType functionType = enclosingFunction == null ? null : enclosingFunction.getType();
    Type expectedReturnType = functionType == null ? null : functionType.getReturnType();
    Expression returnExpression = node.getExpression();
    if (expectedReturnType != null && !expectedReturnType.isVoid() && returnExpression != null) {
      Type actualReturnType = getType(returnExpression);
      if (!actualReturnType.isAssignableTo(expectedReturnType)) {
        errorReporter.reportError(
            StaticTypeWarningCode.RETURN_OF_INVALID_TYPE,
            returnExpression,
            actualReturnType.getName(),
            expectedReturnType.getName(),
            enclosingFunction.getName());
        return true;
      }
    }
    return false;
  }

  /**
   * This verifies that the type arguments in the passed instance creation expression are all within
   * their bounds as specified by the class element where the constructor [that is being invoked] is
   * declared.
   * 
   * @param node the instance creation expression to evaluate
   * @param typeName the {@link TypeName} of the {@link ConstructorName} from the
   *          {@link InstanceCreationExpression}, this is the AST node that the error is attached to
   * @param constructorElement the {@link ConstructorElement} from the instance creation expression
   * @return return {@code true} if and only if an error code is generated on the passed node
   * @see StaticTypeWarningCode#TYPE_ARGUMENT_NOT_MATCHING_BOUNDS
   */
  private boolean checkForTypeArgumentNotMatchingBounds(InstanceCreationExpression node,
      ConstructorElement constructorElement, TypeName typeName) {
    if (typeName.getTypeArguments() != null && constructorElement != null) {
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
            return true;
          }
        }
      }
    }
    return false;
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

  /**
   * Return the variable element represented by the given expression, or {@code null} if there is no
   * such element.
   * 
   * @param expression the expression whose element is to be returned
   * @return the variable element represented by the expression
   */
  private VariableElement getVariableElement(Expression expression) {
    if (expression instanceof Identifier) {
      Element element = ((Identifier) expression).getElement();
      if (element instanceof VariableElement) {
        return (VariableElement) element;
      }
    }
    return null;
  }
}
