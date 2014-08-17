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

import com.google.dart.tools.core.completion.CompletionProposal;
import com.google.dart.tools.ui.DartElementLabels;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.internal.text.dart.ProposalContextInformation;
import com.google.dart.tools.ui.text.dart.DartContentAssistInvocationContext;
import com.google.dart.tools.ui.text.editor.tmp.Signature;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.osgi.util.TextProcessor;

public class DartMethodCompletionProposal extends LazyDartCompletionProposal {
  /** Triggers for method proposals without parameters. Do not modify. */
  protected final static char[] METHOD_TRIGGERS = new char[] {';', ',', '.', '\t', '['};
  /** Triggers for method proposals. Do not modify. */
  protected final static char[] METHOD_WITH_ARGUMENTS_TRIGGERS = new char[] {'(', '-', ' '};
  /** Triggers for method name proposals (static imports). Do not modify. */
  protected final static char[] METHOD_NAME_TRIGGERS = new char[] {';'};

  private boolean fHasParameters;
  private boolean fHasParametersComputed = false;
  private FormatterPrefs fFormatterPrefs;

  public DartMethodCompletionProposal(CompletionProposal proposal,
      DartContentAssistInvocationContext context) {
    super(proposal, context);
  }

  @Override
  public void apply(IDocument document, char trigger, int offset) {
    if (trigger == ' ' || trigger == '(') {
      trigger = '\0';
    }
    super.apply(document, trigger, offset);
    if (needsLinkedMode()) {
      setUpLinkedMode(document, ')');
    }
  }

  @Override
  public int getPrefixCompletionStart(IDocument document, int completionOffset) {
    if (fProposal.getKind() == CompletionProposal.CONSTRUCTOR_INVOCATION) {
      return fProposal.getRequiredProposals()[0].getReplaceStart();
    }
    return super.getPrefixCompletionStart(document, completionOffset);
  }

  @Override
  public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
    if (hasArgumentList() || fProposal.getKind() == CompletionProposal.CONSTRUCTOR_INVOCATION) {
      String completion = String.valueOf(fProposal.getName());
      if (isCamelCaseMatching()) {
        String prefix = getPrefix(document, completionOffset);
        return getCamelCaseCompound(prefix, completion);
      }
      return completion;
    }
    return super.getPrefixCompletionText(document, completionOffset);
  }

  /**
   * Appends everything up to the method name including the opening parenthesis.
   * <p>
   * 
   * @param buffer the string buffer
   */
  protected void appendMethodNameReplacement(StringBuffer buffer) {
    if (fProposal.getKind() != CompletionProposal.CONSTRUCTOR_INVOCATION) {
      buffer.append(fProposal.getName());
    }

    FormatterPrefs prefs = getFormatterPrefs();
    if (prefs.beforeOpeningParen) {
      buffer.append(SPACE);
    }
    buffer.append(LPAREN);
  }

  @Override
  protected IContextInformation computeContextInformation() {
    // no context information for METHOD_NAME_REF proposals (e.g. for static imports)
    if ((fProposal.getKind() == CompletionProposal.METHOD_REF || fProposal.getKind() == CompletionProposal.CONSTRUCTOR_INVOCATION)
        && hasParameters()
        && (getReplacementString().endsWith(RPAREN) || getReplacementString().length() == 0)) {
      ProposalContextInformation contextInformation = new ProposalContextInformation(fProposal);
      if (fContextInformationPosition != 0 && fProposal.getCompletion().length == 0) {
        contextInformation.setContextInformationPosition(fContextInformationPosition);
      }
      return contextInformation;
    }
    return super.computeContextInformation();
  }

  @Override
  protected String computeReplacementString() {
    if (!hasArgumentList()) {
      return super.computeReplacementString();
    }

    // we're inserting a method plus the argument list - respect formatter preferences
    StringBuffer buffer = new StringBuffer();
    appendMethodNameReplacement(buffer);

    FormatterPrefs prefs = getFormatterPrefs();

    if (hasParameters()) {
      setCursorPosition(buffer.length());

      if (prefs.afterOpeningParen) {
        buffer.append(SPACE);
      }

    } else {
      if (prefs.inEmptyList) {
        buffer.append(SPACE);
      }
    }

    buffer.append(RPAREN);

    return buffer.toString();

  }

  @Override
  protected String computeSortString() {
    /*
     * Lexicographical sort order: 1) by relevance (done by the proposal sorter) 2) by method name
     * 3) by parameter count 4) by parameter type names
     */
    char[] name = fProposal.getName();
    char[] parameterList = Signature.toCharArray(fProposal.getSignature(), null, null, false, false);
    int parameterCount = fProposal.getParameterNames().length % 10; // we don't care about insane methods with >9 parameters
    StringBuffer buf = new StringBuffer(name.length + 2 + parameterList.length);

    buf.append(name);
    buf.append('\0'); // separator
    buf.append(parameterCount);
    buf.append(parameterList);
    return buf.toString();
  }

  @Override
  protected char[] computeTriggerCharacters() {
    if (fProposal.getKind() == CompletionProposal.METHOD_NAME_REFERENCE) {
      return METHOD_NAME_TRIGGERS;
    }
    if (hasParameters()) {
      return METHOD_WITH_ARGUMENTS_TRIGGERS;
    }
    return METHOD_TRIGGERS;
  }

  /**
   * Returns the method formatter preferences.
   * 
   * @return the formatter settings
   */
  @Override
  protected final FormatterPrefs getFormatterPrefs() {
    if (fFormatterPrefs == null) {
      fFormatterPrefs = new FormatterPrefs(null);
    }
    return fFormatterPrefs;
  }

  @Override
  protected String getPrefix(IDocument document, int offset) {
    if (fProposal.getKind() != CompletionProposal.CONSTRUCTOR_INVOCATION) {
      return super.getPrefix(document, offset);
    }

    int replacementOffset = fProposal.getRequiredProposals()[0].getReplaceStart();

    try {
      int length = offset - replacementOffset;
      if (length > 0) {
        return document.get(replacementOffset, length);
      }
    } catch (BadLocationException x) {
    }
    return ""; //$NON-NLS-1$

  }

  /**
   * Returns <code>true</code> if the argument list should be inserted by the proposal,
   * <code>false</code> if not.
   * 
   * @return <code>true</code> when the proposal is not in javadoc nor within an import and
   *         comprises the parameter list
   */
  protected boolean hasArgumentList() {
    if (CompletionProposal.METHOD_NAME_REFERENCE == fProposal.getKind()) {
      return false;
    }
    IPreferenceStore preferenceStore = DartToolsPlugin.getDefault().getPreferenceStore();
    boolean noOverwrite = preferenceStore.getBoolean(PreferenceConstants.CODEASSIST_INSERT_COMPLETION)
        ^ isToggleEating();
    char[] completion = fProposal.getCompletion();
    return !isInDartDoc() && completion.length > 0
        && (noOverwrite || completion[completion.length - 1] == ')');
  }

  /**
   * Returns <code>true</code> if the method being inserted has at least one parameter. Note that
   * this does not say anything about whether the argument list should be inserted. This depends on
   * the position in the document and the kind of proposal; see {@link #hasArgumentList() }.
   * 
   * @return <code>true</code> if the method has any parameters, <code>false</code> if it has no
   *         parameters
   */
  protected final boolean hasParameters() {
    if (!fHasParametersComputed) {
      fHasParametersComputed = true;
      fHasParameters = computeHasParameters();
    }
    return fHasParameters;
  }

  @Override
  protected boolean isOffsetValid(int offset) {
    if (fProposal.getKind() != CompletionProposal.CONSTRUCTOR_INVOCATION) {
      return super.isOffsetValid(offset);
    }

    return fProposal.getRequiredProposals()[0].getReplaceStart() <= offset;
  }

  @Override
  protected boolean isValidPrefix(String prefix) {
    if (super.isValidPrefix(prefix)) {
      return true;
    }

    String word = TextProcessor.deprocess(getDisplayString());
    if (fProposal.getKind() == CompletionProposal.CONSTRUCTOR_INVOCATION) {
      int start = word.indexOf(DartElementLabels.CONCAT_STRING)
          + DartElementLabels.CONCAT_STRING.length();
      word = word.substring(start);
      return isPrefix(prefix, word) || isPrefix(prefix, new String(fProposal.getName()));
    }

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
    }
    return isPrefix(prefix, word);
  }

  protected boolean needsLinkedMode() {
    return hasArgumentList() && hasParameters();
  }

  private boolean computeHasParameters() throws IllegalArgumentException {
    return getProposal().getParameterNames().length > 0;
  }
}
