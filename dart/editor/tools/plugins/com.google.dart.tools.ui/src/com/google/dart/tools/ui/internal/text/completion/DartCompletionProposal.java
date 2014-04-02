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
package com.google.dart.tools.ui.internal.text.completion;

import com.google.dart.engine.element.Element;
import com.google.dart.tools.core.completion.CompletionProposal;
import com.google.dart.tools.ui.internal.text.editor.DartTextHover;
import com.google.dart.tools.ui.text.dart.DartContentAssistInvocationContext;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.swt.graphics.Image;

public class DartCompletionProposal extends AbstractDartCompletionProposal {
  private Element element;

  /**
   * Creates a new completion proposal. All fields are initialized based on the provided
   * information.
   * 
   * @param replacementString the actual string to be inserted into the document
   * @param replacementOffset the offset of the text to be replaced
   * @param replacementLength the length of the text to be replaced
   * @param image the image to display for this proposal
   * @param displayString the string to be displayed for the proposal If set to <code>null</code>,
   *          the replacement string will be taken as display string.
   * @param relevance the relevance
   */
  public DartCompletionProposal(String replacementString, int replacementOffset,
      int replacementLength, int replacementLengthIdentifier, Image image, String displayString,
      int relevance, Element element) {
    this(
        replacementString,
        replacementOffset,
        replacementLength,
        replacementLengthIdentifier,
        image,
        new StyledString(displayString),
        relevance,
        false,
        element);
  }

  /**
   * Creates a new completion proposal. All fields are initialized based on the provided
   * information.
   * 
   * @param replacementString the actual string to be inserted into the document
   * @param replacementOffset the offset of the text to be replaced
   * @param replacementLength the length of the text to be replaced
   * @param image the image to display for this proposal
   * @param displayString the string to be displayed for the proposal If set to <code>null</code>,
   *          the replacement string will be taken as display string.
   * @param relevance the relevance
   * @param inJavadoc <code>true</code> for a javadoc proposal
   */
  public DartCompletionProposal(String replacementString, int replacementOffset,
      int replacementLength, int replacementLengthIdentifier, Image image,
      StyledString displayString, int relevance, boolean inJavadoc, Element element) {
    this(
        replacementString,
        replacementOffset,
        replacementLength,
        replacementLengthIdentifier,
        image,
        displayString,
        relevance,
        inJavadoc,
        element,
        null);
  }

  /**
   * Creates a new completion proposal. All fields are initialized based on the provided
   * information.
   * 
   * @param replacementString the actual string to be inserted into the document
   * @param replacementOffset the offset of the text to be replaced
   * @param replacementLength the length of the text to be replaced
   * @param image the image to display for this proposal
   * @param displayString the string to be displayed for the proposal If set to <code>null</code>,
   *          the replacement string will be taken as display string.
   * @param relevance the relevance
   * @param inJavadoc <code>true</code> for a javadoc proposal
   * @param invocationContext the invocation context of this completion proposal or
   *          <code>null</code> not available
   */
  public DartCompletionProposal(String replacementString, int replacementOffset,
      int replacementLength, int replacementLengthIdentifier, Image image,
      StyledString displayString, int relevance, boolean inJavadoc, Element element,
      DartContentAssistInvocationContext invocationContext) {
    super(invocationContext);
    Assert.isNotNull(replacementString);
    Assert.isTrue(replacementOffset >= 0);
    Assert.isTrue(replacementLength >= 0);
    Assert.isTrue(replacementLengthIdentifier >= 0);

    // put cursor at the marker position
    int cursorPos = replacementString.length();
    {
      int exclamationPos = replacementString.indexOf(CompletionProposal.CURSOR_MARKER);
      if (exclamationPos != -1) {
        cursorPos = exclamationPos;
        replacementString = replacementString.substring(0, exclamationPos)
            + replacementString.substring(exclamationPos + 1);
        triggerCompletionAfterApply = true;
      }
    }

    setReplacementString(replacementString);
    setReplacementOffset(replacementOffset);
    setReplacementLength(replacementLength);
    setReplacementLengthIdentifier(replacementLengthIdentifier);
    setImage(image);
    setStyledDisplayString(displayString == null ? new StyledString(replacementString)
        : displayString);
    setRelevance(relevance);
    setCursorPosition(cursorPos);
    setInDartDoc(inJavadoc);
    setSortString(displayString == null ? replacementString : displayString.getString());
    setElement(element);
  }

  /**
   * Creates a new completion proposal. All fields are initialized based on the provided
   * information.
   * 
   * @param replacementString the actual string to be inserted into the document
   * @param replacementOffset the offset of the text to be replaced
   * @param replacementLength the length of the text to be replaced
   * @param image the image to display for this proposal
   * @param displayString the string to be displayed for the proposal If set to <code>null</code>,
   *          the replacement string will be taken as display string.
   * @param relevance the relevance
   */
  public DartCompletionProposal(String replacementString, int replacementOffset,
      int replacementLength, int replacementLengthIdentifier, Image image,
      StyledString displayString, int relevance, Element element) {
    this(
        replacementString,
        replacementOffset,
        replacementLength,
        replacementLengthIdentifier,
        image,
        displayString,
        relevance,
        false,
        element);
  }

  public Element getElement() {
    return element;
  }

  @Override
  public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
    String string = getReplacementString();
    int pos = string.indexOf('(');
    if (pos > 0) {
      return string.subSequence(0, pos);
    } else if (string.startsWith("this.")) {
      return string.substring(5);
    } else {
      return string;
    }
  }

  public void setElement(Element element) {
    this.element = element;
  }

  @Override
  protected ProposalInfo getProposalInfo() {
    String html = DartTextHover.getElementDocumentationHtml(null, element);
    return new ProposalInfo(null, html);
  }

  @Override
  protected boolean isValidPrefix(String prefix) {
    String word = TextProcessor.deprocess(getDisplayString());
    if (isInDartDoc()) {
      int idx = word.indexOf("{@link "); //$NON-NLS-1$
      if (idx == 0) {
        word = word.substring(7);
      } else {
        idx = word.indexOf("{@value "); //$NON-NLS-1$
        if (idx == 0) {
          word = word.substring(8);
        }
      }
    } else if (word.indexOf("this.") != -1) { //$NON-NLS-1$
      word = word.substring(word.indexOf("this.") + 5); //$NON-NLS-1$
    }
    return isPrefix(prefix, word);
  }
}
