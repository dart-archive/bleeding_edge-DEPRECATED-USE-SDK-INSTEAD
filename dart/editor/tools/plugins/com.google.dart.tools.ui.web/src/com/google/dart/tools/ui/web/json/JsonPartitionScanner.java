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

import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * A partion scanner for json files
 */
public class JsonPartitionScanner extends RuleBasedPartitionScanner {
  public final static String JSON_COMMENT = "__json_comment";
  public final static String JSON_STRING = "__json_string";

  public JsonPartitionScanner() {
    IToken commentToken = new Token(JSON_COMMENT);
    IToken stringToken = new Token(JSON_STRING);

    List<IPredicateRule> rules = new ArrayList<IPredicateRule>();

    rules.add(new EndOfLineRule("//", commentToken));
    rules.add(new SingleLineRule("'", "'", stringToken, '\\', true));
    rules.add(new SingleLineRule("\"", "\"", stringToken, '\\', true));

    setPredicateRules(rules.toArray(new IPredicateRule[rules.size()]));
  }

}
