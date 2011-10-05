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
package com.google.dart.tools.ui.internal.text.editor.selectionactions;

import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import java.util.ArrayList;
import java.util.List;

public class SelectionHistory {

  private List<SourceRange> fHistory;
  private DartEditor fEditor;
  private ISelectionChangedListener fSelectionListener;
  private int fSelectionChangeListenerCounter;
  private StructureSelectHistoryAction fHistoryAction;

  public SelectionHistory(DartEditor editor) {
    Assert.isNotNull(editor);
    fEditor = editor;
    fHistory = new ArrayList<SourceRange>(3);
    fSelectionListener = new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        if (fSelectionChangeListenerCounter == 0) {
          flush();
        }
      }
    };
    fEditor.getSelectionProvider().addSelectionChangedListener(fSelectionListener);
  }

  public void dispose() {
    fEditor.getSelectionProvider().removeSelectionChangedListener(fSelectionListener);
  }

  public void flush() {
    if (fHistory.isEmpty()) {
      return;
    }
    fHistory.clear();
    fHistoryAction.update();
  }

  public SourceRange getLast() {
    if (isEmpty()) {
      return null;
    }
    int size = fHistory.size();
    SourceRange result = fHistory.remove(size - 1);
    fHistoryAction.update();
    return result;
  }

  public void ignoreSelectionChanges() {
    fSelectionChangeListenerCounter++;
  }

  public boolean isEmpty() {
    return fHistory.isEmpty();
  }

  public void listenToSelectionChanges() {
    fSelectionChangeListenerCounter--;
  }

  public void remember(SourceRange range) {
    fHistory.add(range);
    fHistoryAction.update();
  }

  public void setHistoryAction(StructureSelectHistoryAction action) {
    Assert.isNotNull(action);
    fHistoryAction = action;
  }
}
