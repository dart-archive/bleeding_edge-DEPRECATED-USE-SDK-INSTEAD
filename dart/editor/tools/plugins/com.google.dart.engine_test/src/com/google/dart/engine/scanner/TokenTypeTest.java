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
package com.google.dart.engine.scanner;

import com.google.dart.engine.EngineTestCase;

public class TokenTypeTest extends EngineTestCase {
  public void test_isOperator() {
    assertTrue(TokenType.AMPERSAND.isOperator());
    assertTrue(TokenType.AMPERSAND_AMPERSAND.isOperator());
    assertTrue(TokenType.AMPERSAND_EQ.isOperator());
    assertTrue(TokenType.BANG.isOperator());
    assertTrue(TokenType.BANG_EQ.isOperator());
    assertTrue(TokenType.BANG_EQ_EQ.isOperator());
    assertTrue(TokenType.BAR.isOperator());
    assertTrue(TokenType.BAR_BAR.isOperator());
    assertTrue(TokenType.BAR_EQ.isOperator());
    assertTrue(TokenType.CARET.isOperator());
    assertTrue(TokenType.CARET_EQ.isOperator());
    assertTrue(TokenType.EQ.isOperator());
    assertTrue(TokenType.EQ_EQ.isOperator());
    assertTrue(TokenType.EQ_EQ_EQ.isOperator());
    assertTrue(TokenType.GT.isOperator());
    assertTrue(TokenType.GT_EQ.isOperator());
    assertTrue(TokenType.GT_GT.isOperator());
    assertTrue(TokenType.GT_GT_EQ.isOperator());
    assertTrue(TokenType.GT_GT_GT.isOperator());
    assertTrue(TokenType.GT_GT_GT_EQ.isOperator());
    assertTrue(TokenType.INDEX.isOperator());
    assertTrue(TokenType.INDEX_EQ.isOperator());
    assertTrue(TokenType.IS.isOperator());
    assertTrue(TokenType.LT.isOperator());
    assertTrue(TokenType.LT_EQ.isOperator());
    assertTrue(TokenType.LT_LT.isOperator());
    assertTrue(TokenType.LT_LT_EQ.isOperator());
    assertTrue(TokenType.MINUS.isOperator());
    assertTrue(TokenType.MINUS_EQ.isOperator());
    assertTrue(TokenType.MINUS_MINUS.isOperator());
    assertTrue(TokenType.PERCENT.isOperator());
    assertTrue(TokenType.PERCENT_EQ.isOperator());
    assertTrue(TokenType.PERIOD_PERIOD.isOperator());
    assertTrue(TokenType.PLUS.isOperator());
    assertTrue(TokenType.PLUS_EQ.isOperator());
    assertTrue(TokenType.PLUS_PLUS.isOperator());
    assertTrue(TokenType.QUESTION.isOperator());
    assertTrue(TokenType.SLASH.isOperator());
    assertTrue(TokenType.SLASH_EQ.isOperator());
    assertTrue(TokenType.STAR.isOperator());
    assertTrue(TokenType.STAR_EQ.isOperator());
    assertTrue(TokenType.TILDE.isOperator());
    assertTrue(TokenType.TILDE_SLASH.isOperator());
    assertTrue(TokenType.TILDE_SLASH_EQ.isOperator());
  }

  public void test_isUserDefinableOperator() {
    assertTrue(TokenType.AMPERSAND.isUserDefinableOperator());
    assertTrue(TokenType.BAR.isUserDefinableOperator());
    assertTrue(TokenType.CARET.isUserDefinableOperator());
    assertTrue(TokenType.EQ_EQ.isUserDefinableOperator());
    assertTrue(TokenType.GT.isUserDefinableOperator());
    assertTrue(TokenType.GT_EQ.isUserDefinableOperator());
    assertTrue(TokenType.GT_GT.isUserDefinableOperator());
    assertTrue(TokenType.INDEX.isUserDefinableOperator());
    assertTrue(TokenType.INDEX_EQ.isUserDefinableOperator());
    assertTrue(TokenType.LT.isUserDefinableOperator());
    assertTrue(TokenType.LT_EQ.isUserDefinableOperator());
    assertTrue(TokenType.LT_LT.isUserDefinableOperator());
    assertTrue(TokenType.MINUS.isUserDefinableOperator());
    assertTrue(TokenType.PERCENT.isUserDefinableOperator());
    assertTrue(TokenType.PLUS.isUserDefinableOperator());
    assertTrue(TokenType.SLASH.isUserDefinableOperator());
    assertTrue(TokenType.STAR.isUserDefinableOperator());
    assertTrue(TokenType.TILDE.isUserDefinableOperator());
    assertTrue(TokenType.TILDE_SLASH.isUserDefinableOperator());
  }
}
