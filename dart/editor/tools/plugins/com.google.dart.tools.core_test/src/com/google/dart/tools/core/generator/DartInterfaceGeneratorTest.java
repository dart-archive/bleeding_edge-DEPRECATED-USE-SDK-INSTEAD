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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.testutil.TestFileUtil;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import java.io.IOException;

public class DartInterfaceGeneratorTest extends DartFileGeneratorTest {

  public void test_DartInterfaceGeneratorTest_execute() throws IOException, CoreException {
    DartInterfaceGenerator generator = getGenerator();
    DartLibraryGenerator gen = getLibGenerator();
    gen.execute(new NullProgressMonitor());
    generator.setName("MyNewInterface");
    generator.setLibrary(getLibPath(gen));
    IStatus status = generator.validate();
    assertTrue(status.getMessage(), status.isOK());
    generator.execute(new NullProgressMonitor());
    IFile file = getLibFolder().getFile(new Path("MyNewInterface.dart"));
    assertTrue(file.exists());
    String expected = readExpectedContent("MyNewInterface.txt");
    String actual = TestFileUtil.readFile(file);
    assertEquals(expected, actual);
  }

  public void test_DartInterfaceGeneratorTest_validate_bad1() throws CoreException {
    testValidate("/", null, false);
  }

  public void test_DartInterfaceGeneratorTest_validate_bad2() throws CoreException {
    testValidate("foo/", null, false);
  }

  public void test_DartInterfaceGeneratorTest_validate_bad3() throws CoreException {
    testValidate("/foo", null, false);
  }

  public void test_DartInterfaceGeneratorTest_validate_empty() throws CoreException {
    testValidate("", null, false);
  }

  public void test_DartInterfaceGeneratorTest_validate_good() throws CoreException {
    testValidate("Foo", null, true);
  }

  public void test_DartInterfaceGeneratorTest_validate_goodWithParent() throws CoreException {
    testValidate("Foo", "Bar", true);
  }

  private DartInterfaceGenerator getGenerator() throws CoreException {
    DartInterfaceGenerator generator = new DartInterfaceGenerator(true);
    generator.setContainer(getLibFolder());
    return generator;
  }

  private IContainer getLibFolder() throws CoreException {
    return TestFileUtil.getOrCreateDartLibContainer(getProject(), "MyLib");
  }

  private DartLibraryGenerator getLibGenerator() throws CoreException {
    DartLibraryGenerator gen = new DartLibraryGenerator(false);
    gen.setName("MyLib");
    gen.setContainerPath(getProject().getFullPath().append("src").toOSString());
    return gen;
  }

  private String getLibPath(DartLibraryGenerator library) {
    return library.getContainerPath() + "/" + library.getName() + ".dart";
  }

  private void testValidate(String name, String parentName, boolean expected) throws CoreException {
    DartInterfaceGenerator interfaceGen = getGenerator();
    DartLibraryGenerator libGen = getLibGenerator();
    if (parentName != null) {
      DartInterfaceGenerator parentGenerator = getGenerator();
      parentGenerator.setName(parentName);
      parentGenerator.execute(new NullProgressMonitor());
      DartElement lib = DartCore.create(parentGenerator.getFile());
      DartElement[] arr = {lib};
      interfaceGen.setInterfaceList(arr);
    }
    interfaceGen.setName(name);
    interfaceGen.setLibrary(getLibPath(libGen));
    IStatus status = interfaceGen.validate();
    assertEquals(status.getMessage(), expected, status.isOK());
  }
}
