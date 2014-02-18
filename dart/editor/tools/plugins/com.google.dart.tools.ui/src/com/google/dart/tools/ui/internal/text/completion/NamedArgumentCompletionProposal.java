/*
 * Copyright (c) 2013, the Dart project authors.
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
import com.google.dart.tools.core.dom.rewrite.TrackedNodePosition;
import com.google.dart.tools.internal.corext.fix.LinkedProposalModel;
import com.google.dart.tools.internal.corext.fix.LinkedProposalPositionGroup;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.correction.proposals.TrackedPositions;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;
import com.google.dart.tools.ui.internal.viewsupport.LinkedProposalModelPresenter;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.viewers.StyledString;

public class NamedArgumentCompletionProposal extends AbstractDartCompletionProposal {
  private final String name;
  private final String type;

  public NamedArgumentCompletionProposal(CompletionProposal proxy) {
    setReplacementOffset(proxy.getReplaceStart());
    setReplacementLength(proxy.getReplaceEnd() - proxy.getReceiverStart());
    String completion = new String(proxy.getCompletion()) + ": ";
    setReplacementString(completion);
    setSortString(completion);
    setCursorPosition(completion.length());
    name = proxy.getParameterName();
    type = proxy.getParameterType();
    setImage(DartPluginImages.get(DartPluginImages.IMG_MISC_PROTECTED));
    setRelevance(proxy.getRelevance() * 100); // needs to be higher than computeRelevance()
  }

  @Override
  public void apply(IDocument document, char trigger, int offset) {
    super.apply(document, trigger, offset);
  }

  @Override
  public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
    super.apply(viewer, trigger, stateMask, offset);
    showValueProposals(viewer);
  }

  @Override
  public StyledString getStyledDisplayString() {
    return new StyledString("{" + type + " " + name + "}");
  }

  @Override
  public boolean validate(IDocument document, int offset, DocumentEvent event) {
    return super.validate(document, offset, event);
  }

  @Override
  protected boolean isValidPrefix(String prefix) {
    return isPrefix(prefix, name);
  }

  private void showValueProposals(ITextViewer viewer) {
    // only "bool" right now
    if (!"bool".equals(type)) {
      return;
    }
    try {
      DartEditor activeEditor = EditorUtility.getActiveEditor();
      LinkedProposalModel linkedProposalModel = new LinkedProposalModel();
      LinkedProposalPositionGroup group = linkedProposalModel.getPositionGroup("values", true);
      int valuePosition = getReplacementOffset() + getCursorPosition();
      TrackedNodePosition trackedPosition = TrackedPositions.forStartLength(valuePosition, 0);
      group.addPosition(trackedPosition, true);
      group.addProposal("true", null, 0);
      group.addProposal("false", null, 0);
      linkedProposalModel.setEndPosition(trackedPosition);
      new LinkedProposalModelPresenter().enterLinkedMode(
          viewer,
          activeEditor,
          true,
          linkedProposalModel);
    } catch (Throwable e) {
      DartToolsPlugin.log(e);
    }
  }
}
