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
package com.google.dart.tools.ui.internal.text.dart;

import com.google.dart.server.AnalysisServer;
import com.google.dart.server.generated.types.CompletionSuggestion;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.internal.text.completion.DartServerProposalCollector;
import com.google.dart.tools.ui.text.dart.IDartCompletionProposal;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension3;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension6;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

/**
 * {@link DartServerProposal} represents a code completion suggestion returned by
 * {@link AnalysisServer}.
 */
public class DartServerProposal implements ICompletionProposal, ICompletionProposalExtension,
    ICompletionProposalExtension2, ICompletionProposalExtension3, ICompletionProposalExtension4,
    ICompletionProposalExtension5, ICompletionProposalExtension6, IDartCompletionProposal {

  private final DartServerProposalCollector collector;
  private final CompletionSuggestion suggestion;
  private final int relevance;
  private final StyledString styledCompletion;

  public DartServerProposal(DartServerProposalCollector collector, CompletionSuggestion suggestion) {
    this.collector = collector;
    this.suggestion = suggestion;
    this.relevance = computeRelevance();
    this.styledCompletion = computeStyledDisplayString();
  }

  @Override
  public void apply(IDocument document) {
    // not used
  }

  @Override
  public void apply(IDocument document, char trigger, int offset) {
    // not used
  }

  @Override
  public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
    int length = offset - collector.getReplacementOffset();
    try {
      viewer.getDocument().replace(collector.getReplacementOffset(), length, getCompletion());
    } catch (BadLocationException e) {
      DartCore.logInformation("Failed to replace offset:" + collector.getReplacementOffset()
          + " length:" + length + " with:" + getCompletion(), e);
    }
  }

  @Override
  public String getAdditionalProposalInfo() {
    // getAdditionalProposalInfo(IProgressMonitor monitor) is called instead of this method.
    return null;
  }

  @Override
  public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
    //TODO (danrubel): determine if additional information is needed and supply it
    return null;
  }

  @Override
  public IContextInformation getContextInformation() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getContextInformationPosition() {
    return collector.getReplacementOffset() + collector.getReplacementLength();
  }

  @Override
  public String getDisplayString() {
    // this method is used for alphabetic sorting,
    // while getStyledDisplayString() is displayed to the user.
    return getCompletion();
  }

  @Override
  public Image getImage() {
    //TODO (danrubel): compute image based upon type of suggestion
    return null;
  }

  @Override
  public IInformationControlCreator getInformationControlCreator() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getPrefixCompletionStart(IDocument document, int completionOffset) {
    return collector.getReplacementOffset();
  }

  @Override
  public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
    int length = Math.max(0, completionOffset - collector.getReplacementOffset());
    return getCompletion().substring(0, length);
  }

  @Override
  public int getRelevance() {
    return relevance;
  }

  @Override
  public Point getSelection(IDocument document) {
    return new Point(collector.getReplacementOffset() + getCompletion().length(), 0);
  }

  @Override
  public StyledString getStyledDisplayString() {
    return styledCompletion;
  }

  @Override
  public char[] getTriggerCharacters() {
    return null;
  }

  @Override
  public boolean isAutoInsertable() {
    return false;
  }

  @Override
  public boolean isValidFor(IDocument document, int offset) {
    // replaced by validate(IDocument, int, event)
    return true;
  }

  @Override
  public void selected(ITextViewer viewer, boolean smartToggle) {
    // called when the proposal is selected
  }

  @Override
  public void unselected(ITextViewer viewer) {
    // called when the proposal is unselected
  }

  @Override
  public boolean validate(IDocument document, int offset, DocumentEvent event) {
    //TODO (danrubel): hook up camel case filtering
    int replacementOffset = collector.getReplacementOffset();
    if (offset < replacementOffset) {
      return false;
    }
    try {
      return getDisplayString().startsWith(
          document.get(replacementOffset, offset - replacementOffset));
    } catch (BadLocationException x) {
      return false;
    }
  }

  private int computeRelevance() {
    String relevance = suggestion.getRelevance();
    if (relevance == "HIGH") {
      return 0;
    } else if (relevance == "LOW") {
      return 2;
    } else { // DEFAULT
      return 1;
    }
  }

  private StyledString computeStyledDisplayString() {
    StringBuilder builder = new StringBuilder();
    builder.append(getCompletion());
    String declaringType = suggestion.getDeclaringType();
    if (declaringType != null && declaringType.length() > 0) {
      builder.append(" - ");
      builder.append(declaringType);
    }
    return new StyledString(builder.toString());
  }

  private String getCompletion() {
    return suggestion.getCompletion();
  }
}
