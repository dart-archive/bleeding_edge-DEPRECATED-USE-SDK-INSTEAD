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
    // (If the replacement start is less than or equal to the end of an existing token, then it
    // means that the existing token might have been modified, so we need to rescan it.)
    //
    while (originalStream.getEnd() < index) {
      originalStream = copyAndAdvance(originalStream, 0);
    }
    //
    // Starting at the smaller of the start of the next token or the given index, scan tokens from
    // the modifiedSource until the end of the just scanned token is greater than or equal to end of
    // the modified region in the modified source.
    //
    int modifiedEnd = index + insertedLength - 1;
    reader.setOffset(Math.min(originalStream.getOffset(), index) - 1);
    int next = reader.advance();
    while (next != -1 && reader.getOffset() <= modifiedEnd) {
      next = bigSwitch(next);
    }
    //
    // Skip tokens in the original stream until we find a token whose offset is greater than the end
    // of the removed region.
    //
    int removedEnd = index + removedLength - 1;
    while (originalStream.getOffset() <= removedEnd) {
      originalStream = originalStream.getNext();
    }
    //
    // Compute the delta between the character index of characters after the modified region in the
    // original source and the index of the corresponding character in the modified source.
    //
    int delta = insertedLength - removedLength;
    //
    // Copy the remaining tokens in the original stream, but apply the delta to the token's offset.
    //
    while (originalStream.getType() != TokenType.EOF) {
      originalStream = copyAndAdvance(originalStream, delta);
    }
    copyAndAdvance(originalStream, delta);
    //
    // TODO(brianwilkerson) Update the lineInfo.
    //
    return firstToken();
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
}
