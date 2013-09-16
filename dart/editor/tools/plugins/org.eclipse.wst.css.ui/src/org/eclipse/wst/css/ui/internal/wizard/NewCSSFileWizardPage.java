/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.wizard;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.eclipse.wst.css.core.internal.CSSCorePlugin;
import org.eclipse.wst.css.core.internal.preferences.CSSCorePreferenceNames;
import org.eclipse.wst.css.core.internal.provisional.contenttype.ContentTypeIdForCSS;
import org.eclipse.wst.css.ui.internal.CSSUIMessages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class NewCSSFileWizardPage extends WizardNewFileCreationPage {

  private static final String defaultName = "NewFile"; //$NON-NLS-1$
  private IContentType fContentType;
  private List fValidExtensions = null;

  public NewCSSFileWizardPage(String pageName, IStructuredSelection selection) {
    super(pageName, selection);
  }

  public void createControl(Composite parent) {
    // inherit default container and name specification widgets
    super.createControl(parent);
    setFileName(computeDefaultFileName());
    setPageComplete(validatePage());
  }

  protected String computeDefaultFileName() {
    int count = 0;
    String fileName = addDefaultExtension(defaultName);
    IPath containerFullPath = getContainerFullPath();
    if (containerFullPath != null) {
      while (true) {
        IPath path = containerFullPath.append(fileName);
        if (ResourcesPlugin.getWorkspace().getRoot().exists(path)) {
          count++;
          fileName = addDefaultExtension(defaultName + count);
        } else {
          break;
        }
      }
    }
    return fileName;
  }

  /**
   * This method is overriden to set the selected folder to web contents folder if the current
   * selection is outside the web contents folder.
   */
  protected void initialPopulateContainerNameField() {
    super.initialPopulateContainerNameField();

    IPath fullPath = getContainerFullPath();
    IProject project = getProjectFromPath(fullPath);
    IPath root = FacetModuleCoreSupport.getRootContainerForPath(project, fullPath);
    if (root != null) {
      return;
    }
    root = FacetModuleCoreSupport.getDefaultRootContainer(project);
    if (root != null) {
      setContainerFullPath(root);
      return;
    }
  }

  /**
   * This method is overriden to set additional validation specific to css files.
   */
  protected boolean validatePage() {
    setMessage(null);
    setErrorMessage(null);

    if (!super.validatePage()) {
      return false;
    }

    String fileName = getFileName();
    IPath fullPath = getContainerFullPath();
    if ((fullPath != null) && (fullPath.isEmpty() == false) && (fileName != null)) {
      // check that filename does not contain invalid extension
      if (!extensionValidForContentType(fileName)) {
        setErrorMessage(NLS.bind(CSSUIMessages._ERROR_FILENAME_MUST_END_CSS,
            getValidExtensions().toString()));
        return false;
      }
      // no file extension specified so check adding default
      // extension doesn't equal a file that already exists
      if (fileName.lastIndexOf('.') == -1) {
        String newFileName = addDefaultExtension(fileName);
        IPath resourcePath = fullPath.append(newFileName);

        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IStatus result = workspace.validatePath(resourcePath.toString(), IResource.FOLDER);
        if (!result.isOK()) {
          // path invalid
          setErrorMessage(result.getMessage());
          return false;
        }

        if ((workspace.getRoot().getFolder(resourcePath).exists() || workspace.getRoot().getFile(
            resourcePath).exists())) {
          setErrorMessage(CSSUIMessages.ResourceGroup_nameExists);
          return false;
        }
      }

      // get the IProject for the selection path
      IProject project = getProjectFromPath(fullPath);
      // if inside web project, check if inside webContent folder
      if (project != null && isWebProject(project)) {
        // check that the path is inside the webContent folder
        IPath[] webContentPaths = FacetModuleCoreSupport.getAcceptableRootPaths(project);
        boolean isPrefix = false;
        for (int i = 0; !isPrefix && i < webContentPaths.length; i++) {
          isPrefix |= webContentPaths[i].isPrefixOf(fullPath);
        }
        if (!isPrefix) {
          setMessage(CSSUIMessages._WARNING_FOLDER_MUST_BE_INSIDE_WEB_CONTENT, WARNING);
        }
      }
    }

    return true;
  }

  /**
   * Get content type associated with this new file wizard
   * 
   * @return IContentType
   */
  private IContentType getContentType() {
    if (fContentType == null)
      fContentType = Platform.getContentTypeManager().getContentType(
          ContentTypeIdForCSS.ContentTypeID_CSS);
    return fContentType;
  }

  /**
   * Get list of valid extensions for CSS Content type
   * 
   * @return
   */
  private List getValidExtensions() {
    if (fValidExtensions == null) {
      IContentType type = getContentType();
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
  private boolean extensionValidForContentType(String fileName) {
    boolean valid = false;

    IContentType type = getContentType();
    // there is currently an extension
    if (fileName.lastIndexOf('.') != -1) {
      // check what content types are associated with current extension
      IContentType[] types = Platform.getContentTypeManager().findContentTypesFor(fileName);
      int i = 0;
      while (i < types.length && !valid) {
        valid = types[i].isKindOf(type);
        ++i;
      }
    } else
      valid = true; // no extension so valid
    return valid;
  }

  /**
   * Adds default extension to the filename
   * 
   * @param filename
   * @return
   */
  String addDefaultExtension(String filename) {
    StringBuffer newFileName = new StringBuffer(filename);

    Preferences preference = CSSCorePlugin.getDefault().getPluginPreferences();
    String ext = preference.getString(CSSCorePreferenceNames.DEFAULT_EXTENSION);

    newFileName.append("."); //$NON-NLS-1$
    newFileName.append(ext);

    return newFileName.toString();
  }

  /**
   * Returns the project that contains the specified path
   * 
   * @param path the path which project is needed
   * @return IProject object. If path is <code>null</code> the return value is also
   *         <code>null</code>.
   */
  private IProject getProjectFromPath(IPath path) {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IProject project = null;

    if (path != null) {
      if (workspace.validatePath(path.toString(), IResource.PROJECT).isOK()) {
        project = workspace.getRoot().getProject(path.toString());
      } else {
        project = workspace.getRoot().getFile(path).getProject();
      }
    }

    return project;
  }

  /**
   * Checks if the specified project is a web project.
   * 
   * @param project project to be checked
   * @return true if the project is web project, otherwise false
   */
  private boolean isWebProject(IProject project) {
    return FacetModuleCoreSupport.isWebProject(project);
  }

}
