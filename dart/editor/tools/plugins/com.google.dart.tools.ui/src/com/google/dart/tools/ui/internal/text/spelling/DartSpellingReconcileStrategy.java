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
package com.google.dart.tools.ui.internal.text.spelling;

import com.google.dart.tools.ui.text.DartPartitions;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPartitioningException;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector;
import org.eclipse.ui.texteditor.spelling.SpellingProblem;
import org.eclipse.ui.texteditor.spelling.SpellingReconcileStrategy;
import org.eclipse.ui.texteditor.spelling.SpellingService;

/**
 * Reconcile strategy for spell checking comments.
 */
public class DartSpellingReconcileStrategy extends SpellingReconcileStrategy {

  /**
   * Spelling problem collector that forwards {@link SpellingProblem}s as
   * {@link org.eclipse.wst.jsdt.core.compiler.IProblem}s to the
   * {@link org.eclipse.wst.jsdt.core.compiler.IProblemRequestor}.
   */
  private class JSSpellingProblemCollector implements ISpellingProblemCollector {
    private ISpellingProblemCollector fParentCollector;

    public JSSpellingProblemCollector(ISpellingProblemCollector parentCollector) {
      fParentCollector = parentCollector;
    }

    /*
     * @see org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector#accept
     * (org.eclipse.ui.texteditor.spelling.SpellingProblem)
     */
    @Override
    public void accept(SpellingProblem problem) {
      try {
        String type = ((IDocumentExtension3) getDocument()).getPartition(
            fPartitioning,
            problem.getOffset(),
            false).getType();
        if (DartPartitions.DART_DOC.equals(type)
            || DartPartitions.DART_MULTI_LINE_COMMENT.equals(type)
            || DartPartitions.DART_SINGLE_LINE_COMMENT.equals(type)
            || DartPartitions.DART_SINGLE_LINE_DOC.equals(type)) {
          fParentCollector.accept(problem);
        }
      } catch (BadLocationException e) {
        fParentCollector.accept(problem);
      } catch (BadPartitioningException e) {
        fParentCollector.accept(problem);
      }
    }

    /*
     * @seeorg.eclipse.ui.texteditor.spelling.ISpellingProblemCollector# beginCollecting()
     */
    @Override
    public void beginCollecting() {
      fParentCollector.beginCollecting();
    }

    /*
     * @seeorg.eclipse.ui.texteditor.spelling.ISpellingProblemCollector# endCollecting()
     */
    @Override
    public void endCollecting() {
      fParentCollector.endCollecting();
    }
  }

  /** The id of the problem */
  public static final int SPELLING_PROBLEM_ID = 0x80000000;

  private String fPartitioning;

  public DartSpellingReconcileStrategy(ISourceViewer viewer, SpellingService spellingService,
      String partitioning) {
    super(viewer, spellingService);
    fPartitioning = partitioning;
  }

  @Override
  protected ISpellingProblemCollector createSpellingProblemCollector() {
    return new JSSpellingProblemCollector(super.createSpellingProblemCollector());
  }
}
