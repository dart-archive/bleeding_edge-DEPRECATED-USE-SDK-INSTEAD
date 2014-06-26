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

package com.google.dart.server.internal.local.computer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.services.completion.CompletionEngine;
import com.google.dart.engine.services.completion.CompletionFactory;
import com.google.dart.engine.services.completion.CompletionProposal;
import com.google.dart.engine.services.completion.CompletionRequestor;
import com.google.dart.engine.services.completion.ProposalKind;
import com.google.dart.engine.services.util.DartDocUtilities;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.engine.utilities.translation.DartOmit;
import com.google.dart.server.CompletionSuggestion;
import com.google.dart.server.CompletionSuggestionKind;

import java.util.List;

/**
 * A computer for {@link CompletionSuggestion}s in a Dart {@link CompilationUnit}.
 * 
 * @coverage dart.server.local
 */
@DartOmit
public class DartUnitCompletionSuggestionsComputer {
  @DartOmit
  private static class CompletionRequestorConverter implements CompletionRequestor {
    private final List<CompletionSuggestion> suggestions;

    private CompletionRequestorConverter(List<CompletionSuggestion> suggestions) {
      this.suggestions = suggestions;
    }

    @Override
    public void accept(CompletionProposal proposal) {
      Element proposalElement = proposal.getElement();
      ProposalKind proposalKind = proposal.getKind();
      String elementDocSummary = DartDocUtilities.getTextSummary(null, proposalElement);
      String elementDocDetails = computeDocumentationComment(proposalElement);
      // TODO (jwren) API of CompletionSuggestionImpl has changed.
//      suggestions.add(new CompletionSuggestionImpl(
//          getKind(proposalKind),
//          elementDocSummary,
//          elementDocDetails,
//          proposal.getCompletion(),
//          proposal.getDeclaringType(),
//          proposal.getLocation(),
//          proposal.getParameterName(),
//          proposal.getParameterNames(),
//          proposal.getParameterType(),
//          proposal.getParameterTypes(),
//          proposal.getPositionalParameterCount(),
//          proposal.getRelevance(),
//          proposal.getReplacementLength(),
//          proposal.getReplacementLengthIdentifier(),
//          proposal.getReturnType(),
//          proposal.hasNamed(),
//          proposal.hasPositional(),
//          proposal.isDeprecated(),
//          proposal.isPotentialMatch()));
    }

    @Override
    public void beginReporting() {
    }

    @Override
    public void endReporting() {
    }
  }

  @VisibleForTesting
  public static String computeDocumentationComment(Element element) {
    if (element == null) {
      return null;
    }
    try {
      return element.computeDocumentationComment();
    } catch (Throwable e) {
      return null;
    }
  }

  private static CompletionSuggestionKind getKind(ProposalKind kind) {
    return CompletionSuggestionKind.valueOf(kind.name());
  }

  private final SearchEngine searchEngine;
  private final String analysisContextId;
  private final AnalysisContext analysisContext;
  private final Source source;
  private final CompilationUnit unit;

  private final int offset;

  public DartUnitCompletionSuggestionsComputer(SearchEngine searchEngine, String analysisContextId,
      AnalysisContext analysisContext, Source source, CompilationUnit unit, int offset) {
    this.searchEngine = searchEngine;
    this.analysisContext = analysisContext;
    this.analysisContextId = analysisContextId;
    this.source = source;
    this.unit = unit;
    this.offset = offset;
  }

  /**
   * Returns the computed {@link CompletionSuggestion}s, not {@code null}.
   */
  public CompletionSuggestion[] compute() {
    final List<CompletionSuggestion> suggestions = Lists.newArrayList();
    CompletionRequestor requestor = new CompletionRequestorConverter(suggestions);
    CompletionFactory completionFactory = new CompletionFactory();
    CompletionEngine engine = new CompletionEngine(requestor, completionFactory);
    //
    AssistContext assistContext = new AssistContext(
        searchEngine,
        analysisContext,
        analysisContextId,
        source,
        unit,
        new SourceRange(offset, 0));
    engine.complete(assistContext);
    return suggestions.toArray(new CompletionSuggestion[suggestions.size()]);
  }
}
