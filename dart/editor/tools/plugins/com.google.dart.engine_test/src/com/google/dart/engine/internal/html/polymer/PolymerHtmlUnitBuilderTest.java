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
package com.google.dart.engine.internal.html.polymer;

/**
 * Test for {@link PolymerHtmlUnitBuilder}.
 */
public class PolymerHtmlUnitBuilderTest extends PolymerTest {
  public void test_buildTagHtmlElement_bad_script_no() throws Exception {
    addTagHtmlSource(createSource(//
        "<!DOCTYPE html>",
        "",
        "<polymer-element name='my-element'>",
        "  <template>",
        "    <div>",
        "      Hello!",
        "    </div>",
        "  </template>",
        "</polymer-element>",
        ""));
    resolveTagHtml();
    // HTML part is resolved
    assertNotNull(tagHtmlElement);
    assertNull(tagHtmlElement.getDartElement());
  }

  public void test_buildTagHtmlElement_bad_script_noSuchCustomTag() throws Exception {
    addTagDartSource(createSource(//
        "import 'polymer.dart';",
        "",
        "@CustomTag('other-name')",
        "class MyElement {",
        "}",
        ""));
    addTagHtmlSource(createSource(//
        "<!DOCTYPE html>",
        "",
        "<polymer-element name='my-element'>",
        "  <template>",
        "    <div>",
        "      Hello!",
        "    </div>",
        "  </template>",
        "  <script type='application/dart' src='my-element.dart'></script>",
        "</polymer-element>",
        ""));
    resolveTagDart();
    resolveTagHtml();
    // HTML part is resolved
    assertNotNull(tagHtmlElement);
    // ...but no Dart part found
    assertNull(tagHtmlElement.getDartElement());
  }

  public void test_buildTagHtmlElement_bad_script_notResolved() throws Exception {
    addTagHtmlSource(createSource(//
        "<!DOCTYPE html>",
        "",
        "<polymer-element name='my-element'>",
        "  <template>",
        "    <div>",
        "      Hello!",
        "    </div>",
        "  </template>",
        "  <script type='application/dart' src='no-such-file.dart'></script>",
        "</polymer-element>",
        ""));
    resolveTagHtml();
    // HTML part is resolved
    assertNotNull(tagHtmlElement);
    assertNull(tagHtmlElement.getDartElement());
  }

  public void test_buildTagHtmlElement_OK() throws Exception {
    addTagDartSource(createSource(//
        "import 'polymer.dart';",
        "",
        "@CustomTag('my-element') // marker",
        "class MyElement {",
        "}",
        ""));
    addTagHtmlSource(createSource(//
        "<!DOCTYPE html>",
        "",
        "<polymer-element name='my-element'>",
        "  <template>",
        "    <div>",
        "      Hello!",
        "    </div>",
        "  </template>",
        "  <script type='application/dart' src='my-element.dart'></script>",
        "</polymer-element>",
        ""));
    resolveTagDart();
    resolveTagHtml();
    // Dart and HTML parts are resolved
    assertNotNull(tagDartElement);
    assertNotNull(tagHtmlElement);
    assertEquals("my-element", tagDartElement.getName());
    assertEquals("my-element", tagHtmlElement.getName());
    assertEquals(findTagHtmlOffset("my-element'>"), tagHtmlElement.getNameOffset());
    // Dart and HTML parts should point at each other
    assertSame(tagDartElement, tagHtmlElement.getDartElement());
    assertSame(tagHtmlElement, tagDartElement.getHtmlElement());
    // TODO(scheglov) add test/implementation for attributes
  }
}
