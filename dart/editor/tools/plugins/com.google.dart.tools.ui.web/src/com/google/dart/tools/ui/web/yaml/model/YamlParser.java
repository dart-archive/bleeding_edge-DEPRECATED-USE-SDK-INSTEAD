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
package com.google.dart.tools.ui.web.yaml.model;

import com.google.dart.tools.ui.web.utils.Token;
import com.google.dart.tools.ui.web.yaml.Tokenizer;

import org.eclipse.jface.text.IDocument;

// --- # Lists
//- Item 1
//- Item 2
// ---- # block
// property: value;
// # comment 
//A document contains multiple sections.
//a body contains properties.

// minimal support just for pubspec.yaml
//TODO(keertip): finish the parser

/**
 * A yaml content parser
 */
public class YamlParser {

  public static YamlDocument createEmpty() {
    return new YamlDocument();
  }

  private IDocument document;

  public YamlParser(IDocument document) {
    this.document = document;
  }

  public YamlDocument parse() {
    Tokenizer tokenizer = new Tokenizer(document, new String[] {"#"});

    YamlDocument yamlDocument = new YamlDocument();

    while (tokenizer.hasNext()) {
      readSection(tokenizer, yamlDocument);
    }

    return yamlDocument;
  }

  private void readBody(Tokenizer tokenizer, YamlSection section) {
    YamlBody body = new YamlBody();
    section.setBody(body);
    readProperties(tokenizer, body);
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

  private boolean readComma(Tokenizer tokenizer) {
    return readChar(tokenizer, ',') != null;
  }

  private void readProperties(Tokenizer tokenizer, YamlBody body) {
    while (readProperty(tokenizer, body)) {

    }
  }

  private boolean readProperty(Tokenizer tokenizer, YamlBody body) {
    // TODO(keertip):

    return false;
  }

  private void readSection(Tokenizer tokenizer, YamlDocument yamlDocument) {
    YamlSection section = new YamlSection();

    yamlDocument.addChild(section);

    readSelectors(tokenizer, section);

    readBody(tokenizer, section);
  }

  private boolean readSelector(Tokenizer tokenizer, YamlSection section) {
    // TODO(keertip):

    return false;
  }

  private void readSelectors(Tokenizer tokenizer, YamlSection section) {
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
