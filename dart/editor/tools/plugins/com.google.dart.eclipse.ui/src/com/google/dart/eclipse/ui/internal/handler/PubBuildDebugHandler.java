/*
 * Copyright (c) 2015, the Dart project authors.
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
package com.google.dart.eclipse.ui.internal.handler;

import com.google.dart.eclipse.DartEclipseUI;
import com.google.dart.tools.ui.actions.RunPubAction;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * A handler to run pub build in debug mode.
 */
public class PubBuildDebugHandler extends AbstractHandler {

  /**
   * The id of the pub build debug action.
   */
  public static final String ID = DartEclipseUI.PLUGIN_ID + ".PubBuildDebugAction"; //$NON-NLS-1$

  private RunPubAction pubBuildDebugAction;

  public PubBuildDebugHandler() {

  }

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    getRunAction(HandlerUtil.getActiveWorkbenchWindow(event)).run();
    return null;
  }

  private RunPubAction getRunAction(IWorkbenchWindow window) {
    if (pubBuildDebugAction == null) {
      pubBuildDebugAction = RunPubAction.createPubBuildDebugAction(window);
      pubBuildDebugAction.setId(ID);
    }

    return pubBuildDebugAction;
  }

}
