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

public class SourceFactoryTest extends TestCase {
  public void test_creation() {
    assertNotNull(new SourceFactory());
  }

  public void test_forFile() {
    SourceFactory factory = new SourceFactory();
    File file = new File("/does/not/exist.dart");
    Source result = factory.forFile(file);
    assertNotNull(result);
    assertEquals(file.getAbsolutePath(), result.getFullName());
    assertFalse(result.isInSystemLibrary());
  }

  public void test_resolveUri_absolute() throws Exception {
    final boolean[] invoked = {false};
    SourceFactory factory = new SourceFactory(new UriResolver() {
      @Override
      protected Source resolveAbsolute(SourceFactory factory, URI uri) {
        invoked[0] = true;
        return null;
      }
    });
    factory.resolveUri(null, "dart:core");
    assertTrue(invoked[0]);
  }

  public void test_resolveUri_nonAbsolute_absolute() throws Exception {
    SourceFactory factory = new SourceFactory(new UriResolver() {
      @Override
      protected Source resolveAbsolute(SourceFactory factory, URI uri) {
        return null;
      }
    });
    String absolutePath = "/does/not/matter.dart";
    Source containingSource = factory.forFile(new File("/does/not/exist.dart"));
    Source result = factory.resolveUri(containingSource, absolutePath);
    assertEquals(absolutePath, result.getFullName());
  }

  public void test_resolveUri_nonAbsolute_relative() throws Exception {
    SourceFactory factory = new SourceFactory(new UriResolver() {
      @Override
      protected Source resolveAbsolute(SourceFactory factory, URI uri) {
        return null;
      }
    });
    Source containingSource = factory.forFile(new File("/does/not/have.dart"));
    Source result = factory.resolveUri(containingSource, "exist.dart");
    assertEquals("/does/not/exist.dart", result.getFullName());
  }
}
