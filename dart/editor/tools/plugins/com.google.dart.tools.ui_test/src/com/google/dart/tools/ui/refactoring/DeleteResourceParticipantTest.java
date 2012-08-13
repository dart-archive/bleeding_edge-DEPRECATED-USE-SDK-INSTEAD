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

import com.google.common.base.Joiner;
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.core.test.util.TestProject;
import com.google.dart.tools.internal.corext.refactoring.rename.DeleteResourceParticipant;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameResourceParticipant;
import com.google.dart.tools.internal.corext.refactoring.util.ReflectionUtils;
import com.google.dart.tools.ui.internal.refactoring.RenameSupport;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.DeleteRefactoring;

/**
 * Test for {@link DeleteResourceParticipant}.
 */
public final class DeleteResourceParticipantTest extends RefactoringTest {
  /**
   * Asserts that file was deleted.
   */
  private static void assertFileWasDeleted(IFile file) {
    assertFalse(file.exists());
  }

  /**
   * Uses {@link RenameSupport} to rename {@link DartVariableDeclaration}.
   */
  @SuppressWarnings("restriction")
  private static void deleteFile(IFile file) throws Exception {
    TestProject.waitForAutoBuild();
    // prepare status
    IProgressMonitor pm = new NullProgressMonitor();
    RefactoringStatus status = new RefactoringStatus();
    // create Refactoring
    DeleteRefactoring refactoring = new DeleteRefactoring(
        new org.eclipse.ltk.internal.core.refactoring.resource.DeleteResourcesProcessor(
            new IResource[] {file}));
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
    DeleteResourceParticipant participant = new DeleteResourceParticipant();
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

  public void test_OK_inImport() throws Exception {
    IFile targetFile = (IFile) testProject.setUnitContent("target.dart", "").getResource();
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('Test');",
        "#import('target.dart');",
        "");
    assertTrue(targetFile.exists());
    // do rename
    deleteFile(targetFile);
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('Test');",
        "");
    assertFileWasDeleted(targetFile);
  }

  public void test_OK_inSource() throws Exception {
    IFile targetFile = (IFile) testProject.setUnitContent("target.dart", "").getResource();
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('Test');",
        "#source('target.dart');",
        "");
    assertTrue(targetFile.exists());
    // do rename
    deleteFile(targetFile);
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('Test');",
        "");
    assertFileWasDeleted(targetFile);
  }

  public void test_OK_inSource_spaces_slashR_slashN() throws Exception {
    IFile targetFile = (IFile) testProject.setUnitContent("target.dart", "").getResource();
    setTestUnitContent(Joiner.on("\r\n").join(
        new String[] {
            "// filler filler filler filler filler filler filler filler filler filler",
            "#library('Test');",
            "#source('target.dart'); \t ",
            "// trailing comment",
            ""}));
    assertTrue(targetFile.exists());
    // do rename
    deleteFile(targetFile);
    assertTestUnitContent(Joiner.on("\r\n").join(
        new String[] {
            "// filler filler filler filler filler filler filler filler filler filler",
            "#library('Test');",
            "// trailing comment",
            ""}));
    assertFileWasDeleted(targetFile);
  }

  public void test_OK_noReferences() throws Exception {
    IFile targetFile = (IFile) testProject.setUnitContent("target.dart", "").getResource();
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('Test');",
        "");
    assertTrue(targetFile.exists());
    // do rename
    deleteFile(targetFile);
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('Test');",
        "");
    assertFileWasDeleted(targetFile);
  }
}
