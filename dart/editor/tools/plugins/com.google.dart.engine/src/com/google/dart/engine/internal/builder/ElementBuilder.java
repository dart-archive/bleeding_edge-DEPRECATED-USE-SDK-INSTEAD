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
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.CatchClause;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassTypeAlias;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.DeclaredIdentifier;
import com.google.dart.engine.ast.DefaultFormalParameter;
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
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SuperExpression;
import com.google.dart.engine.ast.SwitchCase;
import com.google.dart.engine.ast.SwitchDefault;
import com.google.dart.engine.ast.SwitchStatement;
import com.google.dart.engine.ast.TypeParameter;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.visitor.RecursiveASTVisitor;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.TypeVariableElement;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.ConstFieldElementImpl;
import com.google.dart.engine.internal.element.ConstLocalVariableElementImpl;
import com.google.dart.engine.internal.element.ConstParameterElementImpl;
import com.google.dart.engine.internal.element.ConstTopLevelVariableElementImpl;
import com.google.dart.engine.internal.element.ConstructorElementImpl;
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

/**
 * Instances of the class {@code ElementBuilder} traverse an AST structure and build the element
 * model representing the AST structure.
 * 
 * @coverage dart.engine.resolver
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
   * A flag indicating whether a variable declaration is within the body of a method or function.
   */
  private boolean inFunction = false;

  /**
   * A flag indicating whether the class currently being visited can be used as a mixin.
   */
  private boolean isValidMixin = false;

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
      exceptionParameter.setElement(exception);

      SimpleIdentifier stackTraceParameter = node.getStackTraceParameter();
      if (stackTraceParameter != null) {
        LocalVariableElementImpl stackTrace = new LocalVariableElementImpl(stackTraceParameter);

        currentHolder.addLocalVariable(stackTrace);
        stackTraceParameter.setElement(stackTrace);
      }
    }
    return super.visitCatchClause(node);
  }

  @Override
  public Void visitClassDeclaration(ClassDeclaration node) {
    ElementHolder holder = new ElementHolder();
    isValidMixin = true;
    visitChildren(holder, node);

    SimpleIdentifier className = node.getName();
    ClassElementImpl element = new ClassElementImpl(className);
    TypeVariableElement[] typeVariables = holder.getTypeVariables();

    InterfaceTypeImpl interfaceType = new InterfaceTypeImpl(element);
    interfaceType.setTypeArguments(createTypeVariableTypes(typeVariables));
    element.setType(interfaceType);

    ConstructorElement[] constructors = holder.getConstructors();
    if (constructors.length == 0) {
      //
      // Create the default constructor.
      //
      ConstructorElementImpl constructor = new ConstructorElementImpl(null);
      constructor.setSynthetic(true);
      FunctionTypeImpl type = new FunctionTypeImpl(constructor);
      type.setReturnType(interfaceType);
      constructor.setType(type);
      constructors = new ConstructorElement[] {constructor};
    }
    element.setAbstract(node.getAbstractKeyword() != null);
    element.setAccessors(holder.getAccessors());
    element.setConstructors(constructors);
    element.setFields(holder.getFields());
    element.setMethods(holder.getMethods());
    element.setTypeVariables(typeVariables);
    element.setValidMixin(isValidMixin);

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
    element.setTypedef(true);
    TypeVariableElement[] typeVariables = holder.getTypeVariables();
    element.setTypeVariables(typeVariables);

    InterfaceTypeImpl interfaceType = new InterfaceTypeImpl(element);
    interfaceType.setTypeArguments(createTypeVariableTypes(typeVariables));
    element.setType(interfaceType);

    currentHolder.addType(element);
    className.setElement(element);
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

    currentHolder.addConstructor(element);
    node.setElement(element);
    if (constructorName == null) {
      Identifier returnType = node.getReturnType();
      if (returnType != null) {
        element.setNameOffset(returnType.getOffset());
      }
    } else {
      constructorName.setElement(element);
    }
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
    variableName.setElement(element);
    return super.visitDeclaredIdentifier(node);
  }

  @Override
  public Void visitDefaultFormalParameter(DefaultFormalParameter node) {
    ElementHolder holder = new ElementHolder();
    visit(holder, node.getDefaultValue());

    FunctionElementImpl initializer = new FunctionElementImpl();
    initializer.setFunctions(holder.getFunctions());
    initializer.setLabels(holder.getLabels());
    initializer.setLocalVariables(holder.getLocalVariables());
    initializer.setParameters(holder.getParameters());

    SimpleIdentifier parameterName = node.getParameter().getIdentifier();
    ParameterElementImpl parameter;
    if (node.isConst()) {
      parameter = new ConstParameterElementImpl(parameterName);
      parameter.setConst(true);
    } else if (node.getParameter() instanceof FieldFormalParameter) {
      parameter = new FieldFormalParameterElementImpl(parameterName);
    } else {
      parameter = new ParameterElementImpl(parameterName);
    }
    parameter.setFinal(node.isFinal());
    parameter.setInitializer(initializer);
    parameter.setParameterKind(node.getKind());
    FunctionBody body = getFunctionBody(node);
    if (body != null) {
      parameter.setVisibleRange(body.getOffset(), body.getLength());
    }

    currentHolder.addParameter(parameter);
    parameterName.setElement(parameter);
    node.getParameter().accept(this);
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
    if (!(node.getParent() instanceof DefaultFormalParameter)) {
      SimpleIdentifier parameterName = node.getIdentifier();
      FieldFormalParameterElementImpl parameter = new FieldFormalParameterElementImpl(parameterName);
      parameter.setConst(node.isConst());
      parameter.setInitializingFormal(true);
      parameter.setFinal(node.isFinal());
      parameter.setParameterKind(node.getKind());

      currentHolder.addParameter(parameter);
      parameterName.setElement(parameter);
    }
    return super.visitFieldFormalParameter(node);
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

      Token property = node.getPropertyKeyword();
      if (property == null) {
        SimpleIdentifier functionName = node.getName();
        FunctionElementImpl element = new FunctionElementImpl(functionName);
        element.setFunctions(holder.getFunctions());
        element.setLabels(holder.getLabels());
        element.setLocalVariables(holder.getLocalVariables());
        element.setParameters(holder.getParameters());

        FunctionTypeImpl type = new FunctionTypeImpl(element);
        element.setType(type);

        currentHolder.addFunction(element);
        expression.setElement(element);
        functionName.setElement(element);
      } else {
        SimpleIdentifier propertyNameNode = node.getName();
        if (propertyNameNode == null) {
          // TODO(brianwilkerson) Report this internal error.
          return null;
        }
        String propertyName = propertyNameNode.getName();
        FieldElementImpl field = (FieldElementImpl) currentHolder.getField(propertyName);
        if (field == null) {
          field = new FieldElementImpl(node.getName().getName());
          field.setFinal(true);

          currentHolder.addField(field);
        }
        if (matches(property, Keyword.GET)) {
          PropertyAccessorElementImpl getter = new PropertyAccessorElementImpl(propertyNameNode);
          getter.setFunctions(holder.getFunctions());
          getter.setLabels(holder.getLabels());
          getter.setLocalVariables(holder.getLocalVariables());

          getter.setVariable(field);
          getter.setGetter(true);
          field.setGetter(getter);

          currentHolder.addAccessor(getter);
          propertyNameNode.setElement(getter);
        } else {
          PropertyAccessorElementImpl setter = new PropertyAccessorElementImpl(propertyNameNode);
          setter.setFunctions(holder.getFunctions());
          setter.setLabels(holder.getLabels());
          setter.setLocalVariables(holder.getLocalVariables());
          setter.setParameters(holder.getParameters());

          setter.setVariable(field);
          setter.setSetter(true);
          field.setSetter(setter);
          field.setFinal(false);

          currentHolder.addAccessor(setter);
          propertyNameNode.setElement(setter);
        }
      }
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

    FunctionElementImpl element = new FunctionElementImpl(node.getBeginToken().getOffset());
    element.setFunctions(holder.getFunctions());
    element.setLabels(holder.getLabels());
    element.setLocalVariables(holder.getLocalVariables());
    element.setParameters(holder.getParameters());
    if (inFunction) {
      Block enclosingBlock = node.getAncestor(Block.class);
      if (enclosingBlock != null) {
        int functionEnd = node.getOffset() + node.getLength();
        int blockEnd = enclosingBlock.getOffset() + enclosingBlock.getLength();
        element.setVisibleRange(functionEnd, blockEnd - functionEnd - 1);
      }
    }

    FunctionTypeImpl type = new FunctionTypeImpl(element);
    element.setType(type);

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
    TypeVariableElement[] typeVariables = holder.getTypeVariables();
    FunctionTypeAliasElementImpl element = new FunctionTypeAliasElementImpl(aliasName);
    element.setParameters(parameters);
    element.setTypeVariables(typeVariables);

    FunctionTypeImpl type = new FunctionTypeImpl(element);
    type.setTypeArguments(createTypeVariableTypes(typeVariables));
    element.setType(type);

    currentHolder.addTypeAlias(element);
    aliasName.setElement(element);
    return null;
  }

  @Override
  public Void visitFunctionTypedFormalParameter(FunctionTypedFormalParameter node) {
    if (!(node.getParent() instanceof DefaultFormalParameter)) {
      SimpleIdentifier parameterName = node.getIdentifier();
      ParameterElementImpl parameter = new ParameterElementImpl(parameterName);
      parameter.setParameterKind(node.getKind());

      currentHolder.addParameter(parameter);
      parameterName.setElement(parameter);
    }
    //
    // The children of this parameter include any parameters defined on the type of this parameter.
    //
    ElementHolder holder = new ElementHolder();
    visitChildren(holder, node);
    ((ParameterElementImpl) node.getElement()).setParameters(holder.getParameters());
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
    return super.visitLabeledStatement(node);
  }

  @Override
  public Void visitMethodDeclaration(MethodDeclaration node) {
    ElementHolder holder = new ElementHolder();
    boolean wasInFunction = inFunction;
    inFunction = true;
    try {
      visitChildren(holder, node);
    } finally {
      inFunction = wasInFunction;
    }

    Token property = node.getPropertyKeyword();
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
      element.setStatic(node.isStatic());

      currentHolder.addMethod(element);
      methodName.setElement(element);
    } else {
      SimpleIdentifier propertyNameNode = node.getName();
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
        getter.setFunctions(holder.getFunctions());
        getter.setLabels(holder.getLabels());
        getter.setLocalVariables(holder.getLocalVariables());

        getter.setVariable(field);
        getter.setGetter(true);
        field.setGetter(getter);

        currentHolder.addAccessor(getter);
        propertyNameNode.setElement(getter);
      } else {
        PropertyAccessorElementImpl setter = new PropertyAccessorElementImpl(propertyNameNode);
        setter.setFunctions(holder.getFunctions());
        setter.setLabels(holder.getLabels());
        setter.setLocalVariables(holder.getLocalVariables());
        setter.setParameters(holder.getParameters());

        setter.setVariable(field);
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
    if (!(node.getParent() instanceof DefaultFormalParameter)) {
      SimpleIdentifier parameterName = node.getIdentifier();
      ParameterElementImpl parameter = new ParameterElementImpl(parameterName);
      parameter.setConst(node.isConst());
      parameter.setFinal(node.isFinal());
      parameter.setParameterKind(node.getKind());

      currentHolder.addParameter(parameter);
      parameterName.setElement(parameter);
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
      labelName.setElement(element);
    }
    return super.visitSwitchCase(node);
  }

  @Override
  public Void visitSwitchDefault(SwitchDefault node) {
    for (Label label : node.getLabels()) {
      SimpleIdentifier labelName = label.getLabel();
      LabelElementImpl element = new LabelElementImpl(labelName, false, true);

      currentHolder.addLabel(element);
      labelName.setElement(element);
    }
    return super.visitSwitchDefault(node);
  }

  @Override
  public Void visitTypeParameter(TypeParameter node) {
    SimpleIdentifier parameterName = node.getName();
    TypeVariableElementImpl element = new TypeVariableElementImpl(parameterName);

    TypeVariableTypeImpl type = new TypeVariableTypeImpl(element);
    element.setType(type);

    currentHolder.addTypeVariable(element);
    parameterName.setElement(element);
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
      fieldName.setElement(field);
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
      variableName.setElement(element);
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
      variableName.setElement(element);
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
      FunctionElementImpl initializer = new FunctionElementImpl();
      initializer.setFunctions(holder.getFunctions());
      initializer.setLabels(holder.getLabels());
      initializer.setLocalVariables(holder.getLocalVariables());
      initializer.setSynthetic(true);
      element.setInitializer(initializer);
    }
    if (element instanceof PropertyInducingElementImpl) {
      PropertyInducingElementImpl variable = (PropertyInducingElementImpl) element;
      PropertyAccessorElementImpl getter = new PropertyAccessorElementImpl(variable);
      getter.setGetter(true);

      currentHolder.addAccessor(getter);
      variable.setGetter(getter);

      if (!isFinal) {
        PropertyAccessorElementImpl setter = new PropertyAccessorElementImpl(variable);
        setter.setSetter(true);

        currentHolder.addAccessor(setter);
        variable.setSetter(setter);
      }
      if (inFieldContext) {
        ((FieldElementImpl) variable).setStatic(matches(
            ((FieldDeclaration) node.getParent().getParent()).getKeyword(),
            Keyword.STATIC));
      }
    }
    return super.visitVariableDeclaration(node);
  }

  private Type[] createTypeVariableTypes(TypeVariableElement[] typeVariables) {
    int typeVariableCount = typeVariables.length;
    Type[] typeArguments = new Type[typeVariableCount];
    for (int i = 0; i < typeVariableCount; i++) {
      TypeVariableElementImpl typeVariable = (TypeVariableElementImpl) typeVariables[i];
      TypeVariableTypeImpl typeArgument = new TypeVariableTypeImpl(typeVariable);
      typeVariable.setType(typeArgument);
      typeArguments[i] = typeArgument;
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
    ASTNode parent = node.getParent();
    while (parent != null) {
      if (parent instanceof FunctionExpression) {
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
   * Make the given holder be the current holder while visiting the given node.
   * 
   * @param holder the holder that will gather elements that are built while visiting the children
   * @param node the node to be visited
   */
  private void visit(ElementHolder holder, ASTNode node) {
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
