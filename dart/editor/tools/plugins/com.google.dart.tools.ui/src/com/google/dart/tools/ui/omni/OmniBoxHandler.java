/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.omni;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Handler for the omnibox pop-up dialog.
 */
public class OmniBoxHandler extends AbstractHandler {

  private IWorkbenchWindow window;

  @Override
  public Object execute(ExecutionEvent executionEvent) {
    window = HandlerUtil.getActiveWorkbenchWindow(executionEvent);
    if (window == null) {
      return null;
    }

    OmniBoxControlContribution control = OmniBoxControlContribution.getControlForWindow(window);
    if (control != null) {
      control.giveFocus();
    }

    return null;
  }
}
