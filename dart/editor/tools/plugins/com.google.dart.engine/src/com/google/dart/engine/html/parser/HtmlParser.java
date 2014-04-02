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

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.html.ast.HtmlScriptTagNode;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.html.ast.XmlAttributeNode;
import com.google.dart.engine.html.ast.XmlNode;
import com.google.dart.engine.html.ast.XmlTagNode;
import com.google.dart.engine.html.scanner.Token;
import com.google.dart.engine.parser.Parser;
import com.google.dart.engine.scanner.Scanner;
import com.google.dart.engine.scanner.SubSequenceReader;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.LineInfo;
import com.google.dart.engine.utilities.source.LineInfo.Location;

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
  /**
   * The line information associated with the source being parsed.
   */
  private LineInfo lineInfo;

  /**
   * The error listener to which errors will be reported.
   */
  private AnalysisErrorListener errorListener;

  private static final String APPLICATION_DART_IN_DOUBLE_QUOTES = "\"application/dart\"";
  private static final String APPLICATION_DART_IN_SINGLE_QUOTES = "'application/dart'";
  private static final String SCRIPT = "script";
  private static final String TYPE = "type";

  /**
   * A set containing the names of tags that do not have a closing tag.
   */
  public static Set<String> SELF_CLOSING = new HashSet<String>(Arrays.asList(new String[] {
      "area", "base", "basefont", "br", "col", "frame", "hr", "img", "input", "link", "meta",
      "param", "!"}));

  /**
   * Given the contents of an embedded expression that occurs at the given offset, parse it as a
   * Dart expression. The contents should not include the expression's delimiters.
   * 
   * @param source the source that contains that given token
   * @param token the token to start parsing from
   * @return the Dart expression that was parsed
   */
  public static Expression parseEmbeddedExpression(Source source,
      com.google.dart.engine.scanner.Token token, AnalysisErrorListener errorListener) {
    Parser parser = new Parser(source, errorListener);
    return parser.parseExpression(token);
  }

  /**
   * Given the contents of an embedded expression that occurs at the given offset, scans it as a
   * Dart code.
   * 
   * @param source the source of that contains the given contents
   * @param contents the contents to scan
   * @param contentOffset the offset of the contents in the larger file
   * @return the first Dart token
   */
  public static com.google.dart.engine.scanner.Token scanDartSource(Source source,
      LineInfo lineInfo, String contents, int contentOffset, AnalysisErrorListener errorListener) {
    Location location = lineInfo.getLocation(contentOffset);
    Scanner scanner = new Scanner(
        source,
        new SubSequenceReader(contents, contentOffset),
        errorListener);
    scanner.setSourceStart(location.getLineNumber(), location.getColumnNumber());
    return scanner.tokenize();
  }

  /**
   * Construct a parser for the specified source.
   * 
   * @param source the source being parsed
   * @param errorListener the error listener to which errors will be reported
   */
  public HtmlParser(Source source, AnalysisErrorListener errorListener) {
    super(source);
    this.errorListener = errorListener;
  }

  /**
   * Parse the given tokens.
   * 
   * @param token the first token in the stream of tokens to be parsed
   * @param lineInfo the line information created by the scanner
   * @return the parse result (not {@code null})
   */
  public HtmlUnit parse(Token token, LineInfo lineInfo) {
    this.lineInfo = lineInfo;
    List<XmlTagNode> tagNodes = parseTopTagNodes(token);
    return new HtmlUnit(token, tagNodes, getCurrentToken());
  }

  @Override
  protected XmlAttributeNode createAttributeNode(Token name, Token equals, Token value) {
    return new XmlAttributeNode(name, equals, value);
  }

  @Override
  protected XmlTagNode createTagNode(Token nodeStart, Token tag, List<XmlAttributeNode> attributes,
      Token attributeEnd, List<XmlTagNode> tagNodes, Token contentEnd, Token closingTag,
      Token nodeEnd) {
    if (isScriptNode(tag, attributes, tagNodes)) {
      HtmlScriptTagNode tagNode = new HtmlScriptTagNode(
          nodeStart,
          tag,
          attributes,
          attributeEnd,
          tagNodes,
          contentEnd,
          closingTag,
          nodeEnd);
      String contents = tagNode.getContent();
      int contentOffset = attributeEnd.getEnd();
      Location location = lineInfo.getLocation(contentOffset);
      Scanner scanner = new Scanner(
          getSource(),
          new SubSequenceReader(contents, contentOffset),
          errorListener);
      scanner.setSourceStart(location.getLineNumber(), location.getColumnNumber());
      com.google.dart.engine.scanner.Token firstToken = scanner.tokenize();
      Parser parser = new Parser(getSource(), errorListener);
      CompilationUnit unit = parser.parseCompilationUnit(firstToken);
      unit.setLineInfo(lineInfo);
      tagNode.setScript(unit);
      return tagNode;
    }
    return new XmlTagNode(
        nodeStart,
        tag,
        attributes,
        attributeEnd,
        tagNodes,
        contentEnd,
        closingTag,
        nodeEnd);
  }

  @Override
  protected boolean isSelfClosing(Token tag) {
    return SELF_CLOSING.contains(tag.getLexeme());
  }

  /**
   * Determine if the specified node is a Dart script.
   * 
   * @param node the node to be tested (not {@code null})
   * @return {@code true} if the node is a Dart script
   */
  private boolean isScriptNode(Token tag, List<XmlAttributeNode> attributes,
      List<XmlTagNode> tagNodes) {
    if (tagNodes.size() != 0 || !tag.getLexeme().equals(SCRIPT)) {
      return false;
    }
    for (XmlAttributeNode attribute : attributes) {
      if (attribute.getName().equals(TYPE)) {
        Token valueToken = attribute.getValueToken();
        if (valueToken != null) {
          String value = valueToken.getLexeme();
          if (value.equals(APPLICATION_DART_IN_DOUBLE_QUOTES)
              || value.equals(APPLICATION_DART_IN_SINGLE_QUOTES)) {
            return true;
          }
        }
      }
    }
    return false;
  }
}
