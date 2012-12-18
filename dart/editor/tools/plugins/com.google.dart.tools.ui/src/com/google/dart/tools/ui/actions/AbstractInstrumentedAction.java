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
 * An abstract class to conditionally emit instrumentation information.
 */
public abstract class AbstractInstrumentedAction extends Action {
  private static final boolean INSTRUMENTATION_ENABLED = true;

  private static final String INSTRUMENTATION_COMMAND_ID = "com.google.dart.tools.ui.BroadcastCommand";
  private static final String INSTRUMENTATION_ACTION_TYPE = "ActionType";

  public AbstractInstrumentedAction() {

  }

  public AbstractInstrumentedAction(String name) {
    super(name);
  }

  public AbstractInstrumentedAction(String name, int flags) {
    super(name, flags);
  }

  protected void emitInstrumentationCommand() {
    if (INSTRUMENTATION_ENABLED) {
      try {
        ICommandService cmd = (ICommandService) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getService(
            ICommandService.class);
        Command ic = cmd.getCommand(INSTRUMENTATION_COMMAND_ID);

        if (ic.getHandler() != null) {
          HashMap<String, String> props = new HashMap<String, String>();
          props.put(INSTRUMENTATION_ACTION_TYPE, this.getClass().toString());

          ExecutionEvent ev = new ExecutionEvent(ic, props, this, PlatformUI.getWorkbench());
          ic.executeWithChecks(ev);
        }
      } catch (Exception e) {
        DartCore.logError(e);
      }
    }
  }

}
