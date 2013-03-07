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

package com.google.dart.tools.ui.web.css.model;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;

// TODO(devoncarew): add more tests

public class CssParserTest extends TestCase {

  public void test_createEmpty() {
    CssDocument document = CssParser.createEmpty();

    assertNotNull(document);
    assertEquals("document", document.getLabel());
    assertNull(document.getParent());
    assertEquals(0, document.getChildren().size());
  }

  public void test_parse1() {
    CssDocument document = parse("foo { background: blue; }");

    assertEquals(1, document.getChildren().size());

    CssSection section = (CssSection) document.getChildren().get(0);

    assertEquals("foo", section.getLabel());

    CssBody body = section.getBody();
    assertEquals(1, body.getChildren().size());

    CssProperty property = (CssProperty) body.getChildren().get(0);
    assertEquals("background", property.getLabel());
    assertEquals("blue", property.getValue().getValue());
  }

  private CssDocument parse(String content) {
    CssParser parser = new CssParser(new Document(content));

    return parser.parse();
  }

}
