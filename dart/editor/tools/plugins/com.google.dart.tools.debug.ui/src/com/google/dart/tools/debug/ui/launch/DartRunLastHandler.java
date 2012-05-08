/*
 * Copyright 2012 Dart project authors.
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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PlatformUI;

/**
 * A command handler to invoke the run last action.
 */
public class DartRunLastHandler extends AbstractHandler {
  private DartRunLastAction runLastAction;

  /**
   * Create a new DartRunHandler.
   */
  public DartRunLastHandler() {

  }

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    getRunAction().run();

    return null;
  }

  private DartRunLastAction getRunAction() {
    if (runLastAction == null) {
      runLastAction = new DartRunLastAction(
          PlatformUI.getWorkbench().getActiveWorkbenchWindow(),
          true);
    }

    return runLastAction;
  }

}
