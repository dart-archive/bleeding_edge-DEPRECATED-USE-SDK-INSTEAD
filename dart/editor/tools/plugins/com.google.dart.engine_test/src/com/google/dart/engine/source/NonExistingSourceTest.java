/*
 * Copyright (c) 2014, the Dart project authors.
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

import java.net.URI;

public class NonExistingSourceTest extends TestCase {
  private Source source = new NonExistingSource("/foo/bar/baz.dart", UriKind.PACKAGE_URI);

  public void test_access() throws Exception {
    assertFalse(source.exists());
    assertSame(UriKind.PACKAGE_URI, source.getUriKind());
    assertEquals("/foo/bar/baz.dart", source.getFullName());
    assertEquals("/foo/bar/baz.dart", source.getShortName());
    assertEquals(0, source.getModificationStamp());
    assertFalse(source.isInSystemLibrary());
  }

  public void test_getContents() throws Exception {
    try {
      source.getContents();
      fail();
    } catch (UnsupportedOperationException e) {
    }
  }

  @SuppressWarnings("deprecation")
  public void test_getContentsToReceiver() throws Exception {
    try {
      source.getContentsToReceiver(null);
      fail();
    } catch (UnsupportedOperationException e) {
    }
  }

  public void test_getEncoding() throws Exception {
    try {
      source.getEncoding();
      fail();
    } catch (UnsupportedOperationException e) {
    }
  }

  public void test_resolveRelative() throws Exception {
    try {
      source.resolveRelativeUri(new URI("qux.dart"));
      fail();
    } catch (UnsupportedOperationException e) {
    }
  }
}
