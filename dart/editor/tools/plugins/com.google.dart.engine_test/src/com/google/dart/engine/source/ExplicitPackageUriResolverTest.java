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
import com.google.dart.engine.utilities.os.OSUtilities;

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
    ContentCache contentCache = new ContentCache();
    UriResolver resolver = new MockExplicitPackageUriResolver(projectDir);

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
    File directory = createFile("/does/not/exist/foo_project");
    UriResolver resolver = new MockExplicitPackageUriResolver(directory);
    Source result = resolver.resolveAbsolute(contentCache, new URI("dart:core"));
    assertNull(result);
  }

  public void test_resolve_resolvePathToPackage() throws Exception {
    if (!OSUtilities.isWindows()) {
      File directory = createFile("/src/foo/bar/baz/lib");
      File dir2 = createFile("/gen/foo/bar/baz");
      String packages = "{\"packages\":{\"unittest\": [\"/dart/unittest/lib\"],"
          + "\"foo.bar.baz\": [\"" + directory.getAbsolutePath() + "\",\"" + dir2.getAbsolutePath()
          + "\"]}}";
      ExplicitPackageUriResolver resolver = new MockExplicitPackageUriResolver(directory, packages);
      File file = createFile("/baz/lib");
      String resolvedPath = resolver.resolvePathToPackage(file.getAbsolutePath());
      assertNotNull(resolvedPath);
      assertEquals("foo.bar.baz", resolvedPath);
      resolvedPath = resolver.resolvePathToPackage(createFile("dart/mypackage").getAbsolutePath());
      assertNull(resolvedPath);
    }
  }

  @Override
  protected void tearDown() throws Exception {
    FileUtilities2.deleteTempDir();
  }
}
