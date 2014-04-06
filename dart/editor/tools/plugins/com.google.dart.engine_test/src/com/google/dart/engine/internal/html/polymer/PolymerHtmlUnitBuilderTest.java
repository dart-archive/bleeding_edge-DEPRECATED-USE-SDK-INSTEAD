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

import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.polymer.PolymerAttributeElement;
import com.google.dart.engine.error.PolymerCode;

/**
 * Test for {@link PolymerHtmlUnitBuilder}.
 */
public class PolymerHtmlUnitBuilderTest extends PolymerTest {
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
        "    <div>Hello!</div>",
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
        "    <div>Hello!</div>",
        "  </template>",
        "  <script type='application/dart' src='no-such-file.dart'></script>",
        "</polymer-element>",
        ""));
    resolveTagHtml();
    // HTML part is resolved
    assertNotNull(tagHtmlElement);
    assertNull(tagHtmlElement.getDartElement());
  }

  public void test_buildTagHtmlElement_error_AttributeFieldNotPublished() throws Exception {
    addTagDartSource(createSource(//
        "import 'polymer.dart';",
        "",
        "const otherAnnotation = null;",
        "",
        "@CustomTag('my-element') // marker",
        "class MyElement {",
        "  @otherAnnotation",
        "  String attr;",
        "}",
        ""));
    addTagHtmlSource(createSource(//
        "<!DOCTYPE html>",
        "",
        "<polymer-element name='my-element' attributes='attr'>",
        "  <template>",
        "    <div>Hello!</div>",
        "  </template>",
        "  <script type='application/dart' src='my-element.dart'></script>",
        "</polymer-element>",
        ""));
    resolveTagDart();
    resolveTagHtml();
    assertNoErrorsTagDart();
    assertErrors(tagHtmlSource, PolymerCode.ATTRIBUTE_FIELD_NOT_PUBLISHED);
    // Dart and HTML parts are resolved
    assertNotNull(tagDartElement);
    assertNotNull(tagHtmlElement);
    // attribute is still created
    PolymerAttributeElement[] attributes = tagHtmlElement.getAttributes();
    assertLength(1, attributes);
    {
      PolymerAttributeElement attribute = attributes[0];
      assertEquals("attr", attribute.getName());
      assertNotNull(attribute.getField());
    }
  }

  public void test_buildTagHtmlElement_error_DuplicateAttributeDefinition() throws Exception {
    addTagDartSource(createSource(//
        "import 'polymer.dart';",
        "",
        "@CustomTag('my-element') // marker",
        "class MyElement {",
        "  @published String attr;",
        "}",
        ""));
    addTagHtmlSource(createSource(//
        "<!DOCTYPE html>",
        "",
        "<polymer-element name='my-element' attributes='attr attr'>",
        "  <template>",
        "    <div>Hello!</div>",
        "  </template>",
        "  <script type='application/dart' src='my-element.dart'></script>",
        "</polymer-element>",
        ""));
    resolveTagDart();
    resolveTagHtml();
    assertNoErrorsTagDart();
    assertErrors(tagHtmlSource, PolymerCode.DUPLICATE_ATTRIBUTE_DEFINITION);
    // Dart and HTML parts are resolved
    assertNotNull(tagDartElement);
    assertNotNull(tagHtmlElement);
    // attribute is still created
    PolymerAttributeElement[] attributes = tagHtmlElement.getAttributes();
    assertLength(1, attributes);
    {
      PolymerAttributeElement attribute = attributes[0];
      assertEquals("attr", attribute.getName());
      assertNotNull(attribute.getField());
    }
  }

  public void test_buildTagHtmlElement_error_EmptyAttributes() throws Exception {
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
        "<polymer-element name='my-element' attributes=''>",
        "  <template>",
        "    <div>Hello!</div>",
        "  </template>",
        "  <script type='application/dart' src='my-element.dart'></script>",
        "</polymer-element>",
        ""));
    resolveTagDart();
    resolveTagHtml();
    assertNoErrorsTagDart();
    assertErrors(tagHtmlSource, PolymerCode.EMPTY_ATTRIBUTES);
    // Dart and HTML parts are resolved
    assertNotNull(tagDartElement);
    assertNotNull(tagHtmlElement);
    // no attributes
    PolymerAttributeElement[] attributes = tagHtmlElement.getAttributes();
    assertLength(0, attributes);
  }

  public void test_buildTagHtmlElement_error_InvalidAttributeName() throws Exception {
    addTagDartSource(createSource(//
        "import 'polymer.dart';",
        "",
        "@CustomTag('my-element') // marker",
        "class MyElement {",
        "  @published String goodAttr;",
        "}",
        ""));
    addTagHtmlSource(createSource(//
        "<!DOCTYPE html>",
        "",
        "<polymer-element name='my-element' attributes='1badAttr goodAttr'>",
        "  <template>",
        "    <div>Hello!</div>",
        "  </template>",
        "  <script type='application/dart' src='my-element.dart'></script>",
        "</polymer-element>",
        ""));
    resolveTagDart();
    resolveTagHtml();
    assertNoErrorsTagDart();
    assertErrors(tagHtmlSource, PolymerCode.INVALID_ATTRIBUTE_NAME);
    // Dart and HTML parts are resolved
    assertNotNull(tagDartElement);
    assertNotNull(tagHtmlElement);
    // one attribute is still created
    PolymerAttributeElement[] attributes = tagHtmlElement.getAttributes();
    assertLength(1, attributes);
    {
      PolymerAttributeElement attribute = attributes[0];
      assertEquals("goodAttr", attribute.getName());
    }
  }

  public void test_buildTagHtmlElement_error_InvalidTagName() throws Exception {
    addTagHtmlSource(createSource(//
        "<!DOCTYPE html>",
        "",
        "<polymer-element name='invalid name'>",
        "  <template>",
        "    <div>Hello!</div>",
        "  </template>",
        "</polymer-element>",
        ""));
    resolveTagHtml();
    assertErrors(tagHtmlSource, PolymerCode.INVALID_TAG_NAME);
    assertNull(tagHtmlElement);
  }

  public void test_buildTagHtmlElement_error_InvalidTagName_noValue() throws Exception {
    addTagHtmlSource(createSource(//
        "<!DOCTYPE html>",
        "",
        "<polymer-element name>",
        "  <template>",
        "    <div>Hello!</div>",
        "  </template>",
        "</polymer-element>",
        ""));
    resolveTagHtml();
    assertErrors(tagHtmlSource, PolymerCode.INVALID_TAG_NAME);
    assertNull(tagHtmlElement);
  }

  public void test_buildTagHtmlElement_error_MissingTagName() throws Exception {
    addTagHtmlSource(createSource(//
        "<!DOCTYPE html>",
        "",
        "<polymer-element>",
        "  <template>",
        "    <div>Hello!</div>",
        "  </template>",
        "</polymer-element>",
        ""));
    resolveTagHtml();
    assertErrors(tagHtmlSource, PolymerCode.MISSING_TAG_NAME);
    assertNull(tagHtmlElement);
  }

  public void test_buildTagHtmlElement_error_UndefinedAttributeField() throws Exception {
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
        "<polymer-element name='my-element' attributes='attr'>",
        "  <template>",
        "    <div>Hello!</div>",
        "  </template>",
        "  <script type='application/dart' src='my-element.dart'></script>",
        "</polymer-element>",
        ""));
    resolveTagDart();
    resolveTagHtml();
    assertNoErrorsTagDart();
    assertErrors(tagHtmlSource, PolymerCode.UNDEFINED_ATTRIBUTE_FIELD);
    // Dart and HTML parts are resolved
    assertNotNull(tagDartElement);
    assertNotNull(tagHtmlElement);
    // attribute is still created
    PolymerAttributeElement[] attributes = tagHtmlElement.getAttributes();
    assertLength(1, attributes);
    {
      PolymerAttributeElement attribute = attributes[0];
      assertEquals("attr", attribute.getName());
      assertNull(attribute.getField());
    }
  }

  public void test_buildTagHtmlElement_OK() throws Exception {
    addTagDartSource(createSource(//
        "import 'polymer.dart';",
        "",
        "@CustomTag('my-element') // marker",
        "class MyElement {",
        "  @published String attrA;",
        "  @published String attrB;",
        "}",
        ""));
    addTagHtmlSource(createSource(//
        "<!DOCTYPE html>",
        "",
        "<polymer-element name='my-element' attributes='attrA attrB'>",
        "  <template>",
        "    <div>Hello!</div>",
        "  </template>",
        "  <script type='application/dart' src='my-element.dart'></script>",
        "</polymer-element>",
        ""));
    resolveTagDart();
    resolveTagHtml();
    assertNoErrorsTag();
    // Dart and HTML parts are resolved
    assertNotNull(tagDartElement);
    assertNotNull(tagHtmlElement);
    assertEquals("my-element", tagDartElement.getName());
    assertEquals("my-element", tagHtmlElement.getName());
    assertEquals(findTagHtmlOffset("my-element' attributes="), tagHtmlElement.getNameOffset());
    // Dart and HTML parts should point at each other
    assertSame(tagDartElement, tagHtmlElement.getDartElement());
    assertSame(tagHtmlElement, tagDartElement.getHtmlElement());
    // check attributes
    PolymerAttributeElement[] attributes = tagHtmlElement.getAttributes();
    assertLength(2, attributes);
    {
      PolymerAttributeElement attribute = attributes[0];
      assertEquals("attrA", attribute.getName());
      assertEquals(findTagHtmlOffset("attrA "), attribute.getNameOffset());
      FieldElement field = attribute.getField();
      assertNotNull(field);
      assertEquals("attrA", field.getName());
    }
    {
      PolymerAttributeElement attribute = attributes[1];
      assertEquals("attrB", attribute.getName());
      assertEquals(findTagHtmlOffset("attrB'>"), attribute.getNameOffset());
      FieldElement field = attribute.getField();
      assertNotNull(field);
      assertEquals("attrB", field.getName());
    }
  }

  public void test_buildTagHtmlElement_OK_noScript() throws Exception {
    addTagHtmlSource(createSource(//
        "<!DOCTYPE html>",
        "",
        "<polymer-element name='my-element'>",
        "  <template>",
        "    <div>Hello!</div>",
        "  </template>",
        "</polymer-element>",
        ""));
    resolveTagHtml();
    assertNoErrorsTagHtml();
    // HTML part is resolved
    assertNotNull(tagHtmlElement);
    assertNull(tagHtmlElement.getDartElement());
  }

  public void test_isValidAttributeName() throws Exception {
    // empty
    assertFalse(PolymerHtmlUnitBuilder.isValidAttributeName(""));
    // invalid first character
    assertFalse(PolymerHtmlUnitBuilder.isValidAttributeName(" "));
    assertFalse(PolymerHtmlUnitBuilder.isValidAttributeName("-"));
    assertFalse(PolymerHtmlUnitBuilder.isValidAttributeName("0"));
    // invalid character in the middle
    assertFalse(PolymerHtmlUnitBuilder.isValidAttributeName("a&"));
    assertFalse(PolymerHtmlUnitBuilder.isValidAttributeName("a-b"));
    // OK
    assertTrue(PolymerHtmlUnitBuilder.isValidAttributeName("a"));
    assertTrue(PolymerHtmlUnitBuilder.isValidAttributeName("bb"));
  }

  public void test_isValidTagName() throws Exception {
    // empty
    assertFalse(PolymerHtmlUnitBuilder.isValidTagName(""));
    // invalid first character
    assertFalse(PolymerHtmlUnitBuilder.isValidTagName(" "));
    assertFalse(PolymerHtmlUnitBuilder.isValidTagName("-"));
    assertFalse(PolymerHtmlUnitBuilder.isValidTagName("0"));
    assertFalse(PolymerHtmlUnitBuilder.isValidTagName("&"));
    // invalid character in the middle
    assertFalse(PolymerHtmlUnitBuilder.isValidTagName("a&"));
    // no '-'
    assertFalse(PolymerHtmlUnitBuilder.isValidTagName("a"));
    // forbidden names
    assertFalse(PolymerHtmlUnitBuilder.isValidTagName("annotation-xml"));
    assertFalse(PolymerHtmlUnitBuilder.isValidTagName("color-profile"));
    assertFalse(PolymerHtmlUnitBuilder.isValidTagName("font-face"));
    assertFalse(PolymerHtmlUnitBuilder.isValidTagName("font-face-src"));
    assertFalse(PolymerHtmlUnitBuilder.isValidTagName("font-face-uri"));
    assertFalse(PolymerHtmlUnitBuilder.isValidTagName("font-face-format"));
    assertFalse(PolymerHtmlUnitBuilder.isValidTagName("font-face-name"));
    assertFalse(PolymerHtmlUnitBuilder.isValidTagName("missing-glyph"));
    // OK
    assertTrue(PolymerHtmlUnitBuilder.isValidTagName("a-b"));
    assertTrue(PolymerHtmlUnitBuilder.isValidTagName("a-b-c"));
    assertTrue(PolymerHtmlUnitBuilder.isValidTagName("aaa-bbb"));
  }
}
