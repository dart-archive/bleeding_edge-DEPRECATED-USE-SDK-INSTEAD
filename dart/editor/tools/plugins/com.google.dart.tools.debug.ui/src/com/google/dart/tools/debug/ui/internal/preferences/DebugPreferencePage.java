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
package com.google.dart.tools.debug.ui.internal.preferences;

import com.google.dart.tools.debug.core.ChromeBrowserConfig;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.Util;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.ListDialog;

import java.io.File;
import java.util.List;

/**
 * The preference page for Dart debugging.
 */
public class DebugPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  private class ProgramLabelProvider extends LabelProvider {

    @Override
    public Image getImage(Object element) {
      return new Image(Display.getDefault(), ((Program) element).getImageData());
    }

    @Override
    public String getText(Object element) {
      return ((Program) element).getName();
    }

  }; //$NON-NLS-1$

  public static final String PAGE_ID = "com.google.dart.tools.debug.debugPreferencePage"; //$NON-NLS-1$
  private List<ChromeBrowserConfig> browsers;

  private TableViewer browserViewer;
  private Text vmField;

  private Text browserField;
  private Button removeBrowserButton;

  private Button renameBrowserButton;

  private Button browseButton;

  private Button defaultBrowserButton;

  private Button selectBrowserButton;

  /**
   * Create a new preference page.
   */
  public DebugPreferencePage() {
    setDescription("Dart Launch Preferences");
  }

  @Override
  public void init(IWorkbench workbench) {
    noDefaultAndApplyButton();
  }

  @Override
  public boolean performOk() {
//    DartDebugCorePlugin.getPlugin().setConfiguredBrowsers(browsers);

    DartDebugCorePlugin.getPlugin().setDartVmExecutablePath(vmField.getText());

    if (defaultBrowserButton.getSelection()) {
      DartDebugCorePlugin.getPlugin().setDefaultBrowser(true);
    } else {
      DartDebugCorePlugin.getPlugin().setDefaultBrowser(false);
      DartDebugCorePlugin.getPlugin().setBrowserExecutablePath(browserField.getText());
    }
    return true;
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridDataFactory.fillDefaults().grab(true, false).indent(0, 10).align(SWT.FILL, SWT.BEGINNING).applyTo(
        composite);
    GridLayoutFactory.fillDefaults().spacing(0, 8).margins(0, 10).applyTo(composite);

//    // Chrome browser
//    Group browersGroup = new Group(composite, SWT.NONE);
//    browersGroup.setText("Chrome browsers");
//    GridDataFactory.fillDefaults().grab(true, false).applyTo(browersGroup);
//    GridLayoutFactory.swtDefaults().numColumns(2).spacing(5, 2).applyTo(browersGroup);
//
//    browserViewer = new TableViewer(browersGroup, SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL
//        | SWT.BORDER);
//    browserViewer.setContentProvider(new ArrayContentProvider());
//    browserViewer.setLabelProvider(new BrowsersLabelProvider());
//    browserViewer.setComparator(new ViewerComparator());
//    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.TOP).grab(true, false).hint(100, 75).applyTo(
//        browserViewer.getControl());
//    browserViewer.addSelectionChangedListener(new ISelectionChangedListener() {
//      @Override
//      public void selectionChanged(SelectionChangedEvent event) {
//        updateButtons();
//      }
//    });
//
//    Composite buttonComposite = new Composite(browersGroup, SWT.NONE);
//    GridDataFactory.fillDefaults().applyTo(buttonComposite);
//    GridLayout buttonLayout = new GridLayout();
//    buttonLayout.horizontalSpacing = 0;
//    buttonLayout.verticalSpacing = convertVerticalDLUsToPixels(3);
//    buttonLayout.marginWidth = 0;
//    buttonLayout.marginHeight = 0;
//    buttonLayout.numColumns = 1;
//    buttonComposite.setLayout(buttonLayout);
//
//    addBrowserButton = new Button(buttonComposite, SWT.PUSH);
//    addBrowserButton.setText("Add...");
//    PixelConverter converter = new PixelConverter(addBrowserButton);
//    int widthHint = converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
//    GridDataFactory.swtDefaults().hint(widthHint, -1).applyTo(addBrowserButton);
//    addBrowserButton.addSelectionListener(new SelectionAdapter() {
//      @Override
//      public void widgetSelected(SelectionEvent e) {
//        handleAddButton();
//      }
//    });
//
//    renameBrowserButton = new Button(buttonComposite, SWT.PUSH);
//    renameBrowserButton.setText("Rename...");
//    GridDataFactory.swtDefaults().hint(widthHint, -1).applyTo(renameBrowserButton);
//    renameBrowserButton.addSelectionListener(new SelectionAdapter() {
//      @Override
//      public void widgetSelected(SelectionEvent e) {
//        handleRenameButton();
//      }
//    });
//
//    removeBrowserButton = new Button(buttonComposite, SWT.PUSH);
//    removeBrowserButton.setText("Remove");
//    GridDataFactory.swtDefaults().hint(widthHint, -1).applyTo(removeBrowserButton);
//    removeBrowserButton.addSelectionListener(new SelectionAdapter() {
//      @Override
//      public void widgetSelected(SelectionEvent e) {
//        handleRemoveButton();
//      }
//    });

//    Label label = new Label(browersGroup, SWT.NONE);
//    label.setText("Enter one or more Chrome / Chromium based browsers");
//    GridDataFactory.swtDefaults().span(2, 1).applyTo(label);

    // Browser
    createBrowserConfig(composite);

    // Dart VM
//    Label label = new Label(composite, SWT.NONE);
    createVmConfig(composite);

    // browsers
//    browsers = new ArrayList<ChromeBrowserConfig>(
//        DartDebugCorePlugin.getPlugin().getConfiguredBrowsers());
//    browserViewer.setInput(browsers);

//    updateButtons();

    // vmField
    if (DartDebugCorePlugin.getPlugin().getDartVmExecutablePath() != null) {
      vmField.setText(DartDebugCorePlugin.getPlugin().getDartVmExecutablePath());
    }
    updateEnablements(DartDebugCorePlugin.getPlugin().getIsDefaultBrowser());

    if (DartDebugCorePlugin.getPlugin().getBrowserExecutablePath() != null) {
      browserField.setText(DartDebugCorePlugin.getPlugin().getBrowserExecutablePath());
    }

    return composite;
  }

  protected void handleVmBrowseButton() {
    FileDialog fd = new FileDialog(getShell(), SWT.OPEN);

    String filePath = fd.open();

    if (filePath != null) {
      browserField.setText(filePath);
    }
  }

  private void createBrowserConfig(Composite parent) {
    Group browserGroup = new Group(parent, SWT.NONE);
    browserGroup.setText(DebugPreferenceMessages.DebugPreferencePage_BrowserTitle);
    GridDataFactory.fillDefaults().grab(true, false).applyTo(browserGroup);
    GridLayoutFactory.swtDefaults().numColumns(3).spacing(5, 2).applyTo(browserGroup);

    defaultBrowserButton = new Button(browserGroup, SWT.RADIO);
    defaultBrowserButton.setText(DebugPreferenceMessages.DebugPreferencePage_DefaultBrowserText);
    defaultBrowserButton.addSelectionListener(new SelectionAdapter() {

      @Override
      public void widgetSelected(SelectionEvent e) {
        updateEnablements(true);
      }
    });
    GridDataFactory.swtDefaults().span(3, 1).applyTo(defaultBrowserButton);
    selectBrowserButton = new Button(browserGroup, SWT.RADIO);
    selectBrowserButton.setText(DebugPreferenceMessages.DebugPreferencePage_SelectBrowserText);
    GridDataFactory.swtDefaults().span(3, 1).applyTo(selectBrowserButton);
    selectBrowserButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateEnablements(false);
      }
    });

    browserField = new Text(browserGroup, SWT.SINGLE | SWT.BORDER);
    GridDataFactory.swtDefaults().indent(15, 0).align(SWT.FILL, SWT.CENTER).hint(100, SWT.DEFAULT).grab(
        true, false).applyTo(browserField);
    browserField.setEnabled(false);
    browseButton = new Button(browserGroup, SWT.PUSH);
    browseButton.setText(DebugPreferenceMessages.DebugPreferencePage_Browse);
    browseButton.setEnabled(false);
    PixelConverter converter = new PixelConverter(browseButton);
    int widthHint = converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
    GridDataFactory.swtDefaults().hint(widthHint, -1).applyTo(browseButton);
    browseButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        handleBrowserConfigBrowseButton();
      }
    });
  }

  private void createVmConfig(Composite composite) {
    Group vmGroup = new Group(composite, SWT.NONE);
    vmGroup.setText(DebugPreferenceMessages.DebugPreferencePage_VMExecutableLocation);
    GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.BEGINNING).applyTo(vmGroup);
    GridLayoutFactory.fillDefaults().numColumns(3).margins(8, 8).applyTo(vmGroup);

    Label vmLabel = new Label(vmGroup, SWT.NONE);
    vmLabel.setText(DebugPreferenceMessages.DebugPreferencePage_VMPath);
    vmField = new Text(vmGroup, SWT.SINGLE | SWT.BORDER);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).hint(100, SWT.DEFAULT).grab(true,
        false).applyTo(vmField);

    Button browseVmButton = new Button(vmGroup, SWT.PUSH);
    browseVmButton.setText(DebugPreferenceMessages.DebugPreferencePage_Browse);
    PixelConverter converter = new PixelConverter(browseVmButton);
    int widthHint = converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
    GridDataFactory.swtDefaults().hint(widthHint, -1).applyTo(browseVmButton);
    browseVmButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        handleVmBrowseButton();
      }
    });
  }

  private String findUniqueName(String name, int count, List<ChromeBrowserConfig> existingBrowsers) {
    String tempName = name + (count == 0 ? "" : count); //$NON-NLS-1$

    for (ChromeBrowserConfig browser : existingBrowsers) {
      if (tempName.equals(browser.getName())) {
        return findUniqueName(tempName, ++count, existingBrowsers);
      }
    }

    return tempName;
  }

  private String findUniqueName(String name, List<ChromeBrowserConfig> existingBrowsers) {
    // strip .exe from the name -
    if (name.endsWith(".exe")) { //$NON-NLS-1$
      name = name.substring(0, name.length() - ".exe".length()); //$NON-NLS-1$
    }

    return findUniqueName(name, 0, existingBrowsers);
  }

  private void handleAddButton() {
    FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);
    fileDialog.setText("Select Chrome based browser"); //$NON-NLS-1$

    String path = fileDialog.open();

    if (path != null) {
      path = patchupPath(path);

      if (path == null) {
        // Let the user know the path was invalid -
        MessageDialog.openWarning(getShell(), "Error Locating Executable", //$NON-NLS-1$
            "Unable to locate a browser executable."); //$NON-NLS-1$
      } else {
        ChromeBrowserConfig browserConfig = new ChromeBrowserConfig();

        browserConfig.setName(findUniqueName(new File(path).getName(), browsers));
        browserConfig.setPath(path);

        browsers.add(browserConfig);

        browserViewer.refresh();
      }
    }
  }

  private void handleBrowserConfigBrowseButton() {

    ListDialog listDialog = new ListDialog(getShell());
    listDialog.setTitle(DebugPreferenceMessages.DebugPreferencePage_DialogTitle);
    listDialog.setMessage(DebugPreferenceMessages.DebugPreferencePage_DialogMessage);

    listDialog.setLabelProvider(new ProgramLabelProvider());
    listDialog.setContentProvider(new ArrayContentProvider());
    listDialog.setInput(Program.getPrograms());

    if (listDialog.open() == Window.OK) {
      Object[] result = listDialog.getResult();
      browserField.setText(((Program) result[0]).getName());
    }

  }

  private void handleRemoveButton() {
    ChromeBrowserConfig browserConfig = (ChromeBrowserConfig) ((IStructuredSelection) browserViewer.getSelection()).getFirstElement();

    browsers.remove(browserConfig);

    browserViewer.refresh();

    updateButtons();
  }

  private void handleRenameButton() {
    final ChromeBrowserConfig browserConfig = (ChromeBrowserConfig) ((IStructuredSelection) browserViewer.getSelection()).getFirstElement();

    InputDialog inputDialog = new InputDialog(getShell(), "Enter Browser Name", //$NON-NLS-1$
        "Enter the new name for the browser configuration:", browserConfig.getName(), //$NON-NLS-1$
        new IInputValidator() {
          @Override
          public String isValid(String newText) {
            if (newText == null || newText.isEmpty()) {
              return "The name must not be empty."; //$NON-NLS-1$
            }

            for (ChromeBrowserConfig other : browsers) {
              if (other == browserConfig) {
                continue;
              }

              if (newText.equals(other.getName())) {
                return "A browser configuration with that name already exists."; //$NON-NLS-1$
              }
            }

            return null;
          }
        });

    if (inputDialog.open() == Window.OK) {
      browserConfig.setName(inputDialog.getValue());

      browserViewer.refresh();
    }
  }

  private String patchupPath(String path) {
    if (path == null) {
      return path;
    }

    File file = new File(path);

    if (file.isDirectory() && Util.isMac()) {
      // Applications on the mac are actually 'blessed' directories.

      // /Applications/Chromium.app/Contents/MacOS/Chromium
      // /Applications/Google Chrome.app/Contents/MacOS/Google Chrome
      String name = file.getName();

      if (name.endsWith(".app")) { //$NON-NLS-1$
        name = name.substring(0, name.length() - ".app".length()); //$NON-NLS-1$

        // Or look in Info.plist for <key>CFBundleExecutable</key><string>app.name</string>?

        path = path + "/Contents/MacOS/" + name; //$NON-NLS-1$

        File appFile = new File(path);

        if (appFile.exists() && appFile.isFile()) {
          return path;
        } else {
          return null;
        }
      } else {
        return null;
      }
    } else if (file.isDirectory()) {
      return null;
    } else {
      return path;
    }
  }

  private void updateButtons() {
    boolean hasSelection = !browserViewer.getSelection().isEmpty();

    renameBrowserButton.setEnabled(hasSelection);
    removeBrowserButton.setEnabled(hasSelection);
  }

  private void updateEnablements(boolean isDefaultBrowser) {
    if (isDefaultBrowser) {
      defaultBrowserButton.setSelection(true);
      selectBrowserButton.setSelection(false);
      browserField.setEnabled(false);
      browseButton.setEnabled(false);
    } else {
      defaultBrowserButton.setSelection(false);
      selectBrowserButton.setSelection(true);
      browserField.setEnabled(true);
      browseButton.setEnabled(true);
    }
  }
}
