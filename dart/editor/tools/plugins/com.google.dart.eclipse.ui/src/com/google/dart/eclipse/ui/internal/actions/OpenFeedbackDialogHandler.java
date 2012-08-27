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
package com.google.dart.eclipse.ui.internal.actions;

import com.google.dart.tools.ui.feedback.OpenFeedbackDialogAction;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Hadnler to open the send feedback dialog.
 */
public class OpenFeedbackDialogHandler extends AbstractHandler implements
    IWorkbenchWindowActionDelegate {

  private IWorkbenchWindow window;

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    init(HandlerUtil.getActiveWorkbenchWindowChecked(event));
    doRun();
    return null;
  }

  @Override
  public void init(IWorkbenchWindow window) {
    this.window = window;
  }

  @Override
  public void run(IAction action) {
    doRun();
  }

  @Override
  public void selectionChanged(IAction action, ISelection selection) {
    //no-op
  }

  private void doRun() {
    new OpenFeedbackDialogAction(window).run();
  }
}
