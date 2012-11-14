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

import java.io.File;
import java.net.URI;

public class PackageUriResolverTest extends TestCase {
  public void test_creation() {
    File directory = new File("/does/not/exist/packages");
    assertNotNull(new PackageUriResolver(directory));
  }

  public void test_resolve_nonPackage() throws Exception {
    SourceFactory factory = new SourceFactory();
    File directory = new File("/does/not/exist/packages");
    UriResolver resolver = new PackageUriResolver(directory);
    Source result = resolver.resolve(factory, null, new URI("dart:core"));
    assertNull(result);
  }

  public void test_resolve_package() throws Exception {
    SourceFactory factory = new SourceFactory();
    File directory = new File("/does/not/exist/packages");
    UriResolver resolver = new PackageUriResolver(directory);
    Source result = resolver.resolve(factory, null, new URI("package:third/party/library.dart"));
    assertNotNull(result);
    assertEquals("/does/not/exist/packages/third/party/library.dart", result.getFullName());
  }
}
