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

  public void test_inAttribute() throws Exception {
    addMyController();
    resolveIndex(//
        "<html>",
        "  <body ng-app>",
        "    <div my-marker>",
        "      <button title='{{ctrl.field}}'>Remove</button>",
        "    </div>",
        "    <script type='application/dart' src='main.dart'></script>",
        "  </body>",
        "</html>");
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
        new ExpectedLocation(indexDartUnit, findOffset("field}}"), "field"));
  }

  public void test_inContent() throws Exception {
    addMyController();
    resolveIndex(//
        "<html>",
        "  <body ng-app>",
        "    <div my-marker>",
        "      {{ctrl.field}}",
        "    </div>",
        "    <script type='application/dart' src='main.dart'></script>",
        "  </body>",
        "</html>");
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
        new ExpectedLocation(indexDartUnit, findOffset("field}}"), "field"));
  }

  public void test_ngRepeat() throws Exception {
    addMyController();
    resolveIndex(//
        "<html ng-app>",
        "  <body>",
        "    <div my-marker>",
        "      <li ng-repeat='name in ctrl.names'>",
        "        {{name}}",
        "      </li>",
        "    </div>",
        "    <script type='application/dart' src='main.dart'></script>",
        "  </body>",
        "</html>");
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
        new ExpectedLocation(indexDartUnit, findOffset("names'>"), "names"));
    assertRecordedRelation(relations, nameElement, IndexConstants.IS_READ_BY, new ExpectedLocation(
        indexDartUnit,
        findOffset("name}}"),
        "name"));
  }

  private List<RecordedRelation> captureRecordedRelations() {
    return captureRelations(store);
  }
}
