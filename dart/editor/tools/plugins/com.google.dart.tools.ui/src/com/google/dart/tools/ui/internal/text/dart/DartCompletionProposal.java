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

import com.google.dart.tools.ui.text.dart.DartContentAssistInvocationContext;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;

public class DartCompletionProposal extends AbstractDartCompletionProposal {

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
   */
  public DartCompletionProposal(String replacementString, int replacementOffset,
      int replacementLength, Image image, String displayString, int relevance) {
    this(replacementString, replacementOffset, replacementLength, image, displayString, relevance,
        false);
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
      int replacementLength, Image image, String displayString, int relevance, boolean inJavadoc) {
    this(replacementString, replacementOffset, replacementLength, image, displayString, relevance,
        inJavadoc, null);
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
      int replacementLength, Image image, String displayString, int relevance, boolean inJavadoc,
      DartContentAssistInvocationContext invocationContext) {
    super(invocationContext);
    Assert.isNotNull(replacementString);
    Assert.isTrue(replacementOffset >= 0);
    Assert.isTrue(replacementLength >= 0);

    setReplacementString(replacementString);
    setReplacementOffset(replacementOffset);
    setReplacementLength(replacementLength);
    setImage(image);
    setDisplayString(displayString == null ? replacementString : displayString);
    setRelevance(relevance);
    setCursorPosition(replacementString.length());
    setInJavadoc(inJavadoc);
    setSortString(displayString == null ? replacementString : displayString);
  }

  /*
   * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension3# getReplacementText()
   */
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

  /*
   * @see com.google.dart.tools.ui.internal.text.dart.AbstractDartCompletionProposal #isValidPrefix
   * (java.lang.String)
   */
  @Override
  protected boolean isValidPrefix(String prefix) {
    String word = getDisplayString();
    if (isInJavadoc()) {
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
