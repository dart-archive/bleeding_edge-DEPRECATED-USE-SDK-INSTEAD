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

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.CatchClause;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassMember;
import com.google.dart.engine.ast.ClassTypeAlias;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.DeclaredIdentifier;
import com.google.dart.engine.ast.DefaultFormalParameter;
import com.google.dart.engine.ast.EmptyFunctionBody;
import com.google.dart.engine.ast.EnumDeclaration;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.FieldFormalParameter;
import com.google.dart.engine.ast.ForEachStatement;
import com.google.dart.engine.ast.FormalParameter;
import com.google.dart.engine.ast.FunctionBody;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.FunctionTypedFormalParameter;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.Label;
import com.google.dart.engine.ast.LabeledStatement;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.NormalFormalParameter;
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SuperExpression;
import com.google.dart.engine.ast.SwitchCase;
import com.google.dart.engine.ast.SwitchDefault;
import com.google.dart.engine.ast.SwitchStatement;
import com.google.dart.engine.ast.TypeParameter;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.visitor.RecursiveAstVisitor;
import com.google.dart.engine.ast.visitor.UnifyingAstVisitor;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.TypeParameterElement;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.ConstFieldElementImpl;
import com.google.dart.engine.internal.element.ConstLocalVariableElementImpl;
import com.google.dart.engine.internal.element.ConstTopLevelVariableElementImpl;
import com.google.dart.engine.internal.element.ConstructorElementImpl;
import com.google.dart.engine.internal.element.DefaultFieldFormalParameterElementImpl;
import com.google.dart.engine.internal.element.DefaultParameterElementImpl;
import com.google.dart.engine.internal.element.FieldElementImpl;
import com.google.dart.engine.internal.element.FieldFormalParameterElementImpl;
import com.google.dart.engine.internal.element.FunctionElementImpl;
import com.google.dart.engine.internal.element.FunctionTypeAliasElementImpl;
import com.google.dart.engine.internal.element.LabelElementImpl;
import com.google.dart.engine.internal.element.LocalVariableElementImpl;
import com.google.dart.engine.internal.element.MethodElementImpl;
import com.google.dart.engine.internal.element.ParameterElementImpl;
import com.google.dart.engine.internal.element.PropertyAccessorElementImpl;
import com.google.dart.engine.internal.element.PropertyInducingElementImpl;
import com.google.dart.engine.internal.element.TopLevelVariableElementImpl;
import com.google.dart.engine.internal.element.TypeParameterElementImpl;
import com.google.dart.engine.internal.element.VariableElementImpl;
import com.google.dart.engine.internal.type.FunctionTypeImpl;
import com.google.dart.engine.internal.type.InterfaceTypeImpl;
import com.google.dart.engine.internal.type.TypeParameterTypeImpl;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.dart.ParameterKind;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Instances of the class {@code ElementBuilder} traverse an AST structure and build the element
 * model representing the AST structure.
 * 
 * @coverage dart.engine.resolver
 */
public class ElementBuilder extends RecursiveAstVisitor<Void> {
  /**
   * The element holder associated with the element that is currently being built.
   */
  private ElementHolder currentHolder;

  /**
   * A flag indicating whether a variable declaration is in the context of a field declaration.
   */
  private boolean inFieldContext = false;

  /**
   * A flag indicating whether a variable declaration is within the body of a method or function.
   */
  private boolean inFunction = false;

  /**
   * A flag indicating whether the class currently being visited can be used as a mixin.
   */
  private boolean isValidMixin = false;

  /**
   * A collection holding the function types defined in a class that need to have their type
   * arguments set to the types of the type parameters for the class, or {@code null} if we are not
   * currently processing nodes within a class.
   */
  private ArrayList<FunctionTypeImpl> functionTypesToFix = null;

  /**
   * A table mapping field names to field elements for the fields defined in the current class, or
   * {@code null} if we are not in the scope of a class.
   */
  private HashMap<String, FieldElement> fieldMap;

  /**
   * Initialize a newly created element builder to build the elements for a compilation unit.
   * 
   * @param initialHolder the element holder associated with the compilation unit being built
   */
  public ElementBuilder(ElementHolder initialHolder) {
    currentHolder = initialHolder;
  }

  @Override
  public Void visitBlock(Block node) {
    boolean wasInField = inFieldContext;
    inFieldContext = false;
    try {
      node.visitChildren(this);
    } finally {
      inFieldContext = wasInField;
    }
    return null;
  }

  @Override
  public Void visitCatchClause(CatchClause node) {
    SimpleIdentifier exceptionParameter = node.getExceptionParameter();
    if (exceptionParameter != null) {
      LocalVariableElementImpl exception = new LocalVariableElementImpl(exceptionParameter);

      currentHolder.addLocalVariable(exception);
      exceptionParameter.setStaticElement(exception);

      SimpleIdentifier stackTraceParameter = node.getStackTraceParameter();
      if (stackTraceParameter != null) {
        LocalVariableElementImpl stackTrace = new LocalVariableElementImpl(stackTraceParameter);

        currentHolder.addLocalVariable(stackTrace);
        stackTraceParameter.setStaticElement(stackTrace);
      }
    }
    return super.visitCatchClause(node);
  }

  @Override
  public Void visitClassDeclaration(ClassDeclaration node) {
    ElementHolder holder = new ElementHolder();
    isValidMixin = true;
    functionTypesToFix = new ArrayList<FunctionTypeImpl>();
    //
    // Process field declarations before constructors and methods so that field formal parameters
    // can be correctly resolved to their fields.
    //
    ElementHolder previousHolder = currentHolder;
    currentHolder = holder;
    try {
      final ArrayList<ClassMember> nonFields = new ArrayList<ClassMember>();
      node.visitChildren(new UnifyingAstVisitor<Void>() {
        @Override
        public Void visitConstructorDeclaration(ConstructorDeclaration node) {
          nonFields.add(node);
          return null;
        }

        @Override
        public Void visitMethodDeclaration(MethodDeclaration node) {
          nonFields.add(node);
          return null;
        }

        @Override
        public Void visitNode(AstNode node) {
          return node.accept(ElementBuilder.this);
        }
      });
      buildFieldMap(holder.getFieldsWithoutFlushing());
      int count = nonFields.size();
      for (int i = 0; i < count; i++) {
        nonFields.get(i).accept(this);
      }
    } finally {
      currentHolder = previousHolder;
    }

    SimpleIdentifier className = node.getName();
    ClassElementImpl element = new ClassElementImpl(className);
    TypeParameterElement[] typeParameters = holder.getTypeParameters();

    Type[] typeArguments = createTypeParameterTypes(typeParameters);
    InterfaceTypeImpl interfaceType = new InterfaceTypeImpl(element);
    interfaceType.setTypeArguments(typeArguments);
    element.setType(interfaceType);

    ConstructorElement[] constructors = holder.getConstructors();
    if (constructors.length == 0) {
      //
      // Create the default constructor.
      //
      constructors = createDefaultConstructors(interfaceType);
    }
    element.setAbstract(node.isAbstract());
    element.setAccessors(holder.getAccessors());
    element.setConstructors(constructors);
    element.setFields(holder.getFields());
    element.setMethods(holder.getMethods());
    element.setTypeParameters(typeParameters);
    element.setValidMixin(isValidMixin);

    int functionTypeCount = functionTypesToFix.size();
    for (int i = 0; i < functionTypeCount; i++) {
      functionTypesToFix.get(i).setTypeArguments(typeArguments);
    }
    functionTypesToFix = null;
    currentHolder.addType(element);
    className.setStaticElement(element);
    fieldMap = null;
    holder.validate();
    return null;
  }

  @Override
  public Void visitClassTypeAlias(ClassTypeAlias node) {
    ElementHolder holder = new ElementHolder();
    functionTypesToFix = new ArrayList<FunctionTypeImpl>();
    visitChildren(holder, node);

    SimpleIdentifier className = node.getName();
    ClassElementImpl element = new ClassElementImpl(className);
    element.setAbstract(node.getAbstractKeyword() != null);
    element.setTypedef(true);
    TypeParameterElement[] typeParameters = holder.getTypeParameters();
    element.setTypeParameters(typeParameters);

    Type[] typeArguments = createTypeParameterTypes(typeParameters);
    InterfaceTypeImpl interfaceType = new InterfaceTypeImpl(element);
    interfaceType.setTypeArguments(typeArguments);
    element.setType(interfaceType);

    // set default constructor
    element.setConstructors(createDefaultConstructors(interfaceType));

    for (FunctionTypeImpl functionType : functionTypesToFix) {
      functionType.setTypeArguments(typeArguments);
    }
    functionTypesToFix = null;
    currentHolder.addType(element);
    className.setStaticElement(element);
    holder.validate();
    return null;
  }

  @Override
  public Void visitConstructorDeclaration(ConstructorDeclaration node) {
    isValidMixin = false;
    ElementHolder holder = new ElementHolder();
    boolean wasInFunction = inFunction;
    inFunction = true;
    try {
      visitChildren(holder, node);
    } finally {
      inFunction = wasInFunction;
    }

    FunctionBody body = node.getBody();
    SimpleIdentifier constructorName = node.getName();
    ConstructorElementImpl element = new ConstructorElementImpl(constructorName);
    if (node.getFactoryKeyword() != null) {
      element.setFactory(true);
    }
    element.setFunctions(holder.getFunctions());
    element.setLabels(holder.getLabels());
    element.setLocalVariables(holder.getLocalVariables());
    element.setParameters(holder.getParameters());
    element.setConst(node.getConstKeyword() != null);
    if (body.isAsynchronous()) {
      element.setAsynchronous(true);
    }
    if (body.isGenerator()) {
      element.setGenerator(true);
    }

    currentHolder.addConstructor(element);
    node.setElement(element);
    if (constructorName == null) {
      Identifier returnType = node.getReturnType();
      if (returnType != null) {
        element.setNameOffset(returnType.getOffset());
      }
    } else {
      constructorName.setStaticElement(element);
    }
    holder.validate();
    return null;
  }

  @Override
  public Void visitDeclaredIdentifier(DeclaredIdentifier node) {
    SimpleIdentifier variableName = node.getIdentifier();
    Token keyword = node.getKeyword();

    LocalVariableElementImpl element = new LocalVariableElementImpl(variableName);
    ForEachStatement statement = (ForEachStatement) node.getParent();
    int declarationEnd = node.getOffset() + node.getLength();
    int statementEnd = statement.getOffset() + statement.getLength();
    element.setVisibleRange(declarationEnd, statementEnd - declarationEnd - 1);
    element.setConst(matches(keyword, Keyword.CONST));
    element.setFinal(matches(keyword, Keyword.FINAL));

    currentHolder.addLocalVariable(element);
    variableName.setStaticElement(element);
    return super.visitDeclaredIdentifier(node);
  }

  @Override
  public Void visitDefaultFormalParameter(DefaultFormalParameter node) {
    ElementHolder holder = new ElementHolder();

    NormalFormalParameter normalParameter = node.getParameter();
    SimpleIdentifier parameterName = normalParameter.getIdentifier();
    ParameterElementImpl parameter;
    if (normalParameter instanceof FieldFormalParameter) {
      parameter = new DefaultFieldFormalParameterElementImpl(parameterName);
      FieldElement field = fieldMap == null ? null : fieldMap.get(parameterName.getName());
      if (field != null) {
        ((DefaultFieldFormalParameterElementImpl) parameter).setField(field);
      }
    } else {
      parameter = new DefaultParameterElementImpl(parameterName);
    }
    parameter.setConst(node.isConst());
    parameter.setFinal(node.isFinal());
    parameter.setParameterKind(node.getKind());

    // set initializer, default value range
    Expression defaultValue = node.getDefaultValue();
    if (defaultValue != null) {
      visit(holder, defaultValue);

      FunctionElementImpl initializer = new FunctionElementImpl(
          defaultValue.getBeginToken().getOffset());
      initializer.setFunctions(holder.getFunctions());
      initializer.setLabels(holder.getLabels());
      initializer.setLocalVariables(holder.getLocalVariables());
      initializer.setParameters(holder.getParameters());
      initializer.setSynthetic(true);

      parameter.setInitializer(initializer);
      parameter.setDefaultValueCode(defaultValue.toSource());
    }

    // visible range
    setParameterVisibleRange(node, parameter);

    currentHolder.addParameter(parameter);
    parameterName.setStaticElement(parameter);
    normalParameter.accept(this);
    holder.validate();
    return null;
  }

  @Override
  public Void visitEnumDeclaration(EnumDeclaration node) {
    SimpleIdentifier enumName = node.getName();
    ClassElementImpl enumElement = new ClassElementImpl(enumName);
    enumElement.setEnum(true);
    InterfaceTypeImpl enumType = new InterfaceTypeImpl(enumElement);
    enumElement.setType(enumType);
    currentHolder.addEnum(enumElement);
    enumName.setStaticElement(enumElement);
    return super.visitEnumDeclaration(node);
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
    if (!(node.getParent() instanceof DefaultFormalParameter)) {
      SimpleIdentifier parameterName = node.getIdentifier();
      FieldElement field = fieldMap == null ? null : fieldMap.get(parameterName.getName());
      FieldFormalParameterElementImpl parameter = new FieldFormalParameterElementImpl(parameterName);
      parameter.setConst(node.isConst());
      parameter.setFinal(node.isFinal());
      parameter.setParameterKind(node.getKind());
      if (field != null) {
        parameter.setField(field);
      }

      currentHolder.addParameter(parameter);
      parameterName.setStaticElement(parameter);
    }
    //
    // The children of this parameter include any parameters defined on the type of this parameter.
    //
    ElementHolder holder = new ElementHolder();
    visitChildren(holder, node);
    ((ParameterElementImpl) node.getElement()).setParameters(holder.getParameters());
    holder.validate();
    return null;
  }

  @Override
  public Void visitFunctionDeclaration(FunctionDeclaration node) {
    FunctionExpression expression = node.getFunctionExpression();
    if (expression != null) {
      ElementHolder holder = new ElementHolder();
      boolean wasInFunction = inFunction;
      inFunction = true;
      try {
        visitChildren(holder, expression);
      } finally {
        inFunction = wasInFunction;
      }

      FunctionBody body = expression.getBody();
      Token property = node.getPropertyKeyword();
      if (property == null) {
        SimpleIdentifier functionName = node.getName();
        FunctionElementImpl element = new FunctionElementImpl(functionName);
        element.setFunctions(holder.getFunctions());
        element.setLabels(holder.getLabels());
        element.setLocalVariables(holder.getLocalVariables());
        element.setParameters(holder.getParameters());
        if (body.isAsynchronous()) {
          element.setAsynchronous(true);
        }
        if (body.isGenerator()) {
          element.setGenerator(true);
        }

        if (inFunction) {
          Block enclosingBlock = node.getAncestor(Block.class);
          if (enclosingBlock != null) {
            int functionEnd = node.getOffset() + node.getLength();
            int blockEnd = enclosingBlock.getOffset() + enclosingBlock.getLength();
            element.setVisibleRange(functionEnd, blockEnd - functionEnd - 1);
          }
        }

        currentHolder.addFunction(element);
        expression.setElement(element);
        functionName.setStaticElement(element);
      } else {
        SimpleIdentifier propertyNameNode = node.getName();
        if (propertyNameNode == null) {
          // TODO(brianwilkerson) Report this internal error.
          return null;
        }
        String propertyName = propertyNameNode.getName();
        TopLevelVariableElementImpl variable = (TopLevelVariableElementImpl) currentHolder.getTopLevelVariable(propertyName);
        if (variable == null) {
          variable = new TopLevelVariableElementImpl(node.getName().getName(), -1);
          variable.setFinal(true);
          variable.setSynthetic(true);

          currentHolder.addTopLevelVariable(variable);
        }
        if (matches(property, Keyword.GET)) {
          PropertyAccessorElementImpl getter = new PropertyAccessorElementImpl(propertyNameNode);
          getter.setFunctions(holder.getFunctions());
          getter.setLabels(holder.getLabels());
          getter.setLocalVariables(holder.getLocalVariables());
          if (body.isAsynchronous()) {
            getter.setAsynchronous(true);
          }
          if (body.isGenerator()) {
            getter.setGenerator(true);
          }

          getter.setVariable(variable);
          getter.setGetter(true);
          getter.setStatic(true);
          variable.setGetter(getter);

          currentHolder.addAccessor(getter);
          expression.setElement(getter);
          propertyNameNode.setStaticElement(getter);
        } else {
          PropertyAccessorElementImpl setter = new PropertyAccessorElementImpl(propertyNameNode);
          setter.setFunctions(holder.getFunctions());
          setter.setLabels(holder.getLabels());
          setter.setLocalVariables(holder.getLocalVariables());
          setter.setParameters(holder.getParameters());
          if (body.isAsynchronous()) {
            setter.setAsynchronous(true);
          }
          if (body.isGenerator()) {
            setter.setGenerator(true);
          }

          setter.setVariable(variable);
          setter.setSetter(true);
          setter.setStatic(true);
          variable.setSetter(setter);
          variable.setFinal(false);

          currentHolder.addAccessor(setter);
          expression.setElement(setter);
          propertyNameNode.setStaticElement(setter);
        }
      }
      holder.validate();
    }
    return null;
  }

  @Override
  public Void visitFunctionExpression(FunctionExpression node) {
    ElementHolder holder = new ElementHolder();
    boolean wasInFunction = inFunction;
    inFunction = true;
    try {
      visitChildren(holder, node);
    } finally {
      inFunction = wasInFunction;
    }

    FunctionBody body = node.getBody();
    FunctionElementImpl element = new FunctionElementImpl(node.getBeginToken().getOffset());
    element.setFunctions(holder.getFunctions());
    element.setLabels(holder.getLabels());
    element.setLocalVariables(holder.getLocalVariables());
    element.setParameters(holder.getParameters());
    if (body.isAsynchronous()) {
      element.setAsynchronous(true);
    }
    if (body.isGenerator()) {
      element.setGenerator(true);
    }
    if (inFunction) {
      Block enclosingBlock = node.getAncestor(Block.class);
      if (enclosingBlock != null) {
        int functionEnd = node.getOffset() + node.getLength();
        int blockEnd = enclosingBlock.getOffset() + enclosingBlock.getLength();
        element.setVisibleRange(functionEnd, blockEnd - functionEnd - 1);
      }
    }

    FunctionTypeImpl type = new FunctionTypeImpl(element);
    if (functionTypesToFix != null) {
      functionTypesToFix.add(type);
    }
    element.setType(type);

    currentHolder.addFunction(element);
    node.setElement(element);
    holder.validate();
    return null;
  }

  @Override
  public Void visitFunctionTypeAlias(FunctionTypeAlias node) {
    ElementHolder holder = new ElementHolder();
    visitChildren(holder, node);

    SimpleIdentifier aliasName = node.getName();
    ParameterElement[] parameters = holder.getParameters();
    TypeParameterElement[] typeParameters = holder.getTypeParameters();
    FunctionTypeAliasElementImpl element = new FunctionTypeAliasElementImpl(aliasName);
    element.setParameters(parameters);
    element.setTypeParameters(typeParameters);

    FunctionTypeImpl type = new FunctionTypeImpl(element);
    type.setTypeArguments(createTypeParameterTypes(typeParameters));
    element.setType(type);

    currentHolder.addTypeAlias(element);
    aliasName.setStaticElement(element);
    holder.validate();
    return null;
  }

  @Override
  public Void visitFunctionTypedFormalParameter(FunctionTypedFormalParameter node) {
    if (!(node.getParent() instanceof DefaultFormalParameter)) {
      SimpleIdentifier parameterName = node.getIdentifier();
      ParameterElementImpl parameter = new ParameterElementImpl(parameterName);
      parameter.setParameterKind(node.getKind());
      setParameterVisibleRange(node, parameter);

      currentHolder.addParameter(parameter);
      parameterName.setStaticElement(parameter);
    }
    //
    // The children of this parameter include any parameters defined on the type of this parameter.
    //
    ElementHolder holder = new ElementHolder();
    visitChildren(holder, node);
    ((ParameterElementImpl) node.getElement()).setParameters(holder.getParameters());
    holder.validate();
    return null;
  }

  @Override
  public Void visitLabeledStatement(LabeledStatement node) {
    boolean onSwitchStatement = node.getStatement() instanceof SwitchStatement;
    for (Label label : node.getLabels()) {
      SimpleIdentifier labelName = label.getLabel();
      LabelElementImpl element = new LabelElementImpl(labelName, onSwitchStatement, false);

      currentHolder.addLabel(element);
      labelName.setStaticElement(element);
    }
    return super.visitLabeledStatement(node);
  }

  @Override
  public Void visitMethodDeclaration(MethodDeclaration node) {
    try {
      ElementHolder holder = new ElementHolder();
      boolean wasInFunction = inFunction;
      inFunction = true;
      try {
        visitChildren(holder, node);
      } finally {
        inFunction = wasInFunction;
      }

      boolean isStatic = node.isStatic();
      Token property = node.getPropertyKeyword();
      FunctionBody body = node.getBody();
      if (property == null) {
        SimpleIdentifier methodName = node.getName();
        String nameOfMethod = methodName.getName();
        if (nameOfMethod.equals(TokenType.MINUS.getLexeme())
            && node.getParameters().getParameters().size() == 0) {
          nameOfMethod = "unary-";
        }
        MethodElementImpl element = new MethodElementImpl(nameOfMethod, methodName.getOffset());
        element.setAbstract(node.isAbstract());
        element.setFunctions(holder.getFunctions());
        element.setLabels(holder.getLabels());
        element.setLocalVariables(holder.getLocalVariables());
        element.setParameters(holder.getParameters());
        element.setStatic(isStatic);
        if (body.isAsynchronous()) {
          element.setAsynchronous(true);
        }
        if (body.isGenerator()) {
          element.setGenerator(true);
        }

        currentHolder.addMethod(element);
        methodName.setStaticElement(element);
      } else {
        SimpleIdentifier propertyNameNode = node.getName();
        String propertyName = propertyNameNode.getName();
        FieldElementImpl field = (FieldElementImpl) currentHolder.getField(propertyName);
        if (field == null) {
          field = new FieldElementImpl(node.getName().getName(), -1);
          field.setFinal(true);
          field.setStatic(isStatic);
          field.setSynthetic(true);

          currentHolder.addField(field);
        }
        if (matches(property, Keyword.GET)) {
          PropertyAccessorElementImpl getter = new PropertyAccessorElementImpl(propertyNameNode);
          getter.setFunctions(holder.getFunctions());
          getter.setLabels(holder.getLabels());
          getter.setLocalVariables(holder.getLocalVariables());
          if (body.isAsynchronous()) {
            getter.setAsynchronous(true);
          }
          if (body.isGenerator()) {
            getter.setGenerator(true);
          }

          getter.setVariable(field);
          getter.setAbstract(body instanceof EmptyFunctionBody && node.getExternalKeyword() == null);
          getter.setGetter(true);
          getter.setStatic(isStatic);
          field.setGetter(getter);

          currentHolder.addAccessor(getter);
          propertyNameNode.setStaticElement(getter);
        } else {
          PropertyAccessorElementImpl setter = new PropertyAccessorElementImpl(propertyNameNode);
          setter.setFunctions(holder.getFunctions());
          setter.setLabels(holder.getLabels());
          setter.setLocalVariables(holder.getLocalVariables());
          setter.setParameters(holder.getParameters());
          if (body.isAsynchronous()) {
            setter.setAsynchronous(true);
          }
          if (body.isGenerator()) {
            setter.setGenerator(true);
          }

          setter.setVariable(field);
          setter.setAbstract(body instanceof EmptyFunctionBody
              && !matches(node.getExternalKeyword(), Keyword.EXTERNAL));
          setter.setSetter(true);
          setter.setStatic(isStatic);
          field.setSetter(setter);
          field.setFinal(false);

          currentHolder.addAccessor(setter);
          propertyNameNode.setStaticElement(setter);
        }
      }
      holder.validate();
    } catch (Exception ex) {
      if (node.getName().getStaticElement() == null) {
        ClassDeclaration classNode = node.getAncestor(ClassDeclaration.class);
        StringBuilder builder = new StringBuilder();
        builder.append("The element for the method ");
        builder.append(node.getName());
        builder.append(" in ");
        builder.append(classNode.getName());
        builder.append(" was not set while trying to build the element model.");
        AnalysisEngine.getInstance().getLogger().logError(
            builder.toString(),
            new AnalysisException(builder.toString(), ex));
      } else {
        String message = "Exception caught in ElementBuilder.visitMethodDeclaration()";
        AnalysisEngine.getInstance().getLogger().logError(
            message,
            new AnalysisException(message, ex));
      }
    } finally {
      if (node.getName().getStaticElement() == null) {
        ClassDeclaration classNode = node.getAncestor(ClassDeclaration.class);
        StringBuilder builder = new StringBuilder();
        builder.append("The element for the method ");
        builder.append(node.getName());
        builder.append(" in ");
        builder.append(classNode.getName());
        builder.append(" was not set while trying to resolve types.");
        AnalysisEngine.getInstance().getLogger().logError(
            builder.toString(),
            new AnalysisException(builder.toString()));
      }
    }
    return null;
  }

  @Override
  public Void visitSimpleFormalParameter(SimpleFormalParameter node) {
    if (!(node.getParent() instanceof DefaultFormalParameter)) {
      SimpleIdentifier parameterName = node.getIdentifier();
      ParameterElementImpl parameter = new ParameterElementImpl(parameterName);
      parameter.setConst(node.isConst());
      parameter.setFinal(node.isFinal());
      parameter.setParameterKind(node.getKind());
      setParameterVisibleRange(node, parameter);

      currentHolder.addParameter(parameter);
      parameterName.setStaticElement(parameter);
    }
    return super.visitSimpleFormalParameter(node);
  }

  @Override
  public Void visitSuperExpression(SuperExpression node) {
    isValidMixin = false;
    return super.visitSuperExpression(node);
  }

  @Override
  public Void visitSwitchCase(SwitchCase node) {
    for (Label label : node.getLabels()) {
      SimpleIdentifier labelName = label.getLabel();
      LabelElementImpl element = new LabelElementImpl(labelName, false, true);

      currentHolder.addLabel(element);
      labelName.setStaticElement(element);
    }
    return super.visitSwitchCase(node);
  }

  @Override
  public Void visitSwitchDefault(SwitchDefault node) {
    for (Label label : node.getLabels()) {
      SimpleIdentifier labelName = label.getLabel();
      LabelElementImpl element = new LabelElementImpl(labelName, false, true);

      currentHolder.addLabel(element);
      labelName.setStaticElement(element);
    }
    return super.visitSwitchDefault(node);
  }

  @Override
  public Void visitTypeParameter(TypeParameter node) {
    SimpleIdentifier parameterName = node.getName();
    TypeParameterElementImpl typeParameter = new TypeParameterElementImpl(parameterName);

    TypeParameterTypeImpl typeParameterType = new TypeParameterTypeImpl(typeParameter);
    typeParameter.setType(typeParameterType);

    currentHolder.addTypeParameter(typeParameter);
    parameterName.setStaticElement(typeParameter);
    return super.visitTypeParameter(node);
  }

  @Override
  public Void visitVariableDeclaration(VariableDeclaration node) {
    Token keyword = ((VariableDeclarationList) node.getParent()).getKeyword();
    boolean isConst = matches(keyword, Keyword.CONST);
    boolean isFinal = matches(keyword, Keyword.FINAL);
    boolean hasInitializer = node.getInitializer() != null;

    VariableElementImpl element;
    if (inFieldContext) {
      SimpleIdentifier fieldName = node.getName();
      FieldElementImpl field;
      if (isConst && hasInitializer) {
        field = new ConstFieldElementImpl(fieldName);
      } else {
        field = new FieldElementImpl(fieldName);
      }
      element = field;

      currentHolder.addField(field);
      fieldName.setStaticElement(field);
    } else if (inFunction) {
      SimpleIdentifier variableName = node.getName();
      LocalVariableElementImpl variable;
      if (isConst && hasInitializer) {
        variable = new ConstLocalVariableElementImpl(variableName);
      } else {
        variable = new LocalVariableElementImpl(variableName);
      }
      element = variable;
      Block enclosingBlock = node.getAncestor(Block.class);
      int functionEnd = node.getOffset() + node.getLength();
      int blockEnd = enclosingBlock.getOffset() + enclosingBlock.getLength();
      // TODO(brianwilkerson) This isn't right for variables declared in a for loop.
      variable.setVisibleRange(functionEnd, blockEnd - functionEnd - 1);

      currentHolder.addLocalVariable(variable);
      variableName.setStaticElement(element);
    } else {
      SimpleIdentifier variableName = node.getName();
      TopLevelVariableElementImpl variable;
      if (isConst && hasInitializer) {
        variable = new ConstTopLevelVariableElementImpl(variableName);
      } else {
        variable = new TopLevelVariableElementImpl(variableName);
      }
      element = variable;

      currentHolder.addTopLevelVariable(variable);
      variableName.setStaticElement(element);
    }

    element.setConst(isConst);
    element.setFinal(isFinal);
    if (hasInitializer) {
      ElementHolder holder = new ElementHolder();
      boolean wasInFieldContext = inFieldContext;
      inFieldContext = false;
      try {
        visit(holder, node.getInitializer());
      } finally {
        inFieldContext = wasInFieldContext;
      }
      FunctionElementImpl initializer = new FunctionElementImpl(
          node.getInitializer().getBeginToken().getOffset());
      initializer.setFunctions(holder.getFunctions());
      initializer.setLabels(holder.getLabels());
      initializer.setLocalVariables(holder.getLocalVariables());
      initializer.setSynthetic(true);
      element.setInitializer(initializer);
      holder.validate();
    }
    if (element instanceof PropertyInducingElementImpl) {
      PropertyInducingElementImpl variable = (PropertyInducingElementImpl) element;

      if (inFieldContext) {
        ((FieldElementImpl) variable).setStatic(matches(
            ((FieldDeclaration) node.getParent().getParent()).getStaticKeyword(),
            Keyword.STATIC));
      }

      PropertyAccessorElementImpl getter = new PropertyAccessorElementImpl(variable);
      getter.setGetter(true);

      currentHolder.addAccessor(getter);
      variable.setGetter(getter);

      if (!isFinal) {
        PropertyAccessorElementImpl setter = new PropertyAccessorElementImpl(variable);
        setter.setSetter(true);
        ParameterElementImpl parameter = new ParameterElementImpl(
            "_" + variable.getName(),
            variable.getNameOffset());
        parameter.setSynthetic(true);
        parameter.setParameterKind(ParameterKind.REQUIRED);
        setter.setParameters(new ParameterElement[] {parameter});

        currentHolder.addAccessor(setter);
        variable.setSetter(setter);
      }
    }
    return null;
  }

  /**
   * Build the table mapping field names to field elements for the fields defined in the current
   * class.
   * 
   * @param fields the field elements defined in the current class
   */
  private void buildFieldMap(FieldElement[] fields) {
    fieldMap = new HashMap<String, FieldElement>();
    int count = fields.length;
    for (int i = 0; i < count; i++) {
      FieldElement field = fields[i];
      fieldMap.put(field.getName(), field);
    }
  }

  /**
   * Creates the {@link ConstructorElement}s array with the single default constructor element.
   * 
   * @param interfaceType the interface type for which to create a default constructor
   * @return the {@link ConstructorElement}s array with the single default constructor element
   */
  private ConstructorElement[] createDefaultConstructors(InterfaceTypeImpl interfaceType) {
    ConstructorElementImpl constructor = new ConstructorElementImpl(null);
    constructor.setSynthetic(true);
    constructor.setReturnType(interfaceType);
    FunctionTypeImpl type = new FunctionTypeImpl(constructor);
    functionTypesToFix.add(type);
    constructor.setType(type);
    return new ConstructorElement[] {constructor};
  }

  /**
   * Create the types associated with the given type parameters, setting the type of each type
   * parameter, and return an array of types corresponding to the given parameters.
   * 
   * @param typeParameters the type parameters for which types are to be created
   * @return an array of types corresponding to the given parameters
   */
  private Type[] createTypeParameterTypes(TypeParameterElement[] typeParameters) {
    int typeParameterCount = typeParameters.length;
    Type[] typeArguments = new Type[typeParameterCount];
    for (int i = 0; i < typeParameterCount; i++) {
      TypeParameterElementImpl typeParameter = (TypeParameterElementImpl) typeParameters[i];
      TypeParameterTypeImpl typeParameterType = new TypeParameterTypeImpl(typeParameter);
      typeParameter.setType(typeParameterType);
      typeArguments[i] = typeParameterType;
    }
    return typeArguments;
  }

  /**
   * Return the body of the function that contains the given parameter, or {@code null} if no
   * function body could be found.
   * 
   * @param node the parameter contained in the function whose body is to be returned
   * @return the body of the function that contains the given parameter
   */
  private FunctionBody getFunctionBody(FormalParameter node) {
    AstNode parent = node.getParent();
    while (parent != null) {
      if (parent instanceof ConstructorDeclaration) {
        return ((ConstructorDeclaration) parent).getBody();
      } else if (parent instanceof FunctionExpression) {
        return ((FunctionExpression) parent).getBody();
      } else if (parent instanceof MethodDeclaration) {
        return ((MethodDeclaration) parent).getBody();
      }
      parent = parent.getParent();
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
   * Sets the visible source range for formal parameter.
   */
  private void setParameterVisibleRange(FormalParameter node, ParameterElementImpl element) {
    FunctionBody body = getFunctionBody(node);
    if (body != null) {
      element.setVisibleRange(body.getOffset(), body.getLength());
    }
  }

  /**
   * Make the given holder be the current holder while visiting the given node.
   * 
   * @param holder the holder that will gather elements that are built while visiting the children
   * @param node the node to be visited
   */
  private void visit(ElementHolder holder, AstNode node) {
    if (node != null) {
      ElementHolder previousHolder = currentHolder;
      currentHolder = holder;
      try {
        node.accept(this);
      } finally {
        currentHolder = previousHolder;
      }
    }
  }

  /**
   * Make the given holder be the current holder while visiting the children of the given node.
   * 
   * @param holder the holder that will gather elements that are built while visiting the children
   * @param node the node whose children are to be visited
   */
  private void visitChildren(ElementHolder holder, AstNode node) {
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
