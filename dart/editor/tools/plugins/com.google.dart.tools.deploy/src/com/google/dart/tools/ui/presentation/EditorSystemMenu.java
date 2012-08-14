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
package com.google.dart.tools.ui.presentation;

import org.eclipse.jface.action.Separator;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.internal.presentations.SystemMenuCloseAll;
import org.eclipse.ui.internal.presentations.SystemMenuCloseOthers;
import org.eclipse.ui.presentations.IPresentablePart;
import org.eclipse.ui.presentations.IStackPresentationSite;

/**
 * Implements the editor view context menu.
 */
@SuppressWarnings("restriction")
public class EditorSystemMenu extends ViewSystemMenu {
  private SystemMenuCloseOthers closeOthers;
  private SystemMenuCloseAll closeAll;
  private ActionFactory.IWorkbenchAction duplicate;

  /**
   * Create the system editor menu
   * 
   * @param site the associated site
   */
  public EditorSystemMenu(IStackPresentationSite site) {
    super(site);
  }

  @Override
  public void dispose() {
    duplicate.dispose();

    super.dispose();
  }

  @Override
  public void show(Control parent, Point displayCoordinates, IPresentablePart currentSelection) {
    closeOthers.setTarget(currentSelection);
    closeAll.update();

    super.show(parent, displayCoordinates, currentSelection);
  }

  @Override
  protected void initialize(IStackPresentationSite site) {
    super.initialize(site);

    closeOthers = new SystemMenuCloseOthers(site);
    closeAll = new SystemMenuCloseAll(site);
    duplicate = ActionFactory.NEW_EDITOR.create(PlatformUI.getWorkbench().getActiveWorkbenchWindow());
    duplicate.setText("Duplicate"); //$NON-NLS-1$
    menuManager.add(closeOthers);
    menuManager.add(closeAll);
    menuManager.add(new Separator());
    menuManager.add(duplicate);
  }

}
