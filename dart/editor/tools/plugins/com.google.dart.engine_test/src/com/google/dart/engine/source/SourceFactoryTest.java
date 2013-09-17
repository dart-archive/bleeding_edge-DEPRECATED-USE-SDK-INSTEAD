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
      public Source fromEncoding(ContentCache contentCache, UriKind kind, URI uri) {
        invoked[0] = true;
        return new FileBasedSource(contentCache, new File(uri), kind);
      }

      @Override
      public Source resolveAbsolute(ContentCache contentCache, URI uri) {
        return null;
      }
    });
    factory.fromEncoding("dfile:/does/not/exist.dart");
    assertTrue(invoked[0]);
  }

  public void test_fromEncoding_valid_packageSelf() throws Exception {
    final boolean[] invoked = {false};
    SourceFactory factory = new SourceFactory(new UriResolver() {
      @Override
      public Source fromEncoding(ContentCache contentCache, UriKind kind, URI uri) {
        assertSame(UriKind.PACKAGE_SELF_URI, kind);
        invoked[0] = true;
        return new FileBasedSource(contentCache, new File(uri), kind);
      }

      @Override
      public Source resolveAbsolute(ContentCache contentCache, URI uri) {
        return null;
      }
    });
    factory.fromEncoding("sfile:/does/not/exist.dart");
    assertTrue(invoked[0]);
  }

  public void test_resolveUri_absolute() throws Exception {
    final boolean[] invoked = {false};
    SourceFactory factory = new SourceFactory(new UriResolver() {
      @Override
      public Source fromEncoding(ContentCache contentCache, UriKind kind, URI uri) {
        return null;
      }

      @Override
      public Source resolveAbsolute(ContentCache contentCache, URI uri) {
        invoked[0] = true;
        return null;
      }
    });
    factory.resolveUri(null, "dart:core");
    assertTrue(invoked[0]);
  }

  public void test_resolveUri_nonAbsolute_absolute() throws Exception {
    ContentCache contentCache = new ContentCache();
    SourceFactory factory = new SourceFactory(contentCache, new UriResolver() {
      @Override
      public Source fromEncoding(ContentCache contentCache, UriKind kind, URI uri) {
        return null;
      }

      @Override
      public Source resolveAbsolute(ContentCache contentCache, URI uri) {
        return null;
      }
    });
    String absolutePath = "/does/not/matter.dart";
    Source containingSource = new FileBasedSource(contentCache, createFile("/does/not/exist.dart"));
    Source result = factory.resolveUri(containingSource, absolutePath);
    assertEquals(createFile(absolutePath).getAbsolutePath(), result.getFullName());
  }

  public void test_resolveUri_nonAbsolute_relative() throws Exception {
    ContentCache contentCache = new ContentCache();
    SourceFactory factory = new SourceFactory(contentCache, new UriResolver() {
      @Override
      public Source fromEncoding(ContentCache contentCache, UriKind kind, URI uri) {
        return null;
      }

      @Override
      public Source resolveAbsolute(ContentCache contentCache, URI uri) {
        return null;
      }
    });
    Source containingSource = new FileBasedSource(contentCache, createFile("/does/not/have.dart"));
    Source result = factory.resolveUri(containingSource, "exist.dart");
    assertEquals(createFile("/does/not/exist.dart").getAbsolutePath(), result.getFullName());
  }

  public void test_restoreUri() throws Exception {
    File file1 = createFile("/some/file1.dart");
    File file2 = createFile("/some/file2.dart");
    final Source source1 = new FileBasedSource(null, file1);
    final Source source2 = new FileBasedSource(null, file2);
    final URI expected1 = new URI("http://www.google.com");
    SourceFactory factory = new SourceFactory(new UriResolver() {
      @Override
      public Source fromEncoding(ContentCache contentCache, UriKind kind, URI uri) {
        return null;
      }

      @Override
      public Source resolveAbsolute(ContentCache contentCache, URI uri) {
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

  public void test_setContents() {
    ContentCache contentCache = new ContentCache();
    TestSourceFactory factory = new TestSourceFactory(contentCache);
    File file = createFile("/does/not/exist.dart");
    Source source = new FileBasedSource(contentCache, file);
    assertNull(factory.getContents(source));
    assertNull(factory.getModificationStamp(source));

    String contents = "library lib;";
    assertTrue(factory.setContents(source, contents));
    assertEquals(contents, factory.getContents(source));
    assertNotNull(factory.getModificationStamp(source));

    assertFalse(factory.setContents(source, contents));

    assertTrue(factory.setContents(source, null));
    assertNull(factory.getContents(source));
    assertNull(factory.getModificationStamp(source));

    assertFalse(factory.setContents(source, null));
  }

  public void test_sharedContents() {
    ContentCache contentCache = new ContentCache();

    TestSourceFactory factory1 = new TestSourceFactory(contentCache);
    File file = createFile("/does/not/exist.dart");
    Source source1 = new FileBasedSource(contentCache, file);
    assertNull(factory1.getContents(source1));
    String contents = "library lib;";
    factory1.setContents(source1, contents);
    assertEquals(contents, factory1.getContents(source1));

    TestSourceFactory factory2 = new TestSourceFactory(contentCache);
    Source source2 = new FileBasedSource(contentCache, file);
    assertEquals(contents, factory2.getContents(source2));
  }

  public void test_sharedContentsNot() {
    ContentCache contentCache1 = new ContentCache();
    TestSourceFactory factory1 = new TestSourceFactory(contentCache1);
    File file = createFile("/does/not/exist.dart");
    Source source1 = new FileBasedSource(contentCache1, file);
    assertNull(factory1.getContents(source1));
    String contents = "library lib;";
    factory1.setContents(source1, contents);
    assertEquals(contents, factory1.getContents(source1));

    ContentCache contentCache2 = new ContentCache();
    TestSourceFactory factory2 = new TestSourceFactory(contentCache2);
    Source source2 = new FileBasedSource(contentCache2, file);
    assertNull(factory2.getContents(source2));
  }
}
