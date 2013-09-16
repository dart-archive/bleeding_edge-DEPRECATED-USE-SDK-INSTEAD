/*******************************************************************************
 * Copyright (c) 2001, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation David Schneider, david.schneider@unisys.com - [142500] WTP properties pages fonts
 * don't follow Eclipse preferences
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.wizards;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.eclipse.wst.xml.core.internal.provisional.contenttype.ContentTypeIdForXML;

public class NewModelWizard extends Wizard implements INewWizard {

  protected IStructuredSelection fSelection;
  protected IWorkbench fWorkbench;
  protected String fCurrentDirectory;

  public NewModelWizard() {
    super();
  }

  public void init(IWorkbench workbench, IStructuredSelection selection) {
    this.fWorkbench = workbench;
    this.fSelection = selection;
  }

  public boolean performFinish() {
    boolean result = true;
    WizardPage currentPage = (WizardPage) getContainer().getCurrentPage();
    if (currentPage != null) {
      result = currentPage.isPageComplete();
    }
    return result;
  }

  /**
   * showFileDialog
   */
  public FileDialog showFileDialog(Shell shell, String defaultDirectory, String defaultFile,
      String[] filterExtensions) {
    FileDialog fileDialog = new FileDialog(shell, SWT.OPEN);

    // Get the last visit directory if we haven't defined a particular one
    // here.
    if ((defaultDirectory == null) || (defaultDirectory.length() == 0)) {
      // defaultDirectory =
      // Locate.instance(this).getLastSelectedDirectory(getResourcePath());
    }

    if ((defaultDirectory != null) && (defaultDirectory.length() != 0)) {
      fileDialog.setFilterPath(defaultDirectory);
    }

    fileDialog.setFileName(defaultFile);
    fileDialog.setFilterExtensions(filterExtensions);

    fileDialog.open();

    return fileDialog;
  }

  public void setCurrentDirectory(String currentDirectory1) {
    this.fCurrentDirectory = currentDirectory1;
  }

  public void createWorkbenchResource(IContainer container, String fileName) {
    IPath path = container.getFullPath();
    path = path.append(fileName);
    IFile file = container.getWorkspace().getRoot().getFile(path);
    if (!file.exists()) {
      try {
        file.create(null, true, null);
      } catch (CoreException e) {
        // XMLBuilderPlugin.getPlugin().getMsgLogger().write("Encountered
        // exception creating file: " + e.getMessage());
      }
    }
  }

  /**
   * StartPage
   */
  public class StartPage extends WizardPage implements Listener {
    protected int selectedButton;
    protected String[] radioButtonLabel;
    protected Button[] radioButton;

    public StartPage(String pageName, String[] radioButtonLabel) {
      super(pageName);
      this.radioButtonLabel = radioButtonLabel;
      radioButton = new Button[radioButtonLabel.length];
    }

    public Button getRadioButtonAtIndex(int i) {
      Button result = null;
      if ((i >= 0) && (i < radioButton.length)) {
        result = radioButton[i];
      }
      return result;
    }

    public int getSelectedRadioButtonIndex() {
      int result = -1;
      for (int i = 0; i < radioButton.length; i++) {
        if (radioButton[i].getSelection()) {
          result = i;
        }
      }
      return result;
    }

    public void createControl(Composite parent) {
      Composite base = new Composite(parent, SWT.NONE);
      base.setLayout(new GridLayout());

      // radio buttons' container
      Composite radioButtonsGroup = new Composite(base, SWT.NONE);
      GridLayout layout = new GridLayout();
      layout.numColumns = 1;
      layout.makeColumnsEqualWidth = true;
      layout.marginWidth = 0;

      radioButtonsGroup.setLayout(layout);
      GridData gd = new GridData(GridData.FILL_BOTH);
      gd.heightHint = 300;
      gd.widthHint = 400;
      radioButtonsGroup.setLayoutData(gd);

      for (int i = 0; i < radioButtonLabel.length; i++) {
        radioButton[i] = new Button(radioButtonsGroup, SWT.RADIO);
        radioButton[i].setText(radioButtonLabel[i]);
        radioButton[i].setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        // radioButton[i].addListener(SWT.Modify, this);
      }
      setControl(base);
      setPageComplete(isPageComplete());
      Dialog.applyDialogFont(parent);
    }

    public void handleEvent(Event event) {
      if (event.type == SWT.Modify) {
        setPageComplete(isPageComplete());
      }
    }
  }

  /**
   * NewFilePage
   */
  public class NewFilePage extends WizardNewFileCreationPage {
    public String defaultName = "NewFile"; //$NON-NLS-1$
    public String defaultFileExtension = ".txt"; //$NON-NLS-1$
    public String[] filterExtensions = {"*.txt"}; //$NON-NLS-1$
    private List fValidExtensions = null;

    public NewFilePage(IStructuredSelection selection) {
      super("", selection); //$NON-NLS-1$
    }

    protected String computeDefaultFileName() {
      int count = 0;
      String fileName = defaultName + defaultFileExtension;
      IPath containerFullPath = getContainerFullPath();
      if (containerFullPath != null) {
        while (true) {
          IPath path = containerFullPath.append(fileName);
          if (ResourcesPlugin.getWorkspace().getRoot().exists(path)) {
            count++;
            fileName = defaultName + count + defaultFileExtension;
          } else {
            break;
          }
        }
      }
      return fileName;
    }

    // returns true if file of specified name exists in any case for
    // selected container
    protected String existsFileAnyCase(String fileName) {
      if ((getContainerFullPath() != null) && (getContainerFullPath().isEmpty() == false)
          && (fileName.compareTo("") != 0)) //$NON-NLS-1$
      {
        // look through all resources at the specified container -
        // compare in upper case
        IResource parent = ResourcesPlugin.getWorkspace().getRoot().findMember(
            getContainerFullPath());
        if (parent instanceof IContainer) {
          IContainer container = (IContainer) parent;
          try {
            IResource[] members = container.members();
            String enteredFileUpper = fileName.toUpperCase();
            for (int i = 0; i < members.length; i++) {
              String resourceUpperName = members[i].getName().toUpperCase();
              if (resourceUpperName.equals(enteredFileUpper)) {
                return members[i].getName();
              }
            }
          } catch (CoreException e) {
          }
        }
      }
      return null;
    }

    protected boolean validatePage() {
      String fullFileName = getFileName();
      if (!extensionValidForContentType(fullFileName)) {
        setErrorMessage(NLS.bind(XMLWizardsMessages._ERROR_BAD_FILENAME_EXTENSION,
            getValidExtensions().toString()));
        return false;
      }
      // no fileExtension, let's check for this file with default file
      // extension
      if (fullFileName.lastIndexOf('.') == -1) {
        fullFileName += defaultFileExtension;
      }

      if ((getContainerFullPath() != null) && (getContainerFullPath().isEmpty() == false)
          && (getFileName().compareTo("") != 0)) //$NON-NLS-1$
      {
        Path fullPath = new Path(getContainerFullPath().toString() + '/' + fullFileName);
        IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(fullPath);
        if (resource != null) {
          setErrorMessage(XMLWizardsMessages._ERROR_FILE_ALREADY_EXISTS);
          return false;
        }
      }

      // check for file should be case insensitive
      String sameName = existsFileAnyCase(fullFileName);
      if (sameName != null) {
        // ISSUE: is qualitifedFileName not needed, or is it supposed
        // to be used in error message?
        // String qualifiedFileName =
        // getContainerFullPath().toString() + '/' + fullFileName;
        setErrorMessage(XMLWizardsMessages._ERROR_FILE_ALREADY_EXISTS + " " + sameName); //$NON-NLS-1$
        return false;
      }

      return super.validatePage();
    }

    public void createControl(Composite parent) {
      // inherit default container and name specification widgets
      super.createControl(parent);
      this.setFileName(computeDefaultFileName());
      setPageComplete(validatePage());
    }

    /**
     * Get list of valid extensions for XML Content type
     * 
     * @return List
     */
    List getValidExtensions() {
      if (fValidExtensions == null) {
        IContentType type = Platform.getContentTypeManager().getContentType(
            ContentTypeIdForXML.ContentTypeID_XML);
        fValidExtensions = new ArrayList(
            Arrays.asList(type.getFileSpecs(IContentType.FILE_EXTENSION_SPEC)));
      }
      return fValidExtensions;
    }

    /**
     * Verifies if fileName is valid name for content type. Takes base content type into
     * consideration.
     * 
     * @param fileName
     * @return true if extension is valid for this content type
     */
    boolean extensionValidForContentType(String fileName) {
      boolean valid = false;

      IContentType type = Platform.getContentTypeManager().getContentType(
          ContentTypeIdForXML.ContentTypeID_XML);
      // there is currently an extension
      if (fileName.lastIndexOf('.') != -1) {
        // check what content types are associated with current
        // extension
        IContentType[] types = Platform.getContentTypeManager().findContentTypesFor(fileName);
        int i = 0;
        while ((i < types.length) && !valid) {
          valid = types[i].isKindOf(type);
          ++i;
        }
      } else {
        valid = true; // no extension so valid
      }
      return valid;
    }
  }
}
