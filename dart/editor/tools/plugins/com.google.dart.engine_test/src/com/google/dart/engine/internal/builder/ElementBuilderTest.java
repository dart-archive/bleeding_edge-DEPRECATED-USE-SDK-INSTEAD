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
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.DefaultFormalParameter;
import com.google.dart.engine.ast.EnumDeclaration;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.FieldFormalParameter;
import com.google.dart.engine.ast.FormalParameter;
import com.google.dart.engine.ast.FormalParameterList;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.FunctionTypedFormalParameter;
import com.google.dart.engine.ast.LabeledStatement;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.TypeAlias;
import com.google.dart.engine.ast.TypeParameter;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.LabelElement;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.PropertyInducingElement;
import com.google.dart.engine.element.TopLevelVariableElement;
import com.google.dart.engine.element.TypeParameterElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.utilities.dart.ParameterKind;

import static com.google.dart.engine.ast.AstFactory.block;
import static com.google.dart.engine.ast.AstFactory.blockFunctionBody;
import static com.google.dart.engine.ast.AstFactory.breakStatement;
import static com.google.dart.engine.ast.AstFactory.catchClause;
import static com.google.dart.engine.ast.AstFactory.classDeclaration;
import static com.google.dart.engine.ast.AstFactory.constructorDeclaration;
import static com.google.dart.engine.ast.AstFactory.emptyFunctionBody;
import static com.google.dart.engine.ast.AstFactory.enumDeclaration;
import static com.google.dart.engine.ast.AstFactory.fieldDeclaration;
import static com.google.dart.engine.ast.AstFactory.fieldFormalParameter;
import static com.google.dart.engine.ast.AstFactory.formalParameterList;
import static com.google.dart.engine.ast.AstFactory.functionDeclaration;
import static com.google.dart.engine.ast.AstFactory.functionExpression;
import static com.google.dart.engine.ast.AstFactory.functionTypedFormalParameter;
import static com.google.dart.engine.ast.AstFactory.identifier;
import static com.google.dart.engine.ast.AstFactory.label;
import static com.google.dart.engine.ast.AstFactory.labeledStatement;
import static com.google.dart.engine.ast.AstFactory.list;
import static com.google.dart.engine.ast.AstFactory.methodDeclaration;
import static com.google.dart.engine.ast.AstFactory.namedFormalParameter;
import static com.google.dart.engine.ast.AstFactory.returnStatement;
import static com.google.dart.engine.ast.AstFactory.simpleFormalParameter;
import static com.google.dart.engine.ast.AstFactory.tryStatement;
import static com.google.dart.engine.ast.AstFactory.typeAlias;
import static com.google.dart.engine.ast.AstFactory.typeParameter;
import static com.google.dart.engine.ast.AstFactory.typeParameterList;
import static com.google.dart.engine.ast.AstFactory.variableDeclaration;
import static com.google.dart.engine.ast.AstFactory.variableDeclarationList;
import static com.google.dart.engine.ast.AstFactory.variableDeclarationStatement;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeStartEnd;

public class ElementBuilderTest extends EngineTestCase {
  public void test_visitCatchClause() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String exceptionParameterName = "e";
    String stackParameterName = "s";
    CatchClause clause = catchClause(exceptionParameterName, stackParameterName);
    clause.accept(builder);
    LocalVariableElement[] variables = holder.getLocalVariables();
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
    ClassDeclaration classDeclaration = classDeclaration(
        Keyword.ABSTRACT,
        className,
        null,
        null,
        null,
        null);
    classDeclaration.accept(builder);
    ClassElement[] types = holder.getTypes();
    assertLength(1, types);

    ClassElement type = types[0];
    assertNotNull(type);
    assertEquals(className, type.getName());
    TypeParameterElement[] typeParameters = type.getTypeParameters();
    assertLength(0, typeParameters);
    assertTrue(type.isAbstract());
    assertFalse(type.isSynthetic());
  }

  public void test_visitClassDeclaration_minimal() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String className = "C";
    ClassDeclaration classDeclaration = classDeclaration(null, className, null, null, null, null);
    classDeclaration.accept(builder);
    ClassElement[] types = holder.getTypes();
    assertLength(1, types);

    ClassElement type = types[0];
    assertNotNull(type);
    assertEquals(className, type.getName());
    TypeParameterElement[] typeParameters = type.getTypeParameters();
    assertLength(0, typeParameters);
    assertFalse(type.isAbstract());
    assertFalse(type.isSynthetic());
  }

  public void test_visitClassDeclaration_parameterized() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String className = "C";
    String firstVariableName = "E";
    String secondVariableName = "F";
    ClassDeclaration classDeclaration = classDeclaration(
        null,
        className,
        typeParameterList(firstVariableName, secondVariableName),
        null,
        null,
        null);
    classDeclaration.accept(builder);
    ClassElement[] types = holder.getTypes();
    assertLength(1, types);

    ClassElement type = types[0];
    assertNotNull(type);
    assertEquals(className, type.getName());
    TypeParameterElement[] typeParameters = type.getTypeParameters();
    assertLength(2, typeParameters);
    assertEquals(firstVariableName, typeParameters[0].getName());
    assertEquals(secondVariableName, typeParameters[1].getName());
    assertFalse(type.isAbstract());
    assertFalse(type.isSynthetic());
  }

  public void test_visitClassDeclaration_withMembers() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String className = "C";
    String typeParameterName = "E";
    String fieldName = "f";
    String methodName = "m";
    ClassDeclaration classDeclaration = classDeclaration(
        null,
        className,
        typeParameterList(typeParameterName),
        null,
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
    ClassElement[] types = holder.getTypes();
    assertLength(1, types);

    ClassElement type = types[0];
    assertNotNull(type);
    assertEquals(className, type.getName());
    assertFalse(type.isAbstract());
    assertFalse(type.isSynthetic());

    TypeParameterElement[] typeParameters = type.getTypeParameters();
    assertLength(1, typeParameters);
    TypeParameterElement typeParameter = typeParameters[0];
    assertNotNull(typeParameter);
    assertEquals(typeParameterName, typeParameter.getName());

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
    ElementBuilder builder = new ElementBuilder(holder);
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
    ElementBuilder builder = new ElementBuilder(holder);
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
    ElementBuilder builder = new ElementBuilder(holder);
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
    assertSame(constructor, constructorDeclaration.getName().getStaticElement());
    assertSame(constructor, constructorDeclaration.getElement());
  }

  public void test_visitConstructorDeclaration_unnamed() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
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
    assertSame(constructor, constructorDeclaration.getElement());
  }

  public void test_visitEnumDeclaration() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String enumName = "E";
    EnumDeclaration enumDeclaration = enumDeclaration(enumName, "ONE");
    enumDeclaration.accept(builder);
    ClassElement[] enums = holder.getEnums();
    assertLength(1, enums);

    ClassElement enumElement = enums[0];
    assertNotNull(enumElement);
    assertEquals(enumName, enumElement.getName());
  }

  public void test_visitFieldDeclaration() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
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
    ElementBuilder builder = new ElementBuilder(holder);
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
    assertLength(0, parameter.getParameters());
  }

  public void test_visitFieldFormalParameter_funtionTyped() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String parameterName = "p";
    FieldFormalParameter formalParameter = fieldFormalParameter(
        null,
        null,
        parameterName,
        formalParameterList(simpleFormalParameter("a")));
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
    assertLength(1, parameter.getParameters());
  }

  public void test_visitFormalParameterList() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
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

  public void test_visitFunctionDeclaration_getter() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String functionName = "f";
    FunctionDeclaration declaration = functionDeclaration(
        null,
        Keyword.GET,
        functionName,
        functionExpression(formalParameterList(), blockFunctionBody()));
    declaration.accept(builder);
    PropertyAccessorElement[] accessors = holder.getAccessors();
    assertLength(1, accessors);
    PropertyAccessorElement accessor = accessors[0];

    assertNotNull(accessor);
    assertEquals(functionName, accessor.getName());
    assertSame(accessor, declaration.getElement());
    assertSame(accessor, declaration.getFunctionExpression().getElement());
    assertTrue(accessor.isGetter());
    assertFalse(accessor.isSetter());
    assertFalse(accessor.isSynthetic());
    PropertyInducingElement variable = accessor.getVariable();
    assertInstanceOf(TopLevelVariableElement.class, variable);
    assertTrue(variable.isSynthetic());
  }

  public void test_visitFunctionDeclaration_plain() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
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
    assertSame(function, declaration.getElement());
    assertSame(function, declaration.getFunctionExpression().getElement());
    assertFalse(function.isSynthetic());
  }

  public void test_visitFunctionDeclaration_setter() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String functionName = "f";
    FunctionDeclaration declaration = functionDeclaration(
        null,
        Keyword.SET,
        functionName,
        functionExpression(formalParameterList(), blockFunctionBody()));
    declaration.accept(builder);
    PropertyAccessorElement[] accessors = holder.getAccessors();
    assertLength(1, accessors);
    PropertyAccessorElement accessor = accessors[0];

    assertNotNull(accessor);
    assertEquals(functionName + "=", accessor.getName());
    assertSame(accessor, declaration.getElement());
    assertSame(accessor, declaration.getFunctionExpression().getElement());
    assertFalse(accessor.isGetter());
    assertTrue(accessor.isSetter());
    assertFalse(accessor.isSynthetic());
    PropertyInducingElement variable = accessor.getVariable();
    assertInstanceOf(TopLevelVariableElement.class, variable);
    assertTrue(variable.isSynthetic());
  }

  public void test_visitFunctionExpression() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    FunctionExpression expression = functionExpression(formalParameterList(), blockFunctionBody());
    expression.accept(builder);
    FunctionElement[] functions = holder.getFunctions();
    assertLength(1, functions);
    FunctionElement function = functions[0];

    assertNotNull(function);
    assertSame(function, expression.getElement());
    assertFalse(function.isSynthetic());
  }

  public void test_visitFunctionTypeAlias() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String aliasName = "F";
    String parameterName = "E";
    FunctionTypeAlias aliasNode = typeAlias(null, aliasName, typeParameterList(parameterName), null);
    aliasNode.accept(builder);
    FunctionTypeAliasElement[] aliases = holder.getTypeAliases();
    assertLength(1, aliases);

    FunctionTypeAliasElement alias = aliases[0];
    assertNotNull(alias);
    assertEquals(aliasName, alias.getName());
    assertLength(0, alias.getParameters());
    TypeParameterElement[] typeParameters = alias.getTypeParameters();
    assertLength(1, typeParameters);

    TypeParameterElement typeParameter = typeParameters[0];
    assertNotNull(typeParameter);
    assertEquals(parameterName, typeParameter.getName());
  }

  public void test_visitFunctionTypedFormalParameter() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String parameterName = "p";
    FunctionTypedFormalParameter formalParameter = functionTypedFormalParameter(null, parameterName);
    useParameterInMethod(formalParameter, 100, 110);

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
    assertEquals(rangeStartEnd(100, 110), parameter.getVisibleRange());
  }

  public void test_visitLabeledStatement() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
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
    ElementBuilder builder = new ElementBuilder(holder);
    String methodName = "m";
    MethodDeclaration methodDeclaration = methodDeclaration(
        null,
        null,
        null,
        null,
        identifier(methodName),
        formalParameterList(),
        emptyFunctionBody());
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
    assertFalse(getter.isAbstract());
    assertTrue(getter.isGetter());
    assertFalse(getter.isSynthetic());
    assertEquals(methodName, getter.getName());
    assertEquals(field, getter.getVariable());
    assertLength(0, getter.getFunctions());
    assertLength(0, getter.getLabels());
    assertLength(0, getter.getLocalVariables());
    assertLength(0, getter.getParameters());
  }

  public void test_visitMethodDeclaration_getter_abstract() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String methodName = "m";
    MethodDeclaration methodDeclaration = methodDeclaration(
        null,
        null,
        Keyword.GET,
        null,
        identifier(methodName),
        formalParameterList(),
        emptyFunctionBody());
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
    assertTrue(getter.isAbstract());
    assertTrue(getter.isGetter());
    assertFalse(getter.isSynthetic());
    assertEquals(methodName, getter.getName());
    assertEquals(field, getter.getVariable());
    assertLength(0, getter.getFunctions());
    assertLength(0, getter.getLabels());
    assertLength(0, getter.getLocalVariables());
    assertLength(0, getter.getParameters());
  }

  public void test_visitMethodDeclaration_getter_external() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String methodName = "m";
    MethodDeclaration methodDeclaration = methodDeclaration(
        null,
        null,
        Keyword.GET,
        null,
        identifier(methodName),
        formalParameterList());
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
    assertFalse(getter.isAbstract());
    assertTrue(getter.isGetter());
    assertFalse(getter.isSynthetic());
    assertEquals(methodName, getter.getName());
    assertEquals(field, getter.getVariable());
    assertLength(0, getter.getFunctions());
    assertLength(0, getter.getLabels());
    assertLength(0, getter.getLocalVariables());
    assertLength(0, getter.getParameters());
  }

  public void test_visitMethodDeclaration_minimal() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
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
    ElementBuilder builder = new ElementBuilder(holder);
    String methodName = "+";
    MethodDeclaration methodDeclaration = methodDeclaration(
        null,
        null,
        null,
        Keyword.OPERATOR,
        identifier(methodName),
        formalParameterList(simpleFormalParameter("addend")),
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
    assertLength(1, method.getParameters());
    assertFalse(method.isAbstract());
    assertFalse(method.isStatic());
    assertFalse(method.isSynthetic());
  }

  public void test_visitMethodDeclaration_setter() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
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
    assertFalse(setter.isAbstract());
    assertTrue(setter.isSetter());
    assertFalse(setter.isSynthetic());
    assertEquals(methodName + '=', setter.getName());
    assertEquals(methodName, setter.getDisplayName());
    assertEquals(field, setter.getVariable());
    assertLength(0, setter.getFunctions());
    assertLength(0, setter.getLabels());
    assertLength(0, setter.getLocalVariables());
    assertLength(0, setter.getParameters());
  }

  public void test_visitMethodDeclaration_setter_abstract() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String methodName = "m";
    MethodDeclaration methodDeclaration = methodDeclaration(
        null,
        null,
        Keyword.SET,
        null,
        identifier(methodName),
        formalParameterList(),
        emptyFunctionBody());
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
    assertTrue(setter.isAbstract());
    assertTrue(setter.isSetter());
    assertFalse(setter.isSynthetic());
    assertEquals(methodName + '=', setter.getName());
    assertEquals(methodName, setter.getDisplayName());
    assertEquals(field, setter.getVariable());
    assertLength(0, setter.getFunctions());
    assertLength(0, setter.getLabels());
    assertLength(0, setter.getLocalVariables());
    assertLength(0, setter.getParameters());
  }

  public void test_visitMethodDeclaration_setter_external() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String methodName = "m";
    MethodDeclaration methodDeclaration = methodDeclaration(
        null,
        null,
        Keyword.SET,
        null,
        identifier(methodName),
        formalParameterList());
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
    assertFalse(setter.isAbstract());
    assertTrue(setter.isSetter());
    assertFalse(setter.isSynthetic());
    assertEquals(methodName + '=', setter.getName());
    assertEquals(methodName, setter.getDisplayName());
    assertEquals(field, setter.getVariable());
    assertLength(0, setter.getFunctions());
    assertLength(0, setter.getLabels());
    assertLength(0, setter.getLocalVariables());
    assertLength(0, setter.getParameters());
  }

  public void test_visitMethodDeclaration_static() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
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
    ElementBuilder builder = new ElementBuilder(holder);
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
    ElementBuilder builder = new ElementBuilder(holder);
    String parameterName = "p";
    DefaultFormalParameter formalParameter = namedFormalParameter(
        simpleFormalParameter(parameterName),
        identifier("42"));
    useParameterInMethod(formalParameter, 100, 110);

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
    assertEquals(rangeStartEnd(100, 110), parameter.getVisibleRange());
    assertEquals("42", parameter.getDefaultValueCode());

    FunctionElement initializer = parameter.getInitializer();
    assertNotNull(initializer);
    assertTrue(initializer.isSynthetic());
  }

  public void test_visitSimpleFormalParameter() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String parameterName = "p";
    SimpleFormalParameter formalParameter = simpleFormalParameter(parameterName);
    useParameterInMethod(formalParameter, 100, 110);

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
    assertEquals(rangeStartEnd(100, 110), parameter.getVisibleRange());
  }

  public void test_visitTypeAlias_minimal() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String aliasName = "F";
    TypeAlias typeAlias = typeAlias(null, aliasName, null, null);
    typeAlias.accept(builder);
    FunctionTypeAliasElement[] aliases = holder.getTypeAliases();
    assertLength(1, aliases);

    FunctionTypeAliasElement alias = aliases[0];
    assertNotNull(alias);
    assertEquals(aliasName, alias.getName());
    assertNotNull(alias.getType());
    assertFalse(alias.isSynthetic());
  }

  public void test_visitTypeAlias_withFormalParameters() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
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
    FunctionTypeAliasElement[] aliases = holder.getTypeAliases();
    assertLength(1, aliases);

    FunctionTypeAliasElement alias = aliases[0];
    assertNotNull(alias);
    assertEquals(aliasName, alias.getName());
    assertNotNull(alias.getType());
    assertFalse(alias.isSynthetic());
    VariableElement[] parameters = alias.getParameters();
    assertLength(2, parameters);
    assertEquals(firstParameterName, parameters[0].getName());
    assertEquals(secondParameterName, parameters[1].getName());
    TypeParameterElement[] typeParameters = alias.getTypeParameters();
    assertNotNull(typeParameters);
    assertLength(0, typeParameters);
  }

  public void test_visitTypeAlias_withTypeParameters() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String aliasName = "F";
    String firstTypeParameterName = "A";
    String secondTypeParameterName = "B";
    TypeAlias typeAlias = typeAlias(
        null,
        aliasName,
        typeParameterList(firstTypeParameterName, secondTypeParameterName),
        formalParameterList());
    typeAlias.accept(builder);
    FunctionTypeAliasElement[] aliases = holder.getTypeAliases();
    assertLength(1, aliases);

    FunctionTypeAliasElement alias = aliases[0];
    assertNotNull(alias);
    assertEquals(aliasName, alias.getName());
    assertNotNull(alias.getType());
    assertFalse(alias.isSynthetic());
    VariableElement[] parameters = alias.getParameters();
    assertNotNull(parameters);
    assertLength(0, parameters);
    TypeParameterElement[] typeParameters = alias.getTypeParameters();
    assertLength(2, typeParameters);
    assertEquals(firstTypeParameterName, typeParameters[0].getName());
    assertEquals(secondTypeParameterName, typeParameters[1].getName());
  }

  public void test_visitTypeParameter() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String parameterName = "E";
    TypeParameter typeParameter = typeParameter(parameterName);
    typeParameter.accept(builder);
    TypeParameterElement[] typeParameters = holder.getTypeParameters();
    assertLength(1, typeParameters);

    TypeParameterElement typeParameterElement = typeParameters[0];
    assertNotNull(typeParameterElement);
    assertEquals(parameterName, typeParameterElement.getName());
    assertNull(typeParameterElement.getBound());
    assertFalse(typeParameterElement.isSynthetic());
  }

  public void test_visitVariableDeclaration_inConstructor() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    //
    // C() {var v;}
    //
    String variableName = "v";
    VariableDeclaration variable = variableDeclaration(variableName, null);
    Statement statement = variableDeclarationStatement(null, variable);
    ConstructorDeclaration constructor = constructorDeclaration(
        null,
        null,
        identifier("C"),
        "C",
        formalParameterList(),
        null,
        blockFunctionBody(statement));
    constructor.accept(builder);

    ConstructorElement[] constructors = holder.getConstructors();
    assertLength(1, constructors);
    LocalVariableElement[] variableElements = constructors[0].getLocalVariables();
    assertLength(1, variableElements);
    LocalVariableElement variableElement = variableElements[0];
    assertEquals(variableName, variableElement.getName());
  }

  public void test_visitVariableDeclaration_inMethod() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    //
    // m() {var v;}
    //
    String variableName = "v";
    VariableDeclaration variable = variableDeclaration(variableName, null);
    Statement statement = variableDeclarationStatement(null, variable);
    MethodDeclaration constructor = methodDeclaration(
        null,
        null,
        null,
        null,
        identifier("m"),
        formalParameterList(),
        blockFunctionBody(statement));
    constructor.accept(builder);

    MethodElement[] methods = holder.getMethods();
    assertLength(1, methods);
    LocalVariableElement[] variableElements = methods[0].getLocalVariables();
    assertLength(1, variableElements);
    LocalVariableElement variableElement = variableElements[0];
    assertEquals(variableName, variableElement.getName());
  }

  public void test_visitVariableDeclaration_localNestedInField() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    //
    // var f = () {var v;}
    //
    String variableName = "v";
    VariableDeclaration variable = variableDeclaration(variableName, null);
    Statement statement = variableDeclarationStatement(null, variable);
    Expression initializer = functionExpression(formalParameterList(), blockFunctionBody(statement));

    String fieldName = "f";
    VariableDeclaration field = variableDeclaration(fieldName, initializer);
    FieldDeclaration fieldDeclaration = fieldDeclaration(false, null, field);
    fieldDeclaration.accept(builder);

    FieldElement[] variables = holder.getFields();
    assertLength(1, variables);
    FieldElement fieldElement = variables[0];
    assertNotNull(fieldElement);
    FunctionElement initializerElement = fieldElement.getInitializer();
    assertNotNull(initializerElement);
    FunctionElement[] functionElements = initializerElement.getFunctions();
    assertLength(1, functionElements);
    LocalVariableElement[] variableElements = functionElements[0].getLocalVariables();
    assertLength(1, variableElements);
    LocalVariableElement variableElement = variableElements[0];
    assertEquals(variableName, variableElement.getName());
    assertFalse(variableElement.isConst());
    assertFalse(variableElement.isFinal());
    assertFalse(variableElement.isSynthetic());
  }

  public void test_visitVariableDeclaration_noInitializer() {
    ElementHolder holder = new ElementHolder();
    ElementBuilder builder = new ElementBuilder(holder);
    String variableName = "v";
    VariableDeclaration variableDeclaration = variableDeclaration(variableName, null);
    variableDeclarationList(null, variableDeclaration);
    variableDeclaration.accept(builder);
    TopLevelVariableElement[] variables = holder.getTopLevelVariables();
    assertLength(1, variables);

    TopLevelVariableElement variable = variables[0];
    assertNotNull(variable);
    assertNull(variable.getInitializer());
    assertEquals(variableName, variable.getName());
    assertFalse(variable.isConst());
    assertFalse(variable.isFinal());
    assertFalse(variable.isSynthetic());
    assertNotNull(variable.getGetter());
    assertNotNull(variable.getSetter());
  }

  private void useParameterInMethod(FormalParameter formalParameter, int blockOffset, int blockEnd) {
    Block block = block();
    block.getLeftBracket().setOffset(blockOffset);
    block.getRightBracket().setOffset(blockEnd - 1);
    BlockFunctionBody body = blockFunctionBody(block);
    methodDeclaration(
        null,
        null,
        null,
        null,
        identifier("main"),
        formalParameterList(formalParameter),
        body);
  }
}
