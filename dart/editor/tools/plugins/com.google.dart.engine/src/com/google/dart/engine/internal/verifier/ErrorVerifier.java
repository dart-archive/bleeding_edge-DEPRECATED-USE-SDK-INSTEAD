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
import com.google.dart.engine.ast.ArgumentList;
import com.google.dart.engine.ast.AssertStatement;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.BreakStatement;
import com.google.dart.engine.ast.CatchClause;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassMember;
import com.google.dart.engine.ast.ClassTypeAlias;
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
import com.google.dart.engine.ast.ExpressionStatement;
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
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.IndexExpression;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.ListLiteral;
import com.google.dart.engine.ast.MapLiteral;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NativeFunctionBody;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.NormalFormalParameter;
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
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.TypeVariableElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.StaticTypeWarningCode;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.internal.element.FieldFormalParameterElementImpl;
import com.google.dart.engine.internal.element.member.ConstructorMember;
import com.google.dart.engine.internal.error.ErrorReporter;
import com.google.dart.engine.internal.resolver.InheritanceManager;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.internal.scope.Namespace;
import com.google.dart.engine.internal.scope.NamespaceBuilder;
import com.google.dart.engine.internal.type.BottomTypeImpl;
import com.google.dart.engine.internal.type.DynamicTypeImpl;
import com.google.dart.engine.internal.type.VoidTypeImpl;
import com.google.dart.engine.parser.ParserErrorCode;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.type.TypeVariableType;
import com.google.dart.engine.utilities.dart.ParameterKind;
import com.google.dart.engine.utilities.general.ObjectUtilities;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

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
   * Checks if the given expression is the reference to the type.
   * 
   * @param expr the expression to evaluate
   * @return {@code true} if the given expression is the reference to the type
   */
  private static boolean isTypeReference(Expression expr) {
    if (expr instanceof Identifier) {
      Identifier identifier = (Identifier) expr;
      return identifier.getElement() instanceof ClassElement;
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
   * The object providing access to the types defined by the language.
   */
  private TypeProvider typeProvider;

  /**
   * The manager for the inheritance mappings.
   */
  private InheritanceManager inheritanceManager;

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
   * This is set to {@code true} iff the visitor is currently visiting a
   * {@link ConstructorInitializer}.
   */
  private boolean isInConstructorInitializer;

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

  /**
   * A hash of all of the compilation unit accessors for use in discovering their counterparts.
   */
  private HashMap<String, FunctionDeclaration> compilationUnitAccessors;

  /**
   * Analysis information for classes (withs support for nested classes).
   */
  Stack<ClassAccessorInformation> classesAnalysisInformation = new Stack<ClassAccessorInformation>();

  /**
   * Analysis information for the class currently being visited.
   */
  private ClassAccessorInformation currentClassInformation;

  public ErrorVerifier(ErrorReporter errorReporter, LibraryElement currentLibrary,
      TypeProvider typeProvider, InheritanceManager inheritanceManager) {
    this.errorReporter = errorReporter;
    this.currentLibrary = currentLibrary;
    this.isInSystemLibrary = currentLibrary.getSource().isInSystemLibrary();
    this.typeProvider = typeProvider;
    this.inheritanceManager = inheritanceManager;
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
    return super.visitAssignmentExpression(node);
  }

  @Override
  public Void visitBinaryExpression(BinaryExpression node) {
    checkForArgumentTypeNotAssignable(node.getRightOperand());
    return super.visitBinaryExpression(node);
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

    // Make a reference to the class currently being visited, and push to the class stack.
    currentClassInformation = new ClassAccessorInformation();
    classesAnalysisInformation.push(currentClassInformation);
    try {
      enclosingClass = node.getElement();
      checkForBuiltInIdentifierAsName(
          node.getName(),
          CompileTimeErrorCode.BUILT_IN_IDENTIFIER_AS_TYPE_NAME);
      checkForMemberWithClassName();
      checkForAllMixinErrorCodes(node.getWithClause());
      if (!checkForImplementsDisallowedClass(node.getImplementsClause())
          && !checkForExtendsDisallowedClass(node.getExtendsClause())) {
        checkForNonAbstractClassInheritsAbstractMember(node);
        checkForInconsistentMethodInheritance();
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
      return super.visitClassDeclaration(node);
    } finally {
      initialFieldElementsMap = null;
      enclosingClass = outerClass;
      classesAnalysisInformation.pop();
    }
  }

  @Override
  public Void visitClassTypeAlias(ClassTypeAlias node) {
    checkForBuiltInIdentifierAsName(
        node.getName(),
        CompileTimeErrorCode.BUILT_IN_IDENTIFIER_AS_TYPEDEF_NAME);
    checkForAllMixinErrorCodes(node.getWithClause());
    return super.visitClassTypeAlias(node);
  }

  @Override
  public Void visitCompilationUnit(CompilationUnit node) {
    // Initialize a new HashMap of getters and setters for the new compilation unit.
    compilationUnitAccessors = new HashMap<String, FunctionDeclaration>();
    return super.visitCompilationUnit(node);
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
      checkForRedirectingConstructorErrorCodes(node);
      checkForMultipleSuperInitializers(node);
      checkForRecursiveConstructorRedirect(node);
      checkForRecursiveFactoryRedirect(node);
      checkForRedirectToInvalidFunction(node);
      checkForUndefinedConstructorInInitializerImplicit(node);
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
      return super.visitConstructorFieldInitializer(node);
    } finally {
      isInConstructorInitializer = false;
    }
  }

  @Override
  public Void visitDefaultFormalParameter(DefaultFormalParameter node) {
    checkForPrivateOptionalParameter(node);
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
    return super.visitExportDirective(node);
  }

  @Override
  public Void visitFieldFormalParameter(FieldFormalParameter node) {
    checkForConstFormalParameter(node);
    checkForFieldInitializingFormalRedirectingConstructor(node);
    return super.visitFieldFormalParameter(node);
  }

  @Override
  public Void visitFunctionDeclaration(FunctionDeclaration node) {
    ExecutableElement outerFunction = enclosingFunction;
    try {
      enclosingFunction = node.getElement();
      if (node.isSetter() || node.isGetter()) {
        checkForMismatchedAccessorTypes(node);
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
  public Void visitImportDirective(ImportDirective node) {
    checkForImportDuplicateLibraryName(node);
    return super.visitImportDirective(node);
  }

  @Override
  public Void visitIndexExpression(IndexExpression node) {
    checkForArgumentTypeNotAssignable(node.getIndex());
    return super.visitIndexExpression(node);
  }

  @Override
  public Void visitInstanceCreationExpression(InstanceCreationExpression node) {
    ConstructorName constructorName = node.getConstructorName();
    TypeName typeName = constructorName.getType();
    Type type = typeName.getType();
    if (type instanceof InterfaceType) {
      InterfaceType interfaceType = (InterfaceType) type;
      checkForConstOrNewWithAbstractClass(node, typeName, interfaceType);
      if (node.isConst()) {
        checkForConstWithNonConst(node);
        checkForConstWithUndefinedConstructor(node);
        checkForConstWithTypeParameters(node);
      } else {
        checkForNewWithUndefinedConstructor(node);
      }
      // TODO(jwren) Email Luke to make this determination: Should we always call all checks, if not,
      // which order should they be called in?
      // (Should we provide as many errors as possible, or try to be as concise as possible?)
      checkForTypeArgumentNotMatchingBounds(node, constructorName.getElement(), typeName);
    }
    return super.visitInstanceCreationExpression(node);
  }

  @Override
  public Void visitListLiteral(ListLiteral node) {
    if (node.getModifier() != null) {
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
    return super.visitListLiteral(node);
  }

  @Override
  public Void visitMapLiteral(MapLiteral node) {
    TypeArgumentList typeArguments = node.getTypeArguments();
    if (typeArguments != null) {
      NodeList<TypeName> arguments = typeArguments.getArguments();
      if (arguments.size() != 0) {
        checkForInvalidTypeArgumentForKey(arguments);
        if (node.getModifier() != null) {
          checkForInvalidTypeArgumentInConstTypedLiteral(
              arguments,
              CompileTimeErrorCode.INVALID_TYPE_ARGUMENT_IN_CONST_MAP);
        }
      }
    }
    checkForNonConstMapAsExpressionStatement(node);
    return super.visitMapLiteral(node);
  }

  @Override
  public Void visitMethodDeclaration(MethodDeclaration node) {
    ExecutableElement previousFunction = enclosingFunction;
    try {
      enclosingFunction = node.getElement();
      if (node.isSetter()) {
        checkForWrongNumberOfParametersForSetter(node.getName(), node.getParameters());
        checkForNonVoidReturnTypeForSetter(node.getReturnType());
      } else if (node.isOperator()) {
        checkForOptionalParameterInOperator(node);
        checkForWrongNumberOfParametersForOperator(node);
        checkForNonVoidReturnTypeForOperator(node);
      }
      checkForConcreteClassWithAbstractMember(node);
      checkForAllInvalidOverrideErrorCodes(node);
      return super.visitMethodDeclaration(node);
    } finally {
      enclosingFunction = previousFunction;
    }
  }

  @Override
  public Void visitMethodInvocation(MethodInvocation node) {
    checkForStaticAccessToInstanceMember(node.getTarget(), node.getMethodName());
    return super.visitMethodInvocation(node);
  }

  @Override
  public Void visitNativeFunctionBody(NativeFunctionBody node) {
    checkForNativeFunctionBodyInNonSDKCode(node);
    return super.visitNativeFunctionBody(node);
  }

  @Override
  public Void visitPrefixedIdentifier(PrefixedIdentifier node) {
    checkForStaticAccessToInstanceMember(node.getPrefix(), node.getIdentifier());
    return super.visitPrefixedIdentifier(node);
  }

  @Override
  public Void visitPropertyAccess(PropertyAccess node) {
    checkForStaticAccessToInstanceMember(node.getTarget(), node.getPropertyName());
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
    checkForAllReturnStatementErrorCodes(node);
    return super.visitReturnStatement(node);
  }

  @Override
  public Void visitSimpleFormalParameter(SimpleFormalParameter node) {
    checkForConstFormalParameter(node);
    return super.visitSimpleFormalParameter(node);
  }

  @Override
  public Void visitSimpleIdentifier(SimpleIdentifier node) {
    checkForReferenceToDeclaredVariableInInitializer(node);
    checkForImplicitThisReferenceInInitializer(node);
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
  public Void visitSwitchCase(SwitchCase node) {
    checkForCaseBlockNotTerminated(node);
    return super.visitSwitchCase(node);
  }

  @Override
  public Void visitSwitchStatement(SwitchStatement node) {
    checkForCaseExpressionTypeImplementsEquals(node);
    checkForInconsistentCaseExpressionTypes(node);
    checkForSwitchExpressionNotAssignable(node);
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
  public Void visitTypeParameter(TypeParameter node) {
    checkForBuiltInIdentifierAsName(
        node.getName(),
        CompileTimeErrorCode.BUILT_IN_IDENTIFIER_AS_TYPE_VARIABLE_NAME);
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
    try {
      if (initializerNode != null) {
        initializerNode.accept(this);
      }
    } finally {
      namesForReferenceToDeclaredVariableInInitializer.remove(name);
    }
    // done
    return null;
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
                fieldElement.getDisplayName());
            foundError = true;
          }
//          else if (variableElement instanceof TopLevelVariableElement) {
          // TODO(jwren) Report error, constructor initializer variable is a top level element
          // (Either here or in ElementResolver#visitFieldFormalParameter)
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
   * This checks the passed method declaration against override-error codes.
   * 
   * @param node the {@link MethodDeclaration} to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticWarningCode#INSTANCE_METHOD_NAME_COLLIDES_WITH_SUPERCLASS_STATIC
   * @see CompileTimeErrorCode#INVALID_OVERRIDE_REQUIRED
   * @see CompileTimeErrorCode#INVALID_OVERRIDE_POSITIONAL
   * @see CompileTimeErrorCode#INVALID_OVERRIDE_NAMED
   * @see StaticWarningCode#INVALID_METHOD_OVERRIDE_RETURN_TYPE
   */
  private boolean checkForAllInvalidOverrideErrorCodes(MethodDeclaration node) {
    // TODO (jwren) Missing check for INVALID_OVERRIDE_DIFFERENT_DEFAULT_VALUES, we need the
    // constant values cached to do this verification.
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
    String methodNameStr = methodName.getName();
    ExecutableElement overriddenExecutable = inheritanceManager.lookupInheritance(
        enclosingClass,
        executableElement.getName());

    // SWC.INSTANCE_METHOD_NAME_COLLIDES_WITH_SUPERCLASS_STATIC
    if (overriddenExecutable == null) {
      if (!node.isGetter() && !node.isSetter() && !node.isOperator()) {
        ClassElement superclassClassElement = null;
        InterfaceType superclassType = enclosingClass.getSupertype();
        if (superclassType != null) {
          superclassClassElement = superclassType.getElement();
        }
        while (superclassClassElement != null) {
          FieldElement[] fieldElts = superclassClassElement.getFields();
          for (FieldElement fieldElt : fieldElts) {
            if (fieldElt.getName().equals(methodNameStr) && fieldElt.isStatic()) {
              errorReporter.reportError(
                  StaticWarningCode.INSTANCE_METHOD_NAME_COLLIDES_WITH_SUPERCLASS_STATIC,
                  methodName,
                  methodNameStr,
                  fieldElt.getEnclosingElement().getDisplayName());
              return true;
            }
          }
          PropertyAccessorElement[] propertyAccessorElts = superclassClassElement.getAccessors();
          for (PropertyAccessorElement accessorElt : propertyAccessorElts) {
            if (accessorElt.getName().equals(methodNameStr) && accessorElt.isStatic()) {
              errorReporter.reportError(
                  StaticWarningCode.INSTANCE_METHOD_NAME_COLLIDES_WITH_SUPERCLASS_STATIC,
                  methodName,
                  methodNameStr,
                  accessorElt.getEnclosingElement().getDisplayName());
              return true;
            }
          }
          MethodElement[] methodElements = superclassClassElement.getMethods();
          for (MethodElement methodElement : methodElements) {
            if (methodElement.getName().equals(methodNameStr) && methodElement.isStatic()) {
              errorReporter.reportError(
                  StaticWarningCode.INSTANCE_METHOD_NAME_COLLIDES_WITH_SUPERCLASS_STATIC,
                  methodName,
                  methodNameStr,
                  methodElement.getEnclosingElement().getDisplayName());
              return true;
            }
          }
          superclassClassElement = superclassClassElement.getSupertype() != null
              ? superclassClassElement.getSupertype().getElement() : null;
        }
      }
      return false;
    }

    FunctionType overridingFT = executableElement.getType();
    FunctionType overriddenFT = overriddenExecutable.getType();
    InterfaceType enclosingType = enclosingClass.getType();
    overriddenFT = inheritanceManager.substituteTypeArgumentsInMemberFromInheritance(
        overriddenFT,
        methodNameStr,
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
    if (overridingNormalPT.length != overriddenNormalPT.length) {
      errorReporter.reportError(
          CompileTimeErrorCode.INVALID_OVERRIDE_REQUIRED,
          methodName,
          overriddenNormalPT.length,
          overriddenExecutable.getEnclosingElement().getDisplayName());
      return true;
    }
    if (overridingPositionalPT.length < overriddenPositionalPT.length) {
      errorReporter.reportError(
          CompileTimeErrorCode.INVALID_OVERRIDE_POSITIONAL,
          methodName,
          overridingPositionalPT.length,
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
            CompileTimeErrorCode.INVALID_OVERRIDE_NAMED,
            methodName,
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
          !node.isGetter() ? StaticWarningCode.INVALID_METHOD_OVERRIDE_RETURN_TYPE
              : StaticWarningCode.INVALID_GETTER_OVERRIDE_RETURN_TYPE,
          methodName,
          overridingFTReturnType.getName(),
          overriddenFTReturnType.getName(),
          overriddenExecutable.getEnclosingElement().getDisplayName());
      return true;
    }

    // SWC.INVALID_METHOD_OVERRIDE_NORMAL_PARAM_TYPE
    FormalParameterList formalParameterList = node.getParameters();
    if (formalParameterList == null) {
      return false;
    }
    NodeList<FormalParameter> parameterNodeList = formalParameterList.getParameters();
    int parameterIndex = 0;
    for (int i = 0; i < overridingNormalPT.length; i++) {
      if (!overridingNormalPT[i].isAssignableTo(overriddenNormalPT[i])) {
        errorReporter.reportError(
            !node.isSetter() ? StaticWarningCode.INVALID_METHOD_OVERRIDE_NORMAL_PARAM_TYPE
                : StaticWarningCode.INVALID_SETTER_OVERRIDE_NORMAL_PARAM_TYPE,
            parameterNodeList.get(parameterIndex),
            overridingNormalPT[i].getName(),
            overriddenNormalPT[i].getName(),
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
            parameterNodeList.get(parameterIndex),
            overridingPositionalPT[i].getName(),
            overriddenPositionalPT[i].getName(),
            overriddenExecutable.getEnclosingElement().getDisplayName());
        return true;
      }
      parameterIndex++;
    }

    // SWC.INVALID_METHOD_OVERRIDE_NAMED_PARAM_TYPE
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
        // lookup the ast parameter node for the error to select
        NormalFormalParameter parameterToSelect = null;
        for (FormalParameter formalParameter : parameterNodeList) {
          if (formalParameter instanceof DefaultFormalParameter
              && formalParameter.getKind() == ParameterKind.NAMED) {
            DefaultFormalParameter defaultFormalParameter = (DefaultFormalParameter) formalParameter;
            NormalFormalParameter normalFormalParameter = defaultFormalParameter.getParameter();
            if (overriddenNamedPTEntry.getKey().equals(
                normalFormalParameter.getIdentifier().getName())) {
              parameterToSelect = normalFormalParameter;
              break;
            }
          }
        }
        if (parameterToSelect != null) {
          errorReporter.reportError(
              StaticWarningCode.INVALID_METHOD_OVERRIDE_NAMED_PARAM_TYPE,
              parameterToSelect,
              overridingType.getName(),
              overriddenNamedPTEntry.getValue().getName(),
              overriddenExecutable.getEnclosingElement().getDisplayName());
          return true;
        }
      }
    }
    return false;
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
      ClassElement mixinElement = ((InterfaceType) mixinType).getElement();
      problemReported |= checkForMixinDeclaresConstructor(mixinName, mixinElement);
      problemReported |= checkForMixinInheritsNotFromObject(mixinName, mixinElement);
      problemReported |= checkForMixinReferencesSuper(mixinName, mixinElement);
    }
    return problemReported;
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
    // void
    Type staticReturnType = getStaticType(returnExpression);
    if (expectedReturnType.isVoid()) {
      if (staticReturnType.isVoid() || staticReturnType.isDynamic()
          || staticReturnType == BottomTypeImpl.getInstance()) {
        return false;
      }
      errorReporter.reportError(
          StaticTypeWarningCode.RETURN_OF_INVALID_TYPE,
          returnExpression,
          staticReturnType.getName(),
          expectedReturnType.getName(),
          enclosingFunction.getDisplayName());
      return true;
    }
    // RETURN_OF_INVALID_TYPE
    boolean isStaticAssignable = staticReturnType.isAssignableTo(expectedReturnType);
    Type propagatedReturnType = getPropagatedType(returnExpression);
    if (propagatedReturnType == null) {
      if (isStaticAssignable) {
        return false;
      }
      errorReporter.reportError(
          StaticTypeWarningCode.RETURN_OF_INVALID_TYPE,
          returnExpression,
          staticReturnType.getName(),
          expectedReturnType.getName(),
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
          staticReturnType.getName(),
          expectedReturnType.getName(),
          enclosingFunction.getDisplayName());
      return true;
    }
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
   * This verifies that the passed argument can be assigned to their corresponding parameters.
   * 
   * @param node the argument to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticWarningCode#ARGUMENT_TYPE_NOT_ASSIGNABLE
   */
  private boolean checkForArgumentTypeNotAssignable(Expression argument) {
    if (argument == null) {
      return false;
    }
    // prepare corresponding parameter
    ParameterElement parameterElement = argument.getParameterElement();
    if (parameterElement == null) {
      return false;
    }
    // prepare parameter type
    Type parameterType = parameterElement.getType();
    if (parameterType == null) {
      return false;
    }
    // prepare argument type
    Type argumentType = getBestType(argument);
    if (argumentType == null) {
      return false;
    }
    // OK, argument is assignable
    if (argumentType.isAssignableTo(parameterType)) {
      return false;
    }
    // TODO(scheglov) bug in type algebra?
    if (parameterType.isObject() && argumentType instanceof FunctionType) {
      return false;
    }
    // report problem
    errorReporter.reportError(
        StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE,
        argument,
        argumentType,
        parameterType);
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
    Expression lhs = node.getLeftHandSide();
    if (lhs instanceof Identifier) {
      Element leftElement = ((Identifier) lhs).getElement();
      if (leftElement instanceof VariableElement) {
        VariableElement leftVar = (VariableElement) leftElement;
        if (leftVar.isFinal()) {
          errorReporter.reportError(StaticWarningCode.ASSIGNMENT_TO_FINAL, lhs);
          return true;
        }
        return false;
      }
      if (leftElement instanceof PropertyAccessorElement) {
        PropertyAccessorElement leftAccessor = (PropertyAccessorElement) leftElement;
        if (!leftAccessor.isSetter()) {
          errorReporter.reportError(StaticWarningCode.ASSIGNMENT_TO_FINAL, lhs);
          return true;
        }
        return false;
      }
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
   * @return {@code true} if and only if an error code is generated on the passed node
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
   * @return {@code true} if and only if an error code is generated on the passed node
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
   * This verifies that the passed switch statement does not have a case expression with the
   * operator '==' overridden.
   * 
   * @param node the switch statement to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#CASE_EXPRESSION_TYPE_IMPLEMENTS_EQUALS
   */
  private boolean checkForCaseExpressionTypeImplementsEquals(SwitchStatement node) {
    Expression expression = node.getExpression();
    Type type = getStaticType(expression);
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
              element.getDisplayName());
          return true;
        }
      }
    }
    return false;
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

  // TODO(jwren) replace this method with a generic "conflicting" error code evaluation
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
   * This verifies that the passed constructor declaration is 'const' then there are no non-final
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
    ConstructorElement constructorElement = node.getElement();
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
    if (name.getElement() instanceof TypeVariableElement) {
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
    if (node.getElement() != null) {
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
   */
  private boolean checkForImplicitThisReferenceInInitializer(SimpleIdentifier node) {
    if (!isInConstructorInitializer) {
      return false;
    }
    // prepare element
    Element element = node.getElement();
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
    // qualified method invocation
    ASTNode parent = node.getParent();
    if (parent instanceof MethodInvocation) {
      MethodInvocation invocation = (MethodInvocation) parent;
      if (invocation.getMethodName() == node && invocation.getRealTarget() != null) {
        return false;
      }
    }
    // qualified property access
    {
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
    }
    // report problem
    errorReporter.reportError(CompileTimeErrorCode.IMPLICIT_THIS_REFERENCE_IN_INITIALIZER, node);
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
    Element nodeElement = node.getElement();
    if (!(nodeElement instanceof ImportElement)) {
      return false;
    }
    ImportElement nodeImportElement = (ImportElement) nodeElement;
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
          firstType = getBestType(expression);
        } else {
          Type nType = getBestType(expression);
          if (!firstType.equals(nType)) {
            errorReporter.reportError(
                CompileTimeErrorCode.INCONSISTENT_CASE_EXPRESSION_TYPES,
                expression,
                expression.toSource(),
                firstType.getName());
            foundError = true;
          }
        }
      }
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
    MethodElement invokedMethod = node.getElement();
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
          rightType.getName(),
          leftType.getName());
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
    Type propagatedRightType = getPropagatedType(rhs);
    if (propagatedRightType == null) {
      if (!isStaticAssignable) {
        errorReporter.reportError(
            StaticTypeWarningCode.INVALID_ASSIGNMENT,
            rhs,
            staticRightType.getName(),
            leftType.getName());
        return true;
      }
    } else {
      boolean isPropagatedAssignable = propagatedRightType.isAssignableTo(leftType);
      if (!isStaticAssignable && !isPropagatedAssignable) {
        errorReporter.reportError(
            StaticTypeWarningCode.INVALID_ASSIGNMENT,
            rhs,
            staticRightType.getName(),
            leftType.getName());
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
   * Checks to ensure that first type argument to a map literal must be the 'String' type.
   * 
   * @param arguments a non-{@code null}, non-empty {@link TypeName} node list from the respective
   *          {@link MapLiteral}
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#INVALID_TYPE_ARGUMENT_FOR_KEY
   */
  private boolean checkForInvalidTypeArgumentForKey(NodeList<TypeName> arguments) {
    TypeName firstArgument = arguments.get(0);
    Type firstArgumentType = firstArgument.getType();
    if (firstArgumentType != null && !firstArgumentType.equals(typeProvider.getStringType())) {
      errorReporter.reportError(CompileTimeErrorCode.INVALID_TYPE_ARGUMENT_FOR_KEY, firstArgument);
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
      if (typeName.getType() instanceof TypeVariableType) {
        errorReporter.reportError(errorCode, typeName, typeName.getName());
        foundError = true;
      }
    }
    return foundError;
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
   * Check to make sure that all similarly typed accessors are of the same type TODO (ericarnold):
   * (including inherited accessors).
   * 
   * @param node The accessor currently being visited.
   */
  private void checkForMismatchedAccessorTypes(Declaration node) {
    SimpleIdentifier accessorName = null;

    // Check to make sure the node is of the correct type for top-level accessors.
    if (node instanceof FunctionDeclaration) {
      FunctionDeclaration accessorDeclaration = (FunctionDeclaration) node;
      accessorName = accessorDeclaration.getName();
      String accessorTextName = accessorName.getName();

      // Check if there is an existing counterpart getter / setter.
      FunctionDeclaration counterpartAccessor = compilationUnitAccessors.get(accessorTextName);
      if (counterpartAccessor == null) {
        // If not, just make a reference to the accessor and move on.
        compilationUnitAccessors.put(accessorTextName, accessorDeclaration);
      } else {
        Type getterType;
        Type setterType;

        // Get the type of the existing counterpart, and then the current element.
        if (counterpartAccessor.isGetter()) {
          getterType = getGetterType(counterpartAccessor);
          setterType = getSetterType(accessorDeclaration);
        } else {
          getterType = getGetterType(accessorDeclaration);
          setterType = getSetterType(counterpartAccessor);
        }

        // If either types are not assignable to each other, report an error (if the getter is null,
        // it is dynamic which is assignable to everything).
        if (getterType != null && !getterType.isAssignableTo(setterType)) {
          errorReporter.reportError(
              StaticWarningCode.MISMATCHED_GETTER_AND_SETTER_TYPES,
              accessorDeclaration,
              accessorTextName,
              setterType.toString(),
              getterType.toString());
        }
      }
    } else if (node instanceof MethodDeclaration) {
      // TODO(ericarnold): This is a method.  Check our superiors
      // TODO(ericarnold): Check for mismatched getters / setters in this class / level.
      MethodDeclaration methodDeclaration = (MethodDeclaration) node;
      accessorName = methodDeclaration.getName();
      ASTNode parent = methodDeclaration.getParent();
    }
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
    if (node.getElement() != null) {
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
   * This checks that passed class declaration overrides all members required by its superclasses
   * and interfaces.
   * 
   * @param node the {@link ClassDeclaration} to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticWarningCode#NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_SINGLE
   * @see StaticWarningCode#NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_MULTIPLE
   */
  private boolean checkForNonAbstractClassInheritsAbstractMember(ClassDeclaration node) {
    if (enclosingClass.isAbstract()) {
      return false;
    }
    HashSet<ExecutableElement> missingOverrides = new HashSet<ExecutableElement>();

    // Store in local sets the set of all method and accessor names:
    HashSet<String> methodsInEnclosingClass = new HashSet<String>();
    HashSet<String> accessorsInEnclosingClass = new HashSet<String>();
    MethodElement[] methods = enclosingClass.getMethods();
    for (MethodElement method : methods) {
      methodsInEnclosingClass.add(method.getName());
    }
    PropertyAccessorElement[] accessors = enclosingClass.getAccessors();
    for (PropertyAccessorElement accessor : accessors) {
      accessorsInEnclosingClass.add(accessor.getName());
    }

    // Loop through the set of all executable elements inherited from the superclass chain.
    HashMap<String, ExecutableElement> membersInheritedFromSuperclasses = inheritanceManager.getMapOfMembersInheritedFromClasses(enclosingClass);
    for (Entry<String, ExecutableElement> entry : membersInheritedFromSuperclasses.entrySet()) {
      ExecutableElement executableElt = entry.getValue();
      if (executableElt instanceof MethodElement) {
        MethodElement methodElt = (MethodElement) executableElt;
        // If the method is abstract, then verify that this class has a method which overrides the
        // abstract method from the superclass.
        if (methodElt.isAbstract()) {
          String methodName = entry.getKey();
          boolean foundOverridingMethod = false;
          if (methodsInEnclosingClass.contains(methodName)) {
            foundOverridingMethod = true;
          }
          // If an abstract method was found in a superclass, but it is not in the subclass, then
          // add the executable element to the missingOverides set.
          if (!foundOverridingMethod) {
            missingOverrides.add(executableElt);
          }
        }
      } else if (executableElt instanceof PropertyAccessorElement) {
        PropertyAccessorElement propertyAccessorElt = (PropertyAccessorElement) executableElt;
        // If the accessor is abstract, then verify that this class has an accessor which overrides
        // the abstract accessor from the superclass.
        if (propertyAccessorElt.isAbstract()) {
          String accessorName = entry.getKey();
          boolean foundOverridingMember = false;
          if (accessorsInEnclosingClass.contains(accessorName)) {
            foundOverridingMember = true;
          }
          // If an abstract method was found in a superclass, but it is not in the subclass, then
          // add the executable element to the missingOverides set.
          if (!foundOverridingMember) {
            missingOverrides.add(executableElt);
          }
        }
      }
    }

    // Loop through the set of all executable elements inherited from the interfaces.
    HashMap<String, ExecutableElement> membersInheritedFromInterfaces = inheritanceManager.getMapOfMembersInheritedFromInterfaces(enclosingClass);
    for (Entry<String, ExecutableElement> entry : membersInheritedFromInterfaces.entrySet()) {
      ExecutableElement executableElt = entry.getValue();
      // First check to see if this element was declared in the superclass chain.
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
        String methodName = entry.getKey();
        boolean foundOverridingMethod = false;
        if (methodsInEnclosingClass.contains(methodName)) {
          foundOverridingMethod = true;
        }
        // If a method was found in an interface, but it is not in the enclosing class, then add the
        // executable element to the missingOverides set.
        if (!foundOverridingMethod) {
          missingOverrides.add(executableElt);
        }
      } else if (executableElt instanceof PropertyAccessorElement) {
        // Verify that this class has a member which overrides the method from the interface.
        String accessorName = entry.getKey();
        boolean foundOverridingMember = false;
        if (accessorsInEnclosingClass.contains(accessorName)) {
          foundOverridingMember = true;
        }
        // If a method was found in an interface, but it is not in the enclosing class, then add the
        // executable element to the missingOverides set.
        if (!foundOverridingMember) {
          missingOverrides.add(executableElt);
        }
      }
    }
    int missingOverridesSize = missingOverrides.size();
    if (missingOverridesSize == 0) {
      return false;
    }
    // TODO (jwren/scheglov) Add a quick fix which needs the following array.
    //ExecutableElement[] missingOverridesArray = missingOverrides.toArray(new ExecutableElement[missingOverridesSize]);
    if (missingOverridesSize == 1) {
      ExecutableElement executableElt = missingOverrides.iterator().next();
      errorReporter.reportError(
          StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_SINGLE,
          node.getName(),
          executableElt.getDisplayName(),
          executableElt.getEnclosingElement().getDisplayName());
      return true;
    } else {
      errorReporter.reportError(
          StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_MULTIPLE,
          node.getName());
      return true;
    }
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
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticTypeWarningCode#NON_BOOL_EXPRESSION
   */
  private boolean checkForNonBoolExpression(AssertStatement node) {
    Expression expression = node.getCondition();
    Type type = getStaticType(expression);
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
    if (node.getModifier() != null) {
      return false;
    }
    // has type arguments
    if (node.getTypeArguments() != null) {
      return false;
    }
    // prepare statement
    Statement statement = node.getAncestor(Statement.class);
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
  private boolean checkForPrivateOptionalParameter(DefaultFormalParameter node) {
    Token separator = node.getSeparator();
    if (separator != null && separator.getLexeme().equals(":")) {
      NormalFormalParameter parameter = node.getParameter();
      SimpleIdentifier name = parameter.getIdentifier();
      if (!name.isSynthetic() && name.getName().startsWith("_")) {
        errorReporter.reportError(CompileTimeErrorCode.PRIVATE_OPTIONAL_PARAMETER, node);
        return true;
      }
    }
    return false;
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
   * This checks the passed constructor declaration has a valid combination of redirected
   * constructor invocation(s), super constructor invocations and field initializers.
   * 
   * @param node the constructor declaration to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#MULTIPLE_REDIRECTING_CONSTRUCTOR_INVOCATIONS
   * @see CompileTimeErrorCode#SUPER_IN_REDIRECTING_CONSTRUCTOR
   * @see CompileTimeErrorCode#FIELD_INITIALIZER_REDIRECTING_CONSTRUCTOR
   */
  private boolean checkForRedirectingConstructorErrorCodes(ConstructorDeclaration node) {
    int numProblems = 0;
    // check if there are redirected invocations
    int numRedirections = 0;
    for (ConstructorInitializer initializer : node.getInitializers()) {
      if (initializer instanceof RedirectingConstructorInvocation) {
        if (numRedirections > 0) {
          errorReporter.reportError(
              CompileTimeErrorCode.MULTIPLE_REDIRECTING_CONSTRUCTOR_INVOCATIONS,
              initializer);
          numProblems++;
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
          numProblems++;
        }
        if (initializer instanceof ConstructorFieldInitializer) {
          errorReporter.reportError(
              CompileTimeErrorCode.FIELD_INITIALIZER_REDIRECTING_CONSTRUCTOR,
              initializer);
          numProblems++;
        }
      }
    }
    // done
    return numProblems != 0;
  }

  /**
   * This checks if the passed constructor declaration has redirected constructor with compatible
   * function type.
   * 
   * @param node the constructor declaration to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see StaticWarningCode#REDIRECT_TO_INVALID_RETURN_TYPE
   * @see StaticWarningCode#REDIRECT_TO_INVALID_FUNCTION_TYPE
   */
  private boolean checkForRedirectToInvalidFunction(ConstructorDeclaration node) {
    // prepare redirected constructor node
    ConstructorName redirectedNode = node.getRedirectedConstructor();
    if (redirectedNode == null) {
      return false;
    }
    // prepare redirected constructor type
    ConstructorElement redirectedElement = redirectedNode.getElement();
    if (redirectedElement == null) {
      return false;
    }
    FunctionType redirectedType = redirectedElement.getType();
    Type redirectedReturnType = redirectedType.getReturnType();
    // report specific problem when return type is incompatible
    FunctionType constructorType = node.getElement().getType();
    Type constructorReturnType = constructorType.getReturnType();
    if (!redirectedReturnType.isSubtypeOf(constructorReturnType)) {
      errorReporter.reportError(
          StaticWarningCode.REDIRECT_TO_INVALID_RETURN_TYPE,
          redirectedNode,
          redirectedReturnType,
          constructorReturnType);
      return true;
    }
    // check parameters
    if (!redirectedType.isSubtypeOf(constructorType)) {
      errorReporter.reportError(
          StaticWarningCode.REDIRECT_TO_INVALID_FUNCTION_TYPE,
          redirectedNode,
          redirectedType,
          constructorType);
      return true;
    }
    // OK
    return false;
  }

  /**
   * This checks if the passed identifier is banned because it is part of the variable declaration
   * with the same name.
   * 
   * @param node the identifier to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#REFERENCE_TO_DECLARED_VARIABLE_IN_INITIALIZER
   */
  private boolean checkForReferenceToDeclaredVariableInInitializer(SimpleIdentifier node) {
    ASTNode parent = node.getParent();
    // ignore if property
    if (parent instanceof PrefixedIdentifier) {
      PrefixedIdentifier prefixedIdentifier = (PrefixedIdentifier) parent;
      if (prefixedIdentifier.getIdentifier() == node) {
        return false;
      }
    }
    if (parent instanceof PropertyAccess) {
      PropertyAccess propertyAccess = (PropertyAccess) parent;
      if (propertyAccess.getPropertyName() == node) {
        return false;
      }
    }
    // ignore if name of the method with target
    if (parent instanceof MethodInvocation) {
      MethodInvocation methodInvocation = (MethodInvocation) parent;
      if (methodInvocation.getTarget() != null && methodInvocation.getMethodName() == node) {
        return false;
      }
    }
    // ignore if name of the constructor
    if (parent instanceof ConstructorName) {
      ConstructorName constructorName = (ConstructorName) parent;
      if (constructorName.getName() == node) {
        return false;
      }
    }
    // check if name is banned
    String name = node.getName();
    if (!namesForReferenceToDeclaredVariableInInitializer.contains(name)) {
      return false;
    }
    // report problem
    errorReporter.reportError(
        CompileTimeErrorCode.REFERENCE_TO_DECLARED_VARIABLE_IN_INITIALIZER,
        node,
        name);
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
    Element element = name.getElement();
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
   * This verifies that the type arguments in the passed instance creation expression are all within
   * their bounds as specified by the class element where the constructor [that is being invoked] is
   * declared.
   * 
   * @param node the instance creation expression to evaluate
   * @param typeName the {@link TypeName} of the {@link ConstructorName} from the
   *          {@link InstanceCreationExpression}, this is the AST node that the error is attached to
   * @param constructorElement the {@link ConstructorElement} from the instance creation expression
   * @return {@code true} if and only if an error code is generated on the passed node
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
                boundingElts[i].getDisplayName());
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * This checks that if the passed generative constructor has no explicit super constructor
   * invocation, then super class has the default generative constructor.
   * 
   * @param node the constructor declaration to evaluate
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see CompileTimeErrorCode#UNDEFINED_CONSTRUCTOR_IN_INITIALIZER_DEFAULT
   * @see CompileTimeErrorCode#NON_GENERATIVE_CONSTRUCTOR
   */
  private boolean checkForUndefinedConstructorInInitializerImplicit(ConstructorDeclaration node) {
    // ignore if not generative
    if (node.getFactoryKeyword() != null) {
      return false;
    }
    // prepare "super"
    if (enclosingClass == null) {
      return false;
    }
    InterfaceType superType = enclosingClass.getSupertype();
    if (superType == null) {
      return false;
    }
    ClassElement superElement = superType.getElement();
    // has implicit super constructor invocation
    for (ConstructorInitializer constructorInitializer : node.getInitializers()) {
      if (constructorInitializer instanceof SuperConstructorInvocation) {
        return false;
      }
    }
    // OK, super class has unnamed constructor
    ConstructorElement superDefaultConstructor = superElement.getUnnamedConstructor();
    if (superDefaultConstructor != null) {
      if (superDefaultConstructor.isFactory()) {
        errorReporter.reportError(
            CompileTimeErrorCode.NON_GENERATIVE_CONSTRUCTOR,
            node.getReturnType(),
            superDefaultConstructor);
        return true;
      }
      return false;
    }
    // report error
    errorReporter.reportError(
        CompileTimeErrorCode.UNDEFINED_CONSTRUCTOR_IN_INITIALIZER_DEFAULT,
        node.getReturnType(),
        superElement.getName());
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
   * This verifies if the passed setter parameter list have only one parameter.
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
    int numberOfParameters = parameterList.getParameters().size();
    if (numberOfParameters != 1) {
      errorReporter.reportError(
          CompileTimeErrorCode.WRONG_NUMBER_OF_PARAMETERS_FOR_SETTER,
          setterName,
          numberOfParameters);
      return true;
    }
    return false;
  }

  /**
   * Return the propagated type of the given expression, or the static type if there is no
   * propagated type information.
   * 
   * @param expression the expression whose type is to be returned
   * @return the propagated or static type of the given expression, whichever is best
   */
  private Type getBestType(Expression expression) {
    Type type = getPropagatedType(expression);
    if (type == null) {
      type = getStaticType(expression);
    }
    return type;
  }

  /**
   * Returns the Type (return type) for a given getter.
   * 
   * @param getterDeclaration
   * @return The type of the given getter.
   */
  private Type getGetterType(FunctionDeclaration getterDeclaration) {
    TypeName getterTypeName = getterDeclaration.getReturnType();
    if (getterTypeName != null) {
      return getterTypeName.getType();
    } else {
      return null;
    }
  }

  /**
   * Return the propagated type of the given expression that is to be used for type analysis.
   * 
   * @param expression the expression whose type is to be returned
   * @return the propagated type of the given expression
   */
  private Type getPropagatedType(Expression expression) {
    return expression.getPropagatedType();
  }

  /**
   * Returns the Type (first and only parameter) for a given setter.
   * 
   * @param setterDeclaration
   * @return The type of the given setter.
   */
  private Type getSetterType(FunctionDeclaration setterDeclaration) {
    FormalParameterList parameters = setterDeclaration.getFunctionExpression().getParameters();
    FormalParameter firstParameter = parameters.getParameters().get(0);
    Type setterType = firstParameter.getElement().getType();
    return setterType;
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
      Element element = ((Identifier) expression).getElement();
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
      if (n instanceof ConstructorFieldInitializer) {
        return false;
      }
      if (n instanceof MethodDeclaration) {
        MethodDeclaration method = (MethodDeclaration) n;
        return !method.isStatic();
      }
    }
    return false;
  }
}

/**
 * Information about accessors in classes.
 */
final class ClassAccessorInformation {
  public HashMap<String, MethodDeclaration> classGettersAndSetters = null;
}
