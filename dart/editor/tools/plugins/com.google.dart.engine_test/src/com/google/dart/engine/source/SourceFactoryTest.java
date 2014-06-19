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

import java.io.File;
import java.net.URI;

public class SourceFactoryTest extends TestCase {
  public void test_creation() {
    assertNotNull(new SourceFactory());
  }

  public void test_fromEncoding_invalidUri() throws Exception {
    SourceFactory factory = new SourceFactory();
    try {
      factory.fromEncoding("#<:&%>");
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException exception) {
      // Expected
    }
  }

  public void test_fromEncoding_noResolver() throws Exception {
    SourceFactory factory = new SourceFactory();
    try {
      factory.fromEncoding("pfile:/does/not/exist.dart");
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException exception) {
      // Expected
    }
  }

  public void test_fromEncoding_tooShort() throws Exception {
    SourceFactory factory = new SourceFactory();
    try {
      factory.fromEncoding("#");
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException exception) {
      // Expected
    }
  }

  public void test_fromEncoding_unknownKind() throws Exception {
    SourceFactory factory = new SourceFactory();
    try {
      factory.fromEncoding("#file:/does/not/exist.dart");
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException exception) {
      // Expected
    }
  }

  public void test_fromEncoding_valid() throws Exception {
    final boolean[] invoked = {false};
    SourceFactory factory = new SourceFactory(new UriResolver() {
      @Override
      public Source fromEncoding(UriKind kind, URI uri) {
        invoked[0] = true;
        return new FileBasedSource(new File(uri), kind);
      }

      @Override
      public Source resolveAbsolute(URI uri) {
        return null;
      }
    });
    factory.fromEncoding("dfile:/does/not/exist.dart");
    assertTrue(invoked[0]);
  }

  public void test_resolveUri_absolute() throws Exception {
    final boolean[] invoked = {false};
    SourceFactory factory = new SourceFactory(new UriResolver() {
      @Override
      public Source fromEncoding(UriKind kind, URI uri) {
        return null;
      }

      @Override
      public Source resolveAbsolute(URI uri) {
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
      public Source fromEncoding(UriKind kind, URI uri) {
        return null;
      }

      @Override
      public Source resolveAbsolute(URI uri) {
        return null;
      }
    });
    String absolutePath = "/does/not/matter.dart";
    Source containingSource = new FileBasedSource(createFile("/does/not/exist.dart"));
    Source result = factory.resolveUri(containingSource, absolutePath);
    assertEquals(createFile(absolutePath).getAbsolutePath(), result.getFullName());
  }

  public void test_resolveUri_nonAbsolute_relative() throws Exception {
    SourceFactory factory = new SourceFactory(new UriResolver() {
      @Override
      public Source fromEncoding(UriKind kind, URI uri) {
        return null;
      }

      @Override
      public Source resolveAbsolute(URI uri) {
        return null;
      }
    });
    Source containingSource = new FileBasedSource(createFile("/does/not/have.dart"));
    Source result = factory.resolveUri(containingSource, "exist.dart");
    assertEquals(createFile("/does/not/exist.dart").getAbsolutePath(), result.getFullName());
  }

  public void test_restoreUri() throws Exception {
    File file1 = createFile("/some/file1.dart");
    File file2 = createFile("/some/file2.dart");
    final Source source1 = new FileBasedSource(file1);
    final Source source2 = new FileBasedSource(file2);
    final URI expected1 = new URI("http://www.google.com");
    SourceFactory factory = new SourceFactory(new UriResolver() {
      @Override
      public Source fromEncoding(UriKind kind, URI uri) {
        return null;
      }

      @Override
      public Source resolveAbsolute(URI uri) {
        return null;
      }

      @Override
      public URI restoreAbsolute(Source source) {
        if (source == source1) {
          return expected1;
        }
        return null;
      }
    });
    assertSame(expected1, factory.restoreUri(source1));
    assertSame(null, factory.restoreUri(source2));
  }
}
