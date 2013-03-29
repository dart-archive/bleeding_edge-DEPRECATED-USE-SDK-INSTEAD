/*
 * Copyright 2013 Dart project authors.
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

import com.google.dart.tools.debug.ui.internal.dialogs.RemoteConnectionDialog;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PlatformUI;

/**
 * Bring up a dialog to connect to a remote debug host.
 */
public class RemoteConnectionHandler extends AbstractHandler {

  public RemoteConnectionHandler() {

  }

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    RemoteConnectionDialog.show(PlatformUI.getWorkbench());

    return null;
  }

}
