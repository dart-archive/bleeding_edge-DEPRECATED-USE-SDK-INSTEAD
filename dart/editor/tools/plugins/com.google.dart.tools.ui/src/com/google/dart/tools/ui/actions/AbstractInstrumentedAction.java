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

package com.google.dart.tools.ui.actions;

import com.google.dart.tools.core.DartCore;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

import java.util.HashMap;

/**
 * 
 */
public class AbstractInstrumentedAction extends Action {

  //TODO (pquitslund): unused so disabled (NOTE: e4 incompatible)
  private static final boolean INSTRUMENTATION_ENABLED = false;

  public AbstractInstrumentedAction() {

  }

  public AbstractInstrumentedAction(String name) {
    super(name);
  }

  public AbstractInstrumentedAction(String name, int flags) {
    super(name, flags);
  }

  protected void EmitInstrumentationCommand() {

    if (INSTRUMENTATION_ENABLED) {

      try {
        //It doesn't matter which window this command is fired from
        ICommandService cmd = (ICommandService) PlatformUI.getWorkbench().getWorkbenchWindows()[0].getService(ICommandService.class);
        Command ic = cmd.getCommand("com.google.dart.tools.ui.BroadcastCommand");

        HashMap<String, String> props = new HashMap<String, String>();
        props.put("ActionType", this.getClass().toString());
        if (ic.getHandler() != null) {
          ExecutionEvent ev = new ExecutionEvent(ic, props, this, PlatformUI.getWorkbench());

          ic.executeWithChecks(ev);
        }
      } catch (Exception e) {
        DartCore.logError(e);
      }
    }

  }

}
