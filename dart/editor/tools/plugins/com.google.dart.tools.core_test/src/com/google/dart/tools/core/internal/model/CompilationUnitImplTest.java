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
package com.google.dart.tools.core.internal.model;

import com.google.dart.tools.core.internal.workingcopy.DefaultWorkingCopyOwner;
import com.google.dart.tools.core.mock.MockFile;
import com.google.dart.tools.core.mock.MockProject;
import com.google.dart.tools.core.model.DartModelException;

import junit.framework.TestCase;

public class CompilationUnitImplTest extends TestCase {
  public void test_CompilationUnitImpl_getChildren_multipleTypes() {
    // TODO Implement this.
  }

  public void test_CompilationUnitImpl_getChildren_nonExistant() {
    MockFile file = new MockFile(new MockProject());
    CompilationUnitImpl cu = new CompilationUnitImpl(
        (DartLibraryImpl) null,
        file,
        DefaultWorkingCopyOwner.getInstance());
    try {
      cu.getChildren();
      fail("Expected DartModelException");
    } catch (DartModelException exception) {
      // Expected
    }
  }

  public void test_CompilationUnitImpl_getChildren_noTypes() {
    // TODO Implement this.
  }

  public void test_CompilationUnitImpl_getChildren_singleType() {
    // TODO Implement this.
  }

  // public void test_CompilationUnitImpl_getCorrespondingResource()
  // throws DartModelException {
  // IFile file = new MockFile();
  // CompilationUnitImpl cu = new CompilationUnitImpl(null, file,
  // DefaultWorkingCopyOwner.getInstance());
  // assertEquals(file, cu.getCorrespondingResource());
  // }

  public void test_CompilationUnitImpl_getElementName() {
    String fileName = "file.dart";
    MockFile file = new MockFile(new MockProject(), fileName);
    CompilationUnitImpl cu = new CompilationUnitImpl(
        (DartLibraryImpl) null,
        file,
        DefaultWorkingCopyOwner.getInstance());
    assertEquals(fileName, cu.getElementName());
  }

  // public void test_CompilationUnitImpl_getNonDartResources()
  // throws DartModelException {
  // CompilationUnitImpl cu = new CompilationUnitImpl(null, null,
  // DefaultWorkingCopyOwner.getInstance());
  // IResource[] resources = cu.getNonDartResources();
  // assertNotNull(resources);
  // assertEquals(0, resources.length);
  // }

  public void test_CompilationUnitImpl_getTypes_multipleTypes() {
    // TODO Implement this.
  }

  public void test_CompilationUnitImpl_getTypes_nonExistant() {
    MockFile file = new MockFile(new MockProject());
    CompilationUnitImpl cu = new CompilationUnitImpl(
        (DartLibraryImpl) null,
        file,
        DefaultWorkingCopyOwner.getInstance());
    try {
      cu.getTypes();
      fail("Expected DartModelException");
    } catch (DartModelException exception) {
      // Expected
    }
  }

  public void test_CompilationUnitImpl_getTypes_noTypes() {
    // TODO Implement this.
  }

  public void test_CompilationUnitImpl_getTypes_singleType() {
    // TODO Implement this.
  }
}
