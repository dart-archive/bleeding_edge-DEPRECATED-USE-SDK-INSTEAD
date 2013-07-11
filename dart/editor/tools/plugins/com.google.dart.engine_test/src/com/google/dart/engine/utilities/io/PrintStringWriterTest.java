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
package com.google.dart.engine.utilities.io;

import junit.framework.TestCase;

public class PrintStringWriterTest extends TestCase {
  PrintStringWriter writer;

  public void test_length() throws Exception {
    writer.setLength(1);
    assertEquals(1, writer.getLength());

    writer.setLength(10);
    assertEquals(10, writer.getLength());

    writer.setLength(0);
    assertEquals(0, writer.getLength());
  }

  public void test_print_int() throws Exception {
    writer.print(1, 0, PrintStringWriter.ALIGN_LEFT);
    assertEquals(1, writer.getLength());
    assertEquals("1", writer.toString());
  }

  public void test_print_int_left() throws Exception {
    writer.print(1, 3, PrintStringWriter.ALIGN_LEFT);
    assertEquals(3, writer.getLength());
    assertEquals("1  ", writer.toString());
  }

  public void test_print_int_right() throws Exception {
    writer.print(1, 3, PrintStringWriter.ALIGN_RIGHT);
    assertEquals(3, writer.getLength());
    assertEquals("  1", writer.toString());
  }

  public void test_print_String() throws Exception {
    writer.print("str", 0, PrintStringWriter.ALIGN_LEFT);
    assertEquals(3, writer.getLength());
    assertEquals("str", writer.toString());
  }

  public void test_print_String_left() throws Exception {
    writer.print("str", 6, PrintStringWriter.ALIGN_LEFT);
    assertEquals(6, writer.getLength());
    assertEquals("str   ", writer.toString());
  }

  public void test_print_String_right() throws Exception {
    writer.print("str", 6, PrintStringWriter.ALIGN_RIGHT);
    assertEquals(6, writer.getLength());
    assertEquals("   str", writer.toString());
  }

  public void test_printMultiple() throws Exception {
    writer.printMultiple(3, "str");
    assertEquals(9, writer.getLength());
    assertEquals("strstrstr", writer.toString());
  }

  public void test_printMultiple_repeatNothing() throws Exception {
    writer.printMultiple(10, "");
    assertEquals(0, writer.getLength());
    assertEquals("", writer.toString());
  }

  public void test_printMultiple_repeatZero() throws Exception {
    writer.printMultiple(0, "str");
    assertEquals(0, writer.getLength());
    assertEquals("", writer.toString());
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    writer = new PrintStringWriter();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    writer = null;
  }
}
