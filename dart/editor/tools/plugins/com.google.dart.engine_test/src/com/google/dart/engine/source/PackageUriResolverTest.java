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

  public void test_isPackageUri_null_scheme() throws Exception {
    URI uri = new URI("foo.dart");
    assertNull(uri.getScheme());
    assertFalse(PackageUriResolver.isPackageUri(uri));
  }

  public void test_resolve_canonical() throws Exception {
    if (!FileUtilities2.isSymLinkSupported()) {
      System.out.println("Skipping " + getClass().getSimpleName() + " test_resolve_canonical");
      return;
    }

    File lib1Dir = FileUtilities2.createTempDir("pkg1/lib").getCanonicalFile();
    File otherDir = FileUtilities2.createTempDir("pkg1/other").getCanonicalFile();
    File packagesDir = FileUtilities2.createTempDir("pkg1/packages").getCanonicalFile();
    File lib2Dir = FileUtilities2.createTempDir("pkg2/lib").getCanonicalFile();

    // Create symlink packages/pkg1 --> lib1
    File pkg1Dir = new File(packagesDir, "pkg1");
    FileUtilities2.createSymLink(lib1Dir, pkg1Dir);

    // Create symlink packages/pkg1/other --> other
    FileUtilities2.createSymLink(otherDir, new File(lib1Dir, "other"));

    // Create symlink packages/pkg2 --> lib2
    File pkg2Dir = new File(packagesDir, "pkg2");
    FileUtilities2.createSymLink(lib2Dir, pkg2Dir);

    UriResolver resolver = new PackageUriResolver(packagesDir);

    // Assert that package:pkg1 resolves to lib1
    Source result = resolver.resolveAbsolute(new URI("package:pkg1"));
    assertEquals(lib1Dir, new File(result.getFullName()));
    assertSame(UriKind.FILE_URI, result.getUriKind());

    // Assert that package:pkg1/ resolves to lib1
    result = resolver.resolveAbsolute(new URI("package:pkg1/"));
    assertEquals(lib1Dir, new File(result.getFullName()));
    assertSame(UriKind.FILE_URI, result.getUriKind());

    // Assert that package:pkg1/other resolves to lib1/other not other
    result = resolver.resolveAbsolute(new URI("package:pkg1/other"));
    assertEquals(new File(lib1Dir, "other"), new File(result.getFullName()));
    assertSame(UriKind.FILE_URI, result.getUriKind());

    // Assert that package:pkg1/other/some.dart resolves to lib1/other/some.dart not other.dart
    // when some.dart does NOT exist
    File someDart = new File(new File(lib1Dir, "other"), "some.dart");
    result = resolver.resolveAbsolute(new URI("package:pkg1/other/some.dart"));
    assertEquals(someDart, new File(result.getFullName()));

    // Assert that package:pkg1/other/some.dart resolves to lib1/other/some.dart not other.dart
    // when some.dart exists
    assertTrue(new File(otherDir, someDart.getName()).createNewFile());
    assertTrue(someDart.exists());
    result = resolver.resolveAbsolute(new URI("package:pkg1/other/some.dart"));
    assertEquals(someDart, new File(result.getFullName()));

    // Assert that package:pkg2/ resolves to lib2
    result = resolver.resolveAbsolute(new URI("package:pkg2/"));
    assertEquals(lib2Dir, new File(result.getFullName()));
    assertSame(UriKind.PACKAGE_URI, result.getUriKind());
  }

  public void test_resolve_invalid() throws Exception {
    File packagesDir = new File("packages");
    UriResolver resolver = new PackageUriResolver(packagesDir);

    // Invalid: URI
    try {
      new URI("package:");
      fail("Expected exception");
    } catch (URISyntaxException e) {
      //$FALL-THROUGH$
    }

    // Invalid: just slash
    Source result = resolver.resolveAbsolute(new URI("package:/"));
    assertNull(result);

    // Invalid: leading slash... or should we gracefully degrade and ignore the leading slash?
    result = resolver.resolveAbsolute(new URI("package:/foo"));
    assertNull(result);
  }

  public void test_resolve_nonPackage() throws Exception {
    File directory = createFile("/does/not/exist/packages");
    UriResolver resolver = new PackageUriResolver(directory);
    Source result = resolver.resolveAbsolute(new URI("dart:core"));
    assertNull(result);
  }

  public void test_resolve_package() throws Exception {
    File directory = createFile("/does/not/exist/packages");
    UriResolver resolver = new PackageUriResolver(directory);
    Source result = resolver.resolveAbsolute(new URI("package:third/party/library.dart"));
    assertNotNull(result);
    assertEquals(
        createFile("/does/not/exist/packages/third/party/library.dart").getAbsoluteFile(),
        new File(result.getFullName()));
  }

  public void test_restore() throws Exception {
    if (!FileUtilities2.isSymLinkSupported()) {
      System.out.println("Skipping " + getClass().getSimpleName() + " test_restore");
      return;
    }

    File argsCanonicalDir = FileUtilities2.createTempDir("args").getCanonicalFile();
    File packagesDir = FileUtilities2.createTempDir("packages");

    // Create symlink packages/args --> args-canonical
    FileUtilities2.createSymLink(argsCanonicalDir, new File(packagesDir, "args"));

    UriResolver resolver = new PackageUriResolver(packagesDir);

    // args-canonical/args.dart --> packages:args/args.dart
    File someDart = new File(argsCanonicalDir, "args.dart");
    FileBasedSource source = new FileBasedSource(someDart);
    assertEquals(new URI("package:args/args.dart"), resolver.restoreAbsolute(source));
  }

  @Override
  protected void tearDown() throws Exception {
    FileUtilities2.deleteTempDir();
  }
}
