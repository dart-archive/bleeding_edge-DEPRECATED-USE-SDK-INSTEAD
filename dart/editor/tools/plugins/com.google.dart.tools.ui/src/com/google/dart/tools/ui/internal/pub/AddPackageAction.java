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
package com.google.dart.tools.ui.internal.pub;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.pub.PubCacheManager_OLD;
import com.google.dart.tools.ui.actions.InstrumentedSelectionDispatchAction;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.projects.NewApplicationCreationPage.ProjectType;
import com.google.dart.tools.ui.internal.projects.ProjectUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

/**
 * Installs the package from pub.dartlang to pub cache and opens the package as a new folder
 */
public class AddPackageAction extends InstrumentedSelectionDispatchAction {

  private static String PUBSPEC = "name: sample\n" + "dependencies:\n  ";

  private String packageName;
  private String version;
  private IWorkbenchWindow window;

  public AddPackageAction(IWorkbenchPartSite site, String packageName, String version) {
    super(site);
    this.packageName = packageName;
    this.version = version;
    window = site.getWorkbenchWindow();
  }

  @Override
  protected void doRun(Event event, UIInstrumentationBuilder instrumentation) {

    // create project
    String dirName = packageName + "-" + version;
    File potentialDir = new File(DartCore.getUserDefaultDartFolder(), dirName);
    final File newProjectDir = ProjectUtils.generateUniqueSampleDirFrom(
        dirName + "_copy",
        potentialDir);

    Control focusControl = Display.getCurrent().getFocusControl();
    try {
      IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
      progressService.busyCursorWhile(new IRunnableWithProgress() {

        @Override
        public void run(IProgressMonitor monitor) throws InvocationTargetException,
            InterruptedException {
          installAndCopyPackage(newProjectDir, monitor);
        }
      });

    } catch (Throwable ie) {

    } finally {
      if (focusControl != null) {
        focusControl.setFocus();
      }
    }

  }

  private void installAndCopyPackage(final File newProjectDir, IProgressMonitor monitor) {

    if (!isPackageInstalled()) {
      // run pub install
      String pubspec = PUBSPEC + packageName + ": " + version.trim();
      if (PubPackageUtils.createPubspec(newProjectDir, pubspec, monitor)) {
        if (PubPackageUtils.runPubInstall(newProjectDir, monitor)) {
          // copy contents
          String location = PubPackageUtils.getPackageCacheDir(monitor, packageName, version);
          if (location != null && !location.isEmpty()) {
            PubPackageUtils.copyPackageContents(newProjectDir, location, monitor);
            openProject(newProjectDir);
          }
        } else {

        }
      }
    } else {
      String location = PubCacheManager_OLD.getInstance().getCacheLocation(packageName, version);
      if (location != null) {
        PubPackageUtils.copyPackageContents(newProjectDir, location, monitor);
        openProject(newProjectDir);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private boolean isPackageInstalled() {
    HashMap<String, Object> allPackages = PubCacheManager_OLD.getInstance().getAllCachePackages();
    HashMap<String, Object> map = (HashMap<String, Object>) allPackages.get(packageName);
    if (map != null && map.keySet().contains(version)) {
      return true;
    }
    return false;
  }

  private void openProject(final File newProjectDir) {
    final String newProjectName = newProjectDir.getName();
    final IProject newProjectHandle = ResourcesPlugin.getWorkspace().getRoot().getProject(
        newProjectName);

    Display.getDefault().asyncExec(new Runnable() {

      @Override
      public void run() {
        ProjectUtils.createNewProject(
            newProjectName,
            newProjectHandle,
            ProjectType.NONE,
            newProjectDir.toURI(),
            window,
            window.getShell());

      }
    });
  }

}
