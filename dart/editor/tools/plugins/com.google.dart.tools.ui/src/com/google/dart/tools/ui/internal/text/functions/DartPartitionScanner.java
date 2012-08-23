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
package com.google.dart.tools.ui.internal.text.functions;

import com.google.dart.tools.ui.text.DartPartitions;

import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;

import java.util.ArrayList;
import java.util.List;

/**
 * This scanner recognizes the Dartdoc comments and Dart multi line comments.
 */
public class DartPartitionScanner extends RuleBasedPartitionScanner implements DartPartitions {

  /**
   * Detector for empty comments.
   */
  static class EmptyCommentDetector implements IWordDetector {

    /*
     * @see IWordDetector#isWordPart
     */
    @Override
    public boolean isWordPart(char c) {
      return (c == '*' || c == '/');
    }

    /*
     * @see IWordDetector#isWordStart
     */
    @Override
    public boolean isWordStart(char c) {
      return (c == '/');
    }
  }

  /**
   * Word rule for empty comments.
   */
  static class EmptyCommentRule extends WordRule implements IPredicateRule {

    private IToken fSuccessToken;

    /**
     * Constructor for EmptyCommentRule.
     * 
     * @param successToken
     */
    public EmptyCommentRule(IToken successToken) {
      super(new EmptyCommentDetector());
      fSuccessToken = successToken;
      addWord("/**/", fSuccessToken); //$NON-NLS-1$
    }

    /*
     * @see IPredicateRule#evaluate(ICharacterScanner, boolean)
     */
    @Override
    public IToken evaluate(ICharacterScanner scanner, boolean resume) {
      return evaluate(scanner);
    }

    /*
     * @see IPredicateRule#getSuccessToken()
     */
    @Override
    public IToken getSuccessToken() {
      return fSuccessToken;
    }
  }

  /**
   * Creates the partitioner and sets up the appropriate rules.
   */
  public DartPartitionScanner() {
    super();

    IToken string = new Token(DART_STRING);
    @SuppressWarnings("deprecation")
    IToken character = new Token(JAVA_CHARACTER);
    IToken javaDoc = new Token(DART_DOC);
    IToken multiLineComment = new Token(DART_MULTI_LINE_COMMENT);
    IToken singleLineComment = new Token(DART_SINGLE_LINE_COMMENT);

    List<IPredicateRule> rules = new ArrayList<IPredicateRule>();

    // Add rule for single line comments.
    rules.add(new EndOfLineRule("///", javaDoc)); //$NON-NLS-1$
    rules.add(new EndOfLineRule("//", singleLineComment)); //$NON-NLS-1$

    // Add rule for strings.
    rules.add(new SingleLineRule("\"", "\"", string, '\\')); //$NON-NLS-2$ //$NON-NLS-1$

    // Add rule for character constants.
    rules.add(new SingleLineRule("'", "'", character, '\\')); //$NON-NLS-2$ //$NON-NLS-1$

    // Add special case word rule.
    EmptyCommentRule wordRule = new EmptyCommentRule(multiLineComment);
    rules.add(wordRule);

    // Add rules for multi-line comments and javadoc.
    rules.add(new MultiLineRule("/**", "*/", javaDoc)); //$NON-NLS-1$ //$NON-NLS-2$
    rules.add(new MultiLineRule("/*", "*/", multiLineComment)); //$NON-NLS-1$ //$NON-NLS-2$

    IPredicateRule[] result = new IPredicateRule[rules.size()];
    rules.toArray(result);
    setPredicateRules(result);
  }
}
