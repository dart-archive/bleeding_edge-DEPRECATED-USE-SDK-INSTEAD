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
package com.google.dart.tools.ui.internal.text.correction;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.editor.CompilationUnitEditor;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.LegacyHandlerSubmissionExpression;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @coverage dart.editor.ui.correction
 */
public class CorrectionCommandInstaller {

  /**
   * All correction commands must start with the following prefix.
   */
  public static final String COMMAND_PREFIX = "org.eclipse.jdt.ui.correction."; //$NON-NLS-1$

  /**
   * Commands for quick assist must have the following suffix.
   */
  public static final String ASSIST_SUFFIX = ".assist"; //$NON-NLS-1$

  private List<IHandlerActivation> fCorrectionHandlerActivations;

  public CorrectionCommandInstaller() {
    fCorrectionHandlerActivations = null;
  }

  public void deregisterCommands() {
    IHandlerService handlerService = (IHandlerService) PlatformUI.getWorkbench().getAdapter(
        IHandlerService.class);
    if (handlerService != null && fCorrectionHandlerActivations != null) {
      handlerService.deactivateHandlers(fCorrectionHandlerActivations);
      fCorrectionHandlerActivations = null;
    }
  }

  public void registerCommands(CompilationUnitEditor editor) {
    IWorkbench workbench = PlatformUI.getWorkbench();
    ICommandService commandService = (ICommandService) workbench.getAdapter(ICommandService.class);
    IHandlerService handlerService = (IHandlerService) workbench.getAdapter(IHandlerService.class);
    if (commandService == null || handlerService == null) {
      return;
    }

    if (fCorrectionHandlerActivations != null) {
      DartToolsPlugin.logErrorMessage("correction handler activations not released"); //$NON-NLS-1$
    }
    fCorrectionHandlerActivations = new ArrayList<IHandlerActivation>();

    @SuppressWarnings("unchecked")
    Collection<String> definedCommandIds = commandService.getDefinedCommandIds();
    for (Iterator<String> iter = definedCommandIds.iterator(); iter.hasNext();) {
      String id = iter.next();
      if (id.startsWith(COMMAND_PREFIX)) {
        boolean isAssist = id.endsWith(ASSIST_SUFFIX);
        CorrectionCommandHandler handler = new CorrectionCommandHandler(editor, id, isAssist);
        IHandlerActivation activation = handlerService.activateHandler(
            id,
            handler,
            new LegacyHandlerSubmissionExpression(null, null, editor.getSite()));
        fCorrectionHandlerActivations.add(activation);
      }
    }
  }

}
