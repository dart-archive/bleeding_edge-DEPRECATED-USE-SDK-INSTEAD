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
package com.google.dart.server.internal.local.source;

import com.google.common.collect.ImmutableMap;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.UriKind;
import com.google.dart.engine.source.UriResolver;
import com.google.dart.engine.utilities.io.FileUtilities2;

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

import junit.framework.TestCase;

import java.net.URI;
import java.util.Map;

public class PackageMapUriResolverTest extends TestCase {
  private static final Map<String, Resource> EMPTY_MAP = ImmutableMap.of();

  public void test_fromEncoding_nonPackage() throws Exception {
    UriResolver resolver = new PackageMapUriResolver(EMPTY_MAP);
    Source result = resolver.fromEncoding(UriKind.DART_URI, new URI("file:/does/not/exist.dart"));
    assertNull(result);
  }

  public void test_fromEncoding_package() throws Exception {
    UriResolver resolver = new PackageMapUriResolver(EMPTY_MAP);
    Source result = resolver.fromEncoding(UriKind.PACKAGE_URI, new URI("file:/does/not/exist.dart"));
    assertNotNull(result);
    assertEquals(createFile("/does/not/exist.dart").getAbsolutePath(), result.getFullName());
  }

  public void test_isPackageUri() throws Exception {
    URI uri = new URI("package:test/test.dart");
    assertEquals("package", uri.getScheme());
    assertTrue(PackageMapUriResolver.isPackageUri(uri));
  }

  public void test_isPackageUri_null_scheme() throws Exception {
    URI uri = new URI("foo.dart");
    assertNull(uri.getScheme());
    assertFalse(PackageMapUriResolver.isPackageUri(uri));
  }

  public void test_resolve_nonPackage() throws Exception {
    UriResolver resolver = new PackageMapUriResolver(EMPTY_MAP);
    Source result = resolver.resolveAbsolute(new URI("dart:core"));
    assertNull(result);
  }

  public void test_resolve_package_invalid_leadingSlash() throws Exception {
    UriResolver resolver = new PackageMapUriResolver(EMPTY_MAP);
    Source result = resolver.resolveAbsolute(new URI("package:/foo"));
    assertNull(result);
  }

  public void test_resolve_package_invalid_noSlash() throws Exception {
    UriResolver resolver = new PackageMapUriResolver(EMPTY_MAP);
    Source result = resolver.resolveAbsolute(new URI("package:foo"));
    assertNull(result);
  }

  public void test_resolve_package_invalid_onlySlash() throws Exception {
    UriResolver resolver = new PackageMapUriResolver(EMPTY_MAP);
    Source result = resolver.resolveAbsolute(new URI("package:/"));
    assertNull(result);
  }

  public void test_resolve_package_notInMap() throws Exception {
    UriResolver resolver = new PackageMapUriResolver(EMPTY_MAP);
    Source result = resolver.resolveAbsolute(new URI("package:analyzer/analyzer.dart"));
    assertNotNull(result);
    assertFalse(result.exists());
    assertEquals("package:analyzer/analyzer.dart", result.getFullName());
  }

  public void test_resolve_package_OK() throws Exception {
    TestFileSystem fileSystem = new TestFileSystem();
    Resource pkgLibDirA = fileSystem.newDirectory("/pkgA/lib");
    Resource pkgLibDirB = fileSystem.newDirectory("/pkgB/lib");
    Resource pkgFileA = fileSystem.newFile(pkgLibDirA, "libA.dart", "library A;");
    Resource pkgFileB = fileSystem.newFile(pkgLibDirB, "libB.dart", "library B;");
    Map<String, Resource> packageMap = ImmutableMap.of("pkgA", pkgLibDirA, "pkgB", pkgLibDirB);
    UriResolver resolver = new PackageMapUriResolver(packageMap);
    {
      Source result = resolver.resolveAbsolute(new URI("package:pkgA/libA.dart"));
      assertNotNull(result);
      assertTrue(result.exists());
      assertSame(UriKind.PACKAGE_URI, result.getUriKind());
      assertEquals(pkgFileA.getPath(), result.getFullName());
    }
    {
      Source result = resolver.resolveAbsolute(new URI("package:pkgB/libB.dart"));
      assertNotNull(result);
      assertTrue(result.exists());
      assertSame(UriKind.PACKAGE_URI, result.getUriKind());
      assertEquals(pkgFileB.getPath(), result.getFullName());
    }
  }

  @Override
  protected void tearDown() throws Exception {
    FileUtilities2.deleteTempDir();
  }
}
