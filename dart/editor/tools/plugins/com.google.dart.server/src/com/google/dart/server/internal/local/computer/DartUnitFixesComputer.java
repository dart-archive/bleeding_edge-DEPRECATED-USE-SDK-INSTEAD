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

import com.google.common.collect.Maps;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.services.correction.CorrectionProcessors;
import com.google.dart.engine.services.correction.CorrectionProposal;
import com.google.dart.engine.services.correction.QuickFixProcessor;
import com.google.dart.engine.utilities.translation.DartOmit;
import com.google.dart.server.FixesConsumer;

import java.util.Map;

/**
 * A computer {@link AnalysisError}s fixes in a Dart {@link CompilationUnit}.
 * 
 * @coverage dart.server.local
 */
@DartOmit
public class DartUnitFixesComputer {
  /**
   * Returns {@link ErrorCode}s for which this processor may compute fixes.
   */
  public static ErrorCode[] getFixableErrorCodes() {
    QuickFixProcessor processor = CorrectionProcessors.getQuickFixProcessor();
    return processor.getFixableErrorCodes();
  }

  private final SearchEngine searchEngine;
  private final String contextId;
  private final AnalysisContext context;
  private final CompilationUnit unit;
  private final AnalysisError error;
  private final FixesConsumer consumer;

  public DartUnitFixesComputer(SearchEngine searchEngine, String contextId,
      AnalysisContext context, CompilationUnit unit, AnalysisError error, FixesConsumer consumer) {
    this.searchEngine = searchEngine;
    this.contextId = contextId;
    this.context = context;
    this.unit = unit;
    this.error = error;
    this.consumer = consumer;
  }

  /**
   * Computes fixes and notifies {@link #consumer}.
   */
  public void compute() throws Exception {
    AssistContext assistContext = new AssistContext(
        searchEngine,
        context,
        contextId,
        error.getSource(),
        unit,
        error.getOffset(),
        error.getLength());
    QuickFixProcessor processor = CorrectionProcessors.getQuickFixProcessor();
    CorrectionProposal[] proposals = processor.computeProposals(assistContext, error);
    Map<AnalysisError, CorrectionProposal[]> fixesMap = Maps.newHashMap();
    fixesMap.put(error, proposals);
    // API change, the "AnalysisError" is generated above is the wrong "AnalysisError"
    //consumer.computedFixes(fixesMap, false);
  }
}
