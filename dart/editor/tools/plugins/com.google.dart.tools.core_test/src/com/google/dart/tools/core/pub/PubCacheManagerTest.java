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
package com.google.dart.tools.core.pub;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.builder.TestProjects;
import com.google.dart.tools.core.mock.MockFile;
import com.google.dart.tools.core.mock.MockProject;
import com.google.dart.tools.core.mock.MockWorkspaceRoot;

import junit.framework.TestCase;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;

import java.util.HashMap;
import java.util.Map;

public class PubCacheManagerTest extends TestCase {

  private static String CACHE_STRING = "{\"packages\":{\"analyzer\":"
      + "{\"0.5.16\":{\"location\":\"/Users/keertip/.pub-cache/hosted/pub.dartlang.org/analyzer-0.5.16\"},"
      + "\"0.5.17\":{\"location\":\"/Users/keertip/.pub-cache/hosted/pub.dartlang.org/analyzer-0.5.17\"},"
      + "\"0.5.20\":{\"location\":\"/Users/keertip/.pub-cache/hosted/pub.dartlang.org/analyzer-0.5.20\"}},"
      + "\"args\":{\"0.5.11+1\":{\"location\":\"/Users/keertip/.pub-cache/hosted/pub.dartlang.org/args-0.5.11+1\"},"
      + "\"0.5.9\":{\"location\":\"/Users/keertip/.pub-cache/hosted/pub.dartlang.org/args-0.5.9\"}},"
      + "\"benchmark_harness\":{\"1.0.2\":{\"location\":\"/Users/keertip/.pub-cache/hosted/pub.dartlang.org/benchmark_harness-1.0.2\"}},"
      + "\"bot\":{\"0.16.1\":{\"location\":\"/Users/keertip/.pub-cache/hosted/pub.dartlang.org/bot-0.16.1\"},"
      + "\"0.20.1\":{\"location\":\"/Users/keertip/.pub-cache/hosted/pub.dartlang.org/bot_web-0.20.1\"}},"
      + "\"browser\":{\"0.5.16\":{\"location\":\"/Users/keertip/.pub-cache/hosted/pub.dartlang.org/browser-0.5.16\"},"
      + "\"0.5.20\":{\"location\":\"/Users/keertip/.pub-cache/hosted/pub.dartlang.org/browser-0.5.20\"}},"
      + "\"csslib\":{\"0.3.4+4\":{\"location\":\"/Users/keertip/.pub-cache/hosted/pub.dartlang.org/csslib-0.3.4+4\"}},"
      + "\"darmatch\":{\"0.1.0\":{\"location\":\"/Users/keertip/.pub-cache/hosted/pub.dartlang.org/darmatch-0.1.0\"},"
      + "\"0.2.0\":{\"location\":\"/Users/keertip/.pub-cache/hosted/pub.dartlang.org/darmatch-0.2.0\"}},"
      + "\"dart_flex\":{\"0.2.3\":{\"location\":\"/Users/keertip/.pub-cache/hosted/pub.dartlang.org/dart_flex-0.2.3\"}},"
      + "\"dartflash\":{\"0.6.5\":{\"location\":\"/Users/keertip/.pub-cache/hosted/pub.dartlang.org/dartflash-0.6.5\"}},"
      + "\"dartlings\":{\"0.1.0\":{\"location\":\"/Users/keertip/.pub-cache/hosted/pub.dartlang.org/dartlings-0.1.0\"},"
      + "\"0.2.0\":{\"location\":\"/Users/keertip/.pub-cache/hosted/pub.dartlang.org/dartlings-0.2.0\"}},"
      + "\"widget\":{\"0.2.3\":{\"location\":\"/Users/keertip/.pub-cache/hosted/pub.dartlang.org/widget-0.2.3\"}},"
      + "\"yaml\":{\"0.5.0+1\":{\"location\":\"/Users/keertip/.pub-cache/hosted/pub.dartlang.org/yaml-0.5.0+1\"}}}}";

  private static String lockFileContents = "packages:\n" + "  bot:\n" + "    description: bot\n "
      + "   source: hosted \n" + "    version: \"0.20.1\"\n" + "  csslib:\n"
      + "    description: csslib\n" + "    source: hosted\n" + "    version: \"0.3.4+4\"\n"
      + "  hop:\n" + "    description: hop\n" + "    source: hosted\n" + "    version: \"0.21.0\"";

  PubCacheManager_OLD manager = new PubCacheManager_OLD() {

    @Override
    public void updatePackagesList(int delay, Map<String, String> packages) {
      new FillPubCacheList("PubCacheManager test", packages).run(new NullProgressMonitor());
    }

    @Override
    protected IProject[] getProjects() {
      return rootContainer.getProjects();
    }

    @Override
    protected String getPubCacheList() {
      return CACHE_STRING;
    }

  };

  private MockWorkspaceRoot rootContainer;

  public void test_getAllCachePackages() {
    Map<String, Object> p = manager.getAllCachePackages();
    assertNotNull(p);
    assertTrue(p.isEmpty());
  }

  public void test_getCacheLocation() {
    manager.updatePackagesList(0);
    String location = manager.getCacheLocation("browser", "0.5.20");
    assertNotNull(location);
    assertEquals("/Users/keertip/.pub-cache/hosted/pub.dartlang.org/browser-0.5.20", location);
    location = manager.getCacheLocation("args", "0.5.20");
    assertNull(location);
  }

  public void test_getLocalPackages() {
    Map<String, Object> p = manager.getLocalPackages();
    assertNotNull(p);
    assertTrue(p.isEmpty());
  }

  public void test_updatePackagesList() {
    manager.updatePackagesList(0);
    Map<String, Object> p = manager.getLocalPackages();
    assertNotNull(p);
    assertEquals(2, p.size());
    assertTrue(p.keySet().contains("bot"));
  }

  public void test_updatePackagesList2() {
    Map<String, String> map = new HashMap<String, String>();
    map.put("browser", "0.5.16");
    map.put("dartlings", "0.1.0");
    manager.updatePackagesList(0, map);
    Map<String, Object> p = manager.getLocalPackages();
    assertNotNull(p);
    assertEquals(2, p.size());
    assertTrue(p.keySet().contains("dartlings"));

    map.clear();
    map.put("browser", "0.5.16");
    map.put("dartflash", "0.6.5");
    manager.updatePackagesList(0, map);
    p = manager.getLocalPackages();
    assertEquals(3, p.size());
    assertTrue(p.keySet().contains("dartflash"));
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    rootContainer = new MockWorkspaceRoot();
    MockProject projectContainer = TestProjects.newPubProject2(rootContainer);
    MockFile file = new MockFile(
        projectContainer,
        DartCore.PUBSPEC_LOCK_FILE_NAME,
        lockFileContents);
    projectContainer.add(file);
  }

}
