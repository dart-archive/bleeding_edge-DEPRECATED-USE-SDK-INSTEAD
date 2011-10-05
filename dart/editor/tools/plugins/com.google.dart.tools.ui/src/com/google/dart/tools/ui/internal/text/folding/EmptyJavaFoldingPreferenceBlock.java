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
package com.google.dart.tools.ui.internal.text.folding;

import com.google.dart.tools.ui.text.folding.IDartFoldingPreferenceBlock;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/**
 * Empty preference block for extensions to the
 * <code>org.eclipse.wst.jsdt.ui.javaFoldingStructureProvider</code> extension point that do not
 * specify their own.
 */
class EmptyJavaFoldingPreferenceBlock implements IDartFoldingPreferenceBlock {
  /*
   * @see com.google.dart.tools.ui.internal.text.folding.IJavaFoldingPreferences#
   * createControl(org.eclipse.swt.widgets.Group)
   */
  @Override
  public Control createControl(Composite composite) {
    Composite inner = new Composite(composite, SWT.NONE);
    inner.setLayout(new GridLayout(3, false));

    Label label = new Label(inner, SWT.CENTER);
    GridData gd = new GridData(GridData.FILL_BOTH);
    gd.widthHint = 30;
    label.setLayoutData(gd);

    label = new Label(inner, SWT.CENTER);
    label.setText(FoldingMessages.EmptyJavaFoldingPreferenceBlock_emptyCaption);
    gd = new GridData(GridData.CENTER);
    label.setLayoutData(gd);

    label = new Label(inner, SWT.CENTER);
    gd = new GridData(GridData.FILL_BOTH);
    gd.widthHint = 30;
    label.setLayoutData(gd);

    return inner;
  }

  /*
   * @see com.google.dart.tools.ui.internal.text.folding.IJavaFoldingPreferenceBlock #dispose()
   */
  @Override
  public void dispose() {
  }

  /*
   * @see com.google.dart.tools.ui.internal.text.folding.IJavaFoldingPreferenceBlock #initialize()
   */
  @Override
  public void initialize() {
  }

  /*
   * @see com.google.dart.tools.ui.internal.text.folding.IJavaFoldingPreferenceBlock
   * #performDefaults()
   */
  @Override
  public void performDefaults() {
  }

  /*
   * @see com.google.dart.tools.ui.internal.text.folding.IJavaFoldingPreferenceBlock #performOk()
   */
  @Override
  public void performOk() {
  }

}
