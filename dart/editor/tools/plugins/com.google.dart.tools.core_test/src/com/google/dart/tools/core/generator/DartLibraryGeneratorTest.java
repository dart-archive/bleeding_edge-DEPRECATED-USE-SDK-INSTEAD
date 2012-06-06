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
package com.google.dart.tools.core.generator;

import com.google.dart.tools.core.testutil.TestFileUtil;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

public class DartLibraryGeneratorTest extends DartFileGeneratorTest {

  public void test_DartLibraryGenerator_execute_lib() throws Exception {
    final DartLibraryGenerator generator = getGenerator();
    final String libName = "SomeLib";
    assertFalse(getProject().getFolder(libName).exists());
    generator.setName(libName);
    assertTrue(generator.validate().isOK());
    generator.setContainerPath(getProject().getFullPath().append("src").toOSString());
    generator.execute(new NullProgressMonitor());
    IFile file = getProject().getFile("src/" + libName + ".dart");
    assertTrue(file.exists());
    String expected = readExpectedContent("SomeLib.txt");
    String actual = TestFileUtil.readFile(file);
    assertEquals(expected.trim(), actual.trim());
  }

  public void test_DartLibraryGenerator_getSuggestedClassName_empty() throws Exception {
    testGetSuggestedClassName("", "");
  }

  public void test_DartLibraryGenerator_getSuggestedClassName_lib() throws Exception {
    testGetSuggestedClassName("Foo", "Foo");
  }

  public void test_DartLibraryGenerator_getSuggestedClassName_lowerCase() throws Exception {
    testGetSuggestedClassName("foo", "Foo");
  }

  public void test_DartLibraryGenerator_getSuggestedClassName_space() throws Exception {
    testGetSuggestedClassName("My Foo", "MyFoo");
  }

  public void test_DartLibraryGenerator_getSuggestedClassName_valid() throws Exception {
    testGetSuggestedClassName("Foo", "Foo");
  }

  public void test_DartLibraryGenerator_validate_bad1() throws CoreException {
    testValidateName("foo/", false);
  }

  public void test_DartLibraryGenerator_validate_bad2() throws CoreException {
    testValidateName("/foo", false);
  }

  public void test_DartLibraryGenerator_validate_badClassName1() throws CoreException {
    testValidateClassName("/Baz", false);
  }

  public void test_DartLibraryGenerator_validate_badClassName2() throws CoreException {
    testValidateClassName("Baz/", false);
  }

  public void test_DartLibraryGenerator_validate_badClassName3() throws CoreException {
    testValidateClassName("Baz.", false);
  }

  public void test_DartLibraryGenerator_validate_empty() throws CoreException {
    testValidateName("", false);
  }

  public void test_DartLibraryGenerator_validate_good() throws CoreException {
    testValidateName("foo", true);
  }

  public void test_DartLibraryGenerator_validate_good2() throws CoreException {
    testValidateName("foo/bar", true);
  }

  public void test_DartLibraryGenerator_validate_goodClassName1() throws CoreException {
    testValidateClassName("", true);
  }

  public void test_DartLibraryGenerator_validate_goodClassName2() throws CoreException {
    testValidateClassName("Baz", true);
  }

  protected void testGetSuggestedClassName(String libName, String expectedClassName) {
    DartLibraryGenerator generator = new DartLibraryGenerator(false);
    generator.setName(libName);
    assertEquals(expectedClassName, generator.getSuggestedClassName());
  }

  private DartLibraryGenerator getGenerator() throws CoreException {
    final DartLibraryGenerator generator = new DartLibraryGenerator(false);
    generator.setContainer(getProject());
    return generator;
  }

  private void testValidateClassName(String className, boolean expectedOk) throws CoreException {
    final DartLibraryGenerator generator = getGenerator();
    generator.setName("somethingelse");
    generator.setClassName(className);
    assertEquals(expectedOk, generator.validate().isOK());
  }

  private void testValidateName(String libName, boolean expectedOk) throws CoreException {
    final DartLibraryGenerator generator = getGenerator();
    generator.setName(libName);
    generator.setClassName("");
    assertEquals(expectedOk, generator.validate().isOK());
  }
}
