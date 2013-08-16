/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.ui.refactoring;

import com.google.dart.engine.internal.index.AbstractDartTest;
import com.google.dart.tools.core.test.util.TestProject;
import com.google.dart.tools.internal.corext.refactoring.rename.MoveResourceParticipant;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameResourceParticipant;
import com.google.dart.tools.internal.corext.refactoring.util.ReflectionUtils;
import com.google.dart.tools.ui.internal.refactoring.MoveSupport;
import com.google.dart.tools.ui.internal.refactoring.RefactoringUtils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * Test for {@link MoveResourceParticipant}.
 */
public final class MoveResourceParticipantTest extends AbstractDartTest {
  /**
   * Waits for background build and moves the given {@link IFile}.
   */
  private static void buildAndMove(IFolder destination, IFile... files) throws Exception {
    RefactoringUtils.waitReadyForRefactoring(new NullProgressMonitor());
    moveFile(destination, files);
  }

  /**
   * Moves given {@link IFile}.
   */
  private static void moveFile(IFolder destination, IFile... files) throws Exception {
    IProgressMonitor pm = new NullProgressMonitor();
    RefactoringStatus status = new RefactoringStatus();
    // create Refactoring
    Refactoring refactoring = MoveSupport.createMoveRefactoring(status, files, destination);
    // execute Refactoring
    status.merge(refactoring.checkAllConditions(pm));
    Change change = refactoring.createChange(pm);
    change.initializeValidationData(pm);
    new PerformChangeOperation(change).run(pm);
    // all OK
    assertTrue(status.toString(), status.isOK());
  }

  protected TestProject testProject;

  /**
   * Just for coverage of {@link RenameResourceParticipant} accessors.
   */
  public void test_accessors() throws Exception {
    MoveResourceParticipant participant = new MoveResourceParticipant();
    // initialize(Object) requires IFile
    {
      Object notFile = new Object();
      Boolean res = ReflectionUtils.invokeMethod(
          participant,
          "initialize(java.lang.Object)",
          notFile);
      assertFalse(res.booleanValue());
    }
  }

  /**
   * <p>
   * https://code.google.com/p/dart/issues/detail?id=10492
   */
  public void test_importSdk() throws Exception {
    IFolder destination = testProject.createFolder("aaa");
    IFile fileA = setProjectFileContent(
        "A.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "import 'dart:io';",
            ""));
    // do move
    buildAndMove(destination, fileA);
    assertProjectFileContent(
        "aaa/A.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "import 'dart:io';",
            ""));
  }

  /**
   * <p>
   * https://code.google.com/p/dart/issues/detail?id=12492
   */
  public void test_OK_multipleAtOnce() throws Exception {
    IFolder destination = testProject.createFolder("aaa");
    IFile fileA = setProjectFileContent(
        "lib_a.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library lib_a;",
            "class A {}"));
    IFile fileB = setProjectFileContent(
        "lib_b.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library lib_b;",
            "class B {}"));
    setProjectFileContent(
        "main.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "import 'lib_a.dart';",
            "import 'lib_b.dart';",
            ""));
    // do move
    buildAndMove(destination, fileA, fileB);
    assertProjectFileContent(
        "main.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "import 'aaa/lib_a.dart';",
            "import 'aaa/lib_b.dart';",
            ""));
  }

  public void test_OK_noReferences() throws Exception {
    IFolder destination = testProject.createFolder("aaa");
    IFile fileA = setProjectFileContent(
        "A.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library a;",
            ""));
    // do move
    buildAndMove(destination, fileA);
    assertProjectFileContent(
        "aaa/A.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library a;",
            ""));
  }

  public void test_OK_reference_inImport() throws Exception {
    IFolder destination = testProject.createFolder("aaa");
    setProjectFileContent(
        "A.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library a;",
            "import 'B.dart';",
            ""));
    IFile fileB = setProjectFileContent(
        "B.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library b;",
            "import 'A.dart';",
            ""));
    // do move
    buildAndMove(destination, fileB);
    assertProjectFileContent(
        "A.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library a;",
            "import 'aaa/B.dart';",
            ""));
    assertProjectFileContent(
        "aaa/B.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library b;",
            "import '../A.dart';",
            ""));
  }

  /**
   * <p>
   * http://code.google.com/p/dart/issues/detail?id=6978
   */
  public void test_OK_reference_inImport_inDeepFolder() throws Exception {
    IFolder destination = testProject.createFolder("aaa");
    testProject.createFolder("aaa/bbb");
    setProjectFileContent(
        "aaa/bbb/A.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library a;",
            "import 'B.dart';",
            ""));
    IFile fileB = setProjectFileContent(
        "aaa/bbb/B.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library b;",
            "import 'A.dart';",
            ""));
    // do move
    buildAndMove(destination, fileB);
    assertProjectFileContent(
        "aaa/bbb/A.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library a;",
            "import '../B.dart';",
            ""));
    assertProjectFileContent(
        "aaa/B.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library b;",
            "import 'bbb/A.dart';",
            ""));
  }

  public void test_OK_reference_inPart() throws Exception {
    IFolder destination = testProject.createFolder("aaa");
    setProjectFileContent(
        "A.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library a;",
            "part 'B.dart';",
            ""));
    IFile fileB = setProjectFileContent(
        "B.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "part of a;",
            "void foo() {}",
            ""));
    // do move
    buildAndMove(destination, fileB);
    assertProjectFileContent(
        "A.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library a;",
            "part 'aaa/B.dart';",
            ""));
    assertProjectFileContent(
        "aaa/B.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "part of a;",
            "void foo() {}",
            ""));
  }

  public void test_OK_reference_toPart() throws Exception {
    IFolder destination = testProject.createFolder("aaa");
    setProjectFileContent(
        "A.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "part of b;",
            "void foo() {}",
            ""));
    IFile fileB = setProjectFileContent(
        "B.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library b;",
            "part 'A.dart';",
            ""));
    // do move
    buildAndMove(destination, fileB);
    assertProjectFileContent(
        "A.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "part of b;",
            "void foo() {}",
            ""));
    assertProjectFileContent(
        "aaa/B.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library b;",
            "part '../A.dart';",
            ""));
  }

  public void test_OK_spaceInPath() throws Exception {
    IFolder destination = testProject.createFolder("sub folder");
    setProjectFileContent(
        "A.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library a;",
            "import 'B.dart';",
            ""));
    IFile fileB = setProjectFileContent(
        "B.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library b;",
            "import 'A.dart';",
            ""));
    // do move
    buildAndMove(destination, fileB);
    assertProjectFileContent(
        "A.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library a;",
            "import 'sub folder/B.dart';",
            ""));
    assertProjectFileContent(
        "sub folder/B.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library b;",
            "import '../A.dart';",
            ""));
  }

  @Override
  protected void setUp() throws Exception {
    testProject = new TestProject();
  }

  @Override
  protected void tearDown() throws Exception {
    testProject.dispose();
    testProject = null;
  }

  private void assertProjectFileContent(String path, String expectedContent) throws Exception {
    assertEquals(expectedContent, testProject.getFileString(path));
  }

  private IFile setProjectFileContent(String path, String content) throws Exception {
    return testProject.setFileContent(path, content);
  }
}
