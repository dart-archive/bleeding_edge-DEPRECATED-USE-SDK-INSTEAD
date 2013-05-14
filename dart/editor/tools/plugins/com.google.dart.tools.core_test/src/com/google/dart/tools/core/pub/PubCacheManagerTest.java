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

import com.google.dart.tools.core.utilities.yaml.PubYamlUtils;

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

public class PubCacheManagerTest extends TestCase {

  private static String cacheString = "{\"analyzer_experimental\":{\"version\":\"0.5.5\","
      + "\"location\":\"/.pub-cache/hosted/pub.dartlang.org/analyzer_experimental-0.5.5\"},"
      + "\"args\":{\"version\":\"0.5.5\",\"location\":\"/.pub-cache/hosted/pub.dartlang.org/args-0.5.5\"},"
      + "\"benchmark_harness\":{\"version\":\"1.0.2\","
      + "\"location\":\"/.pub-cache/hosted/pub.dartlang.org/benchmark_harness-1.0.2\"},"
      + "\"bot\":{\"version\":\"0.20.1\",\"location\":\"/.pub-cache/hosted/pub.dartlang.org/bot-0.20.1\"},"
      + "\"detester\":{\"version\":\"0.1.0\",\"location\":\"/.pub-cache/hosted/pub.dartlang.org/detester-0.1.0\"},"
      + "\"google_maps\":{\"version\":\"1.1.1\",\"location\":\".pub-cache/hosted/pub.dartlang.org/google_maps-1.1.1\"},"
      + "\"hop\":{\"version\":\"0.21.0\",\"location\":\"/.pub-cache/hosted/pub.dartlang.org/hop-0.21.0\"}}";

  private static String cacheString2 = "{\"analyzer_experimental\":{\"version\":\"0.5.5\","
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
      + "\"hop\":{\"version\":\"0.21.0\",\"location\":\"/.pub-cache/hosted/pub.dartlang.org/hop-0.21.0\"}}";

  PubCacheManager manager = new PubCacheManager() {

    @Override
    public HashMap<String, Object> getLocalPackages() {
      if (pubCachePackages.isEmpty()) {
        Map<String, Object> map = PubYamlUtils.parsePubspecYamlToMap(cacheString);
        added = getPackagesAdded(map);
      }
      return pubCachePackages;
    }

    @Override
    public void updatePubCacheList(int delay) {
      Map<String, Object> map = PubYamlUtils.parsePubspecYamlToMap(cacheString2);
      added = getPackagesAdded(map);
      removed = getPackagesRemoved(map);
    }
  };

  private Map<String, Object> added;
  private Map<String, Object> removed;

  public void test_getPackagesAdded() {
    Map<String, Object> map = PubYamlUtils.parsePubspecYamlToMap(cacheString);
    Map<String, Object> packages = manager.getLocalPackages();
    assertNotNull(packages);
    assertFalse(packages.isEmpty());
    assertEquals(map.size(), packages.size());
    assertEquals(map, packages);
  }

  public void test_getPackagesAdded2() {
    Map<String, Object> packages = manager.getLocalPackages();
    manager.updatePubCacheList(0);
    packages = manager.getLocalPackages();
    assertEquals(10, packages.size());
    assertEquals(5, added.size());
    assertEquals(2, removed.size());
    assertTrue(added.containsKey("bot_io"));
    assertTrue(added.containsKey("browser"));
    assertTrue(added.containsKey("csslib"));
    assertTrue(added.containsKey("dartlings"));
    assertTrue(added.containsKey("dartflash"));
    assertTrue(removed.containsKey("benchmark_harness"));
    assertTrue(removed.containsKey("google_maps"));
  }

}
