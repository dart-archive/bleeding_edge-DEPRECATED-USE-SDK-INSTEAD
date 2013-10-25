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
package com.google.dart.engine.scanner;

import junit.framework.TestCase;

public class CharSequenceReaderTest extends TestCase {
  public void test_advance() {
    CharSequenceReader reader = new CharSequenceReader("x");
    assertEquals('x', reader.advance());
    assertEquals(-1, reader.advance());
    assertEquals(-1, reader.advance());
  }

  public void test_creation() {
    assertNotNull(new CharSequenceReader("x"));
  }

  public void test_getOffset() {
    CharSequenceReader reader = new CharSequenceReader("x");
    assertEquals(-1, reader.getOffset());
    reader.advance();
    assertEquals(0, reader.getOffset());
    reader.advance();
    assertEquals(0, reader.getOffset());
  }

  public void test_getString() {
    CharSequenceReader reader = new CharSequenceReader("xyzzy");
    reader.setOffset(3);
    assertEquals("yzz", reader.getString(1, 0));
    assertEquals("zzy", reader.getString(2, 1));
  }

  public void test_peek() {
    CharSequenceReader reader = new CharSequenceReader("xy");
    assertEquals('x', reader.peek());
    assertEquals('x', reader.peek());
    reader.advance();
    assertEquals('y', reader.peek());
    assertEquals('y', reader.peek());
    reader.advance();
    assertEquals(-1, reader.peek());
    assertEquals(-1, reader.peek());
  }

  public void test_setOffset() {
    CharSequenceReader reader = new CharSequenceReader("xyz");
    reader.setOffset(2);
    assertEquals(2, reader.getOffset());
  }
}
