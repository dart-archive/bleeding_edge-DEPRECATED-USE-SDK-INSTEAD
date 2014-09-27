/*
 * Copyright (c) 2014, the Dart project authors.
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

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.services.correction.MembersSorter;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableEx;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.text.dart.IDartCompletionProposal;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

/**
 * Correction proposal for sorting unit/class members.
 * 
 * @coverage dart.editor.ui.correction
 */
public class SortMembersProposal_OLD implements IDartCompletionProposal {
  private final ITextViewer viewer;
  private final CompilationUnit unit;

  public SortMembersProposal_OLD(ITextViewer viewer, CompilationUnit unit) {
    this.viewer = viewer;
    this.unit = unit;
  }

  @Override
  public void apply(IDocument document) {
    ExecutionUtils.runLog(new RunnableEx() {
      @Override
      public void run() throws Exception {
        String code = viewer.getDocument().get();
        MembersSorter sorter = new MembersSorter(code, unit);
        String sortedCode = sorter.createSortedCode();
        if (sortedCode != null) {
          Point selectedRange = viewer.getSelectedRange();
          viewer.getDocument().set(sortedCode);
          viewer.setSelectedRange(selectedRange.x, selectedRange.y);
          viewer.revealRange(selectedRange.x, selectedRange.y);
        }
      }
    });
  }

  @Override
  public String getAdditionalProposalInfo() {
    return null;
  }

  @Override
  public IContextInformation getContextInformation() {
    return null;
  }

  @Override
  public String getDisplayString() {
    return "Sort unit/class members";
  }

  @Override
  public Image getImage() {
    return DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE);
  }

  @Override
  public int getRelevance() {
    return 5;
  }

  @Override
  public Point getSelection(IDocument document) {
    return null;
  }
}
