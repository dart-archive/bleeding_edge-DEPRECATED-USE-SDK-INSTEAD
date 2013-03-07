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

package com.google.dart.tools.core.snapshot;

import com.google.dart.tools.core.test.util.PlainTestProject;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;

import java.io.File;

public class SnapshotCompilerTest extends TestCase {

  public void testCompile() throws Exception {
    SnapshotCompiler compiler = new SnapshotCompiler();

    PlainTestProject project = new PlainTestProject("fooBar");

    try {
      IFile file = project.setFileContent("foo.dart", "void main() { print('foo'); }");
      File sourceFile = file.getLocation().toFile();
      File destFile = SnapshotCompiler.createDestFileName(sourceFile);

      IStatus result = compiler.compile(sourceFile, destFile);

      if (result.getCode() != IStatus.OK) {
        System.err.print(result.getCode() + ": " + result.getMessage());
      }

      assertEquals(IStatus.OK, result.getCode());
      assertEquals(true, destFile.exists());

      // Make sure we wrote out some content.
      assertTrue(destFile.length() > 100);
    } finally {
      project.dispose();
    }
  }

  public void testCreateDestFileName() {
    File sourceFile = new File("foo.dart");
    File destFile = SnapshotCompiler.createDestFileName(sourceFile);

    assertEquals("foo.snapshot", destFile.getName());
  }

  public void testIsAvailable() {
    SnapshotCompiler compiler = new SnapshotCompiler();

    assertEquals(true, compiler.isAvailable());
  }

}
