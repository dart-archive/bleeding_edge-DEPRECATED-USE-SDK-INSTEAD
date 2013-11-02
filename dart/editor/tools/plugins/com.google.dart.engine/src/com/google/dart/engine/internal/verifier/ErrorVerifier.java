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
import com.google.dart.engine.ast.Annotation;
import com.google.dart.engine.ast.ArgumentDefinitionTest;
import com.google.dart.engine.ast.ArgumentList;
import com.google.dart.engine.ast.AssertStatement;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.BlockFunctionBody;
import com.google.dart.engine.ast.BreakStatement;
import com.google.dart.engine.ast.CatchClause;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassMember;
import com.google.dart.engine.ast.ClassTypeAlias;
import com.google.dart.engine.ast.Comment;
import com.google.dart.engine.ast.CommentReference;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConditionalExpression;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.ConstructorFieldInitializer;
import com.google.dart.engine.ast.ConstructorInitializer;
import com.google.dart.engine.ast.ConstructorName;
import com.google.dart.engine.ast.ContinueStatement;
import com.google.dart.engine.ast.Declaration;
import com.google.dart.engine.ast.DefaultFormalParameter;
import com.google.dart.engine.ast.DoStatement;
import com.google.dart.engine.ast.ExportDirective;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ExpressionFunctionBody;
import com.google.dart.engine.ast.ExpressionStatement;
import com.google.dart.engine.ast.ExtendsClause;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.FieldFormalParameter;
import com.google.dart.engine.ast.FormalParameter;
import com.google.dart.engine.ast.FormalParameterList;
import com.google.dart.engine.ast.FunctionBody;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.FunctionExpressionInvocation;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.FunctionTypedFormalParameter;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.IfStatement;
import com.google.dart.engine.ast.ImplementsClause;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.IndexExpression;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.ListLiteral;
import com.google.dart.engine.ast.MapLiteral;
import com.google.dart.engine.ast.MapLiteralEntry;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NativeClause;
import com.google.dart.engine.ast.NativeFunctionBody;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.NormalFormalParameter;
import com.google.dart.engine.ast.PostfixExpression;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.RedirectingConstructorInvocation;
import com.google.dart.engine.ast.RethrowExpression;
import com.google.dart.engine.ast.ReturnStatement;
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.SuperConstructorInvocation;
import com.google.dart.engine.ast.SwitchCase;
import com.google.dart.engine.ast.SwitchMember;
import com.google.dart.engine.ast.SwitchStatement;
import com.google.dart.engine.ast.ThisExpression;
import com.google.dart.engine.ast.ThrowExpression;
import com.google.dart.engine.ast.TopLevelVariableDeclaration;
import com.google.dart.engine.ast.TypeArgumentList;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.TypeParameter;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.VariableDeclarationStatement;
import com.google.dart.engine.ast.WhileStatement;
import com.google.dart.engine.ast.WithClause;
import com.google.dart.engine.ast.visitor.RecursiveASTVisitor;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.ExportElement;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.TypeParameterElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.element.visitor.GeneralizingElementVisitor;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorWithProperties;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.ErrorProperty;
import com.google.dart.engine.error.StaticTypeWarningCode;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.internal.constant.EvaluationResultImpl;
import com.google.dart.engine.internal.constant.ValidResult;
import com.google.dart.engine.internal.element.FieldFormalParameterElementImpl;
import com.google.dart.engine.internal.element.ParameterElementImpl;
import com.google.dart.engine.internal.element.member.ConstructorMember;
import com.google.dart.engine.internal.error.ErrorReporter;
import com.google.dart.engine.internal.resolver.ElementResolver;
import com.google.dart.engine.internal.resolver.InheritanceManager;
import com.google.dart.engine.internal.resolver.MemberMap;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.internal.scope.Namespace;
import com.google.dart.engine.internal.scope.NamespaceBuilder;
import com.google.dart.engine.internal.type.DynamicTypeImpl;
import com.google.dart.engine.internal.type.VoidTypeImpl;
import com.google.dart.engine.parser.ParserErrorCode;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.sdk.SdkLibrary;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.type.TypeParameterType;
import com.google.dart.engine.utilities.dart.ParameterKind;
import com.google.dart.engine.utilities.general.ObjectUtilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
    INIT_IN_INITIALIZERS
  }

  /**
   * Checks if the given expression is the reference to the type.
   * 
   * @param expr the expression to evaluate
   * @return {@code true} if the given expression is the reference to the type
   */
  private static boolean isTypeReference(Expression expr) {
    if (expr instanceof Identifier) {
      Identifier identifier = (Identifier) expr;
      return identifier.getStaticElement() instanceof ClassElement;
    }
    return false;
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
   * The type representing the type 'bool'.
   */
  private InterfaceType boolType;

  /**
   * The object providing access to the types defined by the language.
   */
  private TypeProvider typeProvider;

  /**
   * The manager for the inheritance mappings.
   */
  private InheritanceManager inheritanceManager;

  /**
   * A flag indicating whether we are running in strict mode. In strict mode, error reporting is
   * based exclusively on the static type information.
   */
  private boolean strictMode;

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
   * This is set to {@code true} iff the visitor is currently visiting children nodes of an
   * {@link Comment}.
   */
  private boolean isInComment;

  /**
   * This is set to {@code true} iff the visitor is currently visiting children nodes of an
   * {@link InstanceCreationExpression}.
   */
  private boolean isInConstInstanceCreation;

  /**
   * This is set to {@code true} iff the visitor is currently visiting children nodes of a native
   * {@link ClassDeclaration}.
   */
  private boolean isInNativeClass;

  /**
   * This is set to {@code true} iff the visitor is currently visiting a static variable
   * declaration.
   */
  private boolean isInStaticVariableDeclaration;

  /**
   * This is set to {@code true} iff the visitor is currently visiting an instance variable
   * declaration.
   */
  private boolean isInInstanceVariableDeclaration;

  /**
   * This is set to {@code true} iff the visitor is currently visiting an instance variable
   * initializer.
   */
  private boolean isInInstanceVariableInitializer;

  /**
   * This is set to {@code true} iff the visitor is currently visiting a
   * {@link ConstructorInitializer}.
   */
  private boolean isInConstructorInitializer;

  /**
   * This is set to {@code true} iff the visitor is currently visiting a
   * {@link FunctionTypedFormalParameter}.
   */
  private boolean isInFunctionTypedFormalParameter;

  /**
   * This is set to {@code true} iff the visitor is currently visiting a static method. By "method"
   * here getter, setter and operator declarations are also implied since they are all represented
   * with a {@link MethodDeclaration} in the AST structure.
   */
  private boolean isInStaticMethod;

  /**
   * This is set to {@code true} iff the visitor is currently visiting code in the SDK.
   */
  private boolean isInSystemLibrary;

  /**
   * The class containing the AST nodes being visited, or {@code null} if we are not in the scope of
   * a class.
   */
  private ClassElement enclosingClass;

  /**
   * The method or function that we are currently visiting, or {@code null} if we are not inside a
   * method or function.
   */
  private ExecutableElement enclosingFunction;

  /**
   * The number of return statements found in the method or function that we are currently visiting
   * that have a return value.
   */
  private int returnWithCount = 0;

  /**
   * The number of return statements found in the method or function that we are currently visiting
   * that do not have a return value.
   */
  private int returnWithoutCount = 0;

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
   * A table mapping name of the library to the export directive which export this library.
   */
  private HashMap<String, LibraryElement> nameToExportElement = new HashMap<String, LibraryElement>();

  /**
   * A table mapping name of the library to the import directive which import this library.
   */
  private HashMap<String, LibraryElement> nameToImportElement = new HashMap<String, LibraryElement>();

  /**
   * A table mapping names to the export elements exported them.
   */
  private HashMap<String, ExportElement> exportedNames = new HashMap<String, ExportElement>();

  /**
   * A set of the names of the variable initializers we are visiting now.
   */
  private HashSet<String> namesForReferenceToDeclaredVariableInInitializer = new HashSet<String>();

  /**
   * A list of types used by the {@link CompileTimeErrorCode#EXTENDS_DISALLOWED_CLASS} and
   * {@link CompileTimeErrorCode#IMPLEMENTS_DISALLOWED_CLASS} error codes.
   */
  private final InterfaceType[] DISALLOWED_TYPES_TO_EXTEND_OR_IMPLEMENT;

  public ErrorVerifier(ErrorReporter errorReporter, LibraryElement currentLibrary,
      TypeProvider typeProvider, InheritanceManager inheritanceManager) {
    this.errorReporter = errorReporter;
    this.currentLibrary = currentLibrary;
    this.isInSystemLibrary = currentLibrary.getSource().isInSystemLibrary();
    this.typeProvider = typeProvider;
    this.inheritanceManager = inheritanceManager;
    strictMode = currentLibrary.getContext().getAnalysisOptions().getStrictMode();
    isEnclosingConstructorConst = false;
    isInCatchClause = false;
    isInStaticVariableDeclaration = false;
    isInInstanceVariableDeclaration = false;
    isInInstanceVariableInitializer = false;
    isInConstructorInitializer = false;
    isInStaticMethod = false;
    boolType = typeProvider.getBoolType();
    dynamicType = typeProvider.getDynamicType();
    DISALLOWED_TYPES_TO_EXTEND_OR_IMPLEMENT = new InterfaceType[] {
        typeProvider.getNullType(), typeProvider.getNumType(), typeProvider.getIntType(),
        typeProvider.getDoubleType(), boolType, typeProvider.getStringType()};
  }

  @Override
  public Void visitArgumentDefinitionTest(ArgumentDefinitionTest node) {
    checkForArgumentDefinitionTestNonParameter(node);
    return super.visitArgumentDefinitionTest(node);
  }

  @Override
  public Void visitArgumentList(ArgumentList node) {
    checkForArgumentTypeNotAssignable(node);
    return super.visitArgumentList(node);
  }

  @Override
  public Void visitAssertStatement(AssertStatement node) {
    checkForNonBoolExpression(node);
    return super.visitAssertStatement(node);
  }

  @Override
  public Void visitAssignmentExpression(AssignmentExpression node) {
    Token operator = node.getOperator();
    TokenType operatorType = operator.getType();
    if (operatorType == TokenType.EQ) {
      checkForInvalidAssignment(node.getLeftHandSide(), node.getRightHandSide());
    } else {
      checkForInvalidAssignment(node);
    }
    checkForAssignmentToFinal(node);
    checkForArgumentTypeNotAssignable(node.getRightHandSide());
    return super.visitAssignmentExpression(node);
  }

  @Override
  public Void visitBinaryExpression(BinaryExpression node) {
    checkForArgumentTypeNotAssignable(node.getRightOperand());
    return super.visitBinaryExpression(node);
  }

  @Override
  public Void visitBlockFunctionBody(BlockFunctionBody node) {
    int previousReturnWithCount = returnWithCount;
    int previousReturnWithoutCount = returnWithoutCount;
    try {
      returnWithCount = 0;
      returnWithoutCount = 0;
      super.visitBlockFunctionBody(node);
      checkForMixedReturns(node);
    } finally {
      returnWithCount = previousReturnWithCount;
      returnWithoutCount = previousReturnWithoutCount;
    }
    return null;
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
    ClassElement outerClass = enclosingClass;
    try {
      isInNativeClass = node.getNativeClause() != null;
      enclosingClass = node.getElement();
      WithClause withClause = node.getWithClause();
      ImplementsClause implementsClause = node.getImplementsClause();
      ExtendsClause extendsClause = node.getExtendsClause();
      checkForBuiltInIdentifierAsName(
          node.getName(),
          CompileTimeErrorCode.BUILT_IN_IDENTIFIER_AS_TYPE_NAME);
      checkForMemberWithClassName();
      checkForNoDefaultSuperConstructorImplicit(node);
      checkForAllMixinErrorCodes(withClause);
      checkForConflictingTypeVariableErrorCodes(node);
      if (implementsClause != null || extendsClause != null) {
        if (!checkForImplementsDisallowedClass(implementsClause)
            && !checkForExtendsDisallowedClass(extendsClause)) {
          checkForNonAbstractClassInheritsAbstractMember(node);
          checkForInconsistentMethodInheritance();
          checkForRecursiveInterfaceInheritance(enclosingClass);
        }
      }
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
      checkForDuplicateDefinitionInheritance();
      checkForConflictingGetterAndMethod();
      checkImplementsSuperClass(node);
      checkImplementsFunctionWithoutCall(node);
      return super.visitClassDeclaration(node);
    } finally {
      isInNativeClass = false;
      initialFieldElementsMap = null;
      enclosingClass = outerClass;
    }
  }

  @Override
  public Void visitClassTypeAlias(ClassTypeAlias node) {
    checkForBuiltInIdentifierAsName(
        node.getName(),
        CompileTimeErrorCode.BUILT_IN_IDENTIFIER_AS_TYPEDEF_NAME);
    checkForAllMixinErrorCodes(node.getWithClause());
    ClassElement outerClassElement = enclosingClass;
    try {
      enclosingClass = node.getElement();
      checkForRecursiveInterfaceInheritance(node.getElement());
      checkForTypeAliasCannotReferenceItself_mixin(node);
    } finally {
      enclosingClass = outerClassElement;
    }
    return super.visitClassTypeAlias(node);
  }

  @Override
  public Void visitComment(Comment node) {
    isInComment = true;
    try {
      return super.visitComment(node);
    } finally {
      isInComment = false;
    }
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
      checkForConstConstructorWithNonConstSuper(node);
      checkForConflictingConstructorNameAndMember(node);
      checkForAllFinalInitializedErrorCodes(node);
      checkForRedirectingConstructorErrorCodes(node);
      checkForMultipleSuperInitializers(node);
      checkForRecursiveConstructorRedirect(node);
      if (!checkForRecursiveFactoryRedirect(node)) {
        checkForAllRedirectConstructorErrorCodes(node);
      }
      checkForUndefinedConstructorInInitializerImplicit(node);
      checkForRedirectToNonConstConstructor(node);
      checkForReturnInGenerativeConstructor(node);
      return super.visitConstructorDeclaration(node);
    } finally {
      isEnclosingConstructorConst = false;
      enclosingFunction = outerFunction;
    }
  }

  @Override
  public Void visitConstructorFieldInitializer(ConstructorFieldInitializer node) {
    isInConstructorInitializer = true;
    try {
      checkForFieldInitializerNotAssignable(node);
      return super.visitConstructorFieldInitializer(node);
    } finally {
      isInConstructorInitializer = false;
    }
  }

  @Override
  public Void visitDefaultFormalParameter(DefaultFormalParameter node) {
    checkForInvalidAssignment(node.getIdentifier(), node.getDefaultValue());
    checkForDefaultValueInFunctionTypedParameter(node);
    return super.visitDefaultFormalParameter(node);
  }

  @Override
  public Void visitDoStatement(DoStatement node) {
    checkForNonBoolCondition(node.getCondition());
    return super.visitDoStatement(node);
  }

  @Override
  public Void visitExportDirective(ExportDirective node) {
    checkForAmbiguousExport(node);
    checkForExportDuplicateLibraryName(node);
    checkForExportInternalLibrary(node);
    return super.visitExportDirective(node);
  }

  @Override
  public Void visitExpressionFunctionBody(ExpressionFunctionBody node) {
    FunctionType functionType = enclosingFunction == null ? null : enclosingFunction.getType();
    Type expectedReturnType = functionType == null ? DynamicTypeImpl.getInstance()
        : functionType.getReturnType();
    checkForReturnOfInvalidType(node.getExpression(), expectedReturnType);
    return super.visitExpressionFunctionBody(node);
  }

  @Override
  public Void visitFieldDeclaration(FieldDeclaration node) {
    if (!node.isStatic()) {
      VariableDeclarationList variables = node.getFields();
      if (variables.isConst()) {
        errorReporter.reportError(CompileTimeErrorCode.CONST_INSTANCE_FIELD, variables.getKeyword());
      }
    }
    isInStaticVariableDeclaration = node.isStatic();
    isInInstanceVariableDeclaration = !isInStaticVariableDeclaration;
    try {
      checkForAllInvalidOverrideErrorCodes(node);
      return super.visitFieldDeclaration(node);
    } finally {
      isInStaticVariableDeclaration = false;
      isInInstanceVariableDeclaration = false;
    }
  }

  @Override
  public Void visitFieldFormalParameter(FieldFormalParameter node) {
    checkForConstFormalParameter(node);
    checkForPrivateOptionalParameter(node);
    checkForFieldInitializingFormalRedirectingConstructor(node);
    return super.visitFieldFormalParameter(node);
  }

  @Override
  public Void visitFunctionDeclaration(FunctionDeclaration node) {
    ExecutableElement outerFunction = enclosingFunction;
    try {
      SimpleIdentifier identifier = node.getName();
      String methodName = "";
      if (identifier != null) {
        methodName = identifier.getName();
      }

      enclosingFunction = node.getElement();
      if (node.isSetter() || node.isGetter()) {
        checkForMismatchedAccessorTypes(node, methodName);
        if (node.isSetter()) {
          FunctionExpression functionExpression = node.getFunctionExpression();
          if (functionExpression != null) {
            checkForWrongNumberOfParametersForSetter(
                node.getName(),
                functionExpression.getParameters());
          }
          TypeName returnType = node.getReturnType();
          checkForNonVoidReturnTypeForSetter(returnType);
        }
      }

      return super.visitFunctionDeclaration(node);
    } finally {
      enclosingFunction = outerFunction;
    }
  }

  @Override
  public Void visitFunctionExpression(FunctionExpression node) {
    // If this function expression is wrapped in a function declaration, don't change the
    // enclosingFunction field.
    if (!(node.getParent() instanceof FunctionDeclaration)) {
      ExecutableElement outerFunction = enclosingFunction;
      try {
        enclosingFunction = node.getElement();
        return super.visitFunctionExpression(node);
      } finally {
        enclosingFunction = outerFunction;
      }
    } else {
      return super.visitFunctionExpression(node);
    }
  }

  @Override
  public Void visitFunctionExpressionInvocation(FunctionExpressionInvocation node) {
    Expression functionExpression = node.getFunction();
    Type expressionType = functionExpression.getStaticType();
    if (!isFunctionType(expressionType)) {
      errorReporter.reportError(
          StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION_EXPRESSION,
          functionExpression);
    }
    return super.visitFunctionExpressionInvocation(node);
  }

  @Override
  public Void visitFunctionTypeAlias(FunctionTypeAlias node) {
    checkForBuiltInIdentifierAsName(
        node.getName(),
        CompileTimeErrorCode.BUILT_IN_IDENTIFIER_AS_TYPEDEF_NAME);
    checkForDefaultValueInFunctionTypeAlias(node);
    checkForTypeAliasCannotReferenceItself_function(node);
    return super.visitFunctionTypeAlias(node);
  }

  @Override
  public Void visitFunctionTypedFormalParameter(FunctionTypedFormalParameter node) {
    boolean old = isInFunctionTypedFormalParameter;
    isInFunctionTypedFormalParameter = true;
    try {
      return super.visitFunctionTypedFormalParameter(node);
    } finally {
      isInFunctionTypedFormalParameter = old;
    }
  }

  @Override
  public Void visitIfStatement(IfStatement node) {
    checkForNonBoolCondition(node.getCondition());
    return super.visitIfStatement(node);
  }

  @Override
  public Void visitImportDirective(ImportDirective node) {
    checkForImportDuplicateLibraryName(node);
    checkForImportInternalLibrary(node);
    return super.visitImportDirective(node);
  }

  @Override
  public Void visitIndexExpression(IndexExpression node) {
    checkForArgumentTypeNotAssignable(node.getIndex());
    return super.visitIndexExpression(node);
  }

  @Override
  public Void visitInstanceCreationExpression(InstanceCreationExpression node) {
    isInConstInstanceCreation = node.isConst();
    try {
      ConstructorName constructorName = node.getConstructorName();
      TypeName typeName = constructorName.getType();
      Type type = typeName.getType();
      if (type instanceof InterfaceType) {
        InterfaceType interfaceType = (InterfaceType) type;
        checkForConstOrNewWithAbstractClass(node, typeName, interfaceType);
        if (isInConstInstanceCreation) {
          checkForConstWithNonConst(node);
          checkForConstWithUndefinedConstructor(node);
          checkForConstWithTypeParameters(node);
        } else {
          checkForNewWithUndefinedConstructor(node);
        }
      }
      return super.visitInstanceCreationExpression(node);
    } finally {
      isInConstInstanceCreation = false;
    }
  }

  @Override
  public Void visitListLiteral(ListLiteral node) {
    if (node.getConstKeyword() != null) {
      TypeArgumentList typeArguments = node.getTypeArguments();
      if (typeArguments != null) {
        NodeList<TypeName> arguments = typeArguments.getArguments();
        if (arguments.size() != 0) {
          checkForInvalidTypeArgumentInConstTypedLiteral(
              arguments,
              CompileTimeErrorCode.INVALID_TYPE_ARGUMENT_IN_CONST_LIST);
        }
      }
    }
    checkForExpectedOneListTypeArgument(node);
    checkForListElementTypeNotAssignable(node);
    return super.visitListLiteral(node);
  }

  @Override
  public Void visitMapLiteral(MapLiteral node) {
    TypeArgumentList typeArguments = node.getTypeArguments();
    if (typeArguments != null) {
      NodeList<TypeName> arguments = typeArguments.getArguments();
      if (arguments.size() != 0) {
        if (node.getConstKeyword() != null) {
          checkForInvalidTypeArgumentInConstTypedLiteral(
              arguments,
              CompileTimeErrorCode.INVALID_TYPE_ARGUMENT_IN_CONST_MAP);
        }
      }
    }
    checkExpectedTwoMapTypeArguments(typeArguments);
    checkForNonConstMapAsExpressionStatement(node);
    checkForMapTypeNotAssignable(node);
    checkForConstMapKeyExpressionTypeImplementsEquals(node);
    return super.visitMapLiteral(node);
  }

  @Override
  public Void visitMethodDeclaration(MethodDeclaration node) {
    ExecutableElement previousFunction = enclosingFunction;
    try {
      isInStaticMethod = node.isStatic();
      enclosingFunction = node.getElement();
      SimpleIdentifier identifier = node.getName();
      String methodName = "";
      if (identifier != null) {
        methodName = identifier.getName();
      }
      if (node.isSetter() || node.isGetter()) {
        checkForMismatchedAccessorTypes(node, methodName);
        checkForConflictingInstanceGetterAndSuperclassMember(node);
      }
      if (node.isGetter()) {
        checkForConflictingStaticGetterAndInstanceSetter(node);
      } else if (node.isSetter()) {
        checkForWrongNumberOfParametersForSetter(node.getName(), node.getParameters());
        checkForNonVoidReturnTypeForSetter(node.getReturnType());
        checkForConflictingStaticSetterAndInstanceMember(node);
      } else if (node.isOperator()) {
        checkForOptionalParameterInOperator(node);
        checkForWrongNumberOfParametersForOperator(node);
        checkForNonVoidReturnTypeForOperator(node);
      } else {
        checkForConflictingInstanceMethodSetter(node);
      }
      checkForConcreteClassWithAbstractMember(node);
      checkForAllInvalidOverrideErrorCodes(node);
      return super.visitMethodDeclaration(node);
    } finally {
      enclosingFunction = previousFunction;
      isInStaticMethod = false;
    }
  }

  @Override
  public Void visitMethodInvocation(MethodInvocation node) {
    Expression target = node.getRealTarget();
    SimpleIdentifier methodName = node.getMethodName();
    checkForStaticAccessToInstanceMember(target, methodName);
    checkForInstanceAccessToStaticMember(target, methodName);
    if (target == null) {
      checkForUnqualifiedReferenceToNonLocalStaticMember(methodName);
    }
    return super.visitMethodInvocation(node);
  }

  @Override
  public Void visitNativeClause(NativeClause node) {
    // TODO(brianwilkerson) Figure out the right rule for when 'native' is allowed.
    if (!isInSystemLibrary) {
      errorReporter.reportError(ParserErrorCode.NATIVE_CLAUSE_IN_NON_SDK_CODE, node);
    }
    return super.visitNativeClause(node);
  }

  @Override
  public Void visitNativeFunctionBody(NativeFunctionBody node) {
    checkForNativeFunctionBodyInNonSDKCode(node);
    return super.visitNativeFunctionBody(node);
  }

  @Override
  public Void visitPostfixExpression(PostfixExpression node) {
    checkForAssignmentToFinal(node.getOperand());
    checkForIntNotAssignable(node.getOperand());
    return super.visitPostfixExpression(node);
  }

  @Override
  public Void visitPrefixedIdentifier(PrefixedIdentifier node) {
    if (!(node.getParent() instanceof Annotation)) {
      checkForStaticAccessToInstanceMember(node.getPrefix(), node.getIdentifier());
      checkForInstanceAccessToStaticMember(node.getPrefix(), node.getIdentifier());
    }
    return super.visitPrefixedIdentifier(node);
  }

  @Override
  public Void visitPrefixExpression(PrefixExpression node) {
    TokenType operatorType = node.getOperator().getType();
    Expression operand = node.getOperand();
    if (operatorType == TokenType.BANG) {
      checkForNonBoolNegationExpression(operand);
    } else if (operatorType.isIncrementOperator()) {
      checkForAssignmentToFinal(operand);
    }
    checkForIntNotAssignable(operand);
    return super.visitPrefixExpression(node);
  }

  @Override
  public Void visitPropertyAccess(PropertyAccess node) {
    Expression target = node.getRealTarget();
    SimpleIdentifier propertyName = node.getPropertyName();
    checkForStaticAccessToInstanceMember(target, propertyName);
    checkForInstanceAccessToStaticMember(target, propertyName);
    return super.visitPropertyAccess(node);
  }

  @Override
  public Void visitRedirectingConstructorInvocation(RedirectingConstructorInvocation node) {
    isInConstructorInitializer = true;
    try {
      return super.visitRedirectingConstructorInvocation(node);
    } finally {
      isInConstructorInitializer = false;
    }
  }

  @Override
  public Void visitRethrowExpression(RethrowExpression node) {
    checkForRethrowOutsideCatch(node);
    return super.visitRethrowExpression(node);
  }

  @Override
  public Void visitReturnStatement(ReturnStatement node) {
    if (node.getExpression() == null) {
      returnWithoutCount++;
    } else {
      returnWithCount++;
    }
    checkForAllReturnStatementErrorCodes(node);
    return super.visitReturnStatement(node);
  }

  @Override
  public Void visitSimpleFormalParameter(SimpleFormalParameter node) {
    checkForConstFormalParameter(node);
    checkForPrivateOptionalParameter(node);
    return super.visitSimpleFormalParameter(node);
  }

  @Override
  public Void visitSimpleIdentifier(SimpleIdentifier node) {
    checkForImplicitThisReferenceInInitializer(node);
    if (!isUnqualifiedReferenceToNonLocalStaticMemberAllowed(node)) {
      checkForUnqualifiedReferenceToNonLocalStaticMember(node);
    }
    return super.visitSimpleIdentifier(node);
  }

  @Override
  public Void visitSuperConstructorInvocation(SuperConstructorInvocation node) {
    isInConstructorInitializer = true;
    try {
      return super.visitSuperConstructorInvocation(node);
    } finally {
      isInConstructorInitializer = false;
    }
  }

  @Override
  public Void visitSwitchStatement(SwitchStatement node) {
    checkForInconsistentCaseExpressionTypes(node);
    checkForSwitchExpressionNotAssignable(node);
    checkForCaseBlocksNotTerminated(node);
    return super.visitSwitchStatement(node);
  }

  @Override
  public Void visitThisExpression(ThisExpression node) {
    checkForInvalidReferenceToThis(node);
    return super.visitThisExpression(node);
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
  public Void visitTypeName(TypeName node) {
    checkForTypeArgumentNotMatchingBounds(node);
    checkForTypeParameterReferencedByStatic(node);
    return super.visitTypeName(node);
  }

  @Override
  public Void visitTypeParameter(TypeParameter node) {
    checkForBuiltInIdentifierAsName(
        node.getName(),
        CompileTimeErrorCode.BUILT_IN_IDENTIFIER_AS_TYPE_PARAMETER_NAME);
    checkForTypeParameterSupertypeOfItsBound(node);
    return super.visitTypeParameter(node);
  }

  @Override
  public Void visitVariableDeclaration(VariableDeclaration node) {
    SimpleIdentifier nameNode = node.getName();
    Expression initializerNode = node.getInitializer();
    // do checks
    checkForInvalidAssignment(nameNode, initializerNode);
    // visit name
    nameNode.accept(this);
    // visit initializer
    String name = nameNode.getName();
    namesForReferenceToDeclaredVariableInInitializer.add(name);
    isInInstanceVariableInitializer = isInInstanceVariableDeclaration;
    try {
      if (initializerNode != null) {
        initializerNode.accept(this);
      }
    } finally {
      isInInstanceVariableInitializer = false;
      namesForReferenceToDeclaredVariableInInitializer.remove(name);
    }
    // done
    return null;
  }

  @Override
  public Void visitVariableDeclarationList(VariableDeclarationList node) {
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
   * This verifies if the passed map literal has type arguments then there is exactly two.
   * 
   * @param node the map literal to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticTypeWarningCode#EXPECTED_TWO_MAP_TYPE_ARGUMENTS
   */
  private boolean checkExpectedTwoMapTypeArguments(TypeArgumentList typeArguments) {
    // has type arguments
    if (typeArguments == null) {
      return false;
    }
    // check number of type arguments
    int num = typeArguments.getArguments().size();
    if (num == 2) {
      return false;
    }
    // report problem
    errorReporter.reportError(
        StaticTypeWarningCode.EXPECTED_TWO_MAP_TYPE_ARGUMENTS,
        typeArguments,
        num);
    return true;
  }

  /**
   * This verifies that the passed constructor declaration does not violate any of the error codes
   * relating to the initialization of fields in the enclosing class.
   * 
   * @param node the {@link ConstructorDeclaration} to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see #initialFieldElementsMap
   * @see CompileTimeErrorCode#FINAL_INITIALIZED_IN_DECLARATION_AND_CONSTRUCTOR
   * @see CompileTimeErrorCode#FINAL_INITIALIZED_MULTIPLE_TIMES
   */
  private boolean checkForAllFinalInitializedErrorCodes(ConstructorDeclaration node) {
    if (node.getFactoryKeyword() != null || node.getRedirectedConstructor() != null
        || node.getExternalKeyword() != null) {
      return false;
    }

    // Ignore if native class.
    if (isInNativeClass) {
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
                StaticWarningCode.FINAL_INITIALIZED_IN_DECLARATION_AND_CONSTRUCTOR,
                formalParameter.getIdentifier(),
                fieldElement.getDisplayName());
            foundError = true;
          }
        } else if (state == INIT_STATE.INIT_IN_FIELD_FORMAL) {
          if (fieldElement.isFinal() || fieldElement.isConst()) {
            errorReporter.reportError(
                CompileTimeErrorCode.FINAL_INITIALIZED_MULTIPLE_TIMES,
                formalParameter.getIdentifier(),
                fieldElement.getDisplayName());
            foundError = true;
          }
        }
      }
    }

    // Visit all of the initializers
    NodeList<ConstructorInitializer> initializers = node.getInitializers();
    for (ConstructorInitializer constructorInitializer : initializers) {
      if (constructorInitializer instanceof RedirectingConstructorInvocation) {
        return false;
      }
      if (constructorInitializer instanceof ConstructorFieldInitializer) {
        ConstructorFieldInitializer constructorFieldInitializer = (ConstructorFieldInitializer) constructorInitializer;
        SimpleIdentifier fieldName = constructorFieldInitializer.getFieldName();
        Element element = fieldName.getStaticElement();
        if (element instanceof FieldElement) {
          FieldElement fieldElement = (FieldElement) element;
          INIT_STATE state = fieldElementsMap.get(fieldElement);
          if (state == INIT_STATE.NOT_INIT) {
            fieldElementsMap.put(fieldElement, INIT_STATE.INIT_IN_INITIALIZERS);
          } else if (state == INIT_STATE.INIT_IN_DECLARATION) {
            if (fieldElement.isFinal() || fieldElement.isConst()) {
              errorReporter.reportError(
                  StaticWarningCode.FIELD_INITIALIZED_IN_INITIALIZER_AND_DECLARATION,
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
                fieldElement.getDisplayName());
            foundError = true;
          }
        }
      }
    }

    // Visit all of the states in the map to ensure that none were never initialized.
    for (Entry<FieldElement, INIT_STATE> entry : fieldElementsMap.entrySet()) {
      if (entry.getValue() == INIT_STATE.NOT_INIT) {
        FieldElement fieldElement = entry.getKey();
        if (fieldElement.isConst()) {
          errorReporter.reportError(
              CompileTimeErrorCode.CONST_NOT_INITIALIZED,
              node.getReturnType(),
              fieldElement.getName());
          foundError = true;
        } else if (fieldElement.isFinal()) {
          errorReporter.reportError(
              StaticWarningCode.FINAL_NOT_INITIALIZED,
              node.getReturnType(),
              fieldElement.getName());
          foundError = true;
        }
      }
    }
    return foundError;
  }

  /**
   * This checks the passed executable element against override-error codes.
   * 
   * @param executableElement a non-null {@link ExecutableElement} to evaluate
   * @param parameters the parameters of the executable element
   * @param errorNameTarget the node to report problems on
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticWarningCode#INSTANCE_METHOD_NAME_COLLIDES_WITH_SUPERCLASS_STATIC
   * @see CompileTimeErrorCode#INVALID_OVERRIDE_REQUIRED
   * @see CompileTimeErrorCode#INVALID_OVERRIDE_POSITIONAL
   * @see CompileTimeErrorCode#INVALID_OVERRIDE_NAMED
   * @see StaticWarningCode#INVALID_GETTER_OVERRIDE_RETURN_TYPE
   * @see StaticWarningCode#INVALID_METHOD_OVERRIDE_RETURN_TYPE
   * @see StaticWarningCode#INVALID_METHOD_OVERRIDE_NORMAL_PARAM_TYPE
   * @see StaticWarningCode#INVALID_SETTER_OVERRIDE_NORMAL_PARAM_TYPE
   * @see StaticWarningCode#INVALID_METHOD_OVERRIDE_OPTIONAL_PARAM_TYPE
   * @see StaticWarningCode#INVALID_METHOD_OVERRIDE_NAMED_PARAM_TYPE
   * @see StaticWarningCode#INVALID_OVERRIDE_DIFFERENT_DEFAULT_VALUES
   */
  private boolean checkForAllInvalidOverrideErrorCodes(ExecutableElement executableElement,
      ParameterElement[] parameters, ASTNode[] parameterLocations, SimpleIdentifier errorNameTarget) {
    String executableElementName = executableElement.getName();
    boolean executableElementPrivate = Identifier.isPrivateName(executableElementName);
    ExecutableElement overriddenExecutable = inheritanceManager.lookupInheritance(
        enclosingClass,
        executableElementName);

    boolean isGetter = false;
    boolean isSetter = false;
    if (executableElement instanceof PropertyAccessorElement) {
      PropertyAccessorElement accessorElement = (PropertyAccessorElement) executableElement;
      isGetter = accessorElement.isGetter();
      isSetter = accessorElement.isSetter();
    }

    // SWC.INSTANCE_METHOD_NAME_COLLIDES_WITH_SUPERCLASS_STATIC
    if (overriddenExecutable == null) {
      if (!isGetter && !isSetter && !executableElement.isOperator()) {
        HashSet<ClassElement> visitedClasses = new HashSet<ClassElement>();
        InterfaceType superclassType = enclosingClass.getSupertype();
        ClassElement superclassElement = superclassType == null ? null
            : superclassType.getElement();
        while (superclassElement != null && !visitedClasses.contains(superclassElement)) {
          visitedClasses.add(superclassElement);
          LibraryElement superclassLibrary = superclassElement.getLibrary();
          // Check fields.
          FieldElement[] fieldElts = superclassElement.getFields();
          for (FieldElement fieldElt : fieldElts) {
            // We need the same name.
            if (!fieldElt.getName().equals(executableElementName)) {
              continue;
            }
            // Ignore if private in a different library - cannot collide.
            if (executableElementPrivate && !currentLibrary.equals(superclassLibrary)) {
              continue;
            }
            // instance vs. static
            if (fieldElt.isStatic()) {
              errorReporter.reportError(
                  StaticWarningCode.INSTANCE_METHOD_NAME_COLLIDES_WITH_SUPERCLASS_STATIC,
                  errorNameTarget,
                  executableElementName,
                  fieldElt.getEnclosingElement().getDisplayName());
              return true;
            }
          }
          // Check methods.
          MethodElement[] methodElements = superclassElement.getMethods();
          for (MethodElement methodElement : methodElements) {
            // We need the same name.
            if (!methodElement.getName().equals(executableElementName)) {
              continue;
            }
            // Ignore if private in a different library - cannot collide.
            if (executableElementPrivate && !currentLibrary.equals(superclassLibrary)) {
              continue;
            }
            // instance vs. static
            if (methodElement.isStatic()) {
              errorReporter.reportError(
                  StaticWarningCode.INSTANCE_METHOD_NAME_COLLIDES_WITH_SUPERCLASS_STATIC,
                  errorNameTarget,
                  executableElementName,
                  methodElement.getEnclosingElement().getDisplayName());
              return true;
            }
          }
          superclassType = superclassElement.getSupertype();
          superclassElement = superclassType == null ? null : superclassType.getElement();
        }
      }
      return false;
    }

    FunctionType overridingFT = executableElement.getType();
    FunctionType overriddenFT = overriddenExecutable.getType();
    InterfaceType enclosingType = enclosingClass.getType();
    overriddenFT = inheritanceManager.substituteTypeArgumentsInMemberFromInheritance(
        overriddenFT,
        executableElementName,
        enclosingType);

    if (overridingFT == null || overriddenFT == null) {
      return false;
    }

    Type overridingFTReturnType = overridingFT.getReturnType();
    Type overriddenFTReturnType = overriddenFT.getReturnType();
    Type[] overridingNormalPT = overridingFT.getNormalParameterTypes();
    Type[] overriddenNormalPT = overriddenFT.getNormalParameterTypes();
    Type[] overridingPositionalPT = overridingFT.getOptionalParameterTypes();
    Type[] overriddenPositionalPT = overriddenFT.getOptionalParameterTypes();
    Map<String, Type> overridingNamedPT = overridingFT.getNamedParameterTypes();
    Map<String, Type> overriddenNamedPT = overriddenFT.getNamedParameterTypes();

    // CTEC.INVALID_OVERRIDE_REQUIRED, CTEC.INVALID_OVERRIDE_POSITIONAL and CTEC.INVALID_OVERRIDE_NAMED
    if (overridingNormalPT.length > overriddenNormalPT.length) {
      errorReporter.reportError(
          StaticWarningCode.INVALID_OVERRIDE_REQUIRED,
          errorNameTarget,
          overriddenNormalPT.length,
          overriddenExecutable.getEnclosingElement().getDisplayName());
      return true;
    }
    if (overridingNormalPT.length + overridingPositionalPT.length < overriddenPositionalPT.length
        + overriddenNormalPT.length) {
      errorReporter.reportError(
          StaticWarningCode.INVALID_OVERRIDE_POSITIONAL,
          errorNameTarget,
          overriddenPositionalPT.length + overriddenNormalPT.length,
          overriddenExecutable.getEnclosingElement().getDisplayName());
      return true;
    }
    // For each named parameter in the overridden method, verify that there is the same name in
    // the overriding method, and in the same order.
    Set<String> overridingParameterNameSet = overridingNamedPT.keySet();
    Iterator<String> overriddenParameterNameIterator = overriddenNamedPT.keySet().iterator();
    while (overriddenParameterNameIterator.hasNext()) {
      String overriddenParamName = overriddenParameterNameIterator.next();
      if (!overridingParameterNameSet.contains(overriddenParamName)) {
        // The overridden method expected the overriding method to have overridingParamName,
        // but it does not.
        errorReporter.reportError(
            StaticWarningCode.INVALID_OVERRIDE_NAMED,
            errorNameTarget,
            overriddenParamName,
            overriddenExecutable.getEnclosingElement().getDisplayName());
        return true;
      }
    }

    // SWC.INVALID_METHOD_OVERRIDE_*

    // The following (comparing the function types with isSubtypeOf):
    // !overridingFT.isSubtypeOf(overriddenFT)
    // is equivalent to the following checks, we break it is split up for the purposes of
    // providing better error messages.

    // SWC.INVALID_METHOD_OVERRIDE_RETURN_TYPE
    if (!overriddenFTReturnType.equals(VoidTypeImpl.getInstance())
        && !overridingFTReturnType.isAssignableTo(overriddenFTReturnType)) {
      errorReporter.reportError(
          !isGetter ? StaticWarningCode.INVALID_METHOD_OVERRIDE_RETURN_TYPE
              : StaticWarningCode.INVALID_GETTER_OVERRIDE_RETURN_TYPE,
          errorNameTarget,
          overridingFTReturnType.getDisplayName(),
          overriddenFTReturnType.getDisplayName(),
          overriddenExecutable.getEnclosingElement().getDisplayName());
      return true;
    }

    // SWC.INVALID_METHOD_OVERRIDE_NORMAL_PARAM_TYPE
    if (parameterLocations == null) {
      return false;
    }
    int parameterIndex = 0;
    for (int i = 0; i < overridingNormalPT.length; i++) {
      if (!overridingNormalPT[i].isAssignableTo(overriddenNormalPT[i])) {
        errorReporter.reportError(
            !isSetter ? StaticWarningCode.INVALID_METHOD_OVERRIDE_NORMAL_PARAM_TYPE
                : StaticWarningCode.INVALID_SETTER_OVERRIDE_NORMAL_PARAM_TYPE,
            parameterLocations[parameterIndex],
            overridingNormalPT[i].getDisplayName(),
            overriddenNormalPT[i].getDisplayName(),
            overriddenExecutable.getEnclosingElement().getDisplayName());
        return true;
      }
      parameterIndex++;
    }

    // SWC.INVALID_METHOD_OVERRIDE_OPTIONAL_PARAM_TYPE
    for (int i = 0; i < overriddenPositionalPT.length; i++) {
      if (!overridingPositionalPT[i].isAssignableTo(overriddenPositionalPT[i])) {
        errorReporter.reportError(
            StaticWarningCode.INVALID_METHOD_OVERRIDE_OPTIONAL_PARAM_TYPE,
            parameterLocations[parameterIndex],
            overridingPositionalPT[i].getDisplayName(),
            overriddenPositionalPT[i].getDisplayName(),
            overriddenExecutable.getEnclosingElement().getDisplayName());
        return true;
      }
      parameterIndex++;
    }

    // SWC.INVALID_METHOD_OVERRIDE_NAMED_PARAM_TYPE & SWC.INVALID_OVERRIDE_DIFFERENT_DEFAULT_VALUES
    Iterator<Entry<String, Type>> overriddenNamedPTIterator = overriddenNamedPT.entrySet().iterator();
    while (overriddenNamedPTIterator.hasNext()) {
      Entry<String, Type> overriddenNamedPTEntry = overriddenNamedPTIterator.next();
      Type overridingType = overridingNamedPT.get(overriddenNamedPTEntry.getKey());
      if (overridingType == null) {
        // Error, this is never reached- INVALID_OVERRIDE_NAMED would have been created above if
        // this could be reached.
        continue;
      }
      if (!overriddenNamedPTEntry.getValue().isAssignableTo(overridingType)) {
        // lookup the parameter for the error to select
        ParameterElement parameterToSelect = null;
        ASTNode parameterLocationToSelect = null;
        for (int i = 0; i < parameters.length; i++) {
          ParameterElement parameter = parameters[i];
          if (parameter.getParameterKind() == ParameterKind.NAMED
              && overriddenNamedPTEntry.getKey().equals(parameter.getName())) {
            parameterToSelect = parameter;
            parameterLocationToSelect = parameterLocations[i];
            break;
          }
        }
        if (parameterToSelect != null) {
          errorReporter.reportError(
              StaticWarningCode.INVALID_METHOD_OVERRIDE_NAMED_PARAM_TYPE,
              parameterLocationToSelect,
              overridingType.getDisplayName(),
              overriddenNamedPTEntry.getValue().getDisplayName(),
              overriddenExecutable.getEnclosingElement().getDisplayName());
          return true;
        }
      }
    }
    // SWC.INVALID_OVERRIDE_DIFFERENT_DEFAULT_VALUES
    //
    // Create three arrays: an array of the optional parameter ASTs (FormalParameters), an array of
    // the optional parameters elements from our method, and finally an array of the optional
    // parameter elements from the method we are overriding.
    //
    boolean foundError = false;
    ArrayList<ASTNode> formalParameters = new ArrayList<ASTNode>();
    ArrayList<ParameterElementImpl> parameterElts = new ArrayList<ParameterElementImpl>();
    ArrayList<ParameterElementImpl> overriddenParameterElts = new ArrayList<ParameterElementImpl>();
    ParameterElement[] overriddenPEs = overriddenExecutable.getParameters();
    for (int i = 0; i < parameters.length; i++) {
      ParameterElement parameter = parameters[i];
      if (parameter.getParameterKind().isOptional()) {
        formalParameters.add(parameterLocations[i]);
        parameterElts.add((ParameterElementImpl) parameter);
      }
    }
    for (ParameterElement parameterElt : overriddenPEs) {
      if (parameterElt.getParameterKind().isOptional()) {
        if (parameterElt instanceof ParameterElementImpl) {
          overriddenParameterElts.add((ParameterElementImpl) parameterElt);
        }
      }
    }
    //
    // Next compare the list of optional parameter elements to the list of overridden optional
    // parameter elements.
    //
    if (parameterElts.size() > 0) {
      if (parameterElts.get(0).getParameterKind() == ParameterKind.NAMED) {
        // Named parameters, consider the names when matching the parameterElts to the overriddenParameterElts
        for (int i = 0; i < parameterElts.size(); i++) {
          ParameterElementImpl parameterElt = parameterElts.get(i);
          EvaluationResultImpl result = parameterElt.getEvaluationResult();
          // TODO (jwren) Ignore Object types, see Dart bug 11287
          if (result == null || result == ValidResult.RESULT_OBJECT) {
            continue;
          }
          String parameterName = parameterElt.getName();
          for (int j = 0; j < overriddenParameterElts.size(); j++) {
            ParameterElementImpl overriddenParameterElt = overriddenParameterElts.get(j);
            String overriddenParameterName = overriddenParameterElt.getName();
            if (parameterName != null && parameterName.equals(overriddenParameterName)) {
              EvaluationResultImpl overriddenResult = overriddenParameterElt.getEvaluationResult();
              if (overriddenResult == null || result == ValidResult.RESULT_OBJECT) {
                break;
              }
              if (!result.equalValues(overriddenResult)) {
                errorReporter.reportError(
                    StaticWarningCode.INVALID_OVERRIDE_DIFFERENT_DEFAULT_VALUES_NAMED,
                    formalParameters.get(i),
                    overriddenExecutable.getEnclosingElement().getDisplayName(),
                    overriddenExecutable.getDisplayName(),
                    parameterName);
                foundError = true;
              }
            }
          }
        }
      } else {
        // Positional parameters, consider the positions when matching the parameterElts to the overriddenParameterElts
        for (int i = 0; i < parameterElts.size() && i < overriddenParameterElts.size(); i++) {
          ParameterElementImpl parameterElt = parameterElts.get(i);
          EvaluationResultImpl result = parameterElt.getEvaluationResult();
          // TODO (jwren) Ignore Object types, see Dart bug 11287
          if (result == null || result == ValidResult.RESULT_OBJECT) {
            continue;
          }
          ParameterElementImpl overriddenParameterElt = overriddenParameterElts.get(i);
          EvaluationResultImpl overriddenResult = overriddenParameterElt.getEvaluationResult();
          if (overriddenResult == null || result == ValidResult.RESULT_OBJECT) {
            continue;
          }
          if (!result.equalValues(overriddenResult)) {
            errorReporter.reportError(
                StaticWarningCode.INVALID_OVERRIDE_DIFFERENT_DEFAULT_VALUES_POSITIONAL,
                formalParameters.get(i),
                overriddenExecutable.getEnclosingElement().getDisplayName(),
                overriddenExecutable.getDisplayName());
            foundError = true;
          }
        }
      }
    }
    return foundError;
  }

  /**
   * This checks the passed field declaration against override-error codes.
   * 
   * @param node the {@link MethodDeclaration} to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see #checkForAllInvalidOverrideErrorCodes(ExecutableElement)
   */
  private boolean checkForAllInvalidOverrideErrorCodes(FieldDeclaration node) {
    if (enclosingClass == null || node.isStatic()) {
      return false;
    }
    boolean hasProblems = false;
    VariableDeclarationList fields = node.getFields();
    for (VariableDeclaration field : fields.getVariables()) {
      FieldElement element = (FieldElement) field.getElement();
      if (element == null) {
        continue;
      }
      PropertyAccessorElement getter = element.getGetter();
      PropertyAccessorElement setter = element.getSetter();
      SimpleIdentifier fieldName = field.getName();
      if (getter != null) {
        hasProblems |= checkForAllInvalidOverrideErrorCodes(
            getter,
            ParameterElementImpl.EMPTY_ARRAY,
            ASTNode.EMPTY_ARRAY,
            fieldName);
      }
      if (setter != null) {
        hasProblems |= checkForAllInvalidOverrideErrorCodes(
            setter,
            setter.getParameters(),
            new ASTNode[] {fieldName},
            fieldName);
      }
    }
    return hasProblems;
  }

  /**
   * This checks the passed method declaration against override-error codes.
   * 
   * @param node the {@link MethodDeclaration} to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see #checkForAllInvalidOverrideErrorCodes(ExecutableElement)
   */
  private boolean checkForAllInvalidOverrideErrorCodes(MethodDeclaration node) {
    if (enclosingClass == null || node.isStatic() || node.getBody() instanceof NativeFunctionBody) {
      return false;
    }
    ExecutableElement executableElement = node.getElement();
    if (executableElement == null) {
      return false;
    }
    SimpleIdentifier methodName = node.getName();
    if (methodName.isSynthetic()) {
      return false;
    }
    FormalParameterList formalParameterList = node.getParameters();
    NodeList<FormalParameter> parameterList = formalParameterList != null
        ? formalParameterList.getParameters() : null;
    ASTNode[] parameters = parameterList != null
        ? parameterList.toArray(new ASTNode[parameterList.size()]) : null;
    return checkForAllInvalidOverrideErrorCodes(
        executableElement,
        executableElement.getParameters(),
        parameters,
        methodName);
  }

  /**
   * This verifies that all classes of the passed 'with' clause are valid.
   * 
   * @param node the 'with' clause to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#MIXIN_DECLARES_CONSTRUCTOR
   * @see CompileTimeErrorCode#MIXIN_INHERITS_FROM_NOT_OBJECT
   * @see CompileTimeErrorCode#MIXIN_REFERENCES_SUPER
   */
  private boolean checkForAllMixinErrorCodes(WithClause withClause) {
    if (withClause == null) {
      return false;
    }
    boolean problemReported = false;
    for (TypeName mixinName : withClause.getMixinTypes()) {
      Type mixinType = mixinName.getType();
      if (!(mixinType instanceof InterfaceType)) {
        continue;
      }
      if (checkForExtendsOrImplementsDisallowedClass(
          mixinName,
          CompileTimeErrorCode.MIXIN_OF_DISALLOWED_CLASS)) {
        problemReported = true;
      } else {
        ClassElement mixinElement = ((InterfaceType) mixinType).getElement();
        problemReported |= checkForMixinDeclaresConstructor(mixinName, mixinElement);
        problemReported |= checkForMixinInheritsNotFromObject(mixinName, mixinElement);
        problemReported |= checkForMixinReferencesSuper(mixinName, mixinElement);
      }
    }
    return problemReported;
  }

  /**
   * This checks error related to the redirected constructors.
   * 
   * @param node the constructor declaration to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticWarningCode#REDIRECT_TO_INVALID_RETURN_TYPE
   * @see StaticWarningCode#REDIRECT_TO_INVALID_FUNCTION_TYPE
   * @see StaticWarningCode#REDIRECT_TO_MISSING_CONSTRUCTOR
   */
  private boolean checkForAllRedirectConstructorErrorCodes(ConstructorDeclaration node) {
    //
    // Prepare redirected constructor node
    //
    ConstructorName redirectedConstructor = node.getRedirectedConstructor();
    if (redirectedConstructor == null) {
      return false;
    }
    //
    // Prepare redirected constructor type
    //
    ConstructorElement redirectedElement = redirectedConstructor.getStaticElement();
    if (redirectedElement == null) {
      //
      // If the element is null, we check for the REDIRECT_TO_MISSING_CONSTRUCTOR case
      //
      TypeName constructorTypeName = redirectedConstructor.getType();
      Type redirectedType = constructorTypeName.getType();
      if (redirectedType != null && redirectedType.getElement() != null
          && !redirectedType.isDynamic()) {
        //
        // Prepare the constructor name
        //
        String constructorStrName = constructorTypeName.getName().getName();
        if (redirectedConstructor.getName() != null) {
          constructorStrName += '.' + redirectedConstructor.getName().getName();
        }
        ErrorCode errorCode = node.getConstKeyword() != null
            ? CompileTimeErrorCode.REDIRECT_TO_MISSING_CONSTRUCTOR
            : StaticWarningCode.REDIRECT_TO_MISSING_CONSTRUCTOR;
        errorReporter.reportError(
            errorCode,
            redirectedConstructor,
            constructorStrName,
            redirectedType.getDisplayName());
        return true;
      }
      return false;
    }
    FunctionType redirectedType = redirectedElement.getType();
    Type redirectedReturnType = redirectedType.getReturnType();
    //
    // Report specific problem when return type is incompatible
    //
    FunctionType constructorType = node.getElement().getType();
    Type constructorReturnType = constructorType.getReturnType();
    if (!redirectedReturnType.isAssignableTo(constructorReturnType)) {
      errorReporter.reportError(
          StaticWarningCode.REDIRECT_TO_INVALID_RETURN_TYPE,
          redirectedConstructor,
          redirectedReturnType,
          constructorReturnType);
      return true;
    }
    //
    // Check parameters
    //
    if (!redirectedType.isSubtypeOf(constructorType)) {
      errorReporter.reportError(
          StaticWarningCode.REDIRECT_TO_INVALID_FUNCTION_TYPE,
          redirectedConstructor,
          redirectedType,
          constructorType);
      return true;
    }
    return false;
  }

  /**
   * This checks that the return statement of the form <i>return e;</i> is not in a generative
   * constructor.
   * <p>
   * This checks that return statements without expressions are not in a generative constructor and
   * the return type is not assignable to {@code null}; that is, we don't have {@code return;} if
   * the enclosing method has a return type.
   * <p>
   * This checks that the return type matches the type of the declared return type in the enclosing
   * method or function.
   * 
   * @param node the return statement to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#RETURN_IN_GENERATIVE_CONSTRUCTOR
   * @see StaticWarningCode#RETURN_WITHOUT_VALUE
   * @see StaticTypeWarningCode#RETURN_OF_INVALID_TYPE
   */
  private boolean checkForAllReturnStatementErrorCodes(ReturnStatement node) {
    FunctionType functionType = enclosingFunction == null ? null : enclosingFunction.getType();
    Type expectedReturnType = functionType == null ? DynamicTypeImpl.getInstance()
        : functionType.getReturnType();
    Expression returnExpression = node.getExpression();
    // RETURN_IN_GENERATIVE_CONSTRUCTOR
    boolean isGenerativeConstructor = enclosingFunction instanceof ConstructorElement
        && !((ConstructorElement) enclosingFunction).isFactory();
    if (isGenerativeConstructor) {
      if (returnExpression == null) {
        return false;
      }
      errorReporter.reportError(
          CompileTimeErrorCode.RETURN_IN_GENERATIVE_CONSTRUCTOR,
          returnExpression);
      return true;
    }
    // RETURN_WITHOUT_VALUE
    if (returnExpression == null) {
      if (VoidTypeImpl.getInstance().isAssignableTo(expectedReturnType)) {
        return false;
      }
      errorReporter.reportError(StaticWarningCode.RETURN_WITHOUT_VALUE, node);
      return true;
    }
    // RETURN_OF_INVALID_TYPE
    return checkForReturnOfInvalidType(returnExpression, expectedReturnType);
  }

  /**
   * This verifies that the export namespace of the passed export directive does not export any name
   * already exported by other export directive.
   * 
   * @param node the export directive node to report problem on
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#AMBIGUOUS_EXPORT
   */
  private boolean checkForAmbiguousExport(ExportDirective node) {
    // prepare ExportElement
    if (!(node.getElement() instanceof ExportElement)) {
      return false;
    }
    ExportElement exportElement = (ExportElement) node.getElement();
    // prepare exported library
    LibraryElement exportedLibrary = exportElement.getExportedLibrary();
    if (exportedLibrary == null) {
      return false;
    }
    // check exported names
    Namespace namespace = new NamespaceBuilder().createExportNamespace(exportElement);
    Set<String> newNames = namespace.getDefinedNames().keySet();
    for (String name : newNames) {
      ExportElement prevElement = exportedNames.get(name);
      if (prevElement != null && prevElement != exportElement) {
        errorReporter.reportError(
            CompileTimeErrorCode.AMBIGUOUS_EXPORT,
            node,
            name,
            prevElement.getExportedLibrary().getDefiningCompilationUnit().getDisplayName(),
            exportedLibrary.getDefiningCompilationUnit().getDisplayName());
        return true;
      } else {
        exportedNames.put(name, exportElement);
      }
    }
    return false;
  }

  /**
   * This verifies that the passed argument definition test identifier is a parameter.
   * 
   * @param node the {@link ArgumentDefinitionTest} to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#ARGUMENT_DEFINITION_TEST_NON_PARAMETER
   */
  private boolean checkForArgumentDefinitionTestNonParameter(ArgumentDefinitionTest node) {
    SimpleIdentifier identifier = node.getIdentifier();
    Element element = identifier.getStaticElement();
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
   * This verifies that the passed arguments can be assigned to their corresponding parameters.
   * 
   * @param node the arguments to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticWarningCode#ARGUMENT_TYPE_NOT_ASSIGNABLE
   */
  private boolean checkForArgumentTypeNotAssignable(ArgumentList argumentList) {
    if (argumentList == null) {
      return false;
    }
    boolean problemReported = false;
    for (Expression argument : argumentList.getArguments()) {
      problemReported |= checkForArgumentTypeNotAssignable(argument);
    }
    // done
    return problemReported;
  }

  /**
   * This verifies that the passed argument can be assigned to its corresponding parameter.
   * 
   * @param argument the argument to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticWarningCode#ARGUMENT_TYPE_NOT_ASSIGNABLE
   */
  private boolean checkForArgumentTypeNotAssignable(Expression argument) {
    if (argument == null) {
      return false;
    }

    ParameterElement staticParameterElement = argument.getStaticParameterElement();
    Type staticParameterType = staticParameterElement == null ? null
        : staticParameterElement.getType();

    ParameterElement propagatedParameterElement = argument.getPropagatedParameterElement();
    Type propagatedParameterType = propagatedParameterElement == null ? null
        : propagatedParameterElement.getType();

    return checkForArgumentTypeNotAssignable(
        argument,
        staticParameterType,
        propagatedParameterType,
        StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
  }

  /**
   * This verifies that the passed expression can be assigned to its corresponding parameters.
   * 
   * @param expression the expression to evaluate
   * @param expectedStaticType the expected static type
   * @param expectedPropagatedType the expected propagated type, may be {@code null}
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticWarningCode#ARGUMENT_TYPE_NOT_ASSIGNABLE
   */
  private boolean checkForArgumentTypeNotAssignable(Expression expression, Type expectedStaticType,
      Type expectedPropagatedType, ErrorCode errorCode) {
    return checkForArgumentTypeNotAssignable(
        expression,
        expectedStaticType,
        getStaticType(expression),
        expectedPropagatedType,
        expression.getPropagatedType(),
        errorCode);
  }

  /**
   * This verifies that the passed expression can be assigned to its corresponding parameters.
   * 
   * @param expression the expression to evaluate
   * @param expectedStaticType the expected static type of the parameter
   * @param actualStaticType the actual static type of the argument
   * @param expectedPropagatedType the expected propagated type of the parameter, may be
   *          {@code null}
   * @param actualPropagatedType the expected propagated type of the parameter, may be {@code null}
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticWarningCode#ARGUMENT_TYPE_NOT_ASSIGNABLE
   */
  private boolean checkForArgumentTypeNotAssignable(Expression expression, Type expectedStaticType,
      Type actualStaticType, Type expectedPropagatedType, Type actualPropagatedType,
      ErrorCode errorCode) {
    //
    // Test static type information
    //
    if (actualStaticType == null || expectedStaticType == null) {
      return false;
    }
    if (strictMode) {
      if (actualStaticType.isAssignableTo(expectedStaticType)) {
        return false;
      }
      errorReporter.reportError(
          errorCode,
          expression,
          actualStaticType.getDisplayName(),
          expectedStaticType.getDisplayName());
      return true;
    }
    //
    // Test propagated type information
    //
    if (actualPropagatedType == null || expectedPropagatedType == null) {
      if (actualStaticType.isAssignableTo(expectedStaticType)) {
        return false;
      }
      errorReporter.reportError(
          errorCode,
          expression,
          actualStaticType.getDisplayName(),
          expectedStaticType.getDisplayName());
      return true;
    }

    if (actualStaticType.isAssignableTo(expectedStaticType)
        || actualStaticType.isAssignableTo(expectedPropagatedType)
        || actualPropagatedType.isAssignableTo(expectedStaticType)
        || actualPropagatedType.isAssignableTo(expectedPropagatedType)) {
      return false;
    }
    errorReporter.reportError(
        errorCode,
        expression,
        (actualPropagatedType == null ? actualStaticType : actualPropagatedType).getDisplayName(),
        (expectedPropagatedType == null ? expectedStaticType : expectedPropagatedType).getDisplayName());
    return true;
  }

  /**
   * This verifies that left hand side of the passed assignment expression is not final.
   * 
   * @param node the assignment expression to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticWarningCode#ASSIGNMENT_TO_FINAL
   */
  private boolean checkForAssignmentToFinal(AssignmentExpression node) {
    Expression leftExpression = node.getLeftHandSide();
    return checkForAssignmentToFinal(leftExpression);
  }

  /**
   * This verifies that the passed expression is not final.
   * 
   * @param node the expression to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticWarningCode#ASSIGNMENT_TO_CONST
   * @see StaticWarningCode#ASSIGNMENT_TO_FINAL
   * @see StaticWarningCode#ASSIGNMENT_TO_METHOD
   */
  private boolean checkForAssignmentToFinal(Expression expression) {
    // prepare element
    Element element = null;
    if (expression instanceof Identifier) {
      element = ((Identifier) expression).getStaticElement();
    }
    if (expression instanceof PropertyAccess) {
      element = ((PropertyAccess) expression).getPropertyName().getStaticElement();
    }
    // check if element is assignable
    if (element instanceof PropertyAccessorElement) {
      PropertyAccessorElement accessor = (PropertyAccessorElement) element;
      element = accessor.getVariable();
    }
    if (element instanceof VariableElement) {
      VariableElement variable = (VariableElement) element;
      if (variable.isConst()) {
        errorReporter.reportError(StaticWarningCode.ASSIGNMENT_TO_CONST, expression);
        return true;
      }
      if (variable.isFinal()) {
        errorReporter.reportError(StaticWarningCode.ASSIGNMENT_TO_FINAL, expression);
        return true;
      }
      return false;
    }
    if (element instanceof MethodElement) {
      errorReporter.reportError(StaticWarningCode.ASSIGNMENT_TO_METHOD, expression);
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
   *          {@link CompileTimeErrorCode#BUILT_IN_IDENTIFIER_AS_TYPE_PARAMETER_NAME} or
   *          {@link CompileTimeErrorCode#BUILT_IN_IDENTIFIER_AS_TYPEDEF_NAME}
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#BUILT_IN_IDENTIFIER_AS_TYPE_NAME
   * @see CompileTimeErrorCode#BUILT_IN_IDENTIFIER_AS_TYPE_PARAMETER_NAME
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
   * This verifies that the given switch case is terminated with 'break', 'continue', 'return' or
   * 'throw'.
   * 
   * @param node the switch case to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticWarningCode#CASE_BLOCK_NOT_TERMINATED
   */
  private boolean checkForCaseBlockNotTerminated(SwitchCase node) {
    NodeList<Statement> statements = node.getStatements();
    if (statements.isEmpty()) {
      // fall-through without statements at all
      ASTNode parent = node.getParent();
      if (parent instanceof SwitchStatement) {
        SwitchStatement switchStatement = (SwitchStatement) parent;
        NodeList<SwitchMember> members = switchStatement.getMembers();
        int index = members.indexOf(node);
        if (index != -1 && index < members.size() - 1) {
          return false;
        }
      }
      // no other switch member after this one
    } else {
      Statement statement = statements.get(statements.size() - 1);
      // terminated with statement
      if (statement instanceof BreakStatement || statement instanceof ContinueStatement
          || statement instanceof ReturnStatement) {
        return false;
      }
      // terminated with 'throw' expression
      if (statement instanceof ExpressionStatement) {
        Expression expression = ((ExpressionStatement) statement).getExpression();
        if (expression instanceof ThrowExpression) {
          return false;
        }
      }
    }
    // report error
    errorReporter.reportError(StaticWarningCode.CASE_BLOCK_NOT_TERMINATED, node.getKeyword());
    return true;
  }

  /**
   * This verifies that the switch cases in the given switch statement is terminated with 'break',
   * 'continue', 'return' or 'throw'.
   * 
   * @param node the switch statement containing the cases to be checked
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticWarningCode#CASE_BLOCK_NOT_TERMINATED
   */
  private boolean checkForCaseBlocksNotTerminated(SwitchStatement node) {
    boolean foundError = false;
    NodeList<SwitchMember> members = node.getMembers();
    int lastMember = members.size() - 1;
    for (int i = 0; i < lastMember; i++) {
      SwitchMember member = members.get(i);
      if (member instanceof SwitchCase) {
        foundError |= checkForCaseBlockNotTerminated((SwitchCase) member);
      }
    }
    return foundError;
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
    errorReporter.reportError(
        CompileTimeErrorCode.CASE_EXPRESSION_TYPE_IMPLEMENTS_EQUALS,
        node.getKeyword(),
        type.getDisplayName());
    return true;
  }

  /**
   * This verifies that the passed method declaration is abstract only if the enclosing class is
   * also abstract.
   * 
   * @param node the method declaration to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticWarningCode#CONCRETE_CLASS_WITH_ABSTRACT_MEMBER
   */
  private boolean checkForConcreteClassWithAbstractMember(MethodDeclaration node) {
    if (node.isAbstract() && enclosingClass != null && !enclosingClass.isAbstract()) {
      SimpleIdentifier methodName = node.getName();
      errorReporter.reportError(
          StaticWarningCode.CONCRETE_CLASS_WITH_ABSTRACT_MEMBER,
          methodName,
          methodName.getName(),
          enclosingClass.getDisplayName());
      return true;
    }
    return false;
  }

  /**
   * This verifies all possible conflicts of the constructor name with other constructors and
   * members of the same class.
   * 
   * @param node the constructor declaration to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#DUPLICATE_CONSTRUCTOR_DEFAULT
   * @see CompileTimeErrorCode#DUPLICATE_CONSTRUCTOR_NAME
   * @see CompileTimeErrorCode#CONFLICTING_CONSTRUCTOR_NAME_AND_FIELD
   * @see CompileTimeErrorCode#CONFLICTING_CONSTRUCTOR_NAME_AND_METHOD
   */
  private boolean checkForConflictingConstructorNameAndMember(ConstructorDeclaration node) {
    ConstructorElement constructorElement = node.getElement();
    SimpleIdentifier constructorName = node.getName();
    String name = constructorElement.getName();
    ClassElement classElement = constructorElement.getEnclosingElement();
    // constructors
    ConstructorElement[] constructors = classElement.getConstructors();
    for (ConstructorElement otherConstructor : constructors) {
      if (otherConstructor == constructorElement) {
        continue;
      }
      if (ObjectUtilities.equals(name, otherConstructor.getName())) {
        if (name == null || name.length() == 0) {
          errorReporter.reportError(CompileTimeErrorCode.DUPLICATE_CONSTRUCTOR_DEFAULT, node);
        } else {
          errorReporter.reportError(CompileTimeErrorCode.DUPLICATE_CONSTRUCTOR_NAME, node, name);
        }
        return true;
      }
    }
    // conflict with class member
    if (constructorName != null && constructorElement != null && !constructorName.isSynthetic()) {
      // fields
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
      // methods
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
   * This verifies that the {@link #enclosingClass} does not have method and getter with the same
   * names.
   * 
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#CONFLICTING_GETTER_AND_METHOD
   * @see CompileTimeErrorCode#CONFLICTING_METHOD_AND_GETTER
   */
  private boolean checkForConflictingGetterAndMethod() {
    if (enclosingClass == null) {
      return false;
    }
    boolean hasProblem = false;
    // method declared in the enclosing class vs. inherited getter
    for (MethodElement method : enclosingClass.getMethods()) {
      String name = method.getName();
      // find inherited property accessor (and can be only getter)
      ExecutableElement inherited = inheritanceManager.lookupInheritance(enclosingClass, name);
      if (!(inherited instanceof PropertyAccessorElement)) {
        continue;
      }
      // report problem
      hasProblem = true;
      errorReporter.reportError(
          CompileTimeErrorCode.CONFLICTING_GETTER_AND_METHOD,
          method.getNameOffset(),
          name.length(),
          enclosingClass.getDisplayName(),
          inherited.getEnclosingElement().getDisplayName(),
          name);
    }
    // getter declared in the enclosing class vs. inherited method
    for (PropertyAccessorElement accessor : enclosingClass.getAccessors()) {
      if (!accessor.isGetter()) {
        continue;
      }
      String name = accessor.getName();
      // find inherited method
      ExecutableElement inherited = inheritanceManager.lookupInheritance(enclosingClass, name);
      if (!(inherited instanceof MethodElement)) {
        continue;
      }
      // report problem
      hasProblem = true;
      errorReporter.reportError(
          CompileTimeErrorCode.CONFLICTING_METHOD_AND_GETTER,
          accessor.getNameOffset(),
          name.length(),
          enclosingClass.getDisplayName(),
          inherited.getEnclosingElement().getDisplayName(),
          name);
    }
    // done
    return hasProblem;
  }

  /**
   * This verifies that the superclass of the enclosing class does not declare accessible static
   * member with the same name as the passed instance getter/setter method declaration.
   * 
   * @param node the method declaration to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticWarningCode#CONFLICTING_INSTANCE_GETTER_AND_SUPERCLASS_MEMBER
   * @see StaticWarningCode#CONFLICTING_INSTANCE_SETTER_AND_SUPERCLASS_MEMBER
   */
  private boolean checkForConflictingInstanceGetterAndSuperclassMember(MethodDeclaration node) {
    if (node.isStatic()) {
      return false;
    }
    // prepare name
    SimpleIdentifier nameNode = node.getName();
    if (nameNode == null) {
      return false;
    }
    String name = nameNode.getName();
    // prepare enclosing type
    if (enclosingClass == null) {
      return false;
    }
    InterfaceType enclosingType = enclosingClass.getType();
    // try to find super element
    ExecutableElement superElement;
    superElement = enclosingType.lookUpGetterInSuperclass(name, currentLibrary);
    if (superElement == null) {
      superElement = enclosingType.lookUpSetterInSuperclass(name, currentLibrary);
    }
    if (superElement == null) {
      superElement = enclosingType.lookUpMethodInSuperclass(name, currentLibrary);
    }
    if (superElement == null) {
      return false;
    }
    // OK, not static
    if (!superElement.isStatic()) {
      return false;
    }
    // prepare "super" type to report its name
    ClassElement superElementClass = (ClassElement) superElement.getEnclosingElement();
    InterfaceType superElementType = superElementClass.getType();
    // report problem
    if (node.isGetter()) {
      errorReporter.reportError(
          StaticWarningCode.CONFLICTING_INSTANCE_GETTER_AND_SUPERCLASS_MEMBER,
          nameNode,
          superElementType.getDisplayName());
    } else {
      errorReporter.reportError(
          StaticWarningCode.CONFLICTING_INSTANCE_SETTER_AND_SUPERCLASS_MEMBER,
          nameNode,
          superElementType.getDisplayName());
    }
    return true;
  }

  /**
   * This verifies that the enclosing class does not have a setter with the same name as the passed
   * instance method declaration.
   * 
   * @param node the method declaration to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticWarningCode#CONFLICTING_INSTANCE_METHOD_SETTER
   */
  private boolean checkForConflictingInstanceMethodSetter(MethodDeclaration node) {
    if (node.isStatic()) {
      return false;
    }
    // prepare name
    SimpleIdentifier nameNode = node.getName();
    if (nameNode == null) {
      return false;
    }
    String name = nameNode.getName();
    // ensure that we have enclosing class
    if (enclosingClass == null) {
      return false;
    }
    // try to find setter
    ExecutableElement setter = inheritanceManager.lookupMember(enclosingClass, name + "=");
    if (setter == null) {
      return false;
    }
    // report problem
    errorReporter.reportError(
        StaticWarningCode.CONFLICTING_INSTANCE_METHOD_SETTER,
        nameNode,
        enclosingClass.getDisplayName(),
        name,
        setter.getEnclosingElement().getDisplayName());
    return true;
  }

  /**
   * This verifies that the enclosing class does not have an instance member with the same name as
   * the passed static getter method declaration.
   * 
   * @param node the method declaration to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticWarningCode#CONFLICTING_STATIC_GETTER_AND_INSTANCE_SETTER
   */
  private boolean checkForConflictingStaticGetterAndInstanceSetter(MethodDeclaration node) {
    if (!node.isStatic()) {
      return false;
    }
    // prepare name
    SimpleIdentifier nameNode = node.getName();
    if (nameNode == null) {
      return false;
    }
    String name = nameNode.getName();
    // prepare enclosing type
    if (enclosingClass == null) {
      return false;
    }
    InterfaceType enclosingType = enclosingClass.getType();
    // try to find setter
    ExecutableElement setter = enclosingType.lookUpSetter(name, currentLibrary);
    if (setter == null) {
      return false;
    }
    // OK, also static
    if (setter.isStatic()) {
      return false;
    }
    // prepare "setter" type to report its name
    ClassElement setterClass = (ClassElement) setter.getEnclosingElement();
    InterfaceType setterType = setterClass.getType();
    // report problem
    errorReporter.reportError(
        StaticWarningCode.CONFLICTING_STATIC_GETTER_AND_INSTANCE_SETTER,
        nameNode,
        setterType.getDisplayName());
    return true;
  }

  /**
   * This verifies that the enclosing class does not have an instance member with the same name as
   * the passed static getter method declaration.
   * 
   * @param node the method declaration to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticWarningCode#CONFLICTING_STATIC_SETTER_AND_INSTANCE_MEMBER
   */
  private boolean checkForConflictingStaticSetterAndInstanceMember(MethodDeclaration node) {
    if (!node.isStatic()) {
      return false;
    }
    // prepare name
    SimpleIdentifier nameNode = node.getName();
    if (nameNode == null) {
      return false;
    }
    String name = nameNode.getName();
    // prepare enclosing type
    if (enclosingClass == null) {
      return false;
    }
    InterfaceType enclosingType = enclosingClass.getType();
    // try to find member
    ExecutableElement member;
    member = enclosingType.lookUpMethod(name, currentLibrary);
    if (member == null) {
      member = enclosingType.lookUpGetter(name, currentLibrary);
    }
    if (member == null) {
      member = enclosingType.lookUpSetter(name, currentLibrary);
    }
    if (member == null) {
      return false;
    }
    // OK, also static
    if (member.isStatic()) {
      return false;
    }
    // prepare "member" type to report its name
    ClassElement memberClass = (ClassElement) member.getEnclosingElement();
    InterfaceType memberType = memberClass.getType();
    // report problem
    errorReporter.reportError(
        StaticWarningCode.CONFLICTING_STATIC_SETTER_AND_INSTANCE_MEMBER,
        nameNode,
        memberType.getDisplayName());
    return true;
  }

  /**
   * This verifies all conflicts between type variable and enclosing class. TODO(scheglov)
   * 
   * @param node the class declaration to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#CONFLICTING_TYPE_VARIABLE_AND_CLASS
   * @see CompileTimeErrorCode#CONFLICTING_TYPE_VARIABLE_AND_MEMBER
   */
  private boolean checkForConflictingTypeVariableErrorCodes(ClassDeclaration node) {
    boolean problemReported = false;
    for (TypeParameterElement typeParameter : enclosingClass.getTypeParameters()) {
      String name = typeParameter.getName();
      // name is same as the name of the enclosing class
      if (enclosingClass.getName().equals(name)) {
        errorReporter.reportError(
            CompileTimeErrorCode.CONFLICTING_TYPE_VARIABLE_AND_CLASS,
            typeParameter.getNameOffset(),
            name.length(),
            name);
        problemReported = true;
      }
      // check members
      if (enclosingClass.getMethod(name) != null || enclosingClass.getGetter(name) != null
          || enclosingClass.getSetter(name) != null) {
        errorReporter.reportError(
            CompileTimeErrorCode.CONFLICTING_TYPE_VARIABLE_AND_MEMBER,
            typeParameter.getNameOffset(),
            name.length(),
            name);
        problemReported = true;
      }
    }
    return problemReported;
  }

  /**
   * This verifies that if the passed constructor declaration is 'const' then there are no
   * invocations of non-'const' super constructors.
   * 
   * @param node the constructor declaration to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#CONST_CONSTRUCTOR_WITH_NON_CONST_SUPER
   */
  private boolean checkForConstConstructorWithNonConstSuper(ConstructorDeclaration node) {
    if (!isEnclosingConstructorConst) {
      return false;
    }
    // OK, const factory, checked elsewhere
    if (node.getFactoryKeyword() != null) {
      return false;
    }
    // try to find and check super constructor invocation
    for (ConstructorInitializer initializer : node.getInitializers()) {
      if (initializer instanceof SuperConstructorInvocation) {
        SuperConstructorInvocation superInvocation = (SuperConstructorInvocation) initializer;
        ConstructorElement element = superInvocation.getStaticElement();
        if (element.isConst()) {
          return false;
        }
        errorReporter.reportError(
            CompileTimeErrorCode.CONST_CONSTRUCTOR_WITH_NON_CONST_SUPER,
            superInvocation);
        return true;
      }
    }
    // no explicit super constructor invocation, check default constructor
    InterfaceType supertype = enclosingClass.getSupertype();
    if (supertype == null) {
      return false;
    }
    if (supertype.isObject()) {
      return false;
    }
    ConstructorElement unnamedConstructor = supertype.getElement().getUnnamedConstructor();
    if (unnamedConstructor == null) {
      return false;
    }
    if (unnamedConstructor.isConst()) {
      return false;
    }
    // default constructor is not 'const', report problem
    errorReporter.reportError(CompileTimeErrorCode.CONST_CONSTRUCTOR_WITH_NON_CONST_SUPER, node);
    return true;
  }

  /**
   * This verifies that if the passed constructor declaration is 'const' then there are no non-final
   * instance variable.
   * 
   * @param node the constructor declaration to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#CONST_CONSTRUCTOR_WITH_NON_FINAL_FIELD
   */
  private boolean checkForConstConstructorWithNonFinalField(ConstructorDeclaration node) {
    if (!isEnclosingConstructorConst) {
      return false;
    }
    // check if there is non-final field
    ConstructorElement constructorElement = node.getElement();
    ClassElement classElement = constructorElement.getEnclosingElement();
    if (!classElement.hasNonFinalField()) {
      return false;
    }
    // report problem
    errorReporter.reportError(CompileTimeErrorCode.CONST_CONSTRUCTOR_WITH_NON_FINAL_FIELD, node);
    return true;
  }

  /**
   * This verifies that the passed throw expression is not enclosed in a 'const' constructor
   * declaration.
   * 
   * @param node the throw expression expression to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#CONST_CONSTRUCTOR_THROWS_EXCEPTION
   */
  private boolean checkForConstEvalThrowsException(ThrowExpression node) {
    if (isEnclosingConstructorConst) {
      errorReporter.reportError(CompileTimeErrorCode.CONST_CONSTRUCTOR_THROWS_EXCEPTION, node);
      return true;
    }
    return false;
  }

  /**
   * This verifies that the passed normal formal parameter is not 'const'.
   * 
   * @param node the normal formal parameter to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
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
   * This verifies that the passed expression (used as a key in constant map) has class type that
   * does not declare operator <i>==<i>.
   * 
   * @param key the expression to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#CONST_MAP_KEY_EXPRESSION_TYPE_IMPLEMENTS_EQUALS
   */
  private boolean checkForConstMapKeyExpressionTypeImplementsEquals(Expression key) {
    Type type = key.getStaticType();
    if (!implementsEqualsWhenNotAllowed(type)) {
      return false;
    }
    // report error
    errorReporter.reportError(
        CompileTimeErrorCode.CONST_MAP_KEY_EXPRESSION_TYPE_IMPLEMENTS_EQUALS,
        key,
        type.getDisplayName());
    return true;
  }

  /**
   * This verifies that the all keys of the passed map literal have class type that does not declare
   * operator <i>==<i>.
   * 
   * @param key the map literal to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#CONST_MAP_KEY_EXPRESSION_TYPE_IMPLEMENTS_EQUALS
   */
  private boolean checkForConstMapKeyExpressionTypeImplementsEquals(MapLiteral node) {
    // OK, not const.
    if (node.getConstKeyword() == null) {
      return false;
    }
    // Check every map entry.
    boolean hasProblems = false;
    for (MapLiteralEntry entry : node.getEntries()) {
      Expression key = entry.getKey();
      hasProblems |= checkForConstMapKeyExpressionTypeImplementsEquals(key);
    }
    return hasProblems;
  }

  /**
   * This verifies that the passed instance creation expression is not being invoked on an abstract
   * class.
   * 
   * @param node the instance creation expression to evaluate
   * @param typeName the {@link TypeName} of the {@link ConstructorName} from the
   *          {@link InstanceCreationExpression}, this is the AST node that the error is attached to
   * @param type the type being constructed with this {@link InstanceCreationExpression}
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticWarningCode#CONST_WITH_ABSTRACT_CLASS
   * @see StaticWarningCode#NEW_WITH_ABSTRACT_CLASS
   */
  private boolean checkForConstOrNewWithAbstractClass(InstanceCreationExpression node,
      TypeName typeName, InterfaceType type) {
    if (type.getElement().isAbstract()) {
      ConstructorElement element = node.getStaticElement();
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
   * This verifies that the passed 'const' instance creation expression is not being invoked on a
   * constructor that is not 'const'.
   * <p>
   * This method assumes that the instance creation was tested to be 'const' before being called.
   * 
   * @param node the instance creation expression to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#CONST_WITH_NON_CONST
   */
  private boolean checkForConstWithNonConst(InstanceCreationExpression node) {
    ConstructorElement constructorElement = node.getStaticElement();
    if (constructorElement != null && !constructorElement.isConst()) {
      errorReporter.reportError(CompileTimeErrorCode.CONST_WITH_NON_CONST, node);
      return true;
    }
    return false;
  }

  /**
   * This verifies that the passed 'const' instance creation expression does not reference any type
   * parameters.
   * <p>
   * This method assumes that the instance creation was tested to be 'const' before being called.
   * 
   * @param node the instance creation expression to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#CONST_WITH_TYPE_PARAMETERS
   */
  private boolean checkForConstWithTypeParameters(InstanceCreationExpression node) {
    ConstructorName constructorName = node.getConstructorName();
    if (constructorName == null) {
      return false;
    }
    TypeName typeName = constructorName.getType();
    return checkForConstWithTypeParameters(typeName);
  }

  /**
   * This verifies that the passed type name does not reference any type parameters.
   * 
   * @param typeName the type name to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#CONST_WITH_TYPE_PARAMETERS
   */
  private boolean checkForConstWithTypeParameters(TypeName typeName) {
    // something wrong with AST
    if (typeName == null) {
      return false;
    }
    Identifier name = typeName.getName();
    if (name == null) {
      return false;
    }
    // should not be a type parameter
    if (name.getStaticElement() instanceof TypeParameterElement) {
      errorReporter.reportError(CompileTimeErrorCode.CONST_WITH_TYPE_PARAMETERS, name);
    }
    // check type arguments
    TypeArgumentList typeArguments = typeName.getTypeArguments();
    if (typeArguments != null) {
      boolean hasError = false;
      for (TypeName argument : typeArguments.getArguments()) {
        hasError |= checkForConstWithTypeParameters(argument);
      }
      return hasError;
    }
    // OK
    return false;
  }

  /**
   * This verifies that if the passed 'const' instance creation expression is being invoked on the
   * resolved constructor.
   * <p>
   * This method assumes that the instance creation was tested to be 'const' before being called.
   * 
   * @param node the instance creation expression to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#CONST_WITH_UNDEFINED_CONSTRUCTOR
   * @see CompileTimeErrorCode#CONST_WITH_UNDEFINED_CONSTRUCTOR_DEFAULT
   */
  private boolean checkForConstWithUndefinedConstructor(InstanceCreationExpression node) {
    // OK if resolved
    if (node.getStaticElement() != null) {
      return false;
    }
    // prepare constructor name
    ConstructorName constructorName = node.getConstructorName();
    if (constructorName == null) {
      return false;
    }
    // prepare class name
    TypeName type = constructorName.getType();
    if (type == null) {
      return false;
    }
    Identifier className = type.getName();
    // report as named or default constructor absence
    SimpleIdentifier name = constructorName.getName();
    if (name != null) {
      errorReporter.reportError(
          CompileTimeErrorCode.CONST_WITH_UNDEFINED_CONSTRUCTOR,
          name,
          className,
          name);
    } else {
      errorReporter.reportError(
          CompileTimeErrorCode.CONST_WITH_UNDEFINED_CONSTRUCTOR_DEFAULT,
          constructorName,
          className);
    }
    return true;
  }

  /**
   * This verifies that there are no default parameters in the passed function type alias.
   * 
   * @param node the function type alias to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
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
   * This verifies that the given default formal parameter is not part of a function typed
   * parameter.
   * 
   * @param node the default formal parameter to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#DEFAULT_VALUE_IN_FUNCTION_TYPED_PARAMETER
   */
  private boolean checkForDefaultValueInFunctionTypedParameter(DefaultFormalParameter node) {
    // OK, not in a function typed parameter.
    if (!isInFunctionTypedFormalParameter) {
      return false;
    }
    // OK, no default value.
    if (node.getDefaultValue() == null) {
      return false;
    }
    // Report problem.
    errorReporter.reportError(CompileTimeErrorCode.DEFAULT_VALUE_IN_FUNCTION_TYPED_PARAMETER, node);
    return true;
  }

  /**
   * This verifies that the enclosing class does not have an instance member with the given name of
   * the static member.
   * 
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#DUPLICATE_DEFINITION_INHERITANCE
   */
  private boolean checkForDuplicateDefinitionInheritance() {
    if (enclosingClass == null) {
      return false;
    }
    boolean hasProblem = false;
    for (ExecutableElement member : enclosingClass.getMethods()) {
      if (!member.isStatic()) {
        continue;
      }
      hasProblem |= checkForDuplicateDefinitionInheritance(member);
    }
    for (ExecutableElement member : enclosingClass.getAccessors()) {
      if (!member.isStatic()) {
        continue;
      }
      hasProblem |= checkForDuplicateDefinitionInheritance(member);
    }
    return hasProblem;
  }

  /**
   * This verifies that the enclosing class does not have an instance member with the given name of
   * the static member.
   * 
   * @param staticMember the static member to check conflict for
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#DUPLICATE_DEFINITION_INHERITANCE
   */
  private boolean checkForDuplicateDefinitionInheritance(ExecutableElement staticMember) {
    // prepare name
    String name = staticMember.getName();
    if (name == null) {
      return false;
    }
    // try to find member
    ExecutableElement inheritedMember = inheritanceManager.lookupInheritance(enclosingClass, name);
    if (inheritedMember == null) {
      return false;
    }
    // OK, also static
    if (inheritedMember.isStatic()) {
      return false;
    }
    // report problem
    errorReporter.reportError(
        CompileTimeErrorCode.DUPLICATE_DEFINITION_INHERITANCE,
        staticMember.getNameOffset(),
        name.length(),
        name,
        inheritedMember.getEnclosingElement().getDisplayName());
    return true;
  }

  /**
   * This verifies if the passed list literal has type arguments then there is exactly one.
   * 
   * @param node the list literal to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticTypeWarningCode#EXPECTED_ONE_LIST_TYPE_ARGUMENTS
   */
  private boolean checkForExpectedOneListTypeArgument(ListLiteral node) {
    // prepare type arguments
    TypeArgumentList typeArguments = node.getTypeArguments();
    if (typeArguments == null) {
      return false;
    }
    // check number of type arguments
    int num = typeArguments.getArguments().size();
    if (num == 1) {
      return false;
    }
    // report problem
    errorReporter.reportError(
        StaticTypeWarningCode.EXPECTED_ONE_LIST_TYPE_ARGUMENTS,
        typeArguments,
        num);
    return true;
  }

  /**
   * This verifies the passed import has unique name among other exported libraries.
   * 
   * @param node the export directive to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#EXPORT_DUPLICATED_LIBRARY_NAME
   */
  private boolean checkForExportDuplicateLibraryName(ExportDirective node) {
    // prepare import element
    Element nodeElement = node.getElement();
    if (!(nodeElement instanceof ExportElement)) {
      return false;
    }
    ExportElement nodeExportElement = (ExportElement) nodeElement;
    // prepare exported library
    LibraryElement nodeLibrary = nodeExportElement.getExportedLibrary();
    if (nodeLibrary == null) {
      return false;
    }
    String name = nodeLibrary.getName();
    // check if there is other exported library with the same name
    LibraryElement prevLibrary = nameToExportElement.get(name);
    if (prevLibrary != null) {
      if (!prevLibrary.equals(nodeLibrary)) {
        errorReporter.reportError(
            StaticWarningCode.EXPORT_DUPLICATED_LIBRARY_NAME,
            node,
            prevLibrary.getDefiningCompilationUnit().getDisplayName(),
            nodeLibrary.getDefiningCompilationUnit().getDisplayName(),
            name);
        return true;
      }
    } else {
      nameToExportElement.put(name, nodeLibrary);
    }
    // OK
    return false;
  }

  /**
   * Check that if the visiting library is not system, then any passed library should not be SDK
   * internal library.
   * 
   * @param node the export directive to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#EXPORT_INTERNAL_LIBRARY
   */
  private boolean checkForExportInternalLibrary(ExportDirective node) {
    if (isInSystemLibrary) {
      return false;
    }
    // prepare export element
    Element element = node.getElement();
    if (!(element instanceof ExportElement)) {
      return false;
    }
    ExportElement exportElement = (ExportElement) element;
    // should be private
    DartSdk sdk = currentLibrary.getContext().getSourceFactory().getDartSdk();
    String uri = exportElement.getUri();
    SdkLibrary sdkLibrary = sdk.getSdkLibrary(uri);
    if (sdkLibrary == null) {
      return false;
    }
    if (!sdkLibrary.isInternal()) {
      return false;
    }
    // report problem
    errorReporter.reportError(CompileTimeErrorCode.EXPORT_INTERNAL_LIBRARY, node, node.getUri());
    return true;
  }

  /**
   * This verifies that the passed extends clause does not extend classes such as num or String.
   * 
   * @param node the extends clause to test
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#EXTENDS_DISALLOWED_CLASS
   */
  private boolean checkForExtendsDisallowedClass(ExtendsClause extendsClause) {
    if (extendsClause == null) {
      return false;
    }
    return checkForExtendsOrImplementsDisallowedClass(
        extendsClause.getSuperclass(),
        CompileTimeErrorCode.EXTENDS_DISALLOWED_CLASS);
  }

  /**
   * This verifies that the passed type name does not extend or implement classes such as 'num' or
   * 'String'.
   * 
   * @param node the type name to test
   * @return {@code true} if and only if an error code is generated on the passed node
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
        errorReporter.reportError(errorCode, typeName, disallowedType.getDisplayName());
        return true;
      }
    }
    return false;
  }

  /**
   * This verifies that the passed constructor field initializer has compatible field and
   * initializer expression types.
   * 
   * @param node the constructor field initializer to test
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#CONST_FIELD_INITIALIZER_NOT_ASSIGNABLE
   * @see StaticWarningCode#FIELD_INITIALIZER_NOT_ASSIGNABLE
   */
  private boolean checkForFieldInitializerNotAssignable(ConstructorFieldInitializer node) {
    // prepare field element
    Element fieldNameElement = node.getFieldName().getStaticElement();
    if (!(fieldNameElement instanceof FieldElement)) {
      return false;
    }
    FieldElement fieldElement = (FieldElement) fieldNameElement;
    // prepare field type
    Type fieldType = fieldElement.getType();
    // prepare expression type
    Expression expression = node.getExpression();
    if (expression == null) {
      return false;
    }
    // test the static type of the expression
    Type staticType = getStaticType(expression);
    if (staticType == null) {
      return false;
    }
    if (staticType.isAssignableTo(fieldType)) {
      return false;
    } else if (strictMode) {
      // report problem
      if (isEnclosingConstructorConst) {
        errorReporter.reportError(
            CompileTimeErrorCode.CONST_FIELD_INITIALIZER_NOT_ASSIGNABLE,
            expression,
            staticType.getDisplayName(),
            fieldType.getDisplayName());
      } else {
        errorReporter.reportError(
            StaticWarningCode.FIELD_INITIALIZER_NOT_ASSIGNABLE,
            expression,
            staticType.getDisplayName(),
            fieldType.getDisplayName());
      }
      return true;
    }
    // test the propagated type of the expression
    Type propagatedType = expression.getPropagatedType();
    if (propagatedType != null && propagatedType.isAssignableTo(fieldType)) {
      return false;
    }
    // report problem
    if (isEnclosingConstructorConst) {
      errorReporter.reportError(
          CompileTimeErrorCode.CONST_FIELD_INITIALIZER_NOT_ASSIGNABLE,
          expression,
          (propagatedType == null ? staticType : propagatedType).getDisplayName(),
          fieldType.getDisplayName());
    } else {
      errorReporter.reportError(
          StaticWarningCode.FIELD_INITIALIZER_NOT_ASSIGNABLE,
          expression,
          (propagatedType == null ? staticType : propagatedType).getDisplayName(),
          fieldType.getDisplayName());
    }
    return true;
  }

  /**
   * This verifies that the passed field formal parameter is in a constructor declaration.
   * 
   * @param node the field formal parameter to test
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#FIELD_INITIALIZER_OUTSIDE_CONSTRUCTOR
   */
  private boolean checkForFieldInitializingFormalRedirectingConstructor(FieldFormalParameter node) {
    ConstructorDeclaration constructor = node.getAncestor(ConstructorDeclaration.class);
    if (constructor == null) {
      errorReporter.reportError(CompileTimeErrorCode.FIELD_INITIALIZER_OUTSIDE_CONSTRUCTOR, node);
      return true;
    }
    // constructor cannot be a factory
    if (constructor.getFactoryKeyword() != null) {
      errorReporter.reportError(CompileTimeErrorCode.FIELD_INITIALIZER_FACTORY_CONSTRUCTOR, node);
      return true;
    }
    // constructor cannot have a redirection
    for (ConstructorInitializer initializer : constructor.getInitializers()) {
      if (initializer instanceof RedirectingConstructorInvocation) {
        errorReporter.reportError(
            CompileTimeErrorCode.FIELD_INITIALIZER_REDIRECTING_CONSTRUCTOR,
            node);
        return true;
      }
    }
    // OK
    return false;
  }

  /**
   * This verifies that final fields that are declared, without any constructors in the enclosing
   * class, are initialized. Cases in which there is at least one constructor are handled at the end
   * of {@link #checkForAllFinalInitializedErrorCodes(ConstructorDeclaration)}.
   * 
   * @param node the class declaration to test
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#CONST_NOT_INITIALIZED
   * @see StaticWarningCode#FINAL_NOT_INITIALIZED
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
   * @see CompileTimeErrorCode#CONST_NOT_INITIALIZED
   * @see StaticWarningCode#FINAL_NOT_INITIALIZED
   */
  private boolean checkForFinalNotInitialized(VariableDeclarationList node) {
    if (isInNativeClass) {
      return false;
    }
    boolean foundError = false;
    if (!node.isSynthetic()) {
      NodeList<VariableDeclaration> variables = node.getVariables();
      for (VariableDeclaration variable : variables) {
        if (variable.getInitializer() == null) {
          if (node.isConst()) {
            errorReporter.reportError(
                CompileTimeErrorCode.CONST_NOT_INITIALIZED,
                variable.getName(),
                variable.getName().getName());
          } else if (node.isFinal()) {
            errorReporter.reportError(
                StaticWarningCode.FINAL_NOT_INITIALIZED,
                variable.getName(),
                variable.getName().getName());
          }
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
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#IMPLEMENTS_DISALLOWED_CLASS
   */
  private boolean checkForImplementsDisallowedClass(ImplementsClause implementsClause) {
    if (implementsClause == null) {
      return false;
    }
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
   * This verifies that if the passed identifier is part of constructor initializer, then it does
   * not reference implicitly 'this' expression.
   * 
   * @param node the simple identifier to test
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#IMPLICIT_THIS_REFERENCE_IN_INITIALIZER
   * @see CompileTimeErrorCode#INSTANCE_MEMBER_ACCESS_FROM_STATIC TODO(scheglov) rename thid method
   */
  private boolean checkForImplicitThisReferenceInInitializer(SimpleIdentifier node) {
    if (!isInConstructorInitializer && !isInStaticMethod && !isInInstanceVariableInitializer
        && !isInStaticVariableDeclaration) {
      return false;
    }
    // prepare element
    Element element = node.getStaticElement();
    if (!(element instanceof MethodElement || element instanceof PropertyAccessorElement)) {
      return false;
    }
    // static element
    ExecutableElement executableElement = (ExecutableElement) element;
    if (executableElement.isStatic()) {
      return false;
    }
    // not a class member
    Element enclosingElement = element.getEnclosingElement();
    if (!(enclosingElement instanceof ClassElement)) {
      return false;
    }
    // comment
    ASTNode parent = node.getParent();
    if (parent instanceof CommentReference) {
      return false;
    }
    // qualified method invocation
    if (parent instanceof MethodInvocation) {
      MethodInvocation invocation = (MethodInvocation) parent;
      if (invocation.getMethodName() == node && invocation.getRealTarget() != null) {
        return false;
      }
    }
    // qualified property access
    if (parent instanceof PropertyAccess) {
      PropertyAccess access = (PropertyAccess) parent;
      if (access.getPropertyName() == node && access.getRealTarget() != null) {
        return false;
      }
    }
    if (parent instanceof PrefixedIdentifier) {
      PrefixedIdentifier prefixed = (PrefixedIdentifier) parent;
      if (prefixed.getIdentifier() == node) {
        return false;
      }
    }
    // report problem
    if (isInStaticMethod) {
      errorReporter.reportError(CompileTimeErrorCode.INSTANCE_MEMBER_ACCESS_FROM_STATIC, node);
    } else {
      errorReporter.reportError(CompileTimeErrorCode.IMPLICIT_THIS_REFERENCE_IN_INITIALIZER, node);
    }
    return true;
  }

  /**
   * This verifies the passed import has unique name among other imported libraries.
   * 
   * @param node the import directive to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#IMPORT_DUPLICATED_LIBRARY_NAME
   */
  private boolean checkForImportDuplicateLibraryName(ImportDirective node) {
    // prepare import element
    ImportElement nodeImportElement = node.getElement();
    if (nodeImportElement == null) {
      return false;
    }
    // prepare imported library
    LibraryElement nodeLibrary = nodeImportElement.getImportedLibrary();
    if (nodeLibrary == null) {
      return false;
    }
    String name = nodeLibrary.getName();
    // check if there is other imported library with the same name
    LibraryElement prevLibrary = nameToImportElement.get(name);
    if (prevLibrary != null) {
      if (!prevLibrary.equals(nodeLibrary)) {
        errorReporter.reportError(
            StaticWarningCode.IMPORT_DUPLICATED_LIBRARY_NAME,
            node,
            prevLibrary.getDefiningCompilationUnit().getDisplayName(),
            nodeLibrary.getDefiningCompilationUnit().getDisplayName(),
            name);
        return true;
      }
    } else {
      nameToImportElement.put(name, nodeLibrary);
    }
    // OK
    return false;
  }

  /**
   * Check that if the visiting library is not system, then any passed library should not be SDK
   * internal library.
   * 
   * @param node the import directive to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#IMPORT_INTERNAL_LIBRARY
   */
  private boolean checkForImportInternalLibrary(ImportDirective node) {
    if (isInSystemLibrary) {
      return false;
    }
    // prepare import element
    ImportElement importElement = node.getElement();
    if (importElement == null) {
      return false;
    }
    // should be private
    DartSdk sdk = currentLibrary.getContext().getSourceFactory().getDartSdk();
    String uri = importElement.getUri();
    SdkLibrary sdkLibrary = sdk.getSdkLibrary(uri);
    if (sdkLibrary == null) {
      return false;
    }
    if (!sdkLibrary.isInternal()) {
      return false;
    }
    // report problem
    errorReporter.reportError(CompileTimeErrorCode.IMPORT_INTERNAL_LIBRARY, node, node.getUri());
    return true;
  }

  /**
   * This verifies that the passed switch statement case expressions all have the same type.
   * 
   * @param node the switch statement to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#INCONSISTENT_CASE_EXPRESSION_TYPES
   */
  private boolean checkForInconsistentCaseExpressionTypes(SwitchStatement node) {
    // TODO(jwren) Revisit this algorithm, should there up to n-1 errors?
    NodeList<SwitchMember> switchMembers = node.getMembers();
    boolean foundError = false;
    Type firstType = null;
    for (SwitchMember switchMember : switchMembers) {
      if (switchMember instanceof SwitchCase) {
        SwitchCase switchCase = (SwitchCase) switchMember;
        Expression expression = switchCase.getExpression();
        if (firstType == null) {
          // TODO(brianwilkerson) This is failing with const variables whose declared type is
          // dynamic. The problem is that we don't have any way to propagate type information for
          // the variable.
          firstType = expression.getBestType();
        } else {
          Type nType = expression.getBestType();
          if (!firstType.equals(nType)) {
            errorReporter.reportError(
                CompileTimeErrorCode.INCONSISTENT_CASE_EXPRESSION_TYPES,
                expression,
                expression.toSource(),
                firstType.getDisplayName());
            foundError = true;
          }
        }
      }
    }
    if (!foundError) {
      checkForCaseExpressionTypeImplementsEquals(node, firstType);
    }
    return foundError;
  }

  /**
   * For each class declaration, this method is called which verifies that all inherited members are
   * inherited consistently.
   * 
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticTypeWarningCode#INCONSISTENT_METHOD_INHERITANCE
   */
  private boolean checkForInconsistentMethodInheritance() {
    // Ensure that the inheritance manager has a chance to generate all errors we may care about,
    // note that we ensure that the interfaces data since there are no errors.
    inheritanceManager.getMapOfMembersInheritedFromInterfaces(enclosingClass);
    HashSet<AnalysisError> errors = inheritanceManager.getErrors(enclosingClass);
    if (errors == null || errors.isEmpty()) {
      return false;
    }
    for (AnalysisError error : errors) {
      errorReporter.reportError(error);
    }
    return true;
  }

  /**
   * This checks that if the given "target" is not a type reference then the "name" is reference to
   * an instance member.
   * 
   * @param target the target of the name access to evaluate
   * @param name the accessed name to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticTypeWarningCode#INSTANCE_ACCESS_TO_STATIC_MEMBER
   */
  private boolean checkForInstanceAccessToStaticMember(Expression target, SimpleIdentifier name) {
    // OK, no target
    if (target == null) {
      return false;
    }
    // OK, in comment
    if (isInComment) {
      return false;
    }
    // prepare member Element
    Element element = name.getStaticElement();
    if (!(element instanceof ExecutableElement)) {
      return false;
    }
    ExecutableElement executableElement = (ExecutableElement) element;
    // OK, top-level element
    if (!(executableElement.getEnclosingElement() instanceof ClassElement)) {
      return false;
    }
    // OK, instance member
    if (!executableElement.isStatic()) {
      return false;
    }
    // OK, target is a type
    if (isTypeReference(target)) {
      return false;
    }
    // report problem
    errorReporter.reportError(
        StaticTypeWarningCode.INSTANCE_ACCESS_TO_STATIC_MEMBER,
        name,
        name.getName());
    return true;
  }

  /**
   * This verifies that an 'int' can be assigned to the parameter corresponding to the given
   * expression. This is used for prefix and postfix expressions where the argument value is
   * implicit.
   * 
   * @param argument the expression to which the operator is being applied
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticWarningCode#ARGUMENT_TYPE_NOT_ASSIGNABLE
   */
  private boolean checkForIntNotAssignable(Expression argument) {
    if (argument == null) {
      return false;
    }

    ParameterElement staticParameterElement = argument.getStaticParameterElement();
    Type staticParameterType = staticParameterElement == null ? null
        : staticParameterElement.getType();

    ParameterElement propagatedParameterElement = argument.getPropagatedParameterElement();
    Type propagatedParameterType = propagatedParameterElement == null ? null
        : propagatedParameterElement.getType();

    return checkForArgumentTypeNotAssignable(
        argument,
        staticParameterType,
        typeProvider.getIntType(),
        propagatedParameterType,
        typeProvider.getIntType(),
        StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
  }

  /**
   * Given an assignment using a compound assignment operator, this verifies that the given
   * assignment is valid.
   * 
   * @param node the assignment expression being tested
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticTypeWarningCode#INVALID_ASSIGNMENT
   */
  private boolean checkForInvalidAssignment(AssignmentExpression node) {
    Expression lhs = node.getLeftHandSide();
    if (lhs == null) {
      return false;
    }
    VariableElement leftElement = getVariableElement(lhs);
    Type leftType = (leftElement == null) ? getStaticType(lhs) : leftElement.getType();
    MethodElement invokedMethod = node.getStaticElement();
    if (invokedMethod == null) {
      return false;
    }
    Type rightType = invokedMethod.getType().getReturnType();
    if (leftType == null || rightType == null) {
      return false;
    }
    if (!rightType.isAssignableTo(leftType)) {
      errorReporter.reportError(
          StaticTypeWarningCode.INVALID_ASSIGNMENT,
          node.getRightHandSide(),
          rightType.getDisplayName(),
          leftType.getDisplayName());
      return true;
    }
    return false;
  }

  /**
   * This verifies that the passed left hand side and right hand side represent a valid assignment.
   * 
   * @param lhs the left hand side expression
   * @param rhs the right hand side expression
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticTypeWarningCode#INVALID_ASSIGNMENT
   */
  private boolean checkForInvalidAssignment(Expression lhs, Expression rhs) {
    if (lhs == null || rhs == null) {
      return false;
    }
    VariableElement leftElement = getVariableElement(lhs);
    Type leftType = (leftElement == null) ? getStaticType(lhs) : leftElement.getType();
    Type staticRightType = getStaticType(rhs);
    boolean isStaticAssignable = staticRightType.isAssignableTo(leftType);
    Type propagatedRightType = rhs.getPropagatedType();
    if (strictMode || propagatedRightType == null) {
      if (!isStaticAssignable) {
        errorReporter.reportError(
            StaticTypeWarningCode.INVALID_ASSIGNMENT,
            rhs,
            staticRightType.getDisplayName(),
            leftType.getDisplayName());
        return true;
      }
    } else {
      boolean isPropagatedAssignable = propagatedRightType.isAssignableTo(leftType);
      if (!isStaticAssignable && !isPropagatedAssignable) {
        errorReporter.reportError(
            StaticTypeWarningCode.INVALID_ASSIGNMENT,
            rhs,
            staticRightType.getDisplayName(),
            leftType.getDisplayName());
        return true;
      }
    }
    return false;
  }

  /**
   * This verifies that the usage of the passed 'this' is valid.
   * 
   * @param node the 'this' expression to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#INVALID_REFERENCE_TO_THIS
   */
  private boolean checkForInvalidReferenceToThis(ThisExpression node) {
    if (!isThisInValidContext(node)) {
      errorReporter.reportError(CompileTimeErrorCode.INVALID_REFERENCE_TO_THIS, node);
      return true;
    }
    return false;
  }

  /**
   * Checks to ensure that the passed {@link ListLiteral} or {@link MapLiteral} does not have a type
   * parameter as a type argument.
   * 
   * @param arguments a non-{@code null}, non-empty {@link TypeName} node list from the respective
   *          {@link ListLiteral} or {@link MapLiteral}
   * @param errorCode either {@link CompileTimeErrorCode#INVALID_TYPE_ARGUMENT_IN_CONST_LIST} or
   *          {@link CompileTimeErrorCode#INVALID_TYPE_ARGUMENT_IN_CONST_MAP}
   * @return {@code true} if and only if an error code is generated on the passed node
   */
  private boolean checkForInvalidTypeArgumentInConstTypedLiteral(NodeList<TypeName> arguments,
      ErrorCode errorCode) {
    boolean foundError = false;
    for (TypeName typeName : arguments) {
      if (typeName.getType() instanceof TypeParameterType) {
        errorReporter.reportError(errorCode, typeName, typeName.getName());
        foundError = true;
      }
    }
    return foundError;
  }

  /**
   * This verifies that the elements given {@link ListLiteral} are subtypes of the specified element
   * type.
   * 
   * @param node the list literal to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#LIST_ELEMENT_TYPE_NOT_ASSIGNABLE
   * @see StaticWarningCode#LIST_ELEMENT_TYPE_NOT_ASSIGNABLE
   */
  private boolean checkForListElementTypeNotAssignable(ListLiteral node) {
    // Prepare list element type.
    TypeArgumentList typeArgumentList = node.getTypeArguments();
    if (typeArgumentList == null) {
      return false;
    }
    NodeList<TypeName> typeArguments = typeArgumentList.getArguments();
    if (typeArguments.size() < 1) {
      return false;
    }
    Type listElementType = typeArguments.get(0).getType();
    // Prepare problem to report.
    ErrorCode errorCode;
    if (node.getConstKeyword() != null) {
      errorCode = CompileTimeErrorCode.LIST_ELEMENT_TYPE_NOT_ASSIGNABLE;
    } else {
      errorCode = StaticWarningCode.LIST_ELEMENT_TYPE_NOT_ASSIGNABLE;
    }
    // Check every list element.
    boolean hasProblems = false;
    for (Expression element : node.getElements()) {
      hasProblems |= checkForArgumentTypeNotAssignable(element, listElementType, null, errorCode);
    }
    return hasProblems;
  }

  /**
   * This verifies that the key/value of entries of the given {@link MapLiteral} are subtypes of the
   * key/value types specified in the type arguments.
   * 
   * @param node the map literal to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#MAP_KEY_TYPE_NOT_ASSIGNABLE
   * @see CompileTimeErrorCode#MAP_VALUE_TYPE_NOT_ASSIGNABLE
   * @see StaticWarningCode#MAP_KEY_TYPE_NOT_ASSIGNABLE
   * @see StaticWarningCode#MAP_VALUE_TYPE_NOT_ASSIGNABLE
   */
  private boolean checkForMapTypeNotAssignable(MapLiteral node) {
    // Prepare maps key/value types.
    TypeArgumentList typeArgumentList = node.getTypeArguments();
    if (typeArgumentList == null) {
      return false;
    }
    NodeList<TypeName> typeArguments = typeArgumentList.getArguments();
    if (typeArguments.size() < 2) {
      return false;
    }
    Type keyType = typeArguments.get(0).getType();
    Type valueType = typeArguments.get(1).getType();
    // Prepare problem to report.
    ErrorCode keyErrorCode;
    ErrorCode valueErrorCode;
    if (node.getConstKeyword() != null) {
      keyErrorCode = CompileTimeErrorCode.MAP_KEY_TYPE_NOT_ASSIGNABLE;
      valueErrorCode = CompileTimeErrorCode.MAP_VALUE_TYPE_NOT_ASSIGNABLE;
    } else {
      keyErrorCode = StaticWarningCode.MAP_KEY_TYPE_NOT_ASSIGNABLE;
      valueErrorCode = StaticWarningCode.MAP_VALUE_TYPE_NOT_ASSIGNABLE;
    }
    // Check every map entry.
    boolean hasProblems = false;
    NodeList<MapLiteralEntry> entries = node.getEntries();
    for (MapLiteralEntry entry : entries) {
      Expression key = entry.getKey();
      Expression value = entry.getValue();
      hasProblems |= checkForArgumentTypeNotAssignable(key, keyType, null, keyErrorCode);
      hasProblems |= checkForArgumentTypeNotAssignable(value, valueType, null, valueErrorCode);
    }
    return hasProblems;
  }

  /**
   * This verifies that the {@link #enclosingClass} does not define members with the same name as
   * the enclosing class.
   * 
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#MEMBER_WITH_CLASS_NAME
   */
  private boolean checkForMemberWithClassName() {
    if (enclosingClass == null) {
      return false;
    }
    String className = enclosingClass.getName();
    if (className == null) {
      return false;
    }
    boolean problemReported = false;
    // check accessors
    for (PropertyAccessorElement accessor : enclosingClass.getAccessors()) {
      if (className.equals(accessor.getName())) {
        errorReporter.reportError(
            CompileTimeErrorCode.MEMBER_WITH_CLASS_NAME,
            accessor.getNameOffset(),
            className.length());
        problemReported = true;
      }
    }
    // don't check methods, they would be constructors
    // done
    return problemReported;
  }

  /**
   * Check to make sure that all similarly typed accessors are of the same type (including inherited
   * accessors).
   * 
   * @param node the accessor currently being visited
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticWarningCode.MISMATCHED_GETTER_AND_SETTER_TYPES
   * @see StaticWarningCode.MISMATCHED_GETTER_AND_SETTER_TYPES_FROM_SUPERTYPE
   */
  // TODO (jwren) In future nit CL, rename this method (and tests) to be consistent with name of error enum
  // TODO (jwren) Revisit error code messages, to add more clarity, we may need the pair split into four codes
  private boolean checkForMismatchedAccessorTypes(Declaration accessorDeclaration,
      String accessorTextName) {
    ExecutableElement accessorElement = (ExecutableElement) accessorDeclaration.getElement();
    if (!(accessorElement instanceof PropertyAccessorElement)) {
      return false;
    }
    PropertyAccessorElement propertyAccessorElement = (PropertyAccessorElement) accessorElement;
    PropertyAccessorElement counterpartAccessor = null;
    ClassElement enclosingClassForCounterpart = null;
    if (propertyAccessorElement.isGetter()) {
      counterpartAccessor = propertyAccessorElement.getCorrespondingSetter();
    } else {
      counterpartAccessor = propertyAccessorElement.getCorrespondingGetter();
      // If the setter and getter are in the same enclosing element, return, this prevents having
      // MISMATCHED_GETTER_AND_SETTER_TYPES reported twice.
      if (counterpartAccessor != null
          && counterpartAccessor.getEnclosingElement() == propertyAccessorElement.getEnclosingElement()) {
        return false;
      }
    }
    if (counterpartAccessor == null) {
      // If the accessor is declared in a class, check the superclasses.
      if (enclosingClass != null) {
        // Figure out the correct identifier to lookup in the inheritance graph, if 'x', then 'x=',
        // or if 'x=', then 'x'.
        String lookupIdentifier = propertyAccessorElement.getName();
        if (lookupIdentifier.endsWith("=")) {
          lookupIdentifier = lookupIdentifier.substring(0, lookupIdentifier.length() - 1);
        } else {
          lookupIdentifier += "=";
        }
        // lookup with the identifier.
        ExecutableElement elementFromInheritance = inheritanceManager.lookupInheritance(
            enclosingClass,
            lookupIdentifier);
        // Verify that we found something, and that it is an accessor
        if (elementFromInheritance != null
            && elementFromInheritance instanceof PropertyAccessorElement) {
          enclosingClassForCounterpart = (ClassElement) elementFromInheritance.getEnclosingElement();
          counterpartAccessor = (PropertyAccessorElement) elementFromInheritance;
        }
      }
      if (counterpartAccessor == null) {
        return false;
      }
    }

    // Default of null == no accessor or no type (dynamic)
    Type getterType = null;
    Type setterType = null;

    // Get an existing counterpart accessor if any.
    if (propertyAccessorElement.isGetter()) {
      getterType = getGetterType(propertyAccessorElement);
      setterType = getSetterType(counterpartAccessor);
    } else if (propertyAccessorElement.isSetter()) {
      setterType = getSetterType(propertyAccessorElement);
      getterType = getGetterType(counterpartAccessor);
    }

    // If either types are not assignable to each other, report an error (if the getter is null,
    // it is dynamic which is assignable to everything).
    if (setterType != null && getterType != null && !getterType.isAssignableTo(setterType)) {
      if (enclosingClassForCounterpart == null) {
        errorReporter.reportError(
            StaticWarningCode.MISMATCHED_GETTER_AND_SETTER_TYPES,
            accessorDeclaration,
            accessorTextName,
            setterType.getDisplayName(),
            getterType.getDisplayName());
        return true;
      } else {
        errorReporter.reportError(
            StaticWarningCode.MISMATCHED_GETTER_AND_SETTER_TYPES_FROM_SUPERTYPE,
            accessorDeclaration,
            accessorTextName,
            setterType.getDisplayName(),
            getterType.getDisplayName(),
            enclosingClassForCounterpart.getDisplayName());
      }
    }
    return false;
  }

  /**
   * This verifies that the given function body does not contain return statements that both have
   * and do not have return values.
   * 
   * @param node the function body being tested
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticWarningCode#MIXED_RETURN_TYPES
   */
  private boolean checkForMixedReturns(BlockFunctionBody node) {
    if (returnWithCount > 0 && returnWithoutCount > 0) {
      errorReporter.reportError(StaticWarningCode.MIXED_RETURN_TYPES, node);
      return true;
    }
    return false;
  }

  /**
   * This verifies that the passed mixin does not have an explicitly declared constructor.
   * 
   * @param mixinName the node to report problem on
   * @param mixinElement the mixing to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#MIXIN_DECLARES_CONSTRUCTOR
   */
  private boolean checkForMixinDeclaresConstructor(TypeName mixinName, ClassElement mixinElement) {
    for (ConstructorElement constructor : mixinElement.getConstructors()) {
      if (!constructor.isSynthetic() && !constructor.isFactory()) {
        errorReporter.reportError(
            CompileTimeErrorCode.MIXIN_DECLARES_CONSTRUCTOR,
            mixinName,
            mixinElement.getName());
        return true;
      }
    }
    return false;
  }

  /**
   * This verifies that the passed mixin has the 'Object' superclass.
   * 
   * @param mixinName the node to report problem on
   * @param mixinElement the mixing to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#MIXIN_INHERITS_FROM_NOT_OBJECT
   */
  private boolean checkForMixinInheritsNotFromObject(TypeName mixinName, ClassElement mixinElement) {
    InterfaceType mixinSupertype = mixinElement.getSupertype();
    if (mixinSupertype != null) {
      if (!mixinSupertype.isObject() || !mixinElement.isTypedef()
          && mixinElement.getMixins().length != 0) {
        errorReporter.reportError(
            CompileTimeErrorCode.MIXIN_INHERITS_FROM_NOT_OBJECT,
            mixinName,
            mixinElement.getName());
        return true;
      }
    }
    return false;
  }

  /**
   * This verifies that the passed mixin does not reference 'super'.
   * 
   * @param mixinName the node to report problem on
   * @param mixinElement the mixing to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#MIXIN_REFERENCES_SUPER
   */
  private boolean checkForMixinReferencesSuper(TypeName mixinName, ClassElement mixinElement) {
    if (mixinElement.hasReferenceToSuper()) {
      errorReporter.reportError(
          CompileTimeErrorCode.MIXIN_REFERENCES_SUPER,
          mixinName,
          mixinElement.getName());
    }
    return false;
  }

  /**
   * This verifies that the passed constructor has at most one 'super' initializer.
   * 
   * @param node the constructor declaration to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#MULTIPLE_SUPER_INITIALIZERS
   */
  private boolean checkForMultipleSuperInitializers(ConstructorDeclaration node) {
    int numSuperInitializers = 0;
    for (ConstructorInitializer initializer : node.getInitializers()) {
      if (initializer instanceof SuperConstructorInvocation) {
        numSuperInitializers++;
        if (numSuperInitializers > 1) {
          errorReporter.reportError(CompileTimeErrorCode.MULTIPLE_SUPER_INITIALIZERS, initializer);
        }
      }
    }
    return numSuperInitializers > 0;
  }

  /**
   * Checks to ensure that native function bodies can only in SDK code.
   * 
   * @param node the native function body to test
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see ParserErrorCode#NATIVE_FUNCTION_BODY_IN_NON_SDK_CODE
   */
  private boolean checkForNativeFunctionBodyInNonSDKCode(NativeFunctionBody node) {
    // TODO(brianwilkerson) Figure out the right rule for when 'native' is allowed.
    if (!isInSystemLibrary) {
      errorReporter.reportError(ParserErrorCode.NATIVE_FUNCTION_BODY_IN_NON_SDK_CODE, node);
      return true;
    }
    return false;
  }

  /**
   * This verifies that the passed 'new' instance creation expression invokes existing constructor.
   * <p>
   * This method assumes that the instance creation was tested to be 'new' before being called.
   * 
   * @param node the instance creation expression to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticWarningCode#NEW_WITH_UNDEFINED_CONSTRUCTOR
   */
  private boolean checkForNewWithUndefinedConstructor(InstanceCreationExpression node) {
    // OK if resolved
    if (node.getStaticElement() != null) {
      return false;
    }
    // prepare constructor name
    ConstructorName constructorName = node.getConstructorName();
    if (constructorName == null) {
      return false;
    }
    // prepare class name
    TypeName type = constructorName.getType();
    if (type == null) {
      return false;
    }
    Identifier className = type.getName();
    // report as named or default constructor absence
    SimpleIdentifier name = constructorName.getName();
    if (name != null) {
      errorReporter.reportError(
          StaticWarningCode.NEW_WITH_UNDEFINED_CONSTRUCTOR,
          name,
          className,
          name);
    } else {
      errorReporter.reportError(
          StaticWarningCode.NEW_WITH_UNDEFINED_CONSTRUCTOR_DEFAULT,
          constructorName,
          className);
    }
    return true;
  }

  /**
   * This checks that if the passed class declaration implicitly calls default constructor of its
   * superclass, there should be such default constructor - implicit or explicit.
   * 
   * @param node the {@link ClassDeclaration} to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#NO_DEFAULT_SUPER_CONSTRUCTOR_IMPLICIT
   */
  private boolean checkForNoDefaultSuperConstructorImplicit(ClassDeclaration node) {
    // do nothing if there is explicit constructor
    ConstructorElement[] constructors = enclosingClass.getConstructors();
    if (!constructors[0].isSynthetic()) {
      return false;
    }
    // prepare super
    InterfaceType superType = enclosingClass.getSupertype();
    if (superType == null) {
      return false;
    }
    ClassElement superElement = superType.getElement();
    // try to find default generative super constructor
    ConstructorElement superUnnamedConstructor = superElement.getUnnamedConstructor();
    if (superUnnamedConstructor != null) {
      if (superUnnamedConstructor.isFactory()) {
        errorReporter.reportError(
            CompileTimeErrorCode.NON_GENERATIVE_CONSTRUCTOR,
            node.getName(),
            superUnnamedConstructor);
        return true;
      }
      if (superUnnamedConstructor.isDefaultConstructor()) {
        return true;
      }
    }
    // report problem
    errorReporter.reportError(
        CompileTimeErrorCode.NO_DEFAULT_SUPER_CONSTRUCTOR_IMPLICIT,
        node.getName(),
        superType.getDisplayName());
    return true;
  }

  /**
   * This checks that passed class declaration overrides all members required by its superclasses
   * and interfaces.
   * 
   * @param node the {@link ClassDeclaration} to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticWarningCode#NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE
   * @see StaticWarningCode#NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_TWO
   * @see StaticWarningCode#NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_THREE
   * @see StaticWarningCode#NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_FOUR
   * @see StaticWarningCode#NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_FIVE_PLUS
   */
  private boolean checkForNonAbstractClassInheritsAbstractMember(ClassDeclaration node) {
    if (enclosingClass.isAbstract()) {
      return false;
    }

    //
    // Store in local sets the set of all method and accessor names
    //
    MethodElement[] methods = enclosingClass.getMethods();
    PropertyAccessorElement[] accessors = enclosingClass.getAccessors();
    HashSet<String> methodsInEnclosingClass = new HashSet<String>();
    for (MethodElement method : methods) {
      String methodName = method.getName();
      // If the enclosing class declares the method noSuchMethod(), then return.
      // From Spec:  It is a static warning if a concrete class does not have an implementation for
      // a method in any of its superinterfaces unless it declares its own noSuchMethod
      // method (7.10).
      if (methodName.equals(ElementResolver.NO_SUCH_METHOD_METHOD_NAME)) {
        return false;
      }
      methodsInEnclosingClass.add(methodName);
    }
    HashSet<String> accessorsInEnclosingClass = new HashSet<String>();
    for (PropertyAccessorElement accessor : accessors) {
      accessorsInEnclosingClass.add(accessor.getName());
    }

    HashSet<ExecutableElement> missingOverrides = new HashSet<ExecutableElement>();

    // Loop through the set of all executable elements declared in the implicit interface.
    MemberMap membersInheritedFromInterfaces = inheritanceManager.getMapOfMembersInheritedFromInterfaces(enclosingClass);
    MemberMap membersInheritedFromSuperclasses = inheritanceManager.getMapOfMembersInheritedFromClasses(enclosingClass);
    for (int i = 0; i < membersInheritedFromInterfaces.getSize(); i++) {
      String memberName = membersInheritedFromInterfaces.getKey(i);
      ExecutableElement executableElt = membersInheritedFromInterfaces.getValue(i);
      if (memberName == null) {
        break;
      }

      // First check to see if this element was declared in the superclass chain, in which case
      // there is already a concrete implementation.
      ExecutableElement elt = membersInheritedFromSuperclasses.get(executableElt.getName());
      if (elt != null) {
        if (elt instanceof MethodElement && !((MethodElement) elt).isAbstract()) {
          continue;
        } else if (elt instanceof PropertyAccessorElement
            && !((PropertyAccessorElement) elt).isAbstract()) {
          continue;
        }
      }

      if (executableElt instanceof MethodElement) {
        // Verify that this class has a method which overrides the method from the interface.
        // If a method was inherited from an interface, but is not implemented by either the class
        // or one of its superclasses, then add the inherited method to the missingOverides set.
        if (!methodsInEnclosingClass.contains(memberName)
            && !memberHasConcreteMethodImplementationInSuperclassChain(
                enclosingClass,
                memberName,
                new ArrayList<ClassElement>())) {
          missingOverrides.add(executableElt);
        }
      } else if (executableElt instanceof PropertyAccessorElement) {
        // Verify that this class has a member which overrides the method from the interface.
        // If an accessor was inherited from an interface, but is not implemented by either the
        // class or one of its superclasses, then add the inherited accessor to the missingOverides
        // set.
        if (!accessorsInEnclosingClass.contains(memberName)
            && !memberHasConcreteAccessorImplementationInSuperclassChain(
                enclosingClass,
                memberName,
                new ArrayList<ClassElement>())) {
          missingOverrides.add(executableElt);
        }
      }
    }
    int missingOverridesSize = missingOverrides.size();
    if (missingOverridesSize == 0) {
      return false;
    }
    ExecutableElement[] missingOverridesArray = missingOverrides.toArray(new ExecutableElement[missingOverridesSize]);
    ArrayList<String> stringMembersArrayListSet = new ArrayList<String>(
        missingOverridesArray.length);
    for (int i = 0; i < missingOverridesArray.length; i++) {
      String newStrMember = missingOverridesArray[i].getEnclosingElement().getDisplayName() + '.'
          + missingOverridesArray[i].getDisplayName();
      if (!stringMembersArrayListSet.contains(newStrMember)) {
        stringMembersArrayListSet.add(newStrMember);
      }
    }
    String[] stringMembersArray = stringMembersArrayListSet.toArray(new String[stringMembersArrayListSet.size()]);
    AnalysisErrorWithProperties analysisError;
    if (stringMembersArray.length == 1) {
      analysisError = errorReporter.newErrorWithProperties(
          StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE,
          node.getName(),
          stringMembersArray[0]);
    } else if (stringMembersArray.length == 2) {
      analysisError = errorReporter.newErrorWithProperties(
          StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_TWO,
          node.getName(),
          stringMembersArray[0],
          stringMembersArray[1]);
    } else if (stringMembersArray.length == 3) {
      analysisError = errorReporter.newErrorWithProperties(
          StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_THREE,
          node.getName(),
          stringMembersArray[0],
          stringMembersArray[1],
          stringMembersArray[2]);
    } else if (stringMembersArray.length == 4) {
      analysisError = errorReporter.newErrorWithProperties(
          StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_FOUR,
          node.getName(),
          stringMembersArray[0],
          stringMembersArray[1],
          stringMembersArray[2],
          stringMembersArray[3]);
    } else {
      analysisError = errorReporter.newErrorWithProperties(
          StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_FIVE_PLUS,
          node.getName(),
          stringMembersArray[0],
          stringMembersArray[1],
          stringMembersArray[2],
          stringMembersArray[3],
          stringMembersArray.length - 4);
    }
    analysisError.setProperty(ErrorProperty.UNIMPLEMENTED_METHODS, missingOverridesArray);
    errorReporter.reportError(analysisError);
    return true;
  }

  /**
   * Checks to ensure that the expressions that need to be of type bool, are. Otherwise an error is
   * reported on the expression.
   * 
   * @param condition the conditional expression to test
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticTypeWarningCode#NON_BOOL_CONDITION
   */
  private boolean checkForNonBoolCondition(Expression condition) {
    Type conditionType = getStaticType(condition);
    if (conditionType != null && !conditionType.isAssignableTo(boolType)) {
      errorReporter.reportError(StaticTypeWarningCode.NON_BOOL_CONDITION, condition);
      return true;
    }
    return false;
  }

  /**
   * This verifies that the passed assert statement has either a 'bool' or '() -> bool' input.
   * 
   * @param node the assert statement to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticTypeWarningCode#NON_BOOL_EXPRESSION
   */
  private boolean checkForNonBoolExpression(AssertStatement node) {
    Expression expression = node.getCondition();
    Type type = getStaticType(expression);
    if (type instanceof InterfaceType) {
      if (!type.isAssignableTo(boolType)) {
        errorReporter.reportError(StaticTypeWarningCode.NON_BOOL_EXPRESSION, expression);
        return true;
      }
    } else if (type instanceof FunctionType) {
      FunctionType functionType = (FunctionType) type;
      if (functionType.getTypeArguments().length == 0
          && !functionType.getReturnType().isAssignableTo(boolType)) {
        errorReporter.reportError(StaticTypeWarningCode.NON_BOOL_EXPRESSION, expression);
        return true;
      }
    }
    return false;
  }

  /**
   * Checks to ensure that the given expression is assignable to bool.
   * 
   * @param expression the expression expression to test
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticTypeWarningCode#NON_BOOL_NEGATION_EXPRESSION
   */
  private boolean checkForNonBoolNegationExpression(Expression expression) {
    Type conditionType = getStaticType(expression);
    if (conditionType != null && !conditionType.isAssignableTo(boolType)) {
      errorReporter.reportError(StaticTypeWarningCode.NON_BOOL_NEGATION_EXPRESSION, expression);
      return true;
    }
    return false;
  }

  /**
   * This verifies the passed map literal either:
   * <ul>
   * <li>has {@code const modifier}</li>
   * <li>has explicit type arguments</li>
   * <li>is not start of the statement</li>
   * <ul>
   * 
   * @param node the map literal to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#NON_CONST_MAP_AS_EXPRESSION_STATEMENT
   */
  private boolean checkForNonConstMapAsExpressionStatement(MapLiteral node) {
    // "const"
    if (node.getConstKeyword() != null) {
      return false;
    }
    // has type arguments
    if (node.getTypeArguments() != null) {
      return false;
    }
    // prepare statement
    Statement statement = node.getAncestor(ExpressionStatement.class);
    if (statement == null) {
      return false;
    }
    // OK, statement does not start with map
    if (statement.getBeginToken() != node.getBeginToken()) {
      return false;
    }
    // report problem
    errorReporter.reportError(CompileTimeErrorCode.NON_CONST_MAP_AS_EXPRESSION_STATEMENT, node);
    return true;
  }

  /**
   * This verifies the passed method declaration of operator {@code []=}, has {@code void} return
   * type.
   * 
   * @param node the method declaration to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticWarningCode#NON_VOID_RETURN_FOR_OPERATOR
   */
  private boolean checkForNonVoidReturnTypeForOperator(MethodDeclaration node) {
    // check that []= operator
    SimpleIdentifier name = node.getName();
    if (!name.getName().equals("[]=")) {
      return false;
    }
    // check return type
    TypeName typeName = node.getReturnType();
    if (typeName != null) {
      Type type = typeName.getType();
      if (type != null && !type.isVoid()) {
        errorReporter.reportError(StaticWarningCode.NON_VOID_RETURN_FOR_OPERATOR, typeName);
      }
    }
    // no warning
    return false;
  }

  /**
   * This verifies the passed setter has no return type or the {@code void} return type.
   * 
   * @param typeName the type name to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticWarningCode#NON_VOID_RETURN_FOR_SETTER
   */
  private boolean checkForNonVoidReturnTypeForSetter(TypeName typeName) {
    if (typeName != null) {
      Type type = typeName.getType();
      if (type != null && !type.isVoid()) {
        errorReporter.reportError(StaticWarningCode.NON_VOID_RETURN_FOR_SETTER, typeName);
      }
    }
    return false;
  }

  /**
   * This verifies the passed operator-method declaration, does not have an optional parameter.
   * <p>
   * This method assumes that the method declaration was tested to be an operator declaration before
   * being called.
   * 
   * @param node the method declaration to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#OPTIONAL_PARAMETER_IN_OPERATOR
   */
  private boolean checkForOptionalParameterInOperator(MethodDeclaration node) {
    FormalParameterList parameterList = node.getParameters();
    if (parameterList == null) {
      return false;
    }
    boolean foundError = false;
    NodeList<FormalParameter> formalParameters = parameterList.getParameters();
    for (FormalParameter formalParameter : formalParameters) {
      if (formalParameter.getKind().isOptional()) {
        errorReporter.reportError(
            CompileTimeErrorCode.OPTIONAL_PARAMETER_IN_OPERATOR,
            formalParameter);
        foundError = true;
      }
    }
    return foundError;
  }

  /**
   * This checks for named optional parameters that begin with '_'.
   * 
   * @param node the default formal parameter to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#PRIVATE_OPTIONAL_PARAMETER
   */
  private boolean checkForPrivateOptionalParameter(FormalParameter node) {
    // should be named parameter
    if (node.getKind() != ParameterKind.NAMED) {
      return false;
    }
    // name should start with '_'
    SimpleIdentifier name = node.getIdentifier();
    if (name.isSynthetic() || !name.getName().startsWith("_")) {
      return false;
    }
    // report problem
    errorReporter.reportError(CompileTimeErrorCode.PRIVATE_OPTIONAL_PARAMETER, node);
    return true;
  }

  /**
   * This checks if the passed constructor declaration is the redirecting generative constructor and
   * references itself directly or indirectly.
   * 
   * @param node the constructor declaration to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#RECURSIVE_CONSTRUCTOR_REDIRECT
   */
  private boolean checkForRecursiveConstructorRedirect(ConstructorDeclaration node) {
    // we check generative constructor here
    if (node.getFactoryKeyword() != null) {
      return false;
    }
    // try to find redirecting constructor invocation and analyzer it for recursion
    for (ConstructorInitializer initializer : node.getInitializers()) {
      if (initializer instanceof RedirectingConstructorInvocation) {
        // OK if no cycle
        ConstructorElement element = node.getElement();
        if (!hasRedirectingFactoryConstructorCycle(element)) {
          return false;
        }
        // report error
        errorReporter.reportError(CompileTimeErrorCode.RECURSIVE_CONSTRUCTOR_REDIRECT, initializer);
        return true;
      }
    }
    // OK, no redirecting constructor invocation
    return false;
  }

  /**
   * This checks if the passed constructor declaration has redirected constructor and references
   * itself directly or indirectly.
   * 
   * @param node the constructor declaration to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#RECURSIVE_FACTORY_REDIRECT
   */
  private boolean checkForRecursiveFactoryRedirect(ConstructorDeclaration node) {
    // prepare redirected constructor
    ConstructorName redirectedConstructorNode = node.getRedirectedConstructor();
    if (redirectedConstructorNode == null) {
      return false;
    }
    // OK if no cycle
    ConstructorElement element = node.getElement();
    if (!hasRedirectingFactoryConstructorCycle(element)) {
      return false;
    }
    // report error
    errorReporter.reportError(
        CompileTimeErrorCode.RECURSIVE_FACTORY_REDIRECT,
        redirectedConstructorNode);
    return true;
  }

  /**
   * This checks the class declaration is not a superinterface to itself.
   * 
   * @param classElt the class element to test
   * @return {@code true} if and only if an error code is generated on the passed element
   * @see CompileTimeErrorCode#RECURSIVE_INTERFACE_INHERITANCE
   * @see CompileTimeErrorCode#RECURSIVE_INTERFACE_INHERITANCE_BASE_CASE_EXTENDS
   * @see CompileTimeErrorCode#RECURSIVE_INTERFACE_INHERITANCE_BASE_CASE_IMPLEMENTS
   */
  private boolean checkForRecursiveInterfaceInheritance(ClassElement classElt) {
    if (classElt == null) {
      return false;
    }
    return checkForRecursiveInterfaceInheritance(classElt, new ArrayList<ClassElement>());
  }

  /**
   * This checks the class declaration is not a superinterface to itself.
   * 
   * @param classElt the class element to test
   * @param path a list containing the potentially cyclic implements path
   * @return {@code true} if and only if an error code is generated on the passed element
   * @see CompileTimeErrorCode#RECURSIVE_INTERFACE_INHERITANCE
   * @see CompileTimeErrorCode#RECURSIVE_INTERFACE_INHERITANCE_BASE_CASE_EXTENDS
   * @see CompileTimeErrorCode#RECURSIVE_INTERFACE_INHERITANCE_BASE_CASE_IMPLEMENTS
   */
  private boolean checkForRecursiveInterfaceInheritance(ClassElement classElt,
      ArrayList<ClassElement> path) {
    // Detect error condition.
    int size = path.size();
    // If this is not the base case (size > 0), and the enclosing class is the passed class
    // element then an error an error.
    if (size > 0 && enclosingClass.equals(classElt)) {
      String enclosingClassName = enclosingClass.getDisplayName();
      if (size > 1) {
        // Construct a string showing the cyclic implements path: "A, B, C, D, A"
        String separator = ", ";
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < size; i++) {
          builder.append(path.get(i).getDisplayName());
          builder.append(separator);
        }
        builder.append(classElt.getDisplayName());
        errorReporter.reportError(
            CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE,
            enclosingClass.getNameOffset(),
            enclosingClassName.length(),
            enclosingClassName,
            builder.toString());
        return true;
      } else { // size == 1
        // RECURSIVE_INTERFACE_INHERITANCE_BASE_CASE_IMPLEMENTS or RECURSIVE_INTERFACE_INHERITANCE_BASE_CASE_EXTENDS
        InterfaceType supertype = classElt.getSupertype();
        ErrorCode errorCode = supertype != null && enclosingClass.equals(supertype.getElement())
            ? CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE_BASE_CASE_EXTENDS
            : CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE_BASE_CASE_IMPLEMENTS;
        errorReporter.reportError(
            errorCode,
            enclosingClass.getNameOffset(),
            enclosingClassName.length(),
            enclosingClassName);
        return true;
      }
    }
    if (path.indexOf(classElt) > 0) {
      return false;
    }
    path.add(classElt);
    // n-case
    InterfaceType supertype = classElt.getSupertype();
    if (supertype != null && checkForRecursiveInterfaceInheritance(supertype.getElement(), path)) {
      return true;
    }
    InterfaceType[] interfaceTypes = classElt.getInterfaces();
    for (InterfaceType interfaceType : interfaceTypes) {
      if (checkForRecursiveInterfaceInheritance(interfaceType.getElement(), path)) {
        return true;
      }
    }
    path.remove(path.size() - 1);
    return false;
  }

  /**
   * This checks the passed constructor declaration has a valid combination of redirected
   * constructor invocation(s), super constructor invocations and field initializers.
   * 
   * @param node the constructor declaration to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#DEFAULT_VALUE_IN_REDIRECTING_FACTORY_CONSTRUCTOR
   * @see CompileTimeErrorCode#FIELD_INITIALIZER_REDIRECTING_CONSTRUCTOR
   * @see CompileTimeErrorCode#MULTIPLE_REDIRECTING_CONSTRUCTOR_INVOCATIONS
   * @see CompileTimeErrorCode#SUPER_IN_REDIRECTING_CONSTRUCTOR
   */
  private boolean checkForRedirectingConstructorErrorCodes(ConstructorDeclaration node) {
    boolean errorReported = false;
    //
    // Check for default values in the parameters
    //
    ConstructorName redirectedConstructor = node.getRedirectedConstructor();
    if (redirectedConstructor != null) {
      for (FormalParameter parameter : node.getParameters().getParameters()) {
        if (parameter instanceof DefaultFormalParameter
            && ((DefaultFormalParameter) parameter).getDefaultValue() != null) {
          errorReporter.reportError(
              CompileTimeErrorCode.DEFAULT_VALUE_IN_REDIRECTING_FACTORY_CONSTRUCTOR,
              parameter.getIdentifier());
          errorReported = true;
        }
      }
    }
    // check if there are redirected invocations
    int numRedirections = 0;
    for (ConstructorInitializer initializer : node.getInitializers()) {
      if (initializer instanceof RedirectingConstructorInvocation) {
        if (numRedirections > 0) {
          errorReporter.reportError(
              CompileTimeErrorCode.MULTIPLE_REDIRECTING_CONSTRUCTOR_INVOCATIONS,
              initializer);
          errorReported = true;
        }
        numRedirections++;
      }
    }
    // check for other initializers
    if (numRedirections > 0) {
      for (ConstructorInitializer initializer : node.getInitializers()) {
        if (initializer instanceof SuperConstructorInvocation) {
          errorReporter.reportError(
              CompileTimeErrorCode.SUPER_IN_REDIRECTING_CONSTRUCTOR,
              initializer);
          errorReported = true;
        }
        if (initializer instanceof ConstructorFieldInitializer) {
          errorReporter.reportError(
              CompileTimeErrorCode.FIELD_INITIALIZER_REDIRECTING_CONSTRUCTOR,
              initializer);
          errorReported = true;
        }
      }
    }
    // done
    return errorReported;
  }

  /**
   * This checks if the passed constructor declaration has redirected constructor and references
   * itself directly or indirectly.
   * 
   * @param node the constructor declaration to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#REDIRECT_TO_NON_CONST_CONSTRUCTOR
   */
  private boolean checkForRedirectToNonConstConstructor(ConstructorDeclaration node) {
    // prepare redirected constructor
    ConstructorName redirectedConstructorNode = node.getRedirectedConstructor();
    if (redirectedConstructorNode == null) {
      return false;
    }
    // prepare element
    ConstructorElement element = node.getElement();
    if (element == null) {
      return false;
    }
    // OK, it is not 'const'
    if (!element.isConst()) {
      return false;
    }
    // prepare redirected constructor
    ConstructorElement redirectedConstructor = element.getRedirectedConstructor();
    if (redirectedConstructor == null) {
      return false;
    }
    // OK, it is also 'const'
    if (redirectedConstructor.isConst()) {
      return false;
    }
    // report error
    errorReporter.reportError(
        CompileTimeErrorCode.REDIRECT_TO_NON_CONST_CONSTRUCTOR,
        redirectedConstructorNode);
    return true;
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
   * This checks that if the the given constructor declaration is generative, then it does not have
   * an expression function body.
   * 
   * @param node the constructor to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#RETURN_IN_GENERATIVE_CONSTRUCTOR
   */
  private boolean checkForReturnInGenerativeConstructor(ConstructorDeclaration node) {
    // ignore factory
    if (node.getFactoryKeyword() != null) {
      return false;
    }
    // block body (with possible return statement) is checked elsewhere
    FunctionBody body = node.getBody();
    if (!(body instanceof ExpressionFunctionBody)) {
      return false;
    }
    // report error
    errorReporter.reportError(CompileTimeErrorCode.RETURN_IN_GENERATIVE_CONSTRUCTOR, body);
    return true;
  }

  /**
   * This checks that a type mis-match between the return type and the expressed return type by the
   * enclosing method or function.
   * <p>
   * This method is called both by {@link #checkForAllReturnStatementErrorCodes(ReturnStatement)}
   * and {@link #visitExpressionFunctionBody(ExpressionFunctionBody)}.
   * 
   * @param returnExpression the returned expression to evaluate
   * @param expectedReturnType the expressed return type by the enclosing method or function
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticTypeWarningCode#RETURN_OF_INVALID_TYPE
   */
  private boolean checkForReturnOfInvalidType(Expression returnExpression, Type expectedReturnType) {
    Type staticReturnType = getStaticType(returnExpression);
    if (expectedReturnType.isVoid()) {
      if (staticReturnType.isVoid() || staticReturnType.isDynamic() || staticReturnType.isBottom()) {
        return false;
      }
      errorReporter.reportError(
          StaticTypeWarningCode.RETURN_OF_INVALID_TYPE,
          returnExpression,
          staticReturnType.getDisplayName(),
          expectedReturnType.getDisplayName(),
          enclosingFunction.getDisplayName());
      return true;
    }
    boolean isStaticAssignable = staticReturnType.isAssignableTo(expectedReturnType);
    Type propagatedReturnType = returnExpression.getPropagatedType();
    if (strictMode || propagatedReturnType == null) {
      if (isStaticAssignable) {
        return false;
      }
      errorReporter.reportError(
          StaticTypeWarningCode.RETURN_OF_INVALID_TYPE,
          returnExpression,
          staticReturnType.getDisplayName(),
          expectedReturnType.getDisplayName(),
          enclosingFunction.getDisplayName());
      return true;
    } else {
      boolean isPropagatedAssignable = propagatedReturnType.isAssignableTo(expectedReturnType);
      if (isStaticAssignable || isPropagatedAssignable) {
        return false;
      }
      errorReporter.reportError(
          StaticTypeWarningCode.RETURN_OF_INVALID_TYPE,
          returnExpression,
          staticReturnType.getDisplayName(),
          expectedReturnType.getDisplayName(),
          enclosingFunction.getDisplayName());
      return true;
    }
  }

  /**
   * This checks that if the given "target" is the type reference then the "name" is not the
   * reference to a instance member.
   * 
   * @param target the target of the name access to evaluate
   * @param name the accessed name to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticWarningCode#STATIC_ACCESS_TO_INSTANCE_MEMBER
   */
  private boolean checkForStaticAccessToInstanceMember(Expression target, SimpleIdentifier name) {
    // prepare member Element
    Element element = name.getStaticElement();
    if (!(element instanceof ExecutableElement)) {
      return false;
    }
    ExecutableElement memberElement = (ExecutableElement) element;
    // OK, static
    if (memberElement.isStatic()) {
      return false;
    }
    // OK, target is not a type
    if (!isTypeReference(target)) {
      return false;
    }
    // report problem
    errorReporter.reportError(
        StaticWarningCode.STATIC_ACCESS_TO_INSTANCE_MEMBER,
        name,
        name.getName());
    return true;
  }

  /**
   * This checks that the type of the passed 'switch' expression is assignable to the type of the
   * 'case' members.
   * 
   * @param node the 'switch' statement to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticWarningCode#SWITCH_EXPRESSION_NOT_ASSIGNABLE
   */
  private boolean checkForSwitchExpressionNotAssignable(SwitchStatement node) {
    // prepare 'switch' expression type
    Expression expression = node.getExpression();
    Type expressionType = getStaticType(expression);
    if (expressionType == null) {
      return false;
    }
    // compare with type of the first 'case'
    NodeList<SwitchMember> members = node.getMembers();
    for (SwitchMember switchMember : members) {
      if (!(switchMember instanceof SwitchCase)) {
        continue;
      }
      SwitchCase switchCase = (SwitchCase) switchMember;
      // prepare 'case' type
      Expression caseExpression = switchCase.getExpression();
      Type caseType = getStaticType(caseExpression);
      // check types
      if (expressionType.isAssignableTo(caseType)) {
        return false;
      }
      // report problem
      errorReporter.reportError(
          StaticWarningCode.SWITCH_EXPRESSION_NOT_ASSIGNABLE,
          expression,
          expressionType,
          caseType);
      return true;
    }
    return false;
  }

  /**
   * This verifies that the passed function type alias does not reference itself directly.
   * 
   * @param node the function type alias to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#TYPE_ALIAS_CANNOT_REFERENCE_ITSELF
   */
  private boolean checkForTypeAliasCannotReferenceItself_function(FunctionTypeAlias node) {
    FunctionTypeAliasElement element = node.getElement();
    if (!hasTypedefSelfReference(element)) {
      return false;
    }
    errorReporter.reportError(CompileTimeErrorCode.TYPE_ALIAS_CANNOT_REFERENCE_ITSELF, node);
    return true;
  }

  /**
   * This verifies that the given class type alias does not reference itself.
   * 
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#TYPE_ALIAS_CANNOT_REFERENCE_ITSELF
   */
  private boolean checkForTypeAliasCannotReferenceItself_mixin(ClassTypeAlias node) {
    ClassElement element = node.getElement();
    if (!hasTypedefSelfReference(element)) {
      return false;
    }
    errorReporter.reportError(CompileTimeErrorCode.TYPE_ALIAS_CANNOT_REFERENCE_ITSELF, node);
    return true;
  }

  /**
   * This verifies that the type arguments in the passed type name are all within their bounds.
   * 
   * @param node the {@link TypeName} to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticTypeWarningCode#TYPE_ARGUMENT_NOT_MATCHING_BOUNDS
   */
  private boolean checkForTypeArgumentNotMatchingBounds(TypeName node) {
    if (node.getTypeArguments() == null) {
      return false;
    }
    // prepare Type
    Type type = node.getType();
    if (type == null) {
      return false;
    }
    // prepare ClassElement
    Element element = type.getElement();
    if (!(element instanceof ClassElement)) {
      return false;
    }
    ClassElement classElement = (ClassElement) element;
    // prepare type parameters
    Type[] typeParameters = classElement.getType().getTypeArguments();
    TypeParameterElement[] boundingElts = classElement.getTypeParameters();
    // iterate over each bounded type parameter and corresponding argument
    NodeList<TypeName> typeNameArgList = node.getTypeArguments().getArguments();
    Type[] typeArguments = ((InterfaceType) type).getTypeArguments();
    int loopThroughIndex = Math.min(typeNameArgList.size(), boundingElts.length);
    boolean foundError = false;
    for (int i = 0; i < loopThroughIndex; i++) {
      TypeName argTypeName = typeNameArgList.get(i);
      Type argType = argTypeName.getType();
      Type boundType = boundingElts[i].getBound();
      if (argType != null && boundType != null) {
        boundType = boundType.substitute(typeArguments, typeParameters);
        if (!argType.isSubtypeOf(boundType)) {
          ErrorCode errorCode;
          if (isInConstConstructorInvocation(node)) {
            errorCode = CompileTimeErrorCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS;
          } else {
            errorCode = StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS;
          }
          errorReporter.reportError(
              errorCode,
              argTypeName,
              argType.getDisplayName(),
              boundType.getDisplayName());
          foundError = true;
        }
      }
    }
    return foundError;
  }

  /**
   * This checks that if the passed type name is a type parameter being used to define a static
   * member.
   * 
   * @param node the type name to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticWarningCode#TYPE_PARAMETER_REFERENCED_BY_STATIC
   */
  private boolean checkForTypeParameterReferencedByStatic(TypeName node) {
    if (isInStaticMethod || isInStaticVariableDeclaration) {
      Type type = node.getType();
      if (type instanceof TypeParameterType) {
        errorReporter.reportError(StaticWarningCode.TYPE_PARAMETER_REFERENCED_BY_STATIC, node);
        return true;
      }
    }
    return false;
  }

  /**
   * This checks that if the passed type parameter is a supertype of its bound.
   * 
   * @param node the type parameter to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticTypeWarningCode#TYPE_PARAMETER_SUPERTYPE_OF_ITS_BOUND
   */
  private boolean checkForTypeParameterSupertypeOfItsBound(TypeParameter node) {
    TypeParameterElement element = node.getElement();
    // prepare bound
    Type bound = element.getBound();
    if (bound == null) {
      return false;
    }
    // OK, type parameter is not supertype of its bound
    if (!bound.isMoreSpecificThan(element.getType())) {
      return false;
    }
    // report problem
    errorReporter.reportError(
        StaticTypeWarningCode.TYPE_PARAMETER_SUPERTYPE_OF_ITS_BOUND,
        node,
        element.getDisplayName());
    return true;
  }

  /**
   * This checks that if the passed generative constructor has neither an explicit super constructor
   * invocation nor a redirecting constructor invocation, that the superclass has a default
   * generative constructor.
   * 
   * @param node the constructor declaration to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#UNDEFINED_CONSTRUCTOR_IN_INITIALIZER_DEFAULT
   * @see CompileTimeErrorCode#NON_GENERATIVE_CONSTRUCTOR
   * @see StaticWarningCode#NO_DEFAULT_SUPER_CONSTRUCTOR_EXPLICIT
   */
  private boolean checkForUndefinedConstructorInInitializerImplicit(ConstructorDeclaration node) {
    //
    // Ignore if the constructor is not generative.
    //
    if (node.getFactoryKeyword() != null) {
      return false;
    }
    //
    // Ignore if the constructor has either an implicit super constructor invocation or a
    // redirecting constructor invocation.
    //
    for (ConstructorInitializer constructorInitializer : node.getInitializers()) {
      if (constructorInitializer instanceof SuperConstructorInvocation
          || constructorInitializer instanceof RedirectingConstructorInvocation) {
        return false;
      }
    }
    //
    // Check to see whether the superclass has a non-factory unnamed constructor.
    //
    if (enclosingClass == null) {
      return false;
    }
    InterfaceType superType = enclosingClass.getSupertype();
    if (superType == null) {
      return false;
    }
    ClassElement superElement = superType.getElement();
    ConstructorElement superUnnamedConstructor = superElement.getUnnamedConstructor();
    if (superUnnamedConstructor != null) {
      if (superUnnamedConstructor.isFactory()) {
        errorReporter.reportError(
            CompileTimeErrorCode.NON_GENERATIVE_CONSTRUCTOR,
            node.getReturnType(),
            superUnnamedConstructor);
        return true;
      }
      if (!superUnnamedConstructor.isDefaultConstructor()) {
        int offset;
        int length;
        {
          Identifier returnType = node.getReturnType();
          SimpleIdentifier name = node.getName();
          offset = returnType.getOffset();
          length = (name != null ? name.getEnd() : returnType.getEnd()) - offset;
        }
        errorReporter.reportError(
            CompileTimeErrorCode.NO_DEFAULT_SUPER_CONSTRUCTOR_EXPLICIT,
            offset,
            length,
            superType.getDisplayName());
      }
      return false;
    }
    errorReporter.reportError(
        CompileTimeErrorCode.UNDEFINED_CONSTRUCTOR_IN_INITIALIZER_DEFAULT,
        node.getReturnType(),
        superElement.getName());
    return true;
  }

  /**
   * This checks that if the given name is a reference to a static member it is defined in the
   * enclosing class rather than in a superclass.
   * 
   * @param name the name to be evaluated
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticTypeWarningCode#UNQUALIFIED_REFERENCE_TO_NON_LOCAL_STATIC_MEMBER
   */
  private boolean checkForUnqualifiedReferenceToNonLocalStaticMember(SimpleIdentifier name) {
    Element element = name.getStaticElement();
    if (element == null || element instanceof TypeParameterElement) {
      return false;
    }
    Element enclosingElement = element.getEnclosingElement();
    if (!(enclosingElement instanceof ClassElement)) {
      return false;
    }
    if ((element instanceof MethodElement && !((MethodElement) element).isStatic())
        || (element instanceof PropertyAccessorElement && !((PropertyAccessorElement) element).isStatic())) {
      return false;
    }
    if (enclosingElement == enclosingClass) {
      return false;
    }
    errorReporter.reportError(
        StaticTypeWarningCode.UNQUALIFIED_REFERENCE_TO_NON_LOCAL_STATIC_MEMBER,
        name,
        name.getName());
    return true;
  }

  /**
   * This verifies the passed operator-method declaration, has correct number of parameters.
   * <p>
   * This method assumes that the method declaration was tested to be an operator declaration before
   * being called.
   * 
   * @param node the method declaration to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#WRONG_NUMBER_OF_PARAMETERS_FOR_OPERATOR
   */
  private boolean checkForWrongNumberOfParametersForOperator(MethodDeclaration node) {
    // prepare number of parameters
    FormalParameterList parameterList = node.getParameters();
    if (parameterList == null) {
      return false;
    }
    int numParameters = parameterList.getParameters().size();
    // prepare operator name
    SimpleIdentifier nameNode = node.getName();
    if (nameNode == null) {
      return false;
    }
    String name = nameNode.getName();
    // check for exact number of parameters
    int expected = -1;
    if ("[]=".equals(name)) {
      expected = 2;
    } else if ("<".equals(name) || ">".equals(name) || "<=".equals(name) || ">=".equals(name)
        || "==".equals(name) || "+".equals(name) || "/".equals(name) || "~/".equals(name)
        || "*".equals(name) || "%".equals(name) || "|".equals(name) || "^".equals(name)
        || "&".equals(name) || "<<".equals(name) || ">>".equals(name) || "[]".equals(name)) {
      expected = 1;
    } else if ("~".equals(name)) {
      expected = 0;
    }
    if (expected != -1 && numParameters != expected) {
      errorReporter.reportError(
          CompileTimeErrorCode.WRONG_NUMBER_OF_PARAMETERS_FOR_OPERATOR,
          nameNode,
          name,
          expected,
          numParameters);
      return true;
    }
    // check for operator "-"
    if ("-".equals(name) && numParameters > 1) {
      errorReporter.reportError(
          CompileTimeErrorCode.WRONG_NUMBER_OF_PARAMETERS_FOR_OPERATOR_MINUS,
          nameNode,
          numParameters);
      return true;
    }
    // OK
    return false;
  }

  /**
   * This verifies if the passed setter parameter list have only one required parameter.
   * <p>
   * This method assumes that the method declaration was tested to be a setter before being called.
   * 
   * @param setterName the name of the setter to report problems on
   * @param parameterList the parameter list to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#WRONG_NUMBER_OF_PARAMETERS_FOR_SETTER
   */
  private boolean checkForWrongNumberOfParametersForSetter(SimpleIdentifier setterName,
      FormalParameterList parameterList) {
    if (setterName == null) {
      return false;
    }
    if (parameterList == null) {
      return false;
    }
    NodeList<FormalParameter> parameters = parameterList.getParameters();
    if (parameters.size() != 1 || parameters.get(0).getKind() != ParameterKind.REQUIRED) {
      errorReporter.reportError(
          CompileTimeErrorCode.WRONG_NUMBER_OF_PARAMETERS_FOR_SETTER,
          setterName);
      return true;
    }
    return false;
  }

  /**
   * This verifies that if the given class declaration implements the class Function that it has a
   * concrete implementation of the call method.
   * 
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticWarningCode#FUNCTION_WITHOUT_CALL
   */
  private boolean checkImplementsFunctionWithoutCall(ClassDeclaration node) {
    if (node.getAbstractKeyword() != null) {
      return false;
    }
    ClassElement classElement = node.getElement();
    if (classElement == null) {
      return false;
    }
    if (!classElement.getType().isSubtypeOf(typeProvider.getFunctionType())) {
      return false;
    }
    ExecutableElement callMethod = inheritanceManager.lookupMember(classElement, "call");
    if (callMethod == null || !(callMethod instanceof MethodElement)
        || ((MethodElement) callMethod).isAbstract()) {
      errorReporter.reportError(StaticWarningCode.FUNCTION_WITHOUT_CALL, node.getName());
      return true;
    }
    return false;
  }

  /**
   * This verifies that the given class declaration does not have the same class in the 'extends'
   * and 'implements' clauses.
   * 
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#IMPLEMENTS_SUPER_CLASS
   */
  private boolean checkImplementsSuperClass(ClassDeclaration node) {
    // prepare super type
    InterfaceType superType = enclosingClass.getSupertype();
    if (superType == null) {
      return false;
    }
    // prepare interfaces
    ImplementsClause implementsClause = node.getImplementsClause();
    if (implementsClause == null) {
      return false;
    }
    // check interfaces
    boolean hasProblem = false;
    for (TypeName interfaceNode : implementsClause.getInterfaces()) {
      if (interfaceNode.getType().equals(superType)) {
        hasProblem = true;
        errorReporter.reportError(
            CompileTimeErrorCode.IMPLEMENTS_SUPER_CLASS,
            interfaceNode,
            superType.getDisplayName());
      }
    }
    // done
    return hasProblem;
  }

  /**
   * Returns the Type (return type) for a given getter.
   * 
   * @param propertyAccessorElement
   * @return The type of the given getter.
   */
  private Type getGetterType(PropertyAccessorElement propertyAccessorElement) {
    FunctionType functionType = propertyAccessorElement.getType();
    if (functionType != null) {
      return functionType.getReturnType();
    } else {
      return null;
    }
  }

  /**
   * Returns the Type (first and only parameter) for a given setter.
   * 
   * @param propertyAccessorElement
   * @return The type of the given setter.
   */
  private Type getSetterType(PropertyAccessorElement propertyAccessorElement) {
    // Get the parameters for MethodDeclaration or FunctionDeclaration
    ParameterElement[] setterParameters = propertyAccessorElement.getParameters();

    // If there are no setter parameters, return no type.
    if (setterParameters.length == 0) {
      return null;
    }
    return setterParameters[0].getType();
  }

  /**
   * Return the static type of the given expression that is to be used for type analysis.
   * 
   * @param expression the expression whose type is to be returned
   * @return the static type of the given expression
   */
  private Type getStaticType(Expression expression) {
    Type type = expression.getStaticType();
    if (type == null) {
      // TODO(brianwilkerson) This should never happen.
      return dynamicType;
    }
    return type;
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
      Element element = ((Identifier) expression).getStaticElement();
      if (element instanceof VariableElement) {
        return (VariableElement) element;
      }
    }
    return null;
  }

  /**
   * @return {@code true} if the given constructor redirects to itself, directly or indirectly
   */
  private boolean hasRedirectingFactoryConstructorCycle(ConstructorElement element) {
    Set<ConstructorElement> constructors = new HashSet<ConstructorElement>();
    ConstructorElement current = element;
    while (current != null) {
      if (constructors.contains(current)) {
        return current == element;
      }
      constructors.add(current);
      current = current.getRedirectedConstructor();
      if (current instanceof ConstructorMember) {
        current = ((ConstructorMember) current).getBaseElement();
      }
    }
    return false;
  }

  /**
   * @return <code>true</code> if given {@link Element} has direct or indirect reference to itself
   *         from anywhere except {@link ClassElement} or type parameter bounds.
   */
  private boolean hasTypedefSelfReference(final Element target) {
    final Set<Element> checked = new HashSet<Element>();
    final List<Element> toCheck = new ArrayList<Element>();
    toCheck.add(target);
    boolean firstIteration = true;
    while (true) {
      Element current;
      // get next element
      while (true) {
        // may be no more elements to check
        if (toCheck.isEmpty()) {
          return false;
        }
        // try to get next element
        current = toCheck.remove(toCheck.size() - 1);
        if (target.equals(current)) {
          if (firstIteration) {
            firstIteration = false;
            break;
          } else {
            return true;
          }
        }
        if (current != null && !checked.contains(current)) {
          break;
        }
      }
      // check current element
      current.accept(new GeneralizingElementVisitor<Void>() {
        private boolean inClass;

        @Override
        public Void visitClassElement(ClassElement element) {
          addTypeToCheck(element.getSupertype());
          for (InterfaceType mixin : element.getMixins()) {
            addTypeToCheck(mixin);
          }
          inClass = !element.isTypedef();
          try {
            return super.visitClassElement(element);
          } finally {
            inClass = false;
          }
        }

        @Override
        public Void visitExecutableElement(ExecutableElement element) {
          if (element.isSynthetic()) {
            return null;
          }
          addTypeToCheck(element.getReturnType());
          return super.visitExecutableElement(element);
        }

        @Override
        public Void visitFunctionTypeAliasElement(FunctionTypeAliasElement element) {
          addTypeToCheck(element.getReturnType());
          return super.visitFunctionTypeAliasElement(element);
        }

        @Override
        public Void visitParameterElement(ParameterElement element) {
          addTypeToCheck(element.getType());
          return super.visitParameterElement(element);
        }

        @Override
        public Void visitTypeParameterElement(TypeParameterElement element) {
          addTypeToCheck(element.getBound());
          return super.visitTypeParameterElement(element);
        }

        @Override
        public Void visitVariableElement(VariableElement element) {
          addTypeToCheck(element.getType());
          return super.visitVariableElement(element);
        }

        private void addTypeToCheck(Type type) {
          if (type == null) {
            return;
          }
          Element element = type.getElement();
          // it is OK to reference target from class
          if (inClass && target.equals(element)) {
            return;
          }
          // schedule for checking
          toCheck.add(element);
          // type arguments
          if (type instanceof InterfaceType) {
            InterfaceType interfaceType = (InterfaceType) type;
            for (Type typeArgument : interfaceType.getTypeArguments()) {
              addTypeToCheck(typeArgument);
            }
          }
        }
      });
      checked.add(current);
    }
  }

  /**
   * @return {@code true} if given {@link Type} implements operator <i>==</i>, and it is not
   *         <i>int</i> or <i>String</i>.
   */
  private boolean implementsEqualsWhenNotAllowed(Type type) {
    // ignore int or String
    if (type == null || type.equals(typeProvider.getIntType())
        || type.equals(typeProvider.getStringType())) {
      return false;
    }
    // prepare ClassElement
    Element element = type.getElement();
    if (!(element instanceof ClassElement)) {
      return false;
    }
    ClassElement classElement = (ClassElement) element;
    // lookup for ==
    MethodElement method = classElement.lookUpMethod("==", currentLibrary);
    if (method == null || method.getEnclosingElement().getType().isObject()) {
      return false;
    }
    // there is == that we don't like
    return true;
  }

  private boolean isFunctionType(Type type) {
    if (type.isDynamic() || type.isBottom()) {
      return true;
    } else if (type instanceof FunctionType || type.isDartCoreFunction()) {
      return true;
    } else if (type instanceof InterfaceType) {
      MethodElement callMethod = ((InterfaceType) type).lookUpMethod(
          ElementResolver.CALL_METHOD_NAME,
          currentLibrary);
      return callMethod != null;
    }
    return false;
  }

  /**
   * @return {@code true} if the given {@link ASTNode} is the part of constant constructor
   *         invocation.
   */
  private boolean isInConstConstructorInvocation(ASTNode node) {
    InstanceCreationExpression creation = node.getAncestor(InstanceCreationExpression.class);
    if (creation == null) {
      return false;
    }
    return creation.isConst();
  }

  /**
   * @param node the 'this' expression to analyze
   * @return {@code true} if the given 'this' expression is in the valid context
   */
  private boolean isThisInValidContext(ThisExpression node) {
    for (ASTNode n = node; n != null; n = n.getParent()) {
      if (n instanceof CompilationUnit) {
        return false;
      }
      if (n instanceof ConstructorDeclaration) {
        ConstructorDeclaration constructor = (ConstructorDeclaration) n;
        return constructor.getFactoryKeyword() == null;
      }
      if (n instanceof ConstructorInitializer) {
        return false;
      }
      if (n instanceof MethodDeclaration) {
        MethodDeclaration method = (MethodDeclaration) n;
        return !method.isStatic();
      }
    }
    return false;
  }

  /**
   * Return {@code true} if the given identifier is in a location where it is allowed to resolve to
   * a static member of a supertype.
   * 
   * @param node the node being tested
   * @return {@code true} if the given identifier is in a location where it is allowed to resolve to
   *         a static member of a supertype
   */
  private boolean isUnqualifiedReferenceToNonLocalStaticMemberAllowed(SimpleIdentifier node) {
    if (node.inDeclarationContext()) {
      return true;
    }
    ASTNode parent = node.getParent();
    if (parent instanceof ConstructorName || parent instanceof MethodInvocation
        || parent instanceof PropertyAccess || parent instanceof SuperConstructorInvocation) {
      return true;
    }
    if (parent instanceof PrefixedIdentifier
        && ((PrefixedIdentifier) parent).getIdentifier() == node) {
      return true;
    }
    if (parent instanceof Annotation && ((Annotation) parent).getConstructorName() == node) {
      return true;
    }
    if (parent instanceof CommentReference) {
      CommentReference commentReference = (CommentReference) parent;
      if (commentReference.getNewKeyword() != null) {
        return true;
      }
    }
    return false;
  }

  /**
   * Return {@code true} iff the passed {@link ClassElement} has a concrete implementation of the
   * passed accessor name in the superclass chain.
   */
  private boolean memberHasConcreteAccessorImplementationInSuperclassChain(
      ClassElement classElement, String accessorName, ArrayList<ClassElement> superclassChain) {
    if (superclassChain.contains(classElement)) {
      return false;
    } else {
      superclassChain.add(classElement);
    }
    for (PropertyAccessorElement accessor : classElement.getAccessors()) {
      if (accessor.getName().equals(accessorName)) {
        if (!accessor.isAbstract()) {
          return true;
        }
      }
    }
    for (InterfaceType mixinType : classElement.getMixins()) {
      if (mixinType != null) {
        ClassElement mixinElement = mixinType.getElement();
        if (mixinElement != null) {
          for (PropertyAccessorElement accessor : mixinElement.getAccessors()) {
            if (accessor.getName().equals(accessorName)) {
              if (!accessor.isAbstract()) {
                return true;
              }
            }
          }
        }
      }
    }
    InterfaceType superType = classElement.getSupertype();
    if (superType != null) {
      ClassElement superClassElt = superType.getElement();
      if (superClassElt != null) {
        return memberHasConcreteAccessorImplementationInSuperclassChain(
            superClassElt,
            accessorName,
            superclassChain);
      }
    }
    return false;
  }

  /**
   * Return {@code true} iff the passed {@link ClassElement} has a concrete implementation of the
   * passed method name in the superclass chain.
   */
  private boolean memberHasConcreteMethodImplementationInSuperclassChain(ClassElement classElement,
      String methodName, ArrayList<ClassElement> superclassChain) {
    if (superclassChain.contains(classElement)) {
      return false;
    } else {
      superclassChain.add(classElement);
    }
    for (MethodElement method : classElement.getMethods()) {
      if (method.getName().equals(methodName)) {
        if (!method.isAbstract()) {
          return true;
        }
      }
    }
    for (InterfaceType mixinType : classElement.getMixins()) {
      if (mixinType != null) {
        ClassElement mixinElement = mixinType.getElement();
        if (mixinElement != null) {
          for (MethodElement method : mixinElement.getMethods()) {
            if (method.getName().equals(methodName)) {
              if (!method.isAbstract()) {
                return true;
              }
            }
          }
        }
      }
    }
    InterfaceType superType = classElement.getSupertype();
    if (superType != null) {
      ClassElement superClassElt = superType.getElement();
      if (superClassElt != null) {
        return memberHasConcreteMethodImplementationInSuperclassChain(
            superClassElt,
            methodName,
            superclassChain);
      }
    }
    return false;
  }
}
