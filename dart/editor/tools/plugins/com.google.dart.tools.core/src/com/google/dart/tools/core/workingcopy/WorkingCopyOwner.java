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

import com.google.dart.tools.core.model.SourceFileElement;
import com.google.dart.tools.core.problem.ProblemRequestor;

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
}
