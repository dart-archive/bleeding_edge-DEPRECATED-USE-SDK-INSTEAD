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

import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.html.ast.HtmlUnitUtils;

public class AngularHtmlUnitResolverTest extends AngularTest {

  public void test_notResolved_no_ngBootstrap_invocation() throws Exception {
    contextHelper.addSource("/main.dart", "// just empty script");
    resolveIndex(//
        "<html ng-app>",
        "  <body>",
        "    <div my-marker>",
        "      {{ctrl.field}}",
        "    </div>",
        "    <script type='application/dart' src='main.dart'></script>",
        "  </body>",
        "</html>");
    assertNoErrors();
    // Angular is not initialized, so "ctrl" is not parsed
    Expression expression = HtmlUnitUtils.getExpression(indexUnit, findOffset("ctrl"));
    assertNull(expression);
  }

  public void test_notResolved_noDartScript() throws Exception {
    resolveIndex(//
        "<html ng-app>",
        "  <body>",
        "    <div my-marker>",
        "      {{ctrl.field}}",
        "    </div>",
        "  </body>",
        "</html>");
    assertNoErrors();
    // Angular is not initialized, so "ctrl" is not parsed
    Expression expression = HtmlUnitUtils.getExpression(indexUnit, findOffset("ctrl"));
    assertNull(expression);
  }

  public void test_notResolved_notAngular() throws Exception {
    resolveIndex(//
        "<html no-ng-app>",
        "  <body>",
        "    <div my-marker>",
        "      {{ctrl.field}}",
        "    </div>",
        "  </body>",
        "</html>");
    assertNoErrors();
    // Angular is not initialized, so "ctrl" is not parsed
    Expression expression = HtmlUnitUtils.getExpression(indexUnit, findOffset("ctrl"));
    assertNull(expression);
  }

  public void test_notResolved_wrongControllerMarker() throws Exception {
    addSingleFieldController();
    resolveIndex(//
        "<html ng-app>",
        "  <body>",
        "    <div not-my-marker>",
        "      {{ctrl.field}}",
        "    </div>",
        "    <script type='application/dart' src='main.dart'></script>",
        "  </body>",
        "</html>");
    assertErrors(indexSource, StaticWarningCode.UNDEFINED_IDENTIFIER);
    // "ctrl" is not resolved
    SimpleIdentifier identifier = findIdentifier("ctrl");
    assertNull(identifier.getBestElement());
  }

  public void test_resolveExpression_inTag() throws Exception {
    addSingleFieldController();
    resolveIndex(//
        "<html ng-app>",
        "  <body>",
        "    <div my-marker>",
        "      {{ctrl.field}}",
        "    </div>",
        "    <script type='application/dart' src='main.dart'></script>",
        "  </body>",
        "</html>");
    assertNoErrors();
    verify(indexSource);
    assertResolvedIdentifier("ctrl", "MyController");
  }

  public void test_resolveExpression_ngApp_onBody() throws Exception {
    addSingleFieldController();
    resolveIndex(//
        "<html>",
        "  <body ng-app>",
        "    <div my-marker>",
        "      {{ctrl.field}}",
        "    </div>",
        "    <script type='application/dart' src='main.dart'></script>",
        "  </body>",
        "</html>");
    assertNoErrors();
    verify(indexSource);
    assertResolvedIdentifier("ctrl", "MyController");
  }
}
