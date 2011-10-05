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
package com.google.dart.tools.debug.ui.internal.view;

import org.eclipse.debug.internal.ui.views.variables.VariablesView;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;

/**
 * This custom subclass of the debugger VariablesView allows us to customize the actions that the
 * view exposes, and override some visibility behavior that is necessary to embed this view in other
 * views.
 */
@SuppressWarnings("restriction")
public class DartVariablesView extends VariablesView {
  private boolean visible;

  /**
   * Create a new DartVariablesView instance.
   */
  public DartVariablesView() {

  }

  @Override
  public void becomesHidden() {
    visible = false;

    super.becomesHidden();
  }

  @Override
  public void becomesVisible() {
    visible = true;

    super.becomesVisible();
  }

  @Override
  public boolean isVisible() {
    return visible;
  }

  @Override
  protected void configureToolBar(IToolBarManager tbm) {
    tbm.add(new Separator(this.getClass().getName()));
    tbm.add(new Separator(IDebugUIConstants.RENDER_GROUP));

    //tbm.add(getAction("ShowTypeNames")); //$NON-NLS-1$
    tbm.add(getAction("ToggleContentProviders")); //$NON-NLS-1$
    //tbm.add(getAction("CollapseAll")); //$NON-NLS-1$
  }

}
