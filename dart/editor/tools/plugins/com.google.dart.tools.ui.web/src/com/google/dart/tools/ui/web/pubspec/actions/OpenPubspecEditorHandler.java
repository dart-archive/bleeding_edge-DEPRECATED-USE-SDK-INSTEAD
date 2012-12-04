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
package com.google.dart.tools.ui.web.pubspec.actions;

import com.google.dart.tools.ui.web.DartWebPlugin;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Handler for action that opens the PubspecEditor
 */
public class OpenPubspecEditorHandler extends AbstractHandler {

  /**
   * The id of the open pubspec editor action.
   */
  public static final String ID = DartWebPlugin.PLUGIN_ID + ".OpenPubspecEditorAction"; //$NON-NLS-1$

  private OpenInPubspecEditorAction openAction;

  public OpenPubspecEditorHandler() {

  }

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    openAction = new OpenInPubspecEditorAction(HandlerUtil.getActiveWorkbenchWindow(event));
    openAction.run();
    return null;
  }

}
