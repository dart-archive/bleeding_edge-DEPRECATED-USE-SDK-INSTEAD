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

import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.PatternRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;

class HtmlPartitionScanner extends RuleBasedPartitionScanner {

  public HtmlPartitionScanner() {
    IToken htmlComment = new Token(HtmlEditor.HTML_COMMENT_PARTITION);
    IToken templateSection = new Token(HtmlEditor.HTML_TEMPLATE_PARTITION);
    IToken styleSection = new Token(HtmlEditor.HTML_STYLE_PARTITION);
    IToken codeSection = new Token(HtmlEditor.HTML_CODE_PARTITION);

    IPredicateRule[] rules = new IPredicateRule[4];

    rules[0] = new MultiLineRule("<!--", "-->", htmlComment);
    rules[1] = new MultiLineRule("{{", "}}", templateSection);

    // TODO(devoncarew): create a custom IPartitionTokenScanner that can correctly create
    // partitions inside the <foo>...</foo> tags, instead of including the tags themselves.
    rules[2] = new PatternRule("<style", "</style>", styleSection, Character.MIN_VALUE, false);
    rules[3] = new PatternRule("<script", "</script>", codeSection, Character.MIN_VALUE, false);

    setPredicateRules(rules);
  }

}
