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

package com.google.dart.tools.debug.ui.launch;

import com.google.dart.tools.debug.ui.internal.chromeapp.ChromeAppLaunchShortcut;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.resources.IFile;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.viewers.StructuredSelection;

import java.util.List;

/**
 * A command handler to launch chrome applications
 */
public class RunInChromeAppHandler extends AbstractHandler {

  public RunInChromeAppHandler() {

  }

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    if (event.getApplicationContext() instanceof IEvaluationContext) {
      IEvaluationContext context = (IEvaluationContext) event.getApplicationContext();

      if (context.getDefaultVariable() instanceof List<?>) {
        List<?> variables = (List<?>) context.getDefaultVariable();

        if (variables.size() > 0 && variables.get(0) instanceof IFile) {
          IFile file = (IFile) variables.get(0);

          launch(file);
        }
      }
    }

    return null;
  }

  private void launch(IFile file) {
    ChromeAppLaunchShortcut shortcut = new ChromeAppLaunchShortcut();

    shortcut.launch(new StructuredSelection(file), ILaunchManager.DEBUG_MODE);
  }

}
