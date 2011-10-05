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

import junit.framework.TestCase;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import static org.junit.Assert.assertArrayEquals;

public class DartProjectGeneratorTest extends TestCase {

  public void test_DartProjectGenerator_execute() throws CoreException {
    final String projectName = "DartProjectGeneratorTest";
    final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
    assertFalse(project.exists());
    final DartProjectGenerator generator = new DartProjectGenerator();
    generator.setName(projectName);
    assertTrue(generator.validate().isOK());
    generator.execute(new NullProgressMonitor());
    assertTrue(project.exists());
    IProjectDescription description = project.getDescription();
    assertEquals(projectName, description.getName());
    assertArrayEquals(new String[] {DartCore.DART_PROJECT_NATURE}, description.getNatureIds());
    assertEquals(1, description.getBuildSpec().length);
    ICommand command = description.getBuildSpec()[0];
    assertEquals(DartCore.DART_BUILDER_ID, command.getBuilderName());
  }

  public void test_DartProjectGenerator_getSuggestedLibraryPath_complex() throws Exception {
    testSuggestedLibraryPath("baz.bar", false, "bar.dart");
  }

  public void test_DartProjectGenerator_getSuggestedLibraryPath_complex1dot() throws Exception {
    testSuggestedLibraryPath("baz.bar.", false, "bar.dart");
  }

  public void test_DartProjectGenerator_getSuggestedLibraryPath_complexApp() throws Exception {
    testSuggestedLibraryPath("baz.bar", true, "bar.dart");
  }

  public void test_DartProjectGenerator_getSuggestedLibraryPath_dot1() throws Exception {
    testSuggestedLibraryPath(".", false, "");
  }

  public void test_DartProjectGenerator_getSuggestedLibraryPath_dot1simple() throws Exception {
    testSuggestedLibraryPath(".foo", false, "foo.dart");
  }

  public void test_DartProjectGenerator_getSuggestedLibraryPath_dot2() throws Exception {
    testSuggestedLibraryPath("..", false, "");
  }

  public void test_DartProjectGenerator_getSuggestedLibraryPath_dot2simple() throws Exception {
    testSuggestedLibraryPath("..foo", false, "foo.dart");
  }

  public void test_DartProjectGenerator_getSuggestedLibraryPath_empty() throws Exception {
    testSuggestedLibraryPath("", false, "");
  }

  public void test_DartProjectGenerator_getSuggestedLibraryPath_simple1() throws Exception {
    testSuggestedLibraryPath("foo", false, "foo.dart");
  }

  public void test_DartProjectGenerator_getSuggestedLibraryPath_simple1dot() throws Exception {
    testSuggestedLibraryPath("foo.", false, "foo.dart");
  }

  public void test_DartProjectGenerator_getSuggestedLibraryPath_simple2dot() throws Exception {
    testSuggestedLibraryPath("foo..", false, "foo.dart");
  }

  public void test_DartProjectGenerator_validate_bad() {
    testValidate("foo/", false);
  }

  public void test_DartProjectGenerator_validate_empty() {
    testValidate("", false);
  }

  public void test_DartProjectGenerator_validate_good() {
    testValidate("foo", true);
  }

  private void testSuggestedLibraryPath(String projName, boolean isApplication, String expected) {
    final DartProjectGenerator generator = new DartProjectGenerator();
    generator.setName(projName);
    assertEquals(projName, generator.getName());
    assertEquals(expected, generator.getSuggestedLibraryPath(isApplication));
  }

  private void testValidate(String projName, boolean expectedOk) {
    final DartProjectGenerator generator = new DartProjectGenerator();
    generator.setName(projName);
    assertEquals(projName, generator.getName());
    assertEquals(expectedOk, generator.validate().isOK());
  }
}
