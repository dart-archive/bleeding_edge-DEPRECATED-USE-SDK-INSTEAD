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
package com.google.dart.tools.ui.web.css.model;

import com.google.dart.tools.ui.web.utils.Token;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

// selector [selector2] [:pseudo-class] {
// property: value;
// [property2: value2;
// ...]
// }
// /* comment */

// A document contains multiple sections.
// A section contains 0 or more selectors, and 0 or more properties.
// a property is a key = value.

/**
 * A CSS content parser.
 */
public class CssParser {
  public static CssDocument createEmpty() {
    return new CssDocument();
  }

  private IDocument document;
  private Tokenizer tokenizer;

  public CssParser(IDocument document) {
    this.document = document;
  }

  public CssDocument parse() {
    tokenizer = new Tokenizer(document);

    CssDocument cssDocument = new CssDocument();

    while (tokenizer.hasNext()) {
      readSection(cssDocument);
    }

    return cssDocument;
  }

  protected void readSection(CssDocument cssDocument) {
    CssSection section = new CssSection();

    cssDocument.addSection(section);

    readSelectors(section);

    readBody(section);
  }

  void readBody(CssSection section) {
    CssBody body = new CssBody();
    section.setBody(body);

    if (tokenizer.peek("{")) {
      Token token = tokenizer.next();

      body.setStart(token);

      readProperties(body);

      if (tokenizer.peek("}")) {
        body.setEnd(tokenizer.next());
      }
    }
  }

  void readSelectors(CssSection section) {
    while (readSelector(section)) {
      if (tokenizer.peek(",")) {
        tokenizer.next();
      }
    }
  }

  private Token combine(Token startToken, Token endToken) {
    if (startToken == null) {
      return null;
    }

    if (startToken == endToken) {
      return startToken;
    }

    try {
      int start = startToken.getLocation();
      int end = endToken.getLocation() + endToken.getLength();

      return new Token(document.get(start, end - start), start);
    } catch (BadLocationException e) {
      throw new RuntimeException(e);
    }
  }

  private void readProperties(CssBody body) {
    while (tokenizer.hasNext() && !tokenizer.peek("}")) {
      readProperty(body);
    }
  }

  private void readProperty(CssBody body) {
    CssProperty property = new CssProperty();

    body.addProperty(property);

    Token token = tokenizer.next();

    property.setKey(token);

    if (tokenizer.peek(":")) {
      tokenizer.next();

      Token valueToken = readPropertyValueToken();

      if (valueToken != null) {
        property.setValue(valueToken);
      }
    }

    if (tokenizer.peek(";")) {
      tokenizer.next();
    }
  }

  private Token readPropertyValueToken() {
    Token startToken = null;
    Token endToken = null;

    while (tokenizer.hasNext() && !tokenizer.peek(";") && !tokenizer.peek("}")) {
      endToken = tokenizer.next();

      if (startToken == null) {
        startToken = endToken;
      }
    }

    return combine(startToken, endToken);
  }

  private boolean readSelector(CssSection section) {
    Token startToken = null;
    Token endToken = null;

    while (tokenizer.hasNext() && !tokenizer.peek(",") && !tokenizer.peek("{")) {
      endToken = tokenizer.next();

      if (startToken == null) {
        startToken = endToken;
      }
    }

    if (startToken != null) {
      section.addSelector(combine(startToken, endToken));

      return true;
    } else {
      return false;
    }
  }

}
