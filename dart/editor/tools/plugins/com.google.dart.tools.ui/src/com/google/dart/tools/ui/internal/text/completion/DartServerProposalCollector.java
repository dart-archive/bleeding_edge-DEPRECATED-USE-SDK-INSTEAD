/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.tools.ui.internal.text.completion;

import com.google.dart.server.generated.types.CompletionSuggestion;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.completion.DartSuggestionReceiver;
import com.google.dart.tools.ui.internal.text.dart.DartServerInformationalProposal;
import com.google.dart.tools.ui.internal.text.dart.DartServerProposal;
import com.google.dart.tools.ui.text.dart.CompletionProposalComparator;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The {@link DartServerProposalCollector} consumes information received from the server by the
 * {@link DartSuggestionReceiver} and produces a collection of completion proposals.
 */
public class DartServerProposalCollector {

  private final boolean auto;
  private final int replacementOffset;
  private final int replacementLength;
  private final List<CompletionSuggestion> suggestions;
  private final boolean isComplete;
  private ArrayList<ICompletionProposal> proposals;

  public DartServerProposalCollector(boolean auto, DartSuggestionReceiver receiver) {
    this(
        auto,
        receiver.getReplacementOffset(),
        receiver.getReplacementLength(),
        receiver.getSuggestions(),
        receiver.isComplete());
  }

  public DartServerProposalCollector(boolean auto, int replacementOffset, int replacementLength,
      List<CompletionSuggestion> suggestions, boolean isComplete) {
    this.auto = auto;
    this.replacementOffset = replacementOffset;
    this.replacementLength = replacementLength;
    this.suggestions = suggestions;
    this.isComplete = isComplete;
  }

  /**
   * Build the list of proposals from the supplied suggestions. This can be called on a background
   * thread so that code completion initialization does not block the UI.
   * 
   * @return {@code true} if proposals should be displayed, else {@code false}
   */
  public boolean computeAndSortProposals() {
    proposals = new ArrayList<ICompletionProposal>();
    if (!DartCore.getAnalysisServer().isSocketOpen()) {
      proposals.add(DartServerInformationalProposal.SERVER_DEAD);
      return !auto; // Do not display message if auto '.' triggered the completion request
    }
    if (suggestions == null) {
      proposals.add(DartServerInformationalProposal.NO_RESPONSE);
      return !auto;
    }
    if (suggestions.size() == 0) {
      proposals.add(isComplete ? DartServerInformationalProposal.NO_RESULTS_COMPLETE
          : DartServerInformationalProposal.NO_RESULTS_TIMED_OUT);
      return !auto;
    }
    for (CompletionSuggestion suggestion : suggestions) {
      proposals.add(new DartServerProposal(this, suggestion));
    }
    Collections.sort(proposals, new CompletionProposalComparator());
    if (!isComplete) {
      proposals.add(Math.min(proposals.size(), 9), DartServerInformationalProposal.PARTIAL_RESULTS);
    }
    return true;
  }

  /**
   * Called on the UI thread to filter the proposals based upon the current document text.
   */
  public void filterProposals(IDocument document, int offset) {
    ArrayList<ICompletionProposal> filtered = new ArrayList<ICompletionProposal>();
    ArrayList<ICompletionProposal> informational = new ArrayList<ICompletionProposal>();
    for (ICompletionProposal p : proposals) {
      if (p instanceof DartServerProposal
          && ((DartServerProposal) p).validate(document, offset, null)) {
        filtered.add(p);
      } else if (p instanceof DartServerInformationalProposal) {
        informational.add(p);
      }
      filtered.addAll(Math.min(filtered.size(), 9), informational);
    }
    if (filtered.isEmpty()) {
      filtered.add(DartServerInformationalProposal.NO_RESULTS_COMPLETE);
    }
    proposals = filtered;
  }

  /**
   * Return the proposals or {@code null} if {@link #computeAndSortProposals()} has not been called
   * or was called but returned {@code false}.
   */
  public List<ICompletionProposal> getProposals() {
    return proposals;
  }

  public int getReplacementLength() {
    return replacementLength;
  }

  public int getReplacementOffset() {
    return replacementOffset;
  }
}
