/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.debug.ui.internal.pubserve;

import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;
import com.google.dart.tools.debug.ui.internal.util.AppSelectionDialog;
import com.google.dart.tools.debug.ui.internal.util.AppSelectionDialog.HtmlWebResourceFilter;
import com.google.dart.tools.ui.internal.util.ExternalBrowserUtil;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;

/**
 * The main tab for the pub serve launch configuration
 */
public class PubServeMainTab extends AbstractLaunchConfigurationTab {

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

  private Button checkedModeButton;
  private Button useWebComponentsButton;
  private Text argumentText;
  private Text htmlText;
  private Button htmlBrowseButton;

  public PubServeMainTab() {
    setMessage("Create a configuration to launch a Dart application in Dartium, use pub to serve contents");
  }

  @Override
  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayoutFactory.swtDefaults().spacing(1, 3).applyTo(composite);

    Group group = new Group(composite, SWT.NONE);
    group.setText("Launch target");
    GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.TOP).applyTo(group);
    GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);

    Label label = new Label(group, SWT.NONE);
    label.setText("HTML file:");
    htmlText = new Text(group, SWT.BORDER | SWT.SINGLE);
    htmlText.addModifyListener(new ModifyListener() {

      @Override
      public void modifyText(ModifyEvent e) {
        notifyPanelChanged();
      }
    });
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).hint(400, SWT.DEFAULT).grab(
        true,
        false).applyTo(htmlText);

    htmlBrowseButton = new Button(group, SWT.PUSH);
    htmlBrowseButton.setText("Select...");
    PixelConverter converter = new PixelConverter(htmlBrowseButton);
    int widthHint = converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BEGINNING).hint(widthHint, -1).applyTo(
        htmlBrowseButton);
    htmlBrowseButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        handleApplicationBrowseButton();
      }
    });

    // Dartium settings group
    group = new Group(composite, SWT.NONE);
    group.setText("Dartium settings");
    GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
    GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
    ((GridLayout) group.getLayout()).marginBottom = 5;

    checkedModeButton = new Button(group, SWT.CHECK);
    checkedModeButton.setText("Run in checked mode");
    checkedModeButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        notifyPanelChanged();
      }
    });
    GridDataFactory.swtDefaults().span(2, 1).grab(true, false).applyTo(checkedModeButton);

    Link infoLink = new Link(group, SWT.NONE);
    infoLink.setText("<a href=\"" + DartDebugUIPlugin.CHECK_MODE_DESC_URL
        + "\">what is checked mode?</a>");
    infoLink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        ExternalBrowserUtil.openInExternalBrowser(DartDebugUIPlugin.CHECK_MODE_DESC_URL);
      }
    });

    useWebComponentsButton = new Button(group, SWT.CHECK);
    useWebComponentsButton.setText("Enable experimental browser features (Web Components)");
    useWebComponentsButton.setToolTipText("--enable-experimental-webkit-features"
        + " and --enable-devtools-experiments");
    GridDataFactory.swtDefaults().span(3, 1).applyTo(useWebComponentsButton);
    useWebComponentsButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        notifyPanelChanged();
      }
    });

    // additional browser arguments
    Label argsLabel = new Label(group, SWT.NONE);
    argsLabel.setText("Browser arguments:");

    argumentText = new Text(group, SWT.BORDER | SWT.SINGLE);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1).applyTo(
        argumentText);

    Label messageLabel = new Label(composite, SWT.NONE);
    messageLabel.setText("Note: Does not support debugging of applications in Dartium");
    messageLabel.setFont(getItalicFont(label.getFont()));

    setControl(composite);
  }

  @Override
  public void dispose() {
    Control control = getControl();

    if (control != null) {
      control.dispose();
      setControl(null);
    }
  }

  @Override
  public String getName() {
    return "Main";
  }

  @Override
  public void initializeFrom(ILaunchConfiguration configuration) {
    DartLaunchConfigWrapper dartLauncher = new DartLaunchConfigWrapper(configuration);

    htmlText.setText(dartLauncher.appendQueryParams(dartLauncher.getApplicationName()));
    checkedModeButton.setSelection(dartLauncher.getCheckedMode());
    useWebComponentsButton.setSelection(dartLauncher.getUseWebComponents());
    argumentText.setText(dartLauncher.getArguments());
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    DartLaunchConfigWrapper dartLauncher = new DartLaunchConfigWrapper(configuration);
    dartLauncher.setShouldLaunchFile(true);

    String fileUrl = htmlText.getText().trim();

    if (fileUrl.indexOf('?') == -1) {
      dartLauncher.setApplicationName(fileUrl);
      dartLauncher.setUrlQueryParams("");
    } else {
      int index = fileUrl.indexOf('?');

      dartLauncher.setApplicationName(fileUrl.substring(0, index));
      dartLauncher.setUrlQueryParams(fileUrl.substring(index + 1));
    }

    dartLauncher.setCheckedMode(checkedModeButton.getSelection());
    dartLauncher.setUseWebComponents(useWebComponentsButton.getSelection());
    dartLauncher.setArguments(argumentText.getText().trim());

  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    DartLaunchConfigWrapper dartLauncher = new DartLaunchConfigWrapper(configuration);
    dartLauncher.setShouldLaunchFile(true);
    dartLauncher.setApplicationName(""); //$NON-NLS-1$
  }

  protected void handleApplicationBrowseButton() {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    AppSelectionDialog dialog = new AppSelectionDialog(
        getShell(),
        workspace.getRoot(),
        new HtmlWebResourceFilter());
    dialog.setTitle("Select a HTML page to launch");
    dialog.setInitialPattern(".", FilteredItemsSelectionDialog.FULL_SELECTION); //$NON-NLS-1$
    IPath path = new Path(htmlText.getText());
    if (workspace.validatePath(path.toString(), IResource.FILE).isOK()) {
      IFile file = workspace.getRoot().getFile(path);
      if (file != null && file.exists()) {
        dialog.setInitialSelections(new Object[] {path});
      }
    }

    dialog.open();

    Object[] results = dialog.getResult();

    if ((results != null) && (results.length > 0) && (results[0] instanceof IFile)) {
      IFile file = (IFile) results[0];
      String pathStr = file.getFullPath().toPortableString();

      htmlText.setText(pathStr);
    }
  }

  private void notifyPanelChanged() {
    setDirty(true);
    updateLaunchConfigurationDialog();
  }

}
