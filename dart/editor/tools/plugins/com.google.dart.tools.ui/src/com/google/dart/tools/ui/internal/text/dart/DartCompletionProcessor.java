/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.text.dart;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.text.dart.ContentAssistInvocationContext;
import com.google.dart.tools.ui.text.dart.DartContentAssistInvocationContext;
import com.google.dart.tools.ui.text.editor.tmp.JavaScriptCore;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.ui.IEditorPart;

import java.util.HashMap;
import java.util.List;

/**
 * Java completion processor.
 */
public class DartCompletionProcessor extends ContentAssistProcessor {

  private final static String VISIBILITY = JavaScriptCore.CODEASSIST_VISIBILITY_CHECK;
  private final static String ENABLED = "enabled"; //$NON-NLS-1$
  private final static String DISABLED = "disabled"; //$NON-NLS-1$

  private IContextInformationValidator fValidator;
  protected final IEditorPart fEditor;

  public DartCompletionProcessor(IEditorPart editor, ContentAssistant assistant, String partition) {
    super(assistant, partition);
    fEditor = editor;
  }

  /*
   * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#
   * getContextInformationValidator()
   */
  @Override
  public IContextInformationValidator getContextInformationValidator() {
    if (fValidator == null) {
      fValidator = new DartParameterListValidator();
    }
    return fValidator;
  }

  /**
   * Tells this processor to restrict is proposals to those starting with matching cases.
   * 
   * @param restrict <code>true</code> if proposals should be restricted
   */
  public void restrictProposalsToMatchingCases(boolean restrict) {
    // not yet supported
  }

  /**
   * Tells this processor to restrict its proposal to those element visible in the actual invocation
   * context.
   * 
   * @param restrict <code>true</code> if proposals should be restricted
   */
  public void restrictProposalsToVisibility(boolean restrict) {
    HashMap<String, String> options = DartCore.getOptions();
    Object value = options.get(VISIBILITY);
    if (value instanceof String) {
      String newValue = restrict ? ENABLED : DISABLED;
      if (!newValue.equals(value)) {
        options.put(VISIBILITY, newValue);
        DartCore.setOptions(options);
      }
    }
  }

  /*
   * @see com.google.dart.tools.ui.internal.text.dart.ContentAssistProcessor# createContext
   * (org.eclipse.jface.text.ITextViewer, int)
   */
  @Override
  protected ContentAssistInvocationContext createContext(ITextViewer viewer, int offset) {
    return new DartContentAssistInvocationContext(viewer, offset, fEditor);
  }

  /*
   * @see com.google.dart.tools.ui.internal.text.dart.ContentAssistProcessor# filterAndSort
   * (java.util.List, org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  protected List filterAndSortProposals(List proposals, IProgressMonitor monitor,
      ContentAssistInvocationContext context) {
    ProposalSorterRegistry.getDefault().getCurrentSorter().sortProposals(context, proposals);
    return proposals;
  }
}
