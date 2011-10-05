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
package com.google.dart.tools.ui.internal.libraryview;

import com.google.dart.tools.ui.ILibrariesViewPart;
import com.google.dart.tools.ui.internal.actions.AbstractToggleLinkingAction;

/**
 * This action toggles whether this package explorer links its selection to the active editor.
 * <p>
 * This action was originally copied over from
 * <code>org.eclipse.jdt.internal.ui.packageview.ToggleLinkingAction</code>.
 */
public class ToggleLinkingAction extends AbstractToggleLinkingAction {

  private ILibrariesViewPart libraryExplorerPart;

  /**
   * Constructs a new toggle linking action.
   * 
   * @param explorer the library explorer
   */
  public ToggleLinkingAction(ILibrariesViewPart explorer) {
    setChecked(explorer.isLinkingEnabled());
    libraryExplorerPart = explorer;
  }

  @Override
  public void run() {
    libraryExplorerPart.setLinkingEnabled(isChecked());
  }

}
