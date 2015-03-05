/*
 * Copyright (c) 2013, the Dart project authors.
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

package com.google.dart.tools.debug.core.dartium;

import com.google.dart.engine.source.ExplicitPackageUriResolver;
import com.google.dart.engine.source.ExplicitPackageUriResolverTest;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

import junit.framework.TestCase;

import org.eclipse.core.resources.IResource;

import java.io.File;
import java.io.IOException;

public class BreakpointManagerTest extends TestCase {

  private final class MockBreakpointManager extends BreakpointManager {

    public MockBreakpointManager() {
      super(null);
    }

    @Override
    protected String resolvePathToPackage(IResource resource, String filePath) {
      File directory = createFile("/src/foo/bar/baz/lib");
      File dir2 = createFile("/gen/foo/bar/baz");
      String packages = "{\"packages\":{\"unittest\": [\"/dart/unittest/lib\"],"
          + "\"foo.bar.baz\": [\"" + directory.getAbsolutePath() + "\",\"" + dir2.getAbsolutePath()
          + "\"]}}";
      ExplicitPackageUriResolver resolver = new ExplicitPackageUriResolverTest.MockExplicitPackageUriResolver(
          new File("root"),
          packages);
      return resolver.resolvePathToPackage(filePath);
    }

  }

  public void test_getPackagePath() throws IOException {
    MockBreakpointManager manager = new MockBreakpointManager();

    // TODO(keertip): fix test for analysis server - send in location
    if (!DartCore.isWindows() && !DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      File file = createFile("/baz/lib/myLib.dart");
      String path = manager.getPackagePath(file.getAbsolutePath(), null, file.getCanonicalPath());
      assertNotNull(path);
      assertEquals("foo.bar.baz/myLib.dart", path);
      file = createFile("/baz/src/myLib.dart");
      path = manager.getPackagePath(file.getAbsolutePath(), null, file.getCanonicalPath());
      assertNull(path);
      file = createFile("/bar/baz/lib/util/util.dart");
      path = manager.getPackagePath(file.getAbsolutePath(), null, file.getCanonicalPath());
      assertNotNull(path);
      assertEquals("foo.bar.baz/util/util.dart", path);
    }
  }

}
