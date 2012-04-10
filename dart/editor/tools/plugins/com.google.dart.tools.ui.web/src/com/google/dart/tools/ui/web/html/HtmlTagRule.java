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
package com.google.dart.tools.ui.web.html;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;

class HtmlTagRule extends MultiLineRule {

  public HtmlTagRule(IToken token) {
    super("<", ">", token);
  }

  @Override
  protected boolean sequenceDetected(ICharacterScanner scanner, char[] sequence, boolean eofAllowed) {
    int c = scanner.read();
    if (sequence[0] == '<') {
      if (c == '?') {
        // processing instruction - abort
        scanner.unread();
        return false;
      }
      if (c == '!') {
        scanner.unread();
        // comment - abort
        return false;
      }
    } else if (sequence[0] == '>') {
      scanner.unread();
    }
    return super.sequenceDetected(scanner, sequence, eofAllowed);
  }

}
