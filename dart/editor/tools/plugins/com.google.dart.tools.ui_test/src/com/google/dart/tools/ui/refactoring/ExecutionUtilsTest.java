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
package com.google.dart.tools.ui.refactoring;

import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableObjectEx;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;

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
   * Test for {@link ExecutionUtils#runObjectCore(RunnableObjectEx)}.
   */
  public void test_runObjectCore() throws Exception {
    final String val = "value";
    String result = ExecutionUtils.runObjectCore(new RunnableObjectEx<String>() {
      @Override
      public String runObject() throws Exception {
        return val;
      }
    });
    assertSame(val, result);
  }

  /**
   * Test for {@link ExecutionUtils#runObjectCore(RunnableObjectEx)}.
   */
  public void test_runObjectCore_whenException() throws Exception {
    final Exception ex = new Exception();
    try {
      ExecutionUtils.runObjectCore(new RunnableObjectEx<String>() {
        @Override
        public String runObject() throws Exception {
          throw ex;
        }
      });
      fail();
    } catch (Throwable e) {
      CoreException coreException = (CoreException) e;
      assertSame(ex, coreException.getCause());
    }
  }

}
