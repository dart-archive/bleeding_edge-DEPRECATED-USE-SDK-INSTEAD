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

import com.google.dart.tools.ui.internal.text.dart.DartCodeScanner;
import com.google.dart.tools.ui.text.IColorManager;
import com.google.dart.tools.ui.web.DartWebPlugin;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("restriction")
class HtmlDartScanner extends DartCodeScanner {

  public HtmlDartScanner(IColorManager manager, IPreferenceStore store) {
    super(manager, store, true);
  }

  @Override
  protected List<IRule> createRules() {
    Token commentsToken = new Token(new TextAttribute(DartWebPlugin.getPlugin().getEditorColor(
        DartWebPlugin.COLOR_COMMENTS)));
    Token docCommentsToken = new Token(new TextAttribute(DartWebPlugin.getPlugin().getEditorColor(
        DartWebPlugin.COLOR_DOC_COMMENTS)));
    Token singleLineCommentToken = new Token(new TextAttribute(
        DartWebPlugin.getPlugin().getEditorColor(DartWebPlugin.COLOR_SINGLE_COMMENTS)));

    Token stringToken = new Token(new TextAttribute(DartWebPlugin.getPlugin().getEditorColor(
        DartWebPlugin.COLOR_STRING)));

    List<IRule> rules = new ArrayList<IRule>(super.createRules());

    rules.add(0, new SingleLineRule("\"", "\"", stringToken, '\\'));
    rules.add(0, new MultiLineRule("\"\"\"", "\"\"\"", stringToken, (char) 0, true));
    rules.add(0, new MultiLineRule("'''", "'''", stringToken, (char) 0, true));
    rules.add(0, new EndOfLineRule("//", singleLineCommentToken));
    rules.add(0, new MultiLineRule("/*", "*/", commentsToken, (char) 0, true));
    rules.add(0, new MultiLineRule("/**", "*/", docCommentsToken, (char) 0, true));

    return rules;
  }

}
