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
package com.google.dart.tools.ui.web.utils;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;

public class AnyWordRule implements IRule {

  /** Internal setting for the un-initialized column constraint. */
  protected static final int UNDEFINED = -1;

  /** The word detector used by this rule. */
  protected IWordDetector fDetector;
  /** The default token to be returned on success and if nothing else has been specified. */
  protected IToken fDefaultToken;
  /** Buffer used for pattern detection. */
  private StringBuffer fBuffer = new StringBuffer();

  /**
   * Creates a rule which, with the help of an word detector, will return the token associated with
   * the detected word. If no token has been associated, the scanner will be rolled back and an
   * undefined token will be returned in order to allow any subsequent rules to analyze the
   * characters.
   * 
   * @param detector the word detector to be used by this rule, may not be <code>null</code>
   * @see #addWord(String, IToken)
   */
  public AnyWordRule(IWordDetector detector) {
    this(detector, new org.eclipse.jface.text.rules.Token("other"));
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
  public AnyWordRule(IWordDetector detector, IToken defaultToken) {
    Assert.isNotNull(detector);
    Assert.isNotNull(defaultToken);

    fDetector = detector;
    fDefaultToken = defaultToken;
  }

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

      return fDefaultToken;
    }

    scanner.unread();

    return org.eclipse.jface.text.rules.Token.UNDEFINED;
  }

}
