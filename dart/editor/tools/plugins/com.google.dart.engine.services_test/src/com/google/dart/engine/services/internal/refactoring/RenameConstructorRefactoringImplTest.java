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

package com.google.dart.engine.services.internal.refactoring;

import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.status.RefactoringStatusSeverity;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;

import java.io.File;

/**
 * Test for {@link RenameConstructorRefactoringImpl}.
 */
public class RenameConstructorRefactoringImplTest extends RenameRefactoringImplTest {
  public void test_checkFinalConditions_hasMember_ClassElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  A.test() {}",
        "  A.newName() {} // existing",
        "}");
    createRenameRefactoring("test() {}");
    // check status
    refactoring.setNewName("newName");
    assertRefactoringStatus(
        refactoring.checkFinalConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "Class 'A' already declares constructor with name 'newName'.",
        findRangeIdentifier("newName() {} // existing"));
  }

  public void test_checkFinalConditions_hasMember_MethodElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  A.test() {}",
        "  newName() {} // existing",
        "}");
    createRenameRefactoring("test() {}");
    // check status
    refactoring.setNewName("newName");
    assertRefactoringStatus(
        refactoring.checkFinalConditions(pm),
        RefactoringStatusSeverity.ERROR,
        "Class 'A' already declares method with name 'newName'.",
        findRangeIdentifier("newName() {} // existing"));
  }

  public void test_checkNewName() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  A.test() {} // marker",
        "}");
    createRenameRefactoring("test() {} // marker");
    // null
    assertRefactoringStatus(
        refactoring.checkNewName(null),
        RefactoringStatusSeverity.ERROR,
        "Constructor name must not be null.");
    // empty
    assertRefactoringStatusOK(refactoring.checkNewName(""));
    // same name
    assertRefactoringStatus(
        refactoring.checkNewName("test"),
        RefactoringStatusSeverity.FATAL,
        "Choose another name.");
  }

  public void test_createChange_changeName_redirect() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A implements B {",
        "  A.test() {} // marker",
        "}",
        "class B {",
        "  factory B.named() = A.test;",
        "}",
        "main() {",
        "  new A.test();",
        "}");
    // configure refactoring
    createRenameRefactoring("test() {} // marker");
    assertEquals("Rename Constructor", refactoring.getRefactoringName());
    refactoring.setNewName("newName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A implements B {",
        "  A.newName() {} // marker",
        "}",
        "class B {",
        "  factory B.named() = A.newName;",
        "}",
        "main() {",
        "  new A.newName();",
        "}");
  }

  public void test_createChange_changeName_super() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  A.test() {} // marker",
        "}",
        "class B extends A {",
        "  B() : super.test() {}",
        "}",
        "main() {",
        "  new A.test();",
        "}");
    // configure refactoring
    createRenameRefactoring("test() {} // marker");
    assertEquals("Rename Constructor", refactoring.getRefactoringName());
    refactoring.setNewName("newName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  A.newName() {} // marker",
        "}",
        "class B extends A {",
        "  B() : super.newName() {}",
        "}",
        "main() {",
        "  new A.newName();",
        "}");
  }

  public void test_createChange_classTypeAlias() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A implements B {",
        "  A.test() {}",
        "}",
        "class B = A;",
        "main() {",
        "  new A.test();",
        "  new B.test();",
        "}");
    // configure refactoring
    {
      ConstructorElement constructor = findNode("A.test() {}", ConstructorDeclaration.class).getElement();
      createRenameRefactoring(constructor);
    }
    assertEquals("Rename Constructor", refactoring.getRefactoringName());
    refactoring.setNewName("newName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A implements B {",
        "  A.newName() {}",
        "}",
        "class B = A;",
        "main() {",
        "  new A.newName();",
        "  new B.newName();",
        "}");
  }

  public void test_createChange_multipleUnits() throws Exception {
    testCode = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "library libA;",
        "class A {",
        "  A.test() {} // marker",
        "}",
        "class B extends A {",
        "  B() : super.test() {}",
        "}",
        "main() {",
        "  new A.test();",
        "}");
    Source libA = addSource(testCode);
    Source libB = addSource(
        "/B.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "import 'test.dart';",
            "main() {",
            "  new A.test();",
            "}"));
    indexTestUnit(libA);
    indexUnit(libB);
    Source sourceB = libB;
    // configure refactoring
    createRenameRefactoring("test() {} // marker");
    assertEquals("Rename Constructor", refactoring.getRefactoringName());
    refactoring.setNewName("newName");
    // validate change
    assertRefactoringStatusOK();
    Change refactoringChange = refactoring.createChange(pm);
    assertChangeResult(
        refactoringChange,
        testSource,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library libA;",
            "class A {",
            "  A.newName() {} // marker",
            "}",
            "class B extends A {",
            "  B() : super.newName() {}",
            "}",
            "main() {",
            "  new A.newName();",
            "}"));
    assertChangeResult(
        refactoringChange,
        sourceB,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "import 'test.dart';",
            "main() {",
            "  new A.newName();",
            "}"));
  }

  public void test_createChange_remove() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  A.test() {} // marker",
        "}",
        "class B extends A {",
        "  B() : super.test() {}",
        "}",
        "main() {",
        "  new A.test();",
        "}");
    // configure refactoring
    createRenameRefactoring("test() {} // marker");
    assertEquals("Rename Constructor", refactoring.getRefactoringName());
    refactoring.setNewName("");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  A() {} // marker",
        "}",
        "class B extends A {",
        "  B() : super() {}",
        "}",
        "main() {",
        "  new A();",
        "}");
  }

  public void test_createChange_setName() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  A() {} // marker",
        "}",
        "class B extends A {",
        "  B() : super() {}",
        "}",
        "main() {",
        "  new A();",
        "}");
    // configure refactoring
    {
      ConstructorElement constructor = findNode("A() {}", ConstructorDeclaration.class).getElement();
      createRenameRefactoring(constructor);
    }
    assertEquals("Rename Constructor", refactoring.getRefactoringName());
    refactoring.setNewName("newName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  A.newName() {} // marker",
        "}",
        "class B extends A {",
        "  B() : super.newName() {}",
        "}",
        "main() {",
        "  new A.newName();",
        "}");
  }

  public void test_shouldReportUnsafeRefactoringSource() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  A.myPublic() {}",
        "  A._myPrivate() {}",
        "}");
    Source externalSource = new FileBasedSource(new File("other.dart"));
    // check public
    createRenameRefactoring("myPublic() {}");
    assertTrue(refactoring.shouldReportUnsafeRefactoringSource(analysisContext, testSource));
    assertTrue(refactoring.shouldReportUnsafeRefactoringSource(analysisContext, externalSource));
    assertFalse(refactoring.shouldReportUnsafeRefactoringSource(null, testSource));
    // check private
    createRenameRefactoring("_myPrivate() {}");
    assertTrue(refactoring.shouldReportUnsafeRefactoringSource(analysisContext, testSource));
    assertFalse(refactoring.shouldReportUnsafeRefactoringSource(analysisContext, externalSource));
  }
}
