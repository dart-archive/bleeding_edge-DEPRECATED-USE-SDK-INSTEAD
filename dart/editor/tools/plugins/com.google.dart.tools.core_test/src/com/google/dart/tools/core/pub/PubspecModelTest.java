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

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;

public class PubspecModelTest extends TestCase {

  // Assert dependency can be added/removed to model
  public void test_addDependency() {
    PubspecModel pubspecModel = new PubspecModel(null);
    DependencyObject dependency = new DependencyObject("unittest");
    pubspecModel.add(new DependencyObject[] {dependency}, IModelListener.ADDED);
    assertTrue(Arrays.asList(pubspecModel.getDependecies()).contains(dependency));
    pubspecModel.remove(new DependencyObject[] {dependency}, false);
    assertTrue(!Arrays.asList(pubspecModel.getDependecies()).contains(dependency));
  }

  // Assert that model creation initializes all fields
  public void test_create() {
    PubspecModel pubspecModel = new PubspecModel(null);
    assertNotNull(pubspecModel);
    assertNotNull(pubspecModel.getName());
    assertNotNull(pubspecModel.getAuthor());
    assertNotNull(pubspecModel.getDescription());
    assertNotNull(pubspecModel.getHomepage());
    assertNotNull(pubspecModel.getVersion());
    assertNotNull(pubspecModel.getDependecies());
    assertNotNull(pubspecModel.getDocumentation());
    assertNotNull(pubspecModel.getHomepage());
  }

  // Assert that there is no info lost in getContents conversion to yaml 
  public void test_getContents() {
    PubspecModel pubspecModel1 = new PubspecModel(PubYamlUtilsTest.pubspecYamlString);
    String pubspecModelString = pubspecModel1.getContents();
    PubspecModel pubspecModel2 = new PubspecModel(pubspecModelString);
    assertEquals(pubspecModel2.getName(), pubspecModel1.getName());
    assertEquals(pubspecModel2.getDescription(), pubspecModel1.getDescription());
    assertEquals(pubspecModel2.getVersion(), pubspecModel1.getVersion());
    assertEquals(pubspecModel2.getAuthor(), pubspecModel1.getAuthor());
    assertEquals(pubspecModel2.getHomepage(), pubspecModel1.getHomepage());
    assertEquals(pubspecModel2.getDocumentation(), pubspecModel1.getDocumentation());
    List<Object> list1 = Arrays.asList(pubspecModel1.getDependecies());
    List<Object> list2 = Arrays.asList(pubspecModel2.getDependecies());
    assertEquals(list1.size(), list2.size());
    assertTrue(list1.containsAll(list2));
  }

  // Assert model can be initialized from pubspec yaml string
  public void test_initialize() {
    PubspecModel pubspecModel = new PubspecModel(null);
    pubspecModel.initialize(PubYamlUtilsTest.pubspecYamlString);
    assertEquals("web_components", pubspecModel.getName());
    assertEquals("an easy way to build web apps in Dart", pubspecModel.getDescription());
    assertEquals("0.0.1", pubspecModel.getVersion());
    assertEquals("dart team", pubspecModel.getAuthor());
    assertEquals("http://pub.dartlang.org", pubspecModel.getHomepage());
    assertEquals("http://www.dartlang.org", pubspecModel.getDocumentation());
    assertEquals(8, pubspecModel.getDependecies().length);
    assertEquals("", pubspecModel.getSdkVersion());
  }

  public void test_initialize2() {
    PubspecModel pubspecModel = new PubspecModel(null);
    pubspecModel.initialize(PubYamlUtilsTest.pubspecYamlString2);
    assertEquals("web_components", pubspecModel.getName());
    assertEquals(">=1.2.3 <2.0.0", pubspecModel.getSdkVersion());
  }

}
