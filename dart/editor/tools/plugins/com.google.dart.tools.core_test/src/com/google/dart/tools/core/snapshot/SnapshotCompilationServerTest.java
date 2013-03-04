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

import com.google.dart.tools.core.test.util.TestProject;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;

import java.io.File;

public class SnapshotCompilationServerTest extends TestCase {

  public void testCompile() throws Exception {
    TestProject project = new TestProject("fooBar");

    try {
      IFile file = project.setFileContent("foo.dart", "void main() { print('foo'); }");
      File sourceFile = file.getLocation().toFile();
      SnapshotCompilationServer compiler = new SnapshotCompilationServer(sourceFile);
      File destFile = compiler.getDestFile();

      assertTrue(compiler.needsRecompilation());

      IStatus result = compiler.compile();

      if (result.getCode() != IStatus.OK) {
        System.err.print(result.getCode() + ": " + result.getMessage());
      }

      assertEquals(IStatus.OK, result.getCode());
      assertEquals(true, destFile.exists());
      assertTrue(destFile.length() > 100);

      assertFalse(compiler.needsRecompilation());

      // adjust the file modification time of the snapshot into the past...
      destFile.setLastModified(System.currentTimeMillis() - 10 * 1000);
      project.setFileContent("foo.dart", "void main() { print('foo2'); }");
      assertTrue(compiler.needsRecompilation());

      // recompile
      assertEquals(IStatus.OK, compiler.compile().getCode());
      assertFalse(compiler.needsRecompilation());
    } finally {
      project.dispose();
    }
  }

  public void testGetDestFile() {
    SnapshotCompilationServer compiler = new SnapshotCompilationServer(new File("foo.dart"));

    assertEquals("foo.snapshot", compiler.getDestFile().getName());
  }
}
