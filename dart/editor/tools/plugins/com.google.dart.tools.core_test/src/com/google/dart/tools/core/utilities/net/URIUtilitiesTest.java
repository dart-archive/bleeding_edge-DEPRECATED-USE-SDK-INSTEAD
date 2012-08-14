/*
 * Copyright 2012, the Dart project authors.
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
package com.google.dart.tools.core.utilities.net;

import com.google.dart.tools.core.test.util.TestUtilities;

import junit.framework.TestCase;

import java.net.URI;

public class URIUtilitiesTest extends TestCase {
  public void test_URIUtilities_makeAbsolute_absolute() throws Exception {
    URI uri = TestUtilities.getLogFile().getAbsoluteFile().toURI();
    assertEquals(uri, URIUtilities.makeAbsolute(uri));
  }

  public void test_URIUtilities_makeAbsolute_null() throws Exception {
    assertNull(URIUtilities.makeAbsolute(null));
  }

  public void test_URIUtilities_makeAbsolute_relative_fileScheme() throws Exception {
    URI uri = new URI("file", "foo/bar", null);
    URI result = URIUtilities.makeAbsolute(uri);
    assertNotNull(result);
    assertTrue(result.toString(), result.toString().endsWith("/foo/bar"));
  }

  public void test_URIUtilities_makeAbsolute_relative_fileWithSpace() throws Exception {
    URI uri = new URI("file", "fo o/bar", null);
    URI result = URIUtilities.makeAbsolute(uri);
    assertNotNull(result);
    assertTrue(result.toString(), result.toString().endsWith("/fo%20o/bar"));
  }

  public void test_URIUtilities_makeAbsolute_relative_nonFileScheme() throws Exception {
    URI uri = new URI("http", "foo/bar", null);
    assertEquals(uri, URIUtilities.makeAbsolute(uri));
  }

  public void test_URIUtilities_makeAbsolute_relative_noScheme() throws Exception {
    URI uri = new URI(null, "foo/bar", null);
    URI result = URIUtilities.makeAbsolute(uri);
    assertNotNull(result);
    assertTrue(result.toString(), result.toString().endsWith("/foo/bar"));
  }

  public void test_URIUtilities_safelyResolveDartUri_invalidDart() throws Exception {
    String path = "core/core_impl.dart/core_impl.dart";
    URI uri = new URI("dart", path, null);
    URI result = URIUtilities.safelyResolveDartUri(uri);
    assertNotNull(result);
    assertTrue(result.toString(), result.toString().endsWith(path));
  }

  public void test_URIUtilities_safelyResolveDartUri_nonDart() throws Exception {
    URI uri = new URI("file", "relative/path.dart", null);
    URI result = URIUtilities.safelyResolveDartUri(uri);
    assertEquals(uri, result);
  }

  public void test_URIUtilities_safelyResolveDartUri_validDart() throws Exception {
    String path = "core.lib";
    URI uri = new URI("dart", path, null);
    URI result = URIUtilities.safelyResolveDartUri(uri);
    assertNotNull(result);
    assertTrue(result.toString(), result.toString().endsWith(path));
  }

  public void test_URIUtilities_uriEncode_encodeSpace() throws Exception {
    String result = URIUtilities.uriEncode("foo bar");
    assertNotNull(result);
    assertEquals("foo%20bar", result);
  }

  public void test_URIUtilities_uriEncode_noEncode() throws Exception {
    String result = URIUtilities.uriEncode("fooBar");
    assertNotNull(result);
    assertEquals("fooBar", result);
  }

}
