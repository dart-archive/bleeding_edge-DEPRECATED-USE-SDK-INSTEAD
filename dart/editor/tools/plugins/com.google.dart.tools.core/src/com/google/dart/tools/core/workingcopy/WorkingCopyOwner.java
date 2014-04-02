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
package com.google.dart.tools.core.workingcopy;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.buffer.Buffer;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElementDelta;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.SourceFileElement;
import com.google.dart.tools.core.problem.ProblemRequestor;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Instances of the class <code>WorkingCopyOwner</code> represent the owner of a
 * {@link SourceFileElement} handle in working copy mode. An owner is used to identify a working
 * copy and to create its buffer.
 * <p>
 * Clients should subclass this class to instantiate a working copy owner that is specific to their
 * need and that they can pass in to various APIs. Clients can also override the default
 * implementation of {@link #createBuffer(SourceFileElement)}.
 * </p>
 * <p>
 * Note: even though this class has no abstract method, which means that it provides functional
 * default behavior, it is still an abstract class, as clients are intended to own their owner
 * implementation.
 * </p>
 * 
 * @see SourceFileElement#becomeWorkingCopy(org.eclipse.core.runtime.IProgressMonitor)
 * @see SourceFileElement#discardWorkingCopy()
 * @see SourceFileElement#getWorkingCopy(org.eclipse.core.runtime.IProgressMonitor)
 * @coverage dart.tools.core
 */
public abstract class WorkingCopyOwner {
  /**
   * Returns the problem requester used by a working copy of this working copy owner.
   * <p>
   * By default, no problem requester is configured. Clients can override this method to provide a
   * requester.
   * </p>
   * 
   * @param workingCopy the problem requester used for the given working copy
   * @return the problem requester to be used by working copies of this working copy owner or
   *         <code>null</code> if no problem requester is configured
   */
  public ProblemRequestor getProblemRequestor(SourceFileElement<?> workingCopy) {
    return null;
  }

  /**
   * Return a new working copy with the given name using this working copy owner to create its
   * buffer.
   * <p>
   * This working copy always belongs to the default package in a package fragment root that
   * corresponds to its JavaScript project, and this JavaScript project never exists. However this
   * JavaScript project has the given include path that is used when resolving names in this working
   * copy.
   * </p>
   * <p>
   * If a DOM AST is created using this working copy, then given include path will be used if
   * bindings need to be resolved. Problems will be reported to the problem requester of the current
   * working copy owner problem if it is not <code>null</code>.
   * <p>
   * </p>
   * Options used to create the DOM AST are got from {@link DartCore#getOptions()} as it is not
   * possible to set the options on a non-existing Dart project. </p>
   * <p>
   * When the working copy instance is created, an {@link DartElementDelta#ADDED added delta} is
   * reported on this working copy.
   * </p>
   * <p>
   * Once done with the working copy, users of this method must discard it using
   * {@link SourceFileElement#discardWorkingCopy()}.
   * </p>
   * <p>
   * Note that when such working copy is committed, only its buffer is saved (see
   * {@link Buffer#save(IProgressMonitor, boolean)}) but no resource is created.
   * </p>
   * <p>
   * This method is not intended to be overridden by clients.
   * </p>
   * 
   * @param name the name of the working copy (e.g. "X.dart")
   * @param monitor a progress monitor used to report progress while opening the working copy or
   *          <code>null</code> if no progress should be reported
   * @return a new working copy
   * @throws DartModelException if the contents of this working copy can not be determined
   */
  public final CompilationUnit newWorkingCopy(IPath path, IProgressMonitor monitor)
      throws DartModelException {

    return null;
  }

}
