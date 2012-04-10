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

import com.google.dart.tools.ui.web.css.Tokenizer;
import com.google.dart.tools.ui.web.utils.Token;

import org.eclipse.jface.text.IDocument;

// selector [, selector2, ...] [:pseudo-class] {
// property: value;
// [property2: value2;
// ...]
// }
// /* comment */

// A document contains multiple sections.
// A section contains 0 or more selectors, and a body
// a body contains 0 or more properties.
// a property is a key = value.

// section, selectors, properties

// TODO(devoncarew): finish css parser

/**
 * A CSS content parser.
 */
public class CssParser {
  public static CssDocument createEmpty() {
    return new CssDocument();
  }

  private IDocument document;

  public CssParser(IDocument document) {
    this.document = document;
  }

  public CssDocument parse() {
    Tokenizer tokenizer = new Tokenizer(document, new String[] {"/*", "*/"});

    CssDocument cssDocument = new CssDocument();

    while (tokenizer.hasNext()) {
      readSection(tokenizer, cssDocument);
    }

    return cssDocument;
  }

  private void readBody(Tokenizer tokenizer, CssSection section) {
    CssBody body = new CssBody();

    if (readOpenBlock(tokenizer, body)) {
      section.setBody(body);

      readProperties(tokenizer, body);

      readCloseBlock(tokenizer, body);
    }
  }

  private Token readChar(Tokenizer tokenizer, char c) {
    if (!tokenizer.hasNext()) {
      return null;
    }

    Token t = tokenizer.next();

    if (t.getValue().equals(Character.toString(c))) {
      return t;
    } else {
      tokenizer.pushBack(t);
    }

    return null;
  }

  private void readCloseBlock(Tokenizer tokenizer, CssBody body) {
    Token t = readChar(tokenizer, '}');

    if (t != null) {
      body.setEnd(t);
    }
  }

  private boolean readComma(Tokenizer tokenizer) {
    return readChar(tokenizer, ',') != null;
  }

  private boolean readOpenBlock(Tokenizer tokenizer, CssBody body) {
    Token t = readChar(tokenizer, '{');

    if (t != null) {
      body.setStart(t);
    }

    return t != null;
  }

  private void readProperties(Tokenizer tokenizer, CssBody body) {
    while (readProperty(tokenizer, body)) {

    }
  }

  private boolean readProperty(Tokenizer tokenizer, CssBody body) {
    // TODO(devoncarew):

    return false;
  }

  private void readSection(Tokenizer tokenizer, CssDocument cssDocument) {
    CssSection section = new CssSection();

    cssDocument.addChild(section);

    readSelectors(tokenizer, section);

    readBody(tokenizer, section);
  }

  private boolean readSelector(Tokenizer tokenizer, CssSection section) {
    // TODO(devoncarew):

    return false;
  }

  private void readSelectors(Tokenizer tokenizer, CssSection section) {
    if (readSelector(tokenizer, section)) {
      while (true) {
        if (readComma(tokenizer)) {
          if (!readSelector(tokenizer, section)) {
            return;
          }
        } else {
          return;
        }
      }
    }
  }
}
