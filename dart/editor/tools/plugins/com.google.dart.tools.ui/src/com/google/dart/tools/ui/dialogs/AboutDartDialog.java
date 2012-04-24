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
package com.google.dart.tools.ui.dialogs;

import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.model.DartSdk;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.Geometry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * A minimal "About Dart" dialog shell.
 */
public class AboutDartDialog extends Shell {

  private static final ImageDescriptor ABOUT_IMG_DESC = ImageDescriptor.createFromURL(Platform.getBundle(
      DartUI.ID_PLUGIN).getEntry(DialogsMessages.AboutDartDialog_about_image));

  private static final String NEW_LINE = System.getProperty("line.separator");

  public AboutDartDialog(Shell shell) {
    super(shell, SWT.CLOSE | SWT.TITLE);

    setText(DialogsMessages.AboutDartDialog_title_text);
    setLayout(GridLayoutFactory.fillDefaults().spacing(0, 5).margins(0, 0).create());

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

    Point size = DartCoreDebug.ENABLE_UPDATE ? new Point(394, 420) : new Point(394, 364);

    setSize(size);

    setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

    Label graphic = newLabel(SWT.SHADOW_NONE | SWT.CENTER);
    GridDataFactory.fillDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).hint(316, 416).applyTo(
        graphic);
    graphic.setImage(DartToolsPlugin.getImageDescriptorRegistry().get(ABOUT_IMG_DESC));

    Label productNameLabel = newLabel(SWT.BOLD);
    productNameLabel.setFont(JFaceResources.getBannerFont());
    productNameLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
    productNameLabel.setText(DialogsMessages.AboutDartDialog_product_label);

    StyledText buildDetailsText = new StyledText(this, SWT.NONE);
    buildDetailsText.setLineSpacing(7);
    buildDetailsText.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
    buildDetailsText.setEditable(false);
    center(buildDetailsText);

    StringBuilder builder = new StringBuilder();

    builder.append(DialogsMessages.AboutDartDialog_version_string_prefix + getVersion() + ", "
        + "Build " + DartToolsPlugin.getBuildId());

    builder.append(NEW_LINE);

    if (DartSdk.isInstalled()) {
      builder.append("Dart SDK version " + DartSdk.getInstance().getSdkVersion());

      builder.append(", ");

      if (DartSdk.getInstance().isDartiumInstalled()) {
        builder.append("Dartium version " + DartSdk.getInstance().getDartiumVersion());
      } else {
        builder.append("Dartium is not installed");
      }

    } else {
      builder.append("Dart SDK is not installed");
    }

    buildDetailsText.setText(builder.toString());

    buildDetailsText.getCaret().setSize(0, 0); //nuke the caret

    // spacer
    newLabel(SWT.NONE);

    Label copyrightLabel = newLabel(SWT.NONE);
    center(copyrightLabel);
    copyrightLabel.setText(DialogsMessages.AboutDartDialog_copyright);

    Label copyrightLabel2 = newLabel(SWT.NONE);
    center(copyrightLabel2);
    copyrightLabel2.setText(DialogsMessages.AboutDartDialog_copyright_line2);

    //spacer
    newLabel(SWT.NONE);

    if (DartCoreDebug.ENABLE_UPDATE) {

      @SuppressWarnings("unused")
      UpdateStatusControl updateStatus = new UpdateStatusControl(this);

      //spacer
      newLabel(SWT.NONE);
    }

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

  private void center(Control control) {
    GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).applyTo(control);
  }

  private String getVersion() {
    return DartToolsPlugin.getVersionString();
  }

  private Label newLabel(int style) {
    Label label = new Label(this, style);
    label.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
    return label;
  }
}
