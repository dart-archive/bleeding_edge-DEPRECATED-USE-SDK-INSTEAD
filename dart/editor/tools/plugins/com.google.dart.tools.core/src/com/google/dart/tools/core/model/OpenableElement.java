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
package com.google.dart.tools.core.model;

import com.google.dart.tools.core.buffer.Buffer;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * The interface <code>OpenableElement</code> defines behavior common to Dart elements that must be
 * opened before they can be navigated or modified. Opening a textual element (such as a compilation
 * unit) involves opening a buffer on its contents. While open, any changes to the buffer can be
 * reflected in the element's structure; see {@link #isConsistent()} and
 * {@link #makeConsistent(IProgressMonitor)}.
 * <p>
 * To reduce complexity in clients, elements are automatically opened by the Dart model as element
 * properties are accessed. The Dart model maintains an LRU cache of open elements, and
 * automatically closes elements as they are swapped out of the cache to make room for other
 * elements. Elements with unsaved changes are never removed from the cache, and thus, if the client
 * maintains many open elements with unsaved changes, the LRU cache can grow in size (in this case
 * the cache is not bounded). However, as elements are saved, the cache will shrink back to its
 * original bounded size.
 * <p>
 * To open an element, all openable parent elements must be open. The Dart model automatically opens
 * parent elements, as it automatically opens elements. Opening an element may provide access to
 * direct children and other descendants, but does not automatically open any descendents which are
 * themselves <code>Openable</code>. For example, opening a compilation unit provides access to all
 * its constituent elements, but opening a library does not open all compilation units in the
 * library.
 */
public interface OpenableElement extends DartElement {
  /**
   * Close this element and its buffer (if any). Closing an element which is not open has no effect.
   * <p>
   * Note: although {@link #close} is exposed in the API, clients are not expected to open and close
   * elements - the Dart model does this automatically as elements are accessed.
   * 
   * @throws DartModelException if an error occurs while closing this element
   */
  public void close() throws DartModelException;

  /**
   * Find and return the recommended line separator for this element. The element's buffer is first
   * searched and the first line separator in this buffer is returned if any. Otherwise the
   * preference {@link org.eclipse.core.runtime.Platform#PREF_LINE_SEPARATOR} on this element's
   * project or workspace is returned. Finally if no such preference is set, the system line
   * separator is returned.
   * 
   * @return the recommended line separator for this element
   * @throws DartModelException if this element does not exist or if an exception occurs while
   *           accessing its corresponding resource.
   */
  public String findRecommendedLineSeparator() throws DartModelException;

  /**
   * Return the buffer opened for this element, or <code>null</code> if this element does not have a
   * buffer.
   * 
   * @return the buffer opened for this element
   * @throws DartModelException if this element does not exist or if an exception occurs while
   *           accessing its corresponding resource.
   */
  public Buffer getBuffer() throws DartModelException;

  /**
   * Return <code>true</code> if this element is open and:
   * <ul>
   * <li>its buffer has unsaved changes, or
   * <li>one of its descendants has unsaved changes, or
   * <li>a working copy has been created on one of this element's children and has not yet destroyed
   * </ul>
   * 
   * @return <code>true</code> if this element is open and:
   *         <ul>
   *         <li>its buffer has unsaved changes, or
   *         <li>one of its descendants has unsaved changes, or
   *         <li>a working copy has been created on one of this element's children and has not yet
   *         destroyed
   *         </ul>
   * @throws DartModelException if this element does not exist or if an exception occurs while
   *           accessing its corresponding resource
   */
  public boolean hasUnsavedChanges() throws DartModelException;

  /**
   * Return <code>true</code> if the element is consistent with its underlying resource or buffer.
   * The element is consistent when opened, and is consistent if the underlying resource or buffer
   * has not been modified since it was last consistent.
   * <p>
   * NOTE: Child consistency is not considered. For example, a library responds <code>true</code>
   * when it knows about all of its compilation units present in its underlying folder. However, one
   * or more of the compilation units could be inconsistent.
   * 
   * @return <code>true</code> if the element is consistent with its underlying resource or buffer
   * @throws DartModelException if this element does not exist or if an exception occurs while
   *           accessing its corresponding resource
   * @see IOpenable#makeConsistent(IProgressMonitor)
   */
  public boolean isConsistent() throws DartModelException;

  /**
   * Return <code>true</code> if this openable is open. This is a handle-only method.
   * 
   * @return <code>true</code> if this openable is open
   */
  public boolean isOpen();

  /**
   * Make this element consistent with its underlying resource or buffer by updating the element's
   * structure and properties as necessary.
   * <p>
   * Note: Using this functionality on a working copy will interfere with any subsequent reconciling
   * operation. Indeed, the next
   * {@link ICompilationUnit#reconcile(int, boolean, WorkingCopyOwner, IProgressMonitor)} or
   * {@link ICompilationUnit#reconcile(int, boolean, boolean, WorkingCopyOwner, IProgressMonitor)}
   * operation will not account for changes which occurred before an explicit use of
   * {@link #makeConsistent(IProgressMonitor)}
   * <p>
   * 
   * @param progress the given progress monitor
   * @throws DartModelException if the element is unable to access the contents of its underlying
   *           resource. Reasons include:
   *           <ul>
   *           <li>This Dart element does not exist (ELEMENT_DOES_NOT_EXIST) </li>
   *           </ul>
   * @see IOpenable#isConsistent()
   * @see ICompilationUnit#reconcile(int, boolean, WorkingCopyOwner, IProgressMonitor)
   */
  public void makeConsistent(IProgressMonitor progress) throws DartModelException;

  /**
   * Open this element and all parent elements that are not already open. For compilation units, a
   * buffer is opened on the contents of the underlying resource.
   * <p>
   * Note: although {@link #open} is exposed in the API, clients are not expected to open and close
   * elements - the Dart model does this automatically as elements are accessed.
   * 
   * @param progress the given progress monitor
   * @throws DartModelException if an error occurs accessing the contents of its underlying
   *           resource. Reasons include:
   *           <ul>
   *           <li>This Dart element does not exist (ELEMENT_DOES_NOT_EXIST) </li>
   *           </ul>
   */
  public void open(IProgressMonitor progress) throws DartModelException;

  /**
   * Save any changes in this element's buffer to its underlying resource via a workspace resource
   * operation. This has no effect if the element has no underlying buffer, or if there are no
   * unsaved changed in the buffer.
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
   * As a result of this operation, the element is consistent with its underlying resource or
   * buffer.
   * 
   * @param progress the given progress monitor
   * @param force it controls how this method deals with cases where the workbench is not completely
   *          in sync with the local file system
   * @throws DartModelException if an error occurs accessing the contents of its underlying
   *           resource. Reasons include:
   *           <ul>
   *           <li>This Dart element does not exist (ELEMENT_DOES_NOT_EXIST) </li> <li>This Dart
   *           element is read-only (READ_ONLY)</li>
   *           </ul>
   */
  public void save(IProgressMonitor progress, boolean force) throws DartModelException;
}
