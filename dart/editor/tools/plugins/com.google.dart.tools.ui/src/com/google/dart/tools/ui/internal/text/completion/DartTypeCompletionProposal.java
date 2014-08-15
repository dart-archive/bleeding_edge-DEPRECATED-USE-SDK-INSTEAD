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

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.internal.text.dart.ImportRewrite;
import com.google.dart.tools.ui.text.dart.DartContentAssistInvocationContext;
import com.google.dart.tools.ui.text.editor.tmp.Signature;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.text.edits.TextEdit;

/**
 * If passed compilation unit is not null, the replacement string will be seen as a qualified type
 * name.
 */
public class DartTypeCompletionProposal extends DartCompletionProposal {

  protected final CompilationUnit fCompilationUnit;

  /** The unqualified type name. */
  private final String fUnqualifiedTypeName;
  /** The fully qualified type name. */
  private final String fFullyQualifiedTypeName;

  public DartTypeCompletionProposal(String replacementString, CompilationUnit cu,
      int replacementOffset, int replacementLength, int replacementLengthIdentifier, Image image,
      StyledString displayString, int relevance, String elementDocSummary, String elementDocDetails) {
    this(
        replacementString,
        cu,
        replacementOffset,
        replacementLength,
        replacementLengthIdentifier,
        image,
        displayString,
        relevance,
        null,
        elementDocSummary,
        elementDocDetails);
  }

  public DartTypeCompletionProposal(String replacementString, CompilationUnit cu,
      int replacementOffset, int replacementLength, int replacementLengthIdentifier, Image image,
      StyledString displayString, int relevance, String fullyQualifiedTypeName,
      String elementDocSummary, String elementDocDetails) {
    this(
        replacementString,
        cu,
        replacementOffset,
        replacementLength,
        replacementLengthIdentifier,
        image,
        displayString,
        relevance,
        fullyQualifiedTypeName,
        elementDocSummary,
        elementDocDetails,
        null);
  }

  public DartTypeCompletionProposal(String replacementString, CompilationUnit cu,
      int replacementOffset, int replacementLength, int replacementLengthIdentifier, Image image,
      StyledString displayString, int relevance, String fullyQualifiedTypeName,
      String elementDocSummary, String elementDocDetails,
      DartContentAssistInvocationContext invocationContext) {
    super(
        replacementString,
        replacementOffset,
        replacementLength,
        replacementLengthIdentifier,
        image,
        displayString,
        relevance,
        false,
        elementDocSummary,
        elementDocDetails,
        invocationContext);
    fCompilationUnit = cu;
    fFullyQualifiedTypeName = fullyQualifiedTypeName;
    fUnqualifiedTypeName = fullyQualifiedTypeName != null
        ? Signature.getSimpleName(fullyQualifiedTypeName) : null;
  }

  @Override
  public void apply(IDocument document, char trigger, int offset) {
    try {
      ImportRewrite impRewrite = null;

      if (fCompilationUnit != null && allowAddingImports()) {
        impRewrite = new ImportRewrite(fCompilationUnit, true);
      }

      boolean updateCursorPosition = updateReplacementString(document, trigger, offset, impRewrite);

      if (updateCursorPosition) {
        setCursorPosition(getReplacementString().length());
      }

      super.apply(document, trigger, offset);

      if (impRewrite != null) {
        int oldLen = document.getLength();
        impRewrite.rewriteImports(new NullProgressMonitor()).apply(
            document,
            TextEdit.UPDATE_REGIONS);
        setReplacementOffset(getReplacementOffset() + document.getLength() - oldLen);
      }
    } catch (CoreException e) {
      DartToolsPlugin.log(e);
    } catch (BadLocationException e) {
      DartToolsPlugin.log(e);
    }
  }

  @Override
  public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
    return fUnqualifiedTypeName;
  }

  @Override
  protected boolean isValidPrefix(String prefix) {
    return super.isValidPrefix(prefix) || isPrefix(prefix, fUnqualifiedTypeName)
        || isPrefix(prefix, fFullyQualifiedTypeName);
  }

  /**
   * Updates the replacement string.
   * 
   * @param document the document
   * @param trigger the trigger
   * @param offset the offset
   * @param impRewrite the import rewrite
   * @return <code>true</code> if the cursor position should be updated, <code>false</code>
   *         otherwise
   * @throws BadLocationException if accessing the document fails
   * @throws CoreException if something else fails
   */
  protected boolean updateReplacementString(IDocument document, char trigger, int offset,
      ImportRewrite impRewrite) throws CoreException, BadLocationException {
    return false;
  }

  private boolean allowAddingImports() {
    IPreferenceStore preferenceStore = DartToolsPlugin.getDefault().getPreferenceStore();
    return preferenceStore.getBoolean(PreferenceConstants.CODEASSIST_ADDIMPORT);
  }

}
