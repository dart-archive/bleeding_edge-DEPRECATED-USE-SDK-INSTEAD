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
package com.google.dart.tools.ui.wizard;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.CompilationUnitImpl;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModel;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.HTMLFile;
import com.google.dart.tools.ui.internal.dialogs.ResourceSelectionDialog;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;

public abstract class AbstractDartWizardPage extends WizardPage {

  /**
   * Handle to the root workspace
   */
  protected IWorkspaceRoot root = DartCore.create(ResourcesPlugin.getWorkspace().getRoot()).getWorkspace().getRoot();

  protected AbstractDartWizardPage(String pageName) {
    super(pageName);
  }

  /**
   * Launches the ContainerSelectionDialog to select the folder to put the new file in
   * 
   * @return-The string of the folder's directory
   */
  protected String[] chooseFileLocation() {
    ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(), root, true,
        "FolderSelection");
    dialog.setTitle("Folder Selection");
    dialog.setMessage("Select a folder for your new File.");
    if (root != null) {
      dialog.setInitialSelections(new Object[] {root});
    }
    if (dialog.open() == Window.OK) {
      String[] location = new String[2];
      if (dialog.getResult() == null || dialog.getResult().length == 0) {
        return null;
      }
      String osString = ((Path) dialog.getResult()[0]).toOSString();
      osString = osString.replaceFirst("/", "");
      location[0] = osString.split("/")[0];
      location[1] = osString.replaceFirst(location[0] + "/", "");
      return location;
    }

    return null;
  }

  protected String chooseLibrary() {
    ResourceSelectionDialog dialog = new ResourceSelectionDialog(getShell(), root,
        "Choose Only ONE Library");
    String[] extensions = {"app", "lib"};
    dialog.setDesiredExtensions(extensions);
    dialog.open();
    Object[] result = dialog.getResult();
    if (result == null || result.length == 0) {
      return null;
    }
    String[] sourcePaths = new String[result.length];
    for (int i = 0; i != result.length; ++i) {
      if (result[i] instanceof IResource) {
        sourcePaths[i] = ((IResource) result[i]).getFullPath().toOSString();
      }
    }
    return sourcePaths[0];
  }

  /**
   * Chooses the selected folder to use as default entry for the {@link #folderNameField} If a
   * project is selected then returns the src folder within that project. If no project is selected
   * it returns an empty String
   * 
   * @return-The path of the folder chosen as default
   */
  protected String getDefaultFolder() {
    ISelection selectedFolder = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getSelection();
    if (selectedFolder instanceof TreeSelection) {
      Object firstElement = ((TreeSelection) selectedFolder).getFirstElement();
      if (firstElement != null) {
        if (firstElement instanceof IProject) {
          IProject project = (IProject) firstElement;
          if (project.getFolder(new Path("/src")).exists()) {
            return project.getFolder(new Path("/src")).getName();
          }
          return "";
        } else if (firstElement instanceof IFolder) {
          return ((IFolder) firstElement).getName();
        } else if (firstElement instanceof CompilationUnitImpl) {
          try {
            IContainer folder = ((CompilationUnitImpl) firstElement).getParent().getCorrespondingResource().getParent();
            return folder.getName();
          } catch (DartModelException e) {
            return "";
          }
        }
      }
    }
    return "";
  }

  /**
   * Chooses the selected project to use as default entry for the {@link #projectNameField} If no
   * project is selected returns the empty String
   * 
   * @return-The path of the folder chosen as default
   */
  protected String getDefaultProject() {
    ISelection selectedFolder = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getSelection();
    if (selectedFolder instanceof TreeSelection) {
      Object firstElement = ((TreeSelection) selectedFolder).getFirstElement();
      if (firstElement != null) {
        if (firstElement instanceof IProject) {
          IProject project = (IProject) firstElement;
          return project.getName();
        } else if (firstElement instanceof IContainer) {
          IContainer folder = (IContainer) firstElement;
          return folder.getProject().getName();
        } else if (firstElement instanceof CompilationUnitImpl) {
          try {
            IContainer folder = ((CompilationUnitImpl) firstElement).getParent().getCorrespondingResource().getProject();
            return folder.getName();
          } catch (DartModelException e) {
            return "";
          }
        }
      }
    }
    return "";
  }

  /**
   * Returns the selected {@link DartLibrary}. If there is no selected library at the time of the
   * launch of this wizard, then return the {@link DartLibrary} that the selected element is most
   * closely associated to. If no such library can be found, return <code>null</code>.
   * 
   * @return the library, determined by the context of the selected element
   */
  protected DartLibrary getSelectedLibrary() {
    ISelection selectedFolder = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getSelection();
    DartLibrary foundLibrary = null;
    Object element = null;
    if (selectedFolder instanceof TreeSelection) {
      element = ((TreeSelection) selectedFolder).getFirstElement();
    }
    if (element == null) {
      element = EditorUtility.getActiveEditorJavaInput();
    }
    if (element != null) {
      if (element instanceof DartElement) {
        foundLibrary = getParentDartLibrary((DartElement) element);
        if (foundLibrary == null && element instanceof HTMLFile) {
          HTMLFile htmlFile = (HTMLFile) element;
          try {
            DartLibrary[] libraries = htmlFile.getReferencedLibraries();
            if (libraries.length > 0) {
              foundLibrary = libraries[0];
            }
          } catch (DartModelException e) {
          }
        }
      }
    }

    if (foundLibrary != null) {
      return foundLibrary;
    } else {
      // No library selected, return null
      return null;
    }
  }

  /**
   * Handle the "Browse..." button for file folder location by launching the proper dialog and then
   * updating the proper text field
   */
  protected void handleBrowseButton(Text locationField) {
    String directoryOnDisk = chooseDirectoryOnDisk();
    if (directoryOnDisk != null) {
      locationField.setText(directoryOnDisk);
      // this triggers the listener if needed, and updateLibraryLocation() is called by our listener on libraryPathField
      return;
    }
    // user didn't make a selection in the dialog, do nothing.
  }

  private String chooseDirectoryOnDisk() {
    DirectoryDialog directoryDialog = new DirectoryDialog(getShell());
    return directoryDialog.open();
  }

  private DartLibrary getParentDartLibrary(DartElement element) {
    if (element == null || element instanceof DartModel) {
      return null;
    } else if (element instanceof DartLibrary) {
      return (DartLibrary) element;
    }
    return getParentDartLibrary(element.getParent());
  }

}
