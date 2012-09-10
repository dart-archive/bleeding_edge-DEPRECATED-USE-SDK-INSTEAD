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
package com.google.dart.tools.ui.internal.text.correction.proposals;

import com.google.dart.tools.internal.corext.refactoring.code.ConvertMethodToGetterRefactoring;
import com.google.dart.tools.internal.corext.refactoring.util.Messages;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.actions.ConvertMethodToGetterAction;
import com.google.dart.tools.ui.actions.DartEditorActionDefinitionIds;
import com.google.dart.tools.ui.internal.text.correction.CorrectionCommandHandler;
import com.google.dart.tools.ui.internal.text.correction.CorrectionMessages;
import com.google.dart.tools.ui.internal.text.correction.ICommandAccess;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.text.dart.IDartCompletionProposal;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension6;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

/**
 * A quick assist proposal that starts the {@link ConvertMethodToGetterRefactoring}.
 * 
 * @coverage dart.editor.ui.correction
 */
public class ConvertMethodToGetterRefactoringProposal implements IDartCompletionProposal,
    ICompletionProposalExtension6, ICommandAccess {

  private final String fLabel;
  private final int fRelevance;
  private final DartEditor fEditor;

  public ConvertMethodToGetterRefactoringProposal(DartEditor editor, int relevance) {
    Assert.isNotNull(editor);
    fEditor = editor;
    fRelevance = relevance;
    fLabel = CorrectionMessages.ConvertMethodToGetterRefactoringProposal_name;
  }

  @Override
  public void apply(IDocument document) {
    new ConvertMethodToGetterAction(fEditor).run();
  }

  @Override
  public String getAdditionalProposalInfo() {
    return CorrectionMessages.ConvertMethodToGetterRefactoringProposal_additionalInfo;
  }

  @Override
  public String getCommandId() {
    return DartEditorActionDefinitionIds.CONVER_METHOD_TO_GETTER;
  }

  @Override
  public IContextInformation getContextInformation() {
    return null;
  }

  @Override
  public String getDisplayString() {
    String shortCutString = CorrectionCommandHandler.getShortCutString(getCommandId());
    if (shortCutString != null) {
      return Messages.format(
          CorrectionMessages.ChangeCorrectionProposal_name_with_shortcut,
          new String[] {fLabel, shortCutString});
    }
    return fLabel;
  }

  @Override
  public Image getImage() {
    return DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE);
  }

  @Override
  public int getRelevance() {
    return fRelevance;
  }

  @Override
  public Point getSelection(IDocument document) {
    return null;
  }

  @Override
  public StyledString getStyledDisplayString() {
    StyledString str = new StyledString(fLabel);
    String shortCutString = CorrectionCommandHandler.getShortCutString(getCommandId());
    if (shortCutString != null) {
      String decorated = Messages.format(
          CorrectionMessages.ChangeCorrectionProposal_name_with_shortcut,
          new String[] {fLabel, shortCutString});
      return StyledCellLabelProvider.styleDecoratedString(
          decorated,
          StyledString.QUALIFIER_STYLER,
          str);
    }
    return str;
  }
}
