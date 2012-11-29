/*
 * Copyright 2012 Dart project authors.
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
package com.google.dart.tools.ui.swtbot;

import com.google.dart.tools.ui.swtbot.performance.SwtBotPerformance;
import com.google.dart.tools.ui.swtbot.views.ProblemsViewHelper;
import com.google.dart.tools.ui.test.model.Workspace.Sample;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Sample tests for the Dart Editor. This set of tests pulls in each of the samples just like the
 * user would, and runs them, making various assertions.
 * 
 * @see TestAll
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public final class SamplesUITest extends AbstractDartEditorTest {

  @Test
  public void test_clock() throws Exception {
    testSample(Sample.CLOCK);
  }

  @Test
  public void test_solar() throws Exception {
    testSample(Sample.SOLAR);
  }

  @Test
  public void test_solar3d() throws Exception {
    testSample(Sample.SOLAR_3D);
  }

  @Test
  public void test_sunflower() throws Exception {
    testSample(Sample.SUNFLOWER);
  }

  @Test
  public void test_swipe() throws Exception {
    testSample(Sample.SWIPE);
  }

  @Test
  public void test_time() throws Exception {
    testSample(Sample.TIME);
  }

  @Test
  public void test_todo_MVC() throws Exception {
    testSample(Sample.TODO_MVC);
  }

  private void testSample(Sample sample) throws CoreException {
    try {

      sample.open();

      SwtBotPerformance.waitForResults(bot);

      //TODO (pquitslund): a stand-in for a proper wait
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

      new ProblemsViewHelper(bot).assertNoProblems();

    } finally {
      sample.dispose();
    }
  }

//  @Test
//  public void testSample_clock() throws Exception {
//    openAndLaunchLibrary(DartLib.CLOCK_SAMPLE, true, true);
//  }
//
//  @Test
//  public void testSample_slider() throws Exception {
//    openAndLaunchLibrary(DartLib.SLIDER_SAMPLE, true, true);
//  }
//
//  @Test
//  public void testSample_sunflower() throws Exception {
//    openAndLaunchLibrary(DartLib.SUNFLOWER_SAMPLE, true, true);
//  }
//
//  @Test
//  public void testSample_timeServer() throws Exception {
//    openAndLaunchLibrary(DartLib.TIME_SERVER_SAMPLE, false, true);
//  }
//
//  @Test
//  public void testSamples_compileAllSamples() throws Exception {
//    new SamplesTest(new SamplesTest.Listener() {
//      @Override
//      public void logParse(long elapseTime, String... comments) {
//        long start = System.currentTimeMillis() - elapseTime;
//        SwtBotPerformance.COMPILER_PARSE.log(start, comments);
//      }
//    }).testSamples(DartLib.getSamplesDir());
//  }

}
