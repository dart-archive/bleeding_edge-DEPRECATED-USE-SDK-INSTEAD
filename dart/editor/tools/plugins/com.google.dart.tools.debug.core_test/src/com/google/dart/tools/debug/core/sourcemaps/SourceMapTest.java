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

package com.google.dart.tools.debug.core.sourcemaps;

import com.google.dart.tools.core.test.AbstractDartCoreTest;
import com.google.dart.tools.debug.core.DartDebugCoreTestPlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;

import static org.junit.Assert.assertArrayEquals;

import java.io.File;
import java.net.URL;

public class SourceMapTest extends AbstractDartCoreTest {

  private static String TEST_SOURCE = "{\nversion : 3,\nfile: \"out.js\",\n"
      + "sourceRoot : \"\",\nsources: [\"foo.js\", \"bar.js\"],\n"
      + "names: [\"src\", \"maps\", \"are\", \"fun\"],\n" + "mappings: \"AA,AB;;ABCDE;\"\n}\n";

  public void testDwcParse() throws Exception {
    IFile file = testProject.setFileContent(
        "main.dart.map",
        getClass().getResourceAsStream("main.dart.map"));

    SourceMap map = SourceMap.createFrom(file);

    assertEquals(3, map.getVersion());
    // [24:0,-1] ==> ../main.dart,14,0
    assertEquals("../main.dart,15,0", map.getMappingFor(24, 0).toString());
  }

  public void testSimpleParse() throws Exception {
    String[] expectedNames = {"foo.js", "bar.js"};

    IFile file = testProject.setFileContent("foo.dart.js.map", TEST_SOURCE);

    SourceMap map = SourceMap.createFrom(file);

    assertEquals(3, map.getVersion());
    assertEquals("out.js", map.getFile());
    assertArrayEquals(expectedNames, map.getSourceNames());
    assertEquals(null, map.getMappingFor(1, 0));
    assertEquals("foo.js,1,-1", map.getMappingFor(2, -1).toString());
    assertEquals("foo.js,1,-1", map.getMappingFor(2, 0).toString());
    assertEquals("foo.js,1,-1", map.getMappingFor(2, 1).toString());
  }

  public void testSolarParse() throws Exception {
    IFile file = testProject.setFileContent(
        "foo.dart.js.map",
        getClass().getResourceAsStream("solar.dart.js.map"));

    SourceMap map = SourceMap.createFrom(file);

    assertEquals(3, map.getVersion());
    assertEquals(
        "file:///C:/tools/eclipse_37/dart-sdk/lib/_internal/compiler/implementation/lib/regexp_helper.dart,84,36",
        map.getMappingFor(100, -1).toString());
    assertEquals(
        "file:///C:/Users/username/solar/solar.dart,263,2",
        map.getMappingFor(1351, -1).toString());
    assertEquals(
        "file:///C:/Users/username/solar/solar.dart,264,8",
        map.getMappingFor(1351, 17).toString());
    assertEquals(
        "file:///C:/Users/username/solar/solar.dart,263,2",
        map.getMappingFor(1351, 18).toString());
    assertEquals(
        "file:///C:/Users/username/solar/solar.dart,264,19",
        map.getMappingFor(1353, 6).toString());
    assertEquals(
        "file:///C:/Users/username/solar/solar.dart,264,8",
        map.getMappingFor(1353, 60).toString());
  }

  public void x_testParseSpeed() throws Exception {
    URL bundleURL = FileLocator.find(DartDebugCoreTestPlugin.getPlugin().getBundle(), new Path(
        "src/com/google/dart/tools/debug/core/sourcemaps/solar.dart.js.map"), null);
    URL fileURL = FileLocator.toFileURL(bundleURL);
    File file = new File(fileURL.toURI());

    int warmup = 2;

    while (warmup-- > 0) {
      SourceMap.createFrom(file);
    }

    long length = file.length();
    int iterationCount = 100;

    long startTime = System.currentTimeMillis();

    for (int i = 0; i < iterationCount; i++) {
      SourceMap.createFrom(file);
    }

    long elapsed = System.currentTimeMillis() - startTime;
    double byteCount = length * iterationCount;

    double mbps = (byteCount / (1024 * 1024)) / (elapsed / 1000.0);

    // Currently 9.84 MB/s.
    System.out.printf("source maps parse at %.2f MB/s\n", mbps);
  }

}
