/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.engine.internal.builder;

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.CatchClause;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassTypeAlias;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.DefaultFormalParameter;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.FieldFormalParameter;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.FunctionTypedFormalParameter;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.Label;
import com.google.dart.engine.ast.LabeledStatement;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SwitchCase;
import com.google.dart.engine.ast.SwitchDefault;
import com.google.dart.engine.ast.SwitchStatement;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.TypeParameter;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.visitor.RecursiveASTVisitor;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.TypeVariableElement;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.ConstructorElementImpl;
import com.google.dart.engine.internal.element.FieldElementImpl;
import com.google.dart.engine.internal.element.FunctionElementImpl;
import com.google.dart.engine.internal.element.LabelElementImpl;
import com.google.dart.engine.internal.element.MethodElementImpl;
import com.google.dart.engine.internal.element.ParameterElementImpl;
import com.google.dart.engine.internal.element.PropertyAccessorElementImpl;
import com.google.dart.engine.internal.element.TypeAliasElementImpl;
import com.google.dart.engine.internal.element.TypeVariableElementImpl;
import com.google.dart.engine.internal.element.VariableElementImpl;
import com.google.dart.engine.internal.type.FunctionTypeImpl;
import com.google.dart.engine.internal.type.InterfaceTypeImpl;
import com.google.dart.engine.internal.type.TypeVariableTypeImpl;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.type.Type;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Instances of the class {@code ElementBuilder} traverse an AST structure and build the element
 * model representing the AST structure.
 */
public class ElementBuilder extends RecursiveASTVisitor<Void> {
  /**
   * The element holder associated with the element that is currently being built.
   */
  private ElementHolder currentHolder;

  /**
   * A flag indicating whether a variable declaration is in the context of a field declaration.
   */
  private boolean inFieldContext = false;

  /**
   * Initialize a newly created element builder to build the elements for a compilation unit.
   * 
   * @param initialHolder the element holder associated with the compilation unit being built
   */
  public ElementBuilder(ElementHolder initialHolder) {
    currentHolder = initialHolder;
  }

  @Override
  public Void visitCatchClause(CatchClause node) {
    SimpleIdentifier exceptionParameter = node.getExceptionParameter();
    if (exceptionParameter != null) {
      VariableElementImpl exception = new VariableElementImpl(exceptionParameter);

      currentHolder.addVariable(exception);
      exceptionParameter.setElement(exception);

      SimpleIdentifier stackTraceParameter = node.getStackTraceParameter();
      if (stackTraceParameter != null) {
        VariableElementImpl stackTrace = new VariableElementImpl(stackTraceParameter);

        currentHolder.addVariable(stackTrace);
        stackTraceParameter.setElement(stackTrace);
      }
    }
    node.visitChildren(this);
    return null;
  }

  @Override
  public Void visitClassDeclaration(ClassDeclaration node) {
    ElementHolder holder = new ElementHolder();
    visitChildren(holder, node);

    SimpleIdentifier className = node.getName();
    ClassElementImpl element = new ClassElementImpl(className);
    MethodElement[] methods = holder.getMethods();
    element.setAbstract(node.getAbstractKeyword() != null);
    element.setAccessors(holder.getAccessors());
    element.setConstructors(holder.getConstructors());
    element.setFields(holder.getFields());
    element.setMethods(methods);
    TypeVariableElement[] typeVariables = holder.getTypeVariables();
    element.setTypeVariables(typeVariables);

    InterfaceTypeImpl interfaceType = new InterfaceTypeImpl(element);
    int typeVariableCount = typeVariables.length;
    Type[] typeArguments = new Type[typeVariableCount];
    for (int i = 0; i < typeVariableCount; i++) {
      TypeVariableElementImpl typeVariable = (TypeVariableElementImpl) typeVariables[i];
      TypeVariableTypeImpl typeArgument = new TypeVariableTypeImpl(typeVariable);
      typeVariable.setType(typeArgument);
      typeArguments[i] = typeArgument;
    }
    interfaceType.setTypeArguments(typeArguments);
    element.setType(interfaceType);

    currentHolder.addType(element);
    className.setElement(element);
    return null;
  }

  @Override
  public Void visitClassTypeAlias(ClassTypeAlias node) {
    ElementHolder holder = new ElementHolder();
    visitChildren(holder, node);

    SimpleIdentifier className = node.getName();
    ClassElementImpl element = new ClassElementImpl(className);
    element.setAbstract(node.getAbstractKeyword() != null);
    TypeVariableElement[] typeVariables = holder.getTypeVariables();
    element.setTypeVariables(typeVariables);

    InterfaceTypeImpl interfaceType = new InterfaceTypeImpl(element);
    int typeVariableCount = typeVariables.length;
    Type[] typeArguments = new Type[typeVariableCount];
    for (int i = 0; i < typeVariableCount; i++) {
      TypeVariableElementImpl typeVariable = (TypeVariableElementImpl) typeVariables[i];
      TypeVariableTypeImpl typeArgument = new TypeVariableTypeImpl(typeVariable);
      typeVariable.setType(typeArgument);
      typeArguments[i] = typeArgument;
    }
    interfaceType.setTypeArguments(typeArguments);
    element.setType(interfaceType);

    currentHolder.addType(element);
    className.setElement(element);
    return null;
  }

  @Override
  public Void visitConstructorDeclaration(ConstructorDeclaration node) {
    ElementHolder holder = new ElementHolder();
    visitChildren(holder, node);

    SimpleIdentifier constructorName = node.getName();
    ConstructorElementImpl element = new ConstructorElementImpl(constructorName);
    if (node.getFactoryKeyword() != null) {
      element.setFactory(true);
    }
    element.setFunctions(holder.getFunctions());
    element.setLabels(holder.getLabels());
    element.setLocalVariables(holder.getVariables());
    element.setParameters(holder.getParameters());

    currentHolder.addConstructor(element);
    node.setElement(element);
    if (constructorName != null) {
      constructorName.setElement(element);
    }
    return null;
  }

  @Override
  public Void visitDefaultFormalParameter(DefaultFormalParameter node) {
    ElementHolder holder = new ElementHolder();
    visitChildren(holder, node.getDefaultValue());

    FunctionElementImpl initializer = new FunctionElementImpl();
    initializer.setFunctions(holder.getFunctions());
    initializer.setLabels(holder.getLabels());
    initializer.setLocalVariables(holder.getVariables());
    initializer.setParameters(holder.getParameters());

    SimpleIdentifier parameterName = node.getParameter().getIdentifier();
    ParameterElementImpl parameter = new ParameterElementImpl(parameterName);
    parameter.setConst(node.isConst());
    parameter.setFinal(node.isFinal());
    parameter.setInitializer(initializer);
    parameter.setParameterKind(node.getKind());

    currentHolder.addParameter(parameter);
    parameterName.setElement(parameter);
    return null;
  }

  @Override
  public Void visitFieldDeclaration(FieldDeclaration node) {
    boolean wasInField = inFieldContext;
    inFieldContext = true;
    try {
      node.visitChildren(this);
    } finally {
      inFieldContext = wasInField;
    }
    return null;
  }

  @Override
  public Void visitFieldFormalParameter(FieldFormalParameter node) {
    SimpleIdentifier parameterName = node.getIdentifier();
    ParameterElementImpl parameter = new ParameterElementImpl(parameterName);
    parameter.setConst(node.isConst());
    parameter.setFinal(node.isFinal());
    parameter.setParameterKind(node.getKind());

    currentHolder.addParameter(parameter);
    parameterName.setElement(parameter);
    return null;
  }

  @Override
  public Void visitFunctionDeclaration(FunctionDeclaration node) {
    ElementHolder holder = new ElementHolder();
    visitChildren(holder, node);

    SimpleIdentifier functionName = node.getName();
    FunctionElementImpl element = new FunctionElementImpl(functionName);
    element.setFunctions(holder.getFunctions());
    element.setLabels(holder.getLabels());
    element.setLocalVariables(holder.getVariables());
    element.setParameters(holder.getParameters());

    currentHolder.addFunction(element);
    functionName.setElement(element);
    return null;
  }

  @Override
  public Void visitFunctionExpression(FunctionExpression node) {
    ElementHolder holder = new ElementHolder();
    visitChildren(holder, node);

    SimpleIdentifier functionName = null;
    FunctionElementImpl element = new FunctionElementImpl(functionName);
    element.setFunctions(holder.getFunctions());
    element.setLabels(holder.getLabels());
    element.setLocalVariables(holder.getVariables());
    element.setParameters(holder.getParameters());

    currentHolder.addFunction(element);
    node.setElement(element);
    return null;
  }

  @Override
  public Void visitFunctionTypeAlias(FunctionTypeAlias node) {
    ElementHolder holder = new ElementHolder();
    visitChildren(holder, node);

    SimpleIdentifier aliasName = node.getName();
    ParameterElement[] parameters = holder.getParameters();
    TypeAliasElementImpl element = new TypeAliasElementImpl(aliasName);
    element.setParameters(parameters);
    element.setTypeVariables(holder.getTypeVariables());

    ArrayList<Type> normalParameterTypes = new ArrayList<Type>();
    ArrayList<Type> optionalParameterTypes = new ArrayList<Type>();
    LinkedHashMap<String, Type> namedParameterTypes = new LinkedHashMap<String, Type>();
    for (ParameterElement parameter : parameters) {
      switch (parameter.getParameterKind()) {
        case REQUIRED:
          normalParameterTypes.add(parameter.getType());
          break;
        case POSITIONAL:
          optionalParameterTypes.add(parameter.getType());
          break;
        case NAMED:
          namedParameterTypes.put(parameter.getName(), parameter.getType());
          break;
      }
    }
    TypeName returnType = node.getReturnType();
    FunctionTypeImpl functionType = new FunctionTypeImpl(element);
    functionType.setNormalParameterTypes(normalParameterTypes.toArray(new Type[normalParameterTypes.size()]));
    functionType.setOptionalParameterTypes(optionalParameterTypes.toArray(new Type[optionalParameterTypes.size()]));
    functionType.setNamedParameterTypes(namedParameterTypes);
    if (returnType != null) {
      functionType.setReturnType(returnType.getType());
    }
    element.setType(functionType);

    currentHolder.addTypeAlias(element);
    aliasName.setElement(element);
    return null;
  }

  @Override
  public Void visitFunctionTypedFormalParameter(FunctionTypedFormalParameter node) {
    SimpleIdentifier parameterName = node.getIdentifier();
    ParameterElementImpl parameter = new ParameterElementImpl(parameterName);
    parameter.setParameterKind(node.getKind());

    currentHolder.addParameter(parameter);
    parameterName.setElement(parameter);
    return null;
  }

  @Override
  public Void visitLabeledStatement(LabeledStatement node) {
    boolean onSwitchStatement = node.getStatement() instanceof SwitchStatement;
    for (Label label : node.getLabels()) {
      SimpleIdentifier labelName = label.getLabel();
      LabelElementImpl element = new LabelElementImpl(labelName, onSwitchStatement, false);

      currentHolder.addLabel(element);
      labelName.setElement(element);
    }
    return null;
  }

  @Override
  public Void visitMethodDeclaration(MethodDeclaration node) {
    ElementHolder holder = new ElementHolder();
    visitChildren(holder, node);

    Token property = node.getPropertyKeyword();
    if (property == null) {
      Identifier methodName = node.getName();
      MethodElementImpl element = new MethodElementImpl(methodName);
      Token keyword = node.getModifierKeyword();
      element.setAbstract(matches(keyword, Keyword.ABSTRACT));
      element.setFunctions(holder.getFunctions());
      element.setLabels(holder.getLabels());
      element.setLocalVariables(holder.getVariables());
      element.setParameters(holder.getParameters());
      element.setStatic(matches(keyword, Keyword.STATIC));

      currentHolder.addMethod(element);
      methodName.setElement(element);
    } else {
      Identifier propertyNameNode = node.getName();
      String propertyName = propertyNameNode.getName();
      FieldElementImpl field = (FieldElementImpl) currentHolder.getField(propertyName);
      if (field == null) {
        field = new FieldElementImpl(node.getName().getName());
        field.setFinal(true);
        field.setStatic(matches(node.getModifierKeyword(), Keyword.STATIC));

        currentHolder.addField(field);
      }
      if (matches(property, Keyword.GET)) {
        PropertyAccessorElementImpl getter = new PropertyAccessorElementImpl(propertyNameNode);
        getter.setField(field);
        getter.setGetter(true);
        field.setGetter(getter);

        currentHolder.addAccessor(getter);
        propertyNameNode.setElement(getter);
      } else {
        PropertyAccessorElementImpl setter = new PropertyAccessorElementImpl(propertyNameNode);
        setter.setField(field);
        setter.setSetter(true);
        field.setSetter(setter);
        field.setFinal(false);

        currentHolder.addAccessor(setter);
        propertyNameNode.setElement(setter);
      }
    }
    return null;
  }

  @Override
  public Void visitSimpleFormalParameter(SimpleFormalParameter node) {
    SimpleIdentifier parameterName = node.getIdentifier();
    ParameterElementImpl parameter = new ParameterElementImpl(parameterName);
    parameter.setConst(node.isConst());
    parameter.setFinal(node.isFinal());
    parameter.setParameterKind(node.getKind());

    currentHolder.addParameter(parameter);
    parameterName.setElement(parameter);
    return null;
  }

  @Override
  public Void visitSwitchCase(SwitchCase node) {
    for (Label label : node.getLabels()) {
      SimpleIdentifier labelName = label.getLabel();
      LabelElementImpl element = new LabelElementImpl(labelName, false, true);

      currentHolder.addLabel(element);
      labelName.setElement(element);
    }
    return null;
  }

  @Override
  public Void visitSwitchDefault(SwitchDefault node) {
    for (Label label : node.getLabels()) {
      SimpleIdentifier labelName = label.getLabel();
      LabelElementImpl element = new LabelElementImpl(labelName, false, true);

      currentHolder.addLabel(element);
      labelName.setElement(element);
    }
    return null;
  }

  @Override
  public Void visitTypeParameter(TypeParameter node) {
    SimpleIdentifier parameterName = node.getName();
    TypeVariableElementImpl element = new TypeVariableElementImpl(parameterName);

    currentHolder.addTypeVariable(element);
    parameterName.setElement(element);
    return null;
  }

  @Override
  public Void visitVariableDeclaration(VariableDeclaration node) {
    VariableElementImpl element;
    if (inFieldContext) {
      SimpleIdentifier fieldName = node.getName();
      element = new FieldElementImpl(fieldName);

      currentHolder.addField((FieldElementImpl) element);
      fieldName.setElement(element);
    } else {
      SimpleIdentifier variableName = node.getName();
      element = new VariableElementImpl(variableName);

      currentHolder.addVariable(element);
      variableName.setElement(element);
    }

    Token keyword = ((VariableDeclarationList) node.getParent()).getKeyword();
    boolean isFinal = matches(keyword, Keyword.FINAL);
    element.setConst(matches(keyword, Keyword.CONST));
    element.setFinal(isFinal);
    if (node.getInitializer() != null) {
      ElementHolder holder = new ElementHolder();
      boolean wasInFieldContext = inFieldContext;
      inFieldContext = false;
      try {
        visitChildren(holder, node.getInitializer());
      } finally {
        inFieldContext = wasInFieldContext;
      }
      FunctionElementImpl initializer = new FunctionElementImpl();
      initializer.setFunctions(holder.getFunctions());
      initializer.setLabels(holder.getLabels());
      initializer.setLocalVariables(holder.getVariables());
      initializer.setSynthetic(true);
      element.setInitializer(initializer);
    }
    if (inFieldContext) {
      FieldElementImpl field = (FieldElementImpl) element;
      PropertyAccessorElementImpl getter = new PropertyAccessorElementImpl(field);
      getter.setGetter(true);
      field.setGetter(getter);

      if (!isFinal) {
        PropertyAccessorElementImpl setter = new PropertyAccessorElementImpl(field);
        setter.setSetter(true);
        field.setSetter(setter);
      }

      field.setStatic(matches(
          ((FieldDeclaration) node.getParent().getParent()).getKeyword(),
          Keyword.STATIC));
    }
    return null;
  }

  /**
   * Return {@code true} if the given token is a token for the given keyword.
   * 
   * @param token the token being tested
   * @param keyword the keyword being tested for
   * @return {@code true} if the given token is a token for the given keyword
   */
  private boolean matches(Token token, Keyword keyword) {
    return token != null && token.getType() == TokenType.KEYWORD
        && ((KeywordToken) token).getKeyword() == keyword;
  }

  /**
   * Make the given holder be the current holder while visiting the children of the given node.
   * 
   * @param holder the holder that will gather elements that are built while visiting the children
   * @param node the node whose children are to be visited
   */
  private void visitChildren(ElementHolder holder, ASTNode node) {
    if (node != null) {
      ElementHolder previousHolder = currentHolder;
      currentHolder = holder;
      try {
        node.visitChildren(this);
      } finally {
        currentHolder = previousHolder;
      }
    }
  }
}
