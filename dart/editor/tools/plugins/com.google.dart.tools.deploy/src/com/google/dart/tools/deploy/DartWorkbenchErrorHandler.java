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
package com.google.dart.tools.deploy;

import org.eclipse.ui.statushandlers.StatusAdapter;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.statushandlers.WorkbenchErrorHandler;

/**
 * A custom error handler.
 */
public class DartWorkbenchErrorHandler extends WorkbenchErrorHandler {

  @Override
  public void handle(StatusAdapter statusAdapter, int style) {

    //demote platform events that would otherwise popup dialogs to log-only events
    //TODO: consider adding our own wrapper dialog that informs the user that something bad has happened
    //and what to do about it
    if (((style & StatusManager.BLOCK) == StatusManager.BLOCK)
        || ((style & StatusManager.SHOW) == StatusManager.SHOW)) {
      style = StatusManager.LOG;
    }

    super.handle(statusAdapter, style);
  }

}
