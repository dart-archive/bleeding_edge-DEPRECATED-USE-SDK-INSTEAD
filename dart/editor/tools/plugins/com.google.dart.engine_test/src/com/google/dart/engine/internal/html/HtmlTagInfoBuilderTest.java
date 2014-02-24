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
package com.google.dart.engine.internal.html;

import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.html.parser.HtmlParserTest;

public class HtmlTagInfoBuilderTest extends HtmlParserTest {
  public void test_buider() throws Exception {
    HtmlTagInfoBuilder builder = new HtmlTagInfoBuilder();
    HtmlUnit unit = parse(createSource(//
        "<html>",
        "  <body>",
        "    <div id=\"x\"></div>",
        "    <p class='c'></p>",
        "    <div class='c'></div>",
        "  </body>",
        "</html>"));
    unit.accept(builder);
    HtmlTagInfo info = builder.getTagInfo();
    assertNotNull(info);
    String[] allTags = info.getAllTags();
    assertLength(4, allTags);
    assertEquals("div", info.getTagWithId("x"));
    String[] tagsWithClass = info.getTagsWithClass("c");
    assertLength(2, tagsWithClass);
  }
}
