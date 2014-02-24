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

import org.json.JSONException;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExplicitPackageUriResolverTest extends TestCase {

  public static class MockExplicitPackageUriResolver extends ExplicitPackageUriResolver {

    String jsonText = "{\"foo\":\"bar\"}";

    public MockExplicitPackageUriResolver(File rootDir) {
      super(null, rootDir);
    }

    public MockExplicitPackageUriResolver(File rootDir, String jsonPackageList) {
      super(null, rootDir);
      if (!jsonPackageList.isEmpty()) {
        jsonText = jsonPackageList;
        packageMap = calculatePackageMap();
      }
    }

    @Override
    protected Map<String, List<File>> calculatePackageMap() {
      try {
        return parsePackageMap(jsonText);
      } catch (JSONException e) {
        return new HashMap<String, List<File>>();
      }
    }
  }

  public void test_creation() {
    File directory = createFile("/does/not/exist/foo_project");
    assertNotNull(new ExplicitPackageUriResolver(null, directory));
  }

  public void test_resolve_invalid() throws Exception {
    File projectDir = new File("foo_project");
    UriResolver resolver = new MockExplicitPackageUriResolver(projectDir);

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
    File directory = createFile("/does/not/exist/foo_project");
    UriResolver resolver = new MockExplicitPackageUriResolver(directory);
    Source result = resolver.resolveAbsolute(new URI("dart:core"));
    assertNull(result);
  }

  public void test_resolve_resolvePathToPackage() throws Exception {
    File directory = createFile("/src/foo/bar/baz/lib");
    String packages = "{\"packages\":{\"unittest\": [\"/dart/unittest/lib\"],"
        + "\"foo.bar.baz\": [\"/src/foo/bar/baz/lib\",\"/gen/foo/bar/baz\"]}}";
    ExplicitPackageUriResolver resolver = new MockExplicitPackageUriResolver(directory, packages);
    String resolvedPath = resolver.resolvePathToPackage(File.separator + "baz" + File.separator
        + "lib");
    assertNotNull(resolvedPath);
    assertEquals("foo.bar.baz", resolvedPath);
    resolvedPath = resolver.resolvePathToPackage(File.separator + "dart" + File.separator
        + "mypackage");
    assertNull(resolvedPath);
  }

  @Override
  protected void tearDown() throws Exception {
    FileUtilities2.deleteTempDir();
  }
}
