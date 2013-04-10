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

import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.html.ast.XmlNode;
import com.google.dart.engine.html.ast.XmlTagNode;
import com.google.dart.engine.html.scanner.HtmlScanResult;
import com.google.dart.engine.html.scanner.HtmlScanner;
import com.google.dart.engine.html.scanner.Token;
import com.google.dart.engine.source.Source;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Instances of the class {@code HtmlParser} are used to parse tokens into a AST structure comprised
 * of {@link XmlNode}s.
 * 
 * @coverage dart.engine.html
 */
public class HtmlParser extends XmlParser {

  public static Set<String> SELF_CLOSING = new HashSet<String>(Arrays.asList(new String[] {
      "area", "base", "basefont", "br", "col", "frame", "hr", "img", "input", "link", "meta",
      "param", "!", "h1", "h2", "h3", "h4", "h5", "h6"}));

  /**
   * Construct a parser for the specified source.
   * 
   * @param source the source being parsed
   */
  public HtmlParser(Source source) {
    super(source);
  }

  /**
   * Parse the tokens specified by the given scan result.
   * 
   * @param scanResult the result of scanning an HTML source (not {@code null})
   * @return the parse result (not {@code null})
   */
  public HtmlParseResult parse(HtmlScanResult scanResult) {
    Token firstToken = scanResult.getToken();
    List<XmlTagNode> tagNodes = parseTopTagNodes(firstToken);
    HtmlUnit unit = new HtmlUnit(firstToken, tagNodes, getCurrentToken());
    return new HtmlParseResult(
        scanResult.getModificationTime(),
        firstToken,
        scanResult.getLineStarts(),
        unit);
  }

  /**
   * Scan then parse the specified source.
   * 
   * @param source the source to be scanned and parsed (not {@code null})
   * @return the parse result (not {@code null})
   */
  public HtmlParseResult parse(Source source) throws Exception {
    HtmlScanner scanner = new HtmlScanner(source);
    source.getContents(scanner);
    return parse(scanner.getResult());
  }

  @Override
  protected boolean isSelfClosing(Token tag) {
    return SELF_CLOSING.contains(tag.getLexeme());
  }
}
