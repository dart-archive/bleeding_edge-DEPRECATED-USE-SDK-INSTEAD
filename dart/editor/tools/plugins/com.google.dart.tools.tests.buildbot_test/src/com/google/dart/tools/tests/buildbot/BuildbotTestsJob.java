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

package com.google.dart.tools.tests.buildbot;

import com.google.dart.tools.tests.buildbot.runner.AbstractTestRunner;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestFailure;
import junit.framework.TestResult;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Run the given TestSuite, and optionally exit Eclipse when finished.
 */
class BuildbotTestsJob extends Job {

  private class JobTestRunner extends AbstractTestRunner {
    private IProgressMonitor monitor;

    public JobTestRunner(IProgressMonitor monitor, Test test) {
      super(test);

      this.monitor = monitor;
    }

    @Override
    protected boolean filterTest(TestCase test) {
      final List<String> proscribed = Arrays.asList(
      // This takes a long time to run, and should only be run during the build.
      "com.google.dart.tools.core.artifact.TestGenerateArtifacts.test_generate_SDK_index");

      String testId = getTestId(test);

      if (proscribed.contains(testId)) {
        return true;
      }

      if (testId.indexOf(".fail_") != -1) {
        return true;
      }

      return super.filterTest(test);
    }

    @Override
    protected void testFailed(TestCase test, TestResult result) {
      System.out.println(getTestId(test) + ": fail");
    }

    @Override
    protected void testPassed(TestCase test) {
      System.out.println(getTestId(test) + ": pass");
    }

    @Override
    protected void testsFinished(List<TestCase> allTests, List<TestResult> failures,
        List<TestTime> slowTests, long totalTime) {
      // shame the slow tests
      printHeader("slow tests");

      if (slowTests.size() > 0) {
        Collections.sort(slowTests);

        for (TestTime testTime : slowTests) {
          System.out.println(testTime.toString());
        }
      } else {
        System.out.println("No slow tests!");
      }

      // print any test failures w/ details
      printHeader("test summary");

      if (failures.size() > 0) {
        for (TestResult result : failures) {
          if (result.failureCount() > 0) {
            printFailure(result.failures().nextElement());
          } else {
            printFailure(result.errors().nextElement());
          }
        }
      }

      System.out.println(formatInt(allTests.size()) + " tests run; " + formatInt(failures.size())
          + " failures [" + formatDouble(totalTime / 1000.0) + " secs].");

      monitor.done();
    }

    @Override
    protected void testsStarted(List<TestCase> tests) {
      //monitor.beginTask(getName(), tests.size());
    }

    @Override
    protected void testStarted(TestCase test) {
      monitor.subTask(getTestId(test));
      monitor.worked(1);
    }
  }

  private static String formatDouble(double d) {
    NumberFormat nf = new DecimalFormat();
    nf.setMaximumFractionDigits(2);
    nf.setMinimumFractionDigits(2);
    return nf.format(d);
  }

  private static String formatInt(int i) {
    return NumberFormat.getIntegerInstance().format(i);
  }

  private boolean exitWhenFinished;
  private Test mainTest;

  public BuildbotTestsJob(boolean exitWhenFinished) {
    this(exitWhenFinished, TestAll.suite());
  }

  public BuildbotTestsJob(boolean exitWhenFinished, Test mainTest) {
    super("Running tests...");

    this.exitWhenFinished = exitWhenFinished;
    this.mainTest = mainTest;
  }

  @Override
  protected IStatus run(final IProgressMonitor monitor) {
    // First, refresh the workspace. Otherwise out-of-sync resources will cause problems.
    try {
      ResourcesPlugin.getWorkspace().getRoot().refreshLocal(
          IResource.DEPTH_INFINITE,
          new NullProgressMonitor());
    } catch (CoreException e) {
      BuildbotPlugin.getPlugin().log(e);
    }

    // Now, run the tests.
    JobTestRunner testRunner = new JobTestRunner(monitor, mainTest);

    boolean testsPassed = testRunner.runTests();

    if (exitWhenFinished) {
      int exitCode = testsPassed ? 0 : 1;

      // We do a hard exit.
      System.exit(exitCode);
    }

    return Status.OK_STATUS;
  }

  private void printFailure(TestFailure failure) {
    TestCase test = (TestCase) failure.failedTest();

    System.out.println(AbstractTestRunner.getTestId(test));
    printStackTrace(failure.trace().split("\n"));
    System.out.println();
  }

  private void printHeader(String title) {
    System.out.println("\n--- " + title + " ---");
  }

  private void printStackTrace(String[] lines) {
    for (String line : lines) {
      if (line.startsWith("\tat sun.reflect.NativeMethod")
          || line.startsWith("\tat java.lang.reflect.Method")
          || line.startsWith("\tat junit.framework.TestCase.runTest")
          || line.startsWith("\tat junit.framework.TestCase.runBare")) {
        return;
      }

      System.out.println(line);
    }
  }

}
