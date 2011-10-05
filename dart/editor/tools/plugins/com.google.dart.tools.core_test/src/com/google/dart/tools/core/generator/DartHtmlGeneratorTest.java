/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.generator;

import com.google.dart.tools.core.testutil.TestFileUtil;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

public class DartHtmlGeneratorTest extends DartFileGeneratorTest {

  public void testExecute() throws Exception {
    DartHtmlGenerator generator = getGenerator();
    generator.setName("MyNewWebPage");
    generator.setTitle("SomeRandomTitle");
    generator.setDartAppFile(getLibFolder().getFile(new Path("js/ANewDartType.app.js")));
    IStatus status = generator.validate();
    assertTrue(status.getMessage(), status.isOK());
    generator.execute(new NullProgressMonitor());
    IFile file = getLibFolder().getFile(new Path("MyNewWebPage.html"));
    assertTrue(file.exists());
    String expected = readExpectedContent("MyNewWebPage.txt");
    String actual = TestFileUtil.readFile(file);
    assertEquals(expected, actual);
  }

  private DartHtmlGenerator getGenerator() throws CoreException {
    DartHtmlGenerator generator = new DartHtmlGenerator(true);
    generator.setContainer(getLibFolder());
    return generator;
  }

  private IContainer getLibFolder() throws CoreException {
    return TestFileUtil.getOrCreateDartLibContainer(getProject(), "MyLib");
  }
}
