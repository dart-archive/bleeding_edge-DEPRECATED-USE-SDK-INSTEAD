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
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.CatchClause;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.DefaultFormalParameter;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.FieldFormalParameter;
import com.google.dart.engine.ast.FormalParameterList;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionTypedFormalParameter;
import com.google.dart.engine.ast.LabeledStatement;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.TypeAlias;
import com.google.dart.engine.ast.TypeParameter;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.LabelElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.TypeAliasElement;
import com.google.dart.engine.element.TypeElement;
import com.google.dart.engine.element.TypeVariableElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.utilities.dart.ParameterKind;

import static com.google.dart.engine.ast.ASTFactory.block;
import static com.google.dart.engine.ast.ASTFactory.blockFunctionBody;
import static com.google.dart.engine.ast.ASTFactory.breakStatement;
import static com.google.dart.engine.ast.ASTFactory.catchClause;
import static com.google.dart.engine.ast.ASTFactory.classDeclaration;
import static com.google.dart.engine.ast.ASTFactory.constructorDeclaration;
import static com.google.dart.engine.ast.ASTFactory.fieldDeclaration;
import static com.google.dart.engine.ast.ASTFactory.fieldFormalParameter;
import static com.google.dart.engine.ast.ASTFactory.formalParameterList;
import static com.google.dart.engine.ast.ASTFactory.functionDeclaration;
import static com.google.dart.engine.ast.ASTFactory.functionExpression;
import static com.google.dart.engine.ast.ASTFactory.functionTypedFormalParameter;
import static com.google.dart.engine.ast.ASTFactory.identifier;
import static com.google.dart.engine.ast.ASTFactory.label;
import static com.google.dart.engine.ast.ASTFactory.labeledStatement;
import static com.google.dart.engine.ast.ASTFactory.list;
import static com.google.dart.engine.ast.ASTFactory.methodDeclaration;
import static com.google.dart.engine.ast.ASTFactory.namedFormalParameter;
import static com.google.dart.engine.ast.ASTFactory.returnStatement;
import static com.google.dart.engine.ast.ASTFactory.simpleFormalParameter;
import static com.google.dart.engine.ast.ASTFactory.tryStatement;
import static com.google.dart.engine.ast.ASTFactory.typeAlias;
import static com.google.dart.engine.ast.ASTFactory.typeParameter;
import static com.google.dart.engine.ast.ASTFactory.typeParameterList;
import static com.google.dart.engine.ast.ASTFactory.variableDeclaration;
import static com.google.dart.engine.ast.ASTFactory.variableDeclarationList;
import static com.google.dart.engine.ast.ASTFactory.variableDeclarationStatement;

import java.util.HashMap;

public class ElementBuilderTest extends EngineTestCase {
  public void test_visitCatchClause() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder, new HashMap<ASTNode, Element>());
    String exceptionParameterName = "e";
    String stackParameterName = "s";
    CatchClause clause = catchClause(exceptionParameterName, stackParameterName);
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
    ElementBuilder builder = new ElementBuilder(holder, new HashMap<ASTNode, Element>());
    String className = "C";
    ClassDeclaration classDeclaration = classDeclaration(
        Keyword.ABSTRACT,
        className,
        null,
        null,
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
    ElementBuilder builder = new ElementBuilder(holder, new HashMap<ASTNode, Element>());
    String className = "C";
    ClassDeclaration classDeclaration = classDeclaration(null, className, null, null, null);
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
    ElementBuilder builder = new ElementBuilder(holder, new HashMap<ASTNode, Element>());
    String className = "C";
    String firstVariableName = "E";
    String secondVariableName = "F";
    ClassDeclaration classDeclaration = classDeclaration(
        null,
        className,
        typeParameterList(firstVariableName, secondVariableName),
        null,
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

  public void test_visitClassDeclaration_withMembers() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder, new HashMap<ASTNode, Element>());
    String className = "C";
    String typeVariableName = "E";
    String fieldName = "f";
    String methodName = "m";
    ClassDeclaration classDeclaration = classDeclaration(
        null,
        className,
        typeParameterList(typeVariableName),
        null,
        null,
        fieldDeclaration(false, null, variableDeclaration(fieldName)),
        methodDeclaration(
            null,
            null,
            null,
            null,
            identifier(methodName),
            formalParameterList(),
            blockFunctionBody()));
    classDeclaration.accept(builder);
    TypeElement[] types = holder.getTypes();
    assertLength(1, types);

    TypeElement type = types[0];
    assertNotNull(type);
    assertEquals(className, type.getName());
    assertFalse(type.isAbstract());
    assertFalse(type.isSynthetic());

    TypeVariableElement[] typeVariables = type.getTypeVariables();
    assertLength(1, typeVariables);
    TypeVariableElement typeVariable = typeVariables[0];
    assertNotNull(typeVariable);
    assertEquals(typeVariableName, typeVariable.getName());

    FieldElement[] fields = type.getFields();
    assertLength(1, fields);
    FieldElement field = fields[0];
    assertNotNull(field);
    assertEquals(fieldName, field.getName());

    MethodElement[] methods = type.getMethods();
    assertLength(1, methods);
    MethodElement method = methods[0];
    assertNotNull(method);
    assertEquals(methodName, method.getName());
  }

  public void test_visitConstructorDeclaration_factory() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder, new HashMap<ASTNode, Element>());
    String className = "A";
    ConstructorDeclaration constructorDeclaration = constructorDeclaration(
        null,
        Keyword.FACTORY,
        identifier(className),
        null,
        formalParameterList(),
        null,
        blockFunctionBody());
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
    ElementBuilder builder = new ElementBuilder(holder, new HashMap<ASTNode, Element>());
    String className = "A";
    ConstructorDeclaration constructorDeclaration = constructorDeclaration(
        null,
        null,
        identifier(className),
        null,
        formalParameterList(),
        null,
        blockFunctionBody());
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
    ElementBuilder builder = new ElementBuilder(holder, new HashMap<ASTNode, Element>());
    String className = "A";
    String constructorName = "c";
    ConstructorDeclaration constructorDeclaration = constructorDeclaration(
        null,
        null,
        identifier(className),
        constructorName,
        formalParameterList(),
        null,
        blockFunctionBody());
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
    ElementBuilder builder = new ElementBuilder(holder, new HashMap<ASTNode, Element>());
    String firstFieldName = "x";
    String secondFieldName = "y";
    FieldDeclaration fieldDeclaration = fieldDeclaration(
        false,
        null,
        variableDeclaration(firstFieldName),
        variableDeclaration(secondFieldName));
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
    ElementBuilder builder = new ElementBuilder(holder, new HashMap<ASTNode, Element>());
    String parameterName = "p";
    FieldFormalParameter formalParameter = fieldFormalParameter(null, null, parameterName);
    formalParameter.accept(builder);
    ParameterElement[] parameters = holder.getParameters();
    assertLength(1, parameters);

    ParameterElement parameter = parameters[0];
    assertNotNull(parameter);
    assertEquals(parameterName, parameter.getName());
    assertNull(parameter.getInitializer());
    assertFalse(parameter.isConst());
    assertFalse(parameter.isFinal());
    assertFalse(parameter.isSynthetic());
    assertEquals(ParameterKind.REQUIRED, parameter.getParameterKind());
  }

  public void test_visitFormalParameterList() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder, new HashMap<ASTNode, Element>());
    String firstParameterName = "a";
    String secondParameterName = "b";
    FormalParameterList parameterList = formalParameterList(
        simpleFormalParameter(firstParameterName),
        simpleFormalParameter(secondParameterName));
    parameterList.accept(builder);
    ParameterElement[] parameters = holder.getParameters();
    assertLength(2, parameters);

    assertEquals(firstParameterName, parameters[0].getName());

    assertEquals(secondParameterName, parameters[1].getName());
  }

  public void test_visitFunctionExpression() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder, new HashMap<ASTNode, Element>());
    String functionName = "f";
    FunctionDeclaration declaration = functionDeclaration(
        null,
        null,
        functionName,
        functionExpression(formalParameterList(), blockFunctionBody()));
    declaration.accept(builder);
    FunctionElement[] functions = holder.getFunctions();
    assertLength(1, functions);
    FunctionElement function = functions[0];

    assertNotNull(function);
    assertEquals(functionName, function.getName());
    assertFalse(function.isSynthetic());
  }

  public void test_visitFunctionTypedFormalParameter() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder, new HashMap<ASTNode, Element>());
    String parameterName = "p";
    FunctionTypedFormalParameter formalParameter = functionTypedFormalParameter(null, parameterName);
    formalParameter.accept(builder);
    ParameterElement[] parameters = holder.getParameters();
    assertLength(1, parameters);

    ParameterElement parameter = parameters[0];
    assertNotNull(parameter);
    assertEquals(parameterName, parameter.getName());
    assertNull(parameter.getInitializer());
    assertFalse(parameter.isConst());
    assertFalse(parameter.isFinal());
    assertFalse(parameter.isSynthetic());
    assertEquals(ParameterKind.REQUIRED, parameter.getParameterKind());
  }

  public void test_visitLabeledStatement() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder, new HashMap<ASTNode, Element>());
    String labelName = "l";
    LabeledStatement statement = labeledStatement(list(label(labelName)), breakStatement());
    statement.accept(builder);
    LabelElement[] labels = holder.getLabels();
    assertLength(1, labels);

    LabelElement label = labels[0];
    assertNotNull(label);
    assertEquals(labelName, label.getName());
    assertFalse(label.isSynthetic());
  }

  public void test_visitMethodDeclaration_abstract() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder, new HashMap<ASTNode, Element>());
    String methodName = "m";
    MethodDeclaration methodDeclaration = methodDeclaration(
        Keyword.ABSTRACT,
        null,
        null,
        null,
        identifier(methodName),
        formalParameterList(),
        blockFunctionBody());
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
    ElementBuilder builder = new ElementBuilder(holder, new HashMap<ASTNode, Element>());
    String methodName = "m";
    MethodDeclaration methodDeclaration = methodDeclaration(
        null,
        null,
        Keyword.GET,
        null,
        identifier(methodName),
        formalParameterList(),
        blockFunctionBody());
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
    ElementBuilder builder = new ElementBuilder(holder, new HashMap<ASTNode, Element>());
    String methodName = "m";
    MethodDeclaration methodDeclaration = methodDeclaration(
        null,
        null,
        null,
        null,
        identifier(methodName),
        formalParameterList(),
        blockFunctionBody());
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
    ElementBuilder builder = new ElementBuilder(holder, new HashMap<ASTNode, Element>());
    String methodName = "+";
    MethodDeclaration methodDeclaration = methodDeclaration(
        null,
        null,
        null,
        Keyword.OPERATOR,
        identifier(methodName),
        formalParameterList(),
        blockFunctionBody());
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
    ElementBuilder builder = new ElementBuilder(holder, new HashMap<ASTNode, Element>());
    String methodName = "m";
    MethodDeclaration methodDeclaration = methodDeclaration(
        null,
        null,
        Keyword.SET,
        null,
        identifier(methodName),
        formalParameterList(),
        blockFunctionBody());
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
    ElementBuilder builder = new ElementBuilder(holder, new HashMap<ASTNode, Element>());
    String methodName = "m";
    MethodDeclaration methodDeclaration = methodDeclaration(
        Keyword.STATIC,
        null,
        null,
        null,
        identifier(methodName),
        formalParameterList(),
        blockFunctionBody());
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

  public void test_visitMethodDeclaration_withMembers() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder, new HashMap<ASTNode, Element>());
    String methodName = "m";
    String parameterName = "p";
    String localVariableName = "v";
    String labelName = "l";
    String exceptionParameterName = "e";
    MethodDeclaration methodDeclaration = methodDeclaration(
        null,
        null,
        null,
        null,
        identifier(methodName),
        formalParameterList(simpleFormalParameter(parameterName)),
        blockFunctionBody(
            variableDeclarationStatement(Keyword.VAR, variableDeclaration(localVariableName)),
            tryStatement(
                block(labeledStatement(list(label(labelName)), returnStatement())),
                catchClause(exceptionParameterName))));
    methodDeclaration.accept(builder);
    MethodElement[] methods = holder.getMethods();
    assertLength(1, methods);

    MethodElement method = methods[0];
    assertNotNull(method);
    assertEquals(methodName, method.getName());
    assertFalse(method.isAbstract());
    assertFalse(method.isStatic());
    assertFalse(method.isSynthetic());

    VariableElement[] parameters = method.getParameters();
    assertLength(1, parameters);
    VariableElement parameter = parameters[0];
    assertNotNull(parameter);
    assertEquals(parameterName, parameter.getName());

    VariableElement[] localVariables = method.getLocalVariables();
    assertLength(2, localVariables);
    VariableElement firstVariable = localVariables[0];
    VariableElement secondVariable = localVariables[1];
    assertNotNull(firstVariable);
    assertNotNull(secondVariable);
    assertTrue((firstVariable.getName().equals(localVariableName) && secondVariable.getName().equals(
        exceptionParameterName))
        || (firstVariable.getName().equals(exceptionParameterName) && secondVariable.getName().equals(
            localVariableName)));

    LabelElement[] labels = method.getLabels();
    assertLength(1, labels);
    LabelElement label = labels[0];
    assertNotNull(label);
    assertEquals(labelName, label.getName());
  }

  public void test_visitNamedFormalParameter() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder, new HashMap<ASTNode, Element>());
    String parameterName = "p";
    DefaultFormalParameter formalParameter = namedFormalParameter(
        simpleFormalParameter(parameterName),
        identifier("b"));
    formalParameter.accept(builder);
    ParameterElement[] parameters = holder.getParameters();
    assertLength(1, parameters);

    ParameterElement parameter = parameters[0];
    assertNotNull(parameter);
    assertEquals(parameterName, parameter.getName());
    assertFalse(parameter.isConst());
    assertFalse(parameter.isFinal());
    assertFalse(parameter.isSynthetic());
    assertEquals(ParameterKind.NAMED, parameter.getParameterKind());
    FunctionElement initializer = parameter.getInitializer();
    assertNotNull(initializer);
    assertTrue(initializer.isSynthetic());
  }

  public void test_visitSimpleFormalParameter() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder, new HashMap<ASTNode, Element>());
    String parameterName = "p";
    SimpleFormalParameter formalParameter = simpleFormalParameter(parameterName);
    formalParameter.accept(builder);
    ParameterElement[] parameters = holder.getParameters();
    assertLength(1, parameters);

    ParameterElement parameter = parameters[0];
    assertNotNull(parameter);
    assertEquals(parameterName, parameter.getName());
    assertNull(parameter.getInitializer());
    assertFalse(parameter.isConst());
    assertFalse(parameter.isFinal());
    assertFalse(parameter.isSynthetic());
    assertEquals(ParameterKind.REQUIRED, parameter.getParameterKind());
  }

  public void test_visitTypeAlias_minimal() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder, new HashMap<ASTNode, Element>());
    String aliasName = "F";
    TypeAlias typeAlias = typeAlias(null, aliasName, null, null);
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
    ElementBuilder builder = new ElementBuilder(holder, new HashMap<ASTNode, Element>());
    String aliasName = "F";
    String firstParameterName = "x";
    String secondParameterName = "y";
    TypeAlias typeAlias = typeAlias(
        null,
        aliasName,
        typeParameterList(),
        formalParameterList(
            simpleFormalParameter(firstParameterName),
            simpleFormalParameter(secondParameterName)));
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
    ElementBuilder builder = new ElementBuilder(holder, new HashMap<ASTNode, Element>());
    String aliasName = "F";
    String firstTypeParameterName = "A";
    String secondTypeParameterName = "B";
    TypeAlias typeAlias = typeAlias(
        null,
        aliasName,
        typeParameterList(firstTypeParameterName, secondTypeParameterName),
        formalParameterList());
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
    ElementBuilder builder = new ElementBuilder(holder, new HashMap<ASTNode, Element>());
    String parameterName = "E";
    TypeParameter typeParameter = typeParameter(parameterName);
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
    ElementBuilder builder = new ElementBuilder(holder, new HashMap<ASTNode, Element>());
    String variableName = "v";
    VariableDeclaration variableDeclaration = variableDeclaration(variableName, null);
    variableDeclarationList(null, variableDeclaration);
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
}
