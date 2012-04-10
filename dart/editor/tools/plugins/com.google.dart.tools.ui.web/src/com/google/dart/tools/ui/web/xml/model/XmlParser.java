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
package com.google.dart.tools.ui.web.xml.model;

import com.google.dart.tools.ui.web.css.Tokenizer;
import com.google.dart.tools.ui.web.utils.Token;

import org.eclipse.jface.text.IDocument;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

// TODO(devoncarew): finish html parser

/**
 * An xml parser. This class converts from a stream of characters to an XmlDocument.
 */
public class XmlParser {

  public static XmlDocument createEmpty() {
    return new XmlDocument();
  }

  private IDocument document;

  private boolean isHtml;
  private XmlDocument results;
  private Tokenizer tokenizer;

  private Stack<XmlNode> stack = new Stack<XmlNode>();

  // img, br, hr, and meta

  private static Set<String> SELF_CLOSING = new HashSet<String>(Arrays.asList(new String[] {
      "img", "br", "hr", "meta", "link", "!"}));

  public XmlParser(IDocument document) {
    this(document, false);
  }

  public XmlParser(IDocument document, boolean isHtml) {
    this.document = document;
    this.isHtml = isHtml;
  }

  public XmlDocument parse() {
    tokenizer = new Tokenizer(document, new String[] {"<!--", "-->"});

    results = new XmlDocument();

    parseRoot();
//    while (tokenizer.hasNext()) {
//      Token token = tokenizer.next();
//
//      if (!token.isWhitespace()) {
//        System.out.println(token);
//      }
//    }

    return results;
  }

  private void parseRoot() {
    // EOF, comment, or node start
    while (tokenizer.hasNext()) {
      Token token = tokenizer.next();

      XmlNode parent = results;

      if (!stack.isEmpty()) {
        parent = stack.peek();
      }

      if (token.getValue().equals("<")) {
        startTag(parent, token);
      } else {
        // handle comments - add to the outline view

      }
    }
  }

  private void popTag(String label) {
    if (!stack.isEmpty()) {
      stack.pop();
    }
  }

  private void pushTag(XmlNode tag) {
    stack.push(tag);
  }

  private void startTag(XmlNode parent, Token startToken) {
    if (tokenizer.hasNext()) {
      Token tagName = tokenizer.next();

      if ("/".equals(tagName.getValue())) {
        tagName = tokenizer.next();

        popTag(tagName.getValue());

        while (tokenizer.hasNext()) {
          Token t = tokenizer.next();

          if (t.getValue().equals(">")) {
            // end tag
            return;
          }
        }
      }

      XmlNode tag = new XmlNode(tagName.getValue());

      tag.setStart(startToken);

      parent.addChild(tag);

      boolean pushed = false;

      if (!isHtml || !SELF_CLOSING.contains(tag.getLabel())) {
        pushTag(tag);
        pushed = true;
      }

      Token lastToken = tagName;

      while (tokenizer.hasNext()) {
        Token t = tokenizer.next();

        if (t.getValue().equals(">")) {
          // end tag
          tag.setEnd(t);

          if (pushed && lastToken.getValue().equals("/")) {
            popTag(tagName.getValue());
          }
          return;
        } else {
          lastToken = t;
        }
      }
    }
  }

}
