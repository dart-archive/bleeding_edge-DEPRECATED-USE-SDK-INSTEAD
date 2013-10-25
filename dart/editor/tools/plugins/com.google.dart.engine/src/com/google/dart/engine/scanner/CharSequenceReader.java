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
package com.google.dart.engine.scanner;

/**
 * Instances of the class {@code CharSequenceReader} implement a {@link CharacterReader} that reads
 * characters from a character sequence.
 */
public class CharSequenceReader implements CharacterReader {
  /**
   * The sequence from which characters will be read.
   */
  private final CharSequence sequence;

  /**
   * The number of characters in the string.
   */
  private final int stringLength;

  /**
   * The index, relative to the string, of the last character that was read.
   */
  private int charOffset;

  /**
   * Initialize a newly created reader to read the characters in the given sequence.
   * 
   * @param sequence the sequence from which characters will be read
   */
  public CharSequenceReader(CharSequence sequence) {
    this.sequence = sequence;
    this.stringLength = sequence.length();
    this.charOffset = -1;
  }

  @Override
  public int advance() {
    if (charOffset + 1 >= stringLength) {
      return -1;
    }
    return sequence.charAt(++charOffset);
  }

  @Override
  public int getOffset() {
    return charOffset;
  }

  @Override
  public String getString(int start, int endDelta) {
    return sequence.subSequence(start, charOffset + 1 + endDelta).toString();
  }

  @Override
  public int peek() {
    if (charOffset + 1 >= sequence.length()) {
      return -1;
    }
    return sequence.charAt(charOffset + 1);
  }

  @Override
  public void setOffset(int offset) {
    charOffset = offset;
  }
}
