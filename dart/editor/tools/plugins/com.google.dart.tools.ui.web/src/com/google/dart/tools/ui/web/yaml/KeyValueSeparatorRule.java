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
package com.google.dart.tools.ui.web.yaml;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;

/**
 * A rule for finding the key value seperator in yaml, colon + space
 */
public class KeyValueSeparatorRule implements IRule {

  IToken defaultToken;
  private char[][] originalDelimiters;

  public KeyValueSeparatorRule(IToken defaultToken) {
    this.defaultToken = defaultToken;
  }

  @Override
  public IToken evaluate(ICharacterScanner scanner) {

    originalDelimiters = scanner.getLegalLineDelimiters();

    int c = scanner.read();

    if (c == ':') {
      c = scanner.read();
      if (c == ' ' || isEndOfLine(c)) {
        scanner.unread();
        return defaultToken;
      }

    }

    scanner.unread();

    return org.eclipse.jface.text.rules.Token.UNDEFINED;

  }

  private boolean isEndOfLine(int c) {
    for (int i = 0; i < originalDelimiters.length; i++) {
      if (c == originalDelimiters[i][0]) {
        return true;
      }
    }
    return false;
  }

}
