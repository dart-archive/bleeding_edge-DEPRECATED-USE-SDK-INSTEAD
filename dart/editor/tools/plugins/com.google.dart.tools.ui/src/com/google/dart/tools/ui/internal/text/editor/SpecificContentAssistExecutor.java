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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.tools.ui.internal.text.dart.CompletionProposalCategory;
import com.google.dart.tools.ui.internal.text.dart.CompletionProposalComputerRegistry;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.ITextEditor;

import java.util.Collection;
import java.util.Iterator;

/**
 * A content assist executor can invoke content assist for a specific proposal category on an
 * editor.
 */
public final class SpecificContentAssistExecutor {

  private final CompletionProposalComputerRegistry fRegistry;

  /**
   * Creates a new executor.
   * 
   * @param registry the computer registry to use for the enablement of proposal categories
   */
  public SpecificContentAssistExecutor(CompletionProposalComputerRegistry registry) {
    Assert.isNotNull(registry);
    fRegistry = registry;
  }

  /**
   * Invokes content assist on <code>editor</code>, showing only proposals computed by the
   * <code>CompletionProposalCategory</code> with the given <code>categoryId</code>.
   * 
   * @param editor the editor to invoke code assist on
   * @param categoryId the id of the proposal category to show proposals for
   */
  public void invokeContentAssist(final ITextEditor editor, String categoryId) {
    @SuppressWarnings("unchecked")
    Collection<CompletionProposalCategory> categories = fRegistry.getProposalCategories();
    boolean[] inclusionState = new boolean[categories.size()];
    boolean[] separateState = new boolean[categories.size()];
    int i = 0;
    for (Iterator<CompletionProposalCategory> it = categories.iterator(); it.hasNext(); i++) {
      CompletionProposalCategory cat = it.next();
      inclusionState[i] = cat.isIncluded();
      cat.setIncluded(cat.getId().equals(categoryId));
      separateState[i] = cat.isSeparateCommand();
      cat.setSeparateCommand(false);
    }

    try {
      ITextOperationTarget target = (ITextOperationTarget) editor.getAdapter(ITextOperationTarget.class);
      if (target != null && target.canDoOperation(ISourceViewer.CONTENTASSIST_PROPOSALS)) {
        target.doOperation(ISourceViewer.CONTENTASSIST_PROPOSALS);
      }
    } finally {
      i = 0;
      for (Iterator<CompletionProposalCategory> it = categories.iterator(); it.hasNext(); i++) {
        CompletionProposalCategory cat = it.next();
        cat.setIncluded(inclusionState[i]);
        cat.setSeparateCommand(separateState[i]);
      }
    }
  }
}
