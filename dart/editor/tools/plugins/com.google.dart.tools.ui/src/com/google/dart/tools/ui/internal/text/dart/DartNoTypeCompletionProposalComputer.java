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
package com.google.dart.tools.ui.internal.text.dart;

import com.google.dart.tools.core.completion.CompletionProposal;
import com.google.dart.tools.ui.internal.text.functions.DartHeuristicScanner;
import com.google.dart.tools.ui.internal.text.functions.Symbols;
import com.google.dart.tools.ui.text.dart.CompletionProposalCollector;
import com.google.dart.tools.ui.text.dart.ContentAssistInvocationContext;
import com.google.dart.tools.ui.text.dart.DartContentAssistInvocationContext;

import org.eclipse.jface.text.IDocument;

public class DartNoTypeCompletionProposalComputer extends DartCompletionProposalComputer {

  /*
   * @see com.google.dart.tools.ui.internal.text.dart.DartCompletionProposalComputer#
   * createCollector (com.google.dart.tools.ui.text.dart.DartContentAssistInvocationContext)
   */
  @Override
  protected CompletionProposalCollector createCollector(DartContentAssistInvocationContext context) {
    CompletionProposalCollector collector = super.createCollector(context);
    collector.setIgnored(CompletionProposal.ANONYMOUS_CLASS_DECLARATION, true);
    collector.setIgnored(CompletionProposal.FIELD_REF, false);
    collector.setIgnored(CompletionProposal.KEYWORD, false);
    collector.setIgnored(CompletionProposal.LABEL_REF, false);
    collector.setIgnored(CompletionProposal.LOCAL_VARIABLE_REF, false);
    collector.setIgnored(CompletionProposal.METHOD_DECLARATION, false);
    collector.setIgnored(CompletionProposal.METHOD_NAME_REFERENCE, false);
    collector.setIgnored(CompletionProposal.METHOD_REF, false);
    collector.setIgnored(CompletionProposal.LIBRARY_PREFIX, false);
    collector.setIgnored(CompletionProposal.POTENTIAL_METHOD_DECLARATION, false);
    collector.setIgnored(CompletionProposal.VARIABLE_DECLARATION, false);

    collector.setIgnored(CompletionProposal.JAVADOC_BLOCK_TAG, true);
    collector.setIgnored(CompletionProposal.JAVADOC_FIELD_REF, true);
    collector.setIgnored(CompletionProposal.JAVADOC_INLINE_TAG, true);
    collector.setIgnored(CompletionProposal.JAVADOC_METHOD_REF, true);
    collector.setIgnored(CompletionProposal.JAVADOC_PARAM_REF, true);
    collector.setIgnored(CompletionProposal.JAVADOC_TYPE_REF, true);

    collector.setIgnored(CompletionProposal.TYPE_REF, false);
    return collector;
  }

  @Override
  protected int guessContextInformationPosition(ContentAssistInvocationContext context) {
    final int contextPosition = context.getInvocationOffset();

    IDocument document = context.getDocument();
    DartHeuristicScanner scanner = new DartHeuristicScanner(document);
    int bound = Math.max(-1, contextPosition - 200);

    // try the innermost scope of parentheses that looks like a method call
    int pos = contextPosition - 1;
    do {
      int paren = scanner.findOpeningPeer(pos, bound, '(', ')');
      if (paren == DartHeuristicScanner.NOT_FOUND) {
        break;
      }
      int token = scanner.previousToken(paren - 1, bound);
      // next token must be a method name (identifier) or the closing angle of a
      // constructor call of a parameterized type.
      if (token == Symbols.TokenIDENT || token == Symbols.TokenGREATERTHAN) {
        return paren + 1;
      }
      pos = paren - 1;
    } while (true);

    return super.guessContextInformationPosition(context);
  }
}
