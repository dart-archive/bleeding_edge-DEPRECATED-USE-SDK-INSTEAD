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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Handles the change to the state of the Always Run Last Launch menu item
 */
public class SetRunLastLaunchHandler extends AbstractHandler {

  public static String commandId = "com.google.dart.tools.debug.ui.run.last.launch";

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {

    Command command = event.getCommand();
    DartRunAction.setRunLastLaunch(!HandlerUtil.toggleCommandState(command));
    return null;
  }

}
