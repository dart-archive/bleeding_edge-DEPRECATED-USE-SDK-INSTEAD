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
import com.google.dart.engine.ast.AnnotatedNode;
import com.google.dart.engine.ast.Annotation;
import com.google.dart.engine.ast.ArgumentList;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.AstVisitor;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.BreakStatement;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassTypeAlias;
import com.google.dart.engine.ast.Combinator;
import com.google.dart.engine.ast.CommentReference;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.ConstructorFieldInitializer;
import com.google.dart.engine.ast.ConstructorInitializer;
import com.google.dart.engine.ast.ConstructorName;
import com.google.dart.engine.ast.ContinueStatement;
import com.google.dart.engine.ast.DeclaredIdentifier;
import com.google.dart.engine.ast.ExportDirective;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.FieldFormalParameter;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.FunctionExpressionInvocation;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.FunctionTypedFormalParameter;
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
import com.google.dart.engine.ast.NormalFormalParameter;
import com.google.dart.engine.ast.NullLiteral;
import com.google.dart.engine.ast.PartDirective;
import com.google.dart.engine.ast.PartOfDirective;
import com.google.dart.engine.ast.PostfixExpression;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.RedirectingConstructorInvocation;
import com.google.dart.engine.ast.ShowCombinator;
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SuperConstructorInvocation;
import com.google.dart.engine.ast.SuperExpression;
import com.google.dart.engine.ast.TopLevelVariableDeclaration;
import com.google.dart.engine.ast.TypeParameter;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.visitor.SimpleAstVisitor;
import com.google.dart.engine.context.AnalysisOptions;
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
import com.google.dart.engine.error.HintCode;
import com.google.dart.engine.error.StaticTypeWarningCode;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.internal.element.AuxiliaryElements;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.ConstructorElementImpl;
import com.google.dart.engine.internal.element.ElementAnnotationImpl;
import com.google.dart.engine.internal.element.ElementImpl;
import com.google.dart.engine.internal.element.ExecutableElementImpl;
import com.google.dart.engine.internal.element.LabelElementImpl;
import com.google.dart.engine.internal.element.MethodElementImpl;
import com.google.dart.engine.internal.element.MultiplyDefinedElementImpl;
import com.google.dart.engine.internal.element.ParameterElementImpl;
import com.google.dart.engine.internal.scope.LabelScope;
import com.google.dart.engine.internal.scope.Namespace;
import com.google.dart.engine.internal.scope.NamespaceBuilder;
import com.google.dart.engine.internal.scope.Scope;
import com.google.dart.engine.internal.type.FunctionTypeImpl;
import com.google.dart.engine.internal.type.InterfaceTypeImpl;
import com.google.dart.engine.internal.type.UnionTypeImpl;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.type.TypeParameterType;
import com.google.dart.engine.type.UnionType;
import com.google.dart.engine.utilities.dart.ParameterKind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

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
public class ElementResolver extends SimpleAstVisitor<Void> {
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
    public <R> R accept(AstVisitor<R> visitor) {
      return null;
    }

    @Override
    public Token getBeginToken() {
      return null;
    }

    @Override
    public Element getBestElement() {
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
    public int getPrecedence() {
      return 16;
    }

    @Override
    public Element getPropagatedElement() {
      return null;
    }

    @Override
    public Element getStaticElement() {
      return null;
    }

    @Override
    public void visitChildren(AstVisitor<?> visitor) {
    }
  }

  /**
   * Checks whether the given expression is a reference to a class. If it is then the
   * {@link ClassElement} is returned, otherwise {@code null} is returned.
   * 
   * @param expression the expression to evaluate
   * @return the element representing the class
   */
  public static ClassElementImpl getTypeReference(Expression expression) {
    if (expression instanceof Identifier) {
      Element staticElement = ((Identifier) expression).getStaticElement();
      if (staticElement instanceof ClassElementImpl) {
        return (ClassElementImpl) staticElement;
      }
    }
    return null;
  }

  /**
   * Return a method representing the merge of the given elements. The type of the merged element is
   * the component-wise union of the types of the given elements. If not all input elements have the
   * same shape then [null] is returned.
   * 
   * @param elements the {@code ExecutableElement}s to merge
   * @return an {@code ExecutableElement} representing the merge of {@code elements}
   */
  // TODO (collinsn): somehow return [dynamic] here, or at least in the callers, when not all
  // given methods have the same shape.
  private static ExecutableElement computeMergedExecutableElement(Set<ExecutableElement> elements) {
    ExecutableElement[] elementArrayToMerge = elements.toArray(new ExecutableElement[elements.size()]);
    if (elementArrayToMerge.length == 0) {
      return null;
    } else {
      // Flatten methods structurally. Based on
      // [InheritanceManager.computeMergedExecutableElement] and
      // [InheritanceManager.createSyntheticExecutableElement].
      //
      // However, the approach we take here is much simpler, but expected to work
      // well in the common case. It degrades gracefully in the uncommon case,
      // by computing the type [dynamic] for the method, preventing any
      // hints from being generated (TODO: not done yet).
      //
      // The approach is: we require that each [ExecutableElement] has the
      // same shape: the same number of required, optional positional, and optional named
      // parameters, in the same positions, and with the named parameters in the
      // same order. We compute a type by unioning pointwise.
      ExecutableElement e_0 = elementArrayToMerge[0];
      ParameterElement[] ps_0 = e_0.getParameters();
      ParameterElementImpl[] ps_out = new ParameterElementImpl[ps_0.length];
      for (int j = 0; j < ps_out.length; j++) {
        ps_out[j] = new ParameterElementImpl(ps_0[j].getName(), 0);
        ps_out[j].setSynthetic(true);
        ps_out[j].setType(ps_0[j].getType());
        ps_out[j].setParameterKind(ps_0[j].getParameterKind());
      }
      Type r_out = e_0.getReturnType();

      for (int i = 1; i < elementArrayToMerge.length; i++) {
        ExecutableElement e_i = elementArrayToMerge[i];
        r_out = UnionTypeImpl.union(r_out, e_i.getReturnType());

        ParameterElement[] ps_i = e_i.getParameters();
        // Each function must have the same number of params.
        if (ps_0.length != ps_i.length) {
          return null; // TODO (collinsn): return an element representing [dynamic] here instead.
        } else {
          // Each function must have the same kind of params, with the same names,
          // in the same order.
          for (int j = 0; j < ps_i.length; j++) {
            if (ps_0[j].getParameterKind() != ps_i[j].getParameterKind()
                || ps_0[j].getName() != ps_i[j].getName()) {
              return null;
            } else {
              // The output parameter type is the union of the input parameter types.
              ps_out[j].setType(UnionTypeImpl.union(ps_out[j].getType(), ps_i[j].getType()));
            }
          }
        }
      }
      // TODO (collinsn): this code should work for functions and methods,
      // so we may want [FunctionElementImpl]
      // instead here in some cases? And then there are constructors and property accessors.
      // Maybe the answer is to create a new subclass of [ExecutableElementImpl] which
      // is used for merged executable elements, in analogy with [MultiplyInheritedMethodElementImpl]
      // and [MultiplyInheritedPropertyAcessorElementImpl].
      ExecutableElementImpl e_out = new MethodElementImpl(e_0.getName(), 0);
      e_out.setSynthetic(true);
      e_out.setReturnType(r_out);
      e_out.setParameters(ps_out);
      e_out.setType(new FunctionTypeImpl(e_out));
      return e_out;
    }
  }

  /**
   * Return {@code true} if the given identifier is the return type of a constructor declaration.
   * 
   * @return {@code true} if the given identifier is the return type of a constructor declaration.
   */
  private static boolean isConstructorReturnType(SimpleIdentifier identifier) {
    AstNode parent = identifier.getParent();
    if (parent instanceof ConstructorDeclaration) {
      return ((ConstructorDeclaration) parent).getReturnType() == identifier;
    }
    return false;
  }

  /**
   * Return {@code true} if the given identifier is the return type of a factory constructor.
   * 
   * @return {@code true} if the given identifier is the return type of a factory constructor
   *         declaration.
   */
  private static boolean isFactoryConstructorReturnType(SimpleIdentifier node) {
    AstNode parent = node.getParent();
    if (parent instanceof ConstructorDeclaration) {
      ConstructorDeclaration constructor = (ConstructorDeclaration) parent;
      return constructor.getReturnType() == node && constructor.getFactoryKeyword() != null;
    }
    return false;
  }

  /**
   * Return {@code true} if the given 'super' expression is used in a valid context.
   * 
   * @param node the 'super' expression to analyze
   * @return {@code true} if the 'super' expression is in a valid context
   */
  private static boolean isSuperInValidContext(SuperExpression node) {
    for (AstNode n = node; n != null; n = n.getParent()) {
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
   * The element for the library containing the compilation unit being visited.
   */
  private LibraryElement definingLibrary;

  /**
   * A flag indicating whether we should generate hints.
   */
  private boolean enableHints;

  /**
   * The type representing the type 'dynamic'.
   */
  private Type dynamicType;

  /**
   * The type representing the type 'type'.
   */
  private Type typeType;

  /**
   * A utility class for the resolver to answer the question of "what are my subtypes?".
   */
  private SubtypeManager subtypeManager;

  /**
   * The object keeping track of which elements have had their types promoted.
   */
  private TypePromotionManager promoteManager;

  /**
   * Initialize a newly created visitor to resolve the nodes in a compilation unit.
   * 
   * @param resolver the resolver driving this participant
   */
  public ElementResolver(ResolverVisitor resolver) {
    this.resolver = resolver;
    this.definingLibrary = resolver.getDefiningLibrary();
    AnalysisOptions options = definingLibrary.getContext().getAnalysisOptions();
    enableHints = options.getHint();
    dynamicType = resolver.getTypeProvider().getDynamicType();
    typeType = resolver.getTypeProvider().getTypeType();
    subtypeManager = new SubtypeManager();
    promoteManager = resolver.getPromoteManager();
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
        MethodElement staticMethod = lookUpMethod(leftHandSide, staticType, methodName);
        node.setStaticElement(staticMethod);

        Type propagatedType = getPropagatedType(leftHandSide);
        MethodElement propagatedMethod = lookUpMethod(leftHandSide, propagatedType, methodName);
        node.setPropagatedElement(propagatedMethod);

        if (shouldReportMissingMember(staticType, staticMethod)) {
          if (doesClassElementHaveProxy(staticType.getElement())) {
            resolver.reportErrorForToken(
                StaticTypeWarningCode.UNDEFINED_METHOD,
                operator,
                methodName,
                staticType.getDisplayName());
          }
        } else if (enableHints && shouldReportMissingMember(propagatedType, propagatedMethod)
            && !memberFoundInSubclass(propagatedType.getElement(), methodName, true, false)) {
          if (doesClassElementHaveProxy(propagatedType.getElement())) {
            resolver.reportErrorForToken(
                HintCode.UNDEFINED_METHOD,
                operator,
                methodName,
                propagatedType.getDisplayName());
          }
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
        MethodElement staticMethod = lookUpMethod(leftOperand, staticType, methodName);
        node.setStaticElement(staticMethod);

        Type propagatedType = getPropagatedType(leftOperand);
        MethodElement propagatedMethod = lookUpMethod(leftOperand, propagatedType, methodName);
        node.setPropagatedElement(propagatedMethod);

        if (shouldReportMissingMember(staticType, staticMethod)) {
          if (doesClassElementHaveProxy(staticType.getElement())) {
            resolver.reportErrorForToken(
                StaticTypeWarningCode.UNDEFINED_OPERATOR,
                operator,
                methodName,
                staticType.getDisplayName());
          }
        } else if (enableHints && shouldReportMissingMember(propagatedType, propagatedMethod)
            && !memberFoundInSubclass(propagatedType.getElement(), methodName, true, false)) {
          if (doesClassElementHaveProxy(propagatedType.getElement())) {
            resolver.reportErrorForToken(
                HintCode.UNDEFINED_OPERATOR,
                operator,
                methodName,
                propagatedType.getDisplayName());
          }
        }
      }
    }
    return null;
  }

  @Override
  public Void visitBreakStatement(BreakStatement node) {
    lookupLabel(node, node.getLabel());
    return null;
  }

  @Override
  public Void visitClassDeclaration(ClassDeclaration node) {
    setMetadata(node.getElement(), node);
    return null;
  }

  @Override
  public Void visitClassTypeAlias(ClassTypeAlias node) {
    setMetadata(node.getElement(), node);
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
        if (element.getLibrary() == null || !element.getLibrary().equals(definingLibrary)) {
          // TODO(brianwilkerson) Report this error?
        }
        simpleIdentifier.setStaticElement(element);
        if (node.getNewKeyword() != null) {
          if (element instanceof ClassElement) {
            ConstructorElement constructor = ((ClassElement) element).getUnnamedConstructor();
            if (constructor == null) {
              // TODO(brianwilkerson) Report this error.
            } else {
              simpleIdentifier.setStaticElement(constructor);
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
          prefix.setStaticElement(element);
          // TODO(brianwilkerson) The prefix needs to be resolved to the element for the import that
          // defines the prefix, not the prefix's element.

          // TODO(brianwilkerson) Report this error?
          element = resolver.getNameScope().lookup(identifier, definingLibrary);
          name.setStaticElement(element);
          return null;
        }
        LibraryElement library = element.getLibrary();
        if (library == null) {
          // TODO(brianwilkerson) We need to understand how the library could ever be null.
          AnalysisEngine.getInstance().getLogger().logError(
              "Found element with null library: " + element.getName());
        } else if (!library.equals(definingLibrary)) {
          // TODO(brianwilkerson) Report this error.
        }
        name.setStaticElement(element);
        if (node.getNewKeyword() == null) {
          if (element instanceof ClassElement) {
            Element memberElement = lookupGetterOrMethod(
                ((ClassElement) element).getType(),
                name.getName());
            if (memberElement == null) {
              memberElement = ((ClassElement) element).getNamedConstructor(name.getName());
              if (memberElement == null) {
                memberElement = lookUpSetter(
                    prefix,
                    ((ClassElement) element).getType(),
                    name.getName());
              }
            }
            if (memberElement == null) {
//              reportGetterOrSetterNotFound(prefixedIdentifier, name, element.getDisplayName());
            } else {
              name.setStaticElement(memberElement);
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
              name.setStaticElement(constructor);
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
  public Void visitConstructorDeclaration(ConstructorDeclaration node) {
    super.visitConstructorDeclaration(node);
    ConstructorElement element = node.getElement();
    if (element instanceof ConstructorElementImpl) {
      ConstructorElementImpl constructorElement = (ConstructorElementImpl) element;
      ConstructorName redirectedNode = node.getRedirectedConstructor();
      if (redirectedNode != null) {
        // set redirected factory constructor
        ConstructorElement redirectedElement = redirectedNode.getStaticElement();
        constructorElement.setRedirectedConstructor(redirectedElement);
      } else {
        // set redirected generative constructor
        for (ConstructorInitializer initializer : node.getInitializers()) {
          if (initializer instanceof RedirectingConstructorInvocation) {
            ConstructorElement redirectedElement = ((RedirectingConstructorInvocation) initializer).getStaticElement();
            constructorElement.setRedirectedConstructor(redirectedElement);
          }
        }
      }
      setMetadata(constructorElement, node);
    }
    return null;
  }

  @Override
  public Void visitConstructorFieldInitializer(ConstructorFieldInitializer node) {
    SimpleIdentifier fieldName = node.getFieldName();
    ClassElement enclosingClass = resolver.getEnclosingClass();
    FieldElement fieldElement = enclosingClass.getField(fieldName.getName());
    fieldName.setStaticElement(fieldElement);
    return null;
  }

  @Override
  public Void visitConstructorName(ConstructorName node) {
    Type type = node.getType().getType();
    if (type != null && type.isDynamic()) {
      return null;
    } else if (!(type instanceof InterfaceType)) {
      // TODO(brianwilkerson) Report these errors.
//      ASTNode parent = node.getParent();
//      if (parent instanceof InstanceCreationExpression) {
//        if (((InstanceCreationExpression) parent).isConst()) {
//          // CompileTimeErrorCode.CONST_WITH_NON_TYPE
//        } else {
//          // StaticWarningCode.NEW_WITH_NON_TYPE
//        }
//      } else {
//        // This is part of a redirecting factory constructor; not sure which error code to use
//      }
      return null;
    }
    // look up ConstructorElement
    ConstructorElement constructor;
    SimpleIdentifier name = node.getName();
    InterfaceType interfaceType = (InterfaceType) type;
    if (name == null) {
      constructor = interfaceType.lookUpConstructor(null, definingLibrary);
    } else {
      constructor = interfaceType.lookUpConstructor(name.getName(), definingLibrary);
      name.setStaticElement(constructor);
    }
    node.setStaticElement(constructor);
    return null;
  }

  @Override
  public Void visitContinueStatement(ContinueStatement node) {
    lookupLabel(node, node.getLabel());
    return null;
  }

  @Override
  public Void visitDeclaredIdentifier(DeclaredIdentifier node) {
    setMetadata(node.getElement(), node);
    return null;
  }

  @Override
  public Void visitExportDirective(ExportDirective node) {
    ExportElement exportElement = node.getElement();
    if (exportElement != null) {
      // The element is null when the URI is invalid
      // TODO(brianwilkerson) Figure out whether the element can ever be something other than an
      // ExportElement
      resolveCombinators(exportElement.getExportedLibrary(), node.getCombinators());
      setMetadata(exportElement, node);
    }
    return null;
  }

  @Override
  public Void visitFieldFormalParameter(FieldFormalParameter node) {
    setMetadataForParameter(node.getElement(), node);
    return super.visitFieldFormalParameter(node);
  }

  @Override
  public Void visitFunctionDeclaration(FunctionDeclaration node) {
    setMetadata(node.getElement(), node);
    return null;
  }

  @Override
  public Void visitFunctionExpressionInvocation(FunctionExpressionInvocation node) {
    // TODO(brianwilkerson) Can we ever resolve the function being invoked?
    Expression expression = node.getFunction();
    if (expression instanceof FunctionExpression) {
      FunctionExpression functionExpression = (FunctionExpression) expression;
      ExecutableElement functionElement = functionExpression.getElement();
      ArgumentList argumentList = node.getArgumentList();
      ParameterElement[] parameters = resolveArgumentsToFunction(
          false,
          argumentList,
          functionElement);
      if (parameters != null) {
        argumentList.setCorrespondingStaticParameters(parameters);
      }
    }
    return null;
  }

  @Override
  public Void visitFunctionTypeAlias(FunctionTypeAlias node) {
    setMetadata(node.getElement(), node);
    return null;
  }

  @Override
  public Void visitFunctionTypedFormalParameter(FunctionTypedFormalParameter node) {
    setMetadataForParameter(node.getElement(), node);
    return null;
  }

  @Override
  public Void visitImportDirective(ImportDirective node) {
    SimpleIdentifier prefixNode = node.getPrefix();
    if (prefixNode != null) {
      String prefixName = prefixNode.getName();
      for (PrefixElement prefixElement : definingLibrary.getPrefixes()) {
        if (prefixElement.getDisplayName().equals(prefixName)) {
          prefixNode.setStaticElement(prefixElement);
          break;
        }
      }
    }
    ImportElement importElement = node.getElement();
    if (importElement != null) {
      // The element is null when the URI is invalid
      LibraryElement library = importElement.getImportedLibrary();
      if (library != null) {
        resolveCombinators(library, node.getCombinators());
      }
      setMetadata(importElement, node);
    }
    return null;
  }

  @Override
  public Void visitIndexExpression(IndexExpression node) {
    Expression target = node.getRealTarget();
    Type staticType = getStaticType(target);
    Type propagatedType = getPropagatedType(target);
    String getterMethodName = TokenType.INDEX.getLexeme();
    String setterMethodName = TokenType.INDEX_EQ.getLexeme();
    boolean isInGetterContext = node.inGetterContext();
    boolean isInSetterContext = node.inSetterContext();

    if (isInGetterContext && isInSetterContext) {
      // lookup setter
      MethodElement setterStaticMethod = lookUpMethod(target, staticType, setterMethodName);
      MethodElement setterPropagatedMethod = lookUpMethod(target, propagatedType, setterMethodName);
      // set setter element
      node.setStaticElement(setterStaticMethod);
      node.setPropagatedElement(setterPropagatedMethod);
      // generate undefined method warning
      checkForUndefinedIndexOperator(
          node,
          target,
          getterMethodName,
          setterStaticMethod,
          setterPropagatedMethod,
          staticType,
          propagatedType);

      // lookup getter method
      MethodElement getterStaticMethod = lookUpMethod(target, staticType, getterMethodName);
      MethodElement getterPropagatedMethod = lookUpMethod(target, propagatedType, getterMethodName);
      // set getter element
      AuxiliaryElements auxiliaryElements = new AuxiliaryElements(
          getterStaticMethod,
          getterPropagatedMethod);
      node.setAuxiliaryElements(auxiliaryElements);
      // generate undefined method warning
      checkForUndefinedIndexOperator(
          node,
          target,
          getterMethodName,
          getterStaticMethod,
          getterPropagatedMethod,
          staticType,
          propagatedType);

    } else if (isInGetterContext) {
      // lookup getter method
      MethodElement staticMethod = lookUpMethod(target, staticType, getterMethodName);
      MethodElement propagatedMethod = lookUpMethod(target, propagatedType, getterMethodName);
      // set getter element
      node.setStaticElement(staticMethod);
      node.setPropagatedElement(propagatedMethod);
      // generate undefined method warning
      checkForUndefinedIndexOperator(
          node,
          target,
          getterMethodName,
          staticMethod,
          propagatedMethod,
          staticType,
          propagatedType);
    } else if (isInSetterContext) {
      // lookup setter method
      MethodElement staticMethod = lookUpMethod(target, staticType, setterMethodName);
      MethodElement propagatedMethod = lookUpMethod(target, propagatedType, setterMethodName);
      // set setter element
      node.setStaticElement(staticMethod);
      node.setPropagatedElement(propagatedMethod);
      // generate undefined method warning
      checkForUndefinedIndexOperator(
          node,
          target,
          setterMethodName,
          staticMethod,
          propagatedMethod,
          staticType,
          propagatedType);
    }
    return null;
  }

  @Override
  public Void visitInstanceCreationExpression(InstanceCreationExpression node) {
    ConstructorElement invokedConstructor = node.getConstructorName().getStaticElement();
    node.setStaticElement(invokedConstructor);
    ArgumentList argumentList = node.getArgumentList();
    ParameterElement[] parameters = resolveArgumentsToFunction(
        node.isConst(),
        argumentList,
        invokedConstructor);
    if (parameters != null) {
      argumentList.setCorrespondingStaticParameters(parameters);
    }
    return null;
  }

  @Override
  public Void visitLibraryDirective(LibraryDirective node) {
    setMetadata(node.getElement(), node);
    return null;
  }

  @Override
  public Void visitMethodDeclaration(MethodDeclaration node) {
    setMetadata(node.getElement(), node);
    return null;
  }

  @Override
  public Void visitMethodInvocation(MethodInvocation node) {
    SimpleIdentifier methodName = node.getMethodName();
    //
    // Synthetic identifiers have been already reported during parsing.
    //
    if (methodName.isSynthetic()) {
      return null;
    }
    //
    // We have a method invocation of one of two forms: 'e.m(a1, ..., an)' or 'm(a1, ..., an)'. The
    // first step is to figure out which executable is being invoked, using both the static and the
    // propagated type information.
    //
    Expression target = node.getRealTarget();
    if (target instanceof SuperExpression && !isSuperInValidContext((SuperExpression) target)) {
      return null;
    }
    Element staticElement;
    Element propagatedElement;
    if (target == null) {
      staticElement = resolveInvokedElement(methodName);
      propagatedElement = null;
    } else if (methodName.getName().equals(FunctionElement.LOAD_LIBRARY_NAME)
        && isDeferredPrefix(target)) {
      LibraryElement importedLibrary = getImportedLibrary(target);
      methodName.setStaticElement(importedLibrary.getLoadLibraryFunction());
      return null;
    } else {
      Type staticType = getStaticType(target);
      //
      // If this method invocation is of the form 'C.m' where 'C' is a class, then we don't call
      // resolveInvokedElement(..) which walks up the class hierarchy, instead we just look for the
      // member in the type only.
      //
      ClassElementImpl typeReference = getTypeReference(target);
      if (typeReference != null) {
        staticElement = propagatedElement = resolveElement(typeReference, methodName);
      } else {
        staticElement = resolveInvokedElementWithTarget(target, staticType, methodName);
        propagatedElement = resolveInvokedElementWithTarget(
            target,
            getPropagatedType(target),
            methodName);
      }
    }
    staticElement = convertSetterToGetter(staticElement);
    propagatedElement = convertSetterToGetter(propagatedElement);
    //
    // Record the results.
    //
    methodName.setStaticElement(staticElement);
    methodName.setPropagatedElement(propagatedElement);
    ArgumentList argumentList = node.getArgumentList();
    if (staticElement != null) {
      ParameterElement[] parameters = computeCorrespondingParameters(argumentList, staticElement);
      if (parameters != null) {
        argumentList.setCorrespondingStaticParameters(parameters);
      }
    }
    if (propagatedElement != null) {
      ParameterElement[] parameters = computeCorrespondingParameters(
          argumentList,
          propagatedElement);
      if (parameters != null) {
        argumentList.setCorrespondingPropagatedParameters(parameters);
      }
    }
    //
    // Then check for error conditions.
    //
    ErrorCode errorCode = checkForInvocationError(target, true, staticElement);
    boolean generatedWithTypePropagation = false;
    if (enableHints && errorCode == null && staticElement == null) {
      errorCode = checkForInvocationError(target, false, propagatedElement);
      if (errorCode == StaticTypeWarningCode.UNDEFINED_METHOD) {
        ClassElement classElementContext = null;
        if (target == null) {
          classElementContext = resolver.getEnclosingClass();
        } else {
          Type type = target.getBestType();
          if (type != null) {
            if (type.getElement() instanceof ClassElement) {
              classElementContext = (ClassElement) type.getElement();
            }
          }
        }
        if (classElementContext != null) {
          subtypeManager.ensureLibraryVisited(definingLibrary);
          HashSet<ClassElement> subtypeElements = subtypeManager.computeAllSubtypes(classElementContext);
          for (ClassElement subtypeElement : subtypeElements) {
            if (subtypeElement.getMethod(methodName.getName()) != null) {
              errorCode = null;
            }
          }
        }
      }
      generatedWithTypePropagation = true;
    }
    if (errorCode == null) {
      return null;
    }
    if (errorCode == StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION) {
      resolver.reportErrorForNode(
          StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION,
          methodName,
          methodName.getName());
    } else if (errorCode == StaticTypeWarningCode.UNDEFINED_FUNCTION) {
      resolver.reportErrorForNode(
          StaticTypeWarningCode.UNDEFINED_FUNCTION,
          methodName,
          methodName.getName());
    } else if (errorCode == StaticTypeWarningCode.UNDEFINED_METHOD) {
      String targetTypeName;
      if (target == null) {
        ClassElement enclosingClass = resolver.getEnclosingClass();
        targetTypeName = enclosingClass.getDisplayName();
        ErrorCode proxyErrorCode = generatedWithTypePropagation ? HintCode.UNDEFINED_METHOD
            : StaticTypeWarningCode.UNDEFINED_METHOD;
        if (doesClassElementHaveProxy(resolver.getEnclosingClass())) {
          resolver.reportErrorForNode(
              proxyErrorCode,
              methodName,
              methodName.getName(),
              targetTypeName);
        }

      } else {
        // ignore Function "call"
        // (if we are about to create a hint using type propagation, then we can use type
        // propagation here as well)
        Type targetType = null;
        if (!generatedWithTypePropagation) {
          targetType = getStaticType(target);
        } else {
          // choose the best type
          targetType = getPropagatedType(target);
          if (targetType == null) {
            targetType = getStaticType(target);
          }
        }
        if (targetType != null && targetType.isDartCoreFunction()
            && methodName.getName().equals(FunctionElement.CALL_METHOD_NAME)) {
          // TODO(brianwilkerson) Can we ever resolve the function being invoked?
          //resolveArgumentsToParameters(node.getArgumentList(), invokedFunction);
          return null;
        }
        targetTypeName = targetType == null ? null : targetType.getDisplayName();
        ErrorCode proxyErrorCode = generatedWithTypePropagation ? HintCode.UNDEFINED_METHOD
            : StaticTypeWarningCode.UNDEFINED_METHOD;
        if (doesClassElementHaveProxy(targetType.getElement())) {
          resolver.reportErrorForNode(
              proxyErrorCode,
              methodName,
              methodName.getName(),
              targetTypeName);
        }
      }
    } else if (errorCode == StaticTypeWarningCode.UNDEFINED_SUPER_METHOD) {
      // Generate the type name.
      // The error code will never be generated via type propagation
      Type targetType = getStaticType(target);
      if (targetType instanceof InterfaceType && !targetType.isObject()) {
        targetType = ((InterfaceType) targetType).getSuperclass();
      }
      String targetTypeName = targetType == null ? null : targetType.getName();
      resolver.reportErrorForNode(
          StaticTypeWarningCode.UNDEFINED_SUPER_METHOD,
          methodName,
          methodName.getName(),
          targetTypeName);
    }
    return null;
  }

  @Override
  public Void visitPartDirective(PartDirective node) {
    setMetadata(node.getElement(), node);
    return null;
  }

  @Override
  public Void visitPartOfDirective(PartOfDirective node) {
    setMetadata(node.getElement(), node);
    return null;
  }

  @Override
  public Void visitPostfixExpression(PostfixExpression node) {
    Expression operand = node.getOperand();
    String methodName = getPostfixOperator(node);

    Type staticType = getStaticType(operand);
    MethodElement staticMethod = lookUpMethod(operand, staticType, methodName);
    node.setStaticElement(staticMethod);

    Type propagatedType = getPropagatedType(operand);
    MethodElement propagatedMethod = lookUpMethod(operand, propagatedType, methodName);
    node.setPropagatedElement(propagatedMethod);

    if (shouldReportMissingMember(staticType, staticMethod)) {
      if (doesClassElementHaveProxy(staticType.getElement())) {
        resolver.reportErrorForToken(
            StaticTypeWarningCode.UNDEFINED_OPERATOR,
            node.getOperator(),
            methodName,
            staticType.getDisplayName());
      }
    } else if (enableHints && shouldReportMissingMember(propagatedType, propagatedMethod)
        && !memberFoundInSubclass(propagatedType.getElement(), methodName, true, false)) {
      if (doesClassElementHaveProxy(propagatedType.getElement())) {
        resolver.reportErrorForToken(
            HintCode.UNDEFINED_OPERATOR,
            node.getOperator(),
            methodName,
            propagatedType.getDisplayName());
      }
    }
    return null;
  }

  @Override
  public Void visitPrefixedIdentifier(PrefixedIdentifier node) {
    SimpleIdentifier prefix = node.getPrefix();
    SimpleIdentifier identifier = node.getIdentifier();
    //
    // First, check the "lib.loadLibrary" case
    //
    if (identifier.getName().equals(FunctionElement.LOAD_LIBRARY_NAME) && isDeferredPrefix(prefix)) {
      LibraryElement importedLibrary = getImportedLibrary(prefix);
      identifier.setStaticElement(importedLibrary.getLoadLibraryFunction());
      return null;
    }
    //
    // Check to see whether the prefix is really a prefix.
    //
    Element prefixElement = prefix.getStaticElement();
    if (prefixElement instanceof PrefixElement) {
      Element element = resolver.getNameScope().lookup(node, definingLibrary);
      if (element == null && identifier.inSetterContext()) {
        element = resolver.getNameScope().lookup(
            new SyntheticIdentifier(node.getName() + "="),
            definingLibrary);
      }
      if (element == null) {
        if (identifier.inSetterContext()) {
          resolver.reportErrorForNode(
              StaticWarningCode.UNDEFINED_SETTER,
              identifier,
              identifier.getName(),
              prefixElement.getName());
        } else if (node.getParent() instanceof Annotation) {
          Annotation annotation = (Annotation) node.getParent();
          resolver.reportErrorForNode(CompileTimeErrorCode.INVALID_ANNOTATION, annotation);
          return null;
        } else {
          resolver.reportErrorForNode(
              StaticWarningCode.UNDEFINED_GETTER,
              identifier,
              identifier.getName(),
              prefixElement.getName());
        }
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
      identifier.setStaticElement(element);
      // Validate annotation element.
      if (node.getParent() instanceof Annotation) {
        Annotation annotation = (Annotation) node.getParent();
        resolveAnnotationElement(annotation);
        return null;
      }
      return null;
    }

    // May be annotation, resolve invocation of "const" constructor.
    if (node.getParent() instanceof Annotation) {
      Annotation annotation = (Annotation) node.getParent();
      resolveAnnotationElement(annotation);
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
      MethodElement staticMethod = lookUpMethod(operand, staticType, methodName);
      node.setStaticElement(staticMethod);

      Type propagatedType = getPropagatedType(operand);
      MethodElement propagatedMethod = lookUpMethod(operand, propagatedType, methodName);
      node.setPropagatedElement(propagatedMethod);

      if (shouldReportMissingMember(staticType, staticMethod)) {
        if (doesClassElementHaveProxy(staticType.getElement())) {
          resolver.reportErrorForToken(
              StaticTypeWarningCode.UNDEFINED_OPERATOR,
              operator,
              methodName,
              staticType.getDisplayName());
        }
      } else if (enableHints && shouldReportMissingMember(propagatedType, propagatedMethod)
          && !memberFoundInSubclass(propagatedType.getElement(), methodName, true, false)) {
        if (doesClassElementHaveProxy(propagatedType.getElement())) {
          resolver.reportErrorForToken(
              HintCode.UNDEFINED_OPERATOR,
              operator,
              methodName,
              propagatedType.getDisplayName());
        }
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
      name.setStaticElement(element);
    }
    node.setStaticElement(element);
    ArgumentList argumentList = node.getArgumentList();
    ParameterElement[] parameters = resolveArgumentsToFunction(false, argumentList, element);
    if (parameters != null) {
      argumentList.setCorrespondingStaticParameters(parameters);
    }
    return null;
  }

  @Override
  public Void visitSimpleFormalParameter(SimpleFormalParameter node) {
    setMetadataForParameter(node.getElement(), node);
    return null;
  }

  @Override
  public Void visitSimpleIdentifier(SimpleIdentifier node) {
    //
    // Synthetic identifiers have been already reported during parsing.
    //
    if (node.isSynthetic()) {
      return null;
    }
    //
    // We ignore identifiers that have already been resolved, such as identifiers representing the
    // name in a declaration.
    //
    if (node.getStaticElement() != null) {
      return null;
    }
    //
    // The name dynamic denotes a Type object even though dynamic is not a class.
    //
    if (node.getName().equals(dynamicType.getName())) {
      node.setStaticElement(dynamicType.getElement());
      node.setStaticType(typeType);
      return null;
    }
    //
    // Otherwise, the node should be resolved.
    //
    Element element = resolveSimpleIdentifier(node);
    ClassElement enclosingClass = resolver.getEnclosingClass();
    if (isFactoryConstructorReturnType(node) && element != enclosingClass) {
      resolver.reportErrorForNode(CompileTimeErrorCode.INVALID_FACTORY_NAME_NOT_A_CLASS, node);
    } else if (isConstructorReturnType(node) && element != enclosingClass) {
      resolver.reportErrorForNode(CompileTimeErrorCode.INVALID_CONSTRUCTOR_NAME, node);
      element = null;
    } else if (element == null || (element instanceof PrefixElement && !isValidAsPrefix(node))) {
      // TODO(brianwilkerson) Recover from this error.
      if (isConstructorReturnType(node)) {
        resolver.reportErrorForNode(CompileTimeErrorCode.INVALID_CONSTRUCTOR_NAME, node);
      } else if (node.getParent() instanceof Annotation) {
        Annotation annotation = (Annotation) node.getParent();
        resolver.reportErrorForNode(CompileTimeErrorCode.INVALID_ANNOTATION, annotation);
      } else {
        if (doesClassElementHaveProxy(resolver.getEnclosingClass())) {
          resolver.reportErrorForNode(StaticWarningCode.UNDEFINED_IDENTIFIER, node, node.getName());
        }
      }
    }

    node.setStaticElement(element);
    if (node.inSetterContext() && node.inGetterContext() && enclosingClass != null) {
      InterfaceType enclosingType = enclosingClass.getType();
      AuxiliaryElements auxiliaryElements = new AuxiliaryElements(lookUpGetter(
          null,
          enclosingType,
          node.getName()), null);
      node.setAuxiliaryElements(auxiliaryElements);
    }

    //
    // Validate annotation element.
    //
    if (node.getParent() instanceof Annotation) {
      Annotation annotation = (Annotation) node.getParent();
      resolveAnnotationElement(annotation);
    }
    return null;
  }

  @Override
  public Void visitSuperConstructorInvocation(SuperConstructorInvocation node) {
    ClassElement enclosingClass = resolver.getEnclosingClass();
    if (enclosingClass == null) {
      // TODO(brianwilkerson) Report this error.
      return null;
    }
    InterfaceType superType = enclosingClass.getSupertype();
    if (superType == null) {
      // TODO(brianwilkerson) Report this error.
      return null;
    }
    SimpleIdentifier name = node.getConstructorName();
    String superName = name != null ? name.getName() : null;
    ConstructorElement element = superType.lookUpConstructor(superName, definingLibrary);
    if (element == null) {
      if (name != null) {
        resolver.reportErrorForNode(
            CompileTimeErrorCode.UNDEFINED_CONSTRUCTOR_IN_INITIALIZER,
            node,
            superType.getDisplayName(),
            name);
      } else {
        resolver.reportErrorForNode(
            CompileTimeErrorCode.UNDEFINED_CONSTRUCTOR_IN_INITIALIZER_DEFAULT,
            node,
            superType.getDisplayName());
      }
      return null;
    } else {
      if (element.isFactory()) {
        resolver.reportErrorForNode(CompileTimeErrorCode.NON_GENERATIVE_CONSTRUCTOR, node, element);
      }
    }
    if (name != null) {
      name.setStaticElement(element);
    }
    node.setStaticElement(element);
    ArgumentList argumentList = node.getArgumentList();
    ParameterElement[] parameters = resolveArgumentsToFunction(
        isInConstConstructor(),
        argumentList,
        element);
    if (parameters != null) {
      argumentList.setCorrespondingStaticParameters(parameters);
    }
    return null;
  }

  @Override
  public Void visitSuperExpression(SuperExpression node) {
    if (!isSuperInValidContext(node)) {
      resolver.reportErrorForNode(CompileTimeErrorCode.SUPER_IN_INVALID_CONTEXT, node);
    }
    return super.visitSuperExpression(node);
  }

  @Override
  public Void visitTypeParameter(TypeParameter node) {
    setMetadata(node.getElement(), node);
    return null;
  }

  @Override
  public Void visitVariableDeclaration(VariableDeclaration node) {
    setMetadata(node.getElement(), node);
    return null;
  }

  /**
   * Generate annotation elements for each of the annotations in the given node list and add them to
   * the given list of elements.
   * 
   * @param annotationList the list of elements to which new elements are to be added
   * @param annotations the AST nodes used to generate new elements
   */
  private void addAnnotations(ArrayList<ElementAnnotationImpl> annotationList,
      NodeList<Annotation> annotations) {
    int annotationCount = annotations.size();
    for (int i = 0; i < annotationCount; i++) {
      Annotation annotation = annotations.get(i);
      Element resolvedElement = annotation.getElement();
      if (resolvedElement != null) {
        ElementAnnotationImpl elementAnnotation = new ElementAnnotationImpl(resolvedElement);
        annotation.setElementAnnotation(elementAnnotation);
        annotationList.add(elementAnnotation);
      }
    }
  }

  /**
   * Given that we have found code to invoke the given element, return the error code that should be
   * reported, or {@code null} if no error should be reported.
   * 
   * @param target the target of the invocation, or {@code null} if there was no target
   * @param useStaticContext
   * @param element the element to be invoked
   * @return the error code that should be reported
   */
  private ErrorCode checkForInvocationError(Expression target, boolean useStaticContext,
      Element element) {
    // Prefix is not declared, instead "prefix.id" are declared.
    if (element instanceof PrefixElement) {
      element = null;
    }
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
    } else if (element instanceof MultiplyDefinedElement) {
      // The error has already been reported
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
        Type variableType = ((VariableElement) element).getType();
        if (!isExecutableType(variableType)) {
          return StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION;
        }
      } else {
        if (target == null) {
          ClassElement enclosingClass = resolver.getEnclosingClass();
          if (enclosingClass == null) {
            return StaticTypeWarningCode.UNDEFINED_FUNCTION;
          } else if (element == null) {
            // Proxy-conditional warning, based on state of resolver.getEnclosingClass()
            return StaticTypeWarningCode.UNDEFINED_METHOD;
          } else {
            return StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION;
          }
        } else {
          Type targetType;
          if (useStaticContext) {
            targetType = getStaticType(target);
          } else {
            // Compute and use the propagated type, if it is null, then it may be the case that
            // static type is some type, in which the static type should be used.
            targetType = target.getBestType();
          }
          if (targetType == null) {
            return StaticTypeWarningCode.UNDEFINED_FUNCTION;
          } else if (!targetType.isDynamic() && !targetType.isBottom()) {
            // Proxy-conditional warning, based on state of targetType.getElement()
            return StaticTypeWarningCode.UNDEFINED_METHOD;
          }
        }
      }
    }
    return null;
  }

  /**
   * Check that the for some index expression that the method element was resolved, otherwise a
   * {@link StaticWarningCode#UNDEFINED_OPERATOR} is generated.
   * 
   * @param node the index expression to resolve
   * @param target the target of the expression
   * @param methodName the name of the operator associated with the context of using of the given
   *          index expression
   * @return {@code true} if and only if an error code is generated on the passed node
   */
  private boolean checkForUndefinedIndexOperator(IndexExpression node, Expression target,
      String methodName, MethodElement staticMethod, MethodElement propagatedMethod,
      Type staticType, Type propagatedType) {

    boolean shouldReportMissingMember_static = shouldReportMissingMember(staticType, staticMethod);
    boolean shouldReportMissingMember_propagated = !shouldReportMissingMember_static && enableHints
        && shouldReportMissingMember(propagatedType, propagatedMethod)
        && !memberFoundInSubclass(propagatedType.getElement(), methodName, true, false);

    if (shouldReportMissingMember_static || shouldReportMissingMember_propagated) {
      Token leftBracket = node.getLeftBracket();
      Token rightBracket = node.getRightBracket();
      ErrorCode errorCode = shouldReportMissingMember_static
          ? StaticTypeWarningCode.UNDEFINED_OPERATOR : HintCode.UNDEFINED_OPERATOR;
      if (leftBracket == null || rightBracket == null) {
        if (doesClassElementHaveProxy(shouldReportMissingMember_static ? staticType.getElement()
            : propagatedType.getElement())) {
          resolver.reportErrorForNode(errorCode, node, methodName, shouldReportMissingMember_static
              ? staticType.getDisplayName() : propagatedType.getDisplayName());
        }
      } else {
        int offset = leftBracket.getOffset();
        int length = rightBracket.getOffset() - offset + 1;
        if (doesClassElementHaveProxy(shouldReportMissingMember_static ? staticType.getElement()
            : propagatedType.getElement())) {
          resolver.reportErrorForOffset(
              errorCode,
              offset,
              length,
              methodName,
              shouldReportMissingMember_static ? staticType.getDisplayName()
                  : propagatedType.getDisplayName());
        }
      }
      return true;
    }
    return false;
  }

  /**
   * Given a list of arguments and the element that will be invoked using those argument, compute
   * the list of parameters that correspond to the list of arguments. Return the parameters that
   * correspond to the arguments, or {@code null} if no correspondence could be computed.
   * 
   * @param argumentList the list of arguments being passed to the element
   * @param executableElement the element that will be invoked with the arguments
   * @return the parameters that correspond to the arguments
   */
  private ParameterElement[] computeCorrespondingParameters(ArgumentList argumentList,
      Element element) {
    if (element instanceof PropertyAccessorElement) {
      //
      // This is an invocation of the call method defined on the value returned by the getter.
      //
      FunctionType getterType = ((PropertyAccessorElement) element).getType();
      if (getterType != null) {
        Type getterReturnType = getterType.getReturnType();
        if (getterReturnType instanceof InterfaceType) {
          MethodElement callMethod = ((InterfaceType) getterReturnType).lookUpMethod(
              FunctionElement.CALL_METHOD_NAME,
              definingLibrary);
          if (callMethod != null) {
            return resolveArgumentsToFunction(false, argumentList, callMethod);
          }
        } else if (getterReturnType instanceof FunctionType) {
          ParameterElement[] parameters = ((FunctionType) getterReturnType).getParameters();
          return resolveArgumentsToParameters(false, argumentList, parameters);
        }
      }
    } else if (element instanceof ExecutableElement) {
      return resolveArgumentsToFunction(false, argumentList, (ExecutableElement) element);
    } else if (element instanceof VariableElement) {
      VariableElement variable = (VariableElement) element;
      Type type = promoteManager.getStaticType(variable);
      if (type instanceof FunctionType) {
        FunctionType functionType = (FunctionType) type;
        ParameterElement[] parameters = functionType.getParameters();
        return resolveArgumentsToParameters(false, argumentList, parameters);
      } else if (type instanceof InterfaceType) {
        // "call" invocation
        MethodElement callMethod = ((InterfaceType) type).lookUpMethod(
            FunctionElement.CALL_METHOD_NAME,
            definingLibrary);
        if (callMethod != null) {
          ParameterElement[] parameters = callMethod.getParameters();
          return resolveArgumentsToParameters(false, argumentList, parameters);
        }
      }
    }
    return null;
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
   * Return {@code true} iff the passed {@link Element} is a {@link ClassElement} and either has, or
   * in that is or inherits proxy.
   * 
   * @param element the enclosing element
   * @return {@code true} iff the passed {@link Element} is a {@link ClassElement} and either has,
   *         or in that is or inherits proxy
   * @see ClassElement#isOrInheritsProxy()
   */
  private boolean doesClassElementHaveProxy(Element element) {
    if (element instanceof ClassElement) {
      return !((ClassElement) element).isOrInheritsProxy();
    }
    return true;
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
            element = MultiplyDefinedElementImpl.fromElements(
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
   * Assuming that the given expression is a prefix for a deferred import, return the library that
   * is being imported.
   * 
   * @param expression the expression representing the deferred import's prefix
   * @return the library that is being imported by the import associated with the prefix
   */
  private LibraryElement getImportedLibrary(Expression expression) {
    PrefixElement prefixElement = (PrefixElement) ((SimpleIdentifier) expression).getStaticElement();
    ImportElement[] imports = prefixElement.getEnclosingElement().getImportsWithPrefix(
        prefixElement);
    return imports[0].getImportedLibrary();
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
    Type propagatedType = resolveTypeParameter(expression.getPropagatedType());
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
      return resolver.getTypeProvider().getBottomType();
    }
    Type staticType = resolveTypeParameter(expression.getStaticType());
    if (staticType instanceof FunctionType) {
      //
      // All function types are subtypes of 'Function', which is itself a subclass of 'Object'.
      //
      staticType = resolver.getTypeProvider().getFunctionType();
    }
    return staticType;
  }

  /**
   * Return {@code true} if the given expression is a prefix for a deferred import.
   * 
   * @param expression the expression being tested
   * @return {@code true} if the given expression is a prefix for a deferred import
   */
  private boolean isDeferredPrefix(Expression expression) {
    if (!(expression instanceof SimpleIdentifier)) {
      return false;
    }
    Element element = ((SimpleIdentifier) expression).getStaticElement();
    if (!(element instanceof PrefixElement)) {
      return false;
    }
    PrefixElement prefixElement = (PrefixElement) element;
    ImportElement[] imports = prefixElement.getEnclosingElement().getImportsWithPrefix(
        prefixElement);
    if (imports.length != 1) {
      return false;
    }
    return imports[0].isDeferred();
  }

  /**
   * Return {@code true} if the given type represents an object that could be invoked using the call
   * operator '()'.
   * 
   * @param type the type being tested
   * @return {@code true} if the given type represents an object that could be invoked
   */
  private boolean isExecutableType(Type type) {
    if (type.isDynamic() || (type instanceof FunctionType) || type.isDartCoreFunction()
        || type.isObject()) {
      return true;
    } else if (type instanceof InterfaceType) {
      ClassElement classElement = ((InterfaceType) type).getElement();
      // 16078 from Gilad: If the type is a Functor with the @proxy annotation, treat it as an
      // executable type.
      // example code: NonErrorResolverTest.test_invocationOfNonFunction_proxyOnFunctionClass()
      if (classElement.isProxy() && type.isSubtypeOf(resolver.getTypeProvider().getFunctionType())) {
        return true;
      }
      MethodElement methodElement = classElement.lookUpMethod(
          FunctionElement.CALL_METHOD_NAME,
          definingLibrary);
      return methodElement != null;
    }
    return false;
  }

  /**
   * @return {@code true} iff current enclosing function is constant constructor declaration.
   */
  private boolean isInConstConstructor() {
    ExecutableElement function = resolver.getEnclosingFunction();
    if (function instanceof ConstructorElement) {
      return ((ConstructorElement) function).isConst();
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
   * Return {@code true} if the given node can validly be resolved to a prefix:
   * <ul>
   * <li>it is the prefix in an import directive, or</li>
   * <li>it is the prefix in a prefixed identifier.</li>
   * </ul>
   * 
   * @param node the node being tested
   * @return {@code true} if the given node is the prefix in an import directive
   */
  private boolean isValidAsPrefix(SimpleIdentifier node) {
    AstNode parent = node.getParent();
    if (parent instanceof ImportDirective) {
      return ((ImportDirective) parent).getPrefix() == node;
    } else if (parent instanceof PrefixedIdentifier) {
      return true;
    } else if (parent instanceof MethodInvocation) {
      return ((MethodInvocation) parent).getTarget() == node;
    }
    return false;
  }

  /**
   * Look up the getter with the given name in the given type. Return the element representing the
   * getter that was found, or {@code null} if there is no getter with the given name.
   * 
   * @param target the target of the invocation, or {@code null} if there is no target
   * @param type the type in which the getter is defined
   * @param getterName the name of the getter being looked up
   * @return the element representing the getter that was found
   */
  private PropertyAccessorElement lookUpGetter(Expression target, Type type, String getterName) {
    type = resolveTypeParameter(type);
    if (type instanceof InterfaceType) {
      InterfaceType interfaceType = (InterfaceType) type;
      PropertyAccessorElement accessor;
      if (target instanceof SuperExpression) {
        accessor = interfaceType.lookUpGetterInSuperclass(getterName, definingLibrary);
      } else {
        accessor = interfaceType.lookUpGetter(getterName, definingLibrary);
      }
      if (accessor != null) {
        return accessor;
      }
      return lookUpGetterInInterfaces(interfaceType, false, getterName, new HashSet<ClassElement>());
    }
    return null;
  }

  /**
   * Look up the getter with the given name in the interfaces implemented by the given type, either
   * directly or indirectly. Return the element representing the getter that was found, or
   * {@code null} if there is no getter with the given name.
   * 
   * @param targetType the type in which the getter might be defined
   * @param includeTargetType {@code true} if the search should include the target type
   * @param getterName the name of the getter being looked up
   * @param visitedInterfaces a set containing all of the interfaces that have been examined, used
   *          to prevent infinite recursion and to optimize the search
   * @return the element representing the getter that was found
   */
  private PropertyAccessorElement lookUpGetterInInterfaces(InterfaceType targetType,
      boolean includeTargetType, String getterName, HashSet<ClassElement> visitedInterfaces) {
    // TODO(brianwilkerson) This isn't correct. Section 8.1.1 of the specification (titled
    // "Inheritance and Overriding" under "Interfaces") describes a much more complex scheme for
    // finding the inherited member. We need to follow that scheme. The code below should cover the
    // 80% case.
    ClassElement targetClass = targetType.getElement();
    if (visitedInterfaces.contains(targetClass)) {
      return null;
    }
    visitedInterfaces.add(targetClass);
    if (includeTargetType) {
      PropertyAccessorElement getter = targetType.getGetter(getterName);
      if (getter != null && getter.isAccessibleIn(definingLibrary)) {
        return getter;
      }
    }
    for (InterfaceType interfaceType : targetType.getInterfaces()) {
      PropertyAccessorElement getter = lookUpGetterInInterfaces(
          interfaceType,
          true,
          getterName,
          visitedInterfaces);
      if (getter != null) {
        return getter;
      }
    }
    for (InterfaceType mixinType : targetType.getMixins()) {
      PropertyAccessorElement getter = lookUpGetterInInterfaces(
          mixinType,
          true,
          getterName,
          visitedInterfaces);
      if (getter != null) {
        return getter;
      }
    }
    InterfaceType superclass = targetType.getSuperclass();
    if (superclass == null) {
      return null;
    }
    return lookUpGetterInInterfaces(superclass, true, getterName, visitedInterfaces);
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
    type = resolveTypeParameter(type);
    if (type instanceof InterfaceType) {
      InterfaceType interfaceType = (InterfaceType) type;
      ExecutableElement member = interfaceType.lookUpMethod(memberName, definingLibrary);
      if (member != null) {
        return member;
      }
      member = interfaceType.lookUpGetter(memberName, definingLibrary);
      if (member != null) {
        return member;
      }
      return lookUpGetterOrMethodInInterfaces(
          interfaceType,
          false,
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
   * @param includeTargetType {@code true} if the search should include the target type
   * @param memberName the name of the method or getter being looked up
   * @param visitedInterfaces a set containing all of the interfaces that have been examined, used
   *          to prevent infinite recursion and to optimize the search
   * @return the element representing the method or getter that was found
   */
  private ExecutableElement lookUpGetterOrMethodInInterfaces(InterfaceType targetType,
      boolean includeTargetType, String memberName, HashSet<ClassElement> visitedInterfaces) {
    // TODO(brianwilkerson) This isn't correct. Section 8.1.1 of the specification (titled
    // "Inheritance and Overriding" under "Interfaces") describes a much more complex scheme for
    // finding the inherited member. We need to follow that scheme. The code below should cover the
    // 80% case.
    ClassElement targetClass = targetType.getElement();
    if (visitedInterfaces.contains(targetClass)) {
      return null;
    }
    visitedInterfaces.add(targetClass);
    if (includeTargetType) {
      ExecutableElement member = targetType.getMethod(memberName);
      if (member != null) {
        return member;
      }
      member = targetType.getGetter(memberName);
      if (member != null) {
        return member;
      }
    }
    for (InterfaceType interfaceType : targetType.getInterfaces()) {
      ExecutableElement member = lookUpGetterOrMethodInInterfaces(
          interfaceType,
          true,
          memberName,
          visitedInterfaces);
      if (member != null) {
        return member;
      }
    }
    for (InterfaceType mixinType : targetType.getMixins()) {
      ExecutableElement member = lookUpGetterOrMethodInInterfaces(
          mixinType,
          true,
          memberName,
          visitedInterfaces);
      if (member != null) {
        return member;
      }
    }
    InterfaceType superclass = targetType.getSuperclass();
    if (superclass == null) {
      return null;
    }
    return lookUpGetterOrMethodInInterfaces(superclass, true, memberName, visitedInterfaces);
  }

  /**
   * Find the element corresponding to the given label node in the current label scope.
   * 
   * @param parentNode the node containing the given label
   * @param labelNode the node representing the label being looked up
   * @return the element corresponding to the given label node in the current scope
   */
  private LabelElementImpl lookupLabel(AstNode parentNode, SimpleIdentifier labelNode) {
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
        resolver.reportErrorForNode(
            CompileTimeErrorCode.LABEL_UNDEFINED,
            labelNode,
            labelNode.getName());
      } else {
        labelElement = (LabelElementImpl) labelScope.lookup(labelNode.getName());
        if (labelElement == null) {
          resolver.reportErrorForNode(
              CompileTimeErrorCode.LABEL_UNDEFINED,
              labelNode,
              labelNode.getName());
        } else {
          labelNode.setStaticElement(labelElement);
        }
      }
    }
    if (labelElement != null) {
      ExecutableElement labelContainer = labelElement.getAncestor(ExecutableElement.class);
      if (labelContainer != resolver.getEnclosingFunction()) {
        resolver.reportErrorForNode(
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
   * @param target the target of the invocation, or {@code null} if there is no target
   * @param type the type in which the method is defined
   * @param methodName the name of the method being looked up
   * @return the element representing the method that was found
   */
  private MethodElement lookUpMethod(Expression target, Type type, String methodName) {
    type = resolveTypeParameter(type);
    if (type instanceof InterfaceType) {
      InterfaceType interfaceType = (InterfaceType) type;
      MethodElement method;
      if (target instanceof SuperExpression) {
        method = interfaceType.lookUpMethodInSuperclass(methodName, definingLibrary);
      } else {
        method = interfaceType.lookUpMethod(methodName, definingLibrary);
      }
      if (method != null) {
        return method;
      }
      return lookUpMethodInInterfaces(interfaceType, false, methodName, new HashSet<ClassElement>());
    }

    if (type instanceof UnionType) {
      Set<ExecutableElement> methods = new HashSet<ExecutableElement>();
      for (Type t : ((UnionType) type).getElements()) {
        MethodElement m = lookUpMethod(target, t, methodName);
        if (m != null) {
          methods.add(m);
        }
      }
      // TODO (collinsn): I want [computeMergedExecutableElement] to be general
      // and work with functions, methods, constructors, and property accessors. However,
      // I won't be able to assume it returns [MethodElement] here then.
      return (MethodElement) computeMergedExecutableElement(methods);
    }

    return null;
  }

  /**
   * Look up the method with the given name in the interfaces implemented by the given type, either
   * directly or indirectly. Return the element representing the method that was found, or
   * {@code null} if there is no method with the given name.
   * 
   * @param targetType the type in which the member might be defined
   * @param includeTargetType {@code true} if the search should include the target type
   * @param methodName the name of the method being looked up
   * @param visitedInterfaces a set containing all of the interfaces that have been examined, used
   *          to prevent infinite recursion and to optimize the search
   * @return the element representing the method that was found
   */
  private MethodElement lookUpMethodInInterfaces(InterfaceType targetType,
      boolean includeTargetType, String methodName, HashSet<ClassElement> visitedInterfaces) {
    // TODO(brianwilkerson) This isn't correct. Section 8.1.1 of the specification (titled
    // "Inheritance and Overriding" under "Interfaces") describes a much more complex scheme for
    // finding the inherited member. We need to follow that scheme. The code below should cover the
    // 80% case.
    ClassElement targetClass = targetType.getElement();
    if (visitedInterfaces.contains(targetClass)) {
      return null;
    }
    visitedInterfaces.add(targetClass);
    if (includeTargetType) {
      MethodElement method = targetType.getMethod(methodName);
      if (method != null && method.isAccessibleIn(definingLibrary)) {
        return method;
      }
    }
    for (InterfaceType interfaceType : targetType.getInterfaces()) {
      MethodElement method = lookUpMethodInInterfaces(
          interfaceType,
          true,
          methodName,
          visitedInterfaces);
      if (method != null) {
        return method;
      }
    }
    for (InterfaceType mixinType : targetType.getMixins()) {
      MethodElement method = lookUpMethodInInterfaces(
          mixinType,
          true,
          methodName,
          visitedInterfaces);
      if (method != null) {
        return method;
      }
    }
    InterfaceType superclass = targetType.getSuperclass();
    if (superclass == null) {
      return null;
    }
    return lookUpMethodInInterfaces(superclass, true, methodName, visitedInterfaces);
  }

  /**
   * Look up the setter with the given name in the given type. Return the element representing the
   * setter that was found, or {@code null} if there is no setter with the given name.
   * 
   * @param target the target of the invocation, or {@code null} if there is no target
   * @param type the type in which the setter is defined
   * @param setterName the name of the setter being looked up
   * @return the element representing the setter that was found
   */
  private PropertyAccessorElement lookUpSetter(Expression target, Type type, String setterName) {
    type = resolveTypeParameter(type);
    if (type instanceof InterfaceType) {
      InterfaceType interfaceType = (InterfaceType) type;
      PropertyAccessorElement accessor;
      if (target instanceof SuperExpression) {
        accessor = interfaceType.lookUpSetterInSuperclass(setterName, definingLibrary);
      } else {
        accessor = interfaceType.lookUpSetter(setterName, definingLibrary);
      }
      if (accessor != null) {
        return accessor;
      }
      return lookUpSetterInInterfaces(interfaceType, false, setterName, new HashSet<ClassElement>());
    }
    return null;
  }

  /**
   * Look up the setter with the given name in the interfaces implemented by the given type, either
   * directly or indirectly. Return the element representing the setter that was found, or
   * {@code null} if there is no setter with the given name.
   * 
   * @param targetType the type in which the setter might be defined
   * @param includeTargetType {@code true} if the search should include the target type
   * @param setterName the name of the setter being looked up
   * @param visitedInterfaces a set containing all of the interfaces that have been examined, used
   *          to prevent infinite recursion and to optimize the search
   * @return the element representing the setter that was found
   */
  private PropertyAccessorElement lookUpSetterInInterfaces(InterfaceType targetType,
      boolean includeTargetType, String setterName, HashSet<ClassElement> visitedInterfaces) {
    // TODO(brianwilkerson) This isn't correct. Section 8.1.1 of the specification (titled
    // "Inheritance and Overriding" under "Interfaces") describes a much more complex scheme for
    // finding the inherited member. We need to follow that scheme. The code below should cover the
    // 80% case.
    ClassElement targetClass = targetType.getElement();
    if (visitedInterfaces.contains(targetClass)) {
      return null;
    }
    visitedInterfaces.add(targetClass);
    if (includeTargetType) {
      PropertyAccessorElement setter = targetType.getSetter(setterName);
      if (setter != null && setter.isAccessibleIn(definingLibrary)) {
        return setter;
      }
    }
    for (InterfaceType interfaceType : targetType.getInterfaces()) {
      PropertyAccessorElement setter = lookUpSetterInInterfaces(
          interfaceType,
          true,
          setterName,
          visitedInterfaces);
      if (setter != null) {
        return setter;
      }
    }
    for (InterfaceType mixinType : targetType.getMixins()) {
      PropertyAccessorElement setter = lookUpSetterInInterfaces(
          mixinType,
          true,
          setterName,
          visitedInterfaces);
      if (setter != null) {
        return setter;
      }
    }
    InterfaceType superclass = targetType.getSuperclass();
    if (superclass == null) {
      return null;
    }
    return lookUpSetterInInterfaces(superclass, true, setterName, visitedInterfaces);
  }

  /**
   * Given some class element, this method uses {@link #subtypeManager} to find the set of all
   * subtypes; the subtypes are then searched for a member (method, getter, or setter), that matches
   * a passed
   * 
   * @param element the class element to search the subtypes of, if a non-ClassElement element is
   *          passed, then {@code false} is returned
   * @param memberName the member name to search for
   * @param asMethod {@code true} if the methods should be searched for in the subtypes
   * @param asAccessor {@code true} if the accessors (getters and setters) should be searched for in
   *          the subtypes
   * @return {@code true} if and only if the passed memberName was found in a subtype
   */
  private boolean memberFoundInSubclass(Element element, String memberName, boolean asMethod,
      boolean asAccessor) {
    if (element instanceof ClassElement) {
      subtypeManager.ensureLibraryVisited(definingLibrary);
      HashSet<ClassElement> subtypeElements = subtypeManager.computeAllSubtypes((ClassElement) element);
      for (ClassElement subtypeElement : subtypeElements) {
        if (asMethod && subtypeElement.getMethod(memberName) != null) {
          return true;
        } else if (asAccessor
            && (subtypeElement.getGetter(memberName) != null || subtypeElement.getSetter(memberName) != null)) {
          return true;
        }
      }
    }
    return false;
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
      default:
        // Internal error: Unmapped assignment operator.
        AnalysisEngine.getInstance().getLogger().logError(
            "Failed to map " + operator.getLexeme() + " to it's corresponding operator");
        return operator;
    }
  }

  private void resolveAnnotationConstructorInvocationArguments(Annotation annotation,
      ConstructorElement constructor) {
    ArgumentList argumentList = annotation.getArguments();
    // error will be reported in ConstantVerifier
    if (argumentList == null) {
      return;
    }
    // resolve arguments to parameters
    ParameterElement[] parameters = resolveArgumentsToFunction(true, argumentList, constructor);
    if (parameters != null) {
      argumentList.setCorrespondingStaticParameters(parameters);
    }
  }

  /**
   * Continues resolution of the given {@link Annotation}.
   * 
   * @param annotation the {@link Annotation} to resolve
   */
  private void resolveAnnotationElement(Annotation annotation) {
    SimpleIdentifier nameNode1;
    SimpleIdentifier nameNode2;
    {
      Identifier annName = annotation.getName();
      if (annName instanceof PrefixedIdentifier) {
        PrefixedIdentifier prefixed = (PrefixedIdentifier) annName;
        nameNode1 = prefixed.getPrefix();
        nameNode2 = prefixed.getIdentifier();
      } else {
        nameNode1 = (SimpleIdentifier) annName;
        nameNode2 = null;
      }
    }
    SimpleIdentifier nameNode3 = annotation.getConstructorName();
    ConstructorElement constructor = null;
    //
    // CONST or Class(args)
    //
    if (nameNode1 != null && nameNode2 == null && nameNode3 == null) {
      Element element1 = nameNode1.getStaticElement();
      // CONST
      if (element1 instanceof PropertyAccessorElement) {
        resolveAnnotationElementGetter(annotation, (PropertyAccessorElement) element1);
        return;
      }
      // Class(args)
      if (element1 instanceof ClassElement) {
        ClassElement classElement = (ClassElement) element1;
        constructor = new InterfaceTypeImpl(classElement).lookUpConstructor(null, definingLibrary);
      }
    }
    //
    // prefix.CONST or prefix.Class() or Class.CONST or Class.constructor(args)
    //
    if (nameNode1 != null && nameNode2 != null && nameNode3 == null) {
      Element element1 = nameNode1.getStaticElement();
      Element element2 = nameNode2.getStaticElement();
      // Class.CONST - not resolved yet
      if (element1 instanceof ClassElement) {
        ClassElement classElement = (ClassElement) element1;
        element2 = classElement.lookUpGetter(nameNode2.getName(), definingLibrary);
      }
      // prefix.CONST or Class.CONST
      if (element2 instanceof PropertyAccessorElement) {
        nameNode2.setStaticElement(element2);
        annotation.setElement(element2);
        resolveAnnotationElementGetter(annotation, (PropertyAccessorElement) element2);
        return;
      }
      // prefix.Class()
      if (element2 instanceof ClassElement) {
        ClassElement classElement = (ClassElement) element2;
        constructor = classElement.getUnnamedConstructor();
      }
      // Class.constructor(args)
      if (element1 instanceof ClassElement) {
        ClassElement classElement = (ClassElement) element1;
        constructor = new InterfaceTypeImpl(classElement).lookUpConstructor(
            nameNode2.getName(),
            definingLibrary);
        nameNode2.setStaticElement(constructor);
      }
    }
    //
    // prefix.Class.CONST or prefix.Class.constructor(args)
    //
    if (nameNode1 != null && nameNode2 != null && nameNode3 != null) {
      Element element2 = nameNode2.getStaticElement();
      // element2 should be ClassElement
      if (element2 instanceof ClassElement) {
        ClassElement classElement = (ClassElement) element2;
        String name3 = nameNode3.getName();
        // prefix.Class.CONST
        PropertyAccessorElement getter = classElement.lookUpGetter(name3, definingLibrary);
        if (getter != null) {
          nameNode3.setStaticElement(getter);
          annotation.setElement(element2);
          resolveAnnotationElementGetter(annotation, getter);
          return;
        }
        // prefix.Class.constructor(args)
        constructor = new InterfaceTypeImpl(classElement).lookUpConstructor(name3, definingLibrary);
        nameNode3.setStaticElement(constructor);
      }
    }
    // we need constructor
    if (constructor == null) {
      resolver.reportErrorForNode(CompileTimeErrorCode.INVALID_ANNOTATION, annotation);
      return;
    }
    // record element
    annotation.setElement(constructor);
    // resolve arguments
    resolveAnnotationConstructorInvocationArguments(annotation, constructor);
  }

  private void resolveAnnotationElementGetter(Annotation annotation,
      PropertyAccessorElement accessorElement) {
    // accessor should be synthetic
    if (!accessorElement.isSynthetic()) {
      resolver.reportErrorForNode(CompileTimeErrorCode.INVALID_ANNOTATION, annotation);
      return;
    }
    // variable should be constant
    VariableElement variableElement = accessorElement.getVariable();
    if (!variableElement.isConst()) {
      resolver.reportErrorForNode(CompileTimeErrorCode.INVALID_ANNOTATION, annotation);
    }
    // OK
    return;
  }

  /**
   * Given a list of arguments and the element that will be invoked using those argument, compute
   * the list of parameters that correspond to the list of arguments. Return the parameters that
   * correspond to the arguments, or {@code null} if no correspondence could be computed.
   * 
   * @param reportError if {@code true} then compile-time error should be reported; if {@code false}
   *          then compile-time warning
   * @param argumentList the list of arguments being passed to the element
   * @param executableElement the element that will be invoked with the arguments
   * @return the parameters that correspond to the arguments
   */
  private ParameterElement[] resolveArgumentsToFunction(boolean reportError,
      ArgumentList argumentList, ExecutableElement executableElement) {
    if (executableElement == null) {
      return null;
    }
    ParameterElement[] parameters = executableElement.getParameters();
    return resolveArgumentsToParameters(reportError, argumentList, parameters);
  }

  /**
   * Given a list of arguments and the parameters related to the element that will be invoked using
   * those argument, compute the list of parameters that correspond to the list of arguments. Return
   * the parameters that correspond to the arguments.
   * 
   * @param reportError if {@code true} then compile-time error should be reported; if {@code false}
   *          then compile-time warning
   * @param argumentList the list of arguments being passed to the element
   * @param parameters the of the function that will be invoked with the arguments
   * @return the parameters that correspond to the arguments
   */
  private ParameterElement[] resolveArgumentsToParameters(boolean reportError,
      ArgumentList argumentList, ParameterElement[] parameters) {
    ArrayList<ParameterElement> requiredParameters = new ArrayList<ParameterElement>();
    ArrayList<ParameterElement> positionalParameters = new ArrayList<ParameterElement>();
    HashMap<String, ParameterElement> namedParameters = new HashMap<String, ParameterElement>();
    for (ParameterElement parameter : parameters) {
      ParameterKind kind = parameter.getParameterKind();
      if (kind == ParameterKind.REQUIRED) {
        requiredParameters.add(parameter);
      } else if (kind == ParameterKind.POSITIONAL) {
        positionalParameters.add(parameter);
      } else {
        namedParameters.put(parameter.getName(), parameter);
      }
    }
    ArrayList<ParameterElement> unnamedParameters = new ArrayList<ParameterElement>(
        requiredParameters);
    unnamedParameters.addAll(positionalParameters);
    int unnamedParameterCount = unnamedParameters.size();
    int unnamedIndex = 0;

    NodeList<Expression> arguments = argumentList.getArguments();
    int argumentCount = arguments.size();
    ParameterElement[] resolvedParameters = new ParameterElement[argumentCount];
    int positionalArgumentCount = 0;
    HashSet<String> usedNames = new HashSet<String>();
    boolean noBlankArguments = true;
    for (int i = 0; i < argumentCount; i++) {
      Expression argument = arguments.get(i);
      if (argument instanceof NamedExpression) {
        SimpleIdentifier nameNode = ((NamedExpression) argument).getName().getLabel();
        String name = nameNode.getName();
        ParameterElement element = namedParameters.get(name);
        if (element == null) {
          ErrorCode errorCode = reportError ? CompileTimeErrorCode.UNDEFINED_NAMED_PARAMETER
              : StaticWarningCode.UNDEFINED_NAMED_PARAMETER;
          resolver.reportErrorForNode(errorCode, nameNode, name);
        } else {
          resolvedParameters[i] = element;
          nameNode.setStaticElement(element);
        }
        if (!usedNames.add(name)) {
          resolver.reportErrorForNode(CompileTimeErrorCode.DUPLICATE_NAMED_ARGUMENT, nameNode, name);
        }
      } else {
        if (argument instanceof SimpleIdentifier
            && ((SimpleIdentifier) argument).getName().isEmpty()) {
          noBlankArguments = false;
        }
        positionalArgumentCount++;
        if (unnamedIndex < unnamedParameterCount) {
          resolvedParameters[i] = unnamedParameters.get(unnamedIndex++);
        }
      }
    }
    if (positionalArgumentCount < requiredParameters.size() && noBlankArguments) {
      ErrorCode errorCode = reportError ? CompileTimeErrorCode.NOT_ENOUGH_REQUIRED_ARGUMENTS
          : StaticWarningCode.NOT_ENOUGH_REQUIRED_ARGUMENTS;
      resolver.reportErrorForNode(
          errorCode,
          argumentList,
          requiredParameters.size(),
          positionalArgumentCount);
    } else if (positionalArgumentCount > unnamedParameterCount && noBlankArguments) {
      ErrorCode errorCode = reportError ? CompileTimeErrorCode.EXTRA_POSITIONAL_ARGUMENTS
          : StaticWarningCode.EXTRA_POSITIONAL_ARGUMENTS;
      resolver.reportErrorForNode(
          errorCode,
          argumentList,
          unnamedParameterCount,
          positionalArgumentCount);
    }
    return resolvedParameters;
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
    Namespace namespace = new NamespaceBuilder().createExportNamespaceForLibrary(library);
    for (Combinator combinator : combinators) {
      NodeList<SimpleIdentifier> names;
      if (combinator instanceof HideCombinator) {
        names = ((HideCombinator) combinator).getHiddenNames();
      } else {
        names = ((ShowCombinator) combinator).getShownNames();
      }
      for (SimpleIdentifier name : names) {
        String nameStr = name.getName();
        Element element = namespace.get(nameStr);
        if (element == null) {
          element = namespace.get(nameStr + "=");
        }
        if (element != null) {
          // Ensure that the name always resolves to a top-level variable
          // rather than a getter or setter
          if (element instanceof PropertyAccessorElement) {
            element = ((PropertyAccessorElement) element).getVariable();
          }
          name.setStaticElement(element);
        }
      }
    }
  }

  /**
   * Given an invocation of the form 'C.x()' where 'C' is a class, find and return the element 'x'
   * in 'C'.
   * 
   * @param classElement the class element
   * @param nameNode the member name node
   */
  private Element resolveElement(ClassElementImpl classElement, SimpleIdentifier nameNode) {
    String name = nameNode.getName();
    Element element = classElement.getMethod(name);
    if (element == null && nameNode.inSetterContext()) {
      element = classElement.getSetter(name);
    }
    if (element == null && nameNode.inGetterContext()) {
      element = classElement.getGetter(name);
    }
    if (element != null && element.isAccessibleIn(definingLibrary)) {
      return element;
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
    Element element = resolver.getNameScope().lookup(methodName, definingLibrary);
    if (element == null) {
      //
      // If it isn't defined in the lexical scope, and the invocation is within a class, then look
      // in the inheritance scope.
      //
      ClassElement enclosingClass = resolver.getEnclosingClass();
      if (enclosingClass != null) {
        InterfaceType enclosingType = enclosingClass.getType();
        element = lookUpMethod(null, enclosingType, methodName.getName());
        if (element == null) {
          //
          // If there's no method, then it's possible that 'm' is a getter that returns a function.
          //
          element = lookUpGetter(null, enclosingType, methodName.getName());
        }
      }
    }
    // TODO(brianwilkerson) Report this error.
    return element;
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
  private Element resolveInvokedElementWithTarget(Expression target, Type targetType,
      SimpleIdentifier methodName) {
    if (targetType instanceof InterfaceType || targetType instanceof UnionType) {
      Element element = lookUpMethod(target, targetType, methodName.getName());
      if (element == null) {
        //
        // If there's no method, then it's possible that 'm' is a getter that returns a function.
        //
        // TODO (collinsn): need to add union type support here too, in the style of [lookUpMethod].
        element = lookUpGetter(target, targetType, methodName.getName());
      }
      return element;
    } else if (target instanceof SimpleIdentifier) {
      Element targetElement = ((SimpleIdentifier) target).getStaticElement();
      if (targetElement instanceof PrefixElement) {
        //
        // Look to see whether the name of the method is really part of a prefixed identifier for an
        // imported top-level function or top-level getter that returns a function.
        //
        final String name = ((SimpleIdentifier) target).getName() + "." + methodName;
        Identifier functionName = new SyntheticIdentifier(name);
        Element element = resolver.getNameScope().lookup(functionName, definingLibrary);
        if (element != null) {
          // TODO(brianwilkerson) This isn't a method invocation, it's a function invocation where
          // the function name is a prefixed identifier. Consider re-writing the AST.
          return element;
        }
      }
    }
    // TODO(brianwilkerson) Report this error.
    return null;
  }

  /**
   * Given that we are accessing a property of the given type with the given name, return the
   * element that represents the property.
   * 
   * @param target the target of the invocation ('e')
   * @param targetType the type in which the search for the property should begin
   * @param propertyName the name of the property being accessed
   * @return the element that represents the property
   */
  private ExecutableElement resolveProperty(Expression target, Type targetType,
      SimpleIdentifier propertyName) {
    ExecutableElement memberElement = null;
    if (propertyName.inSetterContext()) {
      memberElement = lookUpSetter(target, targetType, propertyName.getName());
    }
    if (memberElement == null) {
      memberElement = lookUpGetter(target, targetType, propertyName.getName());
    }
    if (memberElement == null) {
      memberElement = lookUpMethod(target, targetType, propertyName.getName());
    }
    return memberElement;
  }

  private void resolvePropertyAccess(Expression target, SimpleIdentifier propertyName) {
    Type staticType = getStaticType(target);
    Type propagatedType = getPropagatedType(target);

    Element staticElement = null;
    Element propagatedElement = null;

    //
    // If this property access is of the form 'C.m' where 'C' is a class, then we don't call
    // resolveProperty(..) which walks up the class hierarchy, instead we just look for the
    // member in the type only.
    //
    ClassElementImpl typeReference = getTypeReference(target);
    if (typeReference != null) {
      // TODO(brianwilkerson) Why are we setting the propagated element here? It looks wrong.
      staticElement = propagatedElement = resolveElement(typeReference, propertyName);
    } else {
      staticElement = resolveProperty(target, staticType, propertyName);
      propagatedElement = resolveProperty(target, propagatedType, propertyName);
    }

    // May be part of annotation, record property element only if exists.
    // Error was already reported in validateAnnotationElement().
    if (target.getParent().getParent() instanceof Annotation) {
      if (staticElement != null) {
        propertyName.setStaticElement(staticElement);
      }
      return;
    }

    propertyName.setStaticElement(staticElement);
    propertyName.setPropagatedElement(propagatedElement);

    boolean shouldReportMissingMember_static = shouldReportMissingMember(staticType, staticElement);
    boolean shouldReportMissingMember_propagated = !shouldReportMissingMember_static && enableHints
        ? shouldReportMissingMember(propagatedType, propagatedElement) : false;

    // If we are about to generate the hint (propagated version of this warning), then check
    // that the member is not in a subtype of the propagated type.
    if (shouldReportMissingMember_propagated) {
      if (memberFoundInSubclass(propagatedType.getElement(), propertyName.getName(), false, true)) {
        shouldReportMissingMember_propagated = false;
      }
    }

    if (shouldReportMissingMember_static || shouldReportMissingMember_propagated) {
      if (staticType.isVoid()) {
        if (propertyName.inSetterContext()) {
          ErrorCode errorCode = shouldReportMissingMember_static
              ? StaticTypeWarningCode.UNDEFINED_SETTER : HintCode.UNDEFINED_SETTER;
          resolver.reportErrorForNode(
              errorCode,
              propertyName,
              propertyName.getName(),
              staticType.getDisplayName());
        } else if (propertyName.inGetterContext()) {
          ErrorCode errorCode = shouldReportMissingMember_static
              ? StaticTypeWarningCode.UNDEFINED_GETTER : HintCode.UNDEFINED_GETTER;
          resolver.reportErrorForNode(
              errorCode,
              propertyName,
              propertyName.getName(),
              staticType.getDisplayName());
        } else {
          resolver.reportErrorForNode(
              StaticWarningCode.UNDEFINED_IDENTIFIER,
              propertyName,
              propertyName.getName());
        }
      }
      Element staticOrPropagatedEnclosingElt = shouldReportMissingMember_static
          ? staticType.getElement() : propagatedType.getElement();
      if (staticOrPropagatedEnclosingElt != null) {
        boolean isStaticProperty = isStatic(staticOrPropagatedEnclosingElt);
        if (propertyName.inSetterContext()) {
          if (isStaticProperty) {
            ErrorCode errorCode = shouldReportMissingMember_static
                ? StaticWarningCode.UNDEFINED_SETTER : HintCode.UNDEFINED_SETTER;
            if (doesClassElementHaveProxy(staticOrPropagatedEnclosingElt)) {
              resolver.reportErrorForNode(
                  errorCode,
                  propertyName,
                  propertyName.getName(),
                  staticOrPropagatedEnclosingElt.getDisplayName());
            }
          } else {
            ErrorCode errorCode = shouldReportMissingMember_static
                ? StaticTypeWarningCode.UNDEFINED_SETTER : HintCode.UNDEFINED_SETTER;
            if (doesClassElementHaveProxy(staticOrPropagatedEnclosingElt)) {
              resolver.reportErrorForNode(
                  errorCode,
                  propertyName,
                  propertyName.getName(),
                  staticOrPropagatedEnclosingElt.getDisplayName());
            }
          }
        } else if (propertyName.inGetterContext()) {
          if (isStaticProperty) {
            ErrorCode errorCode = shouldReportMissingMember_static
                ? StaticWarningCode.UNDEFINED_GETTER : HintCode.UNDEFINED_GETTER;
            if (doesClassElementHaveProxy(staticOrPropagatedEnclosingElt)) {
              resolver.reportErrorForNode(
                  errorCode,
                  propertyName,
                  propertyName.getName(),
                  staticOrPropagatedEnclosingElt.getDisplayName());
            }
          } else {
            if (staticOrPropagatedEnclosingElt instanceof ClassElement) {
              ClassElement classElement = (ClassElement) staticOrPropagatedEnclosingElt;
              InterfaceType targetType = classElement.getType();
              if (targetType != null && targetType.isDartCoreFunction()
                  && propertyName.getName().equals(FunctionElement.CALL_METHOD_NAME)) {
                // TODO(brianwilkerson) Can we ever resolve the function being invoked?
                //resolveArgumentsToParameters(node.getArgumentList(), invokedFunction);
                return;
              } else if (classElement.isEnum() && propertyName.getName().equals("_name")) {
                resolver.reportErrorForNode(
                    CompileTimeErrorCode.ACCESS_PRIVATE_ENUM_FIELD,
                    propertyName,
                    propertyName.getName());
                return;
              }
            }
            ErrorCode errorCode = shouldReportMissingMember_static
                ? StaticTypeWarningCode.UNDEFINED_GETTER : HintCode.UNDEFINED_GETTER;
            if (doesClassElementHaveProxy(staticOrPropagatedEnclosingElt)) {
              resolver.reportErrorForNode(
                  errorCode,
                  propertyName,
                  propertyName.getName(),
                  staticOrPropagatedEnclosingElt.getDisplayName());
            }
          }
        } else {
          if (doesClassElementHaveProxy(staticOrPropagatedEnclosingElt)) {
            resolver.reportErrorForNode(
                StaticWarningCode.UNDEFINED_IDENTIFIER,
                propertyName,
                propertyName.getName());
          }
        }
      }
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
    Element element = resolver.getNameScope().lookup(node, definingLibrary);
    if (element instanceof PropertyAccessorElement && node.inSetterContext()) {
      PropertyInducingElement variable = ((PropertyAccessorElement) element).getVariable();
      if (variable != null) {
        PropertyAccessorElement setter = variable.getSetter();
        if (setter == null) {
          //
          // Check to see whether there might be a locally defined getter and an inherited setter.
          //
          ClassElement enclosingClass = resolver.getEnclosingClass();
          if (enclosingClass != null) {
            setter = lookUpSetter(null, enclosingClass.getType(), node.getName());
          }
        }
        if (setter != null) {
          element = setter;
        }
      }
    } else if (element == null
        && (node.inSetterContext() || node.getParent() instanceof CommentReference)) {
      element = resolver.getNameScope().lookup(
          new SyntheticIdentifier(node.getName() + "="),
          definingLibrary);
    }
    ClassElement enclosingClass = resolver.getEnclosingClass();
    if (element == null && enclosingClass != null) {
      InterfaceType enclosingType = enclosingClass.getType();
      if (element == null
          && (node.inSetterContext() || node.getParent() instanceof CommentReference)) {
        element = lookUpSetter(null, enclosingType, node.getName());
      }
      if (element == null && node.inGetterContext()) {
        element = lookUpGetter(null, enclosingType, node.getName());
      }
      if (element == null) {
        element = lookUpMethod(null, enclosingType, node.getName());
      }
    }
    return element;
  }

  /**
   * If the given type is a type parameter, resolve it to the type that should be used when looking
   * up members. Otherwise, return the original type.
   * 
   * @param type the type that is to be resolved if it is a type parameter
   * @return the type that should be used in place of the argument if it is a type parameter, or the
   *         original argument if it isn't a type parameter
   */
  private Type resolveTypeParameter(Type type) {
    if (type instanceof TypeParameterType) {
      Type bound = ((TypeParameterType) type).getElement().getBound();
      if (bound == null) {
        return resolver.getTypeProvider().getObjectType();
      }
      return bound;
    }
    return type;
  }

  /**
   * Given a node that can have annotations associated with it and the element to which that node
   * has been resolved, create the annotations in the element model representing the annotations on
   * the node.
   * 
   * @param element the element to which the node has been resolved
   * @param node the node that can have annotations associated with it
   */
  private void setMetadata(Element element, AnnotatedNode node) {
    if (!(element instanceof ElementImpl)) {
      return;
    }
    ArrayList<ElementAnnotationImpl> annotationList = new ArrayList<ElementAnnotationImpl>();
    addAnnotations(annotationList, node.getMetadata());
    if (node instanceof VariableDeclaration && node.getParent() instanceof VariableDeclarationList) {
      VariableDeclarationList list = (VariableDeclarationList) node.getParent();
      addAnnotations(annotationList, list.getMetadata());
      if (list.getParent() instanceof FieldDeclaration) {
        FieldDeclaration fieldDeclaration = (FieldDeclaration) list.getParent();
        addAnnotations(annotationList, fieldDeclaration.getMetadata());
      } else if (list.getParent() instanceof TopLevelVariableDeclaration) {
        TopLevelVariableDeclaration variableDeclaration = (TopLevelVariableDeclaration) list.getParent();
        addAnnotations(annotationList, variableDeclaration.getMetadata());
      }
    }
    if (!annotationList.isEmpty()) {
      ((ElementImpl) element).setMetadata(annotationList.toArray(new ElementAnnotationImpl[annotationList.size()]));
    }
  }

  /**
   * Given a node that can have annotations associated with it and the element to which that node
   * has been resolved, create the annotations in the element model representing the annotations on
   * the node.
   * 
   * @param element the element to which the node has been resolved
   * @param node the node that can have annotations associated with it
   */
  private void setMetadataForParameter(Element element, NormalFormalParameter node) {
    if (!(element instanceof ElementImpl)) {
      return;
    }
    ArrayList<ElementAnnotationImpl> annotationList = new ArrayList<ElementAnnotationImpl>();
    addAnnotations(annotationList, node.getMetadata());
    if (!annotationList.isEmpty()) {
      ((ElementImpl) element).setMetadata(annotationList.toArray(new ElementAnnotationImpl[annotationList.size()]));
    }
  }

  /**
   * Return {@code true} if we should report an error as a result of looking up a member in the
   * given type and not finding any member.
   * 
   * @param type the type in which we attempted to perform the look-up
   * @param member the result of the look-up
   * @return {@code true} if we should report an error
   */
  private boolean shouldReportMissingMember(Type type, Element member) {
    if (member != null || type == null || type.isDynamic() || type.isBottom()) {
      return false;
    }
    return true;
  }
}
