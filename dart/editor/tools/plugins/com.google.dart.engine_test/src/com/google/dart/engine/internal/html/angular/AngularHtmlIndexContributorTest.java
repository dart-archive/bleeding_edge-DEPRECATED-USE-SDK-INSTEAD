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
package com.google.dart.engine.internal.html.angular;

import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.angular.AngularComponentElement;
import com.google.dart.engine.element.angular.AngularPropertyElement;
import com.google.dart.engine.element.angular.AngularSelectorElement;
import com.google.dart.engine.index.IndexStore;
import com.google.dart.engine.internal.index.IndexConstants;
import com.google.dart.engine.internal.index.IndexContributorHelper.ExpectedLocation;
import com.google.dart.engine.internal.index.IndexContributorHelper.RecordedRelation;

import static com.google.dart.engine.internal.index.IndexContributorHelper.assertRecordedRelation;
import static com.google.dart.engine.internal.index.IndexContributorHelper.captureRelations;

import static org.mockito.Mockito.mock;

import java.util.List;

public class AngularHtmlIndexContributorTest extends AngularTest {
  private IndexStore store = mock(IndexStore.class);
  private AngularHtmlIndexContributor index = new AngularHtmlIndexContributor(store);

  public void test_expression_inAttribute() throws Exception {
    addMyController();
    resolveIndex(createHtmlWithMyController(//
        "  <button title='{{ctrl.field}}'>Remove</button>",
        ""));
    // prepare elements
    Element fieldGetter = ((FieldElement) findMainElement("field")).getGetter();
    // index
    indexUnit.accept(index);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        fieldGetter,
        IndexConstants.IS_REFERENCED_BY_QUALIFIED,
        new ExpectedLocation(indexHtmlUnit, findOffset("field}}"), "field"));
  }

  public void test_expression_inContent() throws Exception {
    addMyController();
    resolveIndex(createHtmlWithMyController(//
        "      {{ctrl.field}}",
        ""));
    // prepare elements
    Element fieldGetter = ((FieldElement) findMainElement("field")).getGetter();
    // index
    indexUnit.accept(index);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        fieldGetter,
        IndexConstants.IS_REFERENCED_BY_QUALIFIED,
        new ExpectedLocation(indexHtmlUnit, findOffset("field}}"), "field"));
  }

  public void test_expression_ngRepeat() throws Exception {
    addMyController();
    resolveIndex(createHtmlWithMyController(//
        "  <li ng-repeat='name in ctrl.names'>",
        "    {{name}}",
        "  </li>",
        ""));
    // prepare elements
    Element namesElement = ((FieldElement) findMainElement("names")).getGetter();
    Element nameElement = findIndexElement("name");
    // index
    indexUnit.accept(index);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        namesElement,
        IndexConstants.IS_REFERENCED_BY_QUALIFIED,
        new ExpectedLocation(indexHtmlUnit, findOffset("names'>"), "names"));
    assertRecordedRelation(relations, nameElement, IndexConstants.IS_READ_BY, new ExpectedLocation(
        indexHtmlUnit,
        findOffset("name}}"),
        "name"));
  }

  public void test_NgComponent_templateFile() throws Exception {
    addMainSource(createSource("",//
        "import 'angular.dart';",
        "",
        "@NgComponent(",
        "    templateUrl: 'my_template.html', cssUrl: 'my_styles.css',",
        "    publishAs: 'ctrl',",
        "    selector: 'myComponent')",
        "class MyComponent {",
        "  String field;",
        "}"));
    addIndexSource("/my_template.html", createSource(//
        "    <div>",
        "      {{ctrl.field}}",
        "    </div>"));
    contextHelper.addSource("/my_styles.css", "");
    contextHelper.runTasks();
    resolveMain();
    resolveIndex();
    // prepare elements
    AngularComponentElement componentElement = findMainElement("ctrl");
    FieldElement field = findMainElement("field");
    PropertyAccessorElement fieldGetter = field.getGetter();
    // index
    indexUnit.accept(index);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        componentElement,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(indexHtmlUnit, findOffset("ctrl.field"), "ctrl"));
    assertRecordedRelation(
        relations,
        fieldGetter,
        IndexConstants.IS_REFERENCED_BY_QUALIFIED,
        new ExpectedLocation(indexHtmlUnit, findOffset("field}}"), "field"));
  }

  public void test_NgComponent_use() throws Exception {
    resolveMainSource(createSource("",//
        "import 'angular.dart';",
        "",
        "@NgComponent(",
        "    templateUrl: 'my_template.html', cssUrl: 'my_styles.css',",
        "    publishAs: 'ctrl',",
        "    selector: 'myComponent', // selector",
        "    map: const {'attrA' : '=>setA', 'attrB' : '@setB'})",
        "class MyComponent {",
        "  set setA(value) {}",
        "  set setB(value) {}",
        "}"));
    resolveIndex(createHtmlWithMyController(//
    "<myComponent attrA='null' attrB='str'/>"));
    // prepare elements
    AngularSelectorElement selectorElement = findMainElement("myComponent");
    AngularPropertyElement attrA = findMainElement("attrA");
    AngularPropertyElement attrB = findMainElement("attrB");
    // index
    indexUnit.accept(index);
    // verify
    List<RecordedRelation> relations = captureRecordedRelations();
    assertRecordedRelation(
        relations,
        selectorElement,
        IndexConstants.IS_REFERENCED_BY,
        new ExpectedLocation(indexHtmlUnit, findOffset("myComponent attrA='null"), "myComponent"));
    assertRecordedRelation(relations, attrA, IndexConstants.IS_REFERENCED_BY, new ExpectedLocation(
        indexHtmlUnit,
        findOffset("attrA='null"),
        "attrA"));
    assertRecordedRelation(relations, attrB, IndexConstants.IS_REFERENCED_BY, new ExpectedLocation(
        indexHtmlUnit,
        findOffset("attrB='str"),
        "attrB"));
  }

  private List<RecordedRelation> captureRecordedRelations() {
    return captureRelations(store);
  }
}
