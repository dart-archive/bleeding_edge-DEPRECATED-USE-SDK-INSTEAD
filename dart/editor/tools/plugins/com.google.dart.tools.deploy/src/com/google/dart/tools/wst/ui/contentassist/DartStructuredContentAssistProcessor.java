/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.wst.ui.contentassist;

import com.google.dart.tools.ui.internal.text.dart.RelevanceSorter;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext;
import org.eclipse.wst.sse.ui.contentassist.StructuredContentAssistProcessor;

import java.util.Collections;
import java.util.List;

public class DartStructuredContentAssistProcessor extends StructuredContentAssistProcessor {

  private char[] completionPropoaslAutoActivationCharacters = new char[] {'.'};

  public DartStructuredContentAssistProcessor(ContentAssistant assistant, String partitionTypeID,
      ITextViewer viewer, IPreferenceStore preferenceStore) {
    super(assistant, partitionTypeID, viewer, preferenceStore);
  }

  @Override
  public char[] getCompletionProposalAutoActivationCharacters() {
    return completionPropoaslAutoActivationCharacters;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  protected List filterAndSortProposals(List proposals, IProgressMonitor monitor,
      CompletionProposalInvocationContext context) {
    RelevanceSorter sorter = new RelevanceSorter();
    sorter.beginSorting(null);
    Collections.sort(proposals, sorter);
    sorter.endSorting();
    return proposals;
  }

}
