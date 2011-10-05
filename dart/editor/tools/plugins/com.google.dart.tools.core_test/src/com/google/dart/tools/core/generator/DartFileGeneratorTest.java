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

import junit.framework.TestCase;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import java.io.IOException;

public abstract class DartFileGeneratorTest extends TestCase {

  public DartFileGeneratorTest() {
    super();
  }

  public DartFileGeneratorTest(String name) {
    super(name);
  }

  protected IProject getProject() throws CoreException {
    return TestFileUtil.getOrCreateDartProject(getClass().getSimpleName());
  }

  protected String readExpectedContent(String fileName) throws IOException {
    return TestFileUtil.readResource(getClass(), fileName);
  }
}
