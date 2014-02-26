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
package com.google.dart.engine.html.parser;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.html.ast.XmlTagNode;
import com.google.dart.engine.html.parser.XmlValidator.Attributes;
import com.google.dart.engine.html.parser.XmlValidator.Tag;
import com.google.dart.engine.html.scanner.AbstractScanner;
import com.google.dart.engine.html.scanner.StringScanner;
import com.google.dart.engine.html.scanner.Token;
import com.google.dart.engine.source.TestSource;
import com.google.dart.engine.utilities.source.LineInfo;

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

public class HtmlParserTest extends EngineTestCase {
  /**
   * The name of the 'script' tag in an HTML file.
   */
  private static final String TAG_SCRIPT = "script";

  public void fail_parse_scriptWithComment() throws Exception {
    String scriptBody = createSource(//
        "      /**",
        "       *     <editable-label bind-value=\"dartAsignableValue\">",
        "       *     </editable-label>",
        "       */",
        "      class Foo {}");
    HtmlUnit htmlUnit = parse(createSource(//
        "  <html>",
        "    <body>",
        "      <script type=\"application/dart\">",
        scriptBody,
        "      </script>",
        "    </body>",
        "  </html>"));
    validate(
        htmlUnit,
        t("html", t("body", t("script", a("type", "\"application/dart\""), scriptBody))));
  }

  public void test_parse_attribute() throws Exception {
    HtmlUnit htmlUnit = parse("<html><body foo=\"sdfsdf\"></body></html>");
    validate(htmlUnit, t("html", t("body", a("foo", "\"sdfsdf\""), "")));
    XmlTagNode htmlNode = htmlUnit.getTagNodes().get(0);
    XmlTagNode bodyNode = htmlNode.getTagNodes().get(0);
    assertEquals("sdfsdf", bodyNode.getAttributes().get(0).getText());
  }

  public void test_parse_attribute_EOF() throws Exception {
    HtmlUnit htmlUnit = parse("<html><body foo=\"sdfsdf\"");
    validate(htmlUnit, t("html", t("body", a("foo", "\"sdfsdf\""), "")));
  }

  public void test_parse_attribute_EOF_missing_quote() throws Exception {
    HtmlUnit htmlUnit = parse("<html><body foo=\"sdfsd");
    validate(htmlUnit, t("html", t("body", a("foo", "\"sdfsd"), "")));
    XmlTagNode htmlNode = htmlUnit.getTagNodes().get(0);
    XmlTagNode bodyNode = htmlNode.getTagNodes().get(0);
    assertEquals("sdfsd", bodyNode.getAttributes().get(0).getText());
  }

  public void test_parse_attribute_extra_quote() throws Exception {
    HtmlUnit htmlUnit = parse("<html><body foo=\"sdfsdf\"\"></body></html>");
    validate(htmlUnit, t("html", t("body", a("foo", "\"sdfsdf\""), "")));
  }

  public void test_parse_attribute_single_quote() throws Exception {
    HtmlUnit htmlUnit = parse("<html><body foo='sdfsdf'></body></html>");
    validate(htmlUnit, t("html", t("body", a("foo", "'sdfsdf'"), "")));
    XmlTagNode htmlNode = htmlUnit.getTagNodes().get(0);
    XmlTagNode bodyNode = htmlNode.getTagNodes().get(0);
    assertEquals("sdfsdf", bodyNode.getAttributes().get(0).getText());
  }

  public void test_parse_comment_embedded() throws Exception {
    HtmlUnit htmlUnit = parse("<html <!-- comment -->></html>");
    validate(htmlUnit, t("html", ""));
  }

  public void test_parse_comment_first() throws Exception {
    HtmlUnit htmlUnit = parse("<!-- comment --><html></html>");
    validate(htmlUnit, t("html", ""));
  }

  public void test_parse_comment_in_content() throws Exception {
    HtmlUnit htmlUnit = parse("<html><!-- comment --></html>");
    validate(htmlUnit, t("html", "<!-- comment -->"));
  }

  public void test_parse_content() throws Exception {
    HtmlUnit htmlUnit = parse("<html>\n<p a=\"b\">blat \n </p>\n</html>");
    // XmlTagNode.getContent() does not include whitespace between '<' and '>' at this time
    validate(htmlUnit, t("html", "\n<pa=\"b\">blat \n </p>\n", t("p", a("a", "\"b\""), "blat \n ")));
  }

  public void test_parse_content_none() throws Exception {
    HtmlUnit htmlUnit = parse("<html><p/>blat<p/></html>");
    validate(htmlUnit, t("html", "<p/>blat<p/>", t("p", ""), t("p", "")));
  }

  public void test_parse_declaration() throws Exception {
    HtmlUnit htmlUnit = parse("<!DOCTYPE html>\n\n<html><p></p></html>");
    validate(htmlUnit, t("html", t("p", "")));
  }

  public void test_parse_directive() throws Exception {
    HtmlUnit htmlUnit = parse("<?xml ?>\n\n<html><p></p></html>");
    validate(htmlUnit, t("html", t("p", "")));
  }

  public void test_parse_getAttribute() throws Exception {
    HtmlUnit htmlUnit = parse("<html><body foo=\"sdfsdf\"></body></html>");
    XmlTagNode htmlNode = htmlUnit.getTagNodes().get(0);
    XmlTagNode bodyNode = htmlNode.getTagNodes().get(0);
    assertEquals("sdfsdf", bodyNode.getAttribute("foo").getText());
    assertEquals(null, bodyNode.getAttribute("bar"));
    assertEquals(null, bodyNode.getAttribute(null));
  }

  public void test_parse_getAttributeText() throws Exception {
    HtmlUnit htmlUnit = parse("<html><body foo=\"sdfsdf\"></body></html>");
    XmlTagNode htmlNode = htmlUnit.getTagNodes().get(0);
    XmlTagNode bodyNode = htmlNode.getTagNodes().get(0);
    assertEquals("sdfsdf", bodyNode.getAttributeText("foo"));
    assertEquals(null, bodyNode.getAttributeText("bar"));
    assertEquals(null, bodyNode.getAttributeText(null));
  }

  public void test_parse_headers() throws Exception {
    String code = createSource(//
        "<html>",
        "  <body>",
        "    <h2>000</h2>",
        "    <div>",
        "      111",
        "    </div>",
        "  </body>",
        "</html>");
    HtmlUnit htmlUnit = parse(code);
    validate(htmlUnit, t("html", t("body", t("h2", "000"), t("div"))));
  }

  public void test_parse_script() throws Exception {
    HtmlUnit htmlUnit = parse("<html><script >here is <p> some</script></html>");
    validate(htmlUnit, t("html", t("script", "here is <p> some")));
  }

  public void test_parse_self_closing() throws Exception {
    HtmlUnit htmlUnit = parse("<html>foo<br>bar</html>");
    validate(htmlUnit, t("html", "foo<br>bar", t("br", "")));
  }

  public void test_parse_self_closing_declaration() throws Exception {
    HtmlUnit htmlUnit = parse("<!DOCTYPE html><html>foo</html>");
    validate(htmlUnit, t("html", "foo"));
  }

  protected HtmlUnit parse(String contents) throws Exception {
    TestSource source = new TestSource(createFile("/test.dart"), contents);
    AbstractScanner scanner = new StringScanner(source, contents);
    scanner.setPassThroughElements(new String[] {TAG_SCRIPT});
    Token token = scanner.tokenize();
    LineInfo lineInfo = new LineInfo(scanner.getLineStarts());
    GatheringErrorListener errorListener = new GatheringErrorListener();
    HtmlUnit unit = new HtmlParser(source, errorListener).parse(token, lineInfo);
    errorListener.assertNoErrors();
    return unit;
  }

  Attributes a(String... keyValuePairs) {
    return new Attributes(keyValuePairs);
  }

  Tag t(String tag, Attributes attributes, String content, Tag... children) {
    return new Tag(tag, attributes, content, children);
  }

  Tag t(String tag, Attributes attributes, Tag... children) {
    return new Tag(tag, attributes, null, children);
  }

  Tag t(String tag, String content, Tag... children) {
    return new Tag(tag, new Attributes(), content, children);
  }

  Tag t(String tag, Tag... children) {
    return new Tag(tag, new Attributes(), null, children);
  }

  private void validate(HtmlUnit htmlUnit, Tag... expectedTags) {
    XmlValidator validator = new XmlValidator();
    validator.expectTags(expectedTags);
    htmlUnit.accept(validator);
    validator.assertValid();
  }
}
