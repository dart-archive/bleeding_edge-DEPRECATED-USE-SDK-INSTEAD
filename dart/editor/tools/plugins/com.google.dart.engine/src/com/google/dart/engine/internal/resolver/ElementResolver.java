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
import com.google.dart.engine.internal.element.TypeVariableElementImpl;
import com.google.dart.engine.internal.scope.LabelScope;
import com.google.dart.engine.internal.scope.Namespace;
import com.google.dart.engine.internal.scope.NamespaceBuilder;
import com.google.dart.engine.internal.type.DynamicTypeImpl;
import com.google.dart.engine.resolver.ResolverErrorCode;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.type.TypeVariableType;
import com.google.dart.engine.utilities.dart.ParameterKind;

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
        Type leftType = getType(leftNode);
        if (leftType != null) {
          MethodElement method = lookUpMethod(leftType, operator.getLexeme());
          if (method != null) {
            node.setElement(method);
          } else {
            // TODO(brianwilkerson) Report this error. StaticTypeWarningCode.UNDEFINED_MEMBER
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
      if (leftType == null || leftType.isDynamic()) {
        return null;
      } else if (leftType instanceof FunctionType) {
        leftType = resolver.getTypeProvider().getFunctionType();
      }
      String methodName = operator.getLexeme();
      MethodElement member = lookUpMethod(leftType, methodName);
      if (member == null) {
        resolver.reportError(
            StaticWarningCode.UNDEFINED_OPERATOR,
            operator,
            methodName,
            leftType.getName());
      } else {
        node.setElement(member);
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
  public Void visitConstructorFieldInitializer(ConstructorFieldInitializer node) {
    FieldElement fieldElement = null;
    SimpleIdentifier fieldName = node.getFieldName();
    ClassElement enclosingClass = resolver.getEnclosingClass();
    fieldElement = ((ClassElementImpl) enclosingClass).getField(fieldName.getName());
    if (fieldElement != null && !fieldElement.isSynthetic()) {
      recordResolution(fieldName, fieldElement);
    }
    return null;
  }

  @Override
  public Void visitConstructorName(ConstructorName node) {
    Type type = node.getType().getType();
    if (type instanceof DynamicTypeImpl) {
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
      name.setElement(constructor);
    }
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
      // TODO(brianwilkerson) Figure out when the element can ever be something other than an ExportElement
      resolveCombinators(((ExportElement) element).getExportedLibrary(), node.getCombinators());
    }
    return null;
  }

  @Override
  public Void visitFieldFormalParameter(FieldFormalParameter node) {
    String fieldName = node.getIdentifier().getName();
    ClassElement classElement = resolver.getEnclosingClass();
    if (classElement != null) {
      // Call getField directly on the ClassElementImpl since we only care about variables in the
      // immediately enclosing class.
      FieldElement fieldElement = ((ClassElementImpl) classElement).getField(fieldName);

      if (fieldElement != null) {
        if (!fieldElement.isSynthetic()) {
          ParameterElement parameterElement = node.getElement();
          if (parameterElement instanceof FieldFormalParameterElementImpl) {
            ((FieldFormalParameterElementImpl) parameterElement).setField(fieldElement);
            if (fieldElement.isStatic()) {
              resolver.reportError(
                  CompileTimeErrorCode.INITIALIZING_FORMAL_FOR_STATIC_FIELD,
                  node,
                  fieldName);
            }
          }
        }
      } else {
        resolver.reportError(
            CompileTimeErrorCode.INITIALIZING_FORMAL_FOR_NON_EXISTANT_FIELD,
            node,
            fieldName);
      }
    }
//    else {
    // TODO(jwren) Report error, constructor initializer variable is a top level element
    // (Either here or in ErrorVerifier#checkForAllFinalInitializedErrorCodes)
//    }
    return super.visitFieldFormalParameter(node);
  }

  @Override
  public Void visitFunctionExpressionInvocation(FunctionExpressionInvocation node) {
    // TODO(brianwilkerson) Resolve the function being invoked?
    //resolveNamedArguments(node.getArgumentList(), invokedFunction);
    return null;
  }

  @Override
  public Void visitImportDirective(ImportDirective node) {
    SimpleIdentifier prefixNode = node.getPrefix();
    if (prefixNode != null) {
      String prefixName = prefixNode.getName();
      for (PrefixElement prefixElement : resolver.getDefiningLibrary().getPrefixes()) {
        if (prefixElement.getName().equals(prefixName)) {
          recordResolution(prefixNode, prefixElement);
          break;
        }
      }
    }
    Element element = node.getElement();
    if (element instanceof ImportElement) {
      // The element is null when the URI is invalid
      // TODO(brianwilkerson) Figure out when the element can ever be something other than an ImportElement
      resolveCombinators(((ImportElement) element).getImportedLibrary(), node.getCombinators());
    }
    return null;
  }

  @Override
  public Void visitIndexExpression(IndexExpression node) {
    Type arrayType = getType(node.getRealTarget());
    if (arrayType == null || arrayType.isDynamic()) {
      return null;
    }
    String operator;
    if (node.inSetterContext()) {
      operator = TokenType.INDEX_EQ.getLexeme();
    } else {
      operator = TokenType.INDEX.getLexeme();
    }
    MethodElement member = lookUpMethod(arrayType, operator);
    if (member == null) {
      resolver.reportError(
          StaticWarningCode.UNDEFINED_OPERATOR,
          node,
          operator,
          arrayType.getName());
    } else {
      node.setElement(member);
    }
    return null;
  }

  @Override
  public Void visitInstanceCreationExpression(InstanceCreationExpression node) {
    ConstructorElement invokedConstructor = node.getConstructorName().getElement();
    node.setElement(invokedConstructor);
    resolveNamedArguments(node.getArgumentList(), invokedConstructor);
    return null;
  }

  @Override
  public Void visitMethodInvocation(MethodInvocation node) {
    SimpleIdentifier methodName = node.getMethodName();
    Expression target = node.getRealTarget();
    Element element;
    if (target == null) {
      element = resolver.getNameScope().lookup(methodName, resolver.getDefiningLibrary());
      if (element == null) {
        ClassElement enclosingClass = resolver.getEnclosingClass();
        if (enclosingClass != null) {
          InterfaceType enclosingType = enclosingClass.getType();
          element = lookUpMethod(enclosingType, methodName.getName());
          if (element == null) {
            PropertyAccessorElement getter = lookUpGetter(enclosingType, methodName.getName());
            if (getter != null) {
              FunctionType getterType = getter.getType();
              if (getterType != null) {
                Type returnType = getterType.getReturnType();
                // TODO(brianwilkerson) Should we also allow type parameters at this point (because
                // they might be resolved to an executable type)?
                if (!isExecutableType(returnType)) {
                  resolver.reportError(
                      StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION,
                      methodName,
                      methodName.getName());
                }
              }
              recordResolution(methodName, getter);
              return null;
            }
          }
        }
      }
    } else {
      Type targetType = getType(target);
      if (targetType instanceof InterfaceType) {
        InterfaceType classType = (InterfaceType) targetType;
        element = lookUpMethod(classType, methodName.getName());
        if (element == null) {
          PropertyAccessorElement accessor = classType.getGetter(methodName.getName());
          if (accessor != null) {
            Type returnType = accessor.getType().getReturnType();
            if (!isExecutableType(returnType)) {
              resolver.reportError(
                  StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION,
                  methodName,
                  methodName.getName());
              return null;
            }
            element = accessor;
          }
        }
        if (element == null && target instanceof SuperExpression) {
          // TODO(jwren) We should split the UNDEFINED_METHOD into two error codes, this one, and
          // a code that describes the situation where the method was found, but it was not
          // accessible from the current library.
          resolver.reportError(
              StaticTypeWarningCode.UNDEFINED_SUPER_METHOD,
              methodName,
              methodName.getName(),
              targetType.getElement().getName());
          return null;
        }
      } else if (target instanceof SimpleIdentifier) {
        Element targetElement = ((SimpleIdentifier) target).getElement();
        if (targetElement instanceof PrefixElement) {
          // TODO(brianwilkerson) This isn't a method invocation, it's a function invocation where
          // the function name is a prefixed identifier. Consider re-writing the AST.
          final String name = ((SimpleIdentifier) target).getName() + "." + methodName;
          Identifier functionName = new Identifier() {
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
          };
          element = resolver.getNameScope().lookup(functionName, resolver.getDefiningLibrary());
        } else {
          //TODO(brianwilkerson) Report this error.
          return null;
        }
      } else {
        //TODO(brianwilkerson) Report this error.
        return null;
      }
    }
    ExecutableElement invokedMethod = null;
    if (element instanceof PropertyAccessorElement) {
      //
      // This is really a function expression invocation.
      //
      // TODO(brianwilkerson) Consider the possibility of re-writing the AST.
      PropertyAccessorElement getter = (PropertyAccessorElement) element;
      FunctionType getterType = getter.getType();
      if (getterType != null) {
        Type returnType = getterType.getReturnType();
        if (!isExecutableType(returnType)) {
          resolver.reportError(
              StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION,
              methodName,
              methodName.getName());
        }
      }
      recordResolution(methodName, element);
      return null;
    } else if (element instanceof ExecutableElement) {
      invokedMethod = (ExecutableElement) element;
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
            resolver.reportError(
                StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION,
                methodName,
                methodName.getName());
          }
        }
        recordResolution(methodName, element);
        return null;
      } else if (element instanceof VariableElement) {
        Type variableType = resolver.getOverrideManager().getType(element);
        if (variableType == null) {
          variableType = ((VariableElement) element).getType();
        }
        if (!isExecutableType(variableType)) {
          resolver.reportError(
              StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION,
              methodName,
              methodName.getName());
        }
        recordResolution(methodName, element);
        return null;
      } else {
        if (target == null) {
          ClassElement enclosingClass = resolver.getEnclosingClass();
          if (enclosingClass == null) {
            resolver.reportError(
                StaticTypeWarningCode.UNDEFINED_FUNCTION,
                methodName,
                methodName.getName());
          } else if (element == null) {
            resolver.reportError(
                StaticTypeWarningCode.UNDEFINED_METHOD,
                methodName,
                methodName.getName(),
                enclosingClass.getName());
          } else {
            resolver.reportError(
                StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION,
                methodName,
                methodName.getName());
          }
        } else {
          Type targetType = getType(target);
          String targetTypeName = targetType == null ? null : targetType.getName();
          if (targetTypeName == null) {
            resolver.reportError(
                StaticTypeWarningCode.UNDEFINED_FUNCTION,
                methodName,
                methodName.getName());
          } else {
            resolver.reportError(
                StaticTypeWarningCode.UNDEFINED_METHOD,
                methodName,
                methodName.getName(),
                targetTypeName);
          }
        }
        return null;
      }
    }
    recordResolution(methodName, invokedMethod);
    resolveNamedArguments(node.getArgumentList(), invokedMethod);
    return null;
  }

  @Override
  public Void visitPostfixExpression(PostfixExpression node) {
    Token operator = node.getOperator();
    Type operandType = getType(node.getOperand());
    if (operandType == null || operandType.isDynamic()) {
      return null;
    }
    String methodName;
    if (operator.getType() == TokenType.PLUS_PLUS) {
      methodName = TokenType.PLUS.getLexeme();
    } else {
      methodName = TokenType.MINUS.getLexeme();
    }
    MethodElement member = lookUpMethod(operandType, methodName);
    if (member == null) {
      resolver.reportError(
          StaticWarningCode.UNDEFINED_OPERATOR,
          operator,
          methodName,
          operandType.getName());
    } else {
      node.setElement(member);
    }
    return null;
  }

  @Override
  public Void visitPrefixedIdentifier(PrefixedIdentifier node) {
    SimpleIdentifier prefix = node.getPrefix();
    SimpleIdentifier identifier = node.getIdentifier();
    //
    // First, check to see whether the "prefix" is really a prefix or whether it's an expression
    // that happens to be a simple identifier.
    //
    Element prefixElement = prefix.getElement();
    if (prefixElement instanceof PrefixElement) {
      // TODO(brianwilkerson) The prefix needs to be resolved to the element for the import that
      // defines the prefix, not the prefix's element.

      Element element = resolver.getNameScope().lookup(node, resolver.getDefiningLibrary());
      if (element == null) {
        // TODO(brianwilkerson) Report this error.
        return null;
      }
      recordResolution(identifier, element);
      return null;
    }
    //
    // Otherwise, this is really equivalent to a property access node.
    //
    // Look to see whether we're accessing a static member of a class.
    //
    if (prefixElement instanceof ClassElement) {
      Element memberElement;
      if (node.getIdentifier().inSetterContext()) {
        memberElement = ((ClassElementImpl) prefixElement).getSetter(identifier.getName());
      } else {
        memberElement = ((ClassElementImpl) prefixElement).getGetter(identifier.getName());
      }
      if (memberElement == null) {
        MethodElement methodElement = lookUpMethod(
            ((ClassElement) prefixElement).getType(),
            identifier.getName());
        if (methodElement != null) {
          // TODO(brianwilkerson) This should really be a synthetic getter whose type is a function
          // type with no parameters and a return type that is equal to the function type of the method.
          recordResolution(identifier, methodElement);
          return null;
        }
      }
      if (memberElement == null) {
        reportGetterOrSetterNotFound(node, identifier, prefixElement.getName());
      } else {
//      if (!element.isStatic()) {
//        reportError(ResolverErrorCode.STATIC_ACCESS_TO_INSTANCE_MEMBER, identifier, identifier.getName());
//      }
        recordResolution(identifier, memberElement);
      }
      return null;
    }
    //
    // Otherwise, determine the type of the left-hand side.
    //
    Type variableType;
    if (prefixElement instanceof PropertyAccessorElement) {
      PropertyAccessorElement accessor = (PropertyAccessorElement) prefixElement;
      FunctionType type = accessor.getType();
      if (type == null) {
        // TODO(brianwilkerson) Figure out why the type is sometimes null and either prevent it or
        // report it (here or at the point of origin)
        return null;
      }
      if (accessor.isGetter()) {
        variableType = type.getReturnType();
      } else {
        variableType = type.getNormalParameterTypes()[0];
      }
      if (variableType == null || variableType.isDynamic()) {
        return null;
      }
    } else if (prefixElement instanceof VariableElement) {
      variableType = resolver.getOverrideManager().getType(prefixElement);
      if (variableType == null) {
        variableType = ((VariableElement) prefixElement).getType();
      }
      if (variableType == null || variableType.isDynamic()) {
        // TODO(brianwilkerson) Figure out why the type is sometimes null and either prevent it or
        // report it (here or at the point of origin)
        return null;
      }
    } else {
      // reportError(ResolverErrorCode.UNDEFINED_PREFIX);
      return null;
    }
    //
    // Then find the property being accessed.
    //
    PropertyAccessorElement memberElement = null;
    if (node.getIdentifier().inSetterContext()) {
      memberElement = lookUpSetter(variableType, identifier.getName());
    }
    if (memberElement == null && node.getIdentifier().inGetterContext()) {
      memberElement = lookUpGetter(variableType, identifier.getName());
    }
    if (memberElement == null) {
      MethodElement methodElement = lookUpMethod(variableType, identifier.getName());
      if (methodElement != null) {
        // TODO(brianwilkerson) This should really be a synthetic getter whose type is a function
        // type with no parameters and a return type that is equal to the function type of the method.
        recordResolution(identifier, methodElement);
        return null;
      }
    }
    if (memberElement == null) {
      reportGetterOrSetterNotFound(node, identifier, variableType.getElement().getName());
    } else {
      recordResolution(identifier, memberElement);
    }
    return null;
  }

  @Override
  public Void visitPrefixExpression(PrefixExpression node) {
    Token operator = node.getOperator();
    TokenType operatorType = operator.getType();
    if (operatorType.isUserDefinableOperator() || operatorType == TokenType.PLUS_PLUS
        || operatorType == TokenType.MINUS_MINUS) {
      Type operandType = getType(node.getOperand());
      if (operandType == null || operandType.isDynamic()) {
        return null;
      }
      String methodName;
      if (operatorType == TokenType.PLUS_PLUS) {
        methodName = TokenType.PLUS.getLexeme();
      } else if (operatorType == TokenType.MINUS_MINUS) {
        methodName = TokenType.MINUS.getLexeme();
      } else if (operatorType == TokenType.MINUS) {
        methodName = "unary-";
      } else {
        methodName = operator.getLexeme();
      }
      MethodElement member = lookUpMethod(operandType, methodName);
      if (member == null) {
        resolver.reportError(
            StaticWarningCode.UNDEFINED_OPERATOR,
            operator,
            methodName,
            operandType.getName());
      } else {
        node.setElement(member);
      }
    }
    return null;
  }

  @Override
  public Void visitPropertyAccess(PropertyAccess node) {
    Type targetType = getType(node.getRealTarget());
    if (!(targetType instanceof InterfaceType)) {
      // TODO(brianwilkerson) Report this error
      return null;
    }
    SimpleIdentifier identifier = node.getPropertyName();
    PropertyAccessorElement memberElement = null;
    if (identifier.inSetterContext()) {
      memberElement = lookUpSetter(targetType, identifier.getName());
    }
    if (memberElement == null && identifier.inGetterContext()) {
      memberElement = lookUpGetter(targetType, identifier.getName());
    }
    if (memberElement == null) {
      MethodElement methodElement = lookUpMethod(targetType, identifier.getName());
      if (methodElement != null) {
        // TODO(brianwilkerson) This should really be a synthetic getter whose type is a function
        // type with no parameters and a return type that is equal to the function type of the method.
        recordResolution(identifier, methodElement);
        return null;
      }
    }
    if (memberElement == null) {
      if (identifier.inSetterContext()) {
        resolver.reportError(
            StaticWarningCode.UNDEFINED_SETTER,
            identifier,
            identifier.getName(),
            targetType.getName());
      } else if (identifier.inGetterContext()) {
        resolver.reportError(
            StaticWarningCode.UNDEFINED_GETTER,
            identifier,
            identifier.getName(),
            targetType.getName());
      } else {
        resolver.reportError(
            StaticWarningCode.UNDEFINED_IDENTIFIER,
            identifier,
            identifier.getName());
      }
//    } else if (!memberElement.isStatic()) {
//      reportError(
//          ResolverErrorCode.STATIC_ACCESS_TO_INSTANCE_MEMBER,
//          identifier,
//          identifier.getName());
    } else {
      recordResolution(identifier, memberElement);
    }
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
    if (element == null) {
      // TODO(brianwilkerson) Recover from this error.
      resolver.reportError(StaticWarningCode.UNDEFINED_IDENTIFIER, node, node.getName());
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
    node.setElement(element);
    resolveNamedArguments(node.getArgumentList(), element);
    return null;
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
        String parameteName = parameter.getName();
        if (parameteName != null && parameteName.equals(name)) {
          return parameter;
        }
      }
    }
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
    if (expression instanceof NullLiteral) {
      return resolver.getTypeProvider().getObjectType();
    }
    return expression.getStaticType();
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
      MethodElement methodElement = classElement.lookUpMethod("call", resolver.getDefiningLibrary());
      return methodElement != null;
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
    // TODO(brianwilkerson) Decide whether/how to represent members defined in 'dynamic'.
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
    // TODO(brianwilkerson) Decide whether/how to represent members defined in 'dynamic'.
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
    // TODO(brianwilkerson) Decide whether/how to represent members defined in 'dynamic'.
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
    PropertyAccessorElement setter = targetType.getGetter(setterName);
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
      node.setElement(element);
    }
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
    // TODO(jwren) This needs to be modified to also generate the error code StaticTypeWarningCode.INACCESSIBLE_SETTER
    boolean isSetterContext = node.getIdentifier().inSetterContext();
    ErrorCode errorCode = isSetterContext ? StaticTypeWarningCode.UNDEFINED_SETTER
        : StaticTypeWarningCode.UNDEFINED_GETTER;
    resolver.reportError(errorCode, identifier, identifier.getName(), typeName);
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
}
