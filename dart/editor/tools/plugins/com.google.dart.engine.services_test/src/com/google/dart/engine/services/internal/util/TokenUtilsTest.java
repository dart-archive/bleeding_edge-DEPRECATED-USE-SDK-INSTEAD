/*
 * Copyright (c) 2013, the Dart project authors.
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

package com.google.dart.engine.services.internal.util;

import com.google.common.collect.ImmutableList;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;

import junit.framework.TestCase;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

public class TokenUtilsTest extends TestCase {
  public void test_findKeywordToken() throws Exception {
    Token tokenA = new Token(TokenType.AMPERSAND, 0);
    Token tokenB = new KeywordToken(Keyword.ABSTRACT, 0);
    Token tokenC = new KeywordToken(Keyword.BREAK, 0);
    Token tokenD = new Token(TokenType.AMPERSAND, 0);
    List<Token> tokens = ImmutableList.of(tokenA, tokenB, tokenC, tokenD);
    assertSame(null, TokenUtils.findKeywordToken(tokens, Keyword.DO));
    assertSame(tokenB, TokenUtils.findKeywordToken(tokens, Keyword.ABSTRACT));
    assertSame(tokenC, TokenUtils.findKeywordToken(tokens, Keyword.BREAK));
  }

  public void test_findToken() throws Exception {
    Token tokenA = new Token(TokenType.AMPERSAND, 0);
    Token tokenB = new KeywordToken(Keyword.ABSTRACT, 0);
    List<Token> tokens = ImmutableList.of(tokenA, tokenB);
    assertSame(null, TokenUtils.findToken(tokens, TokenType.BANG));
    assertSame(tokenA, TokenUtils.findToken(tokens, TokenType.AMPERSAND));
    assertSame(tokenB, TokenUtils.findToken(tokens, TokenType.KEYWORD));
  }

  public void test_getTokens() throws Exception {
    List<Token> tokens = TokenUtils.getTokens("& | ^");
    assertThat(tokens).hasSize(3);
    assertSame(TokenType.AMPERSAND, tokens.get(0).getType());
    assertSame(TokenType.BAR, tokens.get(1).getType());
    assertSame(TokenType.CARET, tokens.get(2).getType());
  }

  public void test_hasOnly() throws Exception {
    Token tokenA = new Token(TokenType.AMPERSAND, 0);
    Token tokenB = new Token(TokenType.BANG, 0);
    assertFalse(TokenUtils.hasOnly(ImmutableList.of(tokenA, tokenB), TokenType.AMPERSAND));
    assertFalse(TokenUtils.hasOnly(ImmutableList.of(tokenA, tokenA), TokenType.AMPERSAND));
    assertFalse(TokenUtils.hasOnly(ImmutableList.of(tokenA), TokenType.BANG));
    assertTrue(TokenUtils.hasOnly(ImmutableList.of(tokenA), TokenType.AMPERSAND));
    assertTrue(TokenUtils.hasOnly(ImmutableList.of(tokenB), TokenType.BANG));
  }
}
