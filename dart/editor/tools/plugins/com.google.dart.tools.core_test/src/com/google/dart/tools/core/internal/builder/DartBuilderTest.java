/*
 * Copyright 2011, the Dart project authors.
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
package com.google.dart.tools.core.internal.builder;

import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.test.util.MoneyProjectUtilities;
import com.google.dart.tools.core.test.util.TestUtilities;

import junit.framework.TestCase;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;

import java.io.File;

public class DartBuilderTest extends TestCase {
  public void test_DartBuilder_fullBuild() throws Exception {
    testBuild(IncrementalProjectBuilder.FULL_BUILD, "full");
  }

  public void test_DartBuilder_incrementalBuild() throws Exception {
    testBuild(IncrementalProjectBuilder.INCREMENTAL_BUILD, "incremental");
  }

  public void test_DartBuilder_multipleProjects() throws Exception {
    DartProject dartMoneyProject = MoneyProjectUtilities.getMoneyProject();
    IProject moneyProject = dartMoneyProject.getProject();

    DartProject dartSampleProject = TestUtilities.loadPluginRelativeProject(
        "com.google.dart.tools.core_test", "SampleCode");
    IProject sampleProject = dartSampleProject.getProject();

    moneyProject.build(IncrementalProjectBuilder.FULL_BUILD, null);
    sampleProject.build(IncrementalProjectBuilder.FULL_BUILD, null);

    File moneyOutputFile = moneyProject.getLocation().append("money.app.js").toFile();
    assertTrue(moneyOutputFile.length() > 0);

    File sampleOutputFile = sampleProject.getLocation().append("sampler.app.js").toFile();
    assertTrue(sampleOutputFile.length() > 0);
  }

  private void testBuild(int buildKind, String buildType) throws Exception {
    DartProject dartProject = MoneyProjectUtilities.getMoneyProject();
    IProject project = dartProject.getProject();
    project.build(buildKind, null);
    File outputFile = project.getLocation().append("money.app.js").toFile();
    long expectedLength = outputFile.length();
    assertTrue(expectedLength > 0);
    for (int i = 0; i < 10; i++) {
      project.build(buildKind, null);
      long actualLength = outputFile.length();
      assertEquals(
          "Different output file length on iteration " + i + " of " + buildType + " build",
          expectedLength, actualLength);
    }
  }
}
