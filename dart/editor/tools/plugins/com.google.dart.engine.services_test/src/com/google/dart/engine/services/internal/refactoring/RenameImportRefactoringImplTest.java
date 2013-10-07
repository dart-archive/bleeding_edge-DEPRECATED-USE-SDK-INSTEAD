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

import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.services.status.RefactoringStatusSeverity;

/**
 * Test for {@link RenameImportRefactoringImpl}.
 */
public class RenameImportRefactoringImplTest extends RenameRefactoringImplTest {
  public void test_checkNewName() throws Exception {
    indexTestUnit("import 'dart:async' as test;");
    // configure refactoring
    createRenameImportRefactoring("import 'dart:async");
    // null
    assertRefactoringStatus(
        refactoring.checkNewName(null),
        RefactoringStatusSeverity.ERROR,
        "Import prefix name must not be null.");
    // empty
    refactoring.setNewName("");
    assertRefactoringStatusOK(refactoring.checkNewName(""));
    // same name
    assertRefactoringStatus(
        refactoring.checkNewName("test"),
        RefactoringStatusSeverity.FATAL,
        "Choose another name.");
  }

  public void test_createChange_add() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:async';",
        "main() {",
        "  Future f;",
        "}");
    // configure refactoring
    createRenameImportRefactoring("import 'dart:async");
    assertEquals("Rename Import Prefix", refactoring.getRefactoringName());
    assertEquals("", refactoring.getCurrentName());
    refactoring.setNewName("newName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:async' as newName;",
        "main() {",
        "  newName.Future f;",
        "}");
  }

  public void test_createChange_change() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:math' as test;",
        "import 'dart:async' as test;",
        "main() {",
        "  test.Future f;",
        "}");
    // configure refactoring
    createRenameRefactoring("test.Future");
    assertEquals("Rename Import Prefix", refactoring.getRefactoringName());
    assertEquals("test", refactoring.getCurrentName());
    refactoring.setNewName("newName");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:math' as test;",
        "import 'dart:async' as newName;",
        "main() {",
        "  newName.Future f;",
        "}");
  }

  public void test_createChange_remove() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:math' as test;",
        "import 'dart:async' as test;",
        "main() {",
        "  test.Future f;",
        "}");
    // configure refactoring
    createRenameRefactoring("test.Future");
    assertEquals("Rename Import Prefix", refactoring.getRefactoringName());
    assertEquals("test", refactoring.getCurrentName());
    refactoring.setNewName("");
    // validate change
    assertSuccessfulRename(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:math' as test;",
        "import 'dart:async';",
        "main() {",
        "  Future f;",
        "}");
  }

  private void createRenameImportRefactoring(String search) {
    ImportDirective importDirective = findNode(search, ImportDirective.class);
    createRenameRefactoring(importDirective.getElement());
  }
}
