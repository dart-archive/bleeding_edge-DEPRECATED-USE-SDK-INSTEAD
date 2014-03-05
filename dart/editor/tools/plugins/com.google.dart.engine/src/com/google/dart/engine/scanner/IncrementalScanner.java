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

import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.collection.TokenMap;

/**
 * Instances of the class {@code IncrementalScanner} implement a scanner that scans a subset of a
 * string and inserts the resulting tokens into the middle of an existing token stream.
 * 
 * @coverage dart.engine.parser
 */
public class IncrementalScanner extends Scanner {
  /**
   * The reader used to access the characters in the source.
   */
  private CharacterReader reader;

  /**
   * A map from tokens that were copied to the copies of the tokens.
   */
  private TokenMap tokenMap = new TokenMap();

  /**
   * The token in the new token stream immediately to the left of the range of tokens that were
   * inserted, or the token immediately to the left of the modified region if there were no new
   * tokens.
   */
  private Token leftToken;

  /**
   * The token in the new token stream immediately to the right of the range of tokens that were
   * inserted, or the token immediately to the right of the modified region if there were no new
   * tokens.
   */
  private Token rightToken;

  /**
   * A flag indicating whether there were any tokens changed as a result of the modification.
   */
  private boolean hasNonWhitespaceChange = false;

  /**
   * Initialize a newly created scanner.
   * 
   * @param source the source being scanned
   * @param reader the character reader used to read the characters in the source
   * @param errorListener the error listener that will be informed of any errors that are found
   */
  public IncrementalScanner(Source source, CharacterReader reader,
      AnalysisErrorListener errorListener) {
    super(source, reader, errorListener);
    this.reader = reader;
  }

  /**
   * Return the token in the new token stream immediately to the left of the range of tokens that
   * were inserted, or the token immediately to the left of the modified region if there were no new
   * tokens.
   * 
   * @return the token to the left of the inserted tokens
   */
  public Token getLeftToken() {
    return leftToken;
  }

  /**
   * Return the token in the new token stream immediately to the right of the range of tokens that
   * were inserted, or the token immediately to the right of the modified region if there were no
   * new tokens.
   * 
   * @return the token to the right of the inserted tokens
   */
  public Token getRightToken() {
    return rightToken;
  }

  /**
   * Return a map from tokens that were copied to the copies of the tokens.
   * 
   * @return a map from tokens that were copied to the copies of the tokens
   */
  public TokenMap getTokenMap() {
    return tokenMap;
  }

  /**
   * Return {@code true} if there were any tokens either added or removed (or both) as a result of
   * the modification.
   * 
   * @return {@code true} if there were any tokens changed as a result of the modification
   */
  public boolean hasNonWhitespaceChange() {
    return hasNonWhitespaceChange;
  }

  /**
   * Given the stream of tokens scanned from the original source, the modified source (the result of
   * replacing one contiguous range of characters with another string of characters), and a
   * specification of the modification that was made, return a stream of tokens scanned from the
   * modified source. The original stream of tokens will not be modified.
   * 
   * @param originalStream the stream of tokens scanned from the original source
   * @param index the index of the first character in both the original and modified source that was
   *          affected by the modification
   * @param removedLength the number of characters removed from the original source
   * @param insertedLength the number of characters added to the modified source
   */
  public Token rescan(Token originalStream, int index, int removedLength, int insertedLength) {
    //
    // Copy all of the tokens in the originalStream whose end is less than the replacement start.
    // (If the replacement start is equal to the end of an existing token, then it means that the
    // existing token might have been modified, so we need to rescan it.)
    //
    while (originalStream.getType() != TokenType.EOF && originalStream.getEnd() < index) {
      originalStream = copyAndAdvance(originalStream, 0);
    }
    Token oldFirst = originalStream;
    Token oldLeftToken = originalStream.getPrevious();
    leftToken = getTail();
    //
    // Skip tokens in the original stream until we find a token whose offset is greater than the end
    // of the removed region. (If the end of the removed region is equal to the beginning of an
    // existing token, then it means that the existing token might have been modified, so we need to
    // rescan it.)
    //
    int removedEnd = index + (removedLength == 0 ? 0 : removedLength - 1);
    while (originalStream.getType() != TokenType.EOF && originalStream.getOffset() <= removedEnd) {
      originalStream = originalStream.getNext();
    }
    Token oldLast;
    Token oldRightToken;
    if (originalStream.getType() != TokenType.EOF && removedEnd + 1 == originalStream.getOffset()) {
      oldLast = originalStream;
      originalStream = originalStream.getNext();
      oldRightToken = originalStream;
    } else {
      oldLast = originalStream.getPrevious();
      oldRightToken = originalStream;
    }
    //
    // Compute the delta between the character index of characters after the modified region in the
    // original source and the index of the corresponding character in the modified source.
    //
    int delta = insertedLength - removedLength;
    //
    // Compute the range of characters that are known to need to be rescanned. If the index is
    // within an existing token, then we need to start at the beginning of the token.
    //
    int scanStart = Math.min(oldFirst.getOffset(), index);
    int oldEnd = oldLast.getEnd() + delta - 1;
    int newEnd = index + insertedLength - 1;
    int scanEnd = Math.max(newEnd, oldEnd);
    //
    // Starting at the start of the scan region, scan tokens from the modifiedSource until the end
    // of the just scanned token is greater than or equal to end of the scan region in the modified
    // source. Include trailing characters of any token that was split as a result of inserted text,
    // as in "ab" --> "a.b".
    //
    reader.setOffset(scanStart - 1);
    int next = reader.advance();
    while (next != -1 && reader.getOffset() <= scanEnd) {
      next = bigSwitch(next);
    }
    //
    // Copy the remaining tokens in the original stream, but apply the delta to the token's offset.
    //
    if (originalStream.getType() == TokenType.EOF) {
      copyAndAdvance(originalStream, delta);
      rightToken = getTail();
      rightToken.setNextWithoutSettingPrevious(rightToken);
    } else {
      originalStream = copyAndAdvance(originalStream, delta);
      rightToken = getTail();
      while (originalStream.getType() != TokenType.EOF) {
        originalStream = copyAndAdvance(originalStream, delta);
      }
      Token eof = copyAndAdvance(originalStream, delta);
      eof.setNextWithoutSettingPrevious(eof);
    }
    //
    // If the index is immediately after an existing token and the inserted characters did not
    // change that original token, then adjust the leftToken to be the next token. For example, in
    // "a; c;" --> "a;b c;", the leftToken was ";", but this code advances it to "b" since "b" is
    // the first new token.
    //
    Token newFirst = leftToken.getNext();
    while (newFirst != rightToken && oldFirst != oldRightToken
        && newFirst.getType() != TokenType.EOF && equalTokens(oldFirst, newFirst)) {
      tokenMap.put(oldFirst, newFirst);
      oldLeftToken = oldFirst;
      oldFirst = oldFirst.getNext();
      leftToken = newFirst;
      newFirst = newFirst.getNext();
    }
    Token newLast = rightToken.getPrevious();
    while (newLast != leftToken && oldLast != oldLeftToken && newLast.getType() != TokenType.EOF
        && equalTokens(oldLast, newLast)) {
      tokenMap.put(oldLast, newLast);
      oldRightToken = oldLast;
      oldLast = oldLast.getPrevious();
      rightToken = newLast;
      newLast = newLast.getPrevious();
    }
    hasNonWhitespaceChange = leftToken.getNext() != rightToken
        || oldLeftToken.getNext() != oldRightToken;
    //
    // TODO(brianwilkerson) Begin tokens are not getting associated with the corresponding end
    //     tokens (because the end tokens have not been copied when we're copying the begin tokens).
    //     This could have implications for parsing.
    // TODO(brianwilkerson) Update the lineInfo.
    //
    return getFirstToken();
  }

  private Token copyAndAdvance(Token originalToken, int delta) {
    Token copiedToken = originalToken.copy();
    tokenMap.put(originalToken, copiedToken);
    copiedToken.applyDelta(delta);
    appendToken(copiedToken);

    Token originalComment = originalToken.getPrecedingComments();
    Token copiedComment = originalToken.getPrecedingComments();
    while (originalComment != null) {
      tokenMap.put(originalComment, copiedComment);
      originalComment = originalComment.getNext();
      copiedComment = copiedComment.getNext();
    }
    return originalToken.getNext();
  }

  /**
   * Return {@code true} if the two tokens are equal to each other. For the purposes of the
   * incremental scanner, two tokens are equal if they have the same type and lexeme.
   * 
   * @param oldToken the token from the old stream that is being compared
   * @param newToken the token from the new stream that is being compared
   * @return {@code true} if the two tokens are equal to each other
   */
  private boolean equalTokens(Token oldToken, Token newToken) {
    return oldToken.getType() == newToken.getType() && oldToken.getLength() == newToken.getLength()
        && oldToken.getLexeme().equals(newToken.getLexeme());
  }
}
