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
import com.google.dart.tools.core.formatter.DefaultCodeFormatterConstants;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.editor.DartTextHover;
import com.google.dart.tools.ui.text.dart.DartContentAssistInvocationContext;
import com.google.dart.tools.ui.text.editor.tmp.JavaScriptCore;
import com.google.dart.tools.ui.text.editor.tmp.Signature;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.swt.graphics.Image;

public class LazyDartCompletionProposal extends AbstractDartCompletionProposal {

  protected static final class FormatterPrefs {
    /* Methods & constructors */
    public final boolean beforeOpeningParen;
    public final boolean afterOpeningParen;
    public final boolean beforeComma;
    public final boolean afterComma;
    public final boolean beforeClosingParen;
    public final boolean inEmptyList;

    /* type parameters */
    public final boolean beforeOpeningBracket;
    public final boolean afterOpeningBracket;
    public final boolean beforeTypeArgumentComma;
    public final boolean afterTypeArgumentComma;
    public final boolean beforeClosingBracket;

    FormatterPrefs(DartProject project) {
      beforeOpeningParen = getCoreOption(
          project,
          DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_METHOD_INVOCATION,
          false);
      afterOpeningParen = getCoreOption(
          project,
          DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_METHOD_INVOCATION,
          false);
      beforeComma = getCoreOption(
          project,
          DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_INVOCATION_ARGUMENTS,
          false);
      afterComma = getCoreOption(
          project,
          DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_INVOCATION_ARGUMENTS,
          true);
      beforeClosingParen = getCoreOption(
          project,
          DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_METHOD_INVOCATION,
          false);
      inEmptyList = getCoreOption(
          project,
          DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_METHOD_INVOCATION,
          false);

      beforeOpeningBracket = getCoreOption(
          project,
          DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_ANGLE_BRACKET_IN_PARAMETERIZED_TYPE_REFERENCE,
          false);
      afterOpeningBracket = getCoreOption(
          project,
          DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_ANGLE_BRACKET_IN_PARAMETERIZED_TYPE_REFERENCE,
          false);
      beforeTypeArgumentComma = getCoreOption(
          project,
          DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_PARAMETERIZED_TYPE_REFERENCE,
          false);
      afterTypeArgumentComma = getCoreOption(
          project,
          DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_PARAMETERIZED_TYPE_REFERENCE,
          true);
      beforeClosingBracket = getCoreOption(
          project,
          DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_ANGLE_BRACKET_IN_PARAMETERIZED_TYPE_REFERENCE,
          false);
    }

    protected final String getCoreOption(DartProject project, String key) {
      if (project == null) {
        return JavaScriptCore.getOption(key);
      }
      return project.getOption(key, true);
    }

    protected final boolean getCoreOption(DartProject project, String key, boolean def) {
      String option = getCoreOption(project, key);
      if (JavaScriptCore.INSERT.equals(option)) {
        return true;
      }
      if (JavaScriptCore.DO_NOT_INSERT.equals(option)) {
        return false;
      }
      return def;
    }
  }

  protected static final String LPAREN = "("; //$NON-NLS-1$
  protected static final String RPAREN = ")"; //$NON-NLS-1$
  protected static final String COMMA = ","; //$NON-NLS-1$

  protected static final String SPACE = " "; //$NON-NLS-1$

  private boolean fDisplayStringComputed;
  private boolean fReplacementStringComputed;
  private boolean fReplacementOffsetComputed;
  private boolean fReplacementLengthComputed;
  private boolean fCursorPositionComputed;
  private boolean fImageComputed;
  private boolean fContextInformationComputed;
  private boolean fProposalInfoComputed;
  private boolean fTriggerCharactersComputed;
  private boolean fSortStringComputed;
  private boolean fRelevanceComputed;
  private FormatterPrefs fFormatterPrefs;

  /**
   * The core proposal wrapped by this completion proposal.
   */
  protected final CompletionProposal fProposal;
  protected int fContextInformationPosition;

  public LazyDartCompletionProposal(CompletionProposal proposal,
      DartContentAssistInvocationContext context) {
    super(context);
    Assert.isNotNull(proposal);
    Assert.isNotNull(context);
    Assert.isNotNull(context.getCoreContext());
    fProposal = proposal;
  }

  @Override
  public final String getAdditionalProposalInfo() {
    return super.getAdditionalProposalInfo();
  }

  @Override
  public final IContextInformation getContextInformation() {
    if (!fContextInformationComputed) {
      setContextInformation(computeContextInformation());
    }
    return super.getContextInformation();
  }

  @Override
  public String getDisplayString() {
    if (!fDisplayStringComputed) {
      setStyledDisplayString(computeDisplayString());
    }
    return super.getDisplayString();
  }

  @Override
  public final Image getImage() {
    if (!fImageComputed) {
      setImage(computeImage());
    }
    return super.getImage();
  }

  @Override
  public int getPrefixCompletionStart(IDocument document, int completionOffset) {
    return getReplacementOffset();
  }

  /**
   * Gets the proposal's relevance.
   * 
   * @return Returns a int
   */
  @Override
  public final int getRelevance() {
    if (!fRelevanceComputed) {
      setRelevance(computeRelevance());
    }
    return super.getRelevance();
  }

  /**
   * Gets the replacement length.
   * 
   * @return Returns a int
   */
  @Override
  public final int getReplacementLength() {
    if (!fReplacementLengthComputed) {
      setReplacementLength(fProposal.getReplaceEnd() - fProposal.getReplaceStart());
    }
    return super.getReplacementLength();
  }

  @Override
  public int getReplacementLengthIdentifier() {
    return fProposal.getReplaceEndIdentifier() - fProposal.getReplaceStart();
  }

  /**
   * Gets the replacement offset.
   * 
   * @return Returns a int
   */
  @Override
  public final int getReplacementOffset() {
    if (!fReplacementOffsetComputed) {
      setReplacementOffset(fProposal.getReplaceStart());
    }
    return super.getReplacementOffset();
  }

  /**
   * Gets the replacement string.
   * 
   * @return Returns a String
   */
  @Override
  public final String getReplacementString() {
    if (!fReplacementStringComputed) {
      setReplacementString(computeReplacementString());
    }
    return super.getReplacementString();
  }

  @Override
  public final String getSortString() {
    if (!fSortStringComputed) {
      setSortString(computeSortString());
    }
    return super.getSortString();
  }

  @Override
  public StyledString getStyledDisplayString() {
    if (!fDisplayStringComputed) {
      setStyledDisplayString(computeDisplayString());
    }
    return super.getStyledDisplayString();
  }

  @Override
  public final char[] getTriggerCharacters() {
    if (!fTriggerCharactersComputed) {
      setTriggerCharacters(computeTriggerCharacters());
    }
    return super.getTriggerCharacters();
  }

  /**
   * Sets the context information.
   * 
   * @param contextInformation The context information associated with this proposal
   */
  @Override
  public final void setContextInformation(IContextInformation contextInformation) {
    fContextInformationComputed = true;
    super.setContextInformation(contextInformation);
  }

  /**
   * Overrides the default context information position. Ignored if set to zero.
   * 
   * @param contextInformationPosition the replaced position.
   */
  public void setContextInformationPosition(int contextInformationPosition) {
    fContextInformationPosition = contextInformationPosition;
  }

  /**
   * Sets the cursor position relative to the insertion offset. By default this is the length of the
   * completion string (Cursor positioned after the completion)
   * 
   * @param cursorPosition The cursorPosition to set
   */
  @Override
  public final void setCursorPosition(int cursorPosition) {
    fCursorPositionComputed = true;
    super.setCursorPosition(cursorPosition);
  }

  /**
   * Sets the image.
   * 
   * @param image The image to set
   */
  @Override
  public final void setImage(Image image) {
    fImageComputed = true;
    super.setImage(image);
  }

  /**
   * Sets the proposal info.
   * 
   * @param proposalInfo The additional information associated with this proposal or
   *          <code>null</code>
   */
  @Override
  public final void setProposalInfo(ProposalInfo proposalInfo) {
    fProposalInfoComputed = true;
    super.setProposalInfo(proposalInfo);
  }

  /**
   * Sets the proposal's relevance.
   * 
   * @param relevance The relevance to set
   */
  @Override
  public final void setRelevance(int relevance) {
    fRelevanceComputed = true;
    super.setRelevance(relevance);
  }

  /**
   * Sets the replacement length.
   * 
   * @param replacementLength The replacementLength to set
   */
  @Override
  public final void setReplacementLength(int replacementLength) {
    fReplacementLengthComputed = true;
    super.setReplacementLength(replacementLength);
  }

  /**
   * Sets the replacement offset.
   * 
   * @param replacementOffset The replacement offset to set
   */
  @Override
  public final void setReplacementOffset(int replacementOffset) {
    fReplacementOffsetComputed = true;
    super.setReplacementOffset(replacementOffset);
  }

  /**
   * Sets the replacement string.
   * 
   * @param replacementString The replacement string to set
   */
  @Override
  public final void setReplacementString(String replacementString) {
    fReplacementStringComputed = true;
    super.setReplacementString(replacementString);
  }

  @Override
  public void setStyledDisplayString(StyledString text) {
    fDisplayStringComputed = true;
    super.setStyledDisplayString(text);
  }

  /**
   * Sets the trigger characters.
   * 
   * @param triggerCharacters The set of characters which can trigger the application of this
   *          completion proposal
   */
  @Override
  public final void setTriggerCharacters(char[] triggerCharacters) {
    fTriggerCharactersComputed = true;
    super.setTriggerCharacters(triggerCharacters);
  }

  protected IContextInformation computeContextInformation() {
    return null;
  }

  protected int computeCursorPosition() {
    return getReplacementString().length();
  }

  protected StyledString computeDisplayString() {
    return fInvocationContext.getLabelProvider().createStyledLabel(fProposal);
  }

  protected Image computeImage() {
    return DartToolsPlugin.getImageDescriptorRegistry().get(
        fInvocationContext.getLabelProvider().createImageDescriptor(fProposal));
  }

  protected ProposalInfo computeProposalInfo() {
    String html = DartTextHover.getElementDocumentationHtml(
        fProposal.getElementDocSummary(),
        fProposal.getElementDocDetails());
    return new ProposalInfo(fProposal, html);
  }

  protected int computeRelevance() {
    final int baseRelevance = fProposal.getRelevance() * 16;
    switch (fProposal.getKind()) {
      case CompletionProposal.LIBRARY_PREFIX:
        return baseRelevance + 0;
      case CompletionProposal.LABEL_REF:
        return baseRelevance + 1;
      case CompletionProposal.KEYWORD:
        return baseRelevance + 2;
      case CompletionProposal.TYPE_REF:
//      case CompletionProposal.ANONYMOUS_CLASS_DECLARATION:
//      case CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION:
        return baseRelevance + 3;
      case CompletionProposal.METHOD_REF:
      case CompletionProposal.CONSTRUCTOR_INVOCATION:
      case CompletionProposal.METHOD_NAME_REFERENCE:
      case CompletionProposal.METHOD_DECLARATION:
//      case CompletionProposal.ANNOTATION_ATTRIBUTE_REF:
        return baseRelevance + 4;
      case CompletionProposal.POTENTIAL_METHOD_DECLARATION:
        return baseRelevance + 4 /* + 99 */;
      case CompletionProposal.FIELD_REF:
        return baseRelevance + 5;
      case CompletionProposal.LOCAL_VARIABLE_REF:
      case CompletionProposal.VARIABLE_DECLARATION:
        return baseRelevance + 6;
      case CompletionProposal.ARGUMENT_LIST:
        return baseRelevance + 7;
      default:
        return baseRelevance;
    }
  }

  protected String computeReplacementString() {
    return String.valueOf(fProposal.getCompletion());
  }

  protected String computeSortString() {
    return getDisplayString();
  }

  protected char[] computeTriggerCharacters() {
    return new char[0];
  }

  @Override
  protected final int getCursorPosition() {
    if (!fCursorPositionComputed) {
      setCursorPosition(computeCursorPosition());
    }
    return super.getCursorPosition();
  }

  protected FormatterPrefs getFormatterPrefs() {
    if (fFormatterPrefs == null) {
      CompilationUnit cu = null;
      fFormatterPrefs = new FormatterPrefs(cu == null ? null : cu.getDartProject());
    }
    return fFormatterPrefs;
  }

  protected CompletionProposal getProposal() {
    return fProposal;
  }

  /**
   * Returns the additional proposal info, or <code>null</code> if none exists.
   * 
   * @return the additional proposal info, or <code>null</code> if none exists
   */
  @Override
  protected final ProposalInfo getProposalInfo() {
    if (!fProposalInfoComputed) {
      setProposalInfo(computeProposalInfo());
    }
    return super.getProposalInfo();
  }

  @SuppressWarnings("deprecation")
  @Override
  protected final boolean isInDartDoc() {
    return fInvocationContext.getCoreContext().isInJavadoc();
  }

  @Override
  protected boolean isValidPrefix(String prefix) {
    if (super.isValidPrefix(prefix)) {
      return true;
    }

    if (fProposal.getKind() == CompletionProposal.METHOD_NAME_REFERENCE) {
      // static imports - includes package & type name
      StringBuffer buf = new StringBuffer();
      buf.append(Signature.toCharArray(fProposal.getDeclarationSignature()));
      buf.append('.');
      buf.append(TextProcessor.deprocess(getDisplayString()));
      return isPrefix(prefix, buf.toString());
    }

    return false;
  }

  @Override
  protected final void setDisplayString(String string) {
    fDisplayStringComputed = true;
    super.setDisplayString(string);
  }

  @Override
  protected final void setSortString(String string) {
    fSortStringComputed = true;
    super.setSortString(string);
  }
}
