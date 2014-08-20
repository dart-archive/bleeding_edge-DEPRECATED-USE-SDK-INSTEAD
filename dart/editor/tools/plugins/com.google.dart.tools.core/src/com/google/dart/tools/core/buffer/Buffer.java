/*
 * Copyright (c) 2011, the Dart project authors.
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

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartModelStatusConstants;
import com.google.dart.tools.core.model.OpenableElement;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.UndoEdit;

/**
 * The interface <code>Buffer</code> defines the behavior of objects that contain the contents of a
 * resource. It is not language-specific. The contents of the buffer might be in the process of
 * being edited, differing from the actual contents of the underlying resource. If a buffer does not
 * have an underlying resource, saving the buffer has no effect. Buffers can be read-only.
 * <p>
 * Note that Dart model operations that manipulate a <code>Buffer</code> (for example,
 * <code>Type.createMethod(...)</code>) ensure that the same line delimiter (either
 * <code>"\n"</code> or <code>"\r"</code> or <code>"\r\n"</code>) is used across the whole buffer.
 * Thus these operations may change the line delimiter(s) included in the string to be append, or
 * replaced. However, implementers of this interface should be aware that other clients of
 * <code>Buffer</code> might not do such transformations beforehand.
 * <p>
 * This interface may be implemented by clients.
 * </p>
 * 
 * @coverage dart.tools.core.buffer
 */
public interface Buffer {
  /**
   * Implementors of {@link Buffer} can additionally implement {@link Buffer.TextEditCapability}.
   * This adds the capability to apply text edits to the buffer and will be used by
   * {@link CompilationUnit#applyTextEdit(TextEdit, IProgressMonitor)}.
   * <p>
   * This interface may be implemented by clients.
   */
  public interface TextEditCapability {
    /**
     * Apply the given text edit to this underlying buffer.
     * 
     * @param edit the edit to apply
     * @param monitor the progress monitor to use or <code>null</code> if no progress should be
     *          reported
     * @return the undo edit
     * @throws DartModelException if this edit cannot be applied to the buffer. Reasons include:
     *           <ul>
     *           <li>The provided edit can not be applied as there is a problem with the text edit
     *           locations ( {@link DartModelStatusConstants#BAD_TEXT_EDIT_LOCATION})}.</li>
     *           </ul>
     */
    public UndoEdit applyTextEdit(TextEdit edit, IProgressMonitor monitor)
        throws DartModelException;
  }

  /**
   * Adds the given listener for changes to this buffer. Has no effect if an identical listener is
   * already registered or if the buffer is closed.
   * 
   * @param listener the listener of buffer changes
   */
  public void addBufferChangedListener(BufferChangedListener listener);

  /**
   * Appends the given character array to the contents of the buffer. This buffer will now have
   * unsaved changes. Any client can append to the contents of the buffer, not just the owner of the
   * buffer. Reports a buffer changed event.
   * <p>
   * Has no effect if this buffer is read-only or if the buffer is closed.
   * 
   * @param text the given character array to append to contents of the buffer
   */
  public void append(char[] text);

  /**
   * Appends the given string to the contents of the buffer. This buffer will now have unsaved
   * changes. Any client can append to the contents of the buffer, not just the owner of the buffer.
   * Reports a buffer changed event.
   * <p>
   * Has no effect if this buffer is read-only or if the buffer is closed.
   * 
   * @param text the <code>String</code> to append to the contents of the buffer
   */
  public void append(String text);

  /**
   * Closes the buffer. Any unsaved changes are lost. Reports a buffer changed event with a 0 offset
   * and a 0 length. When this event is fired, the buffer should already be closed.
   * <p>
   * Further operations on the buffer are not allowed, except for close. If an attempt is made to
   * close an already closed buffer, the second attempt has no effect.
   */
  public void close();

  /**
   * Returns the character at the given position in this buffer.
   * <p>
   * The returned value is undefined if the buffer is closed.
   * 
   * @param position a zero-based source offset in this buffer
   * @return the character at the given position in this buffer
   */
  public char getChar(int position);

  /**
   * Returns the contents of this buffer as a character array, or <code>null</code> if the buffer
   * has not been initialized.
   * <p>
   * Callers should make no assumption about whether the returned character array is or is not the
   * genuine article or a copy. In other words, if the client wishes to change this array, they
   * should make a copy. Likewise, if the client wishes to hang on to the array in its current
   * state, they should make a copy.
   * </p>
   * <p>
   * The returned value is undefined if the buffer is closed.
   * 
   * @return the characters contained in this buffer
   */
  public char[] getCharacters();

  /**
   * Returns the contents of this buffer as a <code>String</code>. Like all strings, the result is
   * an immutable value object., It can also answer <code>null</code> if the buffer has not been
   * initialized.
   * <p>
   * The returned value is undefined if the buffer is closed.
   * 
   * @return the contents of this buffer as a <code>String</code>
   */
  public String getContents();

  /**
   * Returns number of characters stored in this buffer.
   * <p>
   * The returned value is undefined if the buffer is closed.
   * 
   * @return the number of characters in this buffer
   */
  public int getLength();

  /**
   * Returns the Dart element owning of this buffer.
   * 
   * @return the Dart element owning this buffer
   */
  public OpenableElement getOwner();

  /**
   * Returns the given range of text in this buffer.
   * <p>
   * The returned value is undefined if the buffer is closed.
   * 
   * @param offset the zero-based starting offset
   * @param length the number of characters to retrieve
   * @return the given range of text in this buffer
   */
  public String getText(int offset, int length);

  /**
   * Returns the underlying resource for which this buffer was opened, or <code>null</code> if this
   * buffer was not opened on a resource.
   * 
   * @return the underlying resource for this buffer, or <code>null</code> if none.
   */
  public IResource getUnderlyingResource();

  /**
   * Returns whether this buffer has been modified since it was opened or since it was last saved.
   * If a buffer does not have an underlying resource, this method always returns <code>true</code>.
   * <p>
   * NOTE: when a buffer does not have unsaved changes, the model may decide to close it to claim
   * some memory back. If the associated element needs to be reopened later on, its buffer factory
   * will be requested to create a new buffer.
   * </p>
   * 
   * @return a <code>boolean</code> indicating presence of unsaved changes (in the absence of any
   *         underlying resource, it will always return <code>true</code>).
   */
  public boolean hasUnsavedChanges();

  /**
   * Returns whether this buffer has been closed.
   * 
   * @return a <code>boolean</code> indicating whether this buffer is closed.
   */
  public boolean isClosed();

  /**
   * Returns whether this buffer is read-only.
   * 
   * @return a <code>boolean</code> indicating whether this buffer is read-only
   */
  public boolean isReadOnly();

  /**
   * Removes the given listener from this buffer. Has no affect if an identical listener is not
   * registered or if the buffer is closed.
   * 
   * @param listener the listener
   */
  public void removeBufferChangedListener(BufferChangedListener listener);

  /**
   * Replaces the given range of characters in this buffer with the given text.
   * <code>position</code> and <code>position + length</code> must be in the range [0, getLength()].
   * <code>length</code> must not be negative.
   * <p>
   * Has no effect if this buffer is read-only or if the buffer is closed.
   * 
   * @param position the zero-based starting position of the affected text range in this buffer
   * @param length the length of the affected text range in this buffer
   * @param text the replacing text as a character array
   */
  public void replace(int position, int length, char[] text);

  /**
   * Replaces the given range of characters in this buffer with the given text.
   * <code>position</code> and <code>position + length</code> must be in the range [0, getLength()].
   * <code>length</code> must not be negative.
   * <p>
   * Has no effect if this buffer is read-only or if the buffer is closed.
   * 
   * @param position the zero-based starting position of the affected text range in this buffer
   * @param length the length of the affected text range in this buffer
   * @param text the replacing text as a <code>String</code>
   */
  public void replace(int position, int length, String text);

  /**
   * Saves the contents of this buffer to its underlying resource. If successful, this buffer will
   * have no unsaved changes. The buffer is left open. Saving a buffer with no unsaved changes has
   * no effect - the underlying resource is not changed. If the buffer does not have an underlying
   * resource or is read-only, this has no effect.
   * <p>
   * The <code>force</code> parameter controls how this method deals with cases where the workbench
   * is not completely in sync with the local file system. If <code>false</code> is specified, this
   * method will only attempt to overwrite a corresponding file in the local file system provided it
   * is in sync with the workbench. This option ensures there is no unintended data loss; it is the
   * recommended setting. However, if <code>true</code> is specified, an attempt will be made to
   * write a corresponding file in the local file system, overwriting any existing one if need be.
   * In either case, if this method succeeds, the resource will be marked as being local (even if it
   * wasn't before).
   * <p>
   * Has no effect if this buffer is read-only or if the buffer is closed.
   * 
   * @param progress the progress monitor to notify
   * @param force a <code> boolean </code> flag indicating how to deal with resource
   *          inconsistencies.
   * @throws DartModelException if an error occurs writing the buffer to the underlying resource
   * @see org.eclipse.core.resources.IFile#setContents(java.io.InputStream, boolean, boolean,
   *      org.eclipse.core.runtime.IProgressMonitor)
   */
  public void save(IProgressMonitor progress, boolean force) throws DartModelException;

  /**
   * Sets the contents of this buffer to the given character array. This buffer will now have
   * unsaved changes. Any client can set the contents of the buffer, not just the owner of the
   * buffer. Reports a buffer changed event.
   * <p>
   * Equivalent to <code>replace(0,getLength(),contents)</code>.
   * </p>
   * <p>
   * Has no effect if this buffer is read-only or if the buffer is closed.
   * 
   * @param contents the new contents of this buffer as a character array
   */
  public void setContents(char[] contents);

  /**
   * Sets the contents of this buffer to the given <code>String</code>. This buffer will now have
   * unsaved changes. Any client can set the contents of the buffer, not just the owner of the
   * buffer. Reports a buffer changed event.
   * <p>
   * Equivalent to <code>replace(0,getLength(),contents)</code>.
   * </p>
   * <p>
   * Has no effect if this buffer is read-only or if the buffer is closed.
   * 
   * @param contents the new contents of this buffer as a <code>String</code>
   */
  public void setContents(String contents);
}
