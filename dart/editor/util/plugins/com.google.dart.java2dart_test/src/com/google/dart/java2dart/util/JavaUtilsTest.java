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
package com.google.dart.java2dart.util;

import junit.framework.TestCase;

/**
 * Test for {@link JavaUtils}.
 */
public class JavaUtilsTest extends TestCase {

  public void test_getJdtMethodSignature() throws Exception {
    assertEquals(
        "Ltest/Main;.foo(DI)",
        JavaUtils.getJdtMethodSignature("test.Main", "foo", new String[] {"double", "int"}));
  }

  public void test_getJdtSignature() throws Exception {
    assertEquals("Ltest/Main;", JavaUtils.getJdtSignature("Ltest/Main;"));
    assertEquals("Ltest/Main;.myField", JavaUtils.getJdtSignature("Ltest/Main;.myField)I"));
    assertEquals("Ltest/Main;.myMethod(DI)", JavaUtils.getJdtSignature("Ltest/Main;.myMethod(DI)V"));
    assertEquals(
        "Ltest/Main;.myMethod()",
        JavaUtils.getJdtSignature("Ltest/Main;.myMethod()Ljava/lang/String"));
    assertEquals(
        "Ltest/Main;.myMethod(I)#myParameter",
        JavaUtils.getJdtSignature("Ltest/Main;.myMethod(I)V#myParameter"));
  }

  public void test_getJdtTypeName() throws Exception {
    assertEquals("Z", JavaUtils.getJdtTypeName("boolean"));
    assertEquals("B", JavaUtils.getJdtTypeName("byte"));
    assertEquals("C", JavaUtils.getJdtTypeName("char"));
    assertEquals("D", JavaUtils.getJdtTypeName("double"));
    assertEquals("F", JavaUtils.getJdtTypeName("float"));
    assertEquals("I", JavaUtils.getJdtTypeName("int"));
    assertEquals("J", JavaUtils.getJdtTypeName("long"));
    assertEquals("S", JavaUtils.getJdtTypeName("short"));
    assertEquals("V", JavaUtils.getJdtTypeName("void"));
    assertEquals("Ljava/lang/String;", JavaUtils.getJdtTypeName("java.lang.String"));
  }

  public void test_getRenamedJdtSignature() throws Exception {
    assertEquals(
        "Ltest/Test;.newName",
        JavaUtils.getRenamedJdtSignature("Ltest/Test;.oldName", "newName"));
    assertEquals(
        "Ltest/Main;.newName(DI)",
        JavaUtils.getRenamedJdtSignature("Ltest/Main;.oldName(DI)", "newName"));
    assertEquals(
        "Ltest/Main;.myMethod(DI)#newName",
        JavaUtils.getRenamedJdtSignature("Ltest/Main;.myMethod(DI)#oldName", "newName"));
  }

}
