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

import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;

/**
 * A partion scanner for yaml files
 */
public class YamlPartitionScanner extends RuleBasedPartitionScanner {

  public final static String YAML_COMMENT = "__yaml_comment";

  public YamlPartitionScanner() {
    IToken commentToken = new Token(YAML_COMMENT);

    IPredicateRule[] rules = new IPredicateRule[1];

    rules[0] = new EndOfLineRule("#", commentToken);

    setPredicateRules(rules);
  }

}
