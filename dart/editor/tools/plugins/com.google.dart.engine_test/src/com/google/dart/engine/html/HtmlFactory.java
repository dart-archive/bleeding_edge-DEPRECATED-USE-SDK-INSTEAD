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

package com.google.dart.engine.html;

import com.google.common.collect.Lists;
import com.google.dart.engine.html.ast.XmlAttributeNode;
import com.google.dart.engine.html.ast.XmlTagNode;
import com.google.dart.engine.html.scanner.Token;
import com.google.dart.engine.html.scanner.TokenType;

/**
 * Utility methods to create HTML nodes.
 */
public class HtmlFactory {

  public static XmlAttributeNode attribute(String name, String value) {
    Token nameToken = stringToken(name);
    Token equalsToken = new Token(TokenType.EQ, 0);
    Token valueToken = stringToken(value);
    return new XmlAttributeNode(nameToken, equalsToken, valueToken);
  }

  public static XmlTagNode tagNode(String name, XmlAttributeNode... attributes) {
    return new XmlTagNode(
        ltToken(),
        stringToken(name),
        Lists.<XmlAttributeNode> newArrayList(attributes),
        sgtToken(),
        null,
        null,
        null,
        null);
  }

  private static Token ltToken() {
    return new Token(TokenType.LT, 0);
  }

  private static Token sgtToken() {
    return new Token(TokenType.SLASH_GT, 0);
  }

  private static Token stringToken(String value) {
    return new Token(TokenType.STRING, 0, value);
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private HtmlFactory() {
  }
}
