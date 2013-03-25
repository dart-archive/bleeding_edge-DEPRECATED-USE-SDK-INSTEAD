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
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.scanner.Keyword;

import static com.google.dart.engine.ast.ASTFactory.integer;
import static com.google.dart.engine.ast.ASTFactory.variableDeclaration;
import static com.google.dart.engine.ast.ASTFactory.variableDeclarationList;
import static com.google.dart.engine.element.ElementFactory.localVariableElement;

import java.util.HashMap;

public class ConstantFinderTest extends EngineTestCase {
  public void test_visitVariableDeclaration_const() {
    VariableDeclaration declaration = variableDeclaration("v", integer(0));
    SimpleIdentifier name = declaration.getName();
    VariableElement element = localVariableElement(name);
    name.setElement(element);
    variableDeclarationList(Keyword.CONST, declaration);
    ConstantFinder finder = new ConstantFinder();
    declaration.accept(finder);
    HashMap<VariableElement, VariableDeclaration> variableMap = finder.getVariableMap();
    assertNotNull(variableMap);
    assertSame(declaration, variableMap.get(element));
  }

  public void test_visitVariableDeclaration_noInitializer() {
    VariableDeclaration declaration = variableDeclaration("v");
    SimpleIdentifier name = declaration.getName();
    VariableElement element = localVariableElement(name);
    name.setElement(element);
    variableDeclarationList(Keyword.CONST, declaration);
    ConstantFinder finder = new ConstantFinder();
    declaration.accept(finder);
    HashMap<VariableElement, VariableDeclaration> variableMap = finder.getVariableMap();
    assertNotNull(variableMap);
    assertTrue(variableMap.isEmpty());
  }

  public void test_visitVariableDeclaration_nonConst() {
    VariableDeclaration declaration = variableDeclaration("v", integer(0));
    SimpleIdentifier name = declaration.getName();
    VariableElement element = localVariableElement(name);
    name.setElement(element);
    variableDeclarationList(null, declaration);
    ConstantFinder finder = new ConstantFinder();
    declaration.accept(finder);
    HashMap<VariableElement, VariableDeclaration> variableMap = finder.getVariableMap();
    assertNotNull(variableMap);
    assertTrue(variableMap.isEmpty());
  }
}
