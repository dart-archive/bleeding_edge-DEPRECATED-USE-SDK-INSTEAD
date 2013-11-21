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

import com.google.dart.engine.ast.LibraryDirective;
import com.google.dart.engine.services.status.RefactoringStatusSeverity;
import com.google.dart.engine.source.Source;

/**
 * Test for {@link RenameLibraryRefactoringImpl}.
 */
public class RenameLibraryRefactoringImplTest extends RenameRefactoringImplTest {
  public void test_checkNewName() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "library my.app;",
        "");
    createRenameRefactoring();
    // null
    assertRefactoringStatus(
        refactoring.checkNewName(null),
        RefactoringStatusSeverity.ERROR,
        "Library name must not be null.");
    // empty
    assertRefactoringStatus(
        refactoring.checkNewName(""),
        RefactoringStatusSeverity.ERROR,
        "Library name must not be blank.");
    // same name
    assertRefactoringStatus(
        refactoring.checkNewName("my.app"),
        RefactoringStatusSeverity.FATAL,
        "Choose another name.");
  }

  public void test_createChange_changeName() throws Exception {
    Source partSource = addSource(
        "/part.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "part of my.app;",
            ""));
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "library my.app;",
        "part 'part.dart';",
        "");
    indexUnit(analysisContext.getResolvedCompilationUnit(partSource, testSource));
    // configure refactoring
    createRenameRefactoring();
    assertEquals("Rename Library", refactoring.getRefactoringName());
    refactoring.setNewName("the.new.name");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "library the.new.name;",
        "part 'part.dart';",
        "");
    assertChangeResult(
        refactoringChange,
        partSource,
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "part of the.new.name;",
            ""));
  }

  public void test_shouldReportUnsafeRefactoringSource() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "library my.app;",
        "");
    createRenameRefactoring();
    // check
    assertTrue(refactoring.shouldReportUnsafeRefactoringSource(analysisContext, testSource));
    assertTrue(refactoring.shouldReportUnsafeRefactoringSource(analysisContext, null));
    assertTrue(refactoring.shouldReportUnsafeRefactoringSource(null, null));
  }

  protected void createRenameRefactoring() {
    LibraryDirective node = findNode(findOffset("library "), LibraryDirective.class);
    createRenameRefactoring(node.getElement());
  }
}
