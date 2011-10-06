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
package com.google.dart.tools.ui.dialogs;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.Geometry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * A minimal "About Dart" dialog shell.
 */
public class AboutDartDialog extends Shell {

  private static final ImageDescriptor ABOUT_IMG_DESC = ImageDescriptor.createFromURL(Platform.getBundle(
      DartUI.ID_PLUGIN).getEntry(DialogsMessages.AboutDartDialog_about_image));

  public AboutDartDialog(Shell shell) {
    super(shell, SWT.CLOSE | SWT.TITLE);

    setText(DialogsMessages.AboutDartDialog_title_text);
    setLayout(GridLayoutFactory.fillDefaults().spacing(0, 0).margins(0, 0).create());

    createContents();
  }

  @Override
  protected void checkSubclass() {
    // Disable the check that prevents subclassing of SWT components
  }

  /**
   * Create contents of the shell.
   */
  protected void createContents() {
    setSize(394, 364);
    Label graphic = new Label(this, SWT.SHADOW_NONE | SWT.CENTER);
    GridDataFactory.fillDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).hint(316, 416).applyTo(
        graphic);
    graphic.setImage(DartToolsPlugin.getImageDescriptorRegistry().get(ABOUT_IMG_DESC));

    Label productNameLabel = new Label(this, SWT.BOLD);
    productNameLabel.setFont(JFaceResources.getBannerFont());
    productNameLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
    productNameLabel.setText(DialogsMessages.AboutDartDialog_product_label);

    Label versionLabel = new Label(this, SWT.NONE);
    GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).indent(5, 8).applyTo(versionLabel);
    versionLabel.setText(DialogsMessages.AboutDartDialog_version_string_prefix + getVersion());

    Label copyrightLabel = new Label(this, SWT.NONE);
    GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).indent(5, 5).applyTo(
        copyrightLabel);
    copyrightLabel.setText(DialogsMessages.AboutDartDialog_copyright);

    Label copyrightLabel2 = new Label(this, SWT.NONE);
    GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).indent(5, 3).applyTo(
        copyrightLabel2);
    copyrightLabel2.setText(DialogsMessages.AboutDartDialog_copyright_line2);

    //spacer
    new Label(this, SWT.NONE);

    setLocation(getInitialLocation(getSize()));
  }

  protected Point getInitialLocation(Point initialSize) {
    Composite parent = getParent();

    Rectangle parentBounds = parent.getClientArea();
    Point centerPoint = Geometry.centerPoint(parent.getBounds());

    return new Point(centerPoint.x - (initialSize.x / 2), Math.max(
        parentBounds.y,
        Math.min(centerPoint.y - (initialSize.y * 2 / 3), parentBounds.y + parentBounds.height
            - initialSize.y)));
  }

  private String getVersion() {
    return DartToolsPlugin.getVersionString();
  }
}
