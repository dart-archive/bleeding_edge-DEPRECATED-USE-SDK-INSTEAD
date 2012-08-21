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
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.FieldFormalParameter;
import com.google.dart.engine.ast.FormalParameterList;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.FunctionTypedFormalParameter;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.Label;
import com.google.dart.engine.ast.LabeledStatement;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.NamedFormalParameter;
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SwitchCase;
import com.google.dart.engine.ast.SwitchDefault;
import com.google.dart.engine.ast.SwitchStatement;
import com.google.dart.engine.ast.TypeAlias;
import com.google.dart.engine.ast.TypeParameter;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.visitor.RecursiveASTVisitor;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.internal.element.ConstructorElementImpl;
import com.google.dart.engine.internal.element.FieldElementImpl;
import com.google.dart.engine.internal.element.FunctionElementImpl;
import com.google.dart.engine.internal.element.LabelElementImpl;
import com.google.dart.engine.internal.element.MethodElementImpl;
import com.google.dart.engine.internal.element.PropertyAccessorElementImpl;
import com.google.dart.engine.internal.element.TypeAliasElementImpl;
import com.google.dart.engine.internal.element.TypeElementImpl;
import com.google.dart.engine.internal.element.TypeVariableElementImpl;
import com.google.dart.engine.internal.element.VariableElementImpl;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;

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

      SimpleIdentifier stackTraceParameter = node.getStackTraceParameter();
      if (stackTraceParameter != null) {
        VariableElementImpl stackTrace = new VariableElementImpl(stackTraceParameter);
        currentHolder.addVariable(stackTrace);
      }
    }
    node.visitChildren(this);
    return null;
  }

  @Override
  public Void visitClassDeclaration(ClassDeclaration node) {
    ElementHolder holder = new ElementHolder();
    visitChildren(holder, node);

    TypeElementImpl element = new TypeElementImpl(node.getName());
    MethodElement[] methods = holder.getMethods();
    element.setAbstract(node.getAbstractKeyword() != null || hasAbstractMethod(methods));
    element.setAccessors(holder.getAccessors());
    element.setConstructors(holder.getConstructors());
    element.setFields(holder.getFields());
    element.setMethods(methods);
    element.setTypeVariables(holder.getTypeVariables());
    currentHolder.addType(element);
    return null;
  }

  @Override
  public Void visitConstructorDeclaration(ConstructorDeclaration node) {
    ElementHolder holder = new ElementHolder();
    visitChildren(holder, node);

    ConstructorElementImpl element = new ConstructorElementImpl(node.getName());
    Token keyword = node.getKeyword();
    if (keyword instanceof KeywordToken && ((KeywordToken) keyword).getKeyword() == Keyword.FACTORY) {
      element.setFactory(true);
    }
    element.setFunctions(holder.getFunctions());
    element.setLabels(holder.getLabels());
    element.setLocalVariables(holder.getVariables());
    if (holder.getParameters() != null) {
      element.setParameters(holder.getParameters());
    }
    currentHolder.addConstructor(element);
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
    VariableElementImpl parameter = new VariableElementImpl(node.getIdentifier());
    currentHolder.addVariable(parameter);
    return null;
  }

  @Override
  public Void visitFormalParameterList(FormalParameterList node) {
    ElementHolder holder = new ElementHolder();
    visitChildren(holder, node);
    currentHolder.setParameters(holder.getVariables());
    return null;
  }

  @Override
  public Void visitFunctionExpression(FunctionExpression node) {
    ElementHolder holder = new ElementHolder();
    visitChildren(holder, node);

    FunctionElementImpl element = new FunctionElementImpl(node.getName());
    element.setFunctions(holder.getFunctions());
    element.setLabels(holder.getLabels());
    element.setLocalVariables(holder.getVariables());
    if (holder.getParameters() != null) {
      element.setParameters(holder.getParameters());
    }
    currentHolder.addFunction(element);
    return null;
  }

  @Override
  public Void visitFunctionTypedFormalParameter(FunctionTypedFormalParameter node) {
    VariableElementImpl parameter = new VariableElementImpl(node.getIdentifier());
    currentHolder.addVariable(parameter);
    return null;
  }

  @Override
  public Void visitLabeledStatement(LabeledStatement node) {
    boolean onSwitchStatement = node.getStatement() instanceof SwitchStatement;
    for (Label label : node.getLabels()) {
      LabelElementImpl element = new LabelElementImpl(label.getLabel(), onSwitchStatement, false);
      currentHolder.addLabel(element);
    }
    return null;
  }

  @Override
  public Void visitMethodDeclaration(MethodDeclaration node) {
    ElementHolder holder = new ElementHolder();
    visitChildren(holder, node);

    Token property = node.getPropertyKeyword();
    if (property == null) {
      MethodElementImpl element = new MethodElementImpl(node.getName());
      Token keyword = node.getModifierKeyword();
      element.setAbstract(matches(keyword, Keyword.ABSTRACT));
      element.setFunctions(holder.getFunctions());
      element.setLabels(holder.getLabels());
      element.setLocalVariables(holder.getVariables());
      if (holder.getParameters() != null) {
        element.setParameters(holder.getParameters());
      }
      element.setStatic(matches(keyword, Keyword.STATIC));
      currentHolder.addMethod(element);
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
      } else {
        PropertyAccessorElementImpl setter = new PropertyAccessorElementImpl(propertyNameNode);
        setter.setField(field);
        setter.setSetter(true);
        field.setSetter(setter);
        field.setFinal(false);
      }
    }
    return null;
  }

  @Override
  public Void visitNamedFormalParameter(NamedFormalParameter node) {
    ElementHolder holder = new ElementHolder();
    visitChildren(holder, node.getDefaultValue());

    FunctionElementImpl initializer = new FunctionElementImpl();
    initializer.setFunctions(holder.getFunctions());
    initializer.setLabels(holder.getLabels());
    initializer.setLocalVariables(holder.getVariables());
    if (holder.getParameters() != null) {
      initializer.setParameters(holder.getParameters());
    }

    VariableElementImpl parameter = new VariableElementImpl(node.getParameter().getIdentifier());
    parameter.setInitializer(initializer);
    currentHolder.addVariable(parameter);
    return null;
  }

  @Override
  public Void visitSimpleFormalParameter(SimpleFormalParameter node) {
    VariableElementImpl parameter = new VariableElementImpl(node.getIdentifier());
    currentHolder.addVariable(parameter);
    return null;
  }

  @Override
  public Void visitSwitchCase(SwitchCase node) {
    for (Label label : node.getLabels()) {
      LabelElementImpl element = new LabelElementImpl(label.getLabel(), false, true);
      currentHolder.addLabel(element);
    }
    return null;
  }

  @Override
  public Void visitSwitchDefault(SwitchDefault node) {
    for (Label label : node.getLabels()) {
      LabelElementImpl element = new LabelElementImpl(label.getLabel(), false, true);
      currentHolder.addLabel(element);
    }
    return null;
  }

  @Override
  public Void visitTypeAlias(TypeAlias node) {
    ElementHolder holder = new ElementHolder();
    visitChildren(holder, node);

    TypeAliasElementImpl element = new TypeAliasElementImpl(node.getName());
    if (holder.getParameters() != null) {
      element.setParameters(holder.getParameters());
    }
    element.setTypeVariables(holder.getTypeVariables());
    currentHolder.addTypeAlias(element);
    return null;
  }

  @Override
  public Void visitTypeParameter(TypeParameter node) {
    TypeVariableElementImpl element = new TypeVariableElementImpl(node.getName());
    currentHolder.addTypeVariable(element);
    return null;
  }

  @Override
  public Void visitVariableDeclaration(VariableDeclaration node) {
    VariableElementImpl element;
    if (inFieldContext) {
      element = new FieldElementImpl(node.getName());
      currentHolder.addField((FieldElementImpl) element);
    } else {
      element = new VariableElementImpl(node.getName());
      currentHolder.addVariable(element);
    }

    Token keyword = ((VariableDeclarationList) node.getParent()).getKeyword();
    element.setConst(matches(keyword, Keyword.CONST));
    element.setFinal(matches(keyword, Keyword.FINAL));
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

      PropertyAccessorElementImpl setter = new PropertyAccessorElementImpl(field);
      setter.setSetter(true);
      field.setSetter(setter);

      field.setStatic(matches(
          ((FieldDeclaration) node.getParent().getParent()).getKeyword(),
          Keyword.STATIC));
    }
    return null;
  }

  /**
   * Return {@code true} if any of the given methods are abstract.
   * 
   * @param methods the methods being tested
   * @return {@code true} if any of the given methods are abstract
   */
  private boolean hasAbstractMethod(MethodElement[] methods) {
    for (MethodElement method : methods) {
      if (method.isAbstract()) {
        return true;
      }
    }
    return false;
  }

  private boolean matches(Token token, Keyword keyword) {
    return token != null && token.getType() == TokenType.KEYWORD
        && ((KeywordToken) token).getKeyword() == keyword;
  }

  private Void visitChildren(ElementHolder holder, ASTNode node) {
    ElementHolder previousBuilder = currentHolder;
    currentHolder = holder;
    try {
      node.visitChildren(this);
    } finally {
      currentHolder = previousBuilder;
    }
    return null;
  }
}
