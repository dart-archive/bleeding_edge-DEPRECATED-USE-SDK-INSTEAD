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

package com.google.dart.tools.debug.ui.internal.util;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.debug.core.util.ResourceServer;
import com.google.dart.tools.debug.core.util.ResourceServerManager;
import com.google.dart.tools.debug.ui.internal.DartUtil;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import java.io.IOException;

/**
 * Display information about the embedded web server.
 */
public class ShowWebConsoleAction extends Action implements IWorkbenchWindowActionDelegate {

  public ShowWebConsoleAction() {

  }

  @Override
  public void dispose() {

  }

  @Override
  public void init(IWorkbenchWindow window) {

  }

  @Override
  public void run(IAction action) {
    try {
      ResourceServer server = ResourceServerManager.getServer();
      String localAddress = server.getLocalAddress();

      if (localAddress == null) {
        DartCore.getConsole().println("Unable to get local IP address.");
      } else {
        DartCore.getConsole().println(
            "Connect to the embedded web server at http://" + localAddress + ":" + server.getPort()
                + ".");
      }
    } catch (IOException ioe) {
      DartUtil.logError(ioe);
    }
  }

  @Override
  public void selectionChanged(IAction action, ISelection selection) {

  }

}
