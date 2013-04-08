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

import com.google.dart.engine.utilities.io.FileUtilities2;

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

import junit.framework.TestCase;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

public class PackageUriResolverTest extends TestCase {

  public void test_absolute_vs_canonical() throws Exception {
    File directory = createFile("/does/not/exist/packages");
    // Cannot compare paths on Windows because this
    //    assertEquals(directory.getAbsolutePath(), directory.getCanonicalPath());
    // results in 
    //    expected:<[e]:\does\not\exist\pac...> but was:<[E]:\does\not\exist\pac...>
    assertEquals(directory.getAbsoluteFile(), directory.getCanonicalFile());
  }

  public void test_creation() {
    File directory = createFile("/does/not/exist/packages");
    assertNotNull(new PackageUriResolver(directory));
  }

  public void test_resolve_canonical() throws Exception {

    if (!FileUtilities2.isSymLinkSupported()) {
      System.out.println("Skipping " + getClass().getSimpleName() + " test_resolve_canonical");
      return;
    }

    File libDir = FileUtilities2.createTempDir("lib").getCanonicalFile();
    File otherDir = FileUtilities2.createTempDir("other").getCanonicalFile();
    File packagesDir = FileUtilities2.createTempDir("packages");

    // Create symlink packages/pkg1 --> lib
    File pkg1Dir = new File(packagesDir, "pkg1");
    FileUtilities2.createSymLink(libDir, pkg1Dir);

    // Create symlink packages/pkg1/other --> other
    FileUtilities2.createSymLink(otherDir, new File(libDir, "other"));

    ContentCache contentCache = new ContentCache();
    UriResolver resolver = new PackageUriResolver(packagesDir);

    // Assert that package:pkg1 resolves to lib
    Source result = resolver.resolveAbsolute(contentCache, new URI("package:pkg1"));
    assertEquals(libDir, new File(result.getFullName()));

    // Assert that package:pkg1/ resolves to lib
    result = resolver.resolveAbsolute(contentCache, new URI("package:pkg1/"));
    assertEquals(libDir, new File(result.getFullName()));

    // Assert that package:pkg1/other resolves to lib/other not other
    result = resolver.resolveAbsolute(contentCache, new URI("package:pkg1/other"));
    assertEquals(new File(libDir, "other"), new File(result.getFullName()));

    // Assert that package:pkg1/other/some.dart resolves to lib/other/some.dart not other.dart
    // when some.dart does NOT exist
    File someDart = new File(new File(libDir, "other"), "some.dart");
    result = resolver.resolveAbsolute(contentCache, new URI("package:pkg1/other/some.dart"));
    assertEquals(someDart, new File(result.getFullName()));

    // Assert that package:pkg1/other/some.dart resolves to lib/other/some.dart not other.dart
    // when some.dart exists
    assertTrue(new File(otherDir, someDart.getName()).createNewFile());
    assertTrue(someDart.exists());
    result = resolver.resolveAbsolute(contentCache, new URI("package:pkg1/other/some.dart"));
    assertEquals(someDart, new File(result.getFullName()));
  }

  public void test_resolve_invalid() throws Exception {
    File packagesDir = new File("packages");
    ContentCache contentCache = new ContentCache();
    UriResolver resolver = new PackageUriResolver(packagesDir);

    // Invalid: URI
    try {
      new URI("package:");
      fail("Expected exception");
    } catch (URISyntaxException e) {
      //$FALL-THROUGH$
    }

    // Invalid: just slash
    Source result = resolver.resolveAbsolute(contentCache, new URI("package:/"));
    assertNull(result);

    // Invalid: leading slash... or should we gracefully degrade and ignore the leading slash?
    result = resolver.resolveAbsolute(contentCache, new URI("package:/foo"));
    assertNull(result);
  }

  public void test_resolve_nonPackage() throws Exception {
    ContentCache contentCache = new ContentCache();
    File directory = createFile("/does/not/exist/packages");
    UriResolver resolver = new PackageUriResolver(directory);
    Source result = resolver.resolveAbsolute(contentCache, new URI("dart:core"));
    assertNull(result);
  }

  public void test_resolve_package() throws Exception {
    ContentCache contentCache = new ContentCache();
    File directory = createFile("/does/not/exist/packages");
    UriResolver resolver = new PackageUriResolver(directory);
    Source result = resolver.resolveAbsolute(contentCache, new URI(
        "package:third/party/library.dart"));
    assertNotNull(result);
    assertEquals(
        createFile("/does/not/exist/packages/third/party/library.dart").getAbsoluteFile(),
        new File(result.getFullName()));
  }

  @Override
  protected void tearDown() throws Exception {
    FileUtilities2.deleteTempDir();
  }
}
