/*******************************************************************************
 * Copyright (c) 2002, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.wizards;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyDelegatingOperation;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;
import org.eclipse.wst.xml.ui.internal.Logger;
import org.eclipse.wst.xml.ui.internal.XMLUIPlugin;

public class ExampleProjectCreationWizard extends Wizard implements INewWizard,
    IExecutableExtension {

  private class ImportOverwriteQuery implements IOverwriteQuery {

    private int openDialog(final String file) {
      final int[] result = {IDialogConstants.CANCEL_ID};
      getShell().getDisplay().syncExec(new Runnable() {
        public void run() {
          String title = XMLWizardsMessages.ExampleProjectCreationWizard_overwritequery_title;
          String msg = NLS.bind(
              XMLWizardsMessages.ExampleProjectCreationWizard_overwritequery_message, file);
          String[] options = {
              IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL,
              IDialogConstants.YES_TO_ALL_LABEL, IDialogConstants.CANCEL_LABEL};
          MessageDialog dialog = new MessageDialog(getShell(), title, null, msg,
              MessageDialog.QUESTION, options, 0);
          result[0] = dialog.open();
        }
      });
      return result[0];
    }

    public String queryOverwrite(String file) {
      String[] returnCodes = {YES, NO, ALL, CANCEL};
      int returnVal = openDialog(file);
      return returnVal < 0 ? CANCEL : returnCodes[returnVal];
    }
  }

  private IConfigurationElement wizardConfigElement;
  private IConfigurationElement exampleConfigElement;

  private String EXAMPLE_WIZARD_XP_ID = "org.eclipse.wst.common.ui.exampleProjectCreationWizard"; //$NON-NLS-1$

  private ExampleProjectCreationWizardPage[] pages;

  private final String WEB_BROWSER_ID = "org.eclipse.ui.browser.editor"; //$NON-NLS-1$

  public ExampleProjectCreationWizard() {
    super();
    setDialogSettings(XMLUIPlugin.getDefault().getDialogSettings());
    setWindowTitle(XMLWizardsMessages.ExampleProjectCreationWizard_title);
    setNeedsProgressMonitor(true);
  }

  /*
   * @see Wizard#addPages
   */
  public void addPages() {
    super.addPages();

    if (exampleConfigElement == null) {
      return;
    }
    IConfigurationElement[] children = exampleConfigElement.getChildren("projectsetup"); //$NON-NLS-1$
    if ((children == null) || (children.length == 0)) {
      Logger.log(Logger.ERROR, "descriptor must contain one ore more projectsetup tags"); //$NON-NLS-1$
      return;
    }

    pages = new ExampleProjectCreationWizardPage[children.length];

    for (int i = 0; i < children.length; i++) {
      pages[i] = new ExampleProjectCreationWizardPage(i, children[i]);
      addPage(pages[i]);
    }
  }

  public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
    if (exampleConfigElement != null) {
      String banner = exampleConfigElement.getAttribute("banner"); //$NON-NLS-1$
      if (banner != null) {
        URL imageURL = Platform.find(
            Platform.getBundle(exampleConfigElement.getDeclaringExtension().getNamespace()),
            new Path(banner));
        ImageDescriptor desc = ImageDescriptor.createFromURL(imageURL);
        setDefaultPageImageDescriptor(desc);
      }
    }
  }

  protected IConfigurationElement[] getExtendedConfigurationElements() {
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint exampleWizardXP = registry.getExtensionPoint(EXAMPLE_WIZARD_XP_ID);
    if (exampleWizardXP == null) {
      return new IConfigurationElement[0];
    }
    IExtension extension = exampleWizardXP.getExtension(getWizardExtensionId());
    if (extension != null) {
      return extension.getConfigurationElements();
    }
    IConfigurationElement[] exampleWizardCEs = exampleWizardXP.getConfigurationElements();
    return exampleWizardCEs;
  }

  private void handleException(Throwable target) {
    String title = XMLWizardsMessages.ExampleProjectCreationWizard_op_error_title;
    String message = XMLWizardsMessages.ExampleProjectCreationWizard_op_error_message;
    if (target instanceof CoreException) {
      IStatus status = ((CoreException) target).getStatus();
      ErrorDialog.openError(getShell(), title, message, status);
      Logger.logException(status.getMessage(), status.getException());
    } else {
      MessageDialog.openError(getShell(), title, target.getMessage());
      Logger.logException(target);
    }
  }

  private void openResource(final IResource resource) {
    if (resource.getType() != IResource.FILE) {
      return;
    }
    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    if (window == null) {
      return;
    }
    final IWorkbenchPage activePage = window.getActivePage();
    if (activePage != null) {
      final Display display = getShell().getDisplay();
      display.asyncExec(new Runnable() {
        public void run() {
          try {
            IDE.openEditor(activePage, (IFile) resource, WEB_BROWSER_ID, true);
          } catch (PartInitException e) {
            Logger.logException(e);
          }
        }
      });
      BasicNewResourceWizard.selectAndReveal(resource, activePage.getWorkbenchWindow());
    }
  }

  /*
   * @see Wizard#performFinish
   */
  public boolean performFinish() {
    ExampleProjectCreationOperation runnable = new ExampleProjectCreationOperation(pages,
        new ImportOverwriteQuery());

    IRunnableWithProgress op = new WorkspaceModifyDelegatingOperation(runnable);
    try {
      getContainer().run(false, true, op);
    } catch (InvocationTargetException e) {
      handleException(e.getTargetException());
      return false;
    } catch (InterruptedException e) {
      return false;
    }
    BasicNewProjectResourceWizard.updatePerspective(wizardConfigElement);
    IResource res = runnable.getElementToOpen();
    if (res != null) {
      openResource(res);
    }
    return true;
  }

  /**
   * Stores the configuration element for the wizard. The config element will be used in
   * <code>performFinish</code> to set the result perspective.
   */
  public void setInitializationData(IConfigurationElement cfig, String propertyName, Object data) {
    wizardConfigElement = cfig;
    String title = wizardConfigElement.getAttribute("name"); //$NON-NLS-1$
    if (title != null) {
      setWindowTitle(title);
    }
    String wizardId = getWizardExtensionId();
    IConfigurationElement[] exampleWizardCEs = getExtendedConfigurationElements();
    for (int i = 0; i < exampleWizardCEs.length; i++) {
      IConfigurationElement element = exampleWizardCEs[i];
      String extWizardId = element.getAttribute("id"); //$NON-NLS-1$
      if ((wizardId != null) && (extWizardId != null) && wizardId.equals(extWizardId)) {
        exampleConfigElement = element;
      }
    }
    // initializeDefaultPageImageDescriptor();
  }

  public String getWizardExtensionId() {
    return wizardConfigElement.getAttribute("id"); //$NON-NLS-1$
  }

}
