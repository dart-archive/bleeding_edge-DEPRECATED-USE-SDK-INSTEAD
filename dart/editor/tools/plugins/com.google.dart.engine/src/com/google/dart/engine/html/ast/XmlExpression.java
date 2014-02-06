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
package com.google.dart.engine.html.ast;

import com.google.dart.engine.element.Element;

/**
 * Instances of the class {@code XmlExpression} represent an abstract expression embedded into
 * {@link XmlNode}.
 */
public abstract class XmlExpression {
  /**
   * The reference to the {@link Element}.
   */
  public static final class Reference {
    public final Element element;
    public final int offset;
    public final int length;

    public Reference(Element element, int offset, int length) {
      this.element = element;
      this.offset = offset;
      this.length = length;
    }
  }

  /**
   * An empty array of expressions.
   */
  public static final XmlExpression[] EMPTY_ARRAY = new XmlExpression[0];

  /**
   * Check if the given offset belongs to the expression's source range.
   */
  public boolean contains(int offset) {
    return getOffset() <= offset && offset < getEnd();
  }

  /**
   * Return the offset of the character immediately following the last character of this
   * expression's source range. This is equivalent to {@code getOffset() + getLength()}.
   * 
   * @return the offset of the character just past the expression's source range
   */
  public abstract int getEnd();

  /**
   * Return the number of characters in the expression's source range.
   */
  public abstract int getLength();

  /**
   * Return the offset of the first character in the expression's source range.
   */
  public abstract int getOffset();

  /**
   * Return the {@link Reference} at the given offset.
   * 
   * @param offset the offset from the beginning of the file
   * @return the {@link Reference} at the given offset, maybe {@code null}
   */
  public abstract Reference getReference(int offset);
}
