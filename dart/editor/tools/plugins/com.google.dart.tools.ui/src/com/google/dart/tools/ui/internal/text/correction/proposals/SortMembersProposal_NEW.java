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

import com.google.dart.server.SortMembersConsumer;
import com.google.dart.server.generated.types.RequestError;
import com.google.dart.server.generated.types.SourceFileEdit;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableEx;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.refactoring.ServiceUtils_NEW;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;
import com.google.dart.tools.ui.text.dart.IDartCompletionProposal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

/**
 * Correction proposal for sorting unit/class members.
 * 
 * @coverage dart.editor.ui.correction
 */
public class SortMembersProposal_NEW implements IDartCompletionProposal {
  private final String file;

  public SortMembersProposal_NEW(DartEditor editor) {
    this.file = editor.getInputFilePath();
  }

  @Override
  public void apply(IDocument document) {
    ExecutionUtils.runLog(new RunnableEx() {
      @Override
      public void run() throws Exception {
        DartCore.getAnalysisServer().edit_sortMembers(file, new SortMembersConsumer() {
          @Override
          public void computedEdit(SourceFileEdit edit) {
            Change change = ServiceUtils_NEW.toLTK(edit);
            // I'd like to see if there are any changes after sorting
            if (change instanceof TextFileChange) {
              TextFileChange fileChange = (TextFileChange) change;
              fileChange.setSaveMode(TextFileChange.LEAVE_DIRTY);
            }
            // apply the change
            try {
              IProgressMonitor pm = new NullProgressMonitor();
              change.perform(pm);
            } catch (CoreException e) {
              DartToolsPlugin.log(e);
            }
          }

          @Override
          public void onError(final RequestError requestError) {
            Display.getDefault().asyncExec(new Runnable() {
              @Override
              public void run() {
                String message = requestError.getMessage();
                Status status = new Status(IStatus.ERROR, DartToolsPlugin.PLUGIN_ID, message);
                ExceptionHandler.handle(status, "Sort members", "Error has happened.");
              }
            });
          }
        });
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
    return 3;
  }

  @Override
  public Point getSelection(IDocument document) {
    return null;
  }
}
