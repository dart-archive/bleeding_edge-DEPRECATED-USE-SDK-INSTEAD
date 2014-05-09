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
package com.google.dart.engine.internal.constant;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.scanner.Keyword;

import static com.google.dart.engine.ast.AstFactory.blockFunctionBody;
import static com.google.dart.engine.ast.AstFactory.constructorDeclaration;
import static com.google.dart.engine.ast.AstFactory.formalParameterList;
import static com.google.dart.engine.ast.AstFactory.identifier;
import static com.google.dart.engine.ast.AstFactory.instanceCreationExpression;
import static com.google.dart.engine.ast.AstFactory.integer;
import static com.google.dart.engine.ast.AstFactory.typeName;
import static com.google.dart.engine.ast.AstFactory.variableDeclaration;
import static com.google.dart.engine.ast.AstFactory.variableDeclarationList;
import static com.google.dart.engine.element.ElementFactory.classElement;
import static com.google.dart.engine.element.ElementFactory.constructorElement;
import static com.google.dart.engine.element.ElementFactory.localVariableElement;

import java.util.ArrayList;
import java.util.HashMap;

public class ConstantFinderTest extends EngineTestCase {
  private AstNode node;

  public void test_visitConstructorDeclaration_const() {
    ConstructorElement element = setupConstructorDeclaration("A", true);
    assertSame(node, findConstantDeclarations().get(element));
  }

  public void test_visitConstructorDeclaration_nonConst() {
    setupConstructorDeclaration("A", false);
    assertTrue(findConstantDeclarations().isEmpty());
  }

  public void test_visitInstanceCreationExpression_const() {
    setupInstanceCreationExpression("A", true);
    assertTrue(findConstructorInvocations().contains(node));
  }

  public void test_visitInstanceCreationExpression_nonConst() {
    setupInstanceCreationExpression("A", false);
    assertTrue(findConstructorInvocations().isEmpty());
  }

  public void test_visitVariableDeclaration_const() {
    VariableElement element = setupVariableDeclaration("v", true, true);
    assertSame(node, findVariableDeclarations().get(element));
  }

  public void test_visitVariableDeclaration_noInitializer() {
    setupVariableDeclaration("v", true, false);
    assertTrue(findVariableDeclarations().isEmpty());
  }

  public void test_visitVariableDeclaration_nonConst() {
    setupVariableDeclaration("v", false, true);
    assertTrue(findVariableDeclarations().isEmpty());
  }

  private HashMap<ConstructorElement, ConstructorDeclaration> findConstantDeclarations() {
    ConstantFinder finder = new ConstantFinder();
    node.accept(finder);
    HashMap<ConstructorElement, ConstructorDeclaration> constructorMap = finder.getConstructorMap();
    assertNotNull(constructorMap);
    return constructorMap;
  }

  private ArrayList<InstanceCreationExpression> findConstructorInvocations() {
    ConstantFinder finder = new ConstantFinder();
    node.accept(finder);
    ArrayList<InstanceCreationExpression> constructorInvocations = finder.getConstructorInvocations();
    assertNotNull(constructorInvocations);
    return constructorInvocations;
  }

  private HashMap<VariableElement, VariableDeclaration> findVariableDeclarations() {
    ConstantFinder finder = new ConstantFinder();
    node.accept(finder);
    HashMap<VariableElement, VariableDeclaration> variableMap = finder.getVariableMap();
    assertNotNull(variableMap);
    return variableMap;
  }

  private ConstructorElement setupConstructorDeclaration(String name, boolean isConst) {
    Keyword constKeyword = isConst ? Keyword.CONST : null;
    ConstructorDeclaration constructorDeclaration = constructorDeclaration(
        constKeyword,
        null,
        null,
        name,
        formalParameterList(),
        null,
        blockFunctionBody());
    ClassElement classElement = classElement(name);
    ConstructorElement element = constructorElement(classElement, name, isConst);
    constructorDeclaration.setElement(element);
    node = constructorDeclaration;
    return element;
  }

  private void setupInstanceCreationExpression(String name, boolean isConst) {
    node = instanceCreationExpression(isConst ? Keyword.CONST : null, typeName(identifier(name)));
  }

  private VariableElement setupVariableDeclaration(String name, boolean isConst,
      boolean isInitialized) {
    VariableDeclaration variableDeclaration = isInitialized ? variableDeclaration(name, integer(0))
        : variableDeclaration(name);
    SimpleIdentifier identifier = variableDeclaration.getName();
    VariableElement element = localVariableElement(identifier);
    identifier.setStaticElement(element);
    variableDeclarationList(isConst ? Keyword.CONST : null, variableDeclaration);
    node = variableDeclaration;
    return element;
  }
}
