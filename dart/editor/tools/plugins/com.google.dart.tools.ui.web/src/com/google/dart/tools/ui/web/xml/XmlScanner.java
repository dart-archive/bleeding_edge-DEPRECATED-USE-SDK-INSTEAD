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
package com.google.dart.tools.ui.web.xml;

import com.google.dart.tools.ui.web.DartWebPlugin;
import com.google.dart.tools.ui.web.utils.WhitespaceDetector;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;

class XmlScanner extends RuleBasedScanner {

  public XmlScanner() {
    IToken procInstr = new Token(new TextAttribute(DartWebPlugin.getPlugin().getEditorColor(
        DartWebPlugin.COLOR_ALT_COMMENTS)));

    IRule[] rules = new IRule[2];
    rules[0] = new SingleLineRule("<?", "?>", procInstr);
    rules[1] = new WhitespaceRule(new WhitespaceDetector());

    setRules(rules);
  }

}
