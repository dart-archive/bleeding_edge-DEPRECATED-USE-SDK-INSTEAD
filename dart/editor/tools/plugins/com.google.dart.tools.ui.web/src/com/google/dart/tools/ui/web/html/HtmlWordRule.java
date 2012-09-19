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

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.Token;

import java.util.HashMap;
import java.util.Map;

/**
 * Similar to the class WordRule, but a word must start with a non-word character.
 */
public class HtmlWordRule implements IRule {
  /** Internal setting for the un-initialized column constraint. */
  protected static final int UNDEFINED = -1;

  /** The word detector used by this rule. */
  protected IWordDetector fDetector;

  /** The default token to be returned on success and if nothing else has been specified. */
  protected IToken fDefaultToken;

  /** The table of predefined words and token for this rule. */
  protected Map<String, IToken> fWords = new HashMap<String, IToken>();

  /** Buffer used for pattern detection. */

  private StringBuffer fBuffer = new StringBuffer();

  /** Tells whether this rule is case sensitive. */
  private boolean fIgnoreCase = false;

  /**
   * Creates a rule which, with the help of an word detector, will return the token associated with
   * the detected word. If no token has been associated, the scanner will be rolled back and an
   * undefined token will be returned in order to allow any subsequent rules to analyze the
   * characters.
   * 
   * @param detector the word detector to be used by this rule, may not be <code>null</code>
   * @see #addWord(String, IToken)
   */
  public HtmlWordRule(IWordDetector detector) {
    this(detector, Token.UNDEFINED, false);
  }

  public HtmlWordRule(IWordDetector detector, boolean ignoreCase) {
    this(detector, Token.UNDEFINED, ignoreCase);
  }

  /**
   * Creates a rule which, with the help of a word detector, will return the token associated with
   * the detected word. If no token has been associated, the specified default token will be
   * returned.
   * 
   * @param detector the word detector to be used by this rule, may not be <code>null</code>
   * @param defaultToken the default token to be returned on success if nothing else is specified,
   *          may not be <code>null</code>
   * @see #addWord(String, IToken)
   */
  public HtmlWordRule(IWordDetector detector, IToken defaultToken) {
    this(detector, defaultToken, false);
  }

  /**
   * Creates a rule which, with the help of a word detector, will return the token associated with
   * the detected word. If no token has been associated, the specified default token will be
   * returned.
   * 
   * @param detector the word detector to be used by this rule, may not be <code>null</code>
   * @param defaultToken the default token to be returned on success if nothing else is specified,
   *          may not be <code>null</code>
   * @param ignoreCase the case sensitivity associated with this rule
   * @see #addWord(String, IToken)
   */
  public HtmlWordRule(IWordDetector detector, IToken defaultToken, boolean ignoreCase) {
    Assert.isNotNull(detector);
    Assert.isNotNull(defaultToken);

    fDetector = detector;
    fDefaultToken = defaultToken;
    fIgnoreCase = ignoreCase;
  }

  /**
   * Adds a word and the token to be returned if it is detected.
   * 
   * @param word the word this rule will search for, may not be <code>null</code>
   * @param token the token to be returned if the word has been found, may not be <code>null</code>
   */
  public void addWord(String word, IToken token) {
    Assert.isNotNull(word);
    Assert.isNotNull(token);

    // If case-insensitive, convert to lower case before adding to the map
    if (fIgnoreCase) {
      word = word.toLowerCase();
    }
    fWords.put(word, token);
  }

  /*
   * @see IRule#evaluate(ICharacterScanner)
   */
  @Override
  public IToken evaluate(ICharacterScanner scanner) {
    int c = scanner.read();

    if (c != ICharacterScanner.EOF && fDetector.isWordStart((char) c)) {
      fBuffer.setLength(0);
      do {
        fBuffer.append((char) c);
        c = scanner.read();
      } while (c != ICharacterScanner.EOF && fDetector.isWordPart((char) c));

      scanner.unread();

      String buffer = fBuffer.toString();
      // If case-insensitive, convert to lower case before accessing the map
      if (fIgnoreCase) {
        buffer = buffer.toLowerCase();
      }

      IToken token = fWords.get(buffer);

      if (token != null) {
        return token;
      }

      return fDefaultToken;
    }

    scanner.unread();

    return Token.UNDEFINED;
  }

  /**
   * Returns the characters in the buffer to the scanner.
   * 
   * @param scanner the scanner to be used
   */
  protected void unreadBuffer(ICharacterScanner scanner) {
    for (int i = fBuffer.length() - 1; i >= 0; i--) {
      scanner.unread();
    }
  }

}
