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

import com.google.dart.tools.ui.actions.AbstractDartSelectionAction_OLD;
import com.google.dart.tools.ui.instrumentation.UIInstrumentation;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.text.editor.DartSelection;

import org.eclipse.jface.text.IDocument;

/**
 * A quick assist proposal that runs some (refactoring) action.
 * 
 * @coverage dart.editor.ui.correction
 */
public abstract class AbstractSelectionActionProposal_OLD extends AbstractActionProposal {
  private final AbstractDartSelectionAction_OLD action;
  private final DartSelection selection;

  public AbstractSelectionActionProposal_OLD(AbstractDartSelectionAction_OLD action, String label,
      DartSelection selection) {
    super(action, label);
    this.action = action;
    this.selection = selection;
  }

  @Override
  public void apply(IDocument document) {
    UIInstrumentationBuilder instrumentation = UIInstrumentation.builder(this.getClass());
    try {
      action.doRun(selection, null, instrumentation);
      instrumentation.metric("Apply", "Completed");
    } catch (RuntimeException e) {
      instrumentation.record(e);
      throw e;
    } finally {
      instrumentation.log();
    }
  }
}
