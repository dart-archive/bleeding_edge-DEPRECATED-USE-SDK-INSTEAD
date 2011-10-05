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

import com.google.dart.tools.core.completion.CompletionProposal;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.EditorHighlightingSynchronizer;
import com.google.dart.tools.ui.text.dart.DartContentAssistInvocationContext;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationExtension;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;

/**
 * Proposal for generic types.
 * <p>
 * Only used when compliance is set to 5.0 or higher.
 * </p>
 */
public final class LazyGenericTypeProposal extends LazyDartTypeCompletionProposal {
  /**
   * Short-lived context information object for generic types. Currently, these are only created
   * after inserting a type proposal, as core doesn't give us the correct type proposal from within
   * SomeType<|>.
   */
  private static class ContextInformation implements IContextInformation,
      IContextInformationExtension {
    private final String fInformationDisplayString;
    private final String fContextDisplayString;
    private final Image fImage;
    private final int fPosition;

    ContextInformation(LazyGenericTypeProposal proposal) {
      // don't cache the proposal as content assistant
      // might hang on to the context info
      fContextDisplayString = proposal.getDisplayString();
      fInformationDisplayString = computeContextString(proposal);
      fImage = proposal.getImage();
      fPosition = proposal.getReplacementOffset() + proposal.getReplacementString().indexOf('<')
          + 1;
    }

    /*
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
      if (obj instanceof ContextInformation) {
        ContextInformation ci = (ContextInformation) obj;
        return getContextInformationPosition() == ci.getContextInformationPosition()
            && getInformationDisplayString().equals(ci.getInformationDisplayString());
      }
      return false;
    }

    /*
     * @see org.eclipse.jface.text.contentassist.IContextInformation# getContextDisplayString()
     */
    @Override
    public String getContextDisplayString() {
      return fContextDisplayString;
    }

    /*
     * @see org.eclipse.jface.text.contentassist.IContextInformationExtension#
     * getContextInformationPosition()
     */
    @Override
    public int getContextInformationPosition() {
      return fPosition;
    }

    /*
     * @see org.eclipse.jface.text.contentassist.IContextInformation#getImage()
     */
    @Override
    public Image getImage() {
      return fImage;
    }

    /*
     * @see org.eclipse.jface.text.contentassist.IContextInformation# getInformationDisplayString()
     */
    @Override
    public String getInformationDisplayString() {
      return fInformationDisplayString;
    }

    private String computeContextString(LazyGenericTypeProposal proposal) {
      try {
        TypeArgumentProposal[] proposals = proposal.computeTypeArgumentProposals();
        if (proposals.length == 0) {
          return null;
        }

        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < proposals.length; i++) {
          buf.append(proposals[i].getDisplayName());
          if (i < proposals.length - 1) {
            buf.append(", "); //$NON-NLS-1$
          }
        }
        return buf.toString();

      } catch (DartModelException e) {
        return null;
      }
    }
  }

  private static final class TypeArgumentProposal {
    private final boolean fIsAmbiguous;
    private final String fProposal;
    private final String fTypeDisplayName;

    TypeArgumentProposal(String proposal, boolean ambiguous, String typeDisplayName) {
      fIsAmbiguous = ambiguous;
      fProposal = proposal;
      fTypeDisplayName = typeDisplayName;
    }

    public String getDisplayName() {
      return fTypeDisplayName;
    }

    @Override
    public String toString() {
      return fProposal;
    }

    String getProposals() {
      return fProposal;
    }

    boolean isAmbiguous() {
      return fIsAmbiguous;
    }
  }

  /** Triggers for types. Do not modify. */
  private final static char[] GENERIC_TYPE_TRIGGERS = new char[] {'.', '\t', '[', '(', '<', ' '};

  private IRegion fSelectedRegion; // initialized by apply()
  private TypeArgumentProposal[] fTypeArgumentProposals;

  public LazyGenericTypeProposal(CompletionProposal typeProposal,
      DartContentAssistInvocationContext context) {
    super(typeProposal, context);
  }

  /*
   * @see ICompletionProposalExtension#apply(IDocument, char)
   */
  @Override
  public void apply(IDocument document, char trigger, int offset) {

    if (shouldAppendArguments(document, offset, trigger)) {
      try {
        TypeArgumentProposal[] typeArgumentProposals = computeTypeArgumentProposals();
        if (typeArgumentProposals.length > 0) {

          int[] offsets = new int[typeArgumentProposals.length];
          int[] lengths = new int[typeArgumentProposals.length];
          StringBuffer buffer = createParameterList(typeArgumentProposals, offsets, lengths);

          // set the generic type as replacement string
          boolean insertClosingParenthesis = trigger == '(' && autocloseBrackets();
          if (insertClosingParenthesis) {
            updateReplacementWithParentheses(buffer);
          }
          super.setReplacementString(buffer.toString());

          // add import & remove package, update replacement offset
          super.apply(document, '\0', offset);

          if (getTextViewer() != null) {
            if (hasAmbiguousProposals(typeArgumentProposals)) {
              adaptOffsets(offsets, buffer);
              installLinkedMode(document, offsets, lengths, typeArgumentProposals,
                  insertClosingParenthesis);
            } else {
              if (insertClosingParenthesis) {
                setUpLinkedMode(document, ')');
              } else {
                fSelectedRegion = new Region(getReplacementOffset()
                    + getReplacementString().length(), 0);
              }
            }
          }

          return;
        }
      } catch (DartModelException e) {
        // log and continue
        DartToolsPlugin.log(e);
      }
    }

    // default is to use the super implementation
    // reasons:
    // - not a parameterized type,
    // - already followed by <type arguments>
    // - proposal type does not inherit from expected type
    super.apply(document, trigger, offset);
  }

  /*
   * @see ICompletionProposal#getSelection(IDocument)
   */
  @Override
  public Point getSelection(IDocument document) {
    if (fSelectedRegion == null) {
      return super.getSelection(document);
    }

    return new Point(fSelectedRegion.getOffset(), fSelectedRegion.getLength());
  }

  /*
   * @see com.google.dart.tools.ui.internal.text.dart.LazyDartCompletionProposal#
   * computeContextInformation()
   */
  @Override
  protected IContextInformation computeContextInformation() {
    try {
      if (hasParameters()) {
        TypeArgumentProposal[] proposals = computeTypeArgumentProposals();
        if (hasAmbiguousProposals(proposals)) {
          return new ContextInformation(this);
        }
      }
    } catch (DartModelException e) {
    }
    return super.computeContextInformation();
  }

  @Override
  protected int computeCursorPosition() {
    if (fSelectedRegion != null) {
      return fSelectedRegion.getOffset() - getReplacementOffset();
    }
    return super.computeCursorPosition();
  }

  /*
   * @see com.google.dart.tools.ui.internal.text.dart.LazyDartTypeCompletionProposal#
   * computeTriggerCharacters()
   */
  @Override
  protected char[] computeTriggerCharacters() {
    return GENERIC_TYPE_TRIGGERS;
  }

  /**
   * Adapt the parameter offsets to any modification of the replacement string done by
   * <code>apply</code>. For example, applying the proposal may add an import instead of inserting
   * the fully qualified name.
   * <p>
   * This assumes that modifications happen only at the beginning of the replacement string and do
   * not touch the type arguments list.
   * </p>
   * 
   * @param offsets the offsets to modify
   * @param buffer the original replacement string
   */
  private void adaptOffsets(int[] offsets, StringBuffer buffer) {
    String replacementString = getReplacementString();
    int delta = buffer.length() - replacementString.length(); // due to using an
// import instead of package
    for (int i = 0; i < offsets.length; i++) {
      offsets[i] -= delta;
    }
  }

  /**
   * Computes the type argument proposals for this type proposals. If there is an expected type
   * binding that is a super type of the proposed type, the wildcard type arguments of the proposed
   * type that can be mapped through to type the arguments of the expected type binding are bound
   * accordingly.
   * <p>
   * For type arguments that cannot be mapped to arguments in the expected type, or if there is no
   * expected type, the upper bound of the type argument is proposed.
   * </p>
   * <p>
   * The argument proposals have their <code>isAmbiguos</code> flag set to <code>false</code> if the
   * argument can be mapped to a non-wildcard type argument in the expected type, otherwise the
   * proposal is ambiguous.
   * </p>
   * 
   * @return the type argument proposals for the proposed type
   * @throws DartModelException if accessing the java model fails
   */
  private TypeArgumentProposal[] computeTypeArgumentProposals() throws DartModelException {
    if (fTypeArgumentProposals == null) {

      Type type = (Type) getJavaElement();
      if (type == null) {
        return new TypeArgumentProposal[0];
      }

      return new TypeArgumentProposal[0];
    }
    return fTypeArgumentProposals;
  }

  private StringBuffer createParameterList(TypeArgumentProposal[] typeArguments, int[] offsets,
      int[] lengths) {
    StringBuffer buffer = new StringBuffer();
    buffer.append(getReplacementString());

    FormatterPrefs prefs = getFormatterPrefs();
    final char LESS = '<';
    final char GREATER = '>';
    if (prefs.beforeOpeningBracket) {
      buffer.append(SPACE);
    }
    buffer.append(LESS);
    if (prefs.afterOpeningBracket) {
      buffer.append(SPACE);
    }
    StringBuffer separator = new StringBuffer(3);
    if (prefs.beforeTypeArgumentComma) {
      separator.append(SPACE);
    }
    separator.append(COMMA);
    if (prefs.afterTypeArgumentComma) {
      separator.append(SPACE);
    }

    for (int i = 0; i != typeArguments.length; i++) {
      if (i != 0) {
        buffer.append(separator);
      }

      offsets[i] = buffer.length();
      buffer.append(typeArguments[i]);
      lengths[i] = buffer.length() - offsets[i];
    }
    if (prefs.beforeClosingBracket) {
      buffer.append(SPACE);
    }
    buffer.append(GREATER);

    return buffer;
  }

  /**
   * Returns the currently active java editor, or <code>null</code> if it cannot be determined.
   * 
   * @return the currently active java editor, or <code>null</code>
   */
  private DartEditor getJavaEditor() {
    IEditorPart part = DartToolsPlugin.getActivePage().getActiveEditor();
    if (part instanceof DartEditor) {
      return (DartEditor) part;
    } else {
      return null;
    }
  }

  private boolean hasAmbiguousProposals(TypeArgumentProposal[] typeArgumentProposals) {
    boolean hasAmbiguousProposals = false;
    for (int i = 0; i < typeArgumentProposals.length; i++) {
      if (typeArgumentProposals[i].isAmbiguous()) {
        hasAmbiguousProposals = true;
        break;
      }
    }
    return hasAmbiguousProposals;
  }

  private boolean hasParameters() {
    Type type = (Type) getJavaElement();
    if (type == null) {
      return false;
    }

    return false;
  }

  private void installLinkedMode(IDocument document, int[] offsets, int[] lengths,
      TypeArgumentProposal[] typeArgumentProposals, boolean withParentheses) {
    int replacementOffset = getReplacementOffset();
    String replacementString = getReplacementString();

    try {
      LinkedModeModel model = new LinkedModeModel();
      for (int i = 0; i != offsets.length; i++) {
        if (typeArgumentProposals[i].isAmbiguous()) {
          LinkedPositionGroup group = new LinkedPositionGroup();
          group.addPosition(new LinkedPosition(document, replacementOffset + offsets[i], lengths[i]));
          model.addGroup(group);
        }
      }
      if (withParentheses) {
        LinkedPositionGroup group = new LinkedPositionGroup();
        group.addPosition(new LinkedPosition(document, replacementOffset + getCursorPosition(), 0));
        model.addGroup(group);
      }

      model.forceInstall();
      DartEditor editor = getJavaEditor();
      if (editor != null) {
        model.addLinkingListener(new EditorHighlightingSynchronizer(editor));
      }

      LinkedModeUI ui = new EditorLinkedModeUI(model, getTextViewer());
      ui.setExitPolicy(new ExitPolicy(withParentheses ? ')' : '>', document));
      ui.setExitPosition(getTextViewer(), replacementOffset + replacementString.length(), 0,
          Integer.MAX_VALUE);
      ui.setDoContextInfo(true);
      ui.enter();

      fSelectedRegion = ui.getSelectedRegion();

    } catch (BadLocationException e) {
      DartToolsPlugin.log(e);
      openErrorDialog(e);
    }
  }

  private void openErrorDialog(BadLocationException e) {
    Shell shell = getTextViewer().getTextWidget().getShell();
    MessageDialog.openError(shell, DartTextMessages.FilledArgumentNamesMethodProposal_error_msg,
        e.getMessage());
  }

  /**
   * Returns <code>true</code> if type arguments should be appended when applying this proposal,
   * <code>false</code> if not (for example if the document already contains a type argument list
   * after the insertion point.
   * 
   * @param document the document
   * @param offset the insertion offset
   * @param trigger the trigger character
   * @return <code>true</code> if arguments should be appended
   */
  private boolean shouldAppendArguments(IDocument document, int offset, char trigger) {
    /*
     * No argument list if there were any special triggers (for example a period to qualify an inner
     * type).
     */
    if (trigger != '\0' && trigger != '<' && trigger != '(') {
      return false;
    }

    /*
     * No argument list if the completion is empty (already within the argument list).
     */
    char[] completion = fProposal.getCompletion();
    if (completion.length == 0) {
      return false;
    }

    /* No argument list if there already is a generic signature behind the name. */
    try {
      IRegion region = document.getLineInformationOfOffset(offset);
      String line = document.get(region.getOffset(), region.getLength());

      int index = offset - region.getOffset();
      while (index != line.length() && Character.isUnicodeIdentifierPart(line.charAt(index))) {
        ++index;
      }

      if (index == line.length()) {
        return true;
      }

      char ch = line.charAt(index);
      return ch != '<';

    } catch (BadLocationException e) {
      return true;
    }
  }
}
