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
package com.google.dart.eclipse.ui.internal.handlers;

import com.google.dart.tools.ui.internal.actions.OpenTypeAction;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class OpenTypeDialogHandler extends AbstractHandler implements
    IWorkbenchWindowActionDelegate {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    doRun();
    return null;
  }

  @Override
  public void init(IWorkbenchWindow window) {
    //no-op
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
    new OpenTypeAction().run();
  }

}
