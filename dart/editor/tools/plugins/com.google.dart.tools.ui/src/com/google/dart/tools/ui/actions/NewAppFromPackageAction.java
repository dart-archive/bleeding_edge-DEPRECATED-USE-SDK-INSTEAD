/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.ui.actions;

import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.filesview.nodes.old.pkgs.DartPackageNode_OLD;
import com.google.dart.tools.ui.internal.projects.NewApplicationCreationPage.ProjectType;
import com.google.dart.tools.ui.internal.projects.ProjectUtils;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkbenchWindow;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;

/**
 * Copies the contents of the package selected in the Files View > Installed Packages node and
 * creates a new application/folder in the users default dart directory.
 */
public class NewAppFromPackageAction extends InstrumentedSelectionDispatchAction {

  private IWorkbenchWindow window;

  public NewAppFromPackageAction(IWorkbenchSite site) {
    super(site);
    setText("New Application from package");
    setDescription("Create new application with contents of selected pacakge");
    window = site.getWorkbenchWindow();
  }

  public void openPackage(final IFileStore packageDir) {
    try {
      window.run(true, false, new IRunnableWithProgress() {
        @Override
        public void run(IProgressMonitor monitor) throws InvocationTargetException,
            InterruptedException {
          copyPackage(packageDir, monitor, window);
        }
      });
    } catch (InvocationTargetException e) {
      DartToolsPlugin.log(e);
    } catch (InterruptedException e) {
      DartToolsPlugin.log(e);
    }
  }

  @Override
  protected void doRun(IStructuredSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    if (!selection.isEmpty() && selection.getFirstElement() instanceof DartPackageNode_OLD) {
      DartPackageNode_OLD node = (DartPackageNode_OLD) selection.getFirstElement();
      openPackage(node.getFileStore());
    }
  }

  private void copyPackage(final IFileStore packageDir, final IProgressMonitor monitor,
      final IWorkbenchWindow window) {

    String packageName = packageDir.getName();
    File potentialDir = new File(DartCore.getUserDefaultDartFolder(), packageName);
    final File newProjectDir = ProjectUtils.generateUniqueSampleDirFrom(
        packageName + "_copy",
        potentialDir);

    final String newProjectName = newProjectDir.getName();
    final IProject newProjectHandle = ResourcesPlugin.getWorkspace().getRoot().getProject(
        newProjectName);
    final URI location = newProjectDir.toURI();
    final File fileToOpen = new File(newProjectDir, getFilePath(packageName));
    IFileSystem fileSystem = EFS.getLocalFileSystem();
    final IFileStore destination = fileSystem.fromLocalFile(newProjectDir);

    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {

        try {
          packageDir.copy(destination, 0, monitor);
        } catch (CoreException e1) {
          DartToolsPlugin.log(e1);
        }

        try {
          ProjectUtils.createNewProject(
              newProjectName,
              newProjectHandle,
              ProjectType.NONE,
              location,
              window,
              window.getShell());

          Project project = DartCore.getProjectManager().getProject(newProjectHandle);
          IFile resource = (IFile) project.getResource(new FileBasedSource(fileToOpen));
          if (resource.exists()) {
            EditorUtility.openInTextEditor(resource, true);
          }

        } catch (CoreException e) {
          DartToolsPlugin.log(e);
        }
      }
    });

  }

  private String getFilePath(String packageName) {
    packageName = packageName.substring(0, packageName.indexOf("-"));
    return "lib/" + packageName + ".dart";
  }

}
