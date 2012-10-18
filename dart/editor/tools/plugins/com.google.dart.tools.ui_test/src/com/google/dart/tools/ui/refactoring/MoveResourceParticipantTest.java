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

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.internal.corext.refactoring.rename.MoveResourceParticipant;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameResourceParticipant;
import com.google.dart.tools.internal.corext.refactoring.util.ReflectionUtils;
import com.google.dart.tools.ui.internal.refactoring.MoveSupport;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * Test for {@link MoveResourceParticipant}.
 */
public final class MoveResourceParticipantTest extends RefactoringTest {

  /**
   * Moves given {@link IFile}.
   */
  private static void moveUnit(CompilationUnit unit, IFolder destination) throws Exception {
    IProgressMonitor pm = new NullProgressMonitor();
    RefactoringStatus status = new RefactoringStatus();
    // create Refactoring
    IFile file = (IFile) unit.getResource();
    Refactoring refactoring = MoveSupport.createMoveRefactoring(
        status,
        new IResource[] {file},
        destination);
    // execute Refactoring
    status.merge(refactoring.checkAllConditions(pm));
    Change change = refactoring.createChange(pm);
    change.initializeValidationData(pm);
    new PerformChangeOperation(change).run(pm);
    // all OK
    assertTrue(status.toString(), status.isOK());
  }

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

  public void test_OK_noReferences() throws Exception {
    IFolder destination = testProject.createFolder("aaa");
    CompilationUnit unitA = setUnitContent("A.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "library a;",
        ""});
    // do move
    moveUnit(unitA, destination);
    assertUnitContent(testProject.getUnit("aaa/A.dart"), new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "library a;",
        ""});
  }

  public void test_OK_reference_inImport() throws Exception {
    IFolder destination = testProject.createFolder("aaa");
    setUnitContent("A.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "library a;",
        "import 'B.dart';",
        ""});
    CompilationUnit unitB = setUnitContent("B.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "library b;",
        "import 'A.dart';",
        ""});
    // do move
    moveUnit(unitB, destination);
    assertUnitContent(testProject.getUnit("A.dart"), new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "library a;",
        "import 'aaa/B.dart';",
        ""});
    assertUnitContent(testProject.getUnit("aaa/B.dart"), new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "library b;",
        "import '../A.dart';",
        ""});
  }

  public void test_OK_reference_inPart() throws Exception {
    IFolder destination = testProject.createFolder("aaa");
    setUnitContent("A.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "library a;",
        "part 'B.dart';",
        ""});
    CompilationUnit unitB = setUnitContent("B.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "void foo() {}",
        ""});
    // do move
    moveUnit(unitB, destination);
    assertUnitContent(testProject.getUnit("A.dart"), new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "library a;",
        "part 'aaa/B.dart';",
        ""});
    assertUnitContent(testProject.getUnit("aaa/B.dart"), new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "void foo() {}",
        ""});
  }

  public void test_OK_reference_toPart() throws Exception {
    IFolder destination = testProject.createFolder("aaa");
    setUnitContent("A.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "void foo() {}",
        ""});
    CompilationUnit unitB = setUnitContent("B.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "library b;",
        "part 'A.dart';",
        ""});
    // do move
    moveUnit(unitB, destination);
    assertUnitContent(testProject.getUnit("A.dart"), new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "void foo() {}",
        ""});
    assertUnitContent(testProject.getUnit("aaa/B.dart"), new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "library b;",
        "part '../A.dart';",
        ""});
  }

}
