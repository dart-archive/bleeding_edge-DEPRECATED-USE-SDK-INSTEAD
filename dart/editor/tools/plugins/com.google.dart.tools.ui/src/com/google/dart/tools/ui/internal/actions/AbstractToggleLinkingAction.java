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
package com.google.dart.tools.ui.internal.actions;

import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.actions.ActionMessages;
import com.google.dart.tools.ui.internal.text.IJavaHelpContextIds;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.PlatformUI;

/**
 * This is an action template for actions that toggle whether it links its selection to the active
 * editor.
 */
public abstract class AbstractToggleLinkingAction extends Action {

  /**
   * Constructs a new action.
   */
  public AbstractToggleLinkingAction() {
    super(ActionMessages.ToggleLinkingAction_label);
    setDescription(ActionMessages.ToggleLinkingAction_description);
    setToolTipText(ActionMessages.ToggleLinkingAction_tooltip);
    DartPluginImages.setLocalImageDescriptors(this, "synced.gif"); //$NON-NLS-1$		
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.LINK_EDITOR_ACTION);
  }

  /**
   * Runs the action.
   */
  @Override
  public abstract void run();
}
