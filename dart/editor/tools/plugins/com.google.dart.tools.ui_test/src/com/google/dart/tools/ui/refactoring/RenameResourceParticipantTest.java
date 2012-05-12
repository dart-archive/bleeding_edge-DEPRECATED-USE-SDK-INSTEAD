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

import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.core.test.util.TestProject;
import com.google.dart.tools.internal.corext.refactoring.rename.RenameResourceParticipant;
import com.google.dart.tools.ui.internal.refactoring.RenameSupport;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.resource.RenameResourceDescriptor;

import java.lang.reflect.Method;

/**
 * Test for {@link RenameResourceParticipant}.
 */
public final class RenameResourceParticipantTest extends RefactoringTest {
  /**
   * Asserts that file was renamed to the given name.
   */
  private static void assertFileWasRenamed(IFile file, String newName) {
    assertFalse(file.exists());
    assertTrue(file.getParent().getFile(new Path(newName)).exists());
  }

  /**
   * Uses {@link RenameSupport} to rename {@link DartVariableDeclaration}.
   */
  private static void renameFile(IFile file, String newName) throws Exception {
    TestProject.waitForAutoBuild();
    IProgressMonitor pm = new NullProgressMonitor();
    RefactoringStatus status = new RefactoringStatus();
    // create Refactoring
    RenameResourceDescriptor refactoringDescriptor = new RenameResourceDescriptor();
    refactoringDescriptor.setResourcePath(file.getFullPath());
    refactoringDescriptor.setNewName(newName);
    refactoringDescriptor.setUpdateReferences(true);
    Refactoring refactoring = refactoringDescriptor.createRefactoring(status);
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
    RenameResourceParticipant participant = new RenameResourceParticipant();
    // initialize(Object) requires IFile
    {
      Method method = participant.getClass().getDeclaredMethod("initialize", Object.class);
      method.setAccessible(true);
      Boolean res = (Boolean) method.invoke(participant, new Object());
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
    renameFile(targetFile, "newName.dart");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('Test');",
        "#import('newName.dart');",
        "");
    assertFileWasRenamed(targetFile, "newName.dart");
  }

  public void test_OK_inResource() throws Exception {
    IFile targetFile = testProject.setFileContent("target.txt", "");
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('Test');",
        "#resource('target.txt');",
        "");
    assertTrue(targetFile.exists());
    // do rename
    renameFile(targetFile, "newName.txt");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('Test');",
        "#resource('newName.txt');",
        "");
    assertFileWasRenamed(targetFile, "newName.txt");
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
    renameFile(targetFile, "newName.dart");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('Test');",
        "#source('newName.dart');",
        "");
    assertFileWasRenamed(targetFile, "newName.dart");
  }

  public void test_OK_noReferences() throws Exception {
    IFile targetFile = (IFile) testProject.setUnitContent("target.dart", "").getResource();
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('Test');",
        "");
    assertTrue(targetFile.exists());
    // do rename
    renameFile(targetFile, "newName.dart");
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('Test');",
        "");
    assertFileWasRenamed(targetFile, "newName.dart");
  }
}
