/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.core.html;

import java.util.Stack;

/**
 * An xml parser. This class converts from a stream of characters to an XmlDocument. For html
 * parsing, see the related HtmlParser class.
 * 
 * @see HtmlParser
 */
public class XmlParser {

  public static XmlDocument createEmpty() {
    return new XmlDocument();
  }

  private XmlDocument document;
  private Tokenizer tokenizer;

  private Stack<XmlElement> stack = new Stack<XmlElement>();

  public XmlParser(String data) {
    tokenizer = new Tokenizer(data);

    if (getPassThroughElements() != null) {
      tokenizer.setPassThroughElements(getPassThroughElements());
    }
  }

  public XmlDocument parse() {
    document = new XmlDocument();

    parseRoot();

    return document;
  }

  protected void endTag(Token token) {
    Token t = tokenizer.next();

    popTag(t.getValue());

    while (tokenizer.hasNext()) {
      t = tokenizer.next();

      if (">".equals(t.getValue())) {
        return;
      }
    }
  }

  protected String[] getPassThroughElements() {
    return null;
  }

  protected boolean isSelfClosing(String entityName) {
    return false;
  }

  protected void parseRoot() {
    // EOF, comment, or node start
    while (tokenizer.hasNext()) {
      Token token = tokenizer.next();

      String value = token.getValue();

      if (value.equals("<")) {
        startTag(token);
      } else if (value.equals("</")) {
        endTag(token);
      } else if (!value.startsWith("<")) {
        // add char data
        if (!stack.isEmpty()) {
          stack.peek().appendContents(value);
        }
      } else if (value.startsWith("<!--")) {
        handleComment(token);
      } else if (value.startsWith("<!")) {
        handleDeclaration(token);
      } else if (value.startsWith("<?")) {
        handleDirective(token);
      } else {
        System.out.println("unhandled token: " + token);
      }
    }
  }

  protected void startTag(Token startToken) {
    Token tagName = tokenizer.next();

    XmlElement element = new XmlElement(tagName.getValue());

    element.setStart(startToken);

    readAttributes(element);

    Token endToken = null;
    boolean autoClosed = false;

    while (tokenizer.hasNext()) {
      if (peek("<")) {
        element.setEnd(endToken);
        autoClosed = true;
        break;
      }

      endToken = tokenizer.next();

      if (endToken.getValue().equals(">")) {
        element.setEnd(endToken);
        break;
      }

      if (endToken.getValue().equals("/>")) {
        element.setEnd(endToken);
        autoClosed = true;
        break;
      }
    }

    getCurrentParent().addChild(element);

    if (!autoClosed && !isSelfClosing(tagName.getValue())) {
      pushTag(element);
    }
  }

  private XmlNode getCurrentParent() {
    if (!stack.isEmpty()) {
      return stack.peek();
    } else {
      return document;
    }
  }

  private void handleComment(Token token) {
    // Ignore comments
//    XmlNode node = new XmlNode("<!-- -->");
//    node.setStart(token);
//    node.setEnd(token);
//    node.setContents(token.getValue());
//    getCurrentParent().addChild(node);
  }

  private void handleDeclaration(Token token) {
    if (stack.isEmpty()) {
      XmlNode node = new XmlNode(token.getValue());
      node.setStart(token);
      node.setEnd(token);
      node.setContents(token.getValue());
      getCurrentParent().addChild(node);
    }
  }

  private void handleDirective(Token token) {
    if (stack.isEmpty()) {
      XmlNode node = new XmlNode(token.getValue());
      node.setStart(token);
      node.setEnd(token);
      node.setContents(token.getValue());
      getCurrentParent().addChild(node);
    }
  }

  private boolean peek(String value) {
    Token token = tokenizer.peek();

    if (token != null && value.equals(token.getValue())) {
      return true;
    } else {
      return false;
    }
  }

  private void popTag(String label) {
    if (!stack.isEmpty()) {
      XmlElement topTag = stack.peek();

      if (label.equals(topTag.getLabel())) {
        // Things are balanced; just pop the last one off.
        stack.pop();
      } else {
        // Look to see if there's a match further up.
        for (int i = stack.size() - 1; i >= 0; i--) {
          XmlElement tag = stack.get(i);

          if (label.equals(tag.getLabel())) {
            stack.setSize(i);
            return;
          }
        }
      }
    }
  }

  private void pushTag(XmlElement tag) {
    stack.push(tag);
  }

  private void readAttributes(XmlElement element) {
    while (true) {
      if (peek(">") || peek("/>") || peek("<")) {
        return;
      } else {
        // foo=foo
        Token token = tokenizer.next();

        XmlAttribute attribute = new XmlAttribute(token.getValue());
        attribute.setStart(token);

        if (peek("=")) {
          // consume the equals
          tokenizer.next();

          if (!peek(">")) {
            token = tokenizer.next();;

            attribute.setValue(stripQuotes(token.getValue()));
            attribute.setEnd(token);
          } else {
            attribute.setEnd(token);
          }
        }

        element.addAttribute(attribute);
      }
    }
  }

  private String stripQuotes(String str) {
    if (str == null || str.length() < 2) {
      return str;
    }

    if (str.startsWith("\"") && str.endsWith("\"")) {
      return str.substring(1, str.length() - 1);
    }

    if (str.startsWith("'") && str.endsWith("'")) {
      return str.substring(1, str.length() - 1);
    }

    return str;
  }

}
