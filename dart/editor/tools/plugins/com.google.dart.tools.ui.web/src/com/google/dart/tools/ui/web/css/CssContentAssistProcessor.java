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
package com.google.dart.tools.ui.web.css;

import com.google.dart.tools.ui.web.DartWebPlugin;
import com.google.dart.tools.ui.web.utils.CssAttributes;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.rules.IWordDetector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CssContentAssistProcessor implements IContentAssistProcessor {

  public CssContentAssistProcessor() {

  }

  @Override
  public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
    IDocument document = viewer.getDocument();

    String prefix = getValidPrefix(document, offset);
    String linePrefix = getLinePrefix(document, offset);

    if (linePrefix == null) {
      return null;
    }

    List<ICompletionProposal> completions = new ArrayList<ICompletionProposal>();

    boolean attributeCompletion = false;

    if (linePrefix != null) {
      if (linePrefix.indexOf(':') != -1) {
        String attribute = getWordBefore(linePrefix, linePrefix.indexOf(':'), new CssWordDetector());

        for (String keyword : CssAttributes.getAttributeValues(attribute)) {
          if (keyword.startsWith(prefix)) {
            completions.add(new CompletionProposal(
                keyword,
                offset - prefix.length(),
                prefix.length(),
                keyword.length(),
                DartWebPlugin.getImage("protected_co.gif"),
                null,
                null,
                null));
          }
        }

        attributeCompletion = true;
      }
    }

    if (!attributeCompletion) {
      for (String keyword : CssAttributes.getAttributes()) {
        if (keyword.startsWith(prefix)) {
          completions.add(new CompletionProposal(
              keyword,
              offset - prefix.length(),
              prefix.length(),
              keyword.length(),
              DartWebPlugin.getImage("protected_co.gif"),
              null,
              null,
              null));
        }
      }
    }

    Collections.sort(completions, new Comparator<ICompletionProposal>() {
      @Override
      public int compare(ICompletionProposal proposal1, ICompletionProposal proposal2) {
        return proposal1.getDisplayString().compareToIgnoreCase(proposal2.getDisplayString());
      }
    });

    return completions.toArray(new ICompletionProposal[completions.size()]);
  }

  @Override
  public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
    return null;
  }

  @Override
  public char[] getCompletionProposalAutoActivationCharacters() {
    return new char[] {};
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

  private String getLinePrefix(IDocument document, int offset) {
    try {
      IRegion lineInfo = document.getLineInformationOfOffset(offset);

      return document.get(lineInfo.getOffset(), offset - lineInfo.getOffset());
    } catch (BadLocationException ble) {
      return null;
    }
  }

  private String getValidPrefix(IDocument doc, int offset) {
    try {
      IRegion lineInfo = doc.getLineInformationOfOffset(offset);

      String line = doc.get(lineInfo.getOffset(), offset - lineInfo.getOffset());

      StringBuilder prefix = new StringBuilder();

      for (int i = line.length() - 1; i >= 0; i--) {
        char c = line.charAt(i);

        if (CssWordDetector.wordPart(c)) {
          prefix.insert(0, c);
        } else {
          return prefix.toString();
        }
      }

      return prefix.toString();
    } catch (BadLocationException ex) {
      return null;
    }
  }

  private String getWordBefore(String str, int index, IWordDetector wordDetector) {
    while (index >= 0 && !wordDetector.isWordPart(str.charAt(index))) {
      index--;
    }

    if (index < 0) {
      return "";
    }

    StringBuilder word = new StringBuilder();

    while (index >= 0 && wordDetector.isWordPart(str.charAt(index))) {
      word.insert(0, str.charAt(index));
      index--;
    }

    return word.toString();
  }

}
