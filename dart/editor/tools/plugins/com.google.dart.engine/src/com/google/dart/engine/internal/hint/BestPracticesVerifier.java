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
package com.google.dart.engine.internal.hint;

import com.google.dart.engine.ast.ArgumentList;
import com.google.dart.engine.ast.AsExpression;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.BlockFunctionBody;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ConstructorName;
import com.google.dart.engine.ast.ExportDirective;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FunctionBody;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.HideCombinator;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.IndexExpression;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.IsExpression;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NullLiteral;
import com.google.dart.engine.ast.ParenthesizedExpression;
import com.google.dart.engine.ast.PostfixExpression;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.RedirectingConstructorInvocation;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SuperConstructorInvocation;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.visitor.RecursiveAstVisitor;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.HintCode;
import com.google.dart.engine.internal.error.ErrorReporter;
import com.google.dart.engine.internal.type.VoidTypeImpl;
import com.google.dart.engine.internal.verifier.ErrorVerifier;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.type.TypeParameterType;

/**
 * Instances of the class {@code BestPracticesVerifier} traverse an AST structure looking for
 * violations of Dart best practices.
 * 
 * @coverage dart.engine.resolver
 */
public class BestPracticesVerifier extends RecursiveAstVisitor<Void> {

  private static final String HASHCODE_GETTER_NAME = "hashCode";

  private static final String NULL_TYPE_NAME = "Null";

  private static final String TO_INT_METHOD_NAME = "toInt";

  /**
   * Given a parenthesized expression, this returns the parent (or recursively grand-parent) of the
   * expression that is a parenthesized expression, but whose parent is not a parenthesized
   * expression.
   * <p>
   * For example given the code {@code (((e)))}: {@code (e) -> (((e)))}.
   * 
   * @param parenthesizedExpression some expression whose parent is a parenthesized expression
   * @return the first parent or grand-parent that is a parenthesized expression, that does not have
   *         a parenthesized expression parent
   */
  private static ParenthesizedExpression wrapParenthesizedExpression(
      ParenthesizedExpression parenthesizedExpression) {
    if (parenthesizedExpression.getParent() instanceof ParenthesizedExpression) {
      return wrapParenthesizedExpression((ParenthesizedExpression) parenthesizedExpression.getParent());
    }
    return parenthesizedExpression;
  }

  /**
   * The class containing the AST nodes being visited, or {@code null} if we are not in the scope of
   * a class.
   */
  private ClassElement enclosingClass;

  /**
   * The error reporter by which errors will be reported.
   */
  private ErrorReporter errorReporter;

  /**
   * Create a new instance of the {@link BestPracticesVerifier}.
   * 
   * @param errorReporter the error reporter
   */
  public BestPracticesVerifier(ErrorReporter errorReporter) {
    this.errorReporter = errorReporter;
  }

  @Override
  public Void visitArgumentList(ArgumentList node) {
    checkForArgumentTypesNotAssignableInList(node);
    return super.visitArgumentList(node);
  }

  @Override
  public Void visitAsExpression(AsExpression node) {
    checkForUnnecessaryCast(node);
    return super.visitAsExpression(node);
  }

  @Override
  public Void visitAssignmentExpression(AssignmentExpression node) {
    TokenType operatorType = node.getOperator().getType();
    if (operatorType == TokenType.EQ) {
      checkForUseOfVoidResult(node.getRightHandSide());
      checkForInvalidAssignment(node.getLeftHandSide(), node.getRightHandSide());
    } else {
      checkForDeprecatedMemberUse(node.getBestElement(), node);
    }
    return super.visitAssignmentExpression(node);
  }

  @Override
  public Void visitBinaryExpression(BinaryExpression node) {
    checkForDivisionOptimizationHint(node);
    checkForDeprecatedMemberUse(node.getBestElement(), node);
    return super.visitBinaryExpression(node);
  }

  @Override
  public Void visitClassDeclaration(ClassDeclaration node) {
    ClassElement outerClass = enclosingClass;
    try {
      enclosingClass = node.getElement();
      // Commented out until we decide that we want this hint in the analyzer
//    checkForOverrideEqualsButNotHashCode(node);
      return super.visitClassDeclaration(node);
    } finally {
      enclosingClass = outerClass;
    }
  }

  @Override
  public Void visitExportDirective(ExportDirective node) {
    checkForDeprecatedMemberUse(node.getUriElement(), node);
    return super.visitExportDirective(node);
  }

  @Override
  public Void visitFunctionDeclaration(FunctionDeclaration node) {
    checkForMissingReturn(node.getReturnType(), node.getFunctionExpression().getBody());
    return super.visitFunctionDeclaration(node);
  }

  @Override
  public Void visitImportDirective(ImportDirective node) {
    checkForDeprecatedMemberUse(node.getUriElement(), node);
    ImportElement importElement = node.getElement();
    if (importElement != null) {
      if (importElement.isDeferred()) {
        checkForLoadLibraryFunction(node, importElement);
      }
    }
    return super.visitImportDirective(node);
  }

  @Override
  public Void visitIndexExpression(IndexExpression node) {
    checkForDeprecatedMemberUse(node.getBestElement(), node);
    return super.visitIndexExpression(node);
  }

  @Override
  public Void visitInstanceCreationExpression(InstanceCreationExpression node) {
    checkForDeprecatedMemberUse(node.getStaticElement(), node);
    return super.visitInstanceCreationExpression(node);
  }

  @Override
  public Void visitIsExpression(IsExpression node) {
    checkAllTypeChecks(node);
    return super.visitIsExpression(node);
  }

  @Override
  public Void visitMethodDeclaration(MethodDeclaration node) {
    // This was determined to not be a good hint, see: dartbug.com/16029
    //checkForOverridingPrivateMember(node);
    checkForMissingReturn(node.getReturnType(), node.getBody());
    return super.visitMethodDeclaration(node);
  }

  @Override
  public Void visitPostfixExpression(PostfixExpression node) {
    checkForDeprecatedMemberUse(node.getBestElement(), node);
    return super.visitPostfixExpression(node);
  }

  @Override
  public Void visitPrefixExpression(PrefixExpression node) {
    checkForDeprecatedMemberUse(node.getBestElement(), node);
    return super.visitPrefixExpression(node);
  }

  @Override
  public Void visitRedirectingConstructorInvocation(RedirectingConstructorInvocation node) {
    checkForDeprecatedMemberUse(node.getStaticElement(), node);
    return super.visitRedirectingConstructorInvocation(node);
  }

  @Override
  public Void visitSimpleIdentifier(SimpleIdentifier node) {
    checkForDeprecatedMemberUseAtIdentifier(node);
    return super.visitSimpleIdentifier(node);
  }

  @Override
  public Void visitSuperConstructorInvocation(SuperConstructorInvocation node) {
    checkForDeprecatedMemberUse(node.getStaticElement(), node);
    return super.visitSuperConstructorInvocation(node);
  }

  @Override
  public Void visitVariableDeclaration(VariableDeclaration node) {
    checkForUseOfVoidResult(node.getInitializer());
    checkForInvalidAssignment(node.getName(), node.getInitializer());
    return super.visitVariableDeclaration(node);
  }

  /**
   * Check for the passed is expression for the unnecessary type check hint codes as well as null
   * checks expressed using an is expression.
   * 
   * @param node the is expression to check
   * @return {@code true} if and only if a hint code is generated on the passed node
   * @see HintCode#TYPE_CHECK_IS_NOT_NULL
   * @see HintCode#TYPE_CHECK_IS_NULL
   * @see HintCode#UNNECESSARY_TYPE_CHECK_TRUE
   * @see HintCode#UNNECESSARY_TYPE_CHECK_FALSE
   */
  private boolean checkAllTypeChecks(IsExpression node) {
    Expression expression = node.getExpression();
    TypeName typeName = node.getType();
    Type lhsType = expression.getStaticType();
    Type rhsType = typeName.getType();
    if (lhsType == null || rhsType == null) {
      return false;
    }
    String rhsNameStr = typeName.getName().getName();
    // if x is dynamic
    if ((rhsType.isDynamic() && rhsNameStr.equals(Keyword.DYNAMIC.getSyntax()))) {
      if (node.getNotOperator() == null) {
        // the is case
        errorReporter.reportErrorForNode(HintCode.UNNECESSARY_TYPE_CHECK_TRUE, node);
      } else {
        // the is not case
        errorReporter.reportErrorForNode(HintCode.UNNECESSARY_TYPE_CHECK_FALSE, node);
      }
      return true;
    }
    Element rhsElement = rhsType.getElement();
    LibraryElement libraryElement = rhsElement != null ? rhsElement.getLibrary() : null;
    if (libraryElement != null && libraryElement.isDartCore()) {
      // if x is Object or null is Null
      if (rhsType.isObject()
          || (expression instanceof NullLiteral && rhsNameStr.equals(NULL_TYPE_NAME))) {
        if (node.getNotOperator() == null) {
          // the is case
          errorReporter.reportErrorForNode(HintCode.UNNECESSARY_TYPE_CHECK_TRUE, node);
        } else {
          // the is not case
          errorReporter.reportErrorForNode(HintCode.UNNECESSARY_TYPE_CHECK_FALSE, node);
        }
        return true;
      } else if (rhsNameStr.equals(NULL_TYPE_NAME)) {
        if (node.getNotOperator() == null) {
          // the is case
          errorReporter.reportErrorForNode(HintCode.TYPE_CHECK_IS_NULL, node);
        } else {
          // the is not case
          errorReporter.reportErrorForNode(HintCode.TYPE_CHECK_IS_NOT_NULL, node);
        }
        return true;
      }
    }
    return false;
  }

  /**
   * This verifies that the passed expression can be assigned to its corresponding parameters.
   * <p>
   * This method corresponds to ErrorVerifier.checkForArgumentTypeNotAssignable.
   * <p>
   * TODO (jwren) In the ErrorVerifier there are other warnings that we could have a corresponding
   * hint for: see other callers of ErrorVerifier.checkForArgumentTypeNotAssignable(..).
   * 
   * @param expression the expression to evaluate
   * @param expectedStaticType the expected static type of the parameter
   * @param actualStaticType the actual static type of the argument
   * @param expectedPropagatedType the expected propagated type of the parameter, may be
   *          {@code null}
   * @param actualPropagatedType the expected propagated type of the parameter, may be {@code null}
   * @return {@code true} if and only if an hint code is generated on the passed node
   * @see HintCode#ARGUMENT_TYPE_NOT_ASSIGNABLE
   */
  private boolean checkForArgumentTypeNotAssignable(Expression expression, Type expectedStaticType,
      Type actualStaticType, Type expectedPropagatedType, Type actualPropagatedType,
      ErrorCode hintCode) {
    //
    // Warning case: test static type information
    //
    if (actualStaticType != null && expectedStaticType != null) {
      if (!actualStaticType.isAssignableTo(expectedStaticType)) {
        // A warning was created in the ErrorVerifier, return false, don't create a hint when a
        // warning has already been created.
        return false;
      }
    }
    //
    // Hint case: test propagated type information
    //
    // Compute the best types to use.
    Type expectedBestType = expectedPropagatedType != null ? expectedPropagatedType
        : expectedStaticType;
    Type actualBestType = actualPropagatedType != null ? actualPropagatedType : actualStaticType;

    if (actualBestType != null && expectedBestType != null) {
      if (!actualBestType.isAssignableTo(expectedBestType)) {
        errorReporter.reportErrorForNode(
            hintCode,
            expression,
            actualBestType.getDisplayName(),
            expectedBestType.getDisplayName());
        return true;
      }
    }
    return false;
  }

  /**
   * This verifies that the passed argument can be assigned to its corresponding parameter.
   * <p>
   * This method corresponds to ErrorCode.checkForArgumentTypeNotAssignableForArgument.
   * 
   * @param argument the argument to evaluate
   * @return {@code true} if and only if an hint code is generated on the passed node
   * @see HintCode#ARGUMENT_TYPE_NOT_ASSIGNABLE
   */
  private boolean checkForArgumentTypeNotAssignableForArgument(Expression argument) {
    if (argument == null) {
      return false;
    }

    ParameterElement staticParameterElement = argument.getStaticParameterElement();
    Type staticParameterType = staticParameterElement == null ? null
        : staticParameterElement.getType();

    ParameterElement propagatedParameterElement = argument.getPropagatedParameterElement();
    Type propagatedParameterType = propagatedParameterElement == null ? null
        : propagatedParameterElement.getType();

    return checkForArgumentTypeNotAssignableWithExpectedTypes(
        argument,
        staticParameterType,
        propagatedParameterType,
        HintCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
  }

  /**
   * This verifies that the passed expression can be assigned to its corresponding parameters.
   * <p>
   * This method corresponds to ErrorCode.checkForArgumentTypeNotAssignableWithExpectedTypes.
   * 
   * @param expression the expression to evaluate
   * @param expectedStaticType the expected static type
   * @param expectedPropagatedType the expected propagated type, may be {@code null}
   * @return {@code true} if and only if an hint code is generated on the passed node
   * @see HintCode#ARGUMENT_TYPE_NOT_ASSIGNABLE
   */
  private boolean checkForArgumentTypeNotAssignableWithExpectedTypes(Expression expression,
      Type expectedStaticType, Type expectedPropagatedType, ErrorCode errorCode) {
    return checkForArgumentTypeNotAssignable(
        expression,
        expectedStaticType,
        expression.getStaticType(),
        expectedPropagatedType,
        expression.getPropagatedType(),
        errorCode);
  }

  /**
   * This verifies that the passed arguments can be assigned to their corresponding parameters.
   * <p>
   * This method corresponds to ErrorCode.checkForArgumentTypesNotAssignableInList.
   * 
   * @param node the arguments to evaluate
   * @return {@code true} if and only if an hint code is generated on the passed node
   * @see HintCode#ARGUMENT_TYPE_NOT_ASSIGNABLE
   */
  private boolean checkForArgumentTypesNotAssignableInList(ArgumentList argumentList) {
    if (argumentList == null) {
      return false;
    }
    boolean problemReported = false;
    for (Expression argument : argumentList.getArguments()) {
      problemReported |= checkForArgumentTypeNotAssignableForArgument(argument);
    }
    return problemReported;
  }

  /**
   * Given some {@link Element}, look at the associated metadata and report the use of the member if
   * it is declared as deprecated.
   * 
   * @param element some element to check for deprecated use of
   * @param node the node use for the location of the error
   * @return {@code true} if and only if a hint code is generated on the passed node
   * @see HintCode#DEPRECATED_MEMBER_USE
   */
  private boolean checkForDeprecatedMemberUse(Element element, AstNode node) {
    if (element != null && element.isDeprecated()) {
      String displayName = element.getDisplayName();
      if (element instanceof ConstructorElement) {
        // TODO(jwren) We should modify ConstructorElement.getDisplayName(), or have the logic
        // centralized elsewhere, instead of doing this logic here.
        ConstructorElement constructorElement = (ConstructorElement) element;
        displayName = constructorElement.getEnclosingElement().getDisplayName();
        if (!constructorElement.getDisplayName().isEmpty()) {
          displayName = displayName + '.' + constructorElement.getDisplayName();
        }
      }
      errorReporter.reportErrorForNode(HintCode.DEPRECATED_MEMBER_USE, node, displayName);
      return true;
    }
    return false;
  }

  /**
   * For {@link SimpleIdentifier}s, only call {@link #checkForDeprecatedMemberUse(Element, AstNode)}
   * if the node is not in a declaration context.
   * <p>
   * Also, if the identifier is a constructor name in a constructor invocation, then calls to the
   * deprecated constructor will be caught by
   * {@link #visitInstanceCreationExpression(InstanceCreationExpression)} and
   * {@link #visitSuperConstructorInvocation(SuperConstructorInvocation)}, and can be ignored by
   * this visit method.
   * 
   * @param identifier some simple identifier to check for deprecated use of
   * @return {@code true} if and only if a hint code is generated on the passed node
   * @see HintCode#DEPRECATED_MEMBER_USE
   */
  private boolean checkForDeprecatedMemberUseAtIdentifier(SimpleIdentifier identifier) {
    if (identifier.inDeclarationContext()) {
      return false;
    }
    AstNode parent = identifier.getParent();
    if ((parent instanceof ConstructorName && identifier == ((ConstructorName) parent).getName())
        || (parent instanceof SuperConstructorInvocation && identifier == ((SuperConstructorInvocation) parent).getConstructorName())
        || parent instanceof HideCombinator) {
      return false;
    }
    return checkForDeprecatedMemberUse(identifier.getBestElement(), identifier);
  }

  /**
   * Check for the passed binary expression for the {@link HintCode#DIVISION_OPTIMIZATION}.
   * 
   * @param node the binary expression to check
   * @return {@code true} if and only if a hint code is generated on the passed node
   * @see HintCode#DIVISION_OPTIMIZATION
   */
  private boolean checkForDivisionOptimizationHint(BinaryExpression node) {
    // Return if the operator is not '/'
    if (!node.getOperator().getType().equals(TokenType.SLASH)) {
      return false;
    }
    // Return if the '/' operator is not defined in core, or if we don't know its static or propagated type
    MethodElement methodElement = node.getBestElement();
    if (methodElement == null) {
      return false;
    }
    LibraryElement libraryElement = methodElement.getLibrary();
    if (libraryElement != null && !libraryElement.isDartCore()) {
      return false;
    }
    // Report error if the (x/y) has toInt() invoked on it
    if (node.getParent() instanceof ParenthesizedExpression) {
      ParenthesizedExpression parenthesizedExpression = wrapParenthesizedExpression((ParenthesizedExpression) node.getParent());
      if (parenthesizedExpression.getParent() instanceof MethodInvocation) {
        MethodInvocation methodInvocation = (MethodInvocation) parenthesizedExpression.getParent();
        if (TO_INT_METHOD_NAME.equals(methodInvocation.getMethodName().getName())
            && methodInvocation.getArgumentList().getArguments().isEmpty()) {
          errorReporter.reportErrorForNode(HintCode.DIVISION_OPTIMIZATION, methodInvocation);
          return true;
        }
      }
    }
    return false;
  }

  /**
   * This verifies that the passed left hand side and right hand side represent a valid assignment.
   * <p>
   * This method corresponds to ErrorVerifier.checkForInvalidAssignment.
   * 
   * @param lhs the left hand side expression
   * @param rhs the right hand side expression
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see HintCode#INVALID_ASSIGNMENT
   */
  private boolean checkForInvalidAssignment(Expression lhs, Expression rhs) {
    if (lhs == null || rhs == null) {
      return false;
    }
    VariableElement leftVariableElement = ErrorVerifier.getVariableElement(lhs);
    Type leftType = (leftVariableElement == null) ? ErrorVerifier.getStaticType(lhs)
        : leftVariableElement.getType();
    Type staticRightType = ErrorVerifier.getStaticType(rhs);
    if (!staticRightType.isAssignableTo(leftType)) {
      // The warning was generated on this rhs
      return false;
    }
    // Test for, and then generate the hint
    Type bestRightType = rhs.getBestType();
    if (leftType != null && bestRightType != null) {
      if (!bestRightType.isAssignableTo(leftType)) {
        String leftName = leftType.getDisplayName();
        String rightName = bestRightType.getDisplayName();
        if (leftName.equals(rightName)) {
          Element leftElement = leftType.getElement();
          Element rightElement = bestRightType.getElement();
          if (leftElement != null && rightElement != null) {
            leftName = leftElement.getExtendedDisplayName();
            rightName = rightElement.getExtendedDisplayName();
          }
        }
        errorReporter.reportErrorForNode(HintCode.INVALID_ASSIGNMENT, rhs, rightName, leftName);
        return true;
      }
    }
    return false;
  }

  /**
   * Check that the imported library does not define a loadLibrary function. The import has already
   * been determined to be deferred when this is called.
   * 
   * @param node the import directive to evaluate
   * @param importElement the {@link ImportElement} retrieved from the node
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#IMPORT_DEFERRED_LIBRARY_WITH_LOAD_FUNCTION
   */
  private boolean checkForLoadLibraryFunction(ImportDirective node, ImportElement importElement) {
    LibraryElement importedLibrary = importElement.getImportedLibrary();
    if (importedLibrary == null) {
      return false;
    }
    if (importedLibrary.hasLoadLibraryFunction()) {
      errorReporter.reportErrorForNode(
          HintCode.IMPORT_DEFERRED_LIBRARY_WITH_LOAD_FUNCTION,
          node,
          importedLibrary.getName());
      return true;
    }
    return false;
  }

  /**
   * Generate a hint for functions or methods that have a return type, but do not have a return
   * statement on all branches. At the end of blocks with no return, Dart implicitly returns
   * {@code null}, avoiding these implicit returns is considered a best practice.
   * 
   * @param node the binary expression to check
   * @param body the function body
   * @return {@code true} if and only if a hint code is generated on the passed node
   * @see HintCode#MISSING_RETURN
   */
  private boolean checkForMissingReturn(TypeName returnType, FunctionBody body) {
    // Check that the method or function has a return type, and a function body
    if (returnType == null || body == null) {
      return false;
    }

    // Check that the body is a BlockFunctionBody
    if (!(body instanceof BlockFunctionBody)) {
      return false;
    }

    // Check that the type is resolvable, and is not "void"
    Type returnTypeType = returnType.getType();
    if (returnTypeType == null || returnTypeType.isVoid()) {
      return false;
    }

    // Check the block for a return statement, if not, create the hint
    BlockFunctionBody blockFunctionBody = (BlockFunctionBody) body;
    if (!blockFunctionBody.accept(new ExitDetector())) {
      errorReporter.reportErrorForNode(
          HintCode.MISSING_RETURN,
          returnType,
          returnTypeType.getDisplayName());
      return true;
    }
    return false;
  }

  /**
   * Check for the passed class declaration for the
   * {@link HintCode#OVERRIDE_EQUALS_BUT_NOT_HASH_CODE} hint code.
   * 
   * @param node the class declaration to check
   * @return {@code true} if and only if a hint code is generated on the passed node
   * @see HintCode#OVERRIDE_EQUALS_BUT_NOT_HASH_CODE
   */
  @SuppressWarnings("unused")
  private boolean checkForOverrideEqualsButNotHashCode(ClassDeclaration node) {
    ClassElement classElement = node.getElement();
    if (classElement == null) {
      return false;
    }
    MethodElement equalsOperatorMethodElement = classElement.getMethod(TokenType.EQ_EQ.getLexeme());
    if (equalsOperatorMethodElement != null) {
      PropertyAccessorElement hashCodeElement = classElement.getGetter(HASHCODE_GETTER_NAME);
      if (hashCodeElement == null) {
        errorReporter.reportErrorForNode(
            HintCode.OVERRIDE_EQUALS_BUT_NOT_HASH_CODE,
            node.getName(),
            classElement.getDisplayName());
        return true;
      }
    }
    return false;
  }

  /**
   * Check for the passed as expression for the {@link HintCode#UNNECESSARY_CAST} hint code.
   * 
   * @param node the as expression to check
   * @return {@code true} if and only if a hint code is generated on the passed node
   * @see HintCode#UNNECESSARY_CAST
   */
  private boolean checkForUnnecessaryCast(AsExpression node) {
    Expression expression = node.getExpression();
    TypeName typeName = node.getType();
    Type lhsType = expression.getStaticType();
    Type rhsType = typeName.getType();
    // TODO(jwren) After dartbug.com/13732, revisit this, we should be able to remove the
    // !(x instanceof TypeParameterType) checks.
    if (lhsType != null && rhsType != null && !lhsType.isDynamic() && !rhsType.isDynamic()
        && !(lhsType instanceof TypeParameterType) && !(rhsType instanceof TypeParameterType)
        && lhsType.isSubtypeOf(rhsType)) {
      errorReporter.reportErrorForNode(HintCode.UNNECESSARY_CAST, node);
      return true;
    }
    return false;
  }

  /**
   * Check for situations where the result of a method or function is used, when it returns 'void'.
   * <p>
   * TODO(jwren) Many other situations of use could be covered. We currently cover the cases var x =
   * m() and x = m(), but we could also cover cases such as m().x, m()[k], a + m(), f(m()), return
   * m().
   * 
   * @param node expression on the RHS of some assignment
   * @return {@code true} if and only if a hint code is generated on the passed node
   * @see HintCode#USE_OF_VOID_RESULT
   */
  private boolean checkForUseOfVoidResult(Expression expression) {
    if (expression == null || !(expression instanceof MethodInvocation)) {
      return false;
    }
    MethodInvocation methodInvocation = (MethodInvocation) expression;
    if (methodInvocation.getStaticType() == VoidTypeImpl.getInstance()) {
      SimpleIdentifier methodName = methodInvocation.getMethodName();
      errorReporter.reportErrorForNode(
          HintCode.USE_OF_VOID_RESULT,
          methodName,
          methodName.getName());
      return true;
    }
    return false;
  }

}
