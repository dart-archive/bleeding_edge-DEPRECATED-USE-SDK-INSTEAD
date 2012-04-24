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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.completion.CompletionProposal;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.functions.DartHeuristicScanner;
import com.google.dart.tools.ui.internal.text.functions.Symbols;
import com.google.dart.tools.ui.internal.util.DartModelUtil;
import com.google.dart.tools.ui.text.dart.CompletionProposalCollector;
import com.google.dart.tools.ui.text.dart.ContentAssistInvocationContext;
import com.google.dart.tools.ui.text.dart.DartContentAssistInvocationContext;
import com.google.dart.tools.ui.text.dart.IDartCompletionProposal;
import com.google.dart.tools.ui.text.editor.tmp.JavaScriptCore;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class DartTypeCompletionProposalComputer extends DartCompletionProposalComputer {
  /*
   * @see com.google.dart.tools.ui.internal.text.dart.DartCompletionProposalComputer#
   * computeCompletionProposals
   * (org.eclipse.jface.text.contentassist.TextContentAssistInvocationContext,
   * org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  public List computeCompletionProposals(ContentAssistInvocationContext context,
      IProgressMonitor monitor) {
    if (true) {
      return new ArrayList(); // TODO enable for type completions
    }
    List types = super.computeCompletionProposals(context, monitor);
    if (context instanceof DartContentAssistInvocationContext) {
      DartContentAssistInvocationContext javaContext = (DartContentAssistInvocationContext) context;
      try {
        if (types.size() > 0 && context.computeIdentifierPrefix().length() == 0) {
          Type expectedType = javaContext.getExpectedType();
          if (expectedType != null) {
            // empty prefix completion - insert LRU types if known, but prune if
            // they already occur in the core list

            // compute minmimum relevance and already proposed list
            int relevance = Integer.MAX_VALUE;
            Set proposed = new HashSet();
            for (Iterator it = types.iterator(); it.hasNext();) {
              AbstractDartCompletionProposal p = (AbstractDartCompletionProposal) it.next();
              DartElement element = p.getDartElement();
              if (element instanceof Type) {
                proposed.add(((Type) element).getElementName());
              }
              relevance = Math.min(relevance, p.getRelevance());
            }

            // insert history types
            List history = DartToolsPlugin.getDefault().getContentAssistHistory().getHistory(
                expectedType.getElementName()).getTypes();
            relevance -= history.size() + 1;
            for (Iterator it = history.iterator(); it.hasNext();) {
              String type = (String) it.next();
              if (proposed.contains(type)) {
                continue;
              }

              IDartCompletionProposal proposal = createTypeProposal(relevance, type, javaContext);

              if (proposal != null) {
                types.add(proposal);
              }
              relevance++;
            }
          }
        }
      } catch (BadLocationException x) {
        // log & ignore
        DartToolsPlugin.log(x);
      } catch (DartModelException x) {
        // log & ignore
        DartToolsPlugin.log(x);
      }
    }
    return types;
  }

  /*
   * @see com.google.dart.tools.ui.internal.text.dart.DartCompletionProposalComputer#
   * createCollector (com.google.dart.tools.ui.text.dart.DartContentAssistInvocationContext)
   */
  @Override
  protected CompletionProposalCollector createCollector(DartContentAssistInvocationContext context) {
    CompletionProposalCollector collector = super.createCollector(context);
    collector.setIgnored(CompletionProposal.ANONYMOUS_CLASS_DECLARATION, true);
    collector.setIgnored(CompletionProposal.FIELD_REF, true);
    collector.setIgnored(CompletionProposal.KEYWORD, true);
    collector.setIgnored(CompletionProposal.LABEL_REF, true);
    collector.setIgnored(CompletionProposal.LOCAL_VARIABLE_REF, true);
    collector.setIgnored(CompletionProposal.METHOD_DECLARATION, true);
    collector.setIgnored(CompletionProposal.METHOD_NAME_REFERENCE, true);
    collector.setIgnored(CompletionProposal.METHOD_REF, true);
    collector.setIgnored(CompletionProposal.PACKAGE_REF, true);
    collector.setIgnored(CompletionProposal.POTENTIAL_METHOD_DECLARATION, true);
    collector.setIgnored(CompletionProposal.VARIABLE_DECLARATION, true);

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

    // try the innermost scope of angle brackets that looks like a generic type
// argument list
    try {
      int pos = contextPosition - 1;
      do {
        int angle = scanner.findOpeningPeer(pos, bound, '<', '>');
        if (angle == DartHeuristicScanner.NOT_FOUND) {
          break;
        }
        int token = scanner.previousToken(angle - 1, bound);
        // next token must be a method name that is a generic type
        if (token == Symbols.TokenIDENT) {
          int off = scanner.getPosition() + 1;
          int end = angle;
          String ident = document.get(off, end - off).trim();
          if (DartHeuristicScanner.isGenericStarter(ident)) {
            return angle + 1;
          }
        }
        pos = angle - 1;
      } while (true);
    } catch (BadLocationException x) {
    }

    return super.guessContextInformationPosition(context);
  }

  private IDartCompletionProposal createTypeProposal(int relevance, String fullyQualifiedType,
      DartContentAssistInvocationContext context) throws DartModelException {
    Type type = DartModelUtil.findType(context.getCompilationUnit().getDartProject(),
        fullyQualifiedType);
    if (type == null) {
      return null;
    }

    CompletionProposal proposal = CompletionProposal.create(CompletionProposal.TYPE_REF,
        context.getInvocationOffset());
    proposal.setCompletion(fullyQualifiedType.toCharArray());
    proposal.setDeclarationSignature(type.getParent().getElementName().toCharArray());
//    proposal.setFlags(type.getFlags());
    proposal.setRelevance(relevance);
    proposal.setReplaceRange(context.getInvocationOffset(), context.getInvocationOffset());
    proposal.setSignature(fullyQualifiedType.toCharArray());

    if (shouldProposeGenerics(context.getProject())) {
      return new LazyGenericTypeProposal(proposal, context);
    } else {
      return new LazyDartTypeCompletionProposal(proposal, context);
    }
  }

  /**
   * Returns <code>true</code> if generic proposals should be allowed, <code>false</code> if not.
   * Note that even though code (in a library) may be referenced that uses generics, it is still
   * possible that the current source does not allow generics.
   * 
   * @param project the Java project
   * @return <code>true</code> if the generic proposals should be allowed, <code>false</code> if not
   */
  private final boolean shouldProposeGenerics(DartProject project) {
    String sourceVersion;
    if (project != null) {
      sourceVersion = project.getOption(JavaScriptCore.COMPILER_SOURCE, true);
    } else {
      sourceVersion = DartCore.getOption(JavaScriptCore.COMPILER_SOURCE);
    }

    return sourceVersion != null && JavaScriptCore.VERSION_1_5.compareTo(sourceVersion) <= 0;
  }

}
