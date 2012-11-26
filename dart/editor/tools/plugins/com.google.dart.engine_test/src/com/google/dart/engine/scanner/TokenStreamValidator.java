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

import junit.framework.Assert;

/**
 * Instances of the class {@code TokenStreamValidator} are used to validate the correct construction
 * of a stream of tokens.
 */
public class TokenStreamValidator {
  /**
   * Validate that the stream of tokens that starts with the given token is correct.
   * 
   * @param token the first token in the stream of tokens to be validated
   */
  public void validate(Token token) {
    StringBuilder builder = new StringBuilder();
    validateStream(builder, token);
    if (builder.length() > 0) {
      Assert.fail(builder.toString());
    }
  }

  private void validateStream(StringBuilder builder, Token token) {
    if (token == null) {
      return;
    }
    Token previousToken = null;
    int previousEnd = -1;
    Token currentToken = token;
    while (currentToken != null && currentToken.getType() != TokenType.EOF) {
      validateStream(builder, currentToken.getPrecedingComments());
      TokenType type = currentToken.getType();
      if (type == TokenType.OPEN_CURLY_BRACKET || type == TokenType.OPEN_PAREN
          || type == TokenType.OPEN_SQUARE_BRACKET
          || type == TokenType.STRING_INTERPOLATION_EXPRESSION) {
        if (!(currentToken instanceof BeginToken)) {
          builder.append("\r\nExpected BeginToken, found ");
          builder.append(currentToken.getClass().getSimpleName());
          builder.append(" ");
          writeToken(builder, currentToken);
        }
      }
      int currentStart = currentToken.getOffset();
      int currentLength = currentToken.getLength();
      int currentEnd = currentStart + currentLength - 1;
      if (currentStart <= previousEnd) {
        builder.append("\r\nInvalid token sequence: ");
        writeToken(builder, previousToken);
        builder.append(" followed by ");
        writeToken(builder, currentToken);
      }
      previousEnd = currentEnd;
      previousToken = currentToken;
      currentToken = currentToken.getNext();
    }
  }

  private void writeToken(StringBuilder builder, Token token) {
    builder.append("[");
    builder.append(token.getType());
    builder.append(", '");
    builder.append(token.getLexeme());
    builder.append("', ");
    builder.append(token.getOffset());
    builder.append(", ");
    builder.append(token.getLength());
    builder.append("]");
  }
}
