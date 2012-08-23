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

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.ui.DartContextType;
import com.google.dart.tools.ui.DartDocContextType;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartX;
import com.google.dart.tools.ui.TemplateEngine;
import com.google.dart.tools.ui.text.DartPartitions;
import com.google.dart.tools.ui.text.dart.ContentAssistInvocationContext;
import com.google.dart.tools.ui.text.dart.DartContentAssistInvocationContext;
import com.google.dart.tools.ui.text.dart.IDartCompletionProposal;
import com.google.dart.tools.ui.text.dart.IDartCompletionProposalComputer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateProposal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 *
 */
public final class TemplateCompletionProposalComputer implements IDartCompletionProposalComputer {

  private final TemplateEngine fJavaTemplateEngine;
  private final TemplateEngine fJavadocTemplateEngine;

  private static final Set<String> KEYWORDS;

  static {
    // TODO(brianwilkerson) This is the wrong set of keywords for Dart.
    Set<String> keywords = new HashSet<String>(42);
    keywords.add("abstract"); //$NON-NLS-1$
    keywords.add("assert"); //$NON-NLS-1$
    keywords.add("break"); //$NON-NLS-1$
    keywords.add("case"); //$NON-NLS-1$
    keywords.add("catch"); //$NON-NLS-1$
    keywords.add("class"); //$NON-NLS-1$
    keywords.add("continue"); //$NON-NLS-1$
    keywords.add("default"); //$NON-NLS-1$
    keywords.add("do"); //$NON-NLS-1$
    keywords.add("else"); //$NON-NLS-1$
    keywords.add("elseif"); //$NON-NLS-1$
    keywords.add("extends"); //$NON-NLS-1$
    keywords.add("final"); //$NON-NLS-1$
    keywords.add("finally"); //$NON-NLS-1$
    keywords.add("for"); //$NON-NLS-1$
    keywords.add("if"); //$NON-NLS-1$
    keywords.add("implements"); //$NON-NLS-1$
    keywords.add("import"); //$NON-NLS-1$
    keywords.add("instanceof"); //$NON-NLS-1$
    keywords.add("interface"); //$NON-NLS-1$
    keywords.add("native"); //$NON-NLS-1$
    keywords.add("new"); //$NON-NLS-1$
    keywords.add("package"); //$NON-NLS-1$
    keywords.add("private"); //$NON-NLS-1$
    keywords.add("protected"); //$NON-NLS-1$
    keywords.add("public"); //$NON-NLS-1$
    keywords.add("return"); //$NON-NLS-1$
    keywords.add("static"); //$NON-NLS-1$
    keywords.add("strictfp"); //$NON-NLS-1$
    keywords.add("super"); //$NON-NLS-1$
    keywords.add("switch"); //$NON-NLS-1$
    keywords.add("synchronized"); //$NON-NLS-1$
    keywords.add("this"); //$NON-NLS-1$
    keywords.add("throw"); //$NON-NLS-1$
    keywords.add("throws"); //$NON-NLS-1$
    keywords.add("transient"); //$NON-NLS-1$
    keywords.add("try"); //$NON-NLS-1$
    keywords.add("volatile"); //$NON-NLS-1$
    keywords.add("while"); //$NON-NLS-1$
    keywords.add("true"); //$NON-NLS-1$
    keywords.add("false"); //$NON-NLS-1$
    keywords.add("null"); //$NON-NLS-1$
    KEYWORDS = Collections.unmodifiableSet(keywords);
  }

  public TemplateCompletionProposalComputer() {
    TemplateContextType contextType = DartToolsPlugin.getDefault().getTemplateContextRegistry().getContextType(
        DartContextType.NAME);
    if (contextType == null) {
      contextType = new DartContextType();
      DartToolsPlugin.getDefault().getTemplateContextRegistry().addContextType(contextType);
    }

    fJavaTemplateEngine = new TemplateEngine(contextType);

    contextType = DartToolsPlugin.getDefault().getTemplateContextRegistry().getContextType(
        "javadoc"); //$NON-NLS-1$

    if (contextType == null) {
      contextType = new DartDocContextType();
      DartToolsPlugin.getDefault().getTemplateContextRegistry().addContextType(contextType);
    }

    fJavadocTemplateEngine = new TemplateEngine(contextType);
  }

  /*
   * @see org.eclipse.jface.text.contentassist.ICompletionProposalComputer#
   * computeCompletionProposals
   * (org.eclipse.jface.text.contentassist.TextContentAssistInvocationContext,
   * org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  public List<ICompletionProposal> computeCompletionProposals(
      ContentAssistInvocationContext context, IProgressMonitor monitor) {
    TemplateEngine engine;
    try {
      String partition = TextUtilities.getContentType(
          context.getDocument(),
          DartPartitions.DART_PARTITIONING,
          context.getInvocationOffset(),
          true);
      if (partition.equals(DartPartitions.DART_DOC)
          || partition.equals(DartPartitions.DART_SINGLE_LINE_DOC)) {
        engine = fJavadocTemplateEngine;
      } else {
        engine = fJavaTemplateEngine;
      }
    } catch (BadLocationException x) {
      return Collections.emptyList();
    }

    if (engine != null) {
      if (!(context instanceof DartContentAssistInvocationContext)) {
        return Collections.emptyList();
      }

      DartContentAssistInvocationContext javaContext = (DartContentAssistInvocationContext) context;
      CompilationUnit unit = javaContext.getCompilationUnit();
      if (unit == null) {
        return Collections.emptyList();
      }

      engine.reset();
      engine.complete(javaContext.getViewer(), javaContext.getInvocationOffset(), unit);

      ICompletionProposal[] templateProposals = engine.getResults();
      List<ICompletionProposal> result = new ArrayList<ICompletionProposal>(
          Arrays.asList(templateProposals));

      IDartCompletionProposal[] keyWordResults = javaContext.getKeywordProposals();
      if (keyWordResults.length > 0) {
        List<TemplateProposal> removals = new ArrayList<TemplateProposal>();

        // update relevance of template proposals that match with a keyword
        // give those templates slightly more relevance than the keyword to
        // sort them first
        // remove keyword templates that don't have an equivalent
        // keyword proposal
        // TODO(devoncarew): commenting out to fix compilation errors
        DartX.todo();
//        if (keyWordResults.length > 0) {
//          outer : for (int k = 0; k < templateProposals.length; k++) {
//            TemplateProposal curr = templateProposals[k];
//            String name = curr.getTemplate().getName();
//            for (int i = 0; i < keyWordResults.length; i++) {
//              String keyword = keyWordResults[i].getDisplayString();
//              if (name.startsWith(keyword)) {
//                curr.setRelevance(keyWordResults[i].getRelevance() + 1);
//                continue outer;
//              }
//            }
//            if (isKeyword(name))
//              removals.add(curr);
//          }
//        }

        result.removeAll(removals);
      }
      return result;
    }

    return Collections.emptyList();
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<IContextInformation> computeContextInformation(
      ContentAssistInvocationContext context, IProgressMonitor monitor) {
    return Collections.EMPTY_LIST;
  }

  /*
   * @see org.eclipse.jface.text.contentassist.ICompletionProposalComputer# getErrorMessage()
   */
  @Override
  public String getErrorMessage() {
    return null;
  }

  /*
   * @see com.google.dart.tools.ui.text.dart.IDartCompletionProposalComputer#sessionEnded ()
   */
  @Override
  public void sessionEnded() {
    fJavadocTemplateEngine.reset();
    fJavaTemplateEngine.reset();
  }

  /*
   * @see com.google.dart.tools.ui.text.dart.IDartCompletionProposalComputer# sessionStarted()
   */
  @Override
  public void sessionStarted() {
  }

  @SuppressWarnings("unused")
  private boolean isKeyword(String name) {
    return KEYWORDS.contains(name);
  }
}
