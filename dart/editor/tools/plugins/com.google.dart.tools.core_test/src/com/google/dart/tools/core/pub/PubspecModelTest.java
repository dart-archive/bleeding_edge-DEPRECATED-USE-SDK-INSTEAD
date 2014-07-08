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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class PubspecModelTest extends TestCase {

  private static final String YAML_WITH_ERRORS = "name: tss \nauthor: GS <s@gmail.com>\n"
      + "description: A sample web application \ndependencies: \n  browser: any"
      + "  webui: \n    git: git://github.com/webui\n    ref: polymer";

  private static final String YAML_NO_ERRORS = "name: tss \nauthor: GS <s@gmail.com>\n"
      + "description: A sample web application \ndependencies: \n  browser: any"
      + "  webui: \n    git: git://github.com/webui\n"
      + "transformers:\n- polymer:\n    entry_points: web/index.html";

  private static final String YAML_NO_ERRORS2 = "name: Yess\n"
      + "description: A sample command-line application\n#dependencies: \n#  browser: any";

  private static final String YAML_HOSTED_DEP = "name:  yess\n" + "dependencies:\n"
      + "  transmogrify: \n"
      + "    hosted:\n      name: transmogrify\n      url: http://some-package-server.com\n"
      + "    version: '>=1.0.0 <2.0.0'";

  // Assert dependency can be added/removed to model
  public void test_addDependency() {
    PubspecModel pubspecModel = new PubspecModel(null);
    DependencyObject dependency = new DependencyObject("unittest");
    pubspecModel.add(new DependencyObject[] {dependency}, IModelListener.ADDED);
    assertTrue(Arrays.asList(pubspecModel.getDependecies()).contains(dependency));
    pubspecModel.remove(new DependencyObject[] {dependency}, false);
    assertTrue(!Arrays.asList(pubspecModel.getDependecies()).contains(dependency));
  }

  public void test_authors() {
    PubspecModel pubspecModel = new PubspecModel(null);
    pubspecModel.setValuesFromString(PubYamlUtilsTest.pubspecYamlString2);
    assertEquals("GS <s@gmail.com>, AS <f@gmail.com>, KM <k@tpl.com>", pubspecModel.getAuthor());
    String string = pubspecModel.getContents();
    PubspecModel model2 = new PubspecModel(string);
    assertEquals("GS <s@gmail.com>, AS <f@gmail.com>, KM <k@tpl.com>", model2.getAuthor());
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

  public void test_errorGeneration() {
    final String projectName = "pubspecTest";
    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
    try {
      project.create(null);
      assertTrue(project.exists());
      IFile file = project.getFile("pubspec.yaml");
      InputStream is = new ByteArrayInputStream(YAML_WITH_ERRORS.getBytes());
      file.create(is, true, null);
      new PubspecModel(file, YAML_WITH_ERRORS);
      assertTrue(file.findMarkers(PubspecModel.PUBSPEC_MARKER, false, IResource.DEPTH_ONE).length > 0);
      is = new ByteArrayInputStream(YAML_NO_ERRORS.getBytes());
      file.setContents(is, 0, null);
      new PubspecModel(file, YAML_NO_ERRORS);
      assertTrue(file.findMarkers(PubspecModel.PUBSPEC_MARKER, false, IResource.DEPTH_ONE).length == 0);
    } catch (CoreException e) {

    }

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

  public void test_getContents_hosted() {
    PubspecModel pubspecModel = new PubspecModel(YAML_HOSTED_DEP);
    String pubspecModelString = pubspecModel.getContents();
    assertTrue(pubspecModelString.contains("hosted:"));
    assertTrue(pubspecModelString.contains("url:"));
    assertTrue(pubspecModelString.contains("http://some-package-server.com"));
    assertTrue(pubspecModelString.contains("name: transmogrify"));
  }

  // Assert model can be initialized from pubspec yaml string
  public void test_initialize() {
    PubspecModel pubspecModel = new PubspecModel(null);
    pubspecModel.setValuesFromString(PubYamlUtilsTest.pubspecYamlString);
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
    pubspecModel.setValuesFromString(PubYamlUtilsTest.pubspecYamlString2);
    assertEquals("web_components", pubspecModel.getName());
    assertEquals(">=1.2.3 <2.0.0", pubspecModel.getSdkVersion());
  }

  public void test_initialize3() {
    PubspecModel pubspecModel = new PubspecModel(null);
    pubspecModel.setValuesFromString(YAML_NO_ERRORS2);
    assertEquals("Yess", pubspecModel.getName());

  }

  public void test_removeDependencies() {
    PubspecModel model1 = new PubspecModel(PubYamlUtilsTest.pubspecYamlString2);
    assertEquals(model1.getDependecies().length, 2);
    DependencyObject dep = (DependencyObject) model1.getDependecies()[0];
    model1.remove(new DependencyObject[] {dep}, false);
    PubspecModel model2 = new PubspecModel(model1.getContents());
    assertEquals(model2.getDependecies().length, 1);
    dep = (DependencyObject) model2.getDependecies()[0];
    model2.remove(new DependencyObject[] {dep}, false);
    PubspecModel model3 = new PubspecModel(model2.getContents());
    assertEquals(model3.getDependecies().length, 0);
  }

}
