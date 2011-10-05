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
package com.google.dart.tools.ui.text.dart;

import org.eclipse.jface.text.contentassist.ICompletionProposal;

import java.util.Comparator;

/**
 * Abstract base class for sorters contributed to the
 * <code>org.eclipse.wst.jsdt.ui.javaCompletionProposalSorters</code> extension point.
 * <p>
 * Subclasses need to implement {@link #compare(ICompletionProposal, ICompletionProposal)} and may
 * override {@link #beginSorting(ContentAssistInvocationContext) beginSorting} and
 * {@link #endSorting() endSorting}.
 * </p>
 * <p>
 * The orderings imposed by a subclass need not be consistent with equals.
 * </p>
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractProposalSorter implements Comparator {

  /**
   * Creates a new sorter. Note that subclasses must provide a zero-argument constructor to be
   * instantiatable via
   * {@link org.eclipse.core.runtime.IConfigurationElement#createExecutableExtension(String)} .
   */
  protected AbstractProposalSorter() {
  }

  /**
   * Called once before sorting.
   * <p>
   * Clients may override, the default implementation does nothing.
   * </p>
   * 
   * @param context the context of the content assist invocation
   */
  public void beginSorting(ContentAssistInvocationContext context) {
  }

  /**
   * Implements the same contract as {@link Comparator#compare(Object, Object)} but with completion
   * proposals as parameters. This method will implement the {@link Comparator} interface if this
   * class is ever converted to extend <code>Comparator&lt;ICompletionProposal&gt;</code>.
   * <p>
   * The orderings imposed by an implementation need not be consistent with equals.
   * </p>
   * 
   * @param p1 the first proposal to be compared
   * @param p2 the second proposal to be compared
   * @return a negative integer, zero, or a positive integer as the first argument is less than,
   *         equal to, or greater than the second.
   */
  public abstract int compare(ICompletionProposal p1, ICompletionProposal p2);

  /**
   * This method delegates to {@link #compare(ICompletionProposal, ICompletionProposal)} and may be
   * removed if the class is ever converted to extend
   * <code>Comparator&lt;ICompletionProposal&gt;</code>.
   * 
   * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
   */
  @Override
  public final int compare(Object o1, Object o2) {
    ICompletionProposal p1 = (ICompletionProposal) o1;
    ICompletionProposal p2 = (ICompletionProposal) o2;

    return compare(p1, p2);
  }

  /**
   * Called once after sorting.
   * <p>
   * Clients may override, the default implementation does nothing.
   * </p>
   */
  public void endSorting() {
  }
}
