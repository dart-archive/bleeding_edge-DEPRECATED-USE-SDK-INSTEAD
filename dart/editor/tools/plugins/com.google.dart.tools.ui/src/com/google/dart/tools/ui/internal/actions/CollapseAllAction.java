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

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.PlatformUI;

/**
 * An {@link Action} that will collapse all nodes in a given {@link TreeViewer}.
 * <p>
 * (see Eclipse Bug 210255, this bug was the request for this feature in the JDT.)
 */
public final class CollapseAllAction extends Action {

  private final TreeViewer viewer;

  public CollapseAllAction(TreeViewer viewer) {
    super(ActionMessages.CollapseAllAction_label, DartPluginImages.DESC_ELCL_COLLAPSEALL);
    Assert.isNotNull(viewer);
    setToolTipText(ActionMessages.CollapseAllAction_tooltip);
    setDescription(ActionMessages.CollapseAllAction_description);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.COLLAPSE_ALL_ACTION);
    this.viewer = viewer;
  }

  @Override
  public void run() {
    try {
      viewer.getControl().setRedraw(false);
      viewer.collapseAll();
    } finally {
      viewer.getControl().setRedraw(true);
    }
  }

}
