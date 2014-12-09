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
package com.google.dart.tools.ui.text.dart;

import com.google.dart.tools.ui.internal.text.completion.AbstractDartCompletionProposal;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.templates.TemplateProposal;

import java.util.Comparator;

/**
 * Comparator for JavaScript completion proposals. Completion proposals can be sorted by relevance
 * or alphabetically.
 * <p>
 * Note: this comparator imposes orderings that are inconsistent with equals.
 * </p>
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
public final class CompletionProposalComparator implements Comparator<Object> {

  private boolean fOrderAlphabetically;

  /**
   * Creates a comparator that sorts by relevance.
   */
  public CompletionProposalComparator() {
    fOrderAlphabetically = false;
  }

  /*
   * @see Comparator#compare(Object, Object)
   */
  @Override
  public int compare(Object o1, Object o2) {
    ICompletionProposal p1 = (ICompletionProposal) o1;
    ICompletionProposal p2 = (ICompletionProposal) o2;

    if (!fOrderAlphabetically) {
      int r1 = getRelevance(p1);
      int r2 = getRelevance(p2);
      int relevanceDif = r1 - r2;
      if (relevanceDif != 0) {
        return relevanceDif;
      }
    }
    /*
     * TODO the correct (but possibly much slower) sorting would use a collator.
     */
    // fix for bug 67468
    return getSortKey(p1).compareToIgnoreCase(getSortKey(p2));
  }

  /**
   * Sets the sort order. Default is <code>false</code>, i.e. order by relevance.
   * 
   * @param orderAlphabetically <code>true</code> to order alphabetically, <code>false</code> to
   *          order by relevance
   */
  public void setOrderAlphabetically(boolean orderAlphabetically) {
    fOrderAlphabetically = orderAlphabetically;
  }

  private int getRelevance(ICompletionProposal obj) {
    if (obj instanceof IDartCompletionProposal) {
      IDartCompletionProposal jcp = (IDartCompletionProposal) obj;
      return jcp.getRelevance();
    } else if (obj instanceof TemplateProposal) {
      TemplateProposal tp = (TemplateProposal) obj;
      return tp.getRelevance();
    }
    // catch all
    return 0;
  }

  private String getSortKey(ICompletionProposal p) {
    if (p instanceof AbstractDartCompletionProposal) {
      return ((AbstractDartCompletionProposal) p).getSortString();
    }
    return p.getDisplayString();
  }

}
