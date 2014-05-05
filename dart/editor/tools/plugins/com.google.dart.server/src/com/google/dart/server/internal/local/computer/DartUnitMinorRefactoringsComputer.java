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

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.services.correction.CorrectionProcessors;
import com.google.dart.engine.services.correction.CorrectionProposal;
import com.google.dart.engine.services.correction.QuickAssistProcessor;
import com.google.dart.engine.source.Source;
import com.google.dart.server.MinorRefactoringsConsumer;

/**
 * A computer for {@link CorrectionProposal}s in a Dart {@link CompilationUnit}.
 * 
 * @coverage dart.server.local
 */
public class DartUnitMinorRefactoringsComputer {
  private final SearchEngine searchEngine;
  private final String contextId;
  private final AnalysisContext context;
  private final Source source;
  private final CompilationUnit unit;
  private final int offset;
  private final int length;
  private final MinorRefactoringsConsumer consumer;

  public DartUnitMinorRefactoringsComputer(SearchEngine searchEngine, String contextId,
      AnalysisContext context, Source source, CompilationUnit unit, int offset, int length,
      MinorRefactoringsConsumer consumer) {
    this.searchEngine = searchEngine;
    this.contextId = contextId;
    this.context = context;
    this.source = source;
    this.unit = unit;
    this.offset = offset;
    this.length = length;
    this.consumer = consumer;
  }

  /**
   * Computes {@link CorrectionProposal}s and notifies {@link #consumer}.
   */
  public void compute() throws Exception {
    QuickAssistProcessor processor = CorrectionProcessors.getQuickAssistProcessor();
    AssistContext assistContext = new AssistContext(
        searchEngine,
        context,
        contextId,
        source,
        unit,
        offset,
        length);
    CorrectionProposal[] proposals = processor.getProposals(assistContext);
    consumer.computedProposals(proposals, false);
  }
}
