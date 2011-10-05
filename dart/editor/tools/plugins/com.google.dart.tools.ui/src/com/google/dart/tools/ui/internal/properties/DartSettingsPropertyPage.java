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
package com.google.dart.tools.ui.internal.properties;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * The property page for Dart build settings. The page currently allows the user to override the
 * default output location.
 */
public class DartSettingsPropertyPage extends PropertyPage implements IWorkbenchPropertyPage {
  private Text outputLocationText;

  /**
   * Create a new DartSettingsPropertyPage.
   */
  public DartSettingsPropertyPage() {

  }

  @Override
  public boolean performOk() {
    DartProject dartProject = getDartProject();

    if (dartProject != null) {
      IPath path = Path.fromPortableString(outputLocationText.getText().trim());

      try {
        dartProject.setOutputLocation(path, new NullProgressMonitor());
      } catch (DartModelException exception) {
        ExceptionHandler.handle(exception, "Dart Core Exception",
            "Unable to set the project's output location.");
      }
    }

    return true;
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayoutFactory.swtDefaults().numColumns(3).applyTo(composite);

    Label label = new Label(composite, SWT.NONE);
    label.setText("Output location:");

    outputLocationText = new Text(composite, SWT.BORDER | SWT.SINGLE);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(
        outputLocationText);

    Button button = new Button(composite, SWT.PUSH);
    button.setText("Browse...");
    PixelConverter converter = new PixelConverter(button);
    int widthHint = converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
    GridDataFactory.swtDefaults().hint(widthHint, -1).applyTo(button);
    button.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        handleBrowseButton();
      }
    });

    initializeFromSettings();

    return composite;
  }

  @Override
  protected void performDefaults() {
    DartProject dartProject = getDartProject();

    if (dartProject != null) {
      // Reset the output location to the default directory ('out'). 
      outputLocationText.setText(dartProject.getDefaultOutputFullPath().toPortableString());
    }

    super.performDefaults();
  }

  private DartProject getDartProject() {
    if (!(getElement() instanceof IProject)) {
      return null;
    }

    IProject project = (IProject) getElement();

    return DartCore.create(project);
  }

  private void handleBrowseButton() {
    DartProject project = getDartProject();

    ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(),
        project.getProject(), true, "Select output location:");
    dialog.showClosedProjects(false);

    if (dialog.open() == Window.OK) {
      Object[] result = dialog.getResult();
      if (result.length == 0) {
        return;
      }
      IPath path = (IPath) result[0];
      outputLocationText.setText(path.toPortableString());
    }
  }

  private void initializeFromSettings() {
    DartProject dartProject = getDartProject();

    if (dartProject != null) {
      try {
        outputLocationText.setText(dartProject.getOutputLocation().toPortableString());
      } catch (DartModelException exception) {
        ExceptionHandler.handle(exception, "Dart Core Exception",
            "Unable to retreive the project's output location.");
      }
    }
  }

}
