/*******************************************************************************
 * Copyright (c) 2002, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/

package org.eclipse.wst.xml.ui.internal.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

public class ExampleProjectCreationWizardPage extends WizardNewProjectCreationPage {

  private IConfigurationElement configurationElement;

  public ExampleProjectCreationWizardPage(int pageNumber, IConfigurationElement elem) {

    super("page" + pageNumber); //$NON-NLS-1$
    configurationElement = elem;

    String name = getAttribute(elem, "name"); //$NON-NLS-1$
    setInitialProjectName(calculateInitialProjectName(name));

    setDescription(getAttribute(configurationElement, "pagedescription")); //$NON-NLS-1$
    setTitle(getAttribute(configurationElement, "pagetitle")); //$NON-NLS-1$

  }

  /*
   * Set the default project name that is to appear on the initialPage page of this wizard.
   */
  protected String calculateInitialProjectName(String projectName) {
    IProject projectHandle = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
    if (!projectHandle.exists()) {
      return projectName;
    }
    // Change the name until it doesn't exists. Try 9 times and then
    // give up.
    for (int i = 1; i < 10; i++) {
      projectHandle = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName + i);
      if (!projectHandle.exists()) {
        return projectName + i;
      }
    }
    return projectName + "9"; //$NON-NLS-1$

  }

  private String getAttribute(IConfigurationElement elem, String tag) {
    String res = elem.getAttribute(tag);
    if (res == null) {
      return '!' + tag + '!';
    }
    return res;
  }

  /**
   * Returns the configuration element of this page.
   * 
   * @return Returns a IConfigurationElement
   */
  public IConfigurationElement getConfigurationElement() {
    return configurationElement;
  }

  /**
   * @see org.eclipse.ui.dialogs.WizardNewProjectCreationPage#validatePage()
   */
  protected boolean validatePage() {
    if (!super.validatePage()) {
      return false;
    }

    String projectName = getProjectName();
    if (projectName == null) {
      return false;
    }

    IWizard wizard = getWizard();
    if (wizard instanceof ExampleProjectCreationWizard) {
      IWizardPage[] pages = wizard.getPages();
      for (int i = 0; i < pages.length; i++) {
        if ((pages[i] != this) && (pages[i] instanceof ExampleProjectCreationWizardPage)) {
          if (projectName.equals(((ExampleProjectCreationWizardPage) pages[i]).getProjectName())) {
            setErrorMessage(XMLWizardsMessages.ExampleProjectCreationWizardPage_error_alreadyexists);
            return false;
          }
        }
      }
    }

    return true;
  }

}
