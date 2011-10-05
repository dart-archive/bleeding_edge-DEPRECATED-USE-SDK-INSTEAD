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

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * A toolbar separator item that uses whitespace to separate components (as opposed to a vertical
 * line).
 */
public class BlankSeparator extends ContributionItem {

  public BlankSeparator() {

  }

  @Override
  public void fill(ToolBar toolBar, int index) {
    final ToolItem tbItem = new ToolItem(toolBar, SWT.SEPARATOR);
    final Composite trimArea = new Composite(toolBar, SWT.NO_BACKGROUND);
    tbItem.setControl(trimArea);
    tbItem.setWidth(12);
  }

}
