/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.engine.internal.html.angular;

import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.angular.AngularPropertyElement;
import com.google.dart.engine.index.IndexStore;
import com.google.dart.engine.internal.index.IndexConstants;
import com.google.dart.engine.internal.index.IndexContributorHelper.ExpectedLocation;
import com.google.dart.engine.internal.index.IndexContributorHelper.RecordedRelation;

import static com.google.dart.engine.internal.index.IndexContributorHelper.assertRecordedRelation;
import static com.google.dart.engine.internal.index.IndexContributorHelper.captureRelations;

import static org.mockito.Mockito.mock;

import java.util.List;

public class AngularDartIndexContributorTest extends AngularTest {
  private IndexStore store = mock(IndexStore.class);
  private AngularDartIndexContributor index = new AngularDartIndexContributor(store);

  public void test_component_propertyField() throws Exception {
    resolveMainSourceNoErrors(createSource("",//
        "import 'angular.dart';",
        "",
        "@NgComponent(",
        "    templateUrl: 'my_template.html', cssUrl: 'my_styles.css',",
        "    publishAs: 'ctrl',",
        "    selector: 'myComponent', // selector",
        "    map: const {'prop' : '=>field'})",
        "class MyComponent {",
        "  set field(value) {}",
        "}",
        "",
        "main() {",
        "  var module = new Module();",
        "  module.type(MyComponent);",
        "  ngBootstrap(module: module);",
        "}"));
    AngularPropertyElement property = findMainElement("prop");
    FieldElement field = findMainElement("field");
    // index
    mainUnit.accept(index);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(relations, field, IndexConstants.IS_REFERENCED_BY, new ExpectedLocation(
        property,
        findMainOffset("field'})"),
        "field"));
  }

  public void test_directive_propertyField() throws Exception {
    resolveMainSourceNoErrors(createSource("",//
        "import 'angular.dart';",
        "",
        "@NgDirective(",
        "    selector: '[my-directive]',",
        "    map: const {'my-directive' : '=>field'})",
        "class MyDirective {",
        "  set field(value) {}",
        "}",
        "",
        "main() {",
        "  var module = new Module();",
        "  module.type(MyDirective);",
        "  ngBootstrap(module: module);",
        "}"));
    AngularPropertyElement property = findMainElement(ElementKind.ANGULAR_PROPERTY, "my-directive");
    FieldElement field = findMainElement("field");
    // index
    mainUnit.accept(index);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(relations, field, IndexConstants.IS_REFERENCED_BY, new ExpectedLocation(
        property,
        findMainOffset("field'})"),
        "field"));
  }

  private List<RecordedRelation> captureRecordedRelations() {
    return captureRelations(store);
  }
}
