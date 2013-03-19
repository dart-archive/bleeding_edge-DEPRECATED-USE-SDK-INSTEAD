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
package com.google.dart.tools.ui.internal.properties;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.DartModelManager;
import com.google.dart.tools.core.jobs.CleanLibrariesJob;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * The property page for Dart build settings. The page currently allows the user to override the
 * default output location.
 */
public class DartSettingsPropertyPage extends PropertyPage implements IWorkbenchPropertyPage {
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

  private Text packageRootText;
  private Button packageRootBrowseButton;

  /**
   * Create a new DartSettingsPropertyPage.
   */
  public DartSettingsPropertyPage() {

  }

  @Override
  public boolean performOk() {
    IProject project = getProject();

    if (project != null) {
      try {
        // dart2js
        DartCore.getPlugin().setDart2jsFlags(project, dart2jsFlagsText.getText().trim());

        // package root
        String oldPackageRoot = DartCore.getPlugin().getProjectPreferences(project).get(
            DartCore.PROJECT_PREF_PACKAGE_ROOT,
            "");
        // TODO(keertip): move this check into setPacakgeRoot and inform whether it has changed.
        String packageRoot = packageRootText.getText().trim();
        if (!oldPackageRoot.equals(packageRoot)) {
          DartCore.getPlugin().setPackageRoot(project, packageRoot);
          DartModelManager.getInstance().resetModel();
          Job job = new CleanLibrariesJob();
          job.schedule();
        }
      } catch (CoreException ce) {
        DartToolsPlugin.log(ce);
      }
    }

    return true;
  }

  @Override
  protected Control createContents(Composite parent) {
    final int indentAmount = 15;

    Composite composite = new Composite(parent, SWT.NONE);
    GridLayoutFactory.fillDefaults().applyTo(composite);

    // dart2js settings
    Group group = new Group(composite, SWT.NONE);
    group.setText("Dart2js settings");
    GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
    GridLayoutFactory.swtDefaults().numColumns(1).applyTo(group);
    ((GridLayout) group.getLayout()).marginBottom = 5;

    Label label = new Label(group, SWT.NONE);
    label.setText("Additional flags for dart2js:");

    dart2jsFlagsText = new Text(group, SWT.BORDER | SWT.SINGLE);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).hint(100, -1).indent(indentAmount, 0).grab(
        true,
        false).applyTo(dart2jsFlagsText);

    label = new Label(group, SWT.NONE);
    label.setText("(e.g. --disallow-unsafe-eval)");
    label.setFont(getItalicFont(label.getFont()));
    GridDataFactory.swtDefaults().indent(indentAmount, 0).applyTo(label);

    // package root settings
    group = new Group(composite, SWT.NONE);
    group.setText("Package root settings");
    GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
    GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
    ((GridLayout) group.getLayout()).marginBottom = 5;

    packageRootText = new Text(group, SWT.BORDER | SWT.SINGLE);
    GridDataFactory.swtDefaults().indent(indentAmount, 0).align(SWT.FILL, SWT.CENTER).hint(100, -1).grab(
        true,
        false).applyTo(packageRootText);

    packageRootBrowseButton = new Button(group, SWT.PUSH);
    packageRootBrowseButton.setText("Select...");
    PixelConverter converter = new PixelConverter(packageRootBrowseButton);
    int widthHint = converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BEGINNING).hint(widthHint, -1).applyTo(
        packageRootBrowseButton);
    packageRootBrowseButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        handlePackageRootBrowse();
      }
    });

    label = new Label(group, SWT.NONE);
    label.setText("The package root setting will override the use of local packages directories.");
    GridDataFactory.swtDefaults().indent(indentAmount, 0).span(2, 1).applyTo(label);

    initializeFromSettings();

    return composite;
  }

  @Override
  protected void performDefaults() {
    dart2jsFlagsText.setText("");
    packageRootText.setText("");
    super.performDefaults();
  }

  private IProject getProject() {
    return (IProject) getElement().getAdapter(IProject.class);
  }

  private void handlePackageRootBrowse() {
    DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.APPLICATION_MODAL | SWT.OPEN);
    dialog.setText("Select the package root path");

    String path = dialog.open();

    if (path != null) {
      packageRootText.setText(path);
    }
  }

  private void initializeFromSettings() {
    IProject project = getProject();

    if (project != null) {
      // dart2js
      String args = DartCore.getPlugin().getDart2jsFlags(project);

      if (args != null) {
        dart2jsFlagsText.setText(args);
      }

      String pref = DartCore.getPlugin().getProjectPreferences(project).get(
          DartCore.PROJECT_PREF_PACKAGE_ROOT,
          "");

      packageRootText.setText(pref);
    }
  }

}
