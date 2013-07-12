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
import com.google.dart.ui.test.model.internal.workbench.WorkbenchCommand;
import com.google.dart.ui.test.model.internal.workbench.WorkbenchFinder;
import com.google.dart.ui.test.runnable.UIThreadRunnable;
import com.google.dart.ui.test.runnable.VoidResult;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.views.IViewDescriptor;

/**
 * Command to close a view.
 */
public class CloseViewCommand extends WorkbenchCommand {

  public static CloseViewCommand forView(IViewDescriptor view) {
    return new CloseViewCommand(view);
  }

  private final IViewDescriptor view;

  public CloseViewCommand(IViewDescriptor view) {
    this.view = view;
  }

  @Override
  public void run(final IWorkbenchWindow window) throws CommandException {

    ensureViewExists();

    final IWorkbenchPage page = WorkbenchFinder.getActivePage();
    if (page == null) {
      throw new CommandException("unable to find active workbench page");
    }

    // Notice that this is done as an async in case the view is dirty
    // and forces a prompt
    UIThreadRunnable.asyncExec(new VoidResult() {
      @Override
      public void run() {
        page.hideView(ViewFinder.getViewRef(view));
      }
    });

  }

  private void ensureViewExists() throws CommandException {
    if (view == null) {
      throw new CommandException("View (" + view + "} not found in registry");
    }
  }

}
