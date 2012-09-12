/*
 * Copyright 2012 Dart project authors.
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

package com.google.dart.tools.core.dartdoc;

import com.google.dart.tools.core.dartdoc.DartdocGenerator.CompilationResult;
import com.google.dart.tools.core.test.util.TestProject;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;

public class DartdocGeneratorTest extends TestCase {

  public void test_dartdoc_available() throws Exception {
    DartdocGenerator compiler = new DartdocGenerator();

    assertEquals(true, compiler.isAvailable());
  }

  public void test_dartdoc_compile1() throws Exception {
    DartdocGenerator dartdocGen = new DartdocGenerator();

    TestProject project = new TestProject("fooBar");

    try {
      IFile file = project.setFileContent("foo.dart", "/**\n" + " ** Some dartdoc.\n" + " */\n"
          + "void main() { print('foo'); }");
      IFolder docsFolder = project.getProject().getFolder("docs");

      CompilationResult result = dartdocGen.dartdoc(
          file.getLocation(),
          docsFolder.getLocation(),
          new NullProgressMonitor());

      if (result.getExitCode() != 0) {
        System.err.print(result.getAllOutput());
      }

      docsFolder.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());

      assertEquals(0, result.getExitCode());
      assertEquals(true, docsFolder.exists());
    } finally {
      project.dispose();
    }
  }
}
