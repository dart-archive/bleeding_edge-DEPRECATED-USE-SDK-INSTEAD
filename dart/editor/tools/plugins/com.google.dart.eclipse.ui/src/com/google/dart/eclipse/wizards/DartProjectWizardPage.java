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
package com.google.dart.eclipse.wizards;

import com.google.dart.tools.ui.internal.projects.NewApplicationCreationPage.ProjectType;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

/**
 * Dart project wizard creation page.
 */
public class DartProjectWizardPage extends WizardPage {

  private ProjectComposite projectComposite;

  public DartProjectWizardPage(ISelection selection) {
    super("wizardPage");

    setTitle("Create a Dart Project");
    setDescription("This wizard creates a new Dart project.");
    //TODO (pquitslund): add wizban
    //setImageDescriptor(DartEclipseUI.getImageDescriptor("icons/dart-icon-wizard.png"));
  }

  @Override
  public void createControl(Composite parent) {

    Composite container = new Composite(parent, SWT.NULL);
    GridLayoutFactory.swtDefaults().spacing(5, 1).applyTo(container);

    projectComposite = new ProjectComposite(container, SWT.NONE);
    projectComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    setControl(container);
  }

  public String getProjectLocation() {
    return projectComposite.getProjectPath();
  }

  public String getProjectName() {
    return projectComposite.getProjectName();
  }

  public ProjectType getSampleContentType() {
    return projectComposite.getSampleType();
  }

  public boolean hasPubSupport() {
    return projectComposite.hasPubSupport();
  }

}
