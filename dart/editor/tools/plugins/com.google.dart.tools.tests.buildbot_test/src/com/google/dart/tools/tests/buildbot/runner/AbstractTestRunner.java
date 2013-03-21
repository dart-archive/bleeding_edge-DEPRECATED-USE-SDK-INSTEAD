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

package com.google.dart.tools.tests.buildbot.runner;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.junit.Ignore;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An abstract class to execute JUnit tests. Subclasses must implement the five abstract methods.
 * The call order is:
 * <p>
 * testsStarted()
 * <p>
 * testStarted()
 * <p>
 * testPassed() or testFailed()
 * <p>
 * testsFinished()
 * <p>
 */
public abstract class AbstractTestRunner {
  /**
   * A TestCase and elapsed time tuple. This is used to store the elapsed time information for slow
   * tests.
   */
  public static class TestTime implements Comparable<TestTime> {
    private TestCase test;
    private long elapsedTime;

    public TestTime(TestCase test, long elapsedTime) {
      this.test = test;
      this.elapsedTime = elapsedTime;
    }

    @Override
    public int compareTo(TestTime other) {
      if (other.elapsedTime == elapsedTime) {
        return 0;
      } else if (other.elapsedTime > elapsedTime) {
        return 1;
      } else {
        return -1;
      }
    }

    @Override
    public String toString() {
      return formatDouble(elapsedTime / 1000.0) + " sec, " + getTestId(test);
    }
  }

  public static String getTestId(TestCase test) {
    return test.getClass().getName() + "." + test.getName();
  }

  private static String formatDouble(double d) {
    NumberFormat nf = new DecimalFormat();
    nf.setMaximumFractionDigits(2);
    nf.setMinimumFractionDigits(2);
    return nf.format(d);
  }

  private Test mainTest;
  private static final long ONE_SEC_MILLIS = 1000;

  private static boolean isAnnotationIgnored(TestCase test) {
    try {
      Method m = test.getClass().getMethod(test.getName());
      Annotation a = m.getAnnotation(Ignore.class);

      return a != null;
    } catch (SecurityException e) {

    } catch (NoSuchMethodException e) {

    }

    return false;
  }

  public AbstractTestRunner(Test test) {
    this.mainTest = test;
  }

  /**
   * Run the tests. Return true if they succeed; false if they fail.
   * 
   * @return true if the tests succeed and false if they fail
   */
  public final boolean runTests() {
    List<TestCase> tests = filterTests(flattenTests(mainTest));

    testsStarted(tests);

    long totalStartTime = System.currentTimeMillis();

    List<TestResult> failures = new ArrayList<TestResult>();
    List<TestTime> slowTests = new ArrayList<TestTime>();

    for (TestCase test : tests) {
      testStarted(test);

      long startTime = System.nanoTime();
      TestResult result = test.run();
      long elapsedTimeMS = (System.nanoTime() - startTime) / (1000 * 1000);

      if (result.wasSuccessful()) {
        testPassed(test);

        if (elapsedTimeMS >= ONE_SEC_MILLIS) {
          slowTests.add(new TestTime(test, elapsedTimeMS));
        }
      } else {
        testFailed(test, result);
        failures.add(result);
      }
    }

    long totalTestTime = System.currentTimeMillis() - totalStartTime;

    testsFinished(tests, failures, slowTests, totalTestTime);

    return failures.size() == 0;
  }

  public final void setStatusFile(String filePath) {
    // TODO(devoncarew): read and use the status file

  }

  protected boolean filterTest(TestCase test) {
    if (isAnnotationIgnored(test)) {
      return true;
    }

    return false;
  }

  protected abstract void testFailed(TestCase test, TestResult result);

  protected abstract void testPassed(TestCase test);

  protected abstract void testsFinished(List<TestCase> allTests, List<TestResult> failures,
      List<TestTime> slowTests, long totalTime);

  protected abstract void testsStarted(List<TestCase> tests);

  protected abstract void testStarted(TestCase test);

  private List<TestCase> filterTests(List<TestCase> tests) {
    List<TestCase> copy = new ArrayList<TestCase>();

    for (TestCase test : tests) {
      if (!filterTest(test)) {
        copy.add(test);
      }
    }

    return copy;
  }

  private void flatten(Test test, List<TestCase> tests) {
    if (test instanceof TestCase) {
      tests.add((TestCase) test);
    } else if (test instanceof TestSuite) {
      TestSuite suite = (TestSuite) test;

      for (Test child : Collections.list(suite.tests())) {
        flatten(child, tests);
      }
    } else {
      System.out.println("Test instance not recognized: " + test);
    }
  }

  private List<TestCase> flattenTests(Test mainTest) {
    List<TestCase> tests = new ArrayList<TestCase>();
    flatten(mainTest, tests);
    return tests;
  }

}
