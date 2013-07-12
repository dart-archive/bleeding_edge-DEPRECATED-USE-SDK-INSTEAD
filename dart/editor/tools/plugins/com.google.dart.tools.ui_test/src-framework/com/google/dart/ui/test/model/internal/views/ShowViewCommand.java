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
package com.google.dart.ui.test.model.internal.views;

import com.google.dart.ui.test.model.internal.workbench.CommandException;
import com.google.dart.ui.test.model.internal.workbench.CommandRunnable;
import com.google.dart.ui.test.model.internal.workbench.WorkbenchCommand;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.internal.ShowViewAction;
import org.eclipse.ui.views.IViewDescriptor;

/**
 * Command to show a view.
 */
@SuppressWarnings("restriction")
public class ShowViewCommand extends WorkbenchCommand {

  private final IViewDescriptor view;

  public ShowViewCommand(IViewDescriptor view) {
    this.view = view;
  }

  @Override
  public void run(final IWorkbenchWindow window) throws CommandException {
    ensureViewExists();
    syncExec(new CommandRunnable() {
      @Override
      public void run() {
        new ShowViewAction(window, view, false) {
        }.run();
      }
    });
  }

  private void ensureViewExists() throws CommandException {
    if (view == null) {
      throw new CommandException("View (" + view + "} not found in registry");
    }
  }

}
