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

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public class PubCacheManagerTest extends TestCase {

  private static String cacheString = "{\"packages\":{\"analyzer_experimental\":{\"version\":\"0.5.5\","
      + "\"location\":\"/.pub-cache/hosted/pub.dartlang.org/analyzer_experimental-0.5.5\"},"
      + "\"bot_io\":{\"version\":\"0.20.2\","
      + "\"location\":\"/Users/keertip/.pub-cache/hosted/pub.dartlang.org/bot_io-0.20.2\"},"
      + "\"browser\":{\"version\":\"0.5.5\",\"location\":\"/.pub-cache/hosted/pub.dartlang.org/browser-0.5.5\"},"
      + "\"csslib\":{\"version\":\"0.4.6+4\",\"location\":\"/.pub-cache/hosted/pub.dartlang.org/csslib-0.4.6+4\"},"
      + "\"dartflash\":{\"version\":\"0.6.5\",\"location\":\"/.pub-cache/hosted/pub.dartlang.org/dartflash-0.6.5\"},"
      + "\"dartlings\":{\"version\":\"0.1.0\",\"location\":\"/.pub-cache/hosted/pub.dartlang.org/dartlings-0.1.0\"},"
      + "\"args\":{\"version\":\"0.5.5\",\"location\":\"/.pub-cache/hosted/pub.dartlang.org/args-0.5.5\"},"
      + "\"bot\":{\"version\":\"0.20.1\",\"location\":\"/.pub-cache/hosted/pub.dartlang.org/bot-0.20.1\"},"
      + "\"detester\":{\"version\":\"0.1.0\",\"location\":\"/.pub-cache/hosted/pub.dartlang.org/detester-0.1.0\"},"
      + "\"hop\":{\"version\":\"0.21.0\",\"location\":\"/.pub-cache/hosted/pub.dartlang.org/hop-0.21.0\"}}}";

  private static String lockFileContents = "packages:\n" + "  bot_io:\n"
      + "    description: bot_io\n " + "   source: hosted \n" + "    version: \"0.20.2\"\n"
      + "  csslib:\n" + "    description: csslib\n" + "    source: hosted\n"
      + "    version: \"0.4.6+4\"\n" + "  hop:\n" + "    description: hop\n"
      + "    source: hosted\n" + "    version: \"0.21.0\"";

  PubCacheManager manager = new PubCacheManager() {

    @Override
    public void updatePackagesList(int delay, Collection<String> packages) {
      new FillPubCacheList("PubCacheManager test", packages).run(new NullProgressMonitor());
    }

    @Override
    protected IProject[] getProjects() {
      return rootContainer.getProjects();
    }

    @Override
    protected String getPubCacheList() {
      return cacheString;
    }

  };

  private MockWorkspaceRoot rootContainer;

  public void test_getLocalPackages() {
    Map<String, Object> p = manager.getLocalPackages();
    assertNotNull(p);
    assertTrue(p.isEmpty());
  }

  public void test_updatePackagesList() {
    manager.updatePackagesList(0);
    Map<String, Object> p = manager.getLocalPackages();
    assertNotNull(p);
    assertEquals(3, p.size());
    assertTrue(p.keySet().contains("hop"));
  }

  public void test_updatePackagesList2() {
    manager.updatePackagesList(0, Arrays.asList("browser", "dartlings"));
    Map<String, Object> p = manager.getLocalPackages();
    assertNotNull(p);
    assertEquals(2, p.size());
    assertTrue(p.keySet().contains("dartlings"));

    manager.updatePackagesList(0, Arrays.asList("browser", "dartflash"));
    p = manager.getLocalPackages();
    assertEquals(3, p.size());
    assertTrue(p.keySet().contains("dartflash"));
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    rootContainer = new MockWorkspaceRoot();
    MockProject projectContainer = TestProjects.newPubProject2(rootContainer);
    rootContainer.add(projectContainer);
    MockFile file = new MockFile(
        projectContainer,
        DartCore.PUBSPEC_LOCK_FILE_NAME,
        lockFileContents);
    projectContainer.add(file);
  }

}
