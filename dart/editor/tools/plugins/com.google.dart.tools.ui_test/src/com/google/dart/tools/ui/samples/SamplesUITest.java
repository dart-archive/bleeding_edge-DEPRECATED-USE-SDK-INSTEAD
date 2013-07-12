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
package com.google.dart.tools.ui.samples;

import com.google.dart.ui.test.UITestCase;
import com.google.dart.ui.test.model.Workspace.Sample;

import org.eclipse.core.runtime.CoreException;

/**
 * Test samples.
 */
public final class SamplesUITest extends UITestCase {

  public void test_clock() throws Exception {
    testSample(Sample.CLOCK);
  }

  public void test_solar() throws Exception {
    testSample(Sample.SOLAR);
  }

  public void test_sunflower() throws Exception {
    testSample(Sample.SUNFLOWER);
  }

  public void test_swipe() throws Exception {
    testSample(Sample.SWIPE);
  }

  public void test_time() throws Exception {
    testSample(Sample.TIME);
  }

  public void test_todo_MVC() throws Exception {
    testSample(Sample.TODO_MVC);
  }

  private void testSample(Sample sample) throws CoreException {

    try {

      sample.open();

      //TODO (pquitslund): add problem marker assertions

      //// ???: maybe: project.getMarkers() ....  or project.assertNoMarkers(IPredicate)
      //// Workspace.assertNoMarkers()

    } finally {
      sample.dispose();
    }
  }
}
