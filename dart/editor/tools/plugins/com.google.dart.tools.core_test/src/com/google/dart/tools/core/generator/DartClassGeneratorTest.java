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

public class DartClassGeneratorTest extends DartFileGeneratorTest {

  public void test_DartClassGeneratorTest_execute() throws IOException, CoreException {
    DartClassGenerator generator = getGenerator();
    DartLibraryGenerator gen = getLibGenerator();
    gen.execute(new NullProgressMonitor());
    generator.setName("MyNewClass");
    generator.setLibrary(getLibPath(gen));
    IStatus status = generator.validate();
    assertTrue(status.getMessage(), status.isOK());
    generator.execute(new NullProgressMonitor());
    IFile file = getLibFolder().getFile(new Path("MyNewClass.dart"));
    assertTrue(file.exists());
    String expected = readExpectedContent("MyNewClass.txt");
    String actual = TestFileUtil.readFile(file);
    assertEquals(expected, actual);
  }

  public void test_DartClassGeneratorTest_validate_bad1() throws CoreException {
    testValidate("/", null, null, false);
  }

  public void test_DartClassGeneratorTest_validate_bad2() throws CoreException {
    testValidate("foo/", null, null, false);
  }

  public void test_DartClassGeneratorTest_validate_bad3() throws CoreException {
    testValidate("/foo", null, null, false);
  }

  public void test_DartClassGeneratorTest_validate_empty() throws CoreException {
    testValidate("", null, null, false);
  }

  public void test_DartClassGeneratorTest_validate_good() throws CoreException {
    testValidate("Foo", null, null, true);
  }

  public void test_DartClassGeneratorTest_validate_goodWithInterface() throws CoreException {
    testValidate("Foo", "Bar", null, true);
  }

  public void test_DartClassGeneratorTest_validate_goodWithParent() throws CoreException {
    testValidate("Foo", null, "TestInterface", true);
  }

  public void test_DartClassGeneratorTest_validate_goodWithParentAndInterface()
      throws CoreException {
    testValidate("Foo", "Bar2", "TestInterface2", true);
  }

  private DartClassGenerator getGenerator() throws CoreException {
    DartClassGenerator generator = new DartClassGenerator(true);
    generator.setContainer(getLibFolder());
    return generator;
  }

  private DartInterfaceGenerator getInterfaceGenerator() throws CoreException {
    DartInterfaceGenerator generator = new DartInterfaceGenerator(false);
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

  private void testValidate(String name, String parentName, String interfaceName, boolean expected)
      throws CoreException {
    DartClassGenerator classGen = getGenerator();
    DartLibraryGenerator libGen = getLibGenerator();
    if (parentName != null) {
      DartClassGenerator parentGen = getGenerator();
      parentGen.setName(parentName);
      parentGen.execute(new NullProgressMonitor());
      DartElement parentClass = DartCore.create(parentGen.getFile());
      classGen.setParentType(parentClass);
    }
    if (interfaceName != null) {
      DartInterfaceGenerator interfaceGen = getInterfaceGenerator();
      interfaceGen.setName(interfaceName);
      interfaceGen.execute(new NullProgressMonitor());
      DartElement parentInterface = DartCore.create(interfaceGen.getFile());
      classGen.setParentType(parentInterface);
    }
    classGen.setName(name);
    classGen.setLibrary(getLibPath(libGen));
    IStatus status = classGen.validate();
    assertEquals(status.getMessage(), expected, status.isOK());
  }
}
