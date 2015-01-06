/*
 * Copyright (c) 2014, the Dart project authors.
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

import com.google.dart.engine.element.Element;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.DartUIMessages;
import com.google.dart.tools.ui.internal.dialogs.FilteredTypesSelectionDialog_NEW;
import com.google.dart.tools.ui.internal.dialogs.OpenTypeSelectionDialog;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Open top level declarations handler
 */
public class OpenTopLevelDeclarationsHandler extends AbstractHandler {

  private static final int SEARCH_ELEMENT_KINDS = 0;

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {

    SelectionDialog dialog;

    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      dialog = new FilteredTypesSelectionDialog_NEW(HandlerUtil.getActiveShell(event), false,
          PlatformUI.getWorkbench().getProgressService(), SEARCH_ELEMENT_KINDS, null);
    } else {
      dialog = new OpenTypeSelectionDialog(HandlerUtil.getActiveShell(event), true,
          PlatformUI.getWorkbench().getProgressService(), null, SEARCH_ELEMENT_KINDS);
    }

    dialog.setTitle(DartUIMessages.OpenTypeAction_dialogTitle);
    dialog.setMessage(DartUIMessages.OpenTypeAction_dialogMessage);

    int result = dialog.open();
    if (result != IDialogConstants.OK_ID) {
      return null;
    }

    Object[] types = dialog.getResult();
    if (types == null || types.length == 0) {
      return null;
    }

    if (types.length == 1) {
      try {
        if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
          DartUI.openInEditor((com.google.dart.server.generated.types.Element) types[0], true);
        } else {
          DartUI.openInEditor((Element) types[0], true, true);
        }
      } catch (Exception x) {
        ExceptionHandler.handle(x, DartUIMessages.OpenTypeAction_errorTitle,
            DartUIMessages.OpenTypeAction_errorMessage);
      }
    }

    return null;
  }
}
