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

import com.google.dart.tools.core.html.HtmlKeywords;
import com.google.dart.tools.ui.web.DartWebPlugin;
import com.google.dart.tools.ui.web.utils.WhitespaceDetector;
import com.google.dart.tools.ui.web.utils.WordDetector;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.swt.SWT;

import java.util.ArrayList;
import java.util.List;

class HtmlTagScanner extends RuleBasedScanner {

  public HtmlTagScanner() {
    Token keywordToken = new Token(new TextAttribute(DartWebPlugin.getPlugin().getEditorColor(
        DartWebPlugin.COLOR_KEYWORD), null, SWT.BOLD));
    Token stringToken = new Token(new TextAttribute(DartWebPlugin.getPlugin().getEditorColor(
        DartWebPlugin.COLOR_STRING)));
    IToken procInstr = new Token(new TextAttribute(DartWebPlugin.getPlugin().getEditorColor(
        DartWebPlugin.COLOR_ALT_COMMENTS)));

    List<IRule> rules = new ArrayList<IRule>();

    rules.add(new SingleLineRule("\"", "\"", stringToken, '\\'));
    rules.add(new SingleLineRule("'", "'", stringToken, '\\'));

    HtmlWordRule keywordRule = new HtmlWordRule(new WordDetector(), true);

    for (String keyword : HtmlKeywords.getKeywords()) {
      keywordRule.addWord(keyword, keywordToken);
    }

    rules.add(keywordRule);

    rules.add(new SingleLineRule("<?", "?>", procInstr));
    rules.add(new WhitespaceRule(new WhitespaceDetector()));

    setRules(rules.toArray(new IRule[rules.size()]));
  }
}
