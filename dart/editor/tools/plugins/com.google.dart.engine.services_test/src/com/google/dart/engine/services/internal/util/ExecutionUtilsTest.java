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
package com.google.dart.engine.services.internal.util;

import com.google.common.collect.Lists;
import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.utilities.logging.Logger;

import junit.framework.TestCase;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Test for {@link ExecutionUtils}.
 */
public class ExecutionUtilsTest extends TestCase {

  /**
   * Test for {@link ExecutionUtils#propagate(Throwable)}.
   */
  public void test_propagate() throws Exception {
    // when we throw Exception, it is thrown as is
    {
      Throwable toThrow = new Exception();
      try {
        ExecutionUtils.propagate(toThrow);
      } catch (Throwable e) {
        assertSame(toThrow, e);
      }
    }
    // when we throw Error, it is thrown as is
    {
      Throwable toThrow = new Error();
      try {
        ExecutionUtils.propagate(toThrow);
      } catch (Throwable e) {
        assertSame(toThrow, e);
      }
    }
    // coverage: for return from propagate()
    {
      String key = "de.ExecutionUtils.propagate().forceReturn";
      System.setProperty(key, "true");
      try {
        Throwable toThrow = new Exception();
        Throwable result = ExecutionUtils.propagate(toThrow);
        assertSame(null, result);
      } finally {
        System.clearProperty(key);
      }
    }
    // coverage: for InstantiationException
    {
      String key = "de.ExecutionUtils.propagate().InstantiationException";
      System.setProperty(key, "true");
      try {
        Throwable toThrow = new Exception();
        Throwable result = ExecutionUtils.propagate(toThrow);
        assertSame(null, result);
      } finally {
        System.clearProperty(key);
      }
    }
    // coverage: for IllegalAccessException
    {
      String key = "de.ExecutionUtils.propagate().IllegalAccessException";
      System.setProperty(key, "true");
      try {
        Throwable toThrow = new Exception();
        Throwable result = ExecutionUtils.propagate(toThrow);
        assertSame(null, result);
      } finally {
        System.clearProperty(key);
      }
    }
  }

  /**
   * Test for {@link ExecutionUtils#runIgnore(RunnableEx)}.
   */
  public void test_runIgnore() throws Exception {
    boolean success;
    // no exception
    final AtomicBoolean executed = new AtomicBoolean();
    success = ExecutionUtils.runIgnore(new RunnableEx() {
      @Override
      public void run() throws Exception {
        executed.set(true);
      }
    });
    assertTrue(executed.get());
    assertTrue(success);
    // with exception
    success = ExecutionUtils.runIgnore(new RunnableEx() {
      @Override
      public void run() throws Exception {
        throw new Exception();
      }
    });
    assertFalse(success);
  }

  public void test_runLog() throws Exception {
    AnalysisEngine engine = AnalysisEngine.getInstance();
    Logger originalLogger = engine.getLogger();
    final List<Throwable> loggedErrors = Lists.newArrayList();
    try {
      engine.setLogger(new Logger.NullLogger() {
        @Override
        public void logError(Throwable exception) {
          loggedErrors.add(exception);
        }
      });
      // no exception
      ExecutionUtils.runLog(new RunnableEx() {
        @Override
        public void run() throws Exception {
        }
      });
      assertThat(loggedErrors).isEmpty();
      // throw exception
      final Exception exceptionToThrow = new Exception();
      ExecutionUtils.runLog(new RunnableEx() {
        @Override
        public void run() throws Exception {
          throw exceptionToThrow;
        }
      });
      assertThat(loggedErrors).containsExactly(exceptionToThrow);
    } finally {
      engine.setLogger(originalLogger);
    }
  }

  /**
   * Test for {@link ExecutionUtils#runObject(RunnableObjectEx)}.
   */
  public void test_runObject() throws Exception {
    final String val = "value";
    String result = ExecutionUtils.runObject(new RunnableObjectEx<String>() {
      @Override
      public String runObject() throws Exception {
        return val;
      }
    });
    assertSame(val, result);
  }

  /**
   * Test for {@link ExecutionUtils#runObject(RunnableObjectEx)}.
   */
  public void test_runObject_whenException() throws Exception {
    final Exception ex = new Exception();
    try {
      ExecutionUtils.runObject(new RunnableObjectEx<String>() {
        @Override
        public String runObject() throws Exception {
          throw ex;
        }
      });
      fail();
    } catch (Throwable e) {
      assertSame(ex, e);
    }
  }

  /**
   * Test for {@link ExecutionUtils#runObjectIgnore(RunnableObjectEx, Object)}.
   */
  public void test_runObjectIgnore() throws Exception {
    final String val = "value";
    String result = ExecutionUtils.runObjectIgnore(new RunnableObjectEx<String>() {
      @Override
      public String runObject() throws Exception {
        return val;
      }
    }, null);
    assertSame(val, result);
  }

  /**
   * Test for {@link ExecutionUtils#runObjectIgnore(RunnableObjectEx, Object)}.
   */
  public void test_runObjectIgnore_whenException() throws Exception {
    String defaultValue = "def";
    String result = ExecutionUtils.runObjectIgnore(new RunnableObjectEx<String>() {
      @Override
      public String runObject() throws Exception {
        throw new Exception();
      }
    }, defaultValue);
    assertSame(defaultValue, result);
  }

  // XXX
  public void test_runRethrow() throws Exception {
    // no exception
    ExecutionUtils.runRethrow(new RunnableEx() {
      @Override
      public void run() throws Exception {
      }
    });
    // throw exception
    final Exception exceptionToThrow = new Exception();
    try {
      ExecutionUtils.runRethrow(new RunnableEx() {
        @Override
        public void run() throws Exception {
          throw exceptionToThrow;
        }
      });
    } catch (Throwable e) {
      assertSame(exceptionToThrow, e);
    }
  }
}
