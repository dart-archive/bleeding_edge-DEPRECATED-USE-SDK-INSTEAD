/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.contentassist;

import java.util.Comparator;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.wst.sse.ui.internal.contentassist.IRelevanceCompletionProposal;

public class ProposalComparator implements Comparator {

  public int compare(Object o1, Object o2) {
    int relevance = 0;
    if ((o1 instanceof IRelevanceCompletionProposal)
        && (o2 instanceof IRelevanceCompletionProposal)) {
      // sort based on relevance
      IRelevanceCompletionProposal cp1 = (IRelevanceCompletionProposal) o1;
      IRelevanceCompletionProposal cp2 = (IRelevanceCompletionProposal) o2;

      relevance = cp2.getRelevance() - cp1.getRelevance();

      // if same relevance, secondary sort (lexigraphically)
      if ((relevance == 0) && (o1 instanceof ICompletionProposal)
          && (o2 instanceof ICompletionProposal)) {
        String displayString1 = ((ICompletionProposal) o1).getDisplayString();
        String displayString2 = ((ICompletionProposal) o2).getDisplayString();
        if ((displayString1 != null) && (displayString2 != null)) {
          // relevance = displayString1.compareTo(displayString2);
          // // this didn't mix caps w/ lowercase
          relevance = com.ibm.icu.text.Collator.getInstance().compare(displayString1,
              displayString2);
        }
      }
    }
    // otherwise if it's not ISEDRelevanceCompletionProposal, don't sort
    return relevance;
  }
}
