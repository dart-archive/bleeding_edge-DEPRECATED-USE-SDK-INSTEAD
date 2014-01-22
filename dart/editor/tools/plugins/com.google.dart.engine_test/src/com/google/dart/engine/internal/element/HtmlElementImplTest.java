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
package com.google.dart.engine.internal.element;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.internal.context.AnalysisContextImpl;

import static com.google.dart.engine.element.ElementFactory.htmlUnit;

public class HtmlElementImplTest extends EngineTestCase {
  public void test_equals_differentSource() {
    AnalysisContextImpl context = createAnalysisContext();
    HtmlElementImpl elementA = htmlUnit(context, "indexA.html");
    HtmlElementImpl elementB = htmlUnit(context, "indexB.html");
    assertFalse(elementA.equals(elementB));
  }

  public void test_equals_null() {
    AnalysisContextImpl context = createAnalysisContext();
    HtmlElementImpl element = htmlUnit(context, "index.html");
    assertFalse(element.equals(null));
  }

  public void test_equals_sameSource() {
    AnalysisContextImpl context = createAnalysisContext();
    HtmlElementImpl elementA = htmlUnit(context, "index.html");
    HtmlElementImpl elementB = htmlUnit(context, "index.html");
    assertTrue(elementA.equals(elementB));
  }

  public void test_equals_self() {
    AnalysisContextImpl context = createAnalysisContext();
    HtmlElementImpl element = htmlUnit(context, "index.html");
    assertTrue(element.equals(element));
  }
}
