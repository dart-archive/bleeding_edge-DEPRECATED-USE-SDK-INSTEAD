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

import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.index.IndexStore;
import com.google.dart.engine.internal.index.IndexContributorHelper.ExpectedLocation;
import com.google.dart.engine.internal.index.IndexContributorHelper.RecordedRelation;

import static com.google.dart.engine.internal.index.IndexConstants.IS_REFERENCED_BY_QUALIFIED;
import static com.google.dart.engine.internal.index.IndexContributorHelper.assertNoRecordedRelation;
import static com.google.dart.engine.internal.index.IndexContributorHelper.assertRecordedRelation;
import static com.google.dart.engine.internal.index.IndexContributorHelper.captureRelations;

import static org.mockito.Mockito.mock;

import java.util.List;

public class AngularDartIndexContributorTest extends AngularTest {
  private IndexStore store = mock(IndexStore.class);
  private AngularDartIndexContributor index = new AngularDartIndexContributor(store);

  public void test_component_propertyField() throws Exception {
    contextHelper.addSource("/my_template.html", "");
    contextHelper.addSource("/my_styles.css", "");
    resolveMainSourceNoErrors(createSource("",//
        "import 'angular.dart';",
        "",
        "@NgComponent(",
        "    templateUrl: 'my_template.html', cssUrl: 'my_styles.css',",
        "    publishAs: 'ctrl',",
        "    selector: 'myComponent', // selector",
        "    map: const {",
        "        'propAttr' : '@field', // attr",
        "        'propOneWay' : '=>field', // one-way",
        "        'propTwoWay' : '<=>field', // two-way",
        "    })",
        "class MyComponent {",
        "  @NgOneWay('annProp')",
        "  var field;",
        "}",
        "",
        "main() {",
        "  var module = new Module();",
        "  module.type(MyComponent);",
        "  ngBootstrap(module: module);",
        "}"));
    FieldElement field = findMainElement("field");
    PropertyAccessorElement getter = field.getGetter();
    PropertyAccessorElement setter = field.getSetter();
    // index
    mainUnit.accept(index);
    List<RecordedRelation> relations = captureRecordedRelations();
    // @field
    {
      ExpectedLocation location = new ExpectedLocation(
          findMainElement("propAttr"),
          findMainOffset("field', // attr"),
          "field");
      assertNoRecordedRelation(relations, getter, IS_REFERENCED_BY_QUALIFIED, location);
      assertRecordedRelation(relations, setter, IS_REFERENCED_BY_QUALIFIED, location);
    }
    // =>field
    {
      ExpectedLocation location = new ExpectedLocation(
          findMainElement("propOneWay"),
          findMainOffset("field', // one-way"),
          "field");
      assertNoRecordedRelation(relations, getter, IS_REFERENCED_BY_QUALIFIED, location);
      assertRecordedRelation(relations, setter, IS_REFERENCED_BY_QUALIFIED, location);
    }
    // <=>field
    {
      ExpectedLocation location = new ExpectedLocation(
          findMainElement("propTwoWay"),
          findMainOffset("field', // two-way"),
          "field");
      assertRecordedRelation(relations, getter, IS_REFERENCED_BY_QUALIFIED, location);
      assertRecordedRelation(relations, setter, IS_REFERENCED_BY_QUALIFIED, location);
    }
    // @NgOneWay('annProp') is ignore - no explicit field reference
    {
      ExpectedLocation location = new ExpectedLocation(findMainElement("annProp"), -1, "field");
      assertNoRecordedRelation(relations, setter, IS_REFERENCED_BY_QUALIFIED, location);
    }
  }

  public void test_directive_propertyField() throws Exception {
    resolveMainSourceNoErrors(createSource("",//
        "import 'angular.dart';",
        "",
        "@NgDirective(",
        "    selector: '[my-directive]',",
        "    map: const {",
        "        'propAttr' : '@field', // attr",
        "        'propOneWay' : '=>field', // one-way",
        "        'propTwoWay' : '<=>field', // two-way",
        "    })",
        "class MyDirective {",
        "  var field;",
        "}",
        "",
        "main() {",
        "  var module = new Module();",
        "  module.type(MyDirective);",
        "  ngBootstrap(module: module);",
        "}"));
    FieldElement field = findMainElement("field");
    PropertyAccessorElement getter = field.getGetter();
    PropertyAccessorElement setter = field.getSetter();
    // index
    mainUnit.accept(index);
    List<RecordedRelation> relations = captureRecordedRelations();
    // @field
    {
      ExpectedLocation location = new ExpectedLocation(
          findMainElement("propAttr"),
          findMainOffset("field', // attr"),
          "field");
      assertNoRecordedRelation(relations, getter, IS_REFERENCED_BY_QUALIFIED, location);
      assertRecordedRelation(relations, setter, IS_REFERENCED_BY_QUALIFIED, location);
    }
    // =>field
    {
      ExpectedLocation location = new ExpectedLocation(
          findMainElement("propOneWay"),
          findMainOffset("field', // one-way"),
          "field");
      assertNoRecordedRelation(relations, getter, IS_REFERENCED_BY_QUALIFIED, location);
      assertRecordedRelation(relations, setter, IS_REFERENCED_BY_QUALIFIED, location);
    }
    // <=>field
    {
      ExpectedLocation location = new ExpectedLocation(
          findMainElement("propTwoWay"),
          findMainOffset("field', // two-way"),
          "field");
      assertRecordedRelation(relations, getter, IS_REFERENCED_BY_QUALIFIED, location);
      assertRecordedRelation(relations, setter, IS_REFERENCED_BY_QUALIFIED, location);
    }
  }

  private List<RecordedRelation> captureRecordedRelations() {
    return captureRelations(store);
  }
}
