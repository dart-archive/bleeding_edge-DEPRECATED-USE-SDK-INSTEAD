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
package com.google.dart.engine.internal.resolver;

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.ASTVisitor;
import com.google.dart.engine.ast.ArgumentList;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.BreakStatement;
import com.google.dart.engine.ast.Combinator;
import com.google.dart.engine.ast.CommentReference;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.ConstructorFieldInitializer;
import com.google.dart.engine.ast.ConstructorName;
import com.google.dart.engine.ast.ContinueStatement;
import com.google.dart.engine.ast.ExportDirective;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FieldFormalParameter;
import com.google.dart.engine.ast.FunctionExpressionInvocation;
import com.google.dart.engine.ast.HideCombinator;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.IndexExpression;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.LibraryDirective;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NamedExpression;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.NullLiteral;
import com.google.dart.engine.ast.PartDirective;
import com.google.dart.engine.ast.PartOfDirective;
import com.google.dart.engine.ast.PostfixExpression;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.RedirectingConstructorInvocation;
import com.google.dart.engine.ast.ShowCombinator;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SuperConstructorInvocation;
import com.google.dart.engine.ast.SuperExpression;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.TypeParameter;
import com.google.dart.engine.ast.visitor.SimpleASTVisitor;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.ExportElement;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LabelElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.MultiplyDefinedElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.PropertyInducingElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.StaticTypeWarningCode;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.FieldFormalParameterElementImpl;
import com.google.dart.engine.internal.element.LabelElementImpl;
import com.google.dart.engine.internal.element.MultiplyDefinedElementImpl;
import com.google.dart.engine.internal.element.TypeVariableElementImpl;
import com.google.dart.engine.internal.scope.LabelScope;
import com.google.dart.engine.internal.scope.Namespace;
import com.google.dart.engine.internal.scope.NamespaceBuilder;
import com.google.dart.engine.internal.scope.Scope;
import com.google.dart.engine.resolver.ResolverErrorCode;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.type.TypeVariableType;
import com.google.dart.engine.utilities.dart.ParameterKind;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Instances of the class {@code ElementResolver} are used by instances of {@link ResolverVisitor}
 * to resolve references within the AST structure to the elements being referenced. The requirements
 * for the element resolver are:
 * <ol>
 * <li>Every {@link SimpleIdentifier} should be resolved to the element to which it refers.
 * Specifically:
 * <ul>
 * <li>An identifier within the declaration of that name should resolve to the element being
 * declared.</li>
 * <li>An identifier denoting a prefix should resolve to the element representing the import that
 * defines the prefix (an {@link ImportElement}).</li>
 * <li>An identifier denoting a variable should resolve to the element representing the variable (a
 * {@link VariableElement}).</li>
 * <li>An identifier denoting a parameter should resolve to the element representing the parameter
 * (a {@link ParameterElement}).</li>
 * <li>An identifier denoting a field should resolve to the element representing the getter or
 * setter being invoked (a {@link PropertyAccessorElement}).</li>
 * <li>An identifier denoting the name of a method or function being invoked should resolve to the
 * element representing the method or function (a {@link ExecutableElement}).</li>
 * <li>An identifier denoting a label should resolve to the element representing the label (a
 * {@link LabelElement}).</li>
 * </ul>
 * The identifiers within directives are exceptions to this rule and are covered below.</li>
 * <li>Every node containing a token representing an operator that can be overridden (
 * {@link BinaryExpression}, {@link PrefixExpression}, {@link PostfixExpression}) should resolve to
 * the element representing the method invoked by that operator (a {@link MethodElement}).</li>
 * <li>Every {@link FunctionExpressionInvocation} should resolve to the element representing the
 * function being invoked (a {@link FunctionElement}). This will be the same element as that to
 * which the name is resolved if the function has a name, but is provided for those cases where an
 * unnamed function is being invoked.</li>
 * <li>Every {@link LibraryDirective} and {@link PartOfDirective} should resolve to the element
 * representing the library being specified by the directive (a {@link LibraryElement}) unless, in
 * the case of a part-of directive, the specified library does not exist.</li>
 * <li>Every {@link ImportDirective} and {@link ExportDirective} should resolve to the element
 * representing the library being specified by the directive unless the specified library does not
 * exist (an {@link ImportElement} or {@link ExportElement}).</li>
 * <li>The identifier representing the prefix in an {@link ImportDirective} should resolve to the
 * element representing the prefix (a {@link PrefixElement}).</li>
 * <li>The identifiers in the hide and show combinators in {@link ImportDirective}s and
 * {@link ExportDirective}s should resolve to the elements that are being hidden or shown,
 * respectively, unless those names are not defined in the specified library (or the specified
 * library does not exist).</li>
 * <li>Every {@link PartDirective} should resolve to the element representing the compilation unit
 * being specified by the string unless the specified compilation unit does not exist (a
 * {@link CompilationUnitElement}).</li>
 * </ol>
 * Note that AST nodes that would represent elements that are not defined are not resolved to
 * anything. This includes such things as references to undeclared variables (which is an error) and
 * names in hide and show combinators that are not defined in the imported library (which is not an
 * error).
 * 
 * @coverage dart.engine.resolver
 */
public class ElementResolver extends SimpleASTVisitor<Void> {
  /**
   * Instances of the class {@code SyntheticIdentifier} implement an identifier that can be used to
   * look up names in the lexical scope when there is no identifier in the AST structure. There is
   * no identifier in the AST when the parser could not distinguish between a method invocation and
   * an invocation of a top-level function imported with a prefix.
   */
  private static class SyntheticIdentifier extends Identifier {
    /**
     * The name of the synthetic identifier.
     */
    private final String name;

    /**
     * Initialize a newly created synthetic identifier to have the given name.
     * 
     * @param name the name of the synthetic identifier
     */
    private SyntheticIdentifier(String name) {
      this.name = name;
    }

    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
      return null;
    }

    @Override
    public Token getBeginToken() {
      return null;
    }

    @Override
    public Element getElement() {
      return null;
    }

    @Override
    public Token getEndToken() {
      return null;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public void visitChildren(ASTVisitor<?> visitor) {
    }
  }

  /**
   * @return {@code true} if the given identifier is the return type of a constructor declaration.
   */
  private static boolean isConstructorReturnType(SimpleIdentifier node) {
    ASTNode parent = node.getParent();
    if (parent instanceof ConstructorDeclaration) {
      ConstructorDeclaration constructor = (ConstructorDeclaration) parent;
      return constructor.getReturnType() == node;
    }
    return false;
  }

  /**
   * Checks if the given 'super' expression is used in the valid context.
   * 
   * @param node the 'super' expression to analyze
   * @return {@code true} if the given 'super' expression is in the valid context
   */
  private static boolean isSuperInValidContext(SuperExpression node) {
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

  /**
   * The resolver driving this participant.
   */
  private ResolverVisitor resolver;

  /**
   * A table mapping nodes in the AST to the element produced based on static type information.
   */
  private HashMap<ASTNode, ExecutableElement> staticElementMap;

  /**
   * The name of the method that can be implemented by a class to allow its instances to be invoked
   * as if they were a function.
   */
  public static final String CALL_METHOD_NAME = "call";

  /**
   * The name of the method that will be invoked if an attempt is made to invoke an undefined method
   * on an object.
   */
  private static final String NO_SUCH_METHOD_METHOD_NAME = "noSuchMethod";

  /**
   * Initialize a newly created visitor to resolve the nodes in a compilation unit.
   * 
   * @param resolver the resolver driving this participant
   */
  public ElementResolver(ResolverVisitor resolver) {
    this.resolver = resolver;
    staticElementMap = resolver.getStaticElementMap();
  }

  @Override
  public Void visitAssignmentExpression(AssignmentExpression node) {
    Token operator = node.getOperator();
    TokenType operatorType = operator.getType();
    if (operatorType != TokenType.EQ) {
      operatorType = operatorFromCompoundAssignment(operatorType);
      Expression leftHandSide = node.getLeftHandSide();
      if (leftHandSide != null) {
        String methodName = operatorType.getLexeme();

        Type staticType = getStaticType(leftHandSide);
        MethodElement staticMethod = lookUpMethod(staticType, methodName);
        staticElementMap.put(node, staticMethod);

        Type propagatedType = getPropagatedType(leftHandSide);
        MethodElement propagatedMethod = lookUpMethod(propagatedType, methodName);
        node.setElement(select(staticMethod, propagatedMethod));

        if (shouldReportMissingMember(staticType, staticMethod)
            && (propagatedType == null || shouldReportMissingMember(
                propagatedType,
                propagatedMethod))) {
          resolver.reportError(
              StaticTypeWarningCode.UNDEFINED_METHOD,
              operator,
              methodName,
              staticType.getName());
        }
      }
    }
    return null;
  }

  @Override
  public Void visitBinaryExpression(BinaryExpression node) {
    Token operator = node.getOperator();
    if (operator.isUserDefinableOperator()) {
      Expression leftOperand = node.getLeftOperand();
      if (leftOperand != null) {
        String methodName = operator.getLexeme();

        Type staticType = getStaticType(leftOperand);
        MethodElement staticMethod = lookUpMethod(staticType, methodName);
        staticElementMap.put(node, staticMethod);

        Type propagatedType = getPropagatedType(leftOperand);
        MethodElement propagatedMethod = lookUpMethod(propagatedType, methodName);
        node.setElement(select(staticMethod, propagatedMethod));

        if (shouldReportMissingMember(staticType, staticMethod)
            && (propagatedType == null || shouldReportMissingMember(
                propagatedType,
                propagatedMethod))) {
          resolver.reportError(
              StaticWarningCode.UNDEFINED_OPERATOR,
              operator,
              methodName,
              staticType.getName());
        }
      }
    }
    return null;
  }

  @Override
  public Void visitBreakStatement(BreakStatement node) {
    SimpleIdentifier labelNode = node.getLabel();
    LabelElementImpl labelElement = lookupLabel(node, labelNode);
    if (labelElement != null && labelElement.isOnSwitchMember()) {
      resolver.reportError(ResolverErrorCode.BREAK_LABEL_ON_SWITCH_MEMBER, labelNode);
    }
    return null;
  }

  @Override
  public Void visitCommentReference(CommentReference node) {
    Identifier identifier = node.getIdentifier();
    if (identifier instanceof SimpleIdentifier) {
      SimpleIdentifier simpleIdentifier = (SimpleIdentifier) identifier;
      Element element = resolveSimpleIdentifier(simpleIdentifier);
      if (element == null) {
        //
        // This might be a reference to an imported name that is missing the prefix.
        //
        element = findImportWithoutPrefix(simpleIdentifier);
        if (element instanceof MultiplyDefinedElement) {
          // TODO(brianwilkerson) Report this error?
          element = null;
        }
      }
      if (element == null) {
        // TODO(brianwilkerson) Report this error?
//        resolver.reportError(
//            StaticWarningCode.UNDEFINED_IDENTIFIER,
//            simpleIdentifier,
//            simpleIdentifier.getName());
      } else {
        if (!element.getLibrary().equals(resolver.getDefiningLibrary())) {
          // TODO(brianwilkerson) Report this error?
        }
        recordResolution(simpleIdentifier, element);
        if (node.getNewKeyword() != null) {
          if (element instanceof ClassElement) {
            ConstructorElement constructor = ((ClassElement) element).getUnnamedConstructor();
            if (constructor == null) {
              // TODO(brianwilkerson) Report this error.
            } else {
              recordResolution(simpleIdentifier, constructor);
            }
          } else {
            // TODO(brianwilkerson) Report this error.
          }
        }
      }
    } else if (identifier instanceof PrefixedIdentifier) {
      PrefixedIdentifier prefixedIdentifier = (PrefixedIdentifier) identifier;
      SimpleIdentifier prefix = prefixedIdentifier.getPrefix();
      SimpleIdentifier name = prefixedIdentifier.getIdentifier();
      Element element = resolveSimpleIdentifier(prefix);
      if (element == null) {
//        resolver.reportError(StaticWarningCode.UNDEFINED_IDENTIFIER, prefix, prefix.getName());
      } else {
        if (element instanceof PrefixElement) {
          recordResolution(prefix, element);
          // TODO(brianwilkerson) The prefix needs to be resolved to the element for the import that
          // defines the prefix, not the prefix's element.

          // TODO(brianwilkerson) Report this error?
          element = resolver.getNameScope().lookup(identifier, resolver.getDefiningLibrary());
          recordResolution(name, element);
          return null;
        }
        if (!element.getLibrary().equals(resolver.getDefiningLibrary())) {
          // TODO(brianwilkerson) Report this error.
        }
        recordResolution(name, element);
        if (node.getNewKeyword() == null) {
          if (element instanceof ClassElement) {
            Element memberElement = lookupGetterOrMethod(
                ((ClassElement) element).getType(),
                name.getName());
            if (memberElement == null) {
              memberElement = ((ClassElement) element).getNamedConstructor(name.getName());
              if (memberElement == null) {
                memberElement = lookUpSetter(((ClassElement) element).getType(), name.getName());
              }
            }
            if (memberElement == null) {
//              reportGetterOrSetterNotFound(prefixedIdentifier, name, element.getDisplayName());
            } else {
              recordResolution(name, memberElement);
            }
          } else {
            // TODO(brianwilkerson) Report this error.
          }
        } else {
          if (element instanceof ClassElement) {
            ConstructorElement constructor = ((ClassElement) element).getNamedConstructor(name.getName());
            if (constructor == null) {
              // TODO(brianwilkerson) Report this error.
            } else {
              recordResolution(name, constructor);
            }
          } else {
            // TODO(brianwilkerson) Report this error.
          }
        }
      }
    }
    return null;
  }

  @Override
  public Void visitConstructorFieldInitializer(ConstructorFieldInitializer node) {
    SimpleIdentifier fieldName = node.getFieldName();
    ClassElement enclosingClass = resolver.getEnclosingClass();
    FieldElement fieldElement = ((ClassElementImpl) enclosingClass).getField(fieldName.getName());
    recordResolution(fieldName, fieldElement);
    if (fieldElement == null || fieldElement.isSynthetic()) {
      resolver.reportError(CompileTimeErrorCode.INITIALIZER_FOR_NON_EXISTANT_FIELD, node, fieldName);
    } else if (fieldElement.isStatic()) {
      resolver.reportError(CompileTimeErrorCode.INITIALIZER_FOR_STATIC_FIELD, node, fieldName);
    }
    return null;
  }

  @Override
  public Void visitConstructorName(ConstructorName node) {
    Type type = node.getType().getType();
    if (type != null && type.isDynamic()) {
      return null;
    } else if (!(type instanceof InterfaceType)) {
      // TODO(brianwilkerson) Report these errors.
      ASTNode parent = node.getParent();
      if (parent instanceof InstanceCreationExpression) {
        if (((InstanceCreationExpression) parent).isConst()) {
          // CompileTimeErrorCode.CONST_WITH_NON_TYPE
        } else {
          // StaticWarningCode.NEW_WITH_NON_TYPE
        }
      } else {
        // This is part of a redirecting factory constructor; not sure which error code to use
      }
      return null;
    }
    ClassElement classElement = ((InterfaceType) type).getElement();
    ConstructorElement constructor;
    SimpleIdentifier name = node.getName();
    if (name == null) {
      constructor = classElement.getUnnamedConstructor();
    } else {
      constructor = classElement.getNamedConstructor(name.getName());
      staticElementMap.put(name, constructor);
      name.setElement(constructor);
    }
    staticElementMap.put(node, constructor);
    node.setElement(constructor);
    return null;
  }

  @Override
  public Void visitContinueStatement(ContinueStatement node) {
    SimpleIdentifier labelNode = node.getLabel();
    LabelElementImpl labelElement = lookupLabel(node, labelNode);
    if (labelElement != null && labelElement.isOnSwitchStatement()) {
      resolver.reportError(ResolverErrorCode.CONTINUE_LABEL_ON_SWITCH, labelNode);
    }
    return null;
  }

  @Override
  public Void visitExportDirective(ExportDirective node) {
    Element element = node.getElement();
    if (element instanceof ExportElement) {
      // The element is null when the URI is invalid
      // TODO(brianwilkerson) Figure out whether the element can ever be something other than an
      // ExportElement
      resolveCombinators(((ExportElement) element).getExportedLibrary(), node.getCombinators());
    }
    return null;
  }

  @Override
  public Void visitFieldFormalParameter(FieldFormalParameter node) {
    String fieldName = node.getIdentifier().getName();
    ClassElement classElement = resolver.getEnclosingClass();
    if (classElement != null) {
      FieldElement fieldElement = ((ClassElementImpl) classElement).getField(fieldName);
      if (fieldElement == null) {
        resolver.reportError(
            CompileTimeErrorCode.INITIALIZING_FORMAL_FOR_NON_EXISTANT_FIELD,
            node,
            fieldName);
      } else {
        ParameterElement parameterElement = node.getElement();
        if (parameterElement instanceof FieldFormalParameterElementImpl) {
          FieldFormalParameterElementImpl fieldFormal = (FieldFormalParameterElementImpl) parameterElement;
          fieldFormal.setField(fieldElement);
          Type declaredType = fieldFormal.getType();
          Type fieldType = fieldElement.getType();
          if (node.getType() == null) {
            fieldFormal.setType(fieldType);
          }
          if (fieldElement.isSynthetic()) {
            resolver.reportError(
                CompileTimeErrorCode.INITIALIZING_FORMAL_FOR_NON_EXISTANT_FIELD,
                node,
                fieldName);
          } else if (fieldElement.isStatic()) {
            resolver.reportError(
                CompileTimeErrorCode.INITIALIZING_FORMAL_FOR_STATIC_FIELD,
                node,
                fieldName);
          } else if (declaredType != null && fieldType != null
              && !declaredType.isAssignableTo(fieldType)) {
            // TODO(brianwilkerson) We should implement a displayName() method for types that will
            // work nicely with function types and then use that below.
            resolver.reportError(
                StaticWarningCode.FIELD_INITIALIZER_WITH_INVALID_TYPE,
                node,
                declaredType.getName(),
                fieldType.getName());
          }
        } else {
          if (fieldElement.isSynthetic()) {
            resolver.reportError(
                CompileTimeErrorCode.INITIALIZING_FORMAL_FOR_NON_EXISTANT_FIELD,
                node,
                fieldName);
          } else if (fieldElement.isStatic()) {
            resolver.reportError(
                CompileTimeErrorCode.INITIALIZING_FORMAL_FOR_STATIC_FIELD,
                node,
                fieldName);
          }
        }
      }
    }
//    else {
//    // TODO(jwren) Report error, constructor initializer variable is a top level element
//    // (Either here or in ErrorVerifier#checkForAllFinalInitializedErrorCodes)
//    }
    return super.visitFieldFormalParameter(node);
  }

  @Override
  public Void visitFunctionExpressionInvocation(FunctionExpressionInvocation node) {
    // TODO(brianwilkerson) Can we ever resolve the function being invoked?
    //resolveNamedArguments(node.getArgumentList(), invokedFunction);
    return null;
  }

  @Override
  public Void visitImportDirective(ImportDirective node) {
    SimpleIdentifier prefixNode = node.getPrefix();
    if (prefixNode != null) {
      String prefixName = prefixNode.getName();
      for (PrefixElement prefixElement : resolver.getDefiningLibrary().getPrefixes()) {
        if (prefixElement.getDisplayName().equals(prefixName)) {
          recordResolution(prefixNode, prefixElement);
          break;
        }
      }
    }
    Element element = node.getElement();
    if (element instanceof ImportElement) {
      // The element is null when the URI is invalid
      // TODO(brianwilkerson) Figure out whether the element can ever be something other than an
      // ImportElement
      ImportElement importElement = (ImportElement) element;
      LibraryElement library = importElement.getImportedLibrary();
      if (library != null) {
        resolveCombinators(library, node.getCombinators());
      }
    }
    return null;
  }

  @Override
  public Void visitIndexExpression(IndexExpression node) {
    Expression target = node.getRealTarget();
    String methodName = getIndexOperator(node);

    Type staticType = getStaticType(target);
    MethodElement staticMethod = lookUpMethod(staticType, methodName);
    staticElementMap.put(node, staticMethod);

    Type propagatedType = getPropagatedType(target);
    MethodElement propagatedMethod = lookUpMethod(propagatedType, methodName);
    node.setElement(select(staticMethod, propagatedMethod));

    if (shouldReportMissingMember(staticType, staticMethod)
        && (propagatedType == null || shouldReportMissingMember(propagatedType, propagatedMethod))) {
      Token leftBracket = node.getLeftBracket();
      Token rightBracket = node.getRightBracket();
      if (leftBracket == null || rightBracket == null) {
        resolver.reportError(
            StaticWarningCode.UNDEFINED_OPERATOR,
            node,
            methodName,
            staticType.getName());
      } else {
        int offset = leftBracket.getOffset();
        resolver.reportError(StaticWarningCode.UNDEFINED_OPERATOR, offset, rightBracket.getOffset()
            - offset + 1, methodName, staticType.getName());
      }
    }
    return null;
  }

  @Override
  public Void visitInstanceCreationExpression(InstanceCreationExpression node) {
    ConstructorElement invokedConstructor = node.getConstructorName().getElement();
    staticElementMap.put(node, invokedConstructor);
    node.setElement(invokedConstructor);
    resolveNamedArguments(node.getArgumentList(), invokedConstructor);
    return null;
  }

  @Override
  public Void visitMethodInvocation(MethodInvocation node) {
    //
    // We have a method invocation of one of two forms: 'e.m(a1, ..., an)' or 'm(a1, ..., an)'. The
    // first step is to figure out which executable is being invoked, using both the static and the
    // propagated type information.
    //
    SimpleIdentifier methodName = node.getMethodName();
    Expression target = node.getRealTarget();
    Element staticElement;
    Element propagatedElement;
    if (target instanceof SuperExpression && !isSuperInValidContext((SuperExpression) target)) {
      return null;
    }
    if (target == null) {
      staticElement = resolveInvokedElement(methodName);
      propagatedElement = null;
    } else {
      Type targetType = getStaticType(target);
      staticElement = resolveInvokedElement(target, targetType, methodName);
      propagatedElement = resolveInvokedElement(target, getPropagatedType(target), methodName);
    }
    staticElement = convertSetterToGetter(staticElement);
    propagatedElement = convertSetterToGetter(propagatedElement);
    //
    // Record the results.
    //
    Element recordedElement = recordResolution(methodName, staticElement, propagatedElement);
    if (recordedElement instanceof PropertyAccessorElement) {
      //
      // This is an invocation of the call method defined on the value returned by the getter.
      //
      FunctionType getterType = ((PropertyAccessorElement) recordedElement).getType();
      if (getterType != null) {
        Type getterReturnType = getterType.getReturnType();
        if (getterReturnType instanceof InterfaceType) {
          MethodElement callMethod = ((InterfaceType) getterReturnType).lookUpMethod(
              CALL_METHOD_NAME,
              resolver.getDefiningLibrary());
          if (callMethod != null) {
            resolveNamedArguments(node.getArgumentList(), callMethod);
          }
        } else if (getterReturnType instanceof FunctionType) {
          Element functionElement = ((FunctionType) getterReturnType).getElement();
          if (functionElement instanceof ExecutableElement) {
            resolveNamedArguments(node.getArgumentList(), (ExecutableElement) functionElement);
          }
        }
      }
    } else if (recordedElement instanceof ExecutableElement) {
      resolveNamedArguments(node.getArgumentList(), (ExecutableElement) recordedElement);
    }
    //
    // Then check for error conditions.
    //
    ErrorCode errorCode;
    if (staticElement == null) {
      if (propagatedElement == null) {
        // TODO(brianwilkerson) It is possible that there is no element because we could not resolve
        // the target. In such a case we would like to suppress this error to reduce noise.
        errorCode = checkForInvocationError(target, staticElement);
      } else {
        errorCode = checkForInvocationError(target, propagatedElement);
      }
    } else {
      errorCode = checkForInvocationError(target, staticElement);
      if (propagatedElement != null) {
        ErrorCode propagatedError = checkForInvocationError(target, propagatedElement);
        errorCode = select(errorCode, propagatedError);
      }
    }
    if (errorCode == StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION) {
      resolver.reportError(
          StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION,
          methodName,
          methodName.getName());
    } else if (errorCode == StaticTypeWarningCode.UNDEFINED_FUNCTION) {
      resolver.reportError(
          StaticTypeWarningCode.UNDEFINED_FUNCTION,
          methodName,
          methodName.getName());
    } else if (errorCode == StaticTypeWarningCode.UNDEFINED_METHOD) {
      String targetTypeName;
      if (target == null) {
        ClassElement enclosingClass = resolver.getEnclosingClass();
        targetTypeName = enclosingClass.getDisplayName();
      } else {
        Type targetType = getPropagatedType(target);
        if (targetType == null) {
          targetType = getStaticType(target);
        }
        targetTypeName = targetType == null ? null : targetType.getName();
      }
      resolver.reportError(
          StaticTypeWarningCode.UNDEFINED_METHOD,
          methodName,
          methodName.getName(),
          targetTypeName);
    } else if (errorCode == StaticTypeWarningCode.UNDEFINED_SUPER_METHOD) {
      Type targetType = getPropagatedType(target);
      if (targetType == null) {
        targetType = getStaticType(target);
      }
      String targetTypeName = targetType == null ? null : targetType.getName();
      resolver.reportError(
          StaticTypeWarningCode.UNDEFINED_SUPER_METHOD,
          methodName,
          methodName.getName(),
          targetTypeName);
    }
    return null;
  }

  @Override
  public Void visitPostfixExpression(PostfixExpression node) {
    Expression operand = node.getOperand();
    String methodName = getPostfixOperator(node);

    Type staticType = getStaticType(operand);
    MethodElement staticMethod = lookUpMethod(staticType, methodName);
    staticElementMap.put(node, staticMethod);

    Type propagatedType = getPropagatedType(operand);
    MethodElement propagatedMethod = lookUpMethod(propagatedType, methodName);
    node.setElement(select(staticMethod, propagatedMethod));

    if (shouldReportMissingMember(staticType, staticMethod)
        && (propagatedType == null || shouldReportMissingMember(propagatedType, propagatedMethod))) {
      resolver.reportError(
          StaticWarningCode.UNDEFINED_OPERATOR,
          node.getOperator(),
          methodName,
          staticType.getName());
    }
    return null;
  }

  @Override
  public Void visitPrefixedIdentifier(PrefixedIdentifier node) {
    SimpleIdentifier prefix = node.getPrefix();
    SimpleIdentifier identifier = node.getIdentifier();
    //
    // First, check to see whether the prefix is really a prefix.
    //
    Element prefixElement = prefix.getElement();
    if (prefixElement instanceof PrefixElement) {
      Element element = resolver.getNameScope().lookup(node, resolver.getDefiningLibrary());
      if (element == null) {
        // TODO(brianwilkerson) Report this error.
        return null;
      }
      if (element instanceof PropertyAccessorElement && identifier.inSetterContext()) {
        PropertyInducingElement variable = ((PropertyAccessorElement) element).getVariable();
        if (variable != null) {
          PropertyAccessorElement setter = variable.getSetter();
          if (setter != null) {
            element = setter;
          }
        }
      }
      // TODO(brianwilkerson) The prefix needs to be resolved to the element for the import that
      // defines the prefix, not the prefix's element.
      recordResolution(identifier, element);
      return null;
    }
    //
    // Otherwise, the prefix is really an expression that happens to be a simple identifier and this
    // is really equivalent to a property access node.
    //
    resolvePropertyAccess(prefix, identifier);
    return null;
  }

  @Override
  public Void visitPrefixExpression(PrefixExpression node) {
    Token operator = node.getOperator();
    TokenType operatorType = operator.getType();
    if (operatorType.isUserDefinableOperator() || operatorType == TokenType.PLUS_PLUS
        || operatorType == TokenType.MINUS_MINUS) {
      Expression operand = node.getOperand();
      String methodName = getPrefixOperator(node);

      Type staticType = getStaticType(operand);
      MethodElement staticMethod = lookUpMethod(staticType, methodName);
      staticElementMap.put(node, staticMethod);

      Type propagatedType = getPropagatedType(operand);
      MethodElement propagatedMethod = lookUpMethod(propagatedType, methodName);
      node.setElement(select(staticMethod, propagatedMethod));

      if (shouldReportMissingMember(staticType, staticMethod)
          && (propagatedType == null || shouldReportMissingMember(propagatedType, propagatedMethod))) {
        resolver.reportError(
            StaticWarningCode.UNDEFINED_OPERATOR,
            operator,
            methodName,
            staticType.getName());
      }
    }
    return null;
  }

  @Override
  public Void visitPropertyAccess(PropertyAccess node) {
    Expression target = node.getRealTarget();
    if (target instanceof SuperExpression && !isSuperInValidContext((SuperExpression) target)) {
      return null;
    }
    SimpleIdentifier propertyName = node.getPropertyName();
    resolvePropertyAccess(target, propertyName);
    return null;
  }

  @Override
  public Void visitRedirectingConstructorInvocation(RedirectingConstructorInvocation node) {
    ClassElement enclosingClass = resolver.getEnclosingClass();
    if (enclosingClass == null) {
      // TODO(brianwilkerson) Report this error.
      return null;
    }
    SimpleIdentifier name = node.getConstructorName();
    ConstructorElement element;
    if (name == null) {
      element = enclosingClass.getUnnamedConstructor();
    } else {
      element = enclosingClass.getNamedConstructor(name.getName());
    }
    if (element == null) {
      // TODO(brianwilkerson) Report this error and decide what element to associate with the node.
      return null;
    }
    if (name != null) {
      recordResolution(name, element);
    }
    staticElementMap.put(node, element);
    node.setElement(element);
    resolveNamedArguments(node.getArgumentList(), element);
    return null;
  }

  @Override
  public Void visitSimpleIdentifier(SimpleIdentifier node) {
    //
    // We ignore identifiers that have already been resolved, such as identifiers representing the
    // name in a declaration.
    //
    if (node.getElement() != null) {
      return null;
    }
    //
    // Otherwise, the node should be resolved.
    //
    Element element = resolveSimpleIdentifier(node);
    if (element == null) {
      // TODO(brianwilkerson) Recover from this error.
      if (isConstructorReturnType(node)) {
        resolver.reportError(CompileTimeErrorCode.INVALID_CONSTRUCTOR_NAME, node);
      } else if (!classDeclaresNoSuchMethod(resolver.getEnclosingClass())) {
        resolver.reportError(StaticWarningCode.UNDEFINED_IDENTIFIER, node, node.getName());
      }
    } else if (element instanceof ExecutableElement) {
      staticElementMap.put(node, (ExecutableElement) element);
    }
    recordResolution(node, element);
    return null;
  }

  @Override
  public Void visitSuperConstructorInvocation(SuperConstructorInvocation node) {
    ClassElement enclosingClass = resolver.getEnclosingClass();
    if (enclosingClass == null) {
      // TODO(brianwilkerson) Report this error.
      return null;
    }
    ClassElement superclass = getSuperclass(enclosingClass);
    if (superclass == null) {
      // TODO(brianwilkerson) Report this error.
      return null;
    }
    SimpleIdentifier name = node.getConstructorName();
    ConstructorElement element;
    if (name == null) {
      element = superclass.getUnnamedConstructor();
    } else {
      element = superclass.getNamedConstructor(name.getName());
    }
    if (element == null) {
      // TODO(brianwilkerson) Report this error and decide what element to associate with the node.
      return null;
    }
    if (name != null) {
      recordResolution(name, element);
    }
    staticElementMap.put(node, element);
    node.setElement(element);
    resolveNamedArguments(node.getArgumentList(), element);
    return null;
  }

  @Override
  public Void visitSuperExpression(SuperExpression node) {
    if (!isSuperInValidContext(node)) {
      resolver.reportError(CompileTimeErrorCode.SUPER_IN_INVALID_CONTEXT, node);
    }
    return super.visitSuperExpression(node);
  }

  @Override
  public Void visitTypeParameter(TypeParameter node) {
    TypeName bound = node.getBound();
    if (bound != null) {
      TypeVariableElementImpl variable = (TypeVariableElementImpl) node.getName().getElement();
      if (variable != null) {
        variable.setBound(bound.getType());
      }
    }
    return null;
  }

  /**
   * Given that we have found code to invoke the given element, return the error code that should be
   * reported, or {@code null} if no error should be reported.
   * 
   * @param target the target of the invocation, or {@code null} if there was no target
   * @param element the element to be invoked
   * @return the error code that should be reported
   */
  private ErrorCode checkForInvocationError(Expression target, Element element) {
    if (element instanceof PropertyAccessorElement) {
      //
      // This is really a function expression invocation.
      //
      // TODO(brianwilkerson) Consider the possibility of re-writing the AST.
      FunctionType getterType = ((PropertyAccessorElement) element).getType();
      if (getterType != null) {
        Type returnType = getterType.getReturnType();
        if (!isExecutableType(returnType)) {
          return StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION;
        }
      }
    } else if (element instanceof ExecutableElement) {
      return null;
    } else if (element == null && target instanceof SuperExpression) {
      // TODO(jwren) We should split the UNDEFINED_METHOD into two error codes, this one, and
      // a code that describes the situation where the method was found, but it was not
      // accessible from the current library.
      return StaticTypeWarningCode.UNDEFINED_SUPER_METHOD;
    } else {
      //
      // This is really a function expression invocation.
      //
      // TODO(brianwilkerson) Consider the possibility of re-writing the AST.
      if (element instanceof PropertyInducingElement) {
        PropertyAccessorElement getter = ((PropertyInducingElement) element).getGetter();
        FunctionType getterType = getter.getType();
        if (getterType != null) {
          Type returnType = getterType.getReturnType();
          if (!isExecutableType(returnType)) {
            return StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION;
          }
        }
      } else if (element instanceof VariableElement) {
        Type variableType = resolver.getOverrideManager().getType(element);
        if (variableType == null) {
          variableType = ((VariableElement) element).getType();
        }
        if (!isExecutableType(variableType)) {
          return StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION;
        }
      } else {
        if (target == null) {
          ClassElement enclosingClass = resolver.getEnclosingClass();
          if (enclosingClass == null) {
            return StaticTypeWarningCode.UNDEFINED_FUNCTION;
          } else if (element == null) {
            if (!classDeclaresNoSuchMethod(enclosingClass)) {
              return StaticTypeWarningCode.UNDEFINED_METHOD;
            }
          } else {
            return StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION;
          }
        } else {
          Type targetType = getStaticType(target);
          if (targetType == null) {
            return StaticTypeWarningCode.UNDEFINED_FUNCTION;
          } else if (!targetType.isDynamic() && !classDeclaresNoSuchMethod(targetType.getElement())) {
            return StaticTypeWarningCode.UNDEFINED_METHOD;
          }
        }
      }
    }
    return null;
  }

  /**
   * Return {@code true} if the given class declares a method named "noSuchMethod" and is not the
   * class 'Object'.
   * 
   * @param element the class being tested
   * @return {@code true} if the given class declares a method named "noSuchMethod"
   */
  private boolean classDeclaresNoSuchMethod(ClassElement classElement) {
    if (classElement == null) {
      return false;
    }
    MethodElement methodElement = classElement.lookUpMethod(
        NO_SUCH_METHOD_METHOD_NAME,
        resolver.getDefiningLibrary());
    return methodElement != null && methodElement.getEnclosingElement().getSupertype() != null;
  }

  /**
   * Return {@code true} if the given element represents a class that declares a method named
   * "noSuchMethod" and is not the class 'Object'.
   * 
   * @param element the element being tested
   * @return {@code true} if the given element represents a class that declares a method named
   *         "noSuchMethod"
   */
  private boolean classDeclaresNoSuchMethod(Element element) {
    if (element instanceof ClassElement) {
      return classDeclaresNoSuchMethod((ClassElement) element);
    }
    return false;
  }

  /**
   * If the given element is a setter, return the getter associated with it. Otherwise, return the
   * element unchanged.
   * 
   * @param element the element to be normalized
   * @return a non-setter element derived from the given element
   */
  private Element convertSetterToGetter(Element element) {
    // TODO(brianwilkerson) Determine whether and why the element could ever be a setter.
    if (element instanceof PropertyAccessorElement) {
      return ((PropertyAccessorElement) element).getVariable().getGetter();
    }
    return element;
  }

  /**
   * Look for any declarations of the given identifier that are imported using a prefix. Return the
   * element that was found, or {@code null} if the name is not imported using a prefix.
   * 
   * @param identifier the identifier that might have been imported using a prefix
   * @return the element that was found
   */
  private Element findImportWithoutPrefix(SimpleIdentifier identifier) {
    Element element = null;
    Scope nameScope = resolver.getNameScope();
    LibraryElement definingLibrary = resolver.getDefiningLibrary();
    for (ImportElement importElement : definingLibrary.getImports()) {
      PrefixElement prefixElement = importElement.getPrefix();
      if (prefixElement != null) {
        Identifier prefixedIdentifier = new SyntheticIdentifier(prefixElement.getName() + "."
            + identifier.getName());
        Element importedElement = nameScope.lookup(prefixedIdentifier, definingLibrary);
        if (importedElement != null) {
          if (element == null) {
            element = importedElement;
          } else {
            element = new MultiplyDefinedElementImpl(
                definingLibrary.getContext(),
                element,
                importedElement);
          }
        }
      }
    }
    return element;
  }

  /**
   * Search through the array of parameters for a parameter whose name matches the given name.
   * Return the parameter with the given name, or {@code null} if there is no such parameter.
   * 
   * @param parameters the parameters being searched
   * @param name the name being searched for
   * @return the parameter with the given name
   */
  private ParameterElement findNamedParameter(ParameterElement[] parameters, String name) {
    for (ParameterElement parameter : parameters) {
      if (parameter.getParameterKind() == ParameterKind.NAMED) {
        String parameterName = parameter.getName();
        if (parameterName != null && parameterName.equals(name)) {
          return parameter;
        }
      }
    }
    return null;
  }

  /**
   * Return the name of the method invoked by the given index expression.
   * 
   * @param node the index expression being invoked
   * @return the name of the method invoked by the expression
   */
  private String getIndexOperator(IndexExpression node) {
    return (node.inSetterContext()) ? TokenType.INDEX_EQ.getLexeme() : TokenType.INDEX.getLexeme();
  }

  /**
   * Return the name of the method invoked by the given postfix expression.
   * 
   * @param node the postfix expression being invoked
   * @return the name of the method invoked by the expression
   */
  private String getPostfixOperator(PostfixExpression node) {
    return (node.getOperator().getType() == TokenType.PLUS_PLUS) ? TokenType.PLUS.getLexeme()
        : TokenType.MINUS.getLexeme();
  }

  /**
   * Return the name of the method invoked by the given postfix expression.
   * 
   * @param node the postfix expression being invoked
   * @return the name of the method invoked by the expression
   */
  private String getPrefixOperator(PrefixExpression node) {
    Token operator = node.getOperator();
    TokenType operatorType = operator.getType();
    if (operatorType == TokenType.PLUS_PLUS) {
      return TokenType.PLUS.getLexeme();
    } else if (operatorType == TokenType.MINUS_MINUS) {
      return TokenType.MINUS.getLexeme();
    } else if (operatorType == TokenType.MINUS) {
      return "unary-";
    } else {
      return operator.getLexeme();
    }
  }

  /**
   * Return the propagated type of the given expression that is to be used for type analysis.
   * 
   * @param expression the expression whose type is to be returned
   * @return the type of the given expression
   */
  private Type getPropagatedType(Expression expression) {
    Type propagatedType = resolveTypeVariable(expression.getPropagatedType());
    if (propagatedType instanceof FunctionType) {
      //
      // All function types are subtypes of 'Function', which is itself a subclass of 'Object'.
      //
      propagatedType = resolver.getTypeProvider().getFunctionType();
    }
    return propagatedType;
  }

  /**
   * Return the static type of the given expression that is to be used for type analysis.
   * 
   * @param expression the expression whose type is to be returned
   * @return the type of the given expression
   */
  private Type getStaticType(Expression expression) {
    if (expression instanceof NullLiteral) {
      return resolver.getTypeProvider().getObjectType();
    }
    Type staticType = resolveTypeVariable(expression.getStaticType());
    if (staticType instanceof FunctionType) {
      //
      // All function types are subtypes of 'Function', which is itself a subclass of 'Object'.
      //
      staticType = resolver.getTypeProvider().getFunctionType();
    }
    return staticType;
  }

  /**
   * Return the element representing the superclass of the given class.
   * 
   * @param targetClass the class whose superclass is to be returned
   * @return the element representing the superclass of the given class
   */
  private ClassElement getSuperclass(ClassElement targetClass) {
    InterfaceType superType = targetClass.getSupertype();
    if (superType == null) {
      return null;
    }
    return superType.getElement();
  }

  /**
   * Return {@code true} if the given type represents an object that could be invoked using the call
   * operator '()'.
   * 
   * @param type the type being tested
   * @return {@code true} if the given type represents an object that could be invoked
   */
  private boolean isExecutableType(Type type) {
    if (type.isDynamic() || (type instanceof FunctionType) || type.isDartCoreFunction()) {
      return true;
    } else if (type instanceof InterfaceType) {
      ClassElement classElement = ((InterfaceType) type).getElement();
      MethodElement methodElement = classElement.lookUpMethod(
          CALL_METHOD_NAME,
          resolver.getDefiningLibrary());
      return methodElement != null;
    }
    return false;
  }

  /**
   * Return {@code true} if the given element is a static element.
   * 
   * @param element the element being tested
   * @return {@code true} if the given element is a static element
   */
  private boolean isStatic(Element element) {
    if (element instanceof ExecutableElement) {
      return ((ExecutableElement) element).isStatic();
    } else if (element instanceof PropertyInducingElement) {
      return ((PropertyInducingElement) element).isStatic();
    }
    return false;
  }

  /**
   * Look up the getter with the given name in the given type. Return the element representing the
   * getter that was found, or {@code null} if there is no getter with the given name.
   * 
   * @param type the type in which the getter is defined
   * @param getterName the name of the getter being looked up
   * @return the element representing the getter that was found
   */
  private PropertyAccessorElement lookUpGetter(Type type, String getterName) {
    type = resolveTypeVariable(type);
    if (type instanceof InterfaceType) {
      InterfaceType interfaceType = (InterfaceType) type;
      PropertyAccessorElement accessor = interfaceType.lookUpGetter(
          getterName,
          resolver.getDefiningLibrary());
      if (accessor != null) {
        return accessor;
      }
      return lookUpGetterInInterfaces(interfaceType, getterName, new HashSet<ClassElement>());
    }
    return null;
  }

  /**
   * Look up the getter with the given name in the interfaces implemented by the given type, either
   * directly or indirectly. Return the element representing the getter that was found, or
   * {@code null} if there is no getter with the given name.
   * 
   * @param targetType the type in which the getter might be defined
   * @param getterName the name of the getter being looked up
   * @param visitedInterfaces a set containing all of the interfaces that have been examined, used
   *          to prevent infinite recursion and to optimize the search
   * @return the element representing the getter that was found
   */
  private PropertyAccessorElement lookUpGetterInInterfaces(InterfaceType targetType,
      String getterName, HashSet<ClassElement> visitedInterfaces) {
    // TODO(brianwilkerson) This isn't correct. Section 8.1.1 of the specification (titled
    // "Inheritance and Overriding" under "Interfaces") describes a much more complex scheme for
    // finding the inherited member. We need to follow that scheme. The code below should cover the
    // 80% case.
    ClassElement targetClass = targetType.getElement();
    if (visitedInterfaces.contains(targetClass)) {
      return null;
    }
    visitedInterfaces.add(targetClass);
    PropertyAccessorElement getter = targetType.getGetter(getterName);
    if (getter != null) {
      return getter;
    }
    for (InterfaceType interfaceType : targetType.getInterfaces()) {
      getter = lookUpGetterInInterfaces(interfaceType, getterName, visitedInterfaces);
      if (getter != null) {
        return getter;
      }
    }
    InterfaceType superclass = targetType.getSuperclass();
    if (superclass == null) {
      return null;
    }
    return lookUpGetterInInterfaces(superclass, getterName, visitedInterfaces);
  }

  /**
   * Look up the method or getter with the given name in the given type. Return the element
   * representing the method or getter that was found, or {@code null} if there is no method or
   * getter with the given name.
   * 
   * @param type the type in which the method or getter is defined
   * @param memberName the name of the method or getter being looked up
   * @return the element representing the method or getter that was found
   */
  private ExecutableElement lookupGetterOrMethod(Type type, String memberName) {
    type = resolveTypeVariable(type);
    if (type instanceof InterfaceType) {
      InterfaceType interfaceType = (InterfaceType) type;
      ExecutableElement member = interfaceType.lookUpMethod(
          memberName,
          resolver.getDefiningLibrary());
      if (member != null) {
        return member;
      }
      member = interfaceType.lookUpGetter(memberName, resolver.getDefiningLibrary());
      if (member != null) {
        return member;
      }
      return lookUpGetterOrMethodInInterfaces(
          interfaceType,
          memberName,
          new HashSet<ClassElement>());
    }
    return null;
  }

  /**
   * Look up the method or getter with the given name in the interfaces implemented by the given
   * type, either directly or indirectly. Return the element representing the method or getter that
   * was found, or {@code null} if there is no method or getter with the given name.
   * 
   * @param targetType the type in which the method or getter might be defined
   * @param memberName the name of the method or getter being looked up
   * @param visitedInterfaces a set containing all of the interfaces that have been examined, used
   *          to prevent infinite recursion and to optimize the search
   * @return the element representing the method or getter that was found
   */
  private ExecutableElement lookUpGetterOrMethodInInterfaces(InterfaceType targetType,
      String memberName, HashSet<ClassElement> visitedInterfaces) {
    // TODO(brianwilkerson) This isn't correct. Section 8.1.1 of the specification (titled
    // "Inheritance and Overriding" under "Interfaces") describes a much more complex scheme for
    // finding the inherited member. We need to follow that scheme. The code below should cover the
    // 80% case.
    ClassElement targetClass = targetType.getElement();
    if (visitedInterfaces.contains(targetClass)) {
      return null;
    }
    visitedInterfaces.add(targetClass);
    ExecutableElement member = targetType.getMethod(memberName);
    if (member != null) {
      return member;
    }
    member = targetType.getGetter(memberName);
    if (member != null) {
      return member;
    }
    for (InterfaceType interfaceType : targetType.getInterfaces()) {
      member = lookUpGetterOrMethodInInterfaces(interfaceType, memberName, visitedInterfaces);
      if (member != null) {
        return member;
      }
    }
    InterfaceType superclass = targetType.getSuperclass();
    if (superclass == null) {
      return null;
    }
    return lookUpGetterInInterfaces(superclass, memberName, visitedInterfaces);
  }

  /**
   * Find the element corresponding to the given label node in the current label scope.
   * 
   * @param parentNode the node containing the given label
   * @param labelNode the node representing the label being looked up
   * @return the element corresponding to the given label node in the current scope
   */
  private LabelElementImpl lookupLabel(ASTNode parentNode, SimpleIdentifier labelNode) {
    LabelScope labelScope = resolver.getLabelScope();
    LabelElementImpl labelElement = null;
    if (labelNode == null) {
      if (labelScope == null) {
        // TODO(brianwilkerson) Do we need to report this error, or is this condition always caught in the parser?
        // reportError(ResolverErrorCode.BREAK_OUTSIDE_LOOP);
      } else {
        labelElement = (LabelElementImpl) labelScope.lookup(LabelScope.EMPTY_LABEL);
        if (labelElement == null) {
          // TODO(brianwilkerson) Do we need to report this error, or is this condition always caught in the parser?
          // reportError(ResolverErrorCode.BREAK_OUTSIDE_LOOP);
        }
        //
        // The label element that was returned was a marker for look-up and isn't stored in the
        // element model.
        //
        labelElement = null;
      }
    } else {
      if (labelScope == null) {
        resolver.reportError(CompileTimeErrorCode.LABEL_UNDEFINED, labelNode, labelNode.getName());
      } else {
        labelElement = (LabelElementImpl) labelScope.lookup(labelNode);
        if (labelElement == null) {
          resolver.reportError(CompileTimeErrorCode.LABEL_UNDEFINED, labelNode, labelNode.getName());
        } else {
          recordResolution(labelNode, labelElement);
        }
      }
    }
    if (labelElement != null) {
      ExecutableElement labelContainer = labelElement.getAncestor(ExecutableElement.class);
      if (labelContainer != resolver.getEnclosingFunction()) {
        resolver.reportError(
            CompileTimeErrorCode.LABEL_IN_OUTER_SCOPE,
            labelNode,
            labelNode.getName());
        labelElement = null;
      }
    }
    return labelElement;
  }

  /**
   * Look up the method with the given name in the given type. Return the element representing the
   * method that was found, or {@code null} if there is no method with the given name.
   * 
   * @param type the type in which the method is defined
   * @param methodName the name of the method being looked up
   * @return the element representing the method that was found
   */
  private MethodElement lookUpMethod(Type type, String methodName) {
    type = resolveTypeVariable(type);
    if (type instanceof InterfaceType) {
      InterfaceType interfaceType = (InterfaceType) type;
      MethodElement method = interfaceType.lookUpMethod(methodName, resolver.getDefiningLibrary());
      if (method != null) {
        return method;
      }
      return lookUpMethodInInterfaces(interfaceType, methodName, new HashSet<ClassElement>());
    }
    return null;
  }

  /**
   * Look up the method with the given name in the interfaces implemented by the given type, either
   * directly or indirectly. Return the element representing the method that was found, or
   * {@code null} if there is no method with the given name.
   * 
   * @param targetType the type in which the member might be defined
   * @param methodName the name of the method being looked up
   * @param visitedInterfaces a set containing all of the interfaces that have been examined, used
   *          to prevent infinite recursion and to optimize the search
   * @return the element representing the method that was found
   */
  private MethodElement lookUpMethodInInterfaces(InterfaceType targetType, String methodName,
      HashSet<ClassElement> visitedInterfaces) {
    // TODO(brianwilkerson) This isn't correct. Section 8.1.1 of the specification (titled
    // "Inheritance and Overriding" under "Interfaces") describes a much more complex scheme for
    // finding the inherited member. We need to follow that scheme. The code below should cover the
    // 80% case.
    ClassElement targetClass = targetType.getElement();
    if (visitedInterfaces.contains(targetClass)) {
      return null;
    }
    visitedInterfaces.add(targetClass);
    MethodElement method = targetType.getMethod(methodName);
    if (method != null) {
      return method;
    }
    for (InterfaceType interfaceType : targetType.getInterfaces()) {
      method = lookUpMethodInInterfaces(interfaceType, methodName, visitedInterfaces);
      if (method != null) {
        return method;
      }
    }
    InterfaceType superclass = targetType.getSuperclass();
    if (superclass == null) {
      return null;
    }
    return lookUpMethodInInterfaces(superclass, methodName, visitedInterfaces);
  }

  /**
   * Look up the setter with the given name in the given type. Return the element representing the
   * setter that was found, or {@code null} if there is no setter with the given name.
   * 
   * @param type the type in which the setter is defined
   * @param setterName the name of the setter being looked up
   * @return the element representing the setter that was found
   */
  private PropertyAccessorElement lookUpSetter(Type type, String setterName) {
    type = resolveTypeVariable(type);
    if (type instanceof InterfaceType) {
      InterfaceType interfaceType = (InterfaceType) type;
      PropertyAccessorElement accessor = interfaceType.lookUpSetter(
          setterName,
          resolver.getDefiningLibrary());
      if (accessor != null) {
        return accessor;
      }
      return lookUpSetterInInterfaces(interfaceType, setterName, new HashSet<ClassElement>());
    }
    return null;
  }

  /**
   * Look up the setter with the given name in the interfaces implemented by the given type, either
   * directly or indirectly. Return the element representing the setter that was found, or
   * {@code null} if there is no setter with the given name.
   * 
   * @param targetType the type in which the setter might be defined
   * @param setterName the name of the setter being looked up
   * @param visitedInterfaces a set containing all of the interfaces that have been examined, used
   *          to prevent infinite recursion and to optimize the search
   * @return the element representing the setter that was found
   */
  private PropertyAccessorElement lookUpSetterInInterfaces(InterfaceType targetType,
      String setterName, HashSet<ClassElement> visitedInterfaces) {
    // TODO(brianwilkerson) This isn't correct. Section 8.1.1 of the specification (titled
    // "Inheritance and Overriding" under "Interfaces") describes a much more complex scheme for
    // finding the inherited member. We need to follow that scheme. The code below should cover the
    // 80% case.
    ClassElement targetClass = targetType.getElement();
    if (visitedInterfaces.contains(targetClass)) {
      return null;
    }
    visitedInterfaces.add(targetClass);
    PropertyAccessorElement setter = targetType.getSetter(setterName);
    if (setter != null) {
      return setter;
    }
    for (InterfaceType interfaceType : targetType.getInterfaces()) {
      setter = lookUpSetterInInterfaces(interfaceType, setterName, visitedInterfaces);
      if (setter != null) {
        return setter;
      }
    }
    InterfaceType superclass = targetType.getSuperclass();
    if (superclass == null) {
      return null;
    }
    return lookUpSetterInInterfaces(superclass, setterName, visitedInterfaces);
  }

  /**
   * Return the binary operator that is invoked by the given compound assignment operator.
   * 
   * @param operator the assignment operator being mapped
   * @return the binary operator that invoked by the given assignment operator
   */
  private TokenType operatorFromCompoundAssignment(TokenType operator) {
    switch (operator) {
      case AMPERSAND_EQ:
        return TokenType.AMPERSAND;
      case BAR_EQ:
        return TokenType.BAR;
      case CARET_EQ:
        return TokenType.CARET;
      case GT_GT_EQ:
        return TokenType.GT_GT;
      case LT_LT_EQ:
        return TokenType.LT_LT;
      case MINUS_EQ:
        return TokenType.MINUS;
      case PERCENT_EQ:
        return TokenType.PERCENT;
      case PLUS_EQ:
        return TokenType.PLUS;
      case SLASH_EQ:
        return TokenType.SLASH;
      case STAR_EQ:
        return TokenType.STAR;
      case TILDE_SLASH_EQ:
        return TokenType.TILDE_SLASH;
    }
    // Internal error: Unmapped assignment operator.
    AnalysisEngine.getInstance().getLogger().logError(
        "Failed to map " + operator.getLexeme() + " to it's corresponding operator");
    return operator;
  }

  /**
   * Record the fact that the given AST node was resolved to the given element.
   * 
   * @param node the AST node that was resolved
   * @param element the element to which the AST node was resolved
   */
  private void recordResolution(SimpleIdentifier node, Element element) {
    if (element != null) {
      if (element instanceof ExecutableElement) {
        staticElementMap.put(node, (ExecutableElement) element);
      }
      node.setElement(element);
    }
  }

  /**
   * Record the fact that the given AST node was resolved to the given elements.
   * 
   * @param node the AST node that was resolved
   * @param staticElement the element to which the AST node was resolved using static type
   *          information
   * @param propagatedElement the element to which the AST node was resolved using propagated type
   *          information
   * @return the element that was associated with the node
   */
  private Element recordResolution(SimpleIdentifier node, Element staticElement,
      Element propagatedElement) {
    if (staticElement instanceof ExecutableElement) {
      staticElementMap.put(node, (ExecutableElement) staticElement);
    }
    Element element = propagatedElement == null ? staticElement : propagatedElement;
    node.setElement(element);
    return element;
  }

  /**
   * Report the {@link StaticTypeWarningCode}s <code>UNDEFINED_SETTER</code> and
   * <code>UNDEFINED_GETTER</code>.
   * 
   * @param node the prefixed identifier that gives the context to determine if the error on the
   *          undefined identifier is a getter or a setter
   * @param identifier the identifier in the passed prefix identifier
   * @param typeName the name of the type of the left hand side of the passed prefixed identifier
   */
  private void reportGetterOrSetterNotFound(PrefixedIdentifier node, SimpleIdentifier identifier,
      String typeName) {
    // This method is only invoked when the prefixed identifier is effectively a property access.
    Type staticTargetType = getStaticType(node.getPrefix());
    Type propagatedTargetType = getPropagatedType(node.getPrefix());
    if ((staticTargetType == null || staticTargetType.isDynamic())
        && (propagatedTargetType == null || propagatedTargetType.isDynamic())) {
      return;
    }
    boolean staticNoSuchMethod = staticTargetType != null
        && classDeclaresNoSuchMethod(staticTargetType.getElement());
    boolean propagatedNoSuchMethod = propagatedTargetType != null
        && classDeclaresNoSuchMethod(propagatedTargetType.getElement());
    if (!staticNoSuchMethod && !propagatedNoSuchMethod) {
      // TODO(jwren) This needs to be modified to also generate the error code StaticTypeWarningCode.INACCESSIBLE_SETTER
      boolean isSetterContext = node.getIdentifier().inSetterContext();
      ErrorCode errorCode = isSetterContext ? StaticTypeWarningCode.UNDEFINED_SETTER
          : StaticTypeWarningCode.UNDEFINED_GETTER;
      resolver.reportError(errorCode, identifier, identifier.getName(), typeName);
    }
  }

  /**
   * Resolve the names in the given combinators in the scope of the given library.
   * 
   * @param library the library that defines the names
   * @param combinators the combinators containing the names to be resolved
   */
  private void resolveCombinators(LibraryElement library, NodeList<Combinator> combinators) {
    if (library == null) {
      //
      // The library will be null if the directive containing the combinators has a URI that is not
      // valid.
      //
      return;
    }
    Namespace namespace = new NamespaceBuilder().createExportNamespace(library);
    for (Combinator combinator : combinators) {
      NodeList<SimpleIdentifier> names;
      if (combinator instanceof HideCombinator) {
        names = ((HideCombinator) combinator).getHiddenNames();
      } else {
        names = ((ShowCombinator) combinator).getShownNames();
      }
      for (SimpleIdentifier name : names) {
        Element element = namespace.get(name.getName());
        if (element != null) {
          name.setElement(element);
        }
      }
    }
  }

  /**
   * Given an invocation of the form 'e.m(a1, ..., an)', resolve 'e.m' to the element being invoked.
   * If the returned element is a method, then the method will be invoked. If the returned element
   * is a getter, the getter will be invoked without arguments and the result of that invocation
   * will then be invoked with the arguments.
   * 
   * @param target the target of the invocation ('e')
   * @param targetType the type of the target
   * @param methodName the name of the method being invoked ('m')
   * @return the element being invoked
   */
  private Element resolveInvokedElement(Expression target, Type targetType,
      SimpleIdentifier methodName) {
    if (targetType instanceof InterfaceType) {
      InterfaceType classType = (InterfaceType) targetType;
      Element element = lookUpMethod(classType, methodName.getName());
      if (element == null) {
        //
        // If there's no method, then it's possible that 'm' is a getter that returns a function.
        //
        element = classType.getGetter(methodName.getName());
      }
      return element;
    } else if (target instanceof SimpleIdentifier) {
      Element targetElement = ((SimpleIdentifier) target).getElement();
      if (targetElement instanceof PrefixElement) {
        //
        // Look to see whether the name of the method is really part of a prefixed identifier for an
        // imported top-level function or top-level getter that returns a function.
        //
        final String name = ((SimpleIdentifier) target).getName() + "." + methodName;
        Identifier functionName = new SyntheticIdentifier(name);
        Element element = resolver.getNameScope().lookup(
            functionName,
            resolver.getDefiningLibrary());
        if (element != null) {
          // TODO(brianwilkerson) This isn't a method invocation, it's a function invocation where
          // the function name is a prefixed identifier. Consider re-writing the AST.
          return element;
        }
      }
      //return targetElement;
    }
    return null;
  }

  /**
   * Given an invocation of the form 'm(a1, ..., an)', resolve 'm' to the element being invoked. If
   * the returned element is a method, then the method will be invoked. If the returned element is a
   * getter, the getter will be invoked without arguments and the result of that invocation will
   * then be invoked with the arguments.
   * 
   * @param methodName the name of the method being invoked ('m')
   * @return the element being invoked
   */
  private Element resolveInvokedElement(SimpleIdentifier methodName) {
    //
    // Look first in the lexical scope.
    //
    Element element = resolver.getNameScope().lookup(methodName, resolver.getDefiningLibrary());
    if (element == null) {
      //
      // If it isn't defined in the lexical scope, and the invocation is within a class, then look
      // in the inheritance scope.
      //
      ClassElement enclosingClass = resolver.getEnclosingClass();
      if (enclosingClass != null) {
        InterfaceType enclosingType = enclosingClass.getType();
        element = lookUpMethod(enclosingType, methodName.getName());
        if (element == null) {
          //
          // If there's no method, then it's possible that 'm' is a getter that returns a function.
          //
          element = lookUpGetter(enclosingType, methodName.getName());
        }
      }
    }
    return element;
  }

  /**
   * Resolve the names associated with any named arguments to the parameter elements named by the
   * argument.
   * 
   * @param argumentList the arguments to be resolved
   * @param invokedMethod the method or function defining the parameters to which the named
   *          arguments are to be resolved
   */
  private void resolveNamedArguments(ArgumentList argumentList, ExecutableElement invokedMethod) {
    if (invokedMethod == null) {
      return;
    }
    ParameterElement[] parameters = invokedMethod.getParameters();
    for (Expression argument : argumentList.getArguments()) {
      if (argument instanceof NamedExpression) {
        SimpleIdentifier name = ((NamedExpression) argument).getName().getLabel();
        ParameterElement parameter = findNamedParameter(parameters, name.getName());
        if (parameter != null) {
          recordResolution(name, parameter);
        }
      }
    }
  }

  /**
   * Given that we are accessing a property of the given type with the given name, return the
   * element that represents the property.
   * 
   * @param targetType the type in which the search for the property should begin
   * @param propertyName the name of the property being accessed
   * @return the element that represents the property
   */
  private ExecutableElement resolveProperty(Type targetType, SimpleIdentifier propertyName) {
    ExecutableElement memberElement = null;
    if (propertyName.inSetterContext()) {
      memberElement = lookUpSetter(targetType, propertyName.getName());
    }
    if (memberElement == null) {
      memberElement = lookUpGetter(targetType, propertyName.getName());
    }
    if (memberElement == null) {
      memberElement = lookUpMethod(targetType, propertyName.getName());
    }
    return memberElement;
  }

  private void resolvePropertyAccess(Expression target, SimpleIdentifier propertyName) {
    Type staticType = getStaticType(target);
    ExecutableElement staticElement = resolveProperty(staticType, propertyName);
    staticElementMap.put(propertyName, staticElement);

    Type propagatedType = getPropagatedType(target);
    ExecutableElement propagatedElement = resolveProperty(propagatedType, propertyName);
    Element selectedElement = select(staticElement, propagatedElement);
    propertyName.setElement(selectedElement);

    if (shouldReportMissingMember(staticType, staticElement)
        && (propagatedType == null || shouldReportMissingMember(propagatedType, propagatedElement))) {
      boolean staticNoSuchMethod = staticType != null
          && classDeclaresNoSuchMethod(staticType.getElement());
      boolean propagatedNoSuchMethod = propagatedType != null
          && classDeclaresNoSuchMethod(propagatedType.getElement());
      if (!staticNoSuchMethod && !propagatedNoSuchMethod) {
        boolean isStaticProperty = isStatic(selectedElement);
        if (propertyName.inSetterContext()) {
          if (isStaticProperty) {
            resolver.reportError(
                StaticWarningCode.UNDEFINED_SETTER,
                propertyName,
                propertyName.getName(),
                staticType.getName());
          } else {
            resolver.reportError(
                StaticTypeWarningCode.UNDEFINED_SETTER,
                propertyName,
                propertyName.getName(),
                staticType.getName());
          }
        } else if (propertyName.inGetterContext()) {
          if (isStaticProperty) {
            resolver.reportError(
                StaticWarningCode.UNDEFINED_GETTER,
                propertyName,
                propertyName.getName(),
                staticType.getName());
          } else {
            resolver.reportError(
                StaticTypeWarningCode.UNDEFINED_GETTER,
                propertyName,
                propertyName.getName(),
                staticType.getName());
          }
        } else {
          resolver.reportError(
              StaticWarningCode.UNDEFINED_IDENTIFIER,
              propertyName,
              propertyName.getName());
        }
      }
//    } else if (selectedElement != null && !selectedElement.isStatic() && isTypeReference(target)) {
//    reportError(
//        ResolverErrorCode.STATIC_ACCESS_TO_INSTANCE_MEMBER,
//        identifier,
//        identifier.getName());
    }
  }

  /**
   * Resolve the given simple identifier if possible. Return the element to which it could be
   * resolved, or {@code null} if it could not be resolved. This does not record the results of the
   * resolution.
   * 
   * @param node the identifier to be resolved
   * @return the element to which the identifier could be resolved
   */
  private Element resolveSimpleIdentifier(SimpleIdentifier node) {
    Element element = resolver.getNameScope().lookup(node, resolver.getDefiningLibrary());
    if (element instanceof PropertyAccessorElement && node.inSetterContext()) {
      PropertyInducingElement variable = ((PropertyAccessorElement) element).getVariable();
      if (variable != null) {
        PropertyAccessorElement setter = variable.getSetter();
        if (setter != null) {
          element = setter;
        }
      }
    }
    ClassElement enclosingClass = resolver.getEnclosingClass();
    if (element == null && enclosingClass != null) {
      InterfaceType enclosingType = enclosingClass.getType();
      if (element == null && node.inSetterContext()) {
        element = lookUpSetter(enclosingType, node.getName());
      }
      if (element == null && node.inGetterContext()) {
        element = lookUpGetter(enclosingType, node.getName());
      }
      if (element == null) {
        element = lookUpMethod(enclosingType, node.getName());
      }
    }
    return element;
  }

  /**
   * If the given type is a type variable, resolve it to the type that should be used when looking
   * up members. Otherwise, return the original type.
   * 
   * @param type the type that is to be resolved if it is a type variable
   * @return the type that should be used in place of the argument if it is a type variable, or the
   *         original argument if it isn't a type variable
   */
  private Type resolveTypeVariable(Type type) {
    if (type instanceof TypeVariableType) {
      Type bound = ((TypeVariableType) type).getElement().getBound();
      if (bound == null) {
        return resolver.getTypeProvider().getObjectType();
      }
      return bound;
    }
    return type;
  }

  /**
   * Given two possible error codes for the same piece of code, one computed using static type
   * information and the other using propagated type information, return the error code that should
   * be reported, or {@code null} if no error should be reported.
   * 
   * @param staticError the error code computed using static type information
   * @param propagatedError the error code computed using propagated type information
   * @return the error code that should be reported
   */
  private ErrorCode select(ErrorCode staticError, ErrorCode propagatedError) {
    if (staticError == null || propagatedError == null) {
      return null;
    }
    //
    // If the errors are different, we assume that the propagated error is more relevant.
    //
    return propagatedError;
  }

  /**
   * Return the propagated element if it is not {@code null}, or the static element if it is.
   * 
   * @param staticElement the element computed using static type information
   * @param propagatedElement the element computed using propagated type information
   * @return the more specific of the two elements
   */
  private ExecutableElement select(ExecutableElement staticElement,
      ExecutableElement propagatedElement) {
    return propagatedElement != null ? propagatedElement : staticElement;
  }

  /**
   * Return the propagated method if it is not {@code null}, or the static method if it is.
   * 
   * @param staticMethod the method computed using static type information
   * @param propagatedMethod the method computed using propagated type information
   * @return the more specific of the two methods
   */
  private MethodElement select(MethodElement staticMethod, MethodElement propagatedMethod) {
    return propagatedMethod != null ? propagatedMethod : staticMethod;
  }

  /**
   * Return {@code true} if we should report an error as a result of looking up a member in the
   * given type and not finding any member.
   * 
   * @param type the type in which we attempted to perform the look-up
   * @param member the result of the look-up
   * @return {@code true} if we should report an error
   */
  private boolean shouldReportMissingMember(Type type, ExecutableElement member) {
    if (member != null || type == null || type.isDynamic()) {
      return false;
    }
    if (type instanceof InterfaceType) {
      return !classDeclaresNoSuchMethod(((InterfaceType) type).getElement());
    }
    return true;
  }
}
