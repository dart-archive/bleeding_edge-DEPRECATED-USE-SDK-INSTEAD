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
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.BreakStatement;
import com.google.dart.engine.ast.ContinueStatement;
import com.google.dart.engine.ast.ExportDirective;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FunctionExpressionInvocation;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.IndexExpression;
import com.google.dart.engine.ast.LibraryDirective;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.PartDirective;
import com.google.dart.engine.ast.PartOfDirective;
import com.google.dart.engine.ast.PostfixExpression;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.visitor.SimpleASTVisitor;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LabelElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.TypeVariableElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.internal.element.LabelElementImpl;
import com.google.dart.engine.resolver.ResolverErrorCode;
import com.google.dart.engine.resolver.scope.LabelScope;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

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
 * exist (a {@link LibraryElement}).</li>
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
 */
public class ElementResolver extends SimpleASTVisitor<Void> {
  /**
   * The resolver driving this participant.
   */
  private ResolverVisitor resolver;

  /**
   * Initialize a newly created visitor to resolve the nodes in a compilation unit.
   * 
   * @param resolver the resolver driving this participant
   */
  public ElementResolver(ResolverVisitor resolver) {
    this.resolver = resolver;
  }

  @Override
  public Void visitAssignmentExpression(AssignmentExpression node) {
    TokenType operator = node.getOperator().getType();
    if (operator != TokenType.EQ) {
      operator = operatorFromCompoundAssignment(operator);
      Expression leftNode = node.getLeftHandSide();
      if (leftNode != null) {
        Type leftType = leftNode.getStaticType();
        if (leftType != null) {
          Element leftElement = leftType.getElement();
          if (leftElement != null) {
            Element method = lookupInHierarchy(leftElement, operator.getLexeme());
            if (method instanceof MethodElement) {
              node.setElement((MethodElement) method);
            } else {
              // TODO(brianwilkerson) Do we need to handle this case?
            }
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
      Type leftType = getType(node.getLeftOperand());
      if (leftType == null) {
        return null;
      }
      Element leftTypeElement = leftType.getElement();
      String methodName = operator.getLexeme();
      Element member = lookupInHierarchy(leftTypeElement, methodName);
      if (member == null) {
        resolver.reportError(ResolverErrorCode.CANNOT_BE_RESOLVED, operator, methodName);
      } else if (member instanceof MethodElement) {
        node.setElement((MethodElement) member);
      } else {
        // TODO(brianwilkerson) Do we need to handle this case?
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
  public Void visitContinueStatement(ContinueStatement node) {
    SimpleIdentifier labelNode = node.getLabel();
    LabelElementImpl labelElement = lookupLabel(node, labelNode);
    if (labelElement != null && labelElement.isOnSwitchStatement()) {
      resolver.reportError(ResolverErrorCode.CONTINUE_LABEL_ON_SWITCH, labelNode);
    }
    return null;
  }

  @Override
  public Void visitFunctionExpressionInvocation(FunctionExpressionInvocation node) {
    // TODO(brianwilkerson) Resolve the function being invoked?
    return null;
  }

  @Override
  public Void visitImportDirective(ImportDirective node) {
    // TODO(brianwilkerson) Determine whether this still needs to be done.
    // TODO(brianwilkerson) Resolve the names in combinators
    SimpleIdentifier prefixNode = node.getPrefix();
    if (prefixNode != null) {
      String prefixName = prefixNode.getName();
      for (PrefixElement prefixElement : resolver.getDefiningLibrary().getPrefixes()) {
        if (prefixElement.getName().equals(prefixName)) {
          recordResolution(prefixNode, prefixElement);
        }
        return null;
      }
    }
    return null;
  }

  @Override
  public Void visitIndexExpression(IndexExpression node) {
    Type arrayType = getType(node.getArray());
    if (arrayType == null) {
      return null;
    }
    Element arrayTypeElement = arrayType.getElement();
    String operator;
    if (node.inSetterContext()) {
      operator = TokenType.INDEX_EQ.getLexeme();
    } else {
      operator = TokenType.INDEX.getLexeme();
    }
    Element member = lookupInHierarchy(arrayTypeElement, operator);
    if (member == null) {
      resolver.reportError(ResolverErrorCode.CANNOT_BE_RESOLVED, node, operator);
    } else if (member instanceof MethodElement) {
      node.setElement((MethodElement) member);
    } else {
      // TODO(brianwilkerson) Do we need to handle this case?
    }
    return null;
  }

  @Override
  public Void visitMethodInvocation(MethodInvocation node) {
    // TODO(brianwilkerson) Resolve the method being invoked
    return null;
  }

  @Override
  public Void visitPostfixExpression(PostfixExpression node) {
    Token operator = node.getOperator();
    if (operator.isUserDefinableOperator()) {
      Type operandType = getType(node.getOperand());
      if (operandType == null) {
        return null;
      }
      Element operandTypeElement = operandType.getElement();
      String methodName;
      if (operator.getType() == TokenType.PLUS_PLUS) {
        methodName = TokenType.PLUS.getLexeme();
      } else {
        methodName = TokenType.MINUS.getLexeme();
      }
      Element member = lookupInHierarchy(operandTypeElement, methodName);
      if (member == null) {
        resolver.reportError(ResolverErrorCode.CANNOT_BE_RESOLVED, operator, methodName);
      } else if (member instanceof MethodElement) {
        node.setElement((MethodElement) member);
      } else {
        // TODO(brianwilkerson) Do we need to handle this case?
      }
    }
    return null;
  }

  @Override
  public Void visitPrefixedIdentifier(PrefixedIdentifier node) {
    SimpleIdentifier prefix = node.getPrefix();
    SimpleIdentifier identifier = node.getIdentifier();

    Element prefixElement = resolver.getNameScope().lookup(prefix, resolver.getDefiningLibrary());
    recordResolution(prefix, prefixElement);
    // TODO(brianwilkerson) This needs to be an ImportElement
    if (prefixElement instanceof PrefixElement) {
      Element element = resolver.getNameScope().lookup(node, resolver.getDefiningLibrary());
      recordResolution(node, element);
    } else if (prefixElement instanceof ClassElement) {
      // TODO(brianwilkerson) Should we replace this node with a PropertyAccess node?
      Element memberElement = lookupInType((ClassElement) prefixElement, identifier.getName());
      if (memberElement == null) {
        resolver.reportError(ResolverErrorCode.CANNOT_BE_RESOLVED, identifier, identifier.getName());
//      } else if (!element.isStatic()) {
//        reportError(ResolverErrorCode.STATIC_ACCESS_TO_INSTANCE_MEMBER, identifier, identifier.getName());
      } else {
        recordResolution(identifier, memberElement);
      }
    } else if (prefixElement instanceof VariableElement) {
      // TODO(brianwilkerson) Should we replace this node with a PropertyAccess node?
      Element variableType = ((VariableElement) prefixElement).getType().getElement();
      Element memberElement = lookupInHierarchy(variableType, identifier.getName());
      if (memberElement == null) {
        resolver.reportError(ResolverErrorCode.CANNOT_BE_RESOLVED, identifier, identifier.getName());
//      } else if (!element.isStatic()) {
//        reportError(ResolverErrorCode.STATIC_ACCESS_TO_INSTANCE_MEMBER, identifier, identifier.getName());
      } else {
        recordResolution(identifier, memberElement);
      }
    } else {
      // reportError(ResolverErrorCode.UNDEFINED_PREFIX);
    }
    return null;
  }

  @Override
  public Void visitPrefixExpression(PrefixExpression node) {
    Token operator = node.getOperator();
    if (operator.isUserDefinableOperator()) {
      Type operandType = getType(node.getOperand());
      if (operandType == null) {
        return null;
      }
      Element operandTypeElement = operandType.getElement();
      String methodName;
      if (operator.getType() == TokenType.PLUS_PLUS) {
        methodName = TokenType.PLUS.getLexeme();
      } else if (operator.getType() == TokenType.MINUS_MINUS) {
        methodName = TokenType.MINUS.getLexeme();
      } else if (operator.getType() == TokenType.MINUS) {
        methodName = "unary-";
      } else {
        methodName = operator.getLexeme();
      }
      Element member = lookupInHierarchy(operandTypeElement, methodName);
      if (member == null) {
        resolver.reportError(ResolverErrorCode.CANNOT_BE_RESOLVED, operator, methodName);
      } else if (member instanceof MethodElement) {
        node.setElement((MethodElement) member);
      } else {
        // TODO(brianwilkerson) Do we need to handle this case?
      }
    }
    return null;
  }

  @Override
  public Void visitPropertyAccess(PropertyAccess node) {
    // TODO(brianwilkerson) Implement this.
    return null;
  }

  @Override
  public Void visitSimpleIdentifier(SimpleIdentifier node) {
    //
    // There are two cases in which we defer the resolution of a simple identifier to the method in
    // which we are resolving it's parent. We do this to prevent creating false positives.
    //
    ASTNode parent = node.getParent();
    if (parent instanceof PrefixedIdentifier
        && ((PrefixedIdentifier) parent).getIdentifier() == node) {
      return null;
    } else if (parent instanceof PropertyAccess
        && ((PropertyAccess) parent).getPropertyName() == node) {
      return null;
    }
    //
    // If it's not one of those special cases, then the node should be resolved.
    //
    Element element = resolver.getNameScope().lookup(node, resolver.getDefiningLibrary());
    recordResolution(node, element);
    return null;
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
   * Return the type of the given expression that is to be used for type analysis.
   * 
   * @param expression the expression whose type is to be returned
   * @return the type of the given expression
   */
  private Type getType(Expression expression) {
    return expression.getStaticType();
  }

  /**
   * Look up the name of a member in the given type. Return the element representing the member that
   * was found, or {@code null} if there is no member with the given name.
   * 
   * @param element the element representing the type in which the member is defined
   * @param memberName the name of the member being looked up
   * @return the element representing the member that was found
   */
  private Element lookupInHierarchy(Element element, String memberName) {
    // TODO(brianwilkerson) Decide how to represent members defined in 'dynamic'.
//    if (element == DynamicTypeImpl.getInstance()) {
//      return ?;
//    } else
    if (element instanceof ClassElement) {
      ClassElement targetClass = (ClassElement) element;
      Element member = lookupInType(targetClass, memberName);
      while (member == null) {
        targetClass = getSuperclass(targetClass);
        if (targetClass == null) {
          return null;
        }
        member = lookupInType(targetClass, memberName);
      }
      // TODO(brianwilkerson) Look in mixins and possibly interfaces
      return member;
    }
    return null;
  }

  /**
   * Look up the name of a member in the given type. Return the element representing the member that
   * was found, or {@code null} if there is no member with the given name.
   * 
   * @param element the element representing the type in which the member is defined
   * @param memberName the name of the member being looked up
   * @return the element representing the member that was found
   */
  private Element lookupInType(ClassElement element, String memberName) {
    ClassElement classElement = element;
    for (FieldElement field : classElement.getFields()) {
      if (field.getName().equals(memberName)) {
        return field;
      }
    }
    for (MethodElement method : classElement.getMethods()) {
      if (method.getName().equals(memberName)) {
        return method;
      }
    }
    for (TypeVariableElement typeVariable : classElement.getTypeVariables()) {
      if (typeVariable.getName().equals(memberName)) {
        return typeVariable;
      }
    }
    return null;
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
      }
    } else {
      if (labelScope == null) {
        resolver.reportError(ResolverErrorCode.UNDEFINED_LABEL, labelNode, labelNode.getName());
      } else {
        labelElement = (LabelElementImpl) labelScope.lookup(labelNode);
        if (labelElement == null) {
          resolver.reportError(ResolverErrorCode.UNDEFINED_LABEL, labelNode, labelNode.getName());
        } else {
          recordResolution(labelNode, labelElement);
        }
      }
    }
    if (labelElement != null) {
      ExecutableElement labelContainer = labelElement.getAncestor(ExecutableElement.class);
      if (labelContainer != resolver.getEnclosingFunction()) {
        if (labelNode == null) {
          // TODO(brianwilkerson) Create a new error for cases where there is no label.
          resolver.reportError(ResolverErrorCode.LABEL_IN_OUTER_SCOPE, parentNode, "");
        } else {
          resolver.reportError(
              ResolverErrorCode.LABEL_IN_OUTER_SCOPE,
              labelNode,
              labelNode.getName());
        }
        labelElement = null;
      }
    }
    return labelElement;
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
  private void recordResolution(Identifier node, Element element) {
    if (element != null) {
      node.setElement(element);
    }
  }
}
