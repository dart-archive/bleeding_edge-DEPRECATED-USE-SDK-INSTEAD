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
package com.google.dart.tools.ui.feedback;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.widgets.Shell;

/**
 * An action to open the {@link FeedbackDialog}.
 */
public class OpenFeedbackDialogAction extends Action implements IShellProvider {

  private IShellProvider shellProvider;

  public OpenFeedbackDialogAction() {
  }

  public OpenFeedbackDialogAction(IShellProvider shellProvider) {
    setShellProvider(shellProvider);
  }

  @Override
  public Shell getShell() {
    return shellProvider.getShell();
  }

  @Override
  public void run() {
    new FeedbackDialog(getShell()).open();
  }

  /**
   * @param shellProvider the shellProvider to set
   */
  public void setShellProvider(IShellProvider shellProvider) {
    this.shellProvider = shellProvider;
  }

}
