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
package com.google.dart.engine.html.ast;

import com.google.dart.engine.html.scanner.Token;

import java.util.List;

/**
 * Instances of the class {@code TagWithEmbeddedExpressions} represent a tag whose text content
 * contains one or more embedded expressions.
 */
public class TagWithEmbeddedExpressions extends XmlTagNode {
  /**
   * The expressions that are embedded in the tag's content.
   */
  private EmbeddedExpression[] expressions;

  /**
   * Initialize a newly created tag whose text content contains one or more embedded expressions.
   * 
   * @param nodeStart the token marking the beginning of the tag
   * @param tag the name of the tag
   * @param attributes the attributes in the tag
   * @param attributeEnd the token terminating the region where attributes can be
   * @param tagNodes the children of the tag
   * @param contentEnd the token that starts the closing tag
   * @param closingTag the name of the tag that occurs in the closing tag
   * @param nodeEnd the last token in the tag
   * @param expressions the expressions that are embedded in the value
   */
  public TagWithEmbeddedExpressions(Token nodeStart, Token tag, List<XmlAttributeNode> attributes,
      Token attributeEnd, List<XmlTagNode> tagNodes, Token contentEnd, Token closingTag,
      Token nodeEnd, EmbeddedExpression[] expressions) {
    super(nodeStart, tag, attributes, attributeEnd, tagNodes, contentEnd, closingTag, nodeEnd);
    this.expressions = expressions;
  }

  @Override
  public EmbeddedExpression[] getExpressions() {
    return expressions;
  }
}
