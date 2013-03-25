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
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.internal.element.VariableElementImpl;

import static com.google.dart.engine.ast.ASTFactory.identifier;
import static com.google.dart.engine.element.ElementFactory.localVariableElement;

import java.util.Set;

public class ReferenceFinderTest extends EngineTestCase {
  public void test_visitSimpleIdentifier_const() {
    VariableElementImpl head = localVariableElement("v1");
    VariableElementImpl tail = localVariableElement("v2");
    tail.setConst(true);
    DirectedGraph<VariableElement> referenceGraph = new DirectedGraph<VariableElement>();
    ReferenceFinder finder = new ReferenceFinder(head, referenceGraph);
    SimpleIdentifier identifier = identifier("v2");
    identifier.setElement(tail);
    identifier.accept(finder);
    Set<VariableElement> tails = referenceGraph.getTails(head);
    assertSize(1, tails);
    assertSame(tail, tails.iterator().next());
  }

  public void test_visitSimpleIdentifier_nonConst() {
    VariableElementImpl head = localVariableElement("v1");
    VariableElementImpl tail = localVariableElement("v2");
    DirectedGraph<VariableElement> referenceGraph = new DirectedGraph<VariableElement>();
    ReferenceFinder finder = new ReferenceFinder(head, referenceGraph);
    SimpleIdentifier identifier = identifier("v2");
    identifier.setElement(tail);
    identifier.accept(finder);
    Set<VariableElement> tails = referenceGraph.getTails(head);
    assertSize(0, tails);
  }
}
