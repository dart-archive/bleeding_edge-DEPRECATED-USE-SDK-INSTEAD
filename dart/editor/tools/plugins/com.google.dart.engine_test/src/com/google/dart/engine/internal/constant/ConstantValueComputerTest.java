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

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.TopLevelVariableDeclaration;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.internal.element.VariableElementImpl;
import com.google.dart.engine.internal.resolver.TestTypeProvider;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.resolver.ResolverTestCase;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.logging.Logger;
import com.google.dart.engine.utilities.logging.TestLogger;

import java.util.HashMap;
import java.util.HashSet;

public class ConstantValueComputerTest extends ResolverTestCase {
  class ValidatingConstantVisitor extends ConstantVisitor {
    private HashMap<AstNode, HashSet<AstNode>> capturedDependencies;
    private AstNode nodeBeingEvaluated;

    public ValidatingConstantVisitor(TypeProvider typeProvider,
        HashMap<AstNode, HashSet<AstNode>> capturedDependencies, AstNode nodeBeingEvaluated) {
      super(typeProvider);
      this.capturedDependencies = capturedDependencies;
      this.nodeBeingEvaluated = nodeBeingEvaluated;
    }

    @Override
    protected void beforeGetEvaluationResult(AstNode node) {
      super.beforeGetEvaluationResult(node);

      // If we are getting the evaluation result for a node in the graph, make sure we properly
      // recorded the dependency.
      if (capturedDependencies.containsKey(node)) {
        assertTrue(capturedDependencies.get(nodeBeingEvaluated).contains(node));
      }
    }
  }

  private class ValidatingConstantValueComputer extends ConstantValueComputer {
    private HashMap<AstNode, HashSet<AstNode>> capturedDependencies;
    private AstNode nodeBeingEvaluated;

    public ValidatingConstantValueComputer(TypeProvider typeProvider) {
      super(typeProvider);
    }

    @Override
    protected void beforeComputeValue(AstNode constNode) {
      super.beforeComputeValue(constNode);
      nodeBeingEvaluated = constNode;
    }

    @Override
    protected void beforeGraphWalk() {
      super.beforeGraphWalk();

      // Capture the dependency info in referenceGraph so that we can check it later.  (We need
      // to capture it now, before nodes get removed from the graph).
      capturedDependencies = new HashMap<AstNode, HashSet<AstNode>>();
      for (AstNode head : referenceGraph.getNodes()) {
        HashSet<AstNode> tails = new HashSet<AstNode>();
        for (AstNode tail : referenceGraph.getTails(head)) {
          tails.add(tail);
        }
        capturedDependencies.put(head, tails);
      }
    }

    @Override
    protected ConstantVisitor createConstantVisitor() {
      return new ValidatingConstantVisitor(typeProvider, capturedDependencies, nodeBeingEvaluated);
    }

  }

  public void test_computeValues_cycle() throws Exception {
    Logger systemLogger = AnalysisEngine.getInstance().getLogger();
    TestLogger logger = new TestLogger();
    AnalysisEngine.getInstance().setLogger(logger);

    try {
      Source librarySource = addSource(createSource(//
          "const int a = c;",
          "const int b = a;",
          "const int c = b;"));
      LibraryElement libraryElement = resolve(librarySource);
      CompilationUnit unit = getAnalysisContext().resolveCompilationUnit(
          librarySource,
          libraryElement);
      getAnalysisContext().computeErrors(librarySource);
      assertNotNull(unit);

      ConstantValueComputer computer = makeConstantValueComputer();
      computer.add(unit);
      computer.computeValues();
      NodeList<CompilationUnitMember> members = unit.getDeclarations();
      assertSizeOfList(3, members);
      validate(false, ((TopLevelVariableDeclaration) members.get(0)).getVariables());
      validate(false, ((TopLevelVariableDeclaration) members.get(1)).getVariables());
      validate(false, ((TopLevelVariableDeclaration) members.get(2)).getVariables());
      assertEquals(2, logger.getErrorCount());
    } finally {
      AnalysisEngine.getInstance().setLogger(systemLogger);
    }
  }

  public void test_computeValues_dependentVariables() throws Exception {
    Source librarySource = addSource(createSource(//
        "const int b = a;",
        "const int a = 0;"));
    LibraryElement libraryElement = resolve(librarySource);
    CompilationUnit unit = getAnalysisContext().resolveCompilationUnit(
        librarySource,
        libraryElement);
    assertNotNull(unit);

    ConstantValueComputer computer = makeConstantValueComputer();
    computer.add(unit);
    computer.computeValues();
    NodeList<CompilationUnitMember> members = unit.getDeclarations();
    assertSizeOfList(2, members);
    validate(true, ((TopLevelVariableDeclaration) members.get(0)).getVariables());
    validate(true, ((TopLevelVariableDeclaration) members.get(1)).getVariables());
  }

  public void test_computeValues_empty() {
    ConstantValueComputer computer = makeConstantValueComputer();
    computer.computeValues();
  }

  public void test_computeValues_multipleSources() throws Exception {
    Source librarySource = addNamedSource("/lib.dart", createSource(//
        "library lib;",
        "part 'part.dart';",
        "const int c = b;",
        "const int a = 0;"));
    Source partSource = addNamedSource("/part.dart", createSource(//
        "part of lib;",
        "const int b = a;",
        "const int d = c;"));
    LibraryElement libraryElement = resolve(librarySource);
    CompilationUnit libraryUnit = getAnalysisContext().resolveCompilationUnit(
        librarySource,
        libraryElement);
    assertNotNull(libraryUnit);
    CompilationUnit partUnit = getAnalysisContext().resolveCompilationUnit(
        partSource,
        libraryElement);
    assertNotNull(partUnit);

    ConstantValueComputer computer = makeConstantValueComputer();
    computer.add(libraryUnit);
    computer.add(partUnit);
    computer.computeValues();

    NodeList<CompilationUnitMember> libraryMembers = libraryUnit.getDeclarations();
    assertSizeOfList(2, libraryMembers);
    validate(true, ((TopLevelVariableDeclaration) libraryMembers.get(0)).getVariables());
    validate(true, ((TopLevelVariableDeclaration) libraryMembers.get(1)).getVariables());

    NodeList<CompilationUnitMember> partMembers = libraryUnit.getDeclarations();
    assertSizeOfList(2, partMembers);
    validate(true, ((TopLevelVariableDeclaration) partMembers.get(0)).getVariables());
    validate(true, ((TopLevelVariableDeclaration) partMembers.get(1)).getVariables());
  }

  public void test_computeValues_singleVariable() throws Exception {
    Source librarySource = addSource("const int a = 0;");
    LibraryElement libraryElement = resolve(librarySource);
    CompilationUnit unit = getAnalysisContext().resolveCompilationUnit(
        librarySource,
        libraryElement);
    assertNotNull(unit);

    ConstantValueComputer computer = makeConstantValueComputer();
    computer.add(unit);
    computer.computeValues();
    NodeList<CompilationUnitMember> members = unit.getDeclarations();
    assertSizeOfList(1, members);
    validate(true, ((TopLevelVariableDeclaration) members.get(0)).getVariables());
  }

  public void test_dependencyOnVariable() throws Exception {
    // x depends on y
    assertProperDependencies(createSource(//
        "const x = y + 1;",
        "const y = 2;"));
  }

  private void assertProperDependencies(String sourceText, ErrorCode... expectedErrorCodes)
      throws AnalysisException {
    Source source = addSource(sourceText);
    LibraryElement element = resolve(source);
    CompilationUnit unit = getAnalysisContext().resolveCompilationUnit(source, element);
    assertNotNull(unit);
    ConstantValueComputer computer = makeConstantValueComputer();
    computer.add(unit);
    computer.computeValues();
    assertErrors(source, expectedErrorCodes);
  }

  private ConstantValueComputer makeConstantValueComputer() {
    return new ValidatingConstantValueComputer(new TestTypeProvider());
  }

  private void validate(boolean shouldBeValid, VariableDeclarationList declarationList) {
    for (VariableDeclaration declaration : declarationList.getVariables()) {
      VariableElementImpl element = (VariableElementImpl) declaration.getElement();
      assertNotNull(element);
      EvaluationResultImpl result = element.getEvaluationResult();
      if (shouldBeValid) {
        assertInstanceOf(ValidResult.class, result);
        Object value = ((ValidResult) result).getValue();
        assertNotNull(value);
      } else {
        assertInstanceOf(ErrorResult.class, result);
      }
    }
  }
}
