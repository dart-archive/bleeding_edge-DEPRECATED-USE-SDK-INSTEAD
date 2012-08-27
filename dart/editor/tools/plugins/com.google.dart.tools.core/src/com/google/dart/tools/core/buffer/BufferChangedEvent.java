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
package com.google.dart.tools.core.buffer;

import java.util.EventObject;

/**
 * Instances of the class <code>BufferChangedEvent</code> describe how a buffer has changed. These
 * events are used in <code>BufferChangedListener</code> notifications.
 * <p>
 * For text insertions, <code>getOffset</code> is the offset of the first inserted character,
 * <code>getText</code> is the inserted text, and <code>getLength</code> is 0.
 * </p>
 * <p>
 * For text removals, <code>getOffset</code> is the offset of the first removed character,
 * <code>getText</code> is <code>null</code>, and <code>getLength</code> is the length of the text
 * that was removed.
 * </p>
 * <p>
 * For replacements (including <code>Buffer.setContents</code>), <code>getOffset</code> is the
 * offset of the first replaced character, <code>getText</code> is the replacement text, and
 * <code>getLength</code> is the length of the original text that was replaced.
 * </p>
 * <p>
 * When a buffer is closed, <code>getOffset</code> is 0, <code>getLength</code> is 0, and
 * <code>getText</code> is <code>null</code>.
 * </p>
 */
public class BufferChangedEvent extends EventObject {
  private static final long serialVersionUID = 1L;

  /**
   * The length of text that has been modified in the buffer.
   */
  private int length;

  /**
   * The offset into the buffer where the modification took place.
   */
  private int offset;

  /**
   * The text that was modified.
   */
  private String text;

  /**
   * Initialize a newly created buffer changed event indicating that the given buffer has changed in
   * the specified way.
   * 
   * @param buffer the given buffer
   * @param offset the given offset
   * @param length the given length
   * @param text the given text
   */
  public BufferChangedEvent(Buffer buffer, int offset, int length, String text) {
    super(buffer);
    this.offset = offset;
    this.length = length;
    this.text = text;
  }

  /**
   * Return the buffer which has changed.
   * 
   * @return the buffer affected by the change
   */
  public Buffer getBuffer() {
    return (Buffer) this.source;
  }

  /**
   * Return the length of text removed or replaced in the buffer, or 0 if text has been inserted
   * into the buffer.
   * 
   * @return the length of the original text fragment modified by the buffer change (
   *         <code> 0 </code> in case of insertion).
   */
  public int getLength() {
    return this.length;
  }

  /**
   * Return the index of the first character inserted, removed, or replaced in the buffer.
   * 
   * @return the source offset of the textual manipulation in the buffer
   */
  public int getOffset() {
    return this.offset;
  }

  /**
   * Return the text that was inserted, the replacement text, or <code>null</code> if text has been
   * removed.
   * 
   * @return the text corresponding to the buffer change (<code> null </code> in case of deletion).
   */
  public String getText() {
    return this.text;
  }
}
