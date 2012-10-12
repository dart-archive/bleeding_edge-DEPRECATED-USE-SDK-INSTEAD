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
package com.google.dart.tools.internal.corext.refactoring.code;

import com.google.common.collect.Lists;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.StringScanner;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableEx;

import java.util.List;

/**
 * Utilities to work with {@link Token}s.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class TokenUtils {

  /**
   * @return the first {@link KeywordToken} with given {@link Keyword}, may be <code>null</code> if
   *         not found.
   */
  public static KeywordToken findKeywordToken(List<Token> tokens, Keyword keyword) {
    for (int i = 0; i < tokens.size(); i++) {
      Token token = tokens.get(i);
      if (token instanceof KeywordToken) {
        KeywordToken keywordToken = (KeywordToken) token;
        if (keywordToken.getKeyword() == keyword) {
          return keywordToken;
        }
      }
    }
    return null;
  }

  /**
   * @return the first {@link Token} with given {@link TokenType}, may be <code>null</code> if not
   *         found.
   */
  public static Token findToken(List<Token> tokens, TokenType type) {
    for (int i = 0; i < tokens.size(); i++) {
      Token token = tokens.get(i);
      if (token.getType() == type) {
        return token;
      }
    }
    return null;
  }

  /**
   * @return {@link Token}s of the given Dart source, not <code>null</code>, may be empty if no
   *         tokens or some exception happens.
   */
  public static List<Token> getTokens(final String s) {
    final List<Token> tokens = Lists.newArrayList();
    ExecutionUtils.runIgnore(new RunnableEx() {
      @Override
      public void run() throws Exception {
        StringScanner scanner = new StringScanner(null, s, null);
        Token token = scanner.tokenize();
        while (token.getType() != TokenType.EOF) {
          tokens.add(token);
          token = token.getNext();
        }
      }
    });
    return tokens;
  }

  /**
   * @return <code>true</code> if given {@link Token}s contain only single {@link Token} with given
   *         {@link TokenType}.
   */
  public static boolean hasOnly(List<Token> tokens, TokenType type) {
    return tokens.size() == 1 && tokens.get(0).getType() == type;
  }
}
