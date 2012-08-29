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

package com.google.dart.tools.debug.ui.internal.view;

import org.eclipse.debug.internal.ui.views.expression.ExpressionView;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;

/**
 * A custom subclass of the standard Eclipse Expressions view.
 */
@SuppressWarnings("restriction")
public class DartExpressionView extends ExpressionView {
  public static final String VIEW_ID = "com.google.dart.tools.debug.expressionsView";

  public DartExpressionView() {

  }

  @Override
  public Viewer createViewer(Composite parent) {
    Viewer viewer = super.createViewer(parent);

    IActionBars actionBars = getViewSite().getActionBars();

    actionBars.getMenuManager().removeAll();

    return viewer;
  }

}
