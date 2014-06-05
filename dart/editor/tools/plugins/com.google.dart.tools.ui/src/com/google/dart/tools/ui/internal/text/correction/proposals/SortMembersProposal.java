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
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.correction.MembersSorter;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableEx;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.internal.refactoring.ServiceUtils;
import com.google.dart.tools.ui.text.dart.IDartCompletionProposal;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

/**
 * Correction proposal for sorting unit/class members.
 * 
 * @coverage dart.editor.ui.correction
 */
public class SortMembersProposal implements IDartCompletionProposal {
  private final ITextViewer viewer;
  private final Source source;
  private final CompilationUnit unit;

  public SortMembersProposal(ITextViewer viewer, Source source, CompilationUnit unit) {
    this.viewer = viewer;
    this.source = source;
    this.unit = unit;
  }

  @Override
  public void apply(IDocument document) {
    ExecutionUtils.runLog(new RunnableEx() {
      @Override
      public void run() throws Exception {
        Point selectedRange = viewer.getSelectedRange();
        // sort members
        {
          MembersSorter sorter = new MembersSorter(source, unit);
          SourceChange change = sorter.createChange();
          if (change != null) {
            TextFileChange ltkChange = ServiceUtils.toLTK(change);
            ltkChange.perform(new NullProgressMonitor());
          }
        }
        // restore selection
        viewer.setSelectedRange(selectedRange.x, selectedRange.y);
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
