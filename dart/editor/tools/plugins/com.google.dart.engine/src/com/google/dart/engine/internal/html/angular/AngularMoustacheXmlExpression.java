/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.engine.internal.html.angular;

/**
 * Implementation of {@link AngularXmlExpression} for an {@link AngularExpression} enclosed between
 * <code>{{</code> and <code>}}</code>.
 */
public class AngularMoustacheXmlExpression extends AngularXmlExpression {
  public static final int OPENING_DELIMITER_CHAR = '{';
  public static final int CLOSING_DELIMITER_CHAR = '}';
  public static final String OPENING_DELIMITER = "{{";
  public static final String CLOSING_DELIMITER = "}}";
  public static final int OPENING_DELIMITER_LENGTH = OPENING_DELIMITER.length();
  public static final int CLOSING_DELIMITER_LENGTH = CLOSING_DELIMITER.length();

  /**
   * The offset of the first character of the opening delimiter.
   */
  private final int openingOffset;

  /**
   * The offset of the first character of the closing delimiter.
   */
  private final int closingOffset;

  public AngularMoustacheXmlExpression(int openingOffset, int closingOffset,
      AngularExpression expression) {
    super(expression);
    this.openingOffset = openingOffset;
    this.closingOffset = closingOffset;
  }

  @Override
  public int getEnd() {
    return closingOffset + CLOSING_DELIMITER_LENGTH;
  }

  @Override
  public int getLength() {
    return closingOffset + CLOSING_DELIMITER_LENGTH - openingOffset;
  }

  @Override
  public int getOffset() {
    return openingOffset;
  }
}
