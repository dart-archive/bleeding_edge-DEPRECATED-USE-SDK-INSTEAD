/*******************************************************************************
 * Copyright (c) 2001, 2007 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.correction;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.wst.sse.ui.internal.StructuredTextViewer;

/**
 * @deprecated since 2.0 RC0 Use org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
 */
public interface IQuickAssistProcessor {
  /**
   * Returns true if the processor can assist at the given offset. This test should be an optimistic
   * guess and be extremly cheap.
   */
  boolean canAssist(StructuredTextViewer viewer, int offset);

  /**
   * Collects proposals for assistant at the given offset.
   */
  ICompletionProposal[] getProposals(StructuredTextViewer viewer, int offset) throws CoreException;
}
