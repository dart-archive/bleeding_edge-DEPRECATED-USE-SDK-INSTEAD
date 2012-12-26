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
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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

  /**
   * Create a new DartSettingsPropertyPage.
   */
  public DartSettingsPropertyPage() {

  }

  @Override
  public boolean performOk() {
    IProject project = getProject();

    if (project != null) {
      String flags = dart2jsFlagsText.getText().trim();

      try {
        DartCore.getPlugin().setDart2jsFlags(project, flags);
      } catch (CoreException ce) {
        DartToolsPlugin.log(ce);
      }
    }

    return true;
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayoutFactory.fillDefaults().applyTo(composite);

    Group dart2js = new Group(composite, SWT.NONE);
    dart2js.setText("Dart2js settings");
    GridDataFactory.fillDefaults().grab(true, false).applyTo(dart2js);
    GridLayoutFactory.swtDefaults().numColumns(2).applyTo(dart2js);

    Label label = new Label(dart2js, SWT.NONE);
    label.setText("Additional flags:");

    dart2jsFlagsText = new Text(dart2js, SWT.BORDER | SWT.SINGLE);
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).hint(100, -1).grab(true, false).applyTo(
        dart2jsFlagsText);

    // spacer
    new Label(dart2js, SWT.NONE);

    label = new Label(dart2js, SWT.NONE);
    label.setText("e.g. --disallow-unsafe-eval");
    label.setFont(getItalicFont(label.getFont()));

    initializeFromSettings();

    return composite;
  }

  @Override
  protected void performDefaults() {
    dart2jsFlagsText.setText("");

    super.performDefaults();
  }

  private IProject getProject() {
    return (IProject) getElement().getAdapter(IProject.class);
  }

  private void initializeFromSettings() {
    IProject project = getProject();

    if (project != null) {
      String args = DartCore.getPlugin().getDart2jsFlags(project);

      if (args != null) {
        dart2jsFlagsText.setText(args);
      }
    }
  }

}
