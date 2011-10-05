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
package com.google.dart.tools.core.completion;

import com.google.dart.tools.core.problem.Problem;

/**
 * The abstract class <code>CompletionRequestor</code> defines the behavior common to objects that
 * represent a completion requestor which is passed completion proposals as they are generated in
 * response to a code assist request.
 * <p>
 * This class is intended to be subclassed by clients.
 * <p>
 * The code assist engine normally invokes methods on completion requestor in the following
 * sequence:
 * 
 * <pre>
 * requestor.beginReporting();
 * requestor.acceptContext(context);
 * requestor.accept(proposal_1);
 * requestor.accept(proposal_2);
 * ...
 * requestor.endReporting();
 * </pre>
 * If, however, the engine is unable to offer completion proposals for whatever reason,
 * <code>completionFailure</code> is called with a problem object describing why completions were
 * unavailable. In this case, the sequence of calls is:
 * 
 * <pre>
 * requestor.beginReporting();
 * requestor.acceptContext(context);
 * requestor.completionFailure(problem);
 * requestor.endReporting();
 * </pre>
 * In either case, the bracketing <code>beginReporting</code> <code>endReporting</code> calls are
 * always made as well as <code>acceptContext</code> call.
 */
public abstract class CompletionRequestor {
  /**
   * The set of CompletionProposal kinds that this requestor ignores; <code>0</code> means the set
   * is empty. 1 << completionProposalKind
   */
  private int ignoreSet = 0;

  private String[] favoriteReferences;

  /**
   * The set of CompletionProposal kinds that this requestor allows for required proposals;
   * <code>0</code> means the set is empty. 1 << completionProposalKind
   */
  private int requiredProposalAllowSet[] = null;

  private boolean requireExtendedContext = false;

  /**
   * Initialize a newly created completion requestor. The requestor is interested in all kinds of
   * completion proposals; none will be ignored. Calls to this constructor are identical to calls to
   * <code>CompletionRequestor(false)</code>
   */
  public CompletionRequestor() {
    this(false);
  }

  /**
   * Initialize a newly created completion requestor. If <code>ignoreAll</code> is <code>true</code>
   * the requestor is not interested in all kinds of completion proposals; all will be ignored. For
   * each kind of completion proposals that is of interest, <code>setIgnored(kind, false)</code>
   * must be called. If <code>ignoreAll</code> is <code>false</code> the requestor is interested in
   * all kinds of completion proposals; none will be ignored.
   * 
   * @param ignoreAll <code>true</code> to ignore all kinds of completion proposals, and
   *          <code>false</code> to propose all kinds
   */
  public CompletionRequestor(boolean ignoreAll) {
    ignoreSet = ignoreAll ? 0xffffffff : 0x00000000;
  }

  /**
   * Proposes a completion. Has no effect if the kind of proposal is being ignored by this
   * requestor. Callers should consider checking {@link #isIgnored(int)} before avoid creating
   * proposal objects that would only be ignored.
   * <p>
   * Similarly, implementers should check {@link #isIgnored(int) isIgnored(proposal.getKind())} and
   * ignore proposals that have been declared as uninteresting. The proposal object passed is only
   * valid for the duration of completion operation.
   * 
   * @param proposal the completion proposal
   * @throws IllegalArgumentException if the proposal is null
   */
  public abstract void accept(CompletionProposal proposal);

  /**
   * Propose the context in which the completion occurs.
   * <p>
   * This method is called one and only one time before any call to
   * {@link #accept(CompletionProposal)}. The default implementation of this method does nothing.
   * Clients may override.
   * 
   * @param context the completion context
   */
  public void acceptContext(CompletionContext context) {
    // do nothing
  }

  /**
   * Pro forma notification sent before reporting a batch of completion proposals.
   * <p>
   * The default implementation of this method does nothing. Clients may override.
   */
  public void beginReporting() {
    // do nothing
  }

  /**
   * Notification of failure to produce any completions. The problem object explains what prevented
   * completing.
   * <p>
   * The default implementation of this method does nothing. Clients may override to receive this
   * kind of notice.
   * 
   * @param problem the problem object
   */
  public void completionFailure(Problem problem) {
    // default behavior is to ignore
  }

  /**
   * Pro forma notification sent after reporting a batch of completion proposals.
   * <p>
   * The default implementation of this method does nothing. Clients may override.
   */
  public void endReporting() {
    // do nothing
  }

  /**
   * Return the favorite references which are used to compute some completion proposals.
   * <p>
   * A favorite reference is a qualified reference as it can be seen in an import statement.<br>
   * e.g. <code>{"java.util.Arrays"}</code><br>
   * It can be an on demand reference.<br>
   * e.g. <code>{"java.util.Arrays.*"}</code> It can be a reference to a static method or field (as
   * in a static import)<br>
   * e.g. <code>{"java.util.Arrays.equals"}</code>
   * <p>
   * Currently only on demand type references (<code>"java.util.Arrays.*"</code> ), references to a
   * static method or a static field are used to compute completion proposals. Other kind of
   * reference could be used in the future.
   * </p>
   * 
   * @return favorite imports
   */
  @Deprecated
  public String[] getFavoriteReferences() {
    return favoriteReferences;
  }

  /**
   * Return <code>true</code> if a proposal of a given kind with a required proposal of the given
   * kind is allowed.
   * 
   * @param proposalKind one of the kind constants declared
   * @param requiredProposalKind one of the kind constants declared on
   *          <code>CompletionProposal</code>
   * @return <code>true</code> if a proposal of a given kind with a required proposal of the given
   *         kind is allowed by this requestor, and <code>false</code> if it isn't of interest.
   *         <p>
   *         By default, all kinds of required proposals aren't allowed.
   * @see #setAllowsRequiredProposals(int, int, boolean)
   * @see CompletionProposal#getKind()
   * @see CompletionProposal#getRequiredProposals()
   */
  public boolean isAllowingRequiredProposals(int proposalKind, int requiredProposalKind) {
    if (proposalKind < CompletionProposal.FIRST_KIND || proposalKind > CompletionProposal.LAST_KIND) {
      throw new IllegalArgumentException(
          "Unknown kind of completion proposal: " + requiredProposalKind); //$NON-NLS-1$
    }

    if (requiredProposalKind < CompletionProposal.FIRST_KIND
        || requiredProposalKind > CompletionProposal.LAST_KIND) {
      throw new IllegalArgumentException(
          "Unknown required kind of completion proposal: " + requiredProposalKind); //$NON-NLS-1$
    }
    if (requiredProposalAllowSet == null) {
      return false;
    }

    return 0 != (requiredProposalAllowSet[proposalKind] & (1 << requiredProposalKind));
  }

  /**
   * Return <code>true</code> if this requestor requires an extended context. By default this method
   * return <code>false</code>.
   * 
   * @return <code>true</code> if this requestor requires an extended context.
   * @see CompletionContext#isExtended()
   */
  public boolean isExtendedContextRequired() {
    return requireExtendedContext;
  }

  /**
   * Return <code>true</code> if the given kind of completion proposal is ignored.
   * 
   * @param completionProposalKind one of the kind constants declared on
   *          <code>CompletionProposal</code>
   * @return <code>true</code> if the given kind of completion proposal is ignored by this
   *         requestor, and <code>false</code> if it is of interest
   * @see #setIgnored(int, boolean)
   * @see CompletionProposal#getKind()
   */
  public boolean isIgnored(int completionProposalKind) {
    if (completionProposalKind < CompletionProposal.FIRST_KIND
        || completionProposalKind > CompletionProposal.LAST_KIND) {
      throw new IllegalArgumentException(
          "Unknown kind of completion proposal: " + completionProposalKind); //$NON-NLS-1$
    }
    return 0 != (ignoreSet & (1 << completionProposalKind));
  }

  /**
   * Set whether a proposal of a given kind with a required proposal of the given kind is allowed.
   * <p>
   * A required proposal of a given kind is proposed even if {@link #isIgnored(int)} return
   * <code>true</code> for that kind.
   * <p>
   * Currently only a subset of kinds support required proposals. To see what combinations are
   * supported you must look at {@link CompletionProposal#getRequiredProposals()} documentation.
   * 
   * @param proposalKind one of the kind constants declared
   * @param requiredProposalKind one of the kind constants declared on
   *          <code>CompletionProposal</code>
   * @param allow <code>true</code> if a proposal of a given kind with a required proposal of the
   *          given kind is allowed by this requestor, and <code>false</code> if it isn't of
   *          interest
   * @see #isAllowingRequiredProposals(int, int)
   * @see CompletionProposal#getKind()
   * @see CompletionProposal#getRequiredProposals()
   */
  public void setAllowsRequiredProposals(int proposalKind, int requiredProposalKind, boolean allow) {
    if (proposalKind < CompletionProposal.FIRST_KIND || proposalKind > CompletionProposal.LAST_KIND) {
      throw new IllegalArgumentException(
          "Unknown kind of completion proposal: " + requiredProposalKind); //$NON-NLS-1$
    }
    if (requiredProposalKind < CompletionProposal.FIRST_KIND
        || requiredProposalKind > CompletionProposal.LAST_KIND) {
      throw new IllegalArgumentException(
          "Unknown required kind of completion proposal: " + requiredProposalKind); //$NON-NLS-1$
    }

    if (requiredProposalAllowSet == null) {
      requiredProposalAllowSet = new int[CompletionProposal.LAST_KIND + 1];
    }

    if (allow) {
      requiredProposalAllowSet[proposalKind] |= (1 << requiredProposalKind);
    } else {
      requiredProposalAllowSet[proposalKind] &= ~(1 << requiredProposalKind);
    }
  }

  /**
   * Set the favorite references which will be used to compute some completion proposals. A favorite
   * reference is a qualified reference as it can be seen in an import statement.<br>
   * 
   * @param favoriteImports
   * @see #getFavoriteReferences()
   */
  public void setFavoriteReferences(String[] favoriteImports) {
    favoriteReferences = favoriteImports;
  }

  /**
   * Sets whether the given kind of completion proposal is ignored.
   * 
   * @param completionProposalKind one of the kind constants declared on
   *          <code>CompletionProposal</code>
   * @param ignore <code>true</code> if the given kind of completion proposal is ignored by this
   *          requestor, and <code>false</code> if it is of interest
   * @see #isIgnored(int)
   * @see CompletionProposal#getKind()
   */
  public void setIgnored(int completionProposalKind, boolean ignore) {
    if (completionProposalKind < CompletionProposal.FIRST_KIND
        || completionProposalKind > CompletionProposal.LAST_KIND) {
      throw new IllegalArgumentException(
          "Unknown kind of completion proposal: " + completionProposalKind); //$NON-NLS-1$
    }
    if (ignore) {
      ignoreSet |= (1 << completionProposalKind);
    } else {
      ignoreSet &= ~(1 << completionProposalKind);
    }
  }

  /**
   * Set whether this requestor requires an extended context.
   * 
   * @param require <code>true</code> if this requestor requires an extended context
   * @see CompletionContext#isExtended()
   */
  public void setRequireExtendedContext(boolean require) {
    requireExtendedContext = require;
  }
}
