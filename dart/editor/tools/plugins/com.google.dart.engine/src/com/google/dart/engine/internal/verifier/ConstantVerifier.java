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

import com.google.dart.engine.ast.Annotation;
import com.google.dart.engine.ast.ArgumentList;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.ConstructorFieldInitializer;
import com.google.dart.engine.ast.ConstructorInitializer;
import com.google.dart.engine.ast.DefaultFormalParameter;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FormalParameter;
import com.google.dart.engine.ast.FormalParameterList;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.ListLiteral;
import com.google.dart.engine.ast.MapLiteral;
import com.google.dart.engine.ast.MapLiteralEntry;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.NamedExpression;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.RedirectingConstructorInvocation;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SuperConstructorInvocation;
import com.google.dart.engine.ast.SwitchCase;
import com.google.dart.engine.ast.SwitchMember;
import com.google.dart.engine.ast.SwitchStatement;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.visitor.RecursiveAstVisitor;
import com.google.dart.engine.constant.DartObject;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.internal.constant.ConstantVisitor;
import com.google.dart.engine.internal.constant.ErrorResult;
import com.google.dart.engine.internal.constant.EvaluationResultImpl;
import com.google.dart.engine.internal.constant.ValidResult;
import com.google.dart.engine.internal.element.VariableElementImpl;
import com.google.dart.engine.internal.error.ErrorReporter;
import com.google.dart.engine.internal.object.BoolState;
import com.google.dart.engine.internal.object.DartObjectImpl;
import com.google.dart.engine.internal.object.DoubleState;
import com.google.dart.engine.internal.object.DynamicState;
import com.google.dart.engine.internal.object.GenericState;
import com.google.dart.engine.internal.object.InstanceState;
import com.google.dart.engine.internal.object.IntState;
import com.google.dart.engine.internal.object.NumState;
import com.google.dart.engine.internal.object.StringState;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.ast.DeferredLibraryReferenceDetector;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Instances of the class {@code ConstantVerifier} traverse an AST structure looking for additional
 * errors and warnings not covered by the parser and resolver. In particular, it looks for errors
 * and warnings related to constant expressions.
 * 
 * @coverage dart.engine.resolver
 */
public class ConstantVerifier extends RecursiveAstVisitor<Void> {
  /**
   * The error reporter by which errors will be reported.
   */
  private ErrorReporter errorReporter;

  /**
   * The type provider used to access the known types.
   */
  private TypeProvider typeProvider;

  /**
   * The type representing the type 'bool'.
   */
  private InterfaceType boolType;

  /**
   * The type representing the type 'int'.
   */
  private InterfaceType intType;

  /**
   * The type representing the type 'num'.
   */
  private InterfaceType numType;

  /**
   * The type representing the type 'string'.
   */
  private InterfaceType stringType;

  /**
   * The current library that is being analyzed.
   */
  private LibraryElement currentLibrary;

  /**
   * Initialize a newly created constant verifier.
   * 
   * @param errorReporter the error reporter by which errors will be reported
   */
  public ConstantVerifier(ErrorReporter errorReporter, LibraryElement currentLibrary,
      TypeProvider typeProvider) {
    this.errorReporter = errorReporter;
    this.currentLibrary = currentLibrary;
    this.typeProvider = typeProvider;
    this.boolType = typeProvider.getBoolType();
    this.intType = typeProvider.getIntType();
    this.numType = typeProvider.getNumType();
    this.stringType = typeProvider.getStringType();
  }

  @Override
  public Void visitAnnotation(Annotation node) {
    super.visitAnnotation(node);
    // check annotation creation
    Element element = node.getElement();
    if (element instanceof ConstructorElement) {
      ConstructorElement constructorElement = (ConstructorElement) element;
      // should 'const' constructor
      if (!constructorElement.isConst()) {
        errorReporter.reportErrorForNode(
            CompileTimeErrorCode.NON_CONSTANT_ANNOTATION_CONSTRUCTOR,
            node);
        return null;
      }
      // should have arguments
      ArgumentList argumentList = node.getArguments();
      if (argumentList == null) {
        errorReporter.reportErrorForNode(
            CompileTimeErrorCode.NO_ANNOTATION_CONSTRUCTOR_ARGUMENTS,
            node);
        return null;
      }
      // arguments should be constants
      validateConstantArguments(argumentList);
    }
    return null;
  }

  @Override
  public Void visitConstructorDeclaration(ConstructorDeclaration node) {
    if (node.getConstKeyword() != null) {
      validateInitializers(node);
    }
    validateDefaultValues(node.getParameters());
    return super.visitConstructorDeclaration(node);
  }

  @Override
  public Void visitFunctionExpression(FunctionExpression node) {
    super.visitFunctionExpression(node);
    validateDefaultValues(node.getParameters());
    return null;
  }

  @Override
  public Void visitInstanceCreationExpression(InstanceCreationExpression node) {
    if (node.isConst()) {
      validate(node, null);
    }
    validateInstanceCreationArguments(node);
    return super.visitInstanceCreationExpression(node);
  }

  @Override
  public Void visitListLiteral(ListLiteral node) {
    super.visitListLiteral(node);
    if (node.getConstKeyword() != null) {
      EvaluationResultImpl result;
      for (Expression element : node.getElements()) {
        result = validate(element, CompileTimeErrorCode.NON_CONSTANT_LIST_ELEMENT);
        if (result instanceof ValidResult) {
          reportErrorIfFromDeferredLibrary(
              element,
              CompileTimeErrorCode.NON_CONSTANT_LIST_ELEMENT_FROM_DEFERRED_LIBRARY);
        }
      }
    }
    return null;
  }

  @Override
  public Void visitMapLiteral(MapLiteral node) {
    super.visitMapLiteral(node);
    boolean isConst = node.getConstKeyword() != null;
    boolean reportEqualKeys = true;
    HashSet<DartObject> keys = new HashSet<DartObject>();
    ArrayList<Expression> invalidKeys = new ArrayList<Expression>();
    for (MapLiteralEntry entry : node.getEntries()) {
      Expression key = entry.getKey();
      if (isConst) {
        EvaluationResultImpl keyResult = validate(key, CompileTimeErrorCode.NON_CONSTANT_MAP_KEY);
        Expression valueExpression = entry.getValue();
        EvaluationResultImpl valueResult = validate(
            valueExpression,
            CompileTimeErrorCode.NON_CONSTANT_MAP_VALUE);
        if (valueResult instanceof ValidResult) {
          reportErrorIfFromDeferredLibrary(
              valueExpression,
              CompileTimeErrorCode.NON_CONSTANT_MAP_VALUE_FROM_DEFERRED_LIBRARY);
        }
        if (keyResult instanceof ValidResult) {
          reportErrorIfFromDeferredLibrary(
              key,
              CompileTimeErrorCode.NON_CONSTANT_MAP_KEY_FROM_DEFERRED_LIBRARY);
          DartObject value = ((ValidResult) keyResult).getValue();
          if (keys.contains(value)) {
            invalidKeys.add(key);
          } else {
            keys.add(value);
          }
          Type type = value.getType();
          if (implementsEqualsWhenNotAllowed(type)) {
            errorReporter.reportErrorForNode(
                CompileTimeErrorCode.CONST_MAP_KEY_EXPRESSION_TYPE_IMPLEMENTS_EQUALS,
                key,
                type.getDisplayName());
          }
        }
      } else {
        EvaluationResultImpl result = key.accept(new ConstantVisitor(typeProvider));
        if (result instanceof ValidResult) {
          DartObject value = ((ValidResult) result).getValue();
          if (keys.contains(value)) {
            invalidKeys.add(key);
          } else {
            keys.add(value);
          }
        } else {
          reportEqualKeys = false;
        }
      }
    }
    if (reportEqualKeys) {
      for (Expression key : invalidKeys) {
        errorReporter.reportErrorForNode(StaticWarningCode.EQUAL_KEYS_IN_MAP, key);
      }
    }
    return null;
  }

  @Override
  public Void visitMethodDeclaration(MethodDeclaration node) {
    super.visitMethodDeclaration(node);
    validateDefaultValues(node.getParameters());
    return null;
  }

  @Override
  public Void visitSwitchStatement(SwitchStatement node) {
    // TODO(paulberry): to minimize error messages, it would be nice to
    // compare all types with the most popular type rather than the first
    // type.
    NodeList<SwitchMember> switchMembers = node.getMembers();
    boolean foundError = false;
    Type firstType = null;
    for (SwitchMember switchMember : switchMembers) {
      if (switchMember instanceof SwitchCase) {
        SwitchCase switchCase = (SwitchCase) switchMember;
        Expression expression = switchCase.getExpression();
        EvaluationResultImpl caseResult = validate(
            expression,
            CompileTimeErrorCode.NON_CONSTANT_CASE_EXPRESSION);
        if (caseResult instanceof ValidResult) {
          reportErrorIfFromDeferredLibrary(
              expression,
              CompileTimeErrorCode.NON_CONSTANT_CASE_EXPRESSION_FROM_DEFERRED_LIBRARY);
          DartObject value = ((ValidResult) caseResult).getValue();
          if (firstType == null) {
            firstType = value.getType();
          } else {
            Type nType = value.getType();
            if (!firstType.equals(nType)) {
              errorReporter.reportErrorForNode(
                  CompileTimeErrorCode.INCONSISTENT_CASE_EXPRESSION_TYPES,
                  expression,
                  expression.toSource(),
                  firstType.getDisplayName());
              foundError = true;
            }
          }
        }
      }
    }
    if (!foundError) {
      checkForCaseExpressionTypeImplementsEquals(node, firstType);
    }
    return super.visitSwitchStatement(node);
  }

  @Override
  public Void visitVariableDeclaration(VariableDeclaration node) {
    super.visitVariableDeclaration(node);
    Expression initializer = node.getInitializer();
    if (initializer != null && node.isConst()) {
      VariableElementImpl element = (VariableElementImpl) node.getElement();
      EvaluationResultImpl result = element.getEvaluationResult();
      if (result == null) {
        //
        // Normally we don't need to visit const variable declarations because we have already
        // computed their values. But if we missed it for some reason, this gives us a second
        // chance.
        //
        result = validate(
            initializer,
            CompileTimeErrorCode.CONST_INITIALIZED_WITH_NON_CONSTANT_VALUE);
        element.setEvaluationResult(result);
        return null;
      } else if (result instanceof ErrorResult) {
        reportErrors(result, CompileTimeErrorCode.CONST_INITIALIZED_WITH_NON_CONSTANT_VALUE);
        return null;
      }
      reportErrorIfFromDeferredLibrary(
          initializer,
          CompileTimeErrorCode.CONST_INITIALIZED_WITH_NON_CONSTANT_VALUE_FROM_DEFERRED_LIBRARY);
    }
    return null;
  }

  /**
   * This verifies that the passed switch statement does not have a case expression with the
   * operator '==' overridden.
   * 
   * @param node the switch statement to evaluate
   * @param type the common type of all 'case' expressions
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#CASE_EXPRESSION_TYPE_IMPLEMENTS_EQUALS
   */
  private boolean checkForCaseExpressionTypeImplementsEquals(SwitchStatement node, Type type) {
    if (!implementsEqualsWhenNotAllowed(type)) {
      return false;
    }
    // report error
    errorReporter.reportErrorForToken(
        CompileTimeErrorCode.CASE_EXPRESSION_TYPE_IMPLEMENTS_EQUALS,
        node.getKeyword(),
        type.getDisplayName());
    return true;
  }

  /**
   * @return {@code true} if given {@link Type} implements operator <i>==</i>, and it is not
   *         <i>int</i> or <i>String</i>.
   */
  private boolean implementsEqualsWhenNotAllowed(Type type) {
    // ignore int or String
    if (type == null || type.equals(intType) || type.equals(typeProvider.getStringType())) {
      return false;
    } else if (type.equals(typeProvider.getDoubleType())) {
      return true;
    }
    // prepare ClassElement
    Element element = type.getElement();
    if (!(element instanceof ClassElement)) {
      return false;
    }
    ClassElement classElement = (ClassElement) element;
    // lookup for ==
    MethodElement method = classElement.lookUpConcreteMethod("==", currentLibrary);
    if (method == null || method.getEnclosingElement().getType().isObject()) {
      return false;
    }
    // there is == that we don't like
    return true;
  }

  /**
   * Given some computed {@link Expression}, this method generates the passed {@link ErrorCode} on
   * the node if its' value consists of information from a deferred library.
   * 
   * @param expression the expression to be tested for a deferred library reference
   * @param errorCode the error code to be used if the expression is or consists of a reference to a
   *          deferred library
   */
  private void reportErrorIfFromDeferredLibrary(Expression expression, ErrorCode errorCode) {
    DeferredLibraryReferenceDetector referenceDetector = new DeferredLibraryReferenceDetector();
    expression.accept(referenceDetector);
    if (referenceDetector.getResult()) {
      errorReporter.reportErrorForNode(errorCode, expression);
    }
  }

  /**
   * If the given result represents one or more errors, report those errors. Except for special
   * cases, use the given error code rather than the one reported in the error.
   * 
   * @param result the result containing any errors that need to be reported
   * @param errorCode the error code to be used if the result represents an error
   */
  private void reportErrors(EvaluationResultImpl result, ErrorCode errorCode) {
    if (result instanceof ErrorResult) {
      for (ErrorResult.ErrorData data : ((ErrorResult) result).getErrorData()) {
        ErrorCode dataErrorCode = data.getErrorCode();
        if (dataErrorCode == CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION
            || dataErrorCode == CompileTimeErrorCode.CONST_EVAL_THROWS_IDBZE
            || dataErrorCode == CompileTimeErrorCode.CONST_EVAL_TYPE_BOOL_NUM_STRING
            || dataErrorCode == CompileTimeErrorCode.CONST_EVAL_TYPE_BOOL
            || dataErrorCode == CompileTimeErrorCode.CONST_EVAL_TYPE_INT
            || dataErrorCode == CompileTimeErrorCode.CONST_EVAL_TYPE_NUM) {
          errorReporter.reportErrorForNode(dataErrorCode, data.getNode());
        } else if (errorCode != null) {
          errorReporter.reportErrorForNode(errorCode, data.getNode());
        }
      }
    }
  }

  private ValidResult valid(InterfaceType type, InstanceState state) {
    return new ValidResult(new DartObjectImpl(type, state));
  }

  /**
   * Validate that the given expression is a compile time constant. Return the value of the compile
   * time constant, or {@code null} if the expression is not a compile time constant.
   * 
   * @param expression the expression to be validated
   * @param errorCode the error code to be used if the expression is not a compile time constant
   * @return the value of the compile time constant
   */
  private EvaluationResultImpl validate(Expression expression, ErrorCode errorCode) {
    EvaluationResultImpl result = expression.accept(new ConstantVisitor(typeProvider));
    reportErrors(result, errorCode);
    return result;
  }

  /**
   * Validate that if the passed arguments are constant expressions.
   * 
   * @param argumentList the argument list to evaluate
   */
  private void validateConstantArguments(ArgumentList argumentList) {
    for (Expression argument : argumentList.getArguments()) {
      if (argument instanceof NamedExpression) {
        argument = ((NamedExpression) argument).getExpression();
      }
      validate(argument, CompileTimeErrorCode.CONST_WITH_NON_CONSTANT_ARGUMENT);
    }
  }

  /**
   * Validate that the default value associated with each of the parameters in the given list is a
   * compile time constant.
   * 
   * @param parameters the list of parameters to be validated
   */
  private void validateDefaultValues(FormalParameterList parameters) {
    if (parameters == null) {
      return;
    }
    for (FormalParameter parameter : parameters.getParameters()) {
      if (parameter instanceof DefaultFormalParameter) {
        DefaultFormalParameter defaultParameter = (DefaultFormalParameter) parameter;
        Expression defaultValue = defaultParameter.getDefaultValue();
        if (defaultValue != null) {
          EvaluationResultImpl result = validate(
              defaultValue,
              CompileTimeErrorCode.NON_CONSTANT_DEFAULT_VALUE);
          VariableElementImpl element = (VariableElementImpl) parameter.getElement();
          element.setEvaluationResult(result);
          if (result instanceof ValidResult) {
            reportErrorIfFromDeferredLibrary(
                defaultValue,
                CompileTimeErrorCode.NON_CONSTANT_DEFAULT_VALUE_FROM_DEFERRED_LIBRARY);
          }
        }
      }
    }
  }

  /**
   * Validates that the given expression is a compile time constant.
   * 
   * @param parameterElements the elements of parameters of constant constructor, they are
   *          considered as a valid potentially constant expressions
   * @param expression the expression to validate
   */
  private void validateInitializerExpression(final ParameterElement[] parameterElements,
      Expression expression) {
    EvaluationResultImpl result = expression.accept(new ConstantVisitor(typeProvider) {
      @Override
      public EvaluationResultImpl visitSimpleIdentifier(SimpleIdentifier node) {
        Element element = node.getStaticElement();
        for (ParameterElement parameterElement : parameterElements) {
          if (parameterElement == element && parameterElement != null) {
            Type type = parameterElement.getType();
            if (type != null) {
              if (type.isDynamic()) {
                return valid(typeProvider.getObjectType(), DynamicState.DYNAMIC_STATE);
              } else if (type.isSubtypeOf(boolType)) {
                return valid(typeProvider.getBoolType(), BoolState.UNKNOWN_VALUE);
              } else if (type.isSubtypeOf(typeProvider.getDoubleType())) {
                return valid(typeProvider.getDoubleType(), DoubleState.UNKNOWN_VALUE);
              } else if (type.isSubtypeOf(intType)) {
                return valid(typeProvider.getIntType(), IntState.UNKNOWN_VALUE);
              } else if (type.isSubtypeOf(numType)) {
                return valid(typeProvider.getNumType(), NumState.UNKNOWN_VALUE);
              } else if (type.isSubtypeOf(stringType)) {
                return valid(typeProvider.getStringType(), StringState.UNKNOWN_VALUE);
              }
              //
              // We don't test for other types of objects (such as List, Map, Function or Type)
              // because there are no operations allowed on such types other than '==' and '!=',
              // which means that we don't need to know the type when there is no specific data
              // about the state of such objects.
              //
            }
            return valid(
                type instanceof InterfaceType ? (InterfaceType) type : typeProvider.getObjectType(),
                GenericState.UNKNOWN_VALUE);
          }
        }
        return super.visitSimpleIdentifier(node);
      }
    });
    reportErrors(result, CompileTimeErrorCode.NON_CONSTANT_VALUE_IN_INITIALIZER);
    if (result instanceof ValidResult) {
      reportErrorIfFromDeferredLibrary(
          expression,
          CompileTimeErrorCode.NON_CONSTANT_VALUE_IN_INITIALIZER_FROM_DEFERRED_LIBRARY);
    }
  }

  /**
   * Validates that all of the arguments of a constructor initializer are compile time constants.
   * 
   * @param parameterElements the elements of parameters of constant constructor, they are
   *          considered as a valid potentially constant expressions
   * @param argumentList the argument list to validate
   */
  private void validateInitializerInvocationArguments(ParameterElement[] parameterElements,
      ArgumentList argumentList) {
    if (argumentList == null) {
      return;
    }
    for (Expression argument : argumentList.getArguments()) {
      validateInitializerExpression(parameterElements, argument);
    }
  }

  /**
   * Validates that the expressions of the given initializers (of a constant constructor) are all
   * compile time constants.
   * 
   * @param constructor the constant constructor declaration to validate
   */
  private void validateInitializers(ConstructorDeclaration constructor) {
    ParameterElement[] parameterElements = constructor.getParameters().getParameterElements();
    NodeList<ConstructorInitializer> initializers = constructor.getInitializers();
    for (ConstructorInitializer initializer : initializers) {
      if (initializer instanceof ConstructorFieldInitializer) {
        ConstructorFieldInitializer fieldInitializer = (ConstructorFieldInitializer) initializer;
        validateInitializerExpression(parameterElements, fieldInitializer.getExpression());
      }
      if (initializer instanceof RedirectingConstructorInvocation) {
        RedirectingConstructorInvocation invocation = (RedirectingConstructorInvocation) initializer;
        validateInitializerInvocationArguments(parameterElements, invocation.getArgumentList());
      }
      if (initializer instanceof SuperConstructorInvocation) {
        SuperConstructorInvocation invocation = (SuperConstructorInvocation) initializer;
        validateInitializerInvocationArguments(parameterElements, invocation.getArgumentList());
      }
    }
  }

  /**
   * Validate that if the passed instance creation is 'const' then all its arguments are constant
   * expressions.
   * 
   * @param node the instance creation evaluate
   */
  private void validateInstanceCreationArguments(InstanceCreationExpression node) {
    if (!node.isConst()) {
      return;
    }
    ArgumentList argumentList = node.getArgumentList();
    if (argumentList == null) {
      return;
    }
    validateConstantArguments(argumentList);
  }
}
