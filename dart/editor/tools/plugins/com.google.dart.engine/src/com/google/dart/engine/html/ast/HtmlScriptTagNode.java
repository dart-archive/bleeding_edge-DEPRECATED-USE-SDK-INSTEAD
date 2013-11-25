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

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.element.HtmlScriptElement;
import com.google.dart.engine.html.ast.visitor.XmlVisitor;
import com.google.dart.engine.html.scanner.Token;

import java.util.List;

/**
 * Instances of the class {@code HtmlScriptTagNode} represent a script tag within an HTML file that
 * references a Dart script.
 */
public class HtmlScriptTagNode extends XmlTagNode {
  /**
   * The AST structure representing the Dart code within this tag.
   */
  private CompilationUnit script;

  /**
   * The element representing this script.
   */
  private HtmlScriptElement scriptElement;

  /**
   * Initialize a newly created node to represent a script tag within an HTML file that references a
   * Dart script.
   * 
   * @param nodeStart the token marking the beginning of the tag
   * @param tag the name of the tag
   * @param attributes the attributes in the tag
   * @param attributeEnd the token terminating the region where attributes can be
   * @param tagNodes the children of the tag
   * @param contentEnd the token that starts the closing tag
   * @param closingTag the name of the tag that occurs in the closing tag
   * @param nodeEnd the last token in the tag
   */
  public HtmlScriptTagNode(Token nodeStart, Token tag, List<XmlAttributeNode> attributes,
      Token attributeEnd, List<XmlTagNode> tagNodes, Token contentEnd, Token closingTag,
      Token nodeEnd) {
    super(nodeStart, tag, attributes, attributeEnd, tagNodes, contentEnd, closingTag, nodeEnd);
  }

  @Override
  public <R> R accept(XmlVisitor<R> visitor) {
    return visitor.visitHtmlScriptTagNode(this);
  }

  /**
   * Return the AST structure representing the Dart code within this tag, or {@code null} if this
   * tag references an external script.
   * 
   * @return the AST structure representing the Dart code within this tag
   */
  public CompilationUnit getScript() {
    return script;
  }

  /**
   * Return the element representing this script.
   * 
   * @return the element representing this script
   */
  public HtmlScriptElement getScriptElement() {
    return scriptElement;
  }

  /**
   * Set the AST structure representing the Dart code within this tag to the given compilation unit.
   * 
   * @param unit the AST structure representing the Dart code within this tag
   */
  public void setScript(CompilationUnit unit) {
    script = unit;
  }

  /**
   * Set the element representing this script to the given element.
   * 
   * @param scriptElement the element representing this script
   */
  public void setScriptElement(HtmlScriptElement scriptElement) {
    this.scriptElement = scriptElement;
  }
}
