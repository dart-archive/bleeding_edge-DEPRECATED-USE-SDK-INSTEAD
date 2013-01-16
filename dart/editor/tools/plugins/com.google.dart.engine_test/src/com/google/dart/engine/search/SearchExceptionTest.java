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
package com.google.dart.engine.search;

import com.google.dart.engine.EngineTestCase;

public class SearchExceptionTest extends EngineTestCase {
  public void test_newDefault() throws Exception {
    SearchException exception = new SearchException();
    assertNull(exception.getMessage());
    assertNull(exception.getCause());
  }

  public void test_newWithCause() throws Exception {
    Throwable cause = new Exception("msg");
    SearchException exception = new SearchException(cause);
    assertEquals("java.lang.Exception: msg", exception.getMessage());
    assertSame(cause, exception.getCause());
  }

  public void test_newWithMessage() throws Exception {
    SearchException exception = new SearchException("msg");
    assertEquals("msg", exception.getMessage());
    assertNull(exception.getCause());
  }
}
