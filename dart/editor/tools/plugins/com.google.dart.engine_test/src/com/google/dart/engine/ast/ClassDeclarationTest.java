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
package com.google.dart.engine.ast;

import com.google.dart.engine.parser.ParserTestCase;

import static com.google.dart.engine.ast.AstFactory.classDeclaration;
import static com.google.dart.engine.ast.AstFactory.constructorDeclaration;
import static com.google.dart.engine.ast.AstFactory.fieldDeclaration;
import static com.google.dart.engine.ast.AstFactory.formalParameterList;
import static com.google.dart.engine.ast.AstFactory.identifier;
import static com.google.dart.engine.ast.AstFactory.methodDeclaration;
import static com.google.dart.engine.ast.AstFactory.variableDeclaration;

import java.util.ArrayList;

public class ClassDeclarationTest extends ParserTestCase {
  public void test_getConstructor() throws Exception {
    ArrayList<ConstructorInitializer> initializers = new ArrayList<ConstructorInitializer>();
    ConstructorDeclaration defaultConstructor = constructorDeclaration(
        identifier("Test"),
        null,
        formalParameterList(),
        initializers);
    ConstructorDeclaration aConstructor = constructorDeclaration(
        identifier("Test"),
        "a",
        formalParameterList(),
        initializers);
    ConstructorDeclaration bConstructor = constructorDeclaration(
        identifier("Test"),
        "b",
        formalParameterList(),
        initializers);
    ClassDeclaration clazz = classDeclaration(
        null,
        "Test",
        null,
        null,
        null,
        null,
        defaultConstructor,
        aConstructor,
        bConstructor);
    assertSame(defaultConstructor, clazz.getConstructor(null));
    assertSame(aConstructor, clazz.getConstructor("a"));
    assertSame(bConstructor, clazz.getConstructor("b"));
    assertSame(null, clazz.getConstructor("noSuchConstructor"));
  }

  public void test_getField() throws Exception {
    VariableDeclaration aVar = variableDeclaration("a");
    VariableDeclaration bVar = variableDeclaration("b");
    VariableDeclaration cVar = variableDeclaration("c");
    ClassDeclaration clazz = classDeclaration(
        null,
        "Test",
        null,
        null,
        null,
        null,
        fieldDeclaration(false, null, aVar),
        fieldDeclaration(false, null, bVar, cVar));
    assertSame(aVar, clazz.getField("a"));
    assertSame(bVar, clazz.getField("b"));
    assertSame(cVar, clazz.getField("c"));
    assertSame(null, clazz.getField("noSuchField"));
  }

  public void test_getMethod() throws Exception {
    MethodDeclaration aMethod = methodDeclaration(
        null,
        null,
        null,
        null,
        identifier("a"),
        formalParameterList());
    MethodDeclaration bMethod = methodDeclaration(
        null,
        null,
        null,
        null,
        identifier("b"),
        formalParameterList());
    ClassDeclaration clazz = classDeclaration(
        null,
        "Test",
        null,
        null,
        null,
        null,
        aMethod,
        bMethod);
    assertSame(aMethod, clazz.getMethod("a"));
    assertSame(bMethod, clazz.getMethod("b"));
    assertSame(null, clazz.getMethod("noSuchMethod"));
  }
}
