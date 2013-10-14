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
package com.google.dart.tools.debug.ui.internal.util;

import com.google.dart.tools.debug.ui.internal.dartium.DartiumLaunchMessages;
import com.google.dart.tools.debug.ui.internal.util.AppSelectionDialog.HtmlResourceFilter;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;

/**
 * A composite that creates a group to enter html and url information for dartium/browser launch
 */
public class LaunchTargetComposite extends Composite {

  protected ModifyListener textModifyListener = new ModifyListener() {
    @Override
    public void modifyText(ModifyEvent e) {
      notifyPanelChanged();
    }
  };

  private Button htmlButton;
  private Text htmlText;
  private Button htmlBrowseButton;
  private Button urlButton;
  private Text urlText;
  private Text sourceDirectoryText;
  private Button projectBrowseButton;

  private int widthHint;

  private Label projectLabel;

  public LaunchTargetComposite(Composite parent, int style) {
    super(parent, style);

    GridLayout layout = new GridLayout(1, false);
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    setLayout(layout);
    GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.TOP).applyTo(this);

    Group group = new Group(this, SWT.NONE);
    group.setText(DartiumLaunchMessages.DartiumMainTab_LaunchTarget);
    GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.TOP).applyTo(group);
    GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);

    createHtmlField(group);

    Label filler = new Label(group, SWT.NONE);
    GridDataFactory.swtDefaults().span(3, 1).hint(-1, 4).applyTo(filler);

    createUrlField(group);
  }

  public int getButtonWidthHint() {
    return widthHint;
  }

  public String getErrorMessage() {

    if (htmlButton.getSelection() && htmlText.getText().length() == 0) {
      return DartiumLaunchMessages.DartiumMainTab_NoHtmlFile;
    }

    if (urlButton.getSelection()) {
      String url = urlText.getText();

      if (url.length() == 0) {
        return DartiumLaunchMessages.DartiumMainTab_NoUrl;
      }

      if (!isValidUrl(url)) {
        return DartiumLaunchMessages.DartiumMainTab_InvalidURL;
      }

      if (sourceDirectoryText.getText().length() == 0) {
        return DartiumLaunchMessages.DartiumMainTab_NoProject;
      }
    }

    return null;
  }

  public boolean getHtmlButtonSelection() {
    return htmlButton.getSelection();
  }

  public String getHtmlFileName() {
    return htmlText.getText().trim();
  }

  public int getLabelColumnWidth() {
    projectLabel.pack();
    return projectLabel.getSize().x;
  }

  public String getSourceDirectory() {
    return sourceDirectoryText.getText().trim();
  }

  public String getUrlString() {
    return urlText.getText().trim();
  }

  public void setHtmlButtonSelection(boolean state) {
    htmlButton.setSelection(state);
    urlButton.setSelection(!state);
    updateEnablements(state);

  }

  public void setHtmlTextValue(String string) {
    htmlText.setText(string);
  }

  public void setSourceDirectoryTextValue(String sourceDirectoryName) {
    sourceDirectoryText.setText(sourceDirectoryName);

  }

  public void setUrlTextValue(String string) {
    urlText.setText(string);
  }

  protected void createHtmlField(Composite composite) {
    htmlButton = new Button(composite, SWT.RADIO);
    htmlButton.setText(DartiumLaunchMessages.DartiumMainTab_HtmlFileLabel);
    htmlButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateEnablements(true);
        notifyPanelChanged();
      }
    });

    htmlText = new Text(composite, SWT.BORDER | SWT.SINGLE);
    htmlText.addModifyListener(textModifyListener);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).hint(400, SWT.DEFAULT).grab(
        true,
        false).applyTo(htmlText);

    htmlBrowseButton = new Button(composite, SWT.PUSH);
    htmlBrowseButton.setText(DartiumLaunchMessages.DartiumMainTab_Browse);
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
  }

  protected void createUrlField(Composite composite) {
    urlButton = new Button(composite, SWT.RADIO);
    urlButton.setText(DartiumLaunchMessages.DartiumMainTab_UrlLabel);
    urlButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateEnablements(false);
        notifyPanelChanged();
      }
    });

    urlText = new Text(composite, SWT.BORDER | SWT.SINGLE);
    urlText.addModifyListener(textModifyListener);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(urlText);

    // spacer
    new Label(composite, SWT.NONE);

    projectLabel = new Label(composite, SWT.NONE);
    projectLabel.setText(DartiumLaunchMessages.DartiumMainTab_SourceDirectoryLabel);
    GridDataFactory.swtDefaults().indent(20, 0).applyTo(projectLabel);

    sourceDirectoryText = new Text(composite, SWT.BORDER | SWT.SINGLE);
    sourceDirectoryText.setCursor(composite.getShell().getDisplay().getSystemCursor(
        SWT.CURSOR_ARROW));
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(
        sourceDirectoryText);

    projectBrowseButton = new Button(composite, SWT.PUSH);
    projectBrowseButton.setText(DartiumLaunchMessages.DartiumMainTab_Browse);
    PixelConverter converter = new PixelConverter(htmlBrowseButton);
    widthHint = converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BEGINNING).hint(widthHint, -1).applyTo(
        projectBrowseButton);
    projectBrowseButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        handleSourceDirectoryBrowseButton();
      }
    });
  }

  protected void handleApplicationBrowseButton() {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    AppSelectionDialog dialog = new AppSelectionDialog(
        getShell(),
        workspace.getRoot(),
        new HtmlResourceFilter());
    dialog.setTitle(DartiumLaunchMessages.DartiumMainTab_SelectHtml);
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

      notifyPanelChanged();
    }
  }

  protected void handleSourceDirectoryBrowseButton() {
    ContainerSelectionDialog dialog = new ContainerSelectionDialog(
        getShell(),
        null,
        false,
        DartiumLaunchMessages.DartiumMainTab_SelectProject);

    dialog.open();

    Object[] results = dialog.getResult();

    if ((results != null) && (results.length > 0)) {
      String pathStr = ((IPath) results[0]).toString();
      sourceDirectoryText.setText(pathStr);
      notifyPanelChanged();
    }
  }

  protected void updateEnablements(boolean isFile) {
    if (isFile) {
      htmlText.setEnabled(true);
      htmlBrowseButton.setEnabled(true);
      urlText.setEnabled(false);
      sourceDirectoryText.setEnabled(false);
      projectBrowseButton.setEnabled(false);
    } else {
      htmlText.setEnabled(false);
      htmlBrowseButton.setEnabled(false);
      urlText.setEnabled(true);
      sourceDirectoryText.setEnabled(true);
      projectBrowseButton.setEnabled(true);
    }
  }

  private boolean isValidUrl(String url) {
    final String[] validSchemes = new String[] {"file:", "http:", "https:"};

    for (String scheme : validSchemes) {
      if (url.startsWith(scheme)) {
        return true;
      }
    }

    return false;
  }

  private void notifyPanelChanged() {
    Event event = new Event();
    event.type = SWT.Modify;
    event.widget = this;
    notifyListeners(SWT.Modify, event);
  }
}
