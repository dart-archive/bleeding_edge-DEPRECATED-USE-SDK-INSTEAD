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

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.BlockFunctionBody;
import com.google.dart.engine.ast.CatchClause;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassMember;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.ConstructorInitializer;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.FieldFormalParameter;
import com.google.dart.engine.ast.FormalParameter;
import com.google.dart.engine.ast.FormalParameterList;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.FunctionTypedFormalParameter;
import com.google.dart.engine.ast.Label;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.NamedFormalParameter;
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.TypeAlias;
import com.google.dart.engine.ast.TypeParameter;
import com.google.dart.engine.ast.TypeParameterList;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.LabelElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.TypeAliasElement;
import com.google.dart.engine.element.TypeElement;
import com.google.dart.engine.element.TypeVariableElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.StringToken;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;

import java.util.ArrayList;

public class ElementBuilderTest extends EngineTestCase {
  public void test_visitCatchClause() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String exceptionParameterName = "e";
    String stackParameterName = "s";
    CatchClause clause = new CatchClause(
        null,
        null,
        null,
        null,
        createIdentifier(exceptionParameterName),
        null,
        createIdentifier(stackParameterName),
        null,
        createEmptyBlock());
    clause.accept(builder);
    VariableElement[] variables = holder.getVariables();
    assertLength(2, variables);

    VariableElement exceptionVariable = variables[0];
    assertNotNull(exceptionVariable);
    assertEquals(exceptionParameterName, exceptionVariable.getName());
    assertFalse(exceptionVariable.isSynthetic());
    assertFalse(exceptionVariable.isConst());
    assertFalse(exceptionVariable.isFinal());
    assertNull(exceptionVariable.getInitializer());

    VariableElement stackVariable = variables[1];
    assertNotNull(stackVariable);
    assertEquals(stackParameterName, stackVariable.getName());
    assertFalse(stackVariable.isSynthetic());
    assertFalse(stackVariable.isConst());
    assertFalse(stackVariable.isFinal());
    assertNull(stackVariable.getInitializer());
  }

  public void test_visitClassDeclaration_abstract() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String className = "C";
    ClassDeclaration classDeclaration = new ClassDeclaration(
        null,
        new KeywordToken(Keyword.ABSTRACT, 0),
        null,
        createIdentifier(className),
        null,
        null,
        null,
        null,
        new ArrayList<ClassMember>(),
        null);
    classDeclaration.accept(builder);
    TypeElement[] types = holder.getTypes();
    assertLength(1, types);

    TypeElement type = types[0];
    assertNotNull(type);
    assertEquals(className, type.getName());
    TypeVariableElement[] typeVariables = type.getTypeVariables();
    assertLength(0, typeVariables);
    assertTrue(type.isAbstract());
    assertFalse(type.isSynthetic());
  }

  public void test_visitClassDeclaration_minimal() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String className = "C";
    ClassDeclaration classDeclaration = new ClassDeclaration(
        null,
        null,
        null,
        createIdentifier(className),
        null,
        null,
        null,
        null,
        new ArrayList<ClassMember>(),
        null);
    classDeclaration.accept(builder);
    TypeElement[] types = holder.getTypes();
    assertLength(1, types);

    TypeElement type = types[0];
    assertNotNull(type);
    assertEquals(className, type.getName());
    TypeVariableElement[] typeVariables = type.getTypeVariables();
    assertLength(0, typeVariables);
    assertFalse(type.isAbstract());
    assertFalse(type.isSynthetic());
  }

  public void test_visitClassDeclaration_parameterized() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String className = "C";
    String firstVariableName = "E";
    String secondVariableName = "F";
    ClassDeclaration classDeclaration = new ClassDeclaration(
        null,
        null,
        null,
        createIdentifier(className),
        createTypeParameterList(firstVariableName, secondVariableName),
        null,
        null,
        null,
        new ArrayList<ClassMember>(),
        null);
    classDeclaration.accept(builder);
    TypeElement[] types = holder.getTypes();
    assertLength(1, types);

    TypeElement type = types[0];
    assertNotNull(type);
    assertEquals(className, type.getName());
    TypeVariableElement[] typeVariables = type.getTypeVariables();
    assertLength(2, typeVariables);
    assertEquals(firstVariableName, typeVariables[0].getName());
    assertEquals(secondVariableName, typeVariables[1].getName());
    assertFalse(type.isAbstract());
    assertFalse(type.isSynthetic());
  }

  public void test_visitConstructorDeclaration_factory() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String className = "A";
    ConstructorDeclaration constructorDeclaration = new ConstructorDeclaration(
        null,
        null,
        new KeywordToken(Keyword.FACTORY, 0),
        createIdentifier(className),
        null,
        null,
        createFormalParameterList(),
        null,
        createConstructorInitializerList(),
        createEmptyFunctionBody());
    constructorDeclaration.accept(builder);
    ConstructorElement[] constructors = holder.getConstructors();
    assertLength(1, constructors);

    ConstructorElement constructor = constructors[0];
    assertNotNull(constructor);
    assertTrue(constructor.isFactory());
    assertEquals("", constructor.getName());
    assertLength(0, constructor.getFunctions());
    assertLength(0, constructor.getLabels());
    assertLength(0, constructor.getLocalVariables());
    assertLength(0, constructor.getParameters());
  }

  public void test_visitConstructorDeclaration_minimal() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String className = "A";
    ConstructorDeclaration constructorDeclaration = new ConstructorDeclaration(
        null,
        null,
        null,
        createIdentifier(className),
        null,
        null,
        createFormalParameterList(),
        null,
        createConstructorInitializerList(),
        createEmptyFunctionBody());
    constructorDeclaration.accept(builder);
    ConstructorElement[] constructors = holder.getConstructors();
    assertLength(1, constructors);

    ConstructorElement constructor = constructors[0];
    assertNotNull(constructor);
    assertFalse(constructor.isFactory());
    assertEquals("", constructor.getName());
    assertLength(0, constructor.getFunctions());
    assertLength(0, constructor.getLabels());
    assertLength(0, constructor.getLocalVariables());
    assertLength(0, constructor.getParameters());
  }

  public void test_visitConstructorDeclaration_named() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String className = "A";
    String constructorName = "c";
    ConstructorDeclaration constructorDeclaration = new ConstructorDeclaration(
        null,
        null,
        null,
        createIdentifier(className),
        null,
        createIdentifier(constructorName),
        createFormalParameterList(),
        null,
        createConstructorInitializerList(),
        createEmptyFunctionBody());
    constructorDeclaration.accept(builder);
    ConstructorElement[] constructors = holder.getConstructors();
    assertLength(1, constructors);

    ConstructorElement constructor = constructors[0];
    assertNotNull(constructor);
    assertFalse(constructor.isFactory());
    assertEquals(constructorName, constructor.getName());
    assertLength(0, constructor.getFunctions());
    assertLength(0, constructor.getLabels());
    assertLength(0, constructor.getLocalVariables());
    assertLength(0, constructor.getParameters());
  }

  public void test_visitFieldDeclaration() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String firstFieldName = "x";
    String secondFieldName = "y";
    FieldDeclaration fieldDeclaration = new FieldDeclaration(
        null,
        null,
        createVariableDeclarationList(null, new VariableDeclaration(
            null,
            createIdentifier(firstFieldName),
            null,
            null), new VariableDeclaration(null, createIdentifier(secondFieldName), null, null)),
        null);
    fieldDeclaration.accept(builder);
    FieldElement[] fields = holder.getFields();
    assertLength(2, fields);

    FieldElement firstField = fields[0];
    assertNotNull(firstField);
    assertEquals(firstFieldName, firstField.getName());
    assertNull(firstField.getInitializer());
    assertFalse(firstField.isConst());
    assertFalse(firstField.isFinal());
    assertFalse(firstField.isSynthetic());

    FieldElement secondField = fields[1];
    assertNotNull(secondField);
    assertEquals(secondFieldName, secondField.getName());
    assertNull(secondField.getInitializer());
    assertFalse(secondField.isConst());
    assertFalse(secondField.isFinal());
    assertFalse(secondField.isSynthetic());
  }

  public void test_visitFieldFormalParameter() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String parameterName = "p";
    FieldFormalParameter formalParameter = new FieldFormalParameter(
        null,
        null,
        null,
        null,
        createIdentifier(parameterName));
    formalParameter.accept(builder);
    VariableElement[] parameters = holder.getVariables();
    assertLength(1, parameters);

    VariableElement parameter = parameters[0];
    assertNotNull(parameter);
    assertEquals(parameterName, parameter.getName());
    assertNull(parameter.getInitializer());
    assertFalse(parameter.isConst());
    assertFalse(parameter.isFinal());
    assertFalse(parameter.isSynthetic());
  }

  public void test_visitFormalParameterList() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String firstParameterName = "a";
    String secondParameterName = "b";
    FormalParameterList parameterList = createFormalParameterList(new SimpleFormalParameter(
        null,
        null,
        createIdentifier(firstParameterName)), new SimpleFormalParameter(
        null,
        null,
        createIdentifier(secondParameterName)));
    parameterList.accept(builder);
    VariableElement[] parameters = holder.getParameters();
    assertLength(2, parameters);

    assertEquals(firstParameterName, parameters[0].getName());

    assertEquals(secondParameterName, parameters[1].getName());
  }

  public void test_visitFunctionExpression() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String functionName = "f";
    FunctionExpression expression = new FunctionExpression(
        null,
        createIdentifier(functionName),
        createFormalParameterList(),
        createEmptyFunctionBody());
    expression.accept(builder);
    FunctionElement[] functions = holder.getFunctions();
    assertLength(1, functions);
    FunctionElement function = functions[0];

    assertNotNull(function);
    assertEquals(functionName, function.getName());
    assertFalse(function.isSynthetic());
  }

  public void test_visitFunctionTypedFormalParameter() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String parameterName = "p";
    FunctionTypedFormalParameter formalParameter = new FunctionTypedFormalParameter(
        null,
        createIdentifier(parameterName),
        createFormalParameterList());
    formalParameter.accept(builder);
    VariableElement[] parameters = holder.getVariables();
    assertLength(1, parameters);

    VariableElement parameter = parameters[0];
    assertNotNull(parameter);
    assertEquals(parameterName, parameter.getName());
    assertNull(parameter.getInitializer());
    assertFalse(parameter.isConst());
    assertFalse(parameter.isFinal());
    assertFalse(parameter.isSynthetic());
  }

  public void test_visitLabel() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String labelName = "l";
    Label labelDeclaration = new Label(createIdentifier(labelName), null);
    labelDeclaration.accept(builder);
    LabelElement[] labels = holder.getLabels();
    assertLength(1, labels);

    LabelElement label = labels[0];
    assertNotNull(label);
    assertEquals(labelName, label.getName());
    assertFalse(label.isSynthetic());
  }

  public void test_visitMethodDeclaration_abstract() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String methodName = "m";
    MethodDeclaration methodDeclaration = new MethodDeclaration(
        null,
        null,
        new KeywordToken(Keyword.ABSTRACT, 0),
        null,
        null,
        null,
        createIdentifier(methodName),
        createFormalParameterList(),
        createEmptyFunctionBody());
    methodDeclaration.accept(builder);
    MethodElement[] methods = holder.getMethods();
    assertLength(1, methods);

    MethodElement method = methods[0];
    assertNotNull(method);
    assertEquals(methodName, method.getName());
    assertLength(0, method.getFunctions());
    assertLength(0, method.getLabels());
    assertLength(0, method.getLocalVariables());
    assertLength(0, method.getParameters());
    assertTrue(method.isAbstract());
    assertFalse(method.isStatic());
    assertFalse(method.isSynthetic());
  }

  public void test_visitMethodDeclaration_getter() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String methodName = "m";
    MethodDeclaration methodDeclaration = new MethodDeclaration(
        null,
        null,
        null,
        null,
        new KeywordToken(Keyword.GET, 0),
        null,
        createIdentifier(methodName),
        createFormalParameterList(),
        createEmptyFunctionBody());
    methodDeclaration.accept(builder);
    FieldElement[] fields = holder.getFields();
    assertLength(1, fields);

    FieldElement field = fields[0];
    assertNotNull(field);
    assertEquals(methodName, field.getName());
    assertTrue(field.isSynthetic());
    assertNull(field.getSetter());
    PropertyAccessorElement getter = field.getGetter();
    assertNotNull(getter);
    assertTrue(getter.isGetter());
    assertFalse(getter.isSynthetic());
    assertEquals(methodName, getter.getName());
    assertEquals(field, getter.getField());
    assertLength(0, getter.getFunctions());
    assertLength(0, getter.getLabels());
    assertLength(0, getter.getLocalVariables());
    assertLength(0, getter.getParameters());
  }

  public void test_visitMethodDeclaration_minimal() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String methodName = "m";
    MethodDeclaration methodDeclaration = new MethodDeclaration(
        null,
        null,
        null,
        null,
        null,
        null,
        createIdentifier(methodName),
        createFormalParameterList(),
        createEmptyFunctionBody());
    methodDeclaration.accept(builder);
    MethodElement[] methods = holder.getMethods();
    assertLength(1, methods);

    MethodElement method = methods[0];
    assertNotNull(method);
    assertEquals(methodName, method.getName());
    assertLength(0, method.getFunctions());
    assertLength(0, method.getLabels());
    assertLength(0, method.getLocalVariables());
    assertLength(0, method.getParameters());
    assertFalse(method.isAbstract());
    assertFalse(method.isStatic());
    assertFalse(method.isSynthetic());
  }

  public void test_visitMethodDeclaration_operator() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String methodName = "+";
    MethodDeclaration methodDeclaration = new MethodDeclaration(
        null,
        null,
        null,
        null,
        null,
        new KeywordToken(Keyword.OPERATOR, 0),
        createIdentifier(methodName),
        createFormalParameterList(),
        createEmptyFunctionBody());
    methodDeclaration.accept(builder);
    MethodElement[] methods = holder.getMethods();
    assertLength(1, methods);

    MethodElement method = methods[0];
    assertNotNull(method);
    assertEquals(methodName, method.getName());
    assertLength(0, method.getFunctions());
    assertLength(0, method.getLabels());
    assertLength(0, method.getLocalVariables());
    assertLength(0, method.getParameters());
    assertFalse(method.isAbstract());
    assertFalse(method.isStatic());
    assertFalse(method.isSynthetic());
  }

  public void test_visitMethodDeclaration_setter() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String methodName = "m";
    MethodDeclaration methodDeclaration = new MethodDeclaration(
        null,
        null,
        null,
        null,
        new KeywordToken(Keyword.SET, 0),
        null,
        createIdentifier(methodName),
        createFormalParameterList(),
        createEmptyFunctionBody());
    methodDeclaration.accept(builder);
    FieldElement[] fields = holder.getFields();
    assertLength(1, fields);

    FieldElement field = fields[0];
    assertNotNull(field);
    assertEquals(methodName, field.getName());
    assertTrue(field.isSynthetic());
    assertNull(field.getGetter());
    PropertyAccessorElement setter = field.getSetter();
    assertNotNull(setter);
    assertTrue(setter.isSetter());
    assertFalse(setter.isSynthetic());
    assertEquals(methodName, setter.getName());
    assertEquals(field, setter.getField());
    assertLength(0, setter.getFunctions());
    assertLength(0, setter.getLabels());
    assertLength(0, setter.getLocalVariables());
    assertLength(0, setter.getParameters());
  }

  public void test_visitMethodDeclaration_static() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String methodName = "m";
    MethodDeclaration methodDeclaration = new MethodDeclaration(
        null,
        null,
        new KeywordToken(Keyword.STATIC, 0),
        null,
        null,
        null,
        createIdentifier(methodName),
        createFormalParameterList(),
        createEmptyFunctionBody());
    methodDeclaration.accept(builder);
    MethodElement[] methods = holder.getMethods();
    assertLength(1, methods);

    MethodElement method = methods[0];
    assertNotNull(method);
    assertEquals(methodName, method.getName());
    assertLength(0, method.getFunctions());
    assertLength(0, method.getLabels());
    assertLength(0, method.getLocalVariables());
    assertLength(0, method.getParameters());
    assertFalse(method.isAbstract());
    assertTrue(method.isStatic());
    assertFalse(method.isSynthetic());
  }

  public void test_visitNamedFormalParameter() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String parameterName = "p";
    NamedFormalParameter formalParameter = new NamedFormalParameter(new SimpleFormalParameter(
        null,
        null,
        createIdentifier(parameterName)), null, createIdentifier("b"));
    formalParameter.accept(builder);
    VariableElement[] parameters = holder.getVariables();
    assertLength(1, parameters);

    VariableElement parameter = parameters[0];
    assertNotNull(parameter);
    assertEquals(parameterName, parameter.getName());
    assertFalse(parameter.isConst());
    assertFalse(parameter.isFinal());
    assertFalse(parameter.isSynthetic());
    FunctionElement initializer = parameter.getInitializer();
    assertNotNull(initializer);
    assertTrue(initializer.isSynthetic());
  }

  public void test_visitSimpleFormalParameter() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String parameterName = "p";
    SimpleFormalParameter formalParameter = new SimpleFormalParameter(
        null,
        null,
        createIdentifier(parameterName));
    formalParameter.accept(builder);
    VariableElement[] parameters = holder.getVariables();
    assertLength(1, parameters);

    VariableElement parameter = parameters[0];
    assertNotNull(parameter);
    assertEquals(parameterName, parameter.getName());
    assertNull(parameter.getInitializer());
    assertFalse(parameter.isConst());
    assertFalse(parameter.isFinal());
    assertFalse(parameter.isSynthetic());
  }

  public void test_visitTypeAlias_minimal() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String aliasName = "F";
    TypeAlias typeAlias = new TypeAlias(
        null,
        null,
        null,
        createIdentifier(aliasName),
        null,
        null,
        null);
    typeAlias.accept(builder);
    TypeAliasElement[] aliases = holder.getTypeAliases();
    assertLength(1, aliases);

    TypeAliasElement alias = aliases[0];
    assertNotNull(alias);
    assertEquals(aliasName, alias.getName());
    assertFalse(alias.isSynthetic());
  }

  public void test_visitTypeAlias_withFormalParameters() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String aliasName = "F";
    String firstParameterName = "x";
    String secondParameterName = "y";
    TypeAlias typeAlias = new TypeAlias(
        null,
        null,
        null,
        createIdentifier(aliasName),
        createTypeParameterList(),
        createFormalParameterList(
            createParameter(firstParameterName),
            createParameter(secondParameterName)), null);
    typeAlias.accept(builder);
    TypeAliasElement[] aliases = holder.getTypeAliases();
    assertLength(1, aliases);

    TypeAliasElement alias = aliases[0];
    assertNotNull(alias);
    assertEquals(aliasName, alias.getName());
    assertFalse(alias.isSynthetic());
    VariableElement[] parameters = alias.getParameters();
    assertLength(2, parameters);
    assertEquals(firstParameterName, parameters[0].getName());
    assertEquals(secondParameterName, parameters[1].getName());
    TypeVariableElement[] typeVariables = alias.getTypeVariables();
    assertNotNull(typeVariables);
    assertLength(0, typeVariables);
  }

  public void test_visitTypeAlias_withTypeParameters() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String aliasName = "F";
    String firstTypeParameterName = "A";
    String secondTypeParameterName = "B";
    TypeAlias typeAlias = new TypeAlias(
        null,
        null,
        null,
        createIdentifier(aliasName),
        createTypeParameterList(firstTypeParameterName, secondTypeParameterName),
        createFormalParameterList(),
        null);
    typeAlias.accept(builder);
    TypeAliasElement[] aliases = holder.getTypeAliases();
    assertLength(1, aliases);

    TypeAliasElement alias = aliases[0];
    assertNotNull(alias);
    assertEquals(aliasName, alias.getName());
    assertFalse(alias.isSynthetic());
    VariableElement[] parameters = alias.getParameters();
    assertNotNull(parameters);
    assertLength(0, parameters);
    TypeVariableElement[] typeVariables = alias.getTypeVariables();
    assertLength(2, typeVariables);
    assertEquals(firstTypeParameterName, typeVariables[0].getName());
    assertEquals(secondTypeParameterName, typeVariables[1].getName());
  }

  public void test_visitTypeParameter() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String parameterName = "E";
    TypeParameter typeParameter = new TypeParameter(createIdentifier(parameterName), null, null);
    typeParameter.accept(builder);
    TypeVariableElement[] typeVariables = holder.getTypeVariables();
    assertLength(1, typeVariables);

    TypeVariableElement typeVariable = typeVariables[0];
    assertNotNull(typeVariable);
    assertEquals(parameterName, typeVariable.getName());
    assertFalse(typeVariable.isSynthetic());
  }

  public void test_visitVariableDeclaration_noInitializer() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String variableName = "v";
    VariableDeclaration variableDeclaration = new VariableDeclaration(
        null,
        createIdentifier(variableName),
        null,
        null);
    createVariableDeclarationList(null, variableDeclaration);
    variableDeclaration.accept(builder);
    VariableElement[] variables = holder.getVariables();
    assertLength(1, variables);

    VariableElement variable = variables[0];
    assertNotNull(variable);
    assertNull(variable.getInitializer());
    assertEquals(variableName, variable.getName());
    assertFalse(variable.isConst());
    assertFalse(variable.isFinal());
    assertFalse(variable.isSynthetic());
  }

  private ArrayList<ConstructorInitializer> createConstructorInitializerList(
      ConstructorInitializer... initializers) {
    ArrayList<ConstructorInitializer> initializerList = new ArrayList<ConstructorInitializer>();
    for (ConstructorInitializer initializer : initializers) {
      initializerList.add(initializer);
    }
    return initializerList;
  }

  private Block createEmptyBlock() {
    return new Block(null, new ArrayList<Statement>(), null);
  }

  private BlockFunctionBody createEmptyFunctionBody() {
    return new BlockFunctionBody(createEmptyBlock());
  }

  private FormalParameterList createFormalParameterList(FormalParameter... parameters) {
    ArrayList<FormalParameter> parameterList = new ArrayList<FormalParameter>();
    for (FormalParameter parameter : parameters) {
      parameterList.add(parameter);
    }
    return new FormalParameterList(null, parameterList, null, null, null);
  }

  private SimpleIdentifier createIdentifier(String name) {
    return new SimpleIdentifier(new StringToken(TokenType.IDENTIFIER, name, 0));
  }

  private FormalParameter createParameter(String parameterName) {
    return new SimpleFormalParameter(null, null, createIdentifier(parameterName));
  }

  private TypeParameterList createTypeParameterList(String... typeNames) {
    ArrayList<TypeParameter> typeParameters = new ArrayList<TypeParameter>();
    for (String typeName : typeNames) {
      typeParameters.add(new TypeParameter(createIdentifier(typeName), null, null));
    }
    return new TypeParameterList(null, typeParameters, null);
  }

  private VariableDeclarationList createVariableDeclarationList(Token keyword,
      VariableDeclaration... variables) {
    ArrayList<VariableDeclaration> variableList = new ArrayList<VariableDeclaration>();
    for (VariableDeclaration variable : variables) {
      variableList.add(variable);
    }
    return new VariableDeclarationList(keyword, null, variableList);
  }
}
