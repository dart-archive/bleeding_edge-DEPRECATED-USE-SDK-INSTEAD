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

import com.google.dart.engine.html.ast.XmlAttributeNode;
import com.google.dart.engine.html.ast.XmlNode;
import com.google.dart.engine.html.ast.XmlTagNode;
import com.google.dart.engine.html.scanner.Token;
import com.google.dart.engine.html.scanner.TokenType;
import com.google.dart.engine.source.Source;

import static com.google.dart.engine.html.scanner.TokenType.COMMENT;
import static com.google.dart.engine.html.scanner.TokenType.DECLARATION;
import static com.google.dart.engine.html.scanner.TokenType.DIRECTIVE;
import static com.google.dart.engine.html.scanner.TokenType.EOF;
import static com.google.dart.engine.html.scanner.TokenType.EQ;
import static com.google.dart.engine.html.scanner.TokenType.GT;
import static com.google.dart.engine.html.scanner.TokenType.LT;
import static com.google.dart.engine.html.scanner.TokenType.LT_SLASH;
import static com.google.dart.engine.html.scanner.TokenType.SLASH_GT;
import static com.google.dart.engine.html.scanner.TokenType.STRING;
import static com.google.dart.engine.html.scanner.TokenType.TAG;

import java.util.ArrayList;
import java.util.List;

/**
 * Instances of the class {@code XmlParser} are used to parse tokens into a AST structure comprised
 * of {@link XmlNode}s.
 * 
 * @coverage dart.engine.html
 */
public class XmlParser {

  /**
   * The source being parsed.
   */
  private final Source source;

  /**
   * The next token to be parsed.
   */
  private Token currentToken;

  /**
   * Construct a parser for the specified source.
   * 
   * @param source the source being parsed
   */
  public XmlParser(Source source) {
    this.source = source;
  }

  /**
   * Answer the source being parsed.
   * 
   * @return the source
   */
  public Source getSource() {
    return source;
  }

  /**
   * Create a node representing an attribute.
   * 
   * @param name the name of the attribute
   * @param equals the equals sign, or {@code null} if there is no value
   * @param value the value of the attribute
   * @return the node that was created
   */
  protected XmlAttributeNode createAttributeNode(Token name, Token equals, Token value) {
    return new XmlAttributeNode(name, equals, value);
  }

  /**
   * Create a node representing a tag.
   * 
   * @param nodeStart the token marking the beginning of the tag
   * @param tag the name of the tag
   * @param attributes the attributes in the tag
   * @param attributeEnd the token terminating the region where attributes can be
   * @param tagNodes the children of the tag
   * @param contentEnd the token that starts the closing tag
   * @param closingTag the name of the tag that occurs in the closing tag
   * @param nodeEnd the last token in the tag
   * @return the node that was created
   */
  protected XmlTagNode createTagNode(Token nodeStart, Token tag, List<XmlAttributeNode> attributes,
      Token attributeEnd, List<XmlTagNode> tagNodes, Token contentEnd, Token closingTag,
      Token nodeEnd) {
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

  /**
   * Answer {@code true} if the specified tag is self closing and thus should never have content or
   * child tag nodes.
   * 
   * @param tag the tag (not {@code null})
   * @return {@code true} if self closing
   */
  protected boolean isSelfClosing(Token tag) {
    return false;
  }

  /**
   * Parse the entire token stream and in the process, advance the current token to the end of the
   * token stream.
   * 
   * @return the list of tag nodes found (not {@code null}, contains no {@code null})
   */
  protected List<XmlTagNode> parseTopTagNodes(Token firstToken) {
    currentToken = firstToken;
    List<XmlTagNode> tagNodes = new ArrayList<XmlTagNode>();
    TokenType type = currentToken.getType();
    while (type != EOF) {
      if (type == LT) {
        tagNodes.add(parseTagNode());
      } else if (type == DECLARATION || type == DIRECTIVE || type == COMMENT) {
        // ignored tokens
        currentToken = currentToken.getNext();
      } else {
        reportUnexpectedToken();
        currentToken = currentToken.getNext();
      }
      type = currentToken.getType();
    }
    return tagNodes;
  }

  /**
   * Answer the current token.
   * 
   * @return the current token
   */
  Token getCurrentToken() {
    return currentToken;
  }

  /**
   * Insert a synthetic token of the specified type before the current token
   * 
   * @param type the type of token to be inserted (not {@code null})
   * @return the synthetic token that was inserted (not {@code null})
   */
  private Token insertSyntheticToken(TokenType type) {
    Token token = new Token(type, currentToken.getOffset(), "");
    currentToken.getPrevious().setNext(token);
    token.setNext(currentToken);
    return token;
  }

  /**
   * Parse the token stream for an attribute. This method advances the current token over the
   * attribute, but should not be called if the {@link #currentToken} is not {@link TokenType#TAG}.
   * 
   * @return the attribute (not {@code null})
   */
  private XmlAttributeNode parseAttribute() {

    // Assume the current token is a tag
    Token name = currentToken;
    currentToken = currentToken.getNext();

    // Equals sign
    Token equals;
    if (currentToken.getType() == TokenType.EQ) {
      equals = currentToken;
      currentToken = currentToken.getNext();
    } else {
      reportUnexpectedToken();
      equals = insertSyntheticToken(EQ);
    }

    // String value
    Token value;
    if (currentToken.getType() == TokenType.STRING) {
      value = currentToken;
      currentToken = currentToken.getNext();
    } else {
      reportUnexpectedToken();
      value = insertSyntheticToken(STRING);
    }

    return createAttributeNode(name, equals, value);
  }

  /**
   * Parse the stream for a sequence of attributes. This method advances the current token to the
   * next {@link TokenType#GT}, {@link TokenType#SLASH_GT}, or {@link TokenType#EOF}.
   * 
   * @return a collection of zero or more attributes (not {@code null}, contains no {@code null}s)
   */
  private List<XmlAttributeNode> parseAttributes() {
    TokenType type = currentToken.getType();
    if (type == GT || type == SLASH_GT || type == EOF) {
      return XmlTagNode.NO_ATTRIBUTES;
    }
    ArrayList<XmlAttributeNode> attributes = new ArrayList<XmlAttributeNode>();
    while (type != GT && type != SLASH_GT && type != EOF) {
      if (type == TAG) {
        attributes.add(parseAttribute());
      } else {
        reportUnexpectedToken();
        currentToken = currentToken.getNext();
      }
      type = currentToken.getType();
    }
    return attributes;
  }

  /**
   * Parse the stream for a sequence of tag nodes existing within a parent tag node. This method
   * advances the current token to the next {@link TokenType#LT_SLASH} or {@link TokenType#EOF}.
   * 
   * @return a list of nodes (not {@code null}, contains no {@code null}s)
   */
  private List<XmlTagNode> parseChildTagNodes() {
    TokenType type = currentToken.getType();
    if (type == LT_SLASH || type == EOF) {
      return XmlTagNode.NO_TAG_NODES;
    }
    ArrayList<XmlTagNode> nodes = new ArrayList<XmlTagNode>();
    while (type != LT_SLASH && type != EOF) {
      if (type == LT) {
        nodes.add(parseTagNode());
      } else if (type == COMMENT) {
        // ignored token
        currentToken = currentToken.getNext();
      } else {
        reportUnexpectedToken();
        currentToken = currentToken.getNext();
      }
      type = currentToken.getType();
    }
    return nodes;
  }

  /**
   * Parse the token stream for the next tag node. This method advances current token over the
   * parsed tag node, but should only be called if the current token is {@link TokenType#LT}
   * 
   * @return the tag node or {@code null} if none found
   */
  private XmlTagNode parseTagNode() {

    // Assume that the current node is a tag node start TokenType#LT
    Token nodeStart = currentToken;
    currentToken = currentToken.getNext();

    // Get the tag or create a synthetic tag and report an error
    Token tag;
    if (currentToken.getType() == TokenType.TAG) {
      tag = currentToken;
      currentToken = currentToken.getNext();
    } else {
      reportUnexpectedToken();
      tag = insertSyntheticToken(TAG);
    }

    // Parse the attributes
    List<XmlAttributeNode> attributes = parseAttributes();

    // Token ending attribute list
    Token attributeEnd;
    if (currentToken.getType() == GT || currentToken.getType() == SLASH_GT) {
      attributeEnd = currentToken;
      currentToken = currentToken.getNext();
    } else {
      reportUnexpectedToken();
      attributeEnd = insertSyntheticToken(SLASH_GT);
    }

    // If the node has no children, then return the node
    if (attributeEnd.getType() == SLASH_GT || isSelfClosing(tag)) {
      return createTagNode(
          nodeStart,
          tag,
          attributes,
          attributeEnd,
          XmlTagNode.NO_TAG_NODES,
          currentToken,
          null,
          attributeEnd);
    }

    // Parse the child tag nodes
    List<XmlTagNode> tagNodes = parseChildTagNodes();

    // Token ending child tag nodes
    Token contentEnd;
    if (currentToken.getType() == LT_SLASH) {
      contentEnd = currentToken;
      currentToken = currentToken.getNext();
    } else {
      // TODO (danrubel): handle self closing HTML elements by inserting synthetic tokens 
      // but not reporting an error
      reportUnexpectedToken();
      contentEnd = insertSyntheticToken(LT_SLASH);
    }

    // Closing tag
    Token closingTag;
    if (currentToken.getType() == TAG) {
      closingTag = currentToken;
      currentToken = currentToken.getNext();
    } else {
      reportUnexpectedToken();
      closingTag = insertSyntheticToken(TAG);
    }

    // Token ending node
    Token nodeEnd;
    if (currentToken.getType() == GT) {
      nodeEnd = currentToken;
      currentToken = currentToken.getNext();
    } else {
      reportUnexpectedToken();
      nodeEnd = insertSyntheticToken(GT);
    }

    return createTagNode(
        nodeStart,
        tag,
        attributes,
        attributeEnd,
        tagNodes,
        contentEnd,
        closingTag,
        nodeEnd);
  }

  /**
   * Report the current token as unexpected
   */
  private void reportUnexpectedToken() {
    // TODO (danrubel): report unexpected token
  }
}
