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
package com.google.dart.tools.ui.actions;

import com.google.dart.tools.ui.internal.refactoring.actions.RenameDartElementAction_NEW;
import com.google.dart.tools.ui.internal.refactoring.actions.RenameResourceAction;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;

/**
 * Renames a Dart element or workbench resource.
 * 
 * @coverage dart.editor.ui.refactoring.ui
 */
public class RenameAction_NEW extends Action implements ISelectionChangedListener {
  private final RenameDartElementAction_NEW renameElement;
  private final RenameResourceAction renameResource;

  private IPropertyChangeListener enabledListener = new IPropertyChangeListener() {
    @Override
    public void propertyChange(PropertyChangeEvent event) {
      if (IAction.ENABLED.equals(event.getProperty())) {
        setEnabled(computeEnabledState());
      }
    }
  };

  public RenameAction_NEW(DartEditor editor) {
    renameElement = new RenameDartElementAction_NEW(editor);
    renameResource = new RenameResourceAction(editor.getSite());
    renameElement.addPropertyChangeListener(enabledListener);
    renameResource.addPropertyChangeListener(enabledListener);
  }

  @Override
  public void run() {
    if (renameElement.isEnabled()) {
      renameElement.run();
    }
    if (renameResource.isEnabled()) {
      renameResource.run();
    }
  }

  @Override
  public void selectionChanged(SelectionChangedEvent event) {
    renameElement.selectionChanged(event);
    renameResource.selectionChanged(event);
  }

//  @Override
//  protected void init() {
//    setText(RefactoringMessages.RenameAction_text);
//  }

  private boolean computeEnabledState() {
    return renameElement.isEnabled() || renameResource.isEnabled();
  }
}
