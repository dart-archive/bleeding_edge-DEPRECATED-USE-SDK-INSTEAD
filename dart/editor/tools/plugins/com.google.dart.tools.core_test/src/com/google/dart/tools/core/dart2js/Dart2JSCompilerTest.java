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

package com.google.dart.tools.core.dart2js;

import com.google.dart.tools.core.dart2js.Dart2JSCompiler.CompilationResult;
import com.google.dart.tools.core.test.util.TestProject;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.NullProgressMonitor;

public class Dart2JSCompilerTest extends TestCase {

  public void test_dart2js_available() throws Exception {
    Dart2JSCompiler compiler = new Dart2JSCompiler();

    assertEquals(true, compiler.isAvailable());
  }

  public void test_dart2js_compile1() throws Exception {
    Dart2JSCompiler compiler = new Dart2JSCompiler();

    TestProject project = new TestProject("fooBar");

    try {
      IFile file = project.setFileContent("foo.dart", "void main() { print('foo'); }");
      IFile outFile = project.getProject().getFile("foo.dart.js");

      CompilationResult result = compiler.compile(
          file.getLocation(),
          outFile.getLocation(),
          new NullProgressMonitor());

      if (result.getExitCode() != 0) {
        System.err.print(result.getAllOutput());
      }

      assertEquals(0, result.getExitCode());
      assertEquals(true, outFile.exists());

      // Make sure we wrote out some content.
      assertTrue(outFile.getLocation().toFile().length() > 100);
    } finally {
      project.dispose();
    }
  }

}
