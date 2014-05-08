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
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.internal.element.VariableElementImpl;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.utilities.collection.DirectedGraph;

import static com.google.dart.engine.ast.AstFactory.identifier;
import static com.google.dart.engine.ast.AstFactory.variableDeclaration;
import static com.google.dart.engine.ast.AstFactory.variableDeclarationList;
import static com.google.dart.engine.element.ElementFactory.localVariableElement;

import java.util.HashMap;
import java.util.Set;

public class ReferenceFinderTest extends EngineTestCase {
  public void test_visitSimpleIdentifier_const() {
    VariableDeclaration head = variableDeclaration("v1");
    VariableDeclaration tail = variableDeclaration("v2");
    VariableElementImpl tailElement = localVariableElement("v2");
    tailElement.setConst(true);
    variableDeclarationList(Keyword.CONST, head, tail);
    DirectedGraph<AstNode> referenceGraph = new DirectedGraph<AstNode>();
    HashMap<VariableElement, VariableDeclaration> declarationMap = new HashMap<VariableElement, VariableDeclaration>();
    declarationMap.put(tailElement, tail);
    ReferenceFinder finder = new ReferenceFinder(head, referenceGraph, declarationMap);
    SimpleIdentifier identifier = identifier("v2");
    identifier.setStaticElement(tailElement);
    identifier.accept(finder);
    Set<AstNode> tails = referenceGraph.getTails(head);
    assertSizeOfSet(1, tails);
    assertSame(tail, tails.iterator().next());
  }

  public void test_visitSimpleIdentifier_nonConst() {
    VariableDeclaration head = variableDeclaration("v1");
    VariableDeclaration tail = variableDeclaration("v2");
    VariableElementImpl tailElement = localVariableElement("v2");
    tailElement.setConst(false);
    variableDeclarationList(Keyword.VAR, head, tail);
    DirectedGraph<AstNode> referenceGraph = new DirectedGraph<AstNode>();
    HashMap<VariableElement, VariableDeclaration> declarationMap = new HashMap<VariableElement, VariableDeclaration>();
    ReferenceFinder finder = new ReferenceFinder(head, referenceGraph, declarationMap);
    SimpleIdentifier identifier = identifier("v2");
    identifier.setStaticElement(tailElement);
    identifier.accept(finder);
    Set<AstNode> tails = referenceGraph.getTails(head);
    assertSizeOfSet(0, tails);
  }

  public void test_visitSimpleIdentifier_notInMap() {
    VariableDeclaration head = variableDeclaration("v1");
    VariableDeclaration tail = variableDeclaration("v2");
    VariableElementImpl tailElement = localVariableElement("v2");
    tailElement.setConst(true);
    variableDeclarationList(Keyword.CONST, head, tail);
    DirectedGraph<AstNode> referenceGraph = new DirectedGraph<AstNode>();
    HashMap<VariableElement, VariableDeclaration> declarationMap = new HashMap<VariableElement, VariableDeclaration>();
    ReferenceFinder finder = new ReferenceFinder(head, referenceGraph, declarationMap);
    SimpleIdentifier identifier = identifier("v2");
    identifier.setStaticElement(tailElement);
    identifier.accept(finder);
    Set<AstNode> tails = referenceGraph.getTails(head);
    assertSizeOfSet(0, tails);
  }
}
