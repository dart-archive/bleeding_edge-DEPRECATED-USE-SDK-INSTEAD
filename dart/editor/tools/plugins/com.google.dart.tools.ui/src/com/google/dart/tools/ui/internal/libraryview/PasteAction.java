/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.ui.internal.libraryview;

import com.google.dart.tools.core.model.DartModel;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.actions.SelectionDispatchAction;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchSite;

/**
 * An action to paste some set of contents from the OS clipboard into the {@link DartModel}.
 * <p>
 * TODO This action is not yet implemented.
 */
public final class PasteAction extends SelectionDispatchAction {

  /**
   * The id of this action.
   */
  public static final String ID = DartToolsPlugin.PLUGIN_ID + ".PasteAction"; //$NON-NLS-1$

  public PasteAction(IWorkbenchSite site) {
    super(site);
    setId(ID);
    setText(CCPMessages.PasteAction_text);
    setDescription(CCPMessages.PasteAction_description);
  }

  @Override
  public void run(IStructuredSelection selection) {
    // do nothing
    return;
  }

  @Override
  public void selectionChanged(IStructuredSelection selection) {
    setEnabled(false);
    return;
  }

}
