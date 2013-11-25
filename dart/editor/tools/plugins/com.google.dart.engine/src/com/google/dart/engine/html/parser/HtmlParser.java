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
import com.google.dart.engine.html.ast.AttributeWithEmbeddedExpressions;
import com.google.dart.engine.html.ast.EmbeddedExpression;
import com.google.dart.engine.html.ast.HtmlScriptTagNode;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.html.ast.TagWithEmbeddedExpressions;
import com.google.dart.engine.html.ast.XmlAttributeNode;
import com.google.dart.engine.html.ast.XmlNode;
import com.google.dart.engine.html.ast.XmlTagNode;
import com.google.dart.engine.html.scanner.HtmlScanResult;
import com.google.dart.engine.html.scanner.HtmlScanner;
import com.google.dart.engine.html.scanner.Token;
import com.google.dart.engine.html.scanner.TokenType;
import com.google.dart.engine.parser.Parser;
import com.google.dart.engine.scanner.Scanner;
import com.google.dart.engine.scanner.SubSequenceReader;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.LineInfo;
import com.google.dart.engine.utilities.source.LineInfo.Location;

import java.util.ArrayList;
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
  private static final String OPENING_DELIMITER = "{{";
  private static final String CLOSING_DELIMITER = "}}";
  private static final String SCRIPT = "script";
  private static final String TYPE = "type";

  /**
   * A set containing the names of tags that do not have a closing tag.
   */
  public static Set<String> SELF_CLOSING = new HashSet<String>(Arrays.asList(new String[] {
      "area", "base", "basefont", "br", "col", "frame", "hr", "img", "input", "link", "meta",
      "param", "!", "h1", "h2", "h3", "h4", "h5", "h6"}));

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

  public Token getEndToken(Token tag, List<XmlAttributeNode> attributes, Token attributeEnd,
      List<XmlTagNode> tagNodes, Token contentEnd, Token closingTag, Token nodeEnd) {
    if (nodeEnd != null) {
      return nodeEnd;
    }
    if (closingTag != null) {
      return closingTag;
    }
    if (contentEnd != null) {
      return contentEnd;
    }
    if (!tagNodes.isEmpty()) {
      return tagNodes.get(tagNodes.size() - 1).getEndToken();
    }
    if (attributeEnd != null) {
      return attributeEnd;
    }
    if (!attributes.isEmpty()) {
      return attributes.get(attributes.size() - 1).getEndToken();
    }
    return tag;
  }

  /**
   * Parse the tokens specified by the given scan result.
   * 
   * @param scanResult the result of scanning an HTML source (not {@code null})
   * @return the parse result (not {@code null})
   */
  public HtmlParseResult parse(HtmlScanResult scanResult) {
    int[] lineStarts = scanResult.getLineStarts();
    lineInfo = new LineInfo(lineStarts);
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
  protected XmlAttributeNode createAttributeNode(Token name, Token equals, Token value) {
    ArrayList<EmbeddedExpression> expressions = new ArrayList<EmbeddedExpression>();
    addEmbeddedExpressions(expressions, value);
    if (expressions.isEmpty()) {
      return new XmlAttributeNode(name, equals, value);
    }
    return new AttributeWithEmbeddedExpressions(
        name,
        equals,
        value,
        expressions.toArray(new EmbeddedExpression[expressions.size()]));
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
    Token token = nodeStart;
    Token endToken = getEndToken(
        tag,
        attributes,
        attributeEnd,
        tagNodes,
        contentEnd,
        closingTag,
        nodeEnd);
    ArrayList<EmbeddedExpression> expressions = new ArrayList<EmbeddedExpression>();
    while (token != endToken) {
      if (token.getType() == TokenType.TEXT) {
        addEmbeddedExpressions(expressions, token);
      }
      token = token.getNext();
    }
    if (expressions.isEmpty()) {
      return super.createTagNode(
          nodeStart,
          tag,
          attributes,
          attributeEnd,
          tagNodes,
          contentEnd,
          closingTag,
          nodeEnd);
    }
    return new TagWithEmbeddedExpressions(
        nodeStart,
        tag,
        attributes,
        attributeEnd,
        tagNodes,
        contentEnd,
        closingTag,
        nodeEnd,
        expressions.toArray(new EmbeddedExpression[expressions.size()]));
  }

  @Override
  protected boolean isSelfClosing(Token tag) {
    return SELF_CLOSING.contains(tag.getLexeme());
  }

  /**
   * Parse the value of the given token for embedded expressions, and add any embedded expressions
   * that are found to the given list of expressions.
   * 
   * @param expressions the list to which embedded expressions are to be added
   * @param token the token whose value is to be parsed
   */
  private void addEmbeddedExpressions(ArrayList<EmbeddedExpression> expressions, Token token) {
    String lexeme = token.getLexeme();
    int startIndex = lexeme.indexOf(OPENING_DELIMITER);
    while (startIndex >= 0) {
      int endIndex = lexeme.indexOf(CLOSING_DELIMITER, startIndex + 2);
      if (endIndex < 0) {
        // TODO(brianwilkerson) Should we report this error or will it be reported by something else?
        return;
      }
      int offset = token.getOffset();
      expressions.add(new EmbeddedExpression(startIndex, parseEmbeddedExpression(
          lexeme.substring(startIndex + 2, endIndex),
          offset + startIndex), endIndex));
      startIndex = lexeme.indexOf(OPENING_DELIMITER, endIndex + 2);
    }
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
      if (attribute.getName().getLexeme().equals(TYPE)) {
        Token valueToken = attribute.getValue();
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

  /**
   * Given the contents of an embedded expression that occurs at the given offset, parse it as a
   * Dart expression. The contents should not include the expression's delimiters.
   * 
   * @param contents the contents of the expression
   * @param contentOffset the offset of the expression in the larger file
   * @return the Dart expression that was parsed
   */
  private Expression parseEmbeddedExpression(String contents, int contentOffset) {
    Location location = lineInfo.getLocation(contentOffset);
    Scanner scanner = new Scanner(
        getSource(),
        new SubSequenceReader(contents, contentOffset),
        errorListener);
    scanner.setSourceStart(location.getLineNumber(), location.getColumnNumber());
    com.google.dart.engine.scanner.Token firstToken = scanner.tokenize();
    Parser parser = new Parser(getSource(), errorListener);
    return parser.parseExpression(firstToken);
  }
}
