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
package com.google.dart.tools.debug.ui.internal.browser;

import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;
import com.google.dart.tools.debug.ui.internal.dartium.DartiumLaunchMessages;
import com.google.dart.tools.debug.ui.internal.dartium.DartiumMainTab;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Main launch tab for Browser launch configurations
 */
public class BrowserMainTab extends DartiumMainTab {

  private static Font italicFont;

  private static Font getItalicFont(Font font) {
    if (italicFont == null) {
      FontData data = font.getFontData()[0];

      italicFont = new Font(Display.getDefault(), new FontData(
          data.getName(),
          data.getHeight(),
          SWT.ITALIC));
    }

    return italicFont;
  }

  private Text dart2jsFlagsText;
  private int hIndent = 20;

  @Override
  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayoutFactory.swtDefaults().spacing(1, 3).applyTo(composite);

    // Project group
    Group group = new Group(composite, SWT.NONE);
    group.setText(Messages.BrowserMainTab_LaunchTarget);
    GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
    GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);

    createHtmlField(group);

    Label filler = new Label(group, SWT.NONE);
    GridDataFactory.swtDefaults().span(3, 1).hint(-1, 4).applyTo(filler);

    urlButton = new Button(group, SWT.RADIO);
    urlButton.setText(DartiumLaunchMessages.DartiumMainTab_UrlLabel);
    urlButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateEnablements(false);
        notifyPanelChanged();
      }
    });

    urlText = new Text(group, SWT.BORDER | SWT.SINGLE);
    urlText.addModifyListener(textModifyListener);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(urlText);

    // spacer
    new Label(group, SWT.NONE);

    Label projectLabel = new Label(group, SWT.NONE);
    projectLabel.setText(DartiumLaunchMessages.DartiumMainTab_SourceDirectoryLabel);
    GridDataFactory.swtDefaults().indent(hIndent, 0).applyTo(projectLabel);
    projectLabel.pack();
    int labelWidth = projectLabel.getSize().x;

    sourceDirectoryText = new Text(group, SWT.BORDER | SWT.SINGLE);
    sourceDirectoryText.setCursor(group.getShell().getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(
        sourceDirectoryText);

    projectBrowseButton = new Button(group, SWT.PUSH);
    projectBrowseButton.setText(DartiumLaunchMessages.DartiumMainTab_Browse);
    PixelConverter converter = new PixelConverter(htmlBrowseButton);
    int widthHint = converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BEGINNING).hint(widthHint, -1).applyTo(
        projectBrowseButton);
    projectBrowseButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        handleSourceDirectoryBrowseButton();
      }
    });

    // dart2js group
    Group dart2jsGroup = new Group(composite, SWT.NONE);
    dart2jsGroup.setText(Messages.BrowserMainTab_Dart2js);
    GridDataFactory.fillDefaults().grab(true, false).applyTo(dart2jsGroup);
    GridLayoutFactory.swtDefaults().numColumns(3).applyTo(dart2jsGroup);
    ((GridLayout) dart2jsGroup.getLayout()).marginBottom = 5;

    Label dart2jsLabel = new Label(dart2jsGroup, SWT.NONE);
    dart2jsLabel.setText("Compiler flags:");
    GridDataFactory.swtDefaults().hint(labelWidth + hIndent, -1).applyTo(dart2jsLabel);

    dart2jsFlagsText = new Text(dart2jsGroup, SWT.BORDER | SWT.SINGLE);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(
        dart2jsFlagsText);

    Label label = new Label(dart2jsGroup, SWT.NONE);
    GridDataFactory.swtDefaults().hint(widthHint, -1).applyTo(label);

    label = new Label(dart2jsGroup, SWT.NONE);
    label.setText("(e.g. --disallow-unsafe-eval)");
    label.setFont(getItalicFont(label.getFont()));
    GridDataFactory.swtDefaults().indent(hIndent + labelWidth, SWT.DEFAULT).span(3, 1).applyTo(
        label);

    setControl(composite);
  }

  @Override
  public void dispose() {
    Control control = getControl();

    if (control != null) {
      control.dispose();
      setControl(null);
    }
    italicFont.dispose();
  }

  @Override
  public String getErrorMessage() {
    String message = super.getErrorMessage();

    if (message != null) {
      return message;
    }

    return null;
  }

  /**
   * Answer the image to show in the configuration tab or <code>null</code> if none
   */
  @Override
  public Image getImage() {
    return DartDebugUIPlugin.getImage("obj16/globe_dark.png"); //$NON-NLS-1$
  }

  @Override
  public String getMessage() {
    return Messages.BrowserMainTab_Description;
  }

  /**
   * Answer the name to show in the configuration tab
   */
  @Override
  public String getName() {
    return Messages.BrowserMainTab_Name;
  }

  /**
   * Initialize the UI from the specified configuration
   */
  @Override
  public void initializeFrom(ILaunchConfiguration config) {
    super.initializeFrom(config);
    DartLaunchConfigWrapper wrapper = new DartLaunchConfigWrapper(config);
    dart2jsFlagsText.setText(wrapper.getDart2jsFlags());
  }

  /**
   * Store the value specified in the UI into the launch configuration
   */
  @Override
  public void performApply(ILaunchConfigurationWorkingCopy config) {
    super.performApply(config);

    DartLaunchConfigWrapper wrapper = new DartLaunchConfigWrapper(config);
    wrapper.setDart2jsFlags(dart2jsFlagsText.getText().trim());

  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    super.setDefaults(configuration);
  }

  @Override
  protected String performSdkCheck() {
    // This tab does not care if the Dart SDK is installed or not.

    return null;
  }

}
