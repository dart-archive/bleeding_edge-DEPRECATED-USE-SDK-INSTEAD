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

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.TopLevelVariableDeclaration;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.internal.element.VariableElementImpl;
import com.google.dart.engine.resolver.ResolverTestCase;
import com.google.dart.engine.source.Source;

public class ConstantValueComputerTest extends ResolverTestCase {
  public void test_computeValues_cycle() throws Exception {
    Source librarySource = addSource("/test.dart", createSource(//
        "const int a = c;",
        "const int b = a;",
        "const int c = b;"));
    LibraryElement libraryElement = resolve(librarySource);
    CompilationUnit unit = getAnalysisContext().resolveCompilationUnit(
        librarySource,
        libraryElement);
    assertNotNull(unit);

    ConstantValueComputer computer = new ConstantValueComputer();
    computer.add(unit);
    computer.computeValues();
    NodeList<CompilationUnitMember> members = unit.getDeclarations();
    assertSize(3, members);
    validate(false, ((TopLevelVariableDeclaration) members.get(0)).getVariables());
    validate(false, ((TopLevelVariableDeclaration) members.get(1)).getVariables());
    validate(false, ((TopLevelVariableDeclaration) members.get(2)).getVariables());
  }

  public void test_computeValues_dependentVariables() throws Exception {
    Source librarySource = addSource("/test.dart", createSource(//
        "const int b = a;",
        "const int a = 0;"));
    LibraryElement libraryElement = resolve(librarySource);
    CompilationUnit unit = getAnalysisContext().resolveCompilationUnit(
        librarySource,
        libraryElement);
    assertNotNull(unit);

    ConstantValueComputer computer = new ConstantValueComputer();
    computer.add(unit);
    computer.computeValues();
    NodeList<CompilationUnitMember> members = unit.getDeclarations();
    assertSize(2, members);
    validate(true, ((TopLevelVariableDeclaration) members.get(0)).getVariables());
    validate(true, ((TopLevelVariableDeclaration) members.get(1)).getVariables());
  }

  public void test_computeValues_empty() {
    ConstantValueComputer computer = new ConstantValueComputer();
    computer.computeValues();
  }

  public void test_computeValues_multipleSources() throws Exception {
    Source librarySource = addSource("/lib.dart", createSource(//
        "library lib;",
        "part 'part.dart';",
        "const int c = b;",
        "const int a = 0;"));
    Source partSource = addSource("/part.dart", createSource(//
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

    ConstantValueComputer computer = new ConstantValueComputer();
    computer.add(libraryUnit);
    computer.add(partUnit);
    computer.computeValues();

    NodeList<CompilationUnitMember> libraryMembers = libraryUnit.getDeclarations();
    assertSize(2, libraryMembers);
    validate(true, ((TopLevelVariableDeclaration) libraryMembers.get(0)).getVariables());
    validate(true, ((TopLevelVariableDeclaration) libraryMembers.get(1)).getVariables());

    NodeList<CompilationUnitMember> partMembers = libraryUnit.getDeclarations();
    assertSize(2, partMembers);
    validate(true, ((TopLevelVariableDeclaration) partMembers.get(0)).getVariables());
    validate(true, ((TopLevelVariableDeclaration) partMembers.get(1)).getVariables());
  }

  public void test_computeValues_singleVariable() throws Exception {
    Source librarySource = addSource("/test.dart", "const int a = 0;");
    LibraryElement libraryElement = resolve(librarySource);
    CompilationUnit unit = getAnalysisContext().resolveCompilationUnit(
        librarySource,
        libraryElement);
    assertNotNull(unit);

    ConstantValueComputer computer = new ConstantValueComputer();
    computer.add(unit);
    computer.computeValues();
    NodeList<CompilationUnitMember> members = unit.getDeclarations();
    assertSize(1, members);
    validate(true, ((TopLevelVariableDeclaration) members.get(0)).getVariables());
  }

  private void validate(boolean shouldBeValid, VariableDeclarationList declarationList) {
    for (VariableDeclaration declaration : declarationList.getVariables()) {
      VariableElementImpl element = (VariableElementImpl) declaration.getName().getElement();
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
