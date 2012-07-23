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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import java.io.IOException;

/**
 * Display information about the embedded web server.
 */
public class ShowWebConsoleHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
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

      return null;
    } catch (IOException ioe) {
      throw new ExecutionException(ioe.getMessage(), ioe);
    }
  }

}
