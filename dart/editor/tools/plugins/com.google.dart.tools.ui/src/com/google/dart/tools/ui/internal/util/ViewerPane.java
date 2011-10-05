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
package com.google.dart.tools.ui.internal.util;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;

/**
 * A <code>ViewerPane</code> is a convenience class which installs a <code>CLabel</code> and a
 * <code>Toolbar</code> in a <code>ViewForm</code>.
 * <P>
 */
public class ViewerPane extends ViewForm {

  private ToolBarManager fToolBarManager;

  public ViewerPane(Composite parent, int style) {
    super(parent, style);

    marginWidth = 0;
    marginHeight = 0;

    CLabel label = new CLabel(this, SWT.NONE);
    setTopLeft(label);

    ToolBar tb = new ToolBar(this, SWT.FLAT);
    setTopCenter(tb);
    fToolBarManager = new ToolBarManager(tb);
  }

  public Image getImage() {
    CLabel cl = (CLabel) getTopLeft();
    return cl.getImage();
  }

  public String getText() {
    CLabel cl = (CLabel) getTopLeft();
    return cl.getText();
  }

  public ToolBarManager getToolBarManager() {
    return fToolBarManager;
  }

  /**
   * Sets the receiver's title image.
   */
  public void setImage(Image image) {
    CLabel cl = (CLabel) getTopLeft();
    cl.setImage(image);
  }

  /**
   * Sets the receiver's title text.
   */
  public void setText(String label) {
    CLabel cl = (CLabel) getTopLeft();
    cl.setText(label);
  }
}
