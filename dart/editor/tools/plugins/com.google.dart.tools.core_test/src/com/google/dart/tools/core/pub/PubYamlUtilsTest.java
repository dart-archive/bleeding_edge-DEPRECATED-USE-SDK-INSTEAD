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
package com.google.dart.tools.core.pub;

import com.google.dart.tools.core.utilities.yaml.PubYamlObject;
import com.google.dart.tools.core.utilities.yaml.PubYamlUtils;

import junit.framework.TestCase;

import java.util.Map;
import java.util.TreeMap;

public class PubYamlUtilsTest extends TestCase {

  public static String pubspecYamlString = "name: web_components\n"
      + "description: an easy way to build web apps in Dart\n" + "author: dart team\n"
      + "version: 0.0.1\n" + "dependencies: \n" + "  unittest: any\n" + "  args: any\n"
      + "  html5lib: 0.0.4\n" + "  kittens:\n"
      + "    git: git://github.com/munificent/kittens.git\n" + "  kittens2: \n" + "    git:\n"
      + "      url: git://github.com/munificent/kittens.git\n" + "      ref: some-branch\n"
      + "  marker_prof:\n" + "    git: https://github.com/johnmccutchan/markerprof.git\n"
      + "  vector_math:\n" + "    git: https://github.com/johnmccutchan/DartVectorMath.git";

  private static String yamlStringWithErrors = "name: web_components\n"
      + "\tdescription: an easy way to build web apps in Dart\n" + "author:";

  private static String yamlLockFile = "{\"packages\":"
      + "{\"vector_math\":{\"version\":\"0.9.1\",\"source\":\"hosted\",\"description\":\"vector_math\"},"
      + "\"unittest\":{\"version\":\"0.0.0-r.15157\",\"source\":\"sdk\",\"description\":\"unittest\"}}}";

  // Assert yaml string can be constructed from PubYamlObject
  public void test_buildYamlString() {
    PubYamlObject object1 = PubYamlUtils.parsePubspecYamlToObject(pubspecYamlString);
    assertNotNull(object1);
    String yamlString = PubYamlUtils.buildYamlString(object1);
    assertNotNull(yamlString);
    PubYamlObject object2 = PubYamlUtils.parsePubspecYamlToObject(yamlString);
    checkPubSpecsEqual(object1, object2);

  }

  // Assert lock file contents can be parsed and version info extracted 
  public void test_getPackageVerionMap() {
    Map<String, String> map = PubYamlUtils.getPackageVersionMap(yamlLockFile);
    assertNotNull(map);
    assertEquals(2, map.size());
    assertEquals("0.9.1", map.get("vector_math"));
    assertEquals("0.0.0-r.15157", map.get("unittest"));
  }

  // Assert pubspec file contents can be loaded into PubYamlObject
  public void test_parseYamlToObject() {
    PubYamlObject object = PubYamlUtils.parsePubspecYamlToObject(pubspecYamlString);
    assertNotNull(object);
    assertEquals("web_components", object.name);
    assertEquals("an easy way to build web apps in Dart", object.description);
    assertEquals("0.0.1", object.version);
    assertEquals("dart team", object.author);
    assertNotNull(object.dependencies);
  }

  // Assert  method returns for yaml with errors 
  public void test_parseYamlToObjectWithErrors() {
    Map<String, Object> map = PubYamlUtils.parsePubspecYamlToMap(yamlStringWithErrors);
    assertTrue(map == null);
  }

  private void checkPubSpecsEqual(PubYamlObject object1, PubYamlObject object2) {
    assertEquals(object1.name, object2.name);
    assertEquals(object1.author, object2.author);
    assertEquals(object1.description, object2.description);
    assertEquals(object1.homepage, object2.homepage);
    assertEquals(object1.version, object2.version);
    assertEquals(
        new TreeMap<String, Object>(object1.dependencies).toString().hashCode(),
        new TreeMap<String, Object>(object2.dependencies).toString().hashCode());

  }

}
