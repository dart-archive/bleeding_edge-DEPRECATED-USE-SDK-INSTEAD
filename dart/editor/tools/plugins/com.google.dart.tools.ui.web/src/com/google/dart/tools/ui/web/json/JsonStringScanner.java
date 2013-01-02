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

package com.google.dart.tools.ui.web.json;

import com.google.dart.tools.ui.web.DartWebPlugin;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import java.util.ArrayList;
import java.util.List;

class JsonStringScanner extends RuleBasedScanner {

  private static class JsonStringWordDetector implements IWordDetector {
    @Override
    public boolean isWordPart(char c) {
      return true;
      //return c != '"' && c != '\'';
    }

    @Override
    public boolean isWordStart(char c) {
      return isWordPart(c);
    }
  }

  public JsonStringScanner(JsonEditor editor) {
    Token stringToken = new Token(new TextAttribute(Display.getDefault().getSystemColor(
        SWT.COLOR_BLUE)));
    Token keywordToken = new Token(new TextAttribute(DartWebPlugin.getPlugin().getEditorColor(
        DartWebPlugin.COLOR_KEYWORD), null, SWT.BOLD));

    List<IRule> rules = new ArrayList<IRule>();

    if (editor.isManifestEditor()) {
      WordRule keywordRule = new WordRule(new JsonStringWordDetector());

      for (String keyword : ManifestKeywords.getKeywords()) {
        keywordRule.addWord("\"" + keyword + "\"", keywordToken);
        keywordRule.addWord("'" + keyword + "'", keywordToken);
      }

      rules.add(keywordRule);
    }

    setRules(rules.toArray(new IRule[rules.size()]));

    setDefaultReturnToken(stringToken);
  }

}
