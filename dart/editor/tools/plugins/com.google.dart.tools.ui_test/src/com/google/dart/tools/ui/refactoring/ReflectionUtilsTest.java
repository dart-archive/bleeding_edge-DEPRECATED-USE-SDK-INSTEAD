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

import com.google.dart.tools.internal.corext.refactoring.util.ReflectionUtils;

import junit.framework.TestCase;

/**
 * Test for {@link ReflectionUtils}.
 */
public class ReflectionUtilsTest extends TestCase {

  @SuppressWarnings("unused")
  private static class ClassTestA {
    static void methodD() throws Exception {
      throw new Exception("d");
    }

    private static String methodA() {
      return "0";
    }

    String methodB() {
      return "1";
    }

    String methodC(String a, int b) {
      return a + Integer.toString(b);
    }

  }

  private static class ClassTestB extends ClassTestA {
  }

  /**
   * Test for {@link ReflectionUtils#getClassName(Class)}.
   */
  public void test_getClassName() throws Exception {
    assertEquals("boolean", ReflectionUtils.getClassName(Boolean.TYPE));
    assertEquals("int", ReflectionUtils.getClassName(Integer.TYPE));
    assertEquals("java.lang.String", ReflectionUtils.getClassName(String.class));
  }

  /**
   * Test for {@link ReflectionUtils#invokeMethod(Object, String, Object...)}.
   */
  public void test_invokeMethod() throws Exception {
    // static, no parameters
    {
      String result = ReflectionUtils.invokeMethod(ClassTestA.class, "methodA()");
      assertEquals("0", result);
    }
    // instance, no parameters
    {
      String result = ReflectionUtils.invokeMethod(new ClassTestA(), "methodB()");
      assertEquals("1", result);
    }
    // instance, no parameters, inherited
    {
      String result = ReflectionUtils.invokeMethod(new ClassTestB(), "methodB()");
      assertEquals("1", result);
    }
    // instance, with parameters
    {
      String result = ReflectionUtils.invokeMethod(
          new ClassTestA(),
          "methodC(java.lang.String,int)",
          "sss",
          2);
      assertEquals("sss2", result);
    }
    // throws exception
    try {
      ReflectionUtils.invokeMethod(ClassTestA.class, "methodD()");
      fail();
    } catch (Exception e) {
      assertEquals("d", e.getMessage());
    }
    // no such method
    try {
      ReflectionUtils.invokeMethod(ClassTestA.class, "noSuchMethod()");
      fail();
    } catch (IllegalArgumentException e) {
      assertEquals(
          "noSuchMethod() in class com.google.dart.tools.ui.refactoring.ReflectionUtilsTest$ClassTestA",
          e.getMessage());
    }
  }

}
