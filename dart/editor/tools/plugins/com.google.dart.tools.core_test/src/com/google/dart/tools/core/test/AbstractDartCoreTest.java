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
package com.google.dart.tools.core.test;

import com.google.common.base.Joiner;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.test.util.TestProject;

import junit.framework.TestCase;

/**
 * Abstract base for any Dart test which uses {@link TestProject}.
 */
public abstract class AbstractDartCoreTest extends TestCase {
  protected static String makeSource(String... lines) {
    return Joiner.on("\n").join(lines);
  }

  protected TestProject testProject;

  protected CompilationUnit testUnit;

  /**
   * Sets content of <code>Test.dart</code> unit.
   */
  protected final CompilationUnit setTestUnitContent(String... lines) throws Exception {
    do {
      testUnit = setUnitContent("Test.dart", lines);
    } while (testUnit == null);
    return testUnit;
  }

  /**
   * Sets content of the unit with given path.
   */
  protected final CompilationUnit setUnitContent(String path, String... lines) throws Exception {
    return testProject.setUnitContent(path, makeSource(lines));
  }

  @Override
  protected void setUp() throws Exception {
    testProject = new TestProject();
    System.setProperty("dartEditorTesting", "true");
    System.setProperty("dartEditorTesting.forceResolveUnit", "true");
  }

  @Override
  protected void tearDown() throws Exception {
    testProject.dispose();
    testProject = null;
    testUnit = null;
  }

}
