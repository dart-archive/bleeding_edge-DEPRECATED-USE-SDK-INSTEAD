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

import com.google.dart.tools.core.problem.ProblemRequestor;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.UndoEdit;

/**
 * The interface <code>SourceFileElement</code> defines the behavior of objects representing files
 * containing editable Dart source code.
 * 
 * @param <E> the type of the source unit
 * @coverage dart.tools.core.model
 */
public interface SourceFileElement<E> extends OpenableElement, SourceReference {
  /**
   * Applies a text edit to the element's buffer.
   * <p>
   * Note that the edit is simply applied to the element's buffer. In particular the undo edit is
   * not grouped with previous undo edits if the buffer doesn't implement
   * {@link Buffer.TextEditCapability}. If it does, the exact semantics for grouping undo edit
   * depends on how {@link Buffer.TextEditCapability#applyTextEdit(TextEdit, IProgressMonitor)} is
   * implemented.
   * 
   * @param edit the edit to apply
   * @param monitor the progress monitor to use or <code>null</code> if no progress should be
   *          reported
   * @return the undo edit
   * @throws DartModelException if this edit can not be applied to the element's buffer. Reasons
   *           include:
   *           <ul>
   *           <li>This element does not exist (
   *           {@link DartModelStatusConstants#ELEMENT_DOES_NOT_EXIST}).</li> <li>The provided edit
   *           can not be applied as there is a problem with the text edit locations (
   *           {@link DartModelStatusConstants#BAD_TEXT_EDIT_LOCATION}).</li>
   *           </ul>
   */
  public UndoEdit applyTextEdit(TextEdit edit, IProgressMonitor monitor) throws DartModelException;

  /**
   * Change this element handle into a working copy. A new {@link Buffer} is created using this
   * element handle's owner. Uses the primary owner if none was specified when this element handle
   * was created.
   * <p>
   * When switching to working copy mode, problems are reported to the {@link ProblemRequestor
   * problem requester} of the {@link WorkingCopyOwner working copy owner}.
   * <p>
   * Once in working copy mode, changes to this element or its children are done in memory. Only the
   * new buffer is affected. Using {@link #commitWorkingCopy(boolean, IProgressMonitor)} will bring
   * the underlying resource in sync with this element.
   * <p>
   * If this element was already in working copy mode, an internal counter is incremented and no
   * other action is taken on this compilation unit. To bring this element back into the original
   * mode (where it reflects the underlying resource), {@link #discardWorkingCopy} must be called as
   * many times as {@link #becomeWorkingCopy(ProblemRequestor, IProgressMonitor)}.
   * 
   * @param monitor a progress monitor used to report progress while opening this element or
   *          <code>null</code> if no progress should be reported
   * @throws DartModelException if this element could not become a working copy
   * @see #discardWorkingCopy()
   */
  public void becomeWorkingCopy(IProgressMonitor monitor) throws DartModelException;

  /**
   * Commits the contents of this working copy to its underlying resource.
   * <p>
   * It is possible that the contents of the original resource have changed since this working copy
   * was created, in which case there is an update conflict. The value of the <code>force</code>
   * parameter affects the resolution of such a conflict:
   * <ul>
   * <li> <code>true</code> - in this case the contents of this working copy are applied to the
   * underlying resource even though this working copy was created before a subsequent change in the
   * resource</li>
   * <li> <code>false</code> - in this case a {@link DartModelException} is thrown</li>
   * </ul>
   * <p>
   * A working copy can be created on a not-yet existing element. Such a working copy can then be
   * committed in order to create the corresponding element.
   * </p>
   * 
   * @param force a flag to handle the cases when the contents of the original resource have changed
   *          since this working copy was created
   * @param monitor the given progress monitor
   * @throws DartModelException if this working copy could not commit. Reasons include:
   *           <ul>
   *           <li>A {@link org.eclipse.core.runtime.CoreException} occurred while updating an
   *           underlying resource
   *           <li>This element is not a working copy (INVALID_ELEMENT_TYPES)
   *           <li>A update conflict (described above) (UPDATE_CONFLICT)
   *           </ul>
   */
  public void commitWorkingCopy(boolean force, IProgressMonitor monitor) throws DartModelException;

  /**
   * Change this element in working copy mode back to its original mode.
   * <p>
   * This has no effect if this element was not in working copy mode.
   * </p>
   * <p>
   * If {@link #becomeWorkingCopy(IProgressMonitor)} method was called several times on this
   * element, {@link #discardWorkingCopy()} must be called as many times before it switches back to
   * the original mode. Same as for method {@link #getWorkingCopy(IProgressMonitor)}.
   * </p>
   * 
   * @throws DartModelException if this working copy could not return in its original mode.
   * @see #becomeWorkingCopy(ProblemRequestor, IProgressMonitor)
   */
  public void discardWorkingCopy() throws DartModelException;

  /**
   * Return the modification stamp associated with this element, or {@link IResource#NULL_STAMP} if
   * this element does not exist or if the modification stamp cannot be accessed. The modification
   * stamp for a element changes every time the element changes but stays constant if the element
   * has not changed.
   * 
   * @return the modification stamp associated with this element
   */
  public long getModificationStamp();

  /**
   * Returns the primary Dart element (whose owner is the primary owner) this working copy was
   * created from, or this Dart element if this a primary Dart file.
   * <p>
   * Note that the returned primary Dart element can be in working copy mode.
   * 
   * @return the primary Dart element this working copy was created from, or this Dart element if it
   *         is primary
   */
  public E getPrimary();

  /**
   * Return a new working copy of this element if it is a primary element, or this element if it is
   * already a non-primary working copy.
   * <p>
   * Note: if intending to share a working copy amongst several clients, then
   * {@link #getWorkingCopy(WorkingCopyOwner, ProblemRequestor, IProgressMonitor)} should be used
   * instead.
   * <p>
   * When the working copy instance is created, an ADDED DartElementDelta is reported on this
   * working copy.
   * <p>
   * Once done with the working copy, users of this method must discard it using
   * {@link #discardWorkingCopy()}.
   * <p>
   * A working copy can be created on a not-yet existing element. In particular, such a working copy
   * can then be committed in order to create the corresponding element.
   * 
   * @param monitor a progress monitor used to report progress while opening this element or
   *          <code>null</code> if no progress should be reported
   * @return a new working copy of this element if this element is not a working copy, or this
   *         element if this element is already a working copy
   * @throws DartModelException if the contents of this element can not be determined
   */
  public E getWorkingCopy(IProgressMonitor monitor) throws DartModelException;

  /**
   * Return <code>true</code> if this element is a working copy.
   * 
   * @return <code>true</code> if this element is a working copy
   */
  public boolean isWorkingCopy();
}
