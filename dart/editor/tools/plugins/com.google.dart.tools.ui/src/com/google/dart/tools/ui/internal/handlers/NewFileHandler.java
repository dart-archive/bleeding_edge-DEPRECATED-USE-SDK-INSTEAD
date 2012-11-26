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
package com.google.dart.tools.ui.internal.handlers;

import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.actions.OpenNewFileWizardAction;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.actions.CommandAction;

/**
 * Opens the New File wizard.
 */
@SuppressWarnings("restriction")
public class NewFileHandler extends AbstractHandler {

  private static class NewFileCommandAction extends CommandAction {
    public NewFileCommandAction(IWorkbenchWindow window) {
      super(window, COMMAND_ID);
      setImageDescriptor(DartPluginImages.DESC_TOOL_NEW_FILE);
    }
  }

  public static final String COMMAND_ID = DartUI.class.getPackage().getName() + ".file.new"; //$NON-NLS-1$

  public static IAction createCommandAction(IWorkbenchWindow window) {
    return new NewFileCommandAction(window);
  }

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
    if (window == null) {
      window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    }

    new OpenNewFileWizardAction(window).run();
    return null;
  }
}
