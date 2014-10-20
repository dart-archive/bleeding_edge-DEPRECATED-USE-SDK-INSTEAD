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
package com.google.dart.engine.source;

import junit.framework.TestCase;

public class UriKindTest extends TestCase {
  public void test_fromEncoding() {
    assertSame(UriKind.DART_URI, UriKind.fromEncoding('d'));
    assertSame(UriKind.FILE_URI, UriKind.fromEncoding('f'));
    assertSame(UriKind.JAVA_URI, UriKind.fromEncoding('j'));
    assertSame(UriKind.PACKAGE_URI, UriKind.fromEncoding('p'));
    assertSame(null, UriKind.fromEncoding('X'));
  }

  public void test_getEncoding() {
    assertEquals('d', UriKind.DART_URI.getEncoding());
    assertEquals('f', UriKind.FILE_URI.getEncoding());
    assertEquals('j', UriKind.JAVA_URI.getEncoding());
    assertEquals('p', UriKind.PACKAGE_URI.getEncoding());
  }
}
