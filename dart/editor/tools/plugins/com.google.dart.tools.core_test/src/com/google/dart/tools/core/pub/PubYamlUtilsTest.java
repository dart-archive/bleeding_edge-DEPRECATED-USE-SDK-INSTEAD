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

import com.google.dart.tools.core.AbstractDartCoreTest;
import com.google.dart.tools.core.utilities.yaml.PubYamlUtils;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PubYamlUtilsTest extends AbstractDartCoreTest {

  public static String pubspecYamlString = "name: web_components\n"
      + "description: an easy way to build web apps in Dart\n" + "author: dart team\n"
      + "version: 0.0.1\n" + "dependencies: \n" + "  args: any\n" + "  html5lib: 0.0.4\n"
      + "  kittens:\n" + "    git: git://github.com/munificent/kittens.git\n" + "  kittens2: \n"
      + "    git:\n" + "      url: git://github.com/munificent/kittens.git\n"
      + "      ref: some-branch\n" + "  marker_prof:\n"
      + "    git: https://github.com/johnmccutchan/markerprof.git\n" + "  vector_math:\n"
      + "    git: https://github.com/johnmccutchan/DartVectorMath.git\n" + "  unittest:\n"
      + "     path:../../unittest-0.1.1/lib\n" + "documentation: http://www.dartlang.org\n"
      + "homepage: http://pub.dartlang.org\n" + "dev_dependencies:\n  browser: any\n"
      + "transformers:\n- polymer:\n    entry_points: web/index.html\n"
      + "dependency_overrides:\n  html5lib: 0.9.0\n" + "test_field: testing an unknown field\n"
      + "test_field2: yet another unknown";

  public static String pubspecYamlString2 = "name: web_components\n"
      + "description: an easy way to build web apps in Dart\n" + "authors: \n"
      + "- GS <s@gmail.com>\n- AS <f@gmail.com>\n- KM <k@tpl.com>\n" + "environment: \n"
      + "  sdk: \">=1.2.3 <2.0.0\"\n" + "version: 0.0.1\n" + "dependencies: \n"
      + "  unittest: any\n" + "  args: any\n";

  private static String yamlStringWithErrors = "name: web_components\n"
      + "\tdescription: an easy way to build web apps in Dart\n" + "author:";

  private static String yamlLockFile = "{\"packages\":"
      + "{\"vector_math\":{\"version\":\"0.9.1\",\"source\":\"hosted\",\"description\":\"vector_math\"},"
      + "\"unittest\":{\"version\":\"0.0.0-r.15157\",\"source\":\"sdk\",\"description\":\"unittest\"}}}";

  // Assert names of dependencies can be extracted from pubspec.yaml string
  public void test_getNamesOfDependencies() {
    List<String> dependencies = PubYamlUtils.getNamesOfDependencies(pubspecYamlString);
    assertNotNull(dependencies);
    assertEquals(7, dependencies.size());
    assertTrue(dependencies.contains("unittest"));
    assertTrue(dependencies.contains("html5lib"));
    assertTrue(dependencies.contains("args"));
    assertTrue(dependencies.contains("kittens2"));
    assertTrue(dependencies.contains("kittens"));
    assertTrue(dependencies.contains("marker_prof"));
    assertTrue(dependencies.contains("vector_math"));
  }

  // Assert lock file contents can be parsed and version info extracted 
  public void test_getPackageVersionMap() {
    Map<String, String> map = PubYamlUtils.getPackageVersionMap(yamlLockFile);
    assertNotNull(map);
    assertEquals(2, map.size());
    assertEquals("0.9.1", map.get("vector_math"));
    assertEquals("0.0.0-r.15157", map.get("unittest"));
  }

  public void test_getPubspecName() {
    String name = PubYamlUtils.getPubspecName(pubspecYamlString);
    assertEquals("web_components", name);
    name = PubYamlUtils.getPubspecName(yamlStringWithErrors);
    assertEquals("web_components", name);
    name = PubYamlUtils.getPubspecName(yamlLockFile);
    assertNull(name);
  }

  public void test_patternForPackageVersion() {
    assertTrue("1.0.0-alpha".matches(PubYamlUtils.PACKAGE_VERSION_EXPRESSION));
    assertTrue("1.0.0-alpha.1".matches(PubYamlUtils.PACKAGE_VERSION_EXPRESSION));
    assertTrue("1.0.0-0.3.7".matches(PubYamlUtils.PACKAGE_VERSION_EXPRESSION));
    assertTrue("1.0.0-x.7.z.92".matches(PubYamlUtils.PACKAGE_VERSION_EXPRESSION));
    assertTrue("1.1.12+build.1".matches(PubYamlUtils.PACKAGE_VERSION_EXPRESSION));
    assertTrue("1.34.78+0.3.7".matches(PubYamlUtils.PACKAGE_VERSION_EXPRESSION));
    assertTrue("1.3.7+build.11.e0f985a".matches(PubYamlUtils.PACKAGE_VERSION_EXPRESSION));
    assertFalse("1.3.a-x.0.0".matches(PubYamlUtils.PACKAGE_VERSION_EXPRESSION));

  }

  public void test_patternForPubspecNameLine() {
    Matcher m = Pattern.compile(PubYamlUtils.PATTERN_PUBSPEC_NAME_LINE).matcher(pubspecYamlString);
    assertTrue(m.find());
    assertEquals(1, m.groupCount());
    assertEquals("name: web_components", m.group(1));
    m = Pattern.compile(PubYamlUtils.PATTERN_PUBSPEC_NAME_LINE).matcher(yamlStringWithErrors);
    assertTrue(m.find());
    assertEquals(1, m.groupCount());
    assertEquals("name: web_components", m.group(1));
    m = Pattern.compile(PubYamlUtils.PATTERN_PUBSPEC_NAME_LINE).matcher(yamlLockFile);
    assertFalse(m.find());
    m = Pattern.compile(PubYamlUtils.PATTERN_PUBSPEC_NAME_LINE).matcher("  name: sample\n");
    assertTrue(m.find());
    assertEquals(1, m.groupCount());
    assertEquals("name: sample", m.group(1));
  }

  public void test_patternForVersionContraints() {
    assertTrue(PubYamlUtils.isValidVersionConstraintString(">=1.2.3 <=2.0.0"));
    assertTrue(PubYamlUtils.isValidVersionConstraintString("=>1.2.3"));
    assertTrue(PubYamlUtils.isValidVersionConstraintString("=<1.2.3"));
    assertTrue(PubYamlUtils.isValidVersionConstraintString(">1.2.3"));
    assertTrue(PubYamlUtils.isValidVersionConstraintString("<1.2.3"));
    assertFalse(PubYamlUtils.isValidVersionConstraintString("=1.2.3"));
    assertFalse(PubYamlUtils.isValidVersionConstraintString("an"));
    assertFalse(PubYamlUtils.isValidVersionConstraintString(">1.0.0  <2.0.0")); // multiple space not allowed
    assertFalse(PubYamlUtils.isValidVersionConstraintString(" >1.0.0 ")); // no leading/trailing space
    assertTrue(PubYamlUtils.isValidVersionConstraintString("any"));
  }

  public void test_sortVersions() {
    String[] versions = new String[] {"0.4.9", "0.4.8+3", "0.4.12+1", "0.4.8+4", "0.4.11+1",};
    versions = PubYamlUtils.sortVersionArray(versions);
    assertEquals("0.4.8+3", versions[0]);
    assertEquals("0.4.12+1", versions[4]);
    versions = new String[] {"5.2.9-hotfix.issue", "0.5.7", "0.5.7+build", "2.8.90-alpha.12"};
    versions = PubYamlUtils.sortVersionArray(versions);
    assertEquals("0.5.7", versions[0]);
    assertEquals("5.2.9-hotfix.issue", versions[3]);
  }

}
