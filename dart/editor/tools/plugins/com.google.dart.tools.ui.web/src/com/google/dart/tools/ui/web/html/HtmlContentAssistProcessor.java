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
package com.google.dart.tools.ui.web.html;

import com.google.dart.tools.ui.web.utils.HtmlKeywords;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

import java.util.ArrayList;
import java.util.List;

class HtmlContentAssistProcessor implements IContentAssistProcessor {

  public HtmlContentAssistProcessor() {

  }

  @Override
  public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
    String prefix = getValidPrefix(viewer, offset);

    if (prefix == null) {
      return null;
    }

    List<ICompletionProposal> completions = new ArrayList<ICompletionProposal>();

    for (String keyword : HtmlKeywords.getKeywords()) {
      if (keyword.startsWith(prefix)) {
        completions.add(new CompletionProposal(
            keyword,
            offset - prefix.length(),
            prefix.length(),
            keyword.length()));
      }
    }

    return completions.toArray(new ICompletionProposal[completions.size()]);
  }

  @Override
  public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
    return null;
  }

  @Override
  public char[] getCompletionProposalAutoActivationCharacters() {
    return new char[] {'<'};
  }

  @Override
  public char[] getContextInformationAutoActivationCharacters() {
    return null;
  }

  @Override
  public IContextInformationValidator getContextInformationValidator() {
    return null;
  }

  @Override
  public String getErrorMessage() {
    return null;
  }

  private String getValidPrefix(ITextViewer viewer, int offset) {
    try {
      IDocument doc = viewer.getDocument();

      IRegion lineInfo = doc.getLineInformationOfOffset(offset);

      String line = doc.get(lineInfo.getOffset(), offset - lineInfo.getOffset());

      StringBuilder prefix = new StringBuilder();

      for (int i = line.length() - 1; i >= 0; i--) {
        char c = line.charAt(i);

        if (c == '<') {
          return prefix.toString();
        } else if (Character.isJavaIdentifierPart(c)) {
          prefix.insert(0, c);
        } else {
          return null;
        }
      }

      return null;
    } catch (BadLocationException ex) {
      return null;
    }
  }
}
