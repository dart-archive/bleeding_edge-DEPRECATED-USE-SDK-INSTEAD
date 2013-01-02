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

import com.google.dart.tools.core.html.HtmlKeywords;
import com.google.dart.tools.core.html.XmlAttribute;
import com.google.dart.tools.core.html.XmlDocument;
import com.google.dart.tools.core.html.XmlElement;
import com.google.dart.tools.core.html.XmlNode;
import com.google.dart.tools.ui.web.DartWebPlugin;
import com.google.dart.tools.ui.web.utils.WordDetector;

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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class HtmlContentAssistProcessor implements IContentAssistProcessor {
  private HtmlEditor editor;

  public HtmlContentAssistProcessor(HtmlEditor editor) {
    this.editor = editor;
  }

  @Override
  public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
    IDocument document = viewer.getDocument();

    String strPrefix = getStrPrefix(document, offset, new WordDetector());
    String bracketStart = getBracketStart(document, offset);

    XmlDocument ast = editor.getModel();
    XmlNode node = ast.getNodeFor(offset);

    // 1. entity name in start node
    // 2. end node
    // 3. attribute name
    // 4. attribute value

    // Check for 1 and 2. If in attribute, check for 4. Default to 3.

    Pattern p = Pattern.compile("(<|</)\\s*\\w*");
    Matcher m = p.matcher(bracketStart);

    List<ICompletionProposal> completions = new ArrayList<ICompletionProposal>();

    if (m.lookingAt() && m.end() == bracketStart.length()) {
      doEntityCompletion(strPrefix, offset, completions);
    } else if (node == null) {
      doEntityCompletion(strPrefix, offset, completions);
    } else if (node instanceof XmlElement) {
      XmlElement element = (XmlElement) node;
      XmlAttribute attribute = null;

      for (XmlAttribute attr : element.getAttributes()) {
        if (attr.getStartOffset() <= offset && attr.getEndOffset() >= offset) {
          attribute = attr;
          break;
        }
      }

      if (attribute == null) {
        doAttributeNameCompletion(element, strPrefix, offset, completions);
      } else {
        String attrValue = getContents(
            document,
            attribute.getStartOffset(),
            offset - attribute.getStartOffset());

        if (attrValue.indexOf('=') == -1) {
          doAttributeNameCompletion(element, strPrefix, offset, completions);
        } else {
          doAttributeValueCompletion(element, strPrefix, offset, completions);
        }
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

  protected void doAttributeNameCompletion(XmlElement element, String strPrefix, int offset,
      List<ICompletionProposal> completions) {
    List<String> attrNames = HtmlKeywords.getAttributes(element.getLabel());

    for (String attrName : attrNames) {
      if (attrName.startsWith(strPrefix)) {
        // add foo=""
        String insertString = attrName + "=\"\"";

        completions.add(new CompletionProposal(
            insertString,
            offset - strPrefix.length(),
            strPrefix.length(),
            insertString.length() - 1,
            DartWebPlugin.getImage("protected_co.gif"),
            attrName,
            null,
            null));
      }
    }
  }

  protected void doAttributeValueCompletion(XmlElement element, String strPrefix, int offset,
      List<ICompletionProposal> completions) {
    // TODO(devoncarew):

  }

  protected void doEntityCompletion(String strPrefix, int offset,
      List<ICompletionProposal> completions) {
    for (String keyword : HtmlKeywords.getKeywords()) {
      if (keyword.startsWith(strPrefix)) {
        completions.add(new CompletionProposal(
            keyword,
            offset - strPrefix.length(),
            strPrefix.length(),
            keyword.length(),
            DartWebPlugin.getImage("xml_node.gif"),
            null,
            null,
            null));
      }
    }
  }

  private String getBracketStart(IDocument document, int offset) {
    try {
      IRegion lineInfo = document.getLineInformationOfOffset(offset);

      String line = document.get(lineInfo.getOffset(), offset - lineInfo.getOffset());

      StringBuilder prefix = new StringBuilder();

      for (int i = line.length() - 1; i >= 0; i--) {
        char c = line.charAt(i);

        if (c == '<') {
          prefix.insert(0, c);
          return prefix.toString();
        } else {
          prefix.insert(0, c);
        }
      }

      return "";
    } catch (BadLocationException ex) {
      return "";
    }
  }

  private String getContents(IDocument document, int offset, int length) {
    try {
      return document.get(offset, length);
    } catch (BadLocationException e) {
      return "";
    }
  }

  private String getStrPrefix(IDocument document, int offset, IWordDetector wordDetector) {
    try {
      IRegion lineInfo = document.getLineInformationOfOffset(offset);

      String line = document.get(lineInfo.getOffset(), offset - lineInfo.getOffset());

      StringBuilder prefix = new StringBuilder();

      for (int i = line.length() - 1; i >= 0; i--) {
        char c = line.charAt(i);

        if (!wordDetector.isWordStart(c)) {
          return prefix.toString();
        } else {
          prefix.insert(0, c);
        }
      }

      return prefix.toString();
    } catch (BadLocationException ex) {
      return null;
    }
  }

}
