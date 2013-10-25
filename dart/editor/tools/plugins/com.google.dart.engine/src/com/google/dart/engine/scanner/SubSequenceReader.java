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
 * Instances of the class {@code SubSequenceReader} implement a {@link CharacterReader} that reads
 * characters from a character sequence, but adds a delta when reporting the current character
 * offset so that the character sequence can be a subsequence from a larger sequence.
 */
public class SubSequenceReader extends CharSequenceReader {
  /**
   * The offset from the beginning of the file to the beginning of the source being scanned.
   */
  private int offsetDelta;

  /**
   * Initialize a newly created reader to read the characters in the given sequence.
   * 
   * @param sequence the sequence from which characters will be read
   * @param offsetDelta the offset from the beginning of the file to the beginning of the source
   *          being scanned
   */
  public SubSequenceReader(CharSequence sequence, int offsetDelta) {
    super(sequence);
    this.offsetDelta = offsetDelta;
  }

  @Override
  public int getOffset() {
    return offsetDelta + super.getOffset();
  }

  @Override
  public String getString(int start, int endDelta) {
    return super.getString(start - offsetDelta, endDelta);
  }

  @Override
  public void setOffset(int offset) {
    super.setOffset(offset - offsetDelta);
  }
}
