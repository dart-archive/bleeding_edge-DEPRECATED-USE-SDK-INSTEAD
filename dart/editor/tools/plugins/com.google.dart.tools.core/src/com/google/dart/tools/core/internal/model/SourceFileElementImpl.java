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
package com.google.dart.tools.core.internal.model;

import com.google.dart.compiler.DartSource;
import com.google.dart.compiler.util.DartSourceString;
import com.google.dart.tools.core.buffer.Buffer;
import com.google.dart.tools.core.internal.buffer.DocumentAdapter;
import com.google.dart.tools.core.internal.util.CharOperation;
import com.google.dart.tools.core.internal.util.Util;
import com.google.dart.tools.core.internal.workingcopy.DefaultWorkingCopyOwner;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartModelStatusConstants;
import com.google.dart.tools.core.model.SourceFileElement;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.SourceReference;
import com.google.dart.tools.core.problem.ProblemRequestor;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.UndoEdit;

/**
 * Instances of the class <code>SourceFileElementImpl</code> implement an object that represents an
 * editable dart source file.
 * 
 * @param <E> the type of source file being represented
 */
public abstract class SourceFileElementImpl<E> extends OpenableElementImpl implements
    SourceFileElement<E>, SourceReference {
  /**
   * The working copy owner for this source file element. If the source file element is not a
   * working copy, then this field will be assigned the default working copy owner.
   */
  protected WorkingCopyOwner owner;

  /**
   * The file being represented by this element.
   */
  private IFile file;

  /**
   * Initialize a newly created source file element to be a child of the given container.
   * 
   * @param container the library or library folder containing the element
   * @param file the file being represented by the element
   * @param owner the working copy owner for this source file element
   */
  public SourceFileElementImpl(CompilationUnitContainer container, IFile file,
      WorkingCopyOwner owner) {
    super((DartElementImpl) container);
    this.file = file;
    this.owner = owner;
  }

  @Override
  public UndoEdit applyTextEdit(TextEdit edit, IProgressMonitor monitor) throws DartModelException {
    Buffer buffer = getBuffer();
    if (buffer instanceof Buffer.TextEditCapability) {
      return ((Buffer.TextEditCapability) buffer).applyTextEdit(edit, monitor);
    } else if (buffer != null) {
      IDocument document = buffer instanceof IDocument ? (IDocument) buffer : new DocumentAdapter(
          buffer);
      try {
        UndoEdit undoEdit = edit.apply(document);
        return undoEdit;
      } catch (MalformedTreeException e) {
        throw new DartModelException(e, DartModelStatusConstants.BAD_TEXT_EDIT_LOCATION);
      } catch (BadLocationException e) {
        throw new DartModelException(e, DartModelStatusConstants.BAD_TEXT_EDIT_LOCATION);
      }
    }
    // cannot happen, there are no source file elements without buffer
    return null;
  }

  @Override
  public void becomeWorkingCopy(IProgressMonitor monitor) throws DartModelException {
    ProblemRequestor requestor = owner == null ? null : owner.getProblemRequestor(this);
    becomeWorkingCopy(requestor, monitor);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof SourceFileElementImpl)) {
      return false;
    }
    SourceFileElementImpl<?> other = (SourceFileElementImpl<?>) obj;
    return owner.equals(other.owner) && super.equals(obj);
  }

  @Override
  public String getElementName() {
    return file.getName();
  }

  public IFile getFile() {
    return file;
  }

  @Override
  public long getModificationStamp() {
    IResource resource = getResource();
    if (resource == null) {
      return IResource.NULL_STAMP;
    }
    return resource.getModificationStamp();
  }

  @Override
  public SourceRange getNameRange() {
    return null;
  }

  @Override
  public WorkingCopyOwner getOwner() {
    return isPrimary() || !isWorkingCopy() ? null : owner;
  }

  @Override
  public IPath getPath() {
    if (getFile() != null) {
      return getFile().getFullPath();
    }
    return null;
  }

  /**
   * Returns the primary Dart source file element (whose owner is the primary owner) this working
   * copy was created from, or this Dart source file element if this a primary Dart file.
   * <p>
   * Note that the returned primary Dart source file element can be in working copy mode.
   * 
   * @return the primary Dart source file element this working copy was created from, or this Dart
   *         source file element if it is primary
   */
  @SuppressWarnings("unchecked")
  @Override
  public E getPrimary() {
    return (E) getPrimaryElement(true);
  }

  @Override
  public String getSource() throws DartModelException {
    Buffer buffer = getBuffer();
    if (buffer == null) {
      return ""; //$NON-NLS-1$
    }
    return buffer.getContents();
  }

  @Override
  public SourceRange getSourceRange() throws DartModelException {
    Buffer buffer = getBuffer();
    if (buffer == null) {
      return null;
    }
    return new SourceRangeImpl(0, buffer.getLength());
  }

  @Override
  public DartSource getSourceRef() throws DartModelException {
    return new DartSourceString(getElementName(), getSource());
  }

  @Override
  public IResource getUnderlyingResource() throws DartModelException {
    return file;
  }

  @Override
  public E getWorkingCopy(IProgressMonitor monitor) throws DartModelException {
    return getWorkingCopy(new WorkingCopyOwner() {
    }, null, monitor);
  }

  @Override
  public E getWorkingCopy(WorkingCopyOwner workingCopyOwner, IProgressMonitor monitor)
      throws DartModelException {
    return getWorkingCopy(workingCopyOwner, null, monitor);
  }

  @Override
  public boolean isPrimary() {
    return owner == DefaultWorkingCopyOwner.getInstance();
  }

  @Override
  public boolean isReadOnly() {
    return file.isReadOnly();
  }

  @Override
  public boolean isWorkingCopy() {
    // For backward compatibility, non primary working copies are always
    // returning true; in removal delta, clients can still check that element
    // was a working copy before being discarded.
    return !isPrimary() || getPerWorkingCopyInfo() != null;
  }

  @Override
  public IResource resource() {
    return file;
  }

  /**
   * Change this source file element handle into a working copy.
   * 
   * @param requestor the problem requester used to collect problems encountered in the creation of
   *          this working copy
   * @param monitor a progress monitor used to report progress while opening this source file
   *          element or <code>null</code> if no progress should be reported
   * @throws DartModelException if this source file element could not become a working copy
   * @see #becomeWorkingCopy(IProgressMonitor)
   */
  protected abstract void becomeWorkingCopy(ProblemRequestor requestor, IProgressMonitor monitor)
      throws DartModelException;

  /**
   * Return the per working copy info for the receiver, or null if none exists. Note: the use count
   * of the per working copy info is NOT incremented.
   * 
   * @return the per working copy info for the receiver
   */
  protected abstract PerWorkingCopyInfo getPerWorkingCopyInfo();

  /**
   * Return a shared working copy of this source file element using the given working copy owner to
   * create the buffer.
   * 
   * @param owner the working copy owner that creates a buffer that is used to get the content of
   *          the working copy
   * @param monitor a progress monitor used to report progress while opening this source file
   *          element or <code>null</code> if no progress should be reported
   * @param problemRequestor the problem requester used to collect problems encountered in the
   *          creation of this working copy
   * @return a new working copy of this source file element using the given owner to create the
   *         buffer, or this source file element if it is already a working copy
   * @throws DartModelException if the contents of this element can not be determined
   * @see #getWorkingCopy(IProgressMonitor)
   */
  protected abstract E getWorkingCopy(WorkingCopyOwner workingCopyOwner,
      ProblemRequestor problemRequestor, IProgressMonitor monitor) throws DartModelException;

  /**
   * Read this source file's contents into the given buffer.
   * 
   * @param buffer the buffer into which file contents will be read
   * @param isWorkingCopy <code>true</code> if this is a working copy, <code>false</code> otherwise
   * @throws DartModelException if the file does not exist
   */
  protected void readBuffer(Buffer buffer, boolean isWorkingCopy) throws DartModelException {
    IFile file = (IFile) getResource();
    if (file == null || !file.exists()) {
      if (!isWorkingCopy) {
        throw newNotPresentException();
      }
      // initialize buffer with empty contents
      buffer.setContents(CharOperation.NO_CHAR);
    } else {
      buffer.setContents(Util.getResourceContentsAsCharArray(file));
    }
  }
}
