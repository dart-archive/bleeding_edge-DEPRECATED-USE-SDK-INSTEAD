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

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartX;
import com.google.dart.tools.ui.PreferenceConstants;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
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
      int replacementOffset, int replacementLength, Image image, String displayString, int relevance) {
    this(replacementString, cu, replacementOffset, replacementLength, image, displayString,
        relevance, null);
  }

  public DartTypeCompletionProposal(String replacementString, CompilationUnit cu,
      int replacementOffset, int replacementLength, Image image, String displayString,
      int relevance, String fullyQualifiedTypeName) {
    super(replacementString, replacementOffset, replacementLength, image, displayString, relevance);
    fCompilationUnit = cu;
    fFullyQualifiedTypeName = fullyQualifiedTypeName;
    DartX.todo();
    fUnqualifiedTypeName = null;
//    fUnqualifiedTypeName = fullyQualifiedTypeName != null
//        ? Signature.getSimpleName(fullyQualifiedTypeName) : null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see ICompletionProposalExtension#apply(IDocument, char, int)
   */
  @Override
  public void apply(IDocument document, char trigger, int offset) {
    try {
      ImportRewrite impRewrite = null;

      if (fCompilationUnit != null && allowAddingImports()) {
        impRewrite = new ImportRewrite(fCompilationUnit, true);
      }

      boolean importAdded = updateReplacementString(document, trigger, offset, impRewrite);

      if (importAdded) {
        setCursorPosition(getReplacementString().length());
      }

      super.apply(document, trigger, offset);

      if (importAdded && impRewrite != null) {
        int oldLen = document.getLength();
        impRewrite.rewriteImports(new NullProgressMonitor()).apply(document,
            TextEdit.UPDATE_REGIONS);
        setReplacementOffset(getReplacementOffset() + document.getLength() - oldLen);
      }
    } catch (CoreException e) {
      DartToolsPlugin.log(e);
    } catch (BadLocationException e) {
      DartToolsPlugin.log(e);
    }
  }

  /*
   * @see com.google.dart.tools.ui.internal.text.dart.DartCompletionProposal# getCompletionText ()
   */
  @Override
  public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
    return fUnqualifiedTypeName;
  }

  /*
   * @see com.google.dart.tools.ui.internal.text.dart.AbstractDartCompletionProposal #isValidPrefix
   * (java.lang.String)
   */
  @Override
  protected boolean isValidPrefix(String prefix) {
    return super.isValidPrefix(prefix) || isPrefix(prefix, fUnqualifiedTypeName)
        || isPrefix(prefix, fFullyQualifiedTypeName);
  }

  protected boolean updateReplacementString(IDocument document, char trigger, int offset,
      ImportRewrite impRewrite) throws CoreException, BadLocationException {
    // avoid adding imports when inside imports container
    if (impRewrite != null && fFullyQualifiedTypeName != null) {
      String replacementString = getReplacementString();
      String qualifiedType = fFullyQualifiedTypeName;
      if (qualifiedType.indexOf('.') != -1 && replacementString.startsWith(qualifiedType)
          && !replacementString.endsWith(String.valueOf(';'))) {
        Type[] types = impRewrite.getCompilationUnit().getTypes();
        if (types.length > 0 && types[0].getSourceRange().getOffset() <= offset) {
          // ignore positions above type.
          setReplacementString(impRewrite.addImport(getReplacementString()));
          return true;
        }
      }
    }
    return false;
  }

  private boolean allowAddingImports() {
    IPreferenceStore preferenceStore = DartToolsPlugin.getDefault().getPreferenceStore();
    return preferenceStore.getBoolean(PreferenceConstants.CODEASSIST_ADDIMPORT);
  }

}
