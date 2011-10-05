/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.text.dart;

import com.google.dart.tools.ui.text.dart.AbstractProposalSorter;
import com.google.dart.tools.ui.text.dart.CompletionProposalComparator;

import org.eclipse.jface.text.contentassist.ICompletionProposal;

/**
 * A alphabetic proposal based sorter.
 */
public final class AlphabeticSorter extends AbstractProposalSorter {

  private final CompletionProposalComparator fComparator = new CompletionProposalComparator();

  public AlphabeticSorter() {
    fComparator.setOrderAlphabetically(true);
  }

  /*
   * @see com.google.dart.tools.ui.text.dart.AbstractProposalSorter#compare(org.eclipse
   * .jface.text.contentassist.ICompletionProposal,
   * org.eclipse.jface.text.contentassist.ICompletionProposal)
   */
  @Override
  public int compare(ICompletionProposal p1, ICompletionProposal p2) {
    return fComparator.compare(p1, p2);
  }

}
