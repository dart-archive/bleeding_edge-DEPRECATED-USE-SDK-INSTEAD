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
package com.google.dart.engine.formatter;

import com.google.dart.engine.formatter.edit.Edit;
import com.google.dart.engine.formatter.edit.EditStore;

/**
 * Records a series of edits to a source string that will cause the string to be formatted when
 * applied.
 */
public abstract class EditRecorder<T> {

  //indentation
  protected int indentationLevel;
  protected final int indentationSize;
  protected int numberOfIndentations;
  protected int tabLength;

  //spaces
  protected boolean needSpace;
  protected boolean pendingSpace;

  protected int lastNumberOfNewLines;

  protected int column; //NOTE: one-based
  protected int line;

  protected int[] lineEnds;

  protected int scannerEndPosition;

  @SuppressWarnings("unused")
  private final Scanner scanner;
  private final EditStore editStore;

  /**
   * Create an instance with the given set of options.
   * 
   * @param options formatting options
   * @param scanner the scanner
   * @param editStore the edit store
   */
  protected EditRecorder(CodeFormatterOptions options, Scanner scanner, EditStore editStore) {
    this.scanner = scanner;
    this.editStore = editStore;
    this.indentationSize = options.indentation_size;
    this.tabLength = options.tab_size;
    reset();
  }

  /**
   * Create an instance using the default formatting options.
   * 
   * @param scanner the scanner
   * @param editStore the edit store
   */
  protected EditRecorder(Scanner scanner, EditStore editStore) {
    this(CodeFormatterOptions.getDefaults(), scanner, editStore);
  }

  /**
   * Build the resulting source edit.
   * 
   * @return the resulting source edit.
   */
  public abstract T buildEdit();

  /**
   * Indent.
   */
  public void indent() {
    indentationLevel += indentationSize;
    numberOfIndentations++;
  }

  /**
   * Reset.
   */
  public void reset() {
    line = 0;
    column = 1;
    editStore.reset();
  }

  /**
   * Space.
   */
  public void space() {
    if (!needSpace) {
      return;
    }
    lastNumberOfNewLines = 0;
    pendingSpace = true;
    column++;
    needSpace = false;
  }

  /**
   * Un-indent.
   */
  public void unIndent() {
    indentationLevel -= indentationSize;
    numberOfIndentations--;
  }

  /**
   * Add an {@link Edit} that describes a textual replacement of a text interval starting at the
   * given offset spanning the given length.
   * 
   * @param offset the offset at which to begin the edit
   * @param length the length of the text interval to replace
   * @param replacement the replacement text
   * @see EditStore#addEdit(int, int, String)
   */
  protected void addEdit(int offset, int length, String replacement) {
    editStore.addEdit(offset, length, replacement);
  }

  /**
   * Get the last edit.
   * 
   * @return the last edit (or {@code null} if there is none).
   * @see EditStore#getLastEdit()
   */
  protected Edit getLastEdit() {
    return editStore.getLastEdit();
  }

  /**
   * Add an {@link Edit} that describes an insertion of text starting at the given offset.
   * 
   * @param offset the offset at which to begin the edit
   * @param insertedString the text to insert
   */
  protected void insert(int insertPosition, String insertedString) {
    editStore.insert(insertPosition, insertedString);
  }

}
