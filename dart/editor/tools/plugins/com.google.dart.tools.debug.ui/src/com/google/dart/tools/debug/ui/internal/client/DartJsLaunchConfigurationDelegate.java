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
package com.google.dart.tools.debug.ui.internal.client;

import com.google.dart.tools.core.generator.DartHtmlGenerator;
import com.google.dart.tools.core.internal.builder.ArtifactProvider;
import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;
import com.google.dart.tools.debug.ui.internal.DartUtil;
import com.google.dart.tools.debug.ui.internal.DebugErrorHandler;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import java.io.File;

/**
 * Launches the Dart application as either a web client or server application
 */
public class DartJsLaunchConfigurationDelegate extends LaunchConfigurationDelegate {

  /**
   *
   */
  private static final String EMBEDDED_BROWSER_NA = "embeddedBrowserNotAvailableWarning";

  /** Browser editor identifier */
  // private static final String BROWSER_ID = "org.eclipse.ui.browser.editor";

  @Override
  public void launch(ILaunchConfiguration config, String mode, ILaunch launch,
      IProgressMonitor monitor) throws CoreException {

    // Determine the launch type
    int launchType = DartUtil.getLaunchType(config);

    // Determine the file to be launched/opened
    final String path = DartUtil.getResourcePath(config);
    if (path == null || path.length() == 0) {
      throwCoreException("Unspecified resource to be launched");
    }
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    final IFile file = root.getFile(new Path(path));
    if (!file.exists()) {
      throwCoreException("Resource to be launched does not exist: " + path);
    }

    // Determine if the page should be displayed in an external browser
    boolean external = DartUtil.isExternalBrowser(config);

    if (ILaunchManager.RUN_MODE.equals(mode)) {
      if (launchType == ILaunchConstants.LAUNCH_TYPE_WEB_CLIENT) {
        runWebClient(file, external);
      } else {
        runServerApplication(file);
      }
    } else if (ILaunchManager.DEBUG_MODE.equals(mode)) {
      if (launchType == ILaunchConstants.LAUNCH_TYPE_WEB_CLIENT) {
        debugWebClient(file, external);
      } else {
        debugServerApplication(file);
      }
    } else {
      throwCoreException("Launch mode not supported: " + mode);
    }
  }

  /**
   * Launch the specified server application in a debug session
   * 
   * @param file the Dart application or source file
   */
  private void debugServerApplication(IFile file) {
    DartUtil.notYetImplemented(file);
    String serviceName = "Debug server application";
    notImplementedYet(serviceName);
  }

  /**
   * Launch the specified web client in a debug session
   * 
   * @param file the web page or Dart application
   * @param external <code>true</code> if the debug session should use an external browser or false
   *          if the web client should be displayed in a browser embedded in Eclipse.
   */
  private void debugWebClient(IFile file, boolean external) {
    DartUtil.notYetImplemented(file);
    String serviceName = "Debug web client";
    notImplementedYet(serviceName);
  }

  /**
   * Based upon the specified file, return the web page to be displayed in the browser. If the
   * specified file is a web page, then return it. Otherwise create a new web page referencing that
   * file.
   * 
   * @param file a web page or a Dart application
   * @return a web page to display the Dart application (not <code>null</code>)
   */
  private IFile getWebPage(IFile file) throws CoreException {

    // If it is already a web page, then just return it
    if (DartUtil.isWebPage(file)) {
      return file;
    }

    // Look for an HTML file with the same name in the folder hierarchy
    String name = file.getName();
    String extension = file.getFileExtension();
    if (extension != null) {
      name = name.substring(0, name.length() - extension.length() - 1);
    }
    name += ".html";
    IContainer container = file.getParent();
    while (container.getType() != IResource.ROOT) {
      IFile htmlFile = container.getFile(new Path(name));
      if (htmlFile.exists()) {
        return htmlFile;
      }
      container = container.getParent();
    }

    // Otherwise, assume it is a Dart app, and return a web page displaying it
    File appJsFile = ArtifactProvider.getJsAppArtifactFile(file);
    if (!appJsFile.exists()) {
      throwCoreException("Compiled Dart application does not exist: " + appJsFile);
    }
    container = file.getParent();
    IFile htmlFile = container.getFile(new Path(file.getName()).removeFileExtension().append("html"));
    if (htmlFile.exists()) {
      return htmlFile;
    }

    // Exclude the file extension from the title
    String title = file.getName();
    int index = title.lastIndexOf('.');
    if (index > 0) {
      title = title.substring(0, index);
    }

    // If the web page does not exist, then create it
    DartHtmlGenerator generator = new DartHtmlGenerator(true);
    generator.setContainer(container);
    // TODO (danrubel) need to modify generator to take a File rather than IFile
    //generator.setDartAppFile(jsFile);
    generator.setName(appJsFile.getName());
    generator.setTitle(title);
    generator.execute(new NullProgressMonitor());

    setDerived(htmlFile);
    return htmlFile;
  }

  // TODO (danrubel) implement callers and remove this method
  private void notImplementedYet(final String serviceName) {
    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        MessageDialog.openInformation(null, "Not Implemented", serviceName
            + " is not implemented yet.");
      }
    });
  }

  /**
   * Run the specified Dart server application
   * 
   * @param file the Dart application or source file
   */
  private void runServerApplication(IFile file) {
    DartUtil.notYetImplemented(file);
    String serviceName = "Run server application";
    notImplementedYet(serviceName);
  }

  /**
   * Run the specified Dart web client
   * 
   * @param file the web page or Dart application
   * @param external <code>true</code> if the launch session should use an external browser or false
   *          if the web client should be displayed in a browser embedded in Eclipse.
   */
  private void runWebClient(IFile file, final boolean external) throws CoreException {
    final IFile webpage = getWebPage(file);
    final IWorkbench workbench = PlatformUI.getWorkbench();
    workbench.getDisplay().asyncExec(new Runnable() {

      @Override
      public void run() {
        try {
          openBrowser(webpage, external);
        } catch (Exception e) {
          DartUtil.logError(e);
        }
      }

      private void openBrowser(final IFile file, final boolean external) throws Exception {
        IWorkbenchBrowserSupport browserSupport = workbench.getBrowserSupport();
        IWebBrowser browser;
        if (external) {
          try {
            browser = browserSupport.getExternalBrowser();
          } catch (PartInitException e) {
            DebugErrorHandler.errorDialog(null, "Launch Failed", e.getMessage(), e);
            throw e;
          }
        } else {
          if (!browserSupport.isInternalWebBrowserAvailable()) {
            warnExternalBrowserNA(workbench);
          }
          browser = browserSupport.createBrowser(null);
        }
        browser.openURL(file.getLocationURI().toURL());
      }

      private void warnExternalBrowserNA(final IWorkbench workbench) {
        Shell shell = workbench.getActiveWorkbenchWindow().getShell();
        IPreferenceStore prefs = DartDebugUIPlugin.getDefault().getPreferenceStore();
        if (prefs.getBoolean(EMBEDDED_BROWSER_NA)) {
          return;
        }
        MessageDialogWithToggle.openInformation(shell, "Embedded Browser",
            "Embedded Browser not available", "Don't show this again", false, prefs,
            EMBEDDED_BROWSER_NA);
      }
    });
  }

  @SuppressWarnings("deprecation")
  private void setDerived(IFile htmlFile) throws CoreException {
    // Switch to Eclipse 3.5 friendly API
    // htmlFile(true, new NullProgressMonitor());
    htmlFile.setDerived(true);
  }

  /**
   * Throw a core exception with the specified message
   * 
   * @param message the message
   */
  private void throwCoreException(String message) throws CoreException {
    throw new CoreException(new Status(IStatus.ERROR, DartDebugUIPlugin.PLUGIN_ID, message));
  }
}
