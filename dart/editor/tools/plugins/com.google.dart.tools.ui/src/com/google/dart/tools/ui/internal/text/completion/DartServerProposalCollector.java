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
import com.google.dart.tools.core.completion.DartSuggestionReceiver;
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

  private final int replacementOffset;
  private final int replacementLength;
  private final List<CompletionSuggestion> suggestions;
  private ArrayList<ICompletionProposal> proposals;

  public DartServerProposalCollector(DartSuggestionReceiver receiver) {
    this(
        receiver.getReplacementOffset(),
        receiver.getReplacementLength(),
        receiver.getSuggestions());
  }

  public DartServerProposalCollector(int replacementOffset, int replacementLength,
      List<CompletionSuggestion> suggestions) {
    this.replacementOffset = replacementOffset;
    this.replacementLength = replacementLength;
    this.suggestions = suggestions;
  }

  /**
   * Build the list of proposals from the supplied suggestions. This can be called on a background
   * thread so that code completion initialization does not block the UI.
   * 
   * @return {@code true} if proposals were computed, else {@code false}
   */
  public boolean computeAndSortProposals() {
    if (suggestions == null || suggestions.size() == 0) {
      return false;
    }
    proposals = new ArrayList<ICompletionProposal>();
    for (CompletionSuggestion suggestion : suggestions) {
      proposals.add(new DartServerProposal(this, suggestion));
    }
    Collections.sort(proposals, new CompletionProposalComparator());
    return true;
  }

  /**
   * Called on the UI thread to filter the proposals based upon the current document text.
   */
  public void filterProposals(IDocument document, int offset) {
    ArrayList<ICompletionProposal> filtered = new ArrayList<ICompletionProposal>();
    for (ICompletionProposal p : proposals) {
      if (((DartServerProposal) p).validate(document, offset, null)) {
        filtered.add(p);
      }
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
