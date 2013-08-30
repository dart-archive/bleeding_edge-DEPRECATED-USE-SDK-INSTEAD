/*
 * Copyright (c) 2013, the Dart project authors.
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

import com.google.dart.tools.core.test.util.PlainTestProject;

import junit.framework.TestCase;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;

public class PolymerSampleTest extends TestCase {

  public void testGenerateSample() throws Exception {
    PlainTestProject testProject = new PlainTestProject("sample");
    IProject project = testProject.getProject();

    try {
      PolymerSample generator = new PolymerSample();

      IFile mainFile = generator.generateInto(project, "foo");

      assertEquals("web/foo.html", mainFile.getProjectRelativePath().toPortableString());
      assertTrue(mainFile.exists());

      IContainer parent = mainFile.getParent();

      IFile clickCounterFile = parent.getFile(new Path("clickcounter.dart"));
      assertTrue(clickCounterFile.exists());

      IFile fooDartFile = parent.getFile(new Path("foo.dart"));
      assertTrue(fooDartFile.exists());
      // TODO(keertip): fix this to analyze against package directory built during the 
      // build process.
      // GeneratorUtils.assertNoAnalysisErrors(fooDartFile);
    } finally {
      testProject.dispose();
    }
  }

}
