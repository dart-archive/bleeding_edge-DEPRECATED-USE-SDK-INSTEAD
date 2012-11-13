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
package com.google.dart.engine.formatter.edit;

/**
 * Describes a text edit.
 */
public class Edit {

  /**
   * The offset at which to apply the edit.
   */
  public final int offset;

  /**
   * The length of text to replace.
   */
  public final int length;

  /**
   * The replacement text.
   */
  public final String replacement;

  /**
   * Create an edit.
   * 
   * @param offset the offset at which to apply the edit
   * @param length the length of text to replace
   * @param replacement the replacement text
   */
  public Edit(int offset, int length, String replacement) {
    this.offset = offset;
    this.length = length;
    this.replacement = replacement;
  }

  @Override
  public String toString() {
    return (offset < 0 ? "(" : "X(") + offset + ", length " + length + " :>" + replacement + "<"; //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$ //$NON-NLS-5$
  }

}
