/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.provisional.extensions.breakpoint;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.wst.sse.ui.internal.provisional.extensions.ISourceEditingTextTools;

/**
 * Interface to provide breakpoint creation
 */
public interface IBreakpointProvider {

  /**
   * Adds breakpoint to specified position
   * 
   * @param document IDocument object
   * @param input current editor input, not necessarily an IFileEditorInput or linked to a resource
   *          in any way
   * @param lineNumber current line number
   * @param offset current caret offset
   * @throws CoreException
   * @return IStatus the status after being asked to add a breakpoint. The Severity of ERROR should
   *         only be used if the location information is both valid for a breakpoint and one could
   *         not be added. Any severity greater than INFO will be logged, and if no breakpoints were
   *         created, shown to the user.
   */
  IStatus addBreakpoint(IDocument document, IEditorInput input, int lineNumber, int offset)
      throws CoreException;

  /**
   * Returns corresponding resource from editor input
   * 
   * @param input
   * @return IResource
   */
  IResource getResource(IEditorInput input);

  /**
   * Set ISourceEditingTextTools object
   * 
   * @param tool ISourceEditingTextTools object
   */
  void setSourceEditingTextTools(ISourceEditingTextTools tool);
}
