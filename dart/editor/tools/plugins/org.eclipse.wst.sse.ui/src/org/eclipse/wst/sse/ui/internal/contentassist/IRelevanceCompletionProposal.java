/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.contentassist;

/**
 * CompletionProposal with a relevance value. The relevance value is used to sort the completion
 * proposals. Proposals with higher relevance should be listed before proposals with lower
 * relevance.
 * 
 * @author pavery
 */
public interface IRelevanceCompletionProposal {
  /**
   * Returns the relevance of the proposal.
   */
  int getRelevance();
}
