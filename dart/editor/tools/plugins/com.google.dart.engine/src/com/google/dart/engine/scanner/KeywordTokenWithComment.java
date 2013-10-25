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

/**
 * Instances of the class {@code KeywordTokenWithComment} implement a keyword token that is preceded
 * by comments.
 * 
 * @coverage dart.engine.parser
 */
public class KeywordTokenWithComment extends KeywordToken {
  /**
   * The first comment in the list of comments that precede this token.
   */
  private Token precedingComment;

  /**
   * Initialize a newly created token to to represent the given keyword and to be preceded by the
   * comments reachable from the given comment.
   * 
   * @param keyword the keyword being represented by this token
   * @param offset the offset from the beginning of the file to the first character in the token
   * @param precedingComment the first comment in the list of comments that precede this token
   */
  public KeywordTokenWithComment(Keyword keyword, int offset, Token precedingComment) {
    super(keyword, offset);
    this.precedingComment = precedingComment;
  }

  @Override
  public Token copy() {
    return new KeywordTokenWithComment(getKeyword(), getOffset(), copyComments(precedingComment));
  }

  @Override
  public Token getPrecedingComments() {
    return precedingComment;
  }

  @Override
  protected void applyDelta(int delta) {
    super.applyDelta(delta);
    Token token = precedingComment;
    while (token != null) {
      token.applyDelta(delta);
      token = token.getNext();
    }
  }
}
