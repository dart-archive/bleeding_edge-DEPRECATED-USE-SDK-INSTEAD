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
import com.google.dart.engine.ast.ConstructorInitializer;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SuperConstructorInvocation;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.ConstructorElementImpl;
import com.google.dart.engine.internal.element.VariableElementImpl;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.utilities.collection.DirectedGraph;

import static com.google.dart.engine.ast.AstFactory.constructorDeclaration;
import static com.google.dart.engine.ast.AstFactory.formalParameterList;
import static com.google.dart.engine.ast.AstFactory.identifier;
import static com.google.dart.engine.ast.AstFactory.instanceCreationExpression;
import static com.google.dart.engine.ast.AstFactory.superConstructorInvocation;
import static com.google.dart.engine.ast.AstFactory.typeName;
import static com.google.dart.engine.ast.AstFactory.variableDeclaration;
import static com.google.dart.engine.ast.AstFactory.variableDeclarationList;
import static com.google.dart.engine.element.ElementFactory.classElement;
import static com.google.dart.engine.element.ElementFactory.constructorElement;
import static com.google.dart.engine.element.ElementFactory.localVariableElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class ReferenceFinderTest extends EngineTestCase {
  private DirectedGraph<AstNode> referenceGraph;
  private HashMap<VariableElement, VariableDeclaration> variableDeclarationMap;
  private HashMap<ConstructorElement, ConstructorDeclaration> constructorDeclarationMap;
  private VariableDeclaration head;
  private AstNode tail;

  public void test_visitInstanceCreationExpression_const() {
    visitNode(makeTailConstructor("A", true, true, true));
    assertOneArc(tail);
  }

  public void test_visitInstanceCreationExpression_nonConstDeclaration() {
    // In the source:
    //   const x = const A();
    // x depends on "const A()" even if the A constructor isn't declared as const.
    visitNode(makeTailConstructor("A", false, true, true));
    assertOneArc(tail);
  }

  public void test_visitInstanceCreationExpression_nonConstUsage() {
    visitNode(makeTailConstructor("A", true, false, true));
    assertNoArcs();
  }

  public void test_visitInstanceCreationExpression_notInMap() {
    // In the source:
    //   const x = const A();
    // x depends on "const A()" even if the AST for the A constructor isn't available.
    visitNode(makeTailConstructor("A", true, true, false));
    assertOneArc(tail);
  }

  public void test_visitSimpleIdentifier_const() {
    visitNode(makeTailVariable("v2", true, true));
    assertOneArc(tail);
  }

  public void test_visitSimpleIdentifier_nonConst() {
    visitNode(makeTailVariable("v2", false, true));
    assertNoArcs();
  }

  public void test_visitSimpleIdentifier_notInMap() {
    visitNode(makeTailVariable("v2", true, false));
    assertNoArcs();
  }

  public void test_visitSuperConstructorInvocation_const() {
    visitNode(makeTailSuperConstructorInvocation("A", true, true));
    assertOneArc(tail);
  }

  public void test_visitSuperConstructorInvocation_nonConst() {
    visitNode(makeTailSuperConstructorInvocation("A", false, true));
    assertNoArcs();
  }

  public void test_visitSuperConstructorInvocation_notInMap() {
    visitNode(makeTailSuperConstructorInvocation("A", true, false));
    assertNoArcs();
  }

  public void test_visitSuperConstructorInvocation_unresolved() {
    SuperConstructorInvocation superConstructorInvocation = superConstructorInvocation();
    tail = superConstructorInvocation;
    visitNode(superConstructorInvocation);
    assertNoArcs();
  }

  @Override
  protected void setUp() {
    referenceGraph = new DirectedGraph<AstNode>();
    variableDeclarationMap = new HashMap<VariableElement, VariableDeclaration>();
    constructorDeclarationMap = new HashMap<ConstructorElement, ConstructorDeclaration>();
    head = variableDeclaration("v1");
  }

  private void assertNoArcs() {
    Set<AstNode> tails = referenceGraph.getTails(head);
    assertSizeOfSet(0, tails);
  }

  private void assertOneArc(AstNode tail) {
    Set<AstNode> tails = referenceGraph.getTails(head);
    assertSizeOfSet(1, tails);
    assertSame(tail, tails.iterator().next());
  }

  private ReferenceFinder createReferenceFinder(AstNode source) {
    return new ReferenceFinder(
        source,
        referenceGraph,
        variableDeclarationMap,
        constructorDeclarationMap);
  }

  private InstanceCreationExpression makeTailConstructor(String name, boolean isConstDeclaration,
      boolean isConstUsage, boolean inMap) {
    ArrayList<ConstructorInitializer> initializers = new ArrayList<ConstructorInitializer>();
    ConstructorDeclaration constructorDeclaration = constructorDeclaration(
        identifier(name),
        null,
        formalParameterList(),
        initializers);
    if (isConstDeclaration) {
      constructorDeclaration.setConstKeyword(new KeywordToken(Keyword.CONST, 0));
    }
    ClassElementImpl classElement = classElement(name);
    SimpleIdentifier identifier = identifier(name);
    TypeName type = typeName(identifier);
    InstanceCreationExpression instanceCreationExpression = instanceCreationExpression(isConstUsage
        ? Keyword.CONST : Keyword.NEW, type);
    tail = instanceCreationExpression;
    ConstructorElementImpl constructorElement = constructorElement(
        classElement,
        name,
        isConstDeclaration);
    if (inMap) {
      constructorDeclarationMap.put(constructorElement, constructorDeclaration);
    }
    instanceCreationExpression.setStaticElement(constructorElement);
    return instanceCreationExpression;
  }

  private SuperConstructorInvocation makeTailSuperConstructorInvocation(String name,
      boolean isConst, boolean inMap) {
    ArrayList<ConstructorInitializer> initializers = new ArrayList<ConstructorInitializer>();
    ConstructorDeclaration constructorDeclaration = constructorDeclaration(
        identifier(name),
        null,
        formalParameterList(),
        initializers);
    tail = constructorDeclaration;
    if (isConst) {
      constructorDeclaration.setConstKeyword(new KeywordToken(Keyword.CONST, 0));
    }
    ClassElementImpl classElement = classElement(name);
    SuperConstructorInvocation superConstructorInvocation = superConstructorInvocation();
    ConstructorElementImpl constructorElement = constructorElement(classElement, name, isConst);
    if (inMap) {
      constructorDeclarationMap.put(constructorElement, constructorDeclaration);
    }
    superConstructorInvocation.setStaticElement(constructorElement);
    return superConstructorInvocation;
  }

  private SimpleIdentifier makeTailVariable(String name, boolean isConst, boolean inMap) {
    VariableDeclaration variableDeclaration = variableDeclaration(name);
    tail = variableDeclaration;
    VariableElementImpl variableElement = localVariableElement(name);
    variableElement.setConst(isConst);
    variableDeclarationList(isConst ? Keyword.CONST : Keyword.VAR, variableDeclaration);
    if (inMap) {
      variableDeclarationMap.put(variableElement, variableDeclaration);
    }
    SimpleIdentifier identifier = identifier(name);
    identifier.setStaticElement(variableElement);
    return identifier;
  }

  private void visitNode(AstNode node) {
    node.accept(createReferenceFinder(head));
  }
}
