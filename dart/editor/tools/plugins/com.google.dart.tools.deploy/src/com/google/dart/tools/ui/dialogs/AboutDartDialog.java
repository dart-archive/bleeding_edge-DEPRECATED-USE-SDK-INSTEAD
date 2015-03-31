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

import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.actions.CopyDetailsToClipboardAction;
import com.google.dart.tools.ui.actions.CopyDetailsToClipboardAction.DetailsProvider;
import com.google.dart.tools.ui.internal.util.ExternalBrowserUtil;
import com.google.dart.tools.update.core.UpdateAdapter;
import com.google.dart.tools.update.core.UpdateCore;
import com.google.dart.tools.update.core.UpdateListener;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.Geometry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;

/**
 * A minimal "About Dart" dialog shell.
 */
public class AboutDartDialog extends Shell implements DetailsProvider {

  private static final ImageDescriptor ABOUT_IMG_DESC = ImageDescriptor.createFromURL(Platform.getBundle(
      DartUI.ID_PLUGIN).getEntry(DialogsMessages.AboutDartDialog_about_image));

  private static final String NEW_LINE = System.getProperty("line.separator");

  private static String getChannelLabel() {
    //TODO (pquitslund): move to UpdateUtils
    String updateUrl = UpdateCore.getUpdateUrl();
    if (updateUrl != null) {
      if (updateUrl.contains("/channels/be/")) {
        return " (BLEEDING EDGE)";
      }
      if (updateUrl.contains("/channels/dev/")) {
        return " (DEV)";
      }
      if (updateUrl.contains("/channels/stable/")) {
        return " (STABLE)";
      }
    }
    //Fall through
    return "";
  }

  private final UpdateListener updateListener = new UpdateAdapter() {
    @Override
    public void installing() {
      AboutDartDialog.this.close();
    }
  };

  public AboutDartDialog(Shell shell) {
    super(shell, SWT.CLOSE | SWT.TITLE);

    setText(DialogsMessages.AboutDartDialog_title_text);
    setLayout(GridLayoutFactory.fillDefaults().spacing(0, 5).margins(0, 0).create());

    createContents();

  }

  @Override
  public void dispose() {
    UpdateCore.getUpdateManager().removeListener(updateListener);
    super.dispose();
  }

  //for copying to the clipboard
  @Override
  public String getDetails() {
    StringBuilder builder = new StringBuilder();

    builder.append(DialogsMessages.AboutDartDialog_version_string_prefix + getVersion());

    builder.append(NEW_LINE);

    if (DartSdkManager.getManager().hasSdk()) {
      builder.append("Dart SDK version " + DartSdkManager.getManager().getSdk().getSdkVersion());
    } else {
      builder.append("Dart SDK is not installed");
    }

    return builder.toString();
  }

  @Override
  public void open() {
    super.open();
    UpdateCore.getUpdateManager().addListener(updateListener);
  }

  @Override
  protected void checkSubclass() {
    // Disable the check that prevents subclassing of SWT components
  }

  /**
   * Create contents of the shell.
   */
  protected void createContents() {

    setSize(new Point(500, 550));

    addCopyDetailsPopup(this);

    setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

    Label graphic = newLabel(SWT.SHADOW_NONE | SWT.CENTER);
    GridDataFactory.fillDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).hint(316, 416).applyTo(
        graphic);
    graphic.setImage(DartToolsPlugin.getImage(ABOUT_IMG_DESC));

    addCopyDetailsPopup(graphic);

    Label productNameLabel = newLabel(SWT.BOLD);
    productNameLabel.setFont(JFaceResources.getBannerFont());
    productNameLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
    productNameLabel.setText(DialogsMessages.AboutDartDialog_product_label);

    addCopyDetailsPopup(productNameLabel);

    StyledText buildDetailsText = new StyledText(this, SWT.WRAP | SWT.CENTER);
    buildDetailsText.setLineSpacing(7);
    buildDetailsText.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
    buildDetailsText.setEditable(false);
    center(buildDetailsText);

    String buildDetails = getDetails();

    buildDetailsText.setText(buildDetails);
    buildDetailsText.setLineAlignment(1, 1, SWT.CENTER);

    addCopyDetailsPopup(buildDetailsText);

    // spacer
    newLabel(SWT.NONE);

    Label copyrightLabel = newLabel(SWT.NONE);
    center(copyrightLabel);
    copyrightLabel.setText(DialogsMessages.AboutDartDialog_copyright);

    addCopyDetailsPopup(copyrightLabel);

    Label copyrightLabel2 = newLabel(SWT.NONE);
    center(copyrightLabel2);
    copyrightLabel2.setText(DialogsMessages.AboutDartDialog_copyright_line2);

    addCopyDetailsPopup(copyrightLabel2);

    FormToolkit toolkit = new FormToolkit(getDisplay());
    Hyperlink link = toolkit.createHyperlink(this, "Privacy Policy", SWT.NONE);
    center(link);
    link.addHyperlinkListener(new HyperlinkAdapter() {
      @Override
      public void linkActivated(HyperlinkEvent e) {
        ExternalBrowserUtil.openInExternalBrowser("https://www.google.com/intl/en/policies/privacy/");
      }
    });

    //spacer and caret repressor
    StyledText spacer = new StyledText(this, SWT.NONE);
    spacer.setEditable(false);
    spacer.setFocus();
    spacer.getCaret().setSize(0, 0); //nuke the caret

    //close button
    Button closeButton = new Button(this, SWT.PUSH);
    closeButton.setText("       Close       ");
    center(closeButton);
    closeButton.addSelectionListener(new SelectionListener() {
      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        widgetSelected(e);
      }

      @Override
      public void widgetSelected(SelectionEvent e) {
        close();
      }
    });
    setDefaultButton(closeButton);

    //spacer and caret repressor
    spacer = new StyledText(this, SWT.NONE);
    spacer.setEditable(false);
    spacer.setFocus();
    spacer.getCaret().setSize(0, 0); //nuke the caret

    Label separator = new Label(this, SWT.SEPARATOR | SWT.SHADOW_OUT | SWT.HORIZONTAL);
    GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(separator);

    @SuppressWarnings("unused")
    UpdateStatusControl updateStatus = new UpdateStatusControl(
        this,
        Display.getDefault().getSystemColor(SWT.COLOR_WHITE),
        new Point(24, 4),
        false);

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

  private void addCopyDetailsPopup(Control control) {
    CopyDetailsToClipboardAction.addCopyDetailsPopup(control, this);
  }

  private void center(Control control) {
    GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).applyTo(control);
  }

  private String getVersion() {
    return DartToolsPlugin.getVersionString() + getChannelLabel();
  }

  private Label newLabel(int style) {
    Label label = new Label(this, style);
    label.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
    return label;
  }

}
