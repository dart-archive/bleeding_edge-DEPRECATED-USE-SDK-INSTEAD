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
 * Manages stored edits.
 */
public interface EditStore {

  /**
   * Add an {@link Edit} that describes a textual replacement of a text interval starting at the
   * given offset spanning the given length.
   * 
   * @param offset the offset at which to begin the edit
   * @param length the length of the text interval to replace
   * @param replacement the replacement text
   */
  void addEdit(int offset, int length, String replacement);

  /**
   * Get the index of the current edit (for use in caching location information).
   * 
   * @return the current edit index
   */
  int getCurrentEditIndex();

  /**
   * Get the last edit.
   * 
   * @return the last edit (or {@code null} if there is none).
   */
  Edit getLastEdit();

  /**
   * Add an {@link Edit} that describes an insertion of text starting at the given offset.
   * 
   * @param offset the offset at which to begin the edit
   * @param insertedString the text to insert
   */
  void insert(int offset, String insertedString);

  /**
   * Reset cached state.
   */
  void reset();

}
