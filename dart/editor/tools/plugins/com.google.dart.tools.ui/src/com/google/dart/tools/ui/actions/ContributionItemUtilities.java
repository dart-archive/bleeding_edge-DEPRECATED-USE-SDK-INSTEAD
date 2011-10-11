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
package com.google.dart.tools.ui.actions;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.internal.handlers.IActionCommandMappingService;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;

/**
 * Utilities for generating and manipulating IContributionItems.
 */
// Added annotation to remove warnings for using classes under org.eclipse.ui.internal.handlers
@SuppressWarnings("restriction")
public class ContributionItemUtilities {

  /**
   * Given a command id, image, label, and a tooltip, generates an IContributionItem for addition to
   * a Menu with the provided action id.
   *
   * @param window The workbench window
   * @param actionId The action id for the returned contribution item
   * @param commandId The command id for a defined command
   * @param image An image for the contribution item, or null
   * @param disabledImage A disabled image for the contribution item, or null
   * @param label A label for the contribution item
   * @param tooltip A tooltip/description for the contribution item
   * @param helpContextId The help context ID for the contribution item, or null
   * @return
   */
  public static IContributionItem getItem(IWorkbenchWindow window, String actionId,
      String commandId, String image, String disabledImage, String label, String tooltip,
      String helpContextId) {
    ISharedImages sharedImages = window.getWorkbench().getSharedImages();

    IActionCommandMappingService acms = (IActionCommandMappingService) window.getService(IActionCommandMappingService.class);
    acms.map(actionId, commandId);

    CommandContributionItemParameter commandParm = new CommandContributionItemParameter(window,
        actionId, commandId, null, sharedImages.getImageDescriptor(image),
        sharedImages.getImageDescriptor(disabledImage), null, label, null, tooltip,
        CommandContributionItem.STYLE_PUSH, null, false);
    return new CommandContributionItem(commandParm);
  }

  private ContributionItemUtilities() {
    // Not instantiable
  }
}
