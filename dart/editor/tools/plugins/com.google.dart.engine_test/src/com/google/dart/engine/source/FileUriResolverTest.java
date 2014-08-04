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

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

import junit.framework.TestCase;

import java.net.URI;

public class FileUriResolverTest extends TestCase {
  public void test_creation() {
    assertNotNull(new FileUriResolver());
  }

  public void test_resolve_file() throws Exception {
    UriResolver resolver = new FileUriResolver();
    Source result = resolver.resolveAbsolute(new URI("file:/does/not/exist.dart"));
    assertNotNull(result);
    assertEquals(createFile("/does/not/exist.dart").getAbsolutePath(), result.getFullName());
  }

  public void test_resolve_nonFile() throws Exception {
    UriResolver resolver = new FileUriResolver();
    Source result = resolver.resolveAbsolute(new URI("dart:core"));
    assertNull(result);
  }
}
