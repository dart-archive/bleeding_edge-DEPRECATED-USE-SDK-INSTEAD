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
package com.google.dart.tools.debug.ui.internal.util;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.LightweightModel;
import com.google.dart.tools.core.dart2js.ProcessRunner;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;
import com.google.dart.tools.debug.ui.internal.DartUtil;
import com.google.dart.tools.debug.ui.internal.browser.BrowserLaunchShortcut;
import com.google.dart.tools.debug.ui.internal.browser.Messages;
import com.google.dart.tools.debug.ui.internal.dartium.DartiumLaunchShortcut;
import com.google.dart.tools.debug.ui.internal.mobile.MobileLaunchShortcut;
import com.google.dart.tools.debug.ui.internal.server.DartServerLaunchShortcut;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.internal.ui.views.console.ProcessConsole;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.ide.IDE;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A utility class for launching and launch configurations.
 */
@SuppressWarnings("restriction")
public class LaunchUtils {

  public static final String DARTIUM_LAUNCH_NAME = "Dartium launch";

  private static List<ILaunchShortcut> shortcuts;

  /**
   * Returns true if the given launch config can be launched w/o waiting on the builder.
   */
  public static boolean canFastLaunch(ILaunchConfiguration config) {
    DartLaunchConfigWrapper wrapper = new DartLaunchConfigWrapper(config);
    IProject project = wrapper.getProject();

    if (project == null) {
      return false;
    }

    // if pubspec.yaml is not up-to-date, return false
    IFile pubspecYamlFile = project.getFile(DartCore.PUBSPEC_FILE_NAME);

    if (pubspecYamlFile.exists()) {
      IFile pubspecLockFile = project.getFile(DartCore.PUBSPEC_LOCK_FILE_NAME);

      if (!pubspecLockFile.exists()) {
        return false;
      }

      if (pubspecLockFile.getLocalTimeStamp() < pubspecYamlFile.getLocalTimeStamp()) {
        return false;
      }
    }

    return true;
  }

  /**
   * Allow the user to choose one from a set of launch configurations.
   * 
   * @param configList
   * @return
   */
  public static ILaunchConfiguration chooseConfiguration(List<ILaunchConfiguration> configList) {
    IDebugModelPresentation labelProvider = DebugUITools.newDebugModelPresentation();

    ElementListSelectionDialog dialog = new ElementListSelectionDialog(
        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
        labelProvider);
    dialog.setElements(configList.toArray());
    dialog.setTitle("Select Dart Application");
    dialog.setMessage("&Select existing configuration:");
    dialog.setMultipleSelection(false);
    int result = dialog.open();
    labelProvider.dispose();

    if (result == Window.OK) {
      return (ILaunchConfiguration) dialog.getFirstResult();
    }

    return null;
  }

  public static ILaunchConfiguration chooseLatest(Collection<ILaunchConfiguration> launches) {
    long latestTime = 0;
    ILaunchConfiguration latestLaunch = null;

    for (ILaunchConfiguration launch : launches) {
      DartLaunchConfigWrapper wrapper = new DartLaunchConfigWrapper(launch);

      long time = wrapper.getLastLaunchTime();

      if (time > latestTime) {
        latestTime = time;
        latestLaunch = launch;
      }
    }

    return latestLaunch;
  }

  public static void clearDartiumConsoles() {
    IConsole[] consoles = ConsolePlugin.getDefault().getConsoleManager().getConsoles();
    for (IConsole console : consoles) {
      if (console instanceof ProcessConsole) {
        if (console.getName().contains(DARTIUM_LAUNCH_NAME)) {
          ((ProcessConsole) console).clearConsole();
        }
      }
    }
  }

  public static List<ILaunchConfiguration> getAllLaunches() {
    try {
      ILaunchConfiguration[] launchConfigs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations();
      List<ILaunchConfiguration> validLaunchConfigs = new ArrayList<ILaunchConfiguration>();

      for (ILaunchConfiguration config : launchConfigs) {
        IResource[] resources = config.getMappedResources();
        if (resources != null) {
          for (int i = 0; i < resources.length; i++) {
            IProject project = resources[i].getProject();
            if (project != null && project.exists()) {
              validLaunchConfigs.add(config);
            }
          }
        } else {
          validLaunchConfigs.add(config);
        }
      }

      return validLaunchConfigs;

    } catch (CoreException exception) {
      DartUtil.logError(exception);

      return Collections.emptyList();
    }
  }

  public static ILaunchConfiguration[] getAllLaunchesArray() {
    List<ILaunchConfiguration> configs = getAllLaunches();
    return configs.toArray(new ILaunchConfiguration[configs.size()]);
  }

  /**
   * @return a list of all the launch shortcuts in the system
   */
  public static List<ILaunchShortcut> getAllLaunchShortcuts() {
    if (shortcuts == null) {
      shortcuts = new ArrayList<ILaunchShortcut>();

      IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(
          IDebugUIConstants.PLUGIN_ID,
          IDebugUIConstants.EXTENSION_POINT_LAUNCH_SHORTCUTS);
      IConfigurationElement[] infos = extensionPoint.getConfigurationElements();

      try {
        for (IConfigurationElement element : infos) {
          ILaunchShortcut shortcut = (ILaunchShortcut) element.createExecutableExtension("class");

          shortcuts.add(shortcut);
        }
      } catch (CoreException ce) {
        DartUtil.logError(ce);
      }
    }

    return shortcuts;
  }

  public static List<ILaunchShortcut> getApplicableLaunchShortcuts(IResource resource) {
    List<ILaunchShortcut> candidates = new ArrayList<ILaunchShortcut>();

    for (ILaunchShortcut shortcut : getAllLaunchShortcuts()) {
      if (shortcut instanceof ILaunchShortcutExt) {
        ILaunchShortcutExt handler = (ILaunchShortcutExt) shortcut;

        if (handler.canLaunch(resource)) {
          candidates.add(shortcut);
        }
      }
    }

    return candidates;
  }

  public static ILaunchShortcut getBrowserLaunchShortcut() {
    for (ILaunchShortcut shortcut : getAllLaunchShortcuts()) {
      if (shortcut instanceof BrowserLaunchShortcut) {
        return shortcut;
      }
    }
    return null;
  }

  public static ILaunchShortcut getDartiumLaunchShortcut() {
    for (ILaunchShortcut shortcut : getAllLaunchShortcuts()) {
      if (shortcut instanceof DartiumLaunchShortcut) {
        return shortcut;
      }
    }
    return null;
  }

  public static List<ILaunchConfiguration> getExistingLaunchesFor(IResource resource) {
    Set<ILaunchConfiguration> configs = new LinkedHashSet<ILaunchConfiguration>();

    for (ILaunchShortcut shortcut : getAllLaunchShortcuts()) {
      if (shortcut instanceof ILaunchShortcutExt) {
        ILaunchShortcutExt handler = (ILaunchShortcutExt) shortcut;

        configs.addAll(Arrays.asList(handler.getAssociatedLaunchConfigurations(resource)));
      }
    }

    return new ArrayList<ILaunchConfiguration>(configs);
  }

  public static List<ILaunchConfiguration> getLaunchesFor(IProject project) {
    List<ILaunchConfiguration> launches = new ArrayList<ILaunchConfiguration>();

    for (ILaunchConfiguration config : LaunchUtils.getAllLaunches()) {
      try {
        if (config.getMappedResources() == null) {
          continue;
        }

        for (IResource resource : config.getMappedResources()) {
          if (project.equals(resource.getProject())) {
            if (!launches.contains(config)) {
              launches.add(config);
            }
          }
        }
      } catch (CoreException exception) {
        DartUtil.logError(exception);
      }
    }

    return launches;
  }

  /**
   * Return the best launch configuration to run for the given resource.
   * 
   * @param resource
   * @return
   * @throws DartModelException
   */
  public static ILaunchConfiguration getLaunchFor(IResource resource) throws DartModelException {
    // If it's a project, find any launches in that project.
    if (resource instanceof IProject) {
      ILaunchConfiguration config = getLaunchForProject((IProject) resource);
      if (config != null) {
        return config;
      }
    }

    List<ILaunchConfiguration> configs = getExistingLaunchesFor(resource);

    if (configs.size() > 0) {
      return chooseLatest(configs);
    }

    return null;
  }

  public static ILaunchConfiguration getLaunchForProject(IProject project) {
    List<ILaunchConfiguration> launches = getLaunchesFor(project);

    if (launches.size() > 0) {
      return chooseLatest(launches);
    }
    return null;
  }

  /**
   * @return a user-consumable long name for the launch config, like "foo.html from foo"
   */
  public static String getLongLaunchName(ILaunchConfiguration config) {
    DartLaunchConfigWrapper wrapper = new DartLaunchConfigWrapper(config);

    if (wrapper.getProject() != null) {
      return config.getName() + " from " + wrapper.getProject().getName();
    } else {
      return config.getName();
    }
  }

  public static ILaunchShortcut getMobileLaunchShortcut() {
    for (ILaunchShortcut shortcut : getAllLaunchShortcuts()) {
      if (shortcut instanceof MobileLaunchShortcut) {
        return shortcut;
      }
    }
    return null;
  }

  public static IResource getSelectedResource(IWorkbenchWindow window) {
    IWorkbenchPage page = window.getActivePage();

    if (page == null) {
      return null;
    }

    IWorkbenchPart part = page.getActivePart();

    if (part instanceof IEditorPart) {
      IEditorPart epart = (IEditorPart) part;

      return (IResource) epart.getEditorInput().getAdapter(IResource.class);
    } else if (part != null) {
      IWorkbenchPartSite site = part.getSite();

      if (site != null) {
        ISelectionProvider provider = site.getSelectionProvider();

        if (provider != null) {
          ISelection selection = provider.getSelection();

          if (selection instanceof IStructuredSelection) {
            IStructuredSelection ss = (IStructuredSelection) selection;

            if (!ss.isEmpty()) {
              Iterator<?> iterator = ss.iterator();

              while (iterator.hasNext()) {
                Object next = iterator.next();

                if (next instanceof IResource) {
                  return (IResource) next;
                } else if (next != null) {
                  IResource resource = (IResource) Platform.getAdapterManager().getAdapter(
                      next,
                      IResource.class);

                  if (resource != null) {
                    return resource;
                  }
                }
              }
            }
          }
        }
      }
    }

    if (page.getActiveEditor() != null) {
      return (IResource) page.getActiveEditor().getEditorInput().getAdapter(IResource.class);
    }

    return null;
  }

  public static ILaunchShortcut getServerLaunchShortcut() {
    for (ILaunchShortcut shortcut : getAllLaunchShortcuts()) {
      if (shortcut instanceof DartServerLaunchShortcut) {
        return shortcut;
      }
    }
    return null;
  }

  /**
   * Checks whether resource can be launched in Dartium or as JavaScript in browser
   * 
   * @param file the resource to check
   * @return
   */
  public static boolean isDartiumOrJsLaunchResource(IFile file) {

    if (DartCore.isHtmlLikeFileName(file.getName())
        && !DartCore.isInBuildDirectory(file.getParent())) {
      return true;
    }

    LightweightModel model = LightweightModel.getModel();

    if (model.isClientLibrary(file)) {
      return true;
    }
    return false;
  }

  /**
   * Launches the given launch configuration in the specified mode in a background job.
   * 
   * @param config the config to launch
   * @param mode the launch mode
   */
  public static void launch(final ILaunchConfiguration config, final String mode) {
    // If there are any dirty editors for the given project, save them now.
    DartLaunchConfigWrapper wrapper = new DartLaunchConfigWrapper(config);
    IProject project = wrapper.getProject();

    if (project != null) {
      IDE.saveAllEditors(new IResource[] {project}, false);
    }

    if (!canFastLaunch(config)) {
      try {
        // Wait on any existing builds (i.e., something like provisioning pub).
        IJobManager jobManager = Job.getJobManager();

        jobManager.join(ResourcesPlugin.FAMILY_MANUAL_BUILD, null);
        jobManager.join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
      } catch (OperationCanceledException e) {
        // user cancelled

      } catch (InterruptedException e) {
        DartDebugCorePlugin.logError(e);
      }
    }

    DebugUITools.launch(config, mode);
  }

  /**
   * Launch the given url using the browser specified in the preferences
   * 
   * @param url
   * @throws CoreException
   */
  public static void launchInExternalBrowser(final String url) throws CoreException {

    String browserName = DartDebugCorePlugin.getPlugin().getBrowserName();
    if (browserName.length() == 0) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          DartDebugUIPlugin.PLUGIN_ID,
          "Specify browser to launch in Preferences > Run and Debug"));
    }

    List<String> cmd = new ArrayList<String>();

    if (DartCore.isMac()) {
      // use open command on mac
      cmd.add("/usr/bin/open");
      cmd.add("-a");
    }
    cmd.add(browserName);
    cmd.add(url);

    if (DartDebugCorePlugin.getPlugin().getBrowserArgs().length() != 0) {
      if (DartCore.isMac()) {
        cmd.add("--args");
        cmd.add(DartDebugCorePlugin.getPlugin().getBrowserArgs());
      } else {
        cmd.addAll(Arrays.asList(DartDebugCorePlugin.getPlugin().getBrowserArgsAsArray()));
      }
    }

    try {
      ProcessBuilder builder = new ProcessBuilder(cmd);
      ProcessRunner runner = new ProcessRunner(builder);

      runner.runAsync();
      runner.await(new NullProgressMonitor(), 500);

      if (runner.getExitCode() != 0) {
        if (DartCore.isWindows()) {
          if (browserName.toLowerCase().indexOf("firefox") != -1) {
            if (runner.getExitCode() == 1) {
              // In this case, the application was opened in a new tab successfully.
              // Don't throw an exception.

              return;
            }
          }
        }

        throw new CoreException(new Status(
            IStatus.ERROR,
            DartDebugUIPlugin.PLUGIN_ID,
            "Could not launch browser \"" + browserName + "\" : \n\n" + runner.getStdErr()));
      }
    } catch (IOException e) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          DartDebugCorePlugin.PLUGIN_ID,
          Messages.BrowserLaunchConfigurationDelegate_BrowserNotFound,
          e));
    }
  }

  /**
   * Open the default browser with the given url
   * 
   * @param url
   * @throws CoreException
   */
  public static void openBrowser(String url) throws CoreException {
    IWebBrowser browser = null;
    try {
      browser = PlatformUI.getWorkbench().getBrowserSupport().createBrowser(
          IWorkbenchBrowserSupport.AS_EXTERNAL,
          "defaultBrowser",
          "Default Browser",
          "Browser");
      if (browser != null) {
        final IWebBrowser defaultBrowser = browser;
        final URL urlToOpen = new URL(url);

        Display.getDefault().asyncExec(new Runnable() {

          @Override
          public void run() {
            try {
              defaultBrowser.openURL(urlToOpen);
            } catch (PartInitException e) {
              DartDebugCorePlugin.logError(
                  Messages.BrowserLaunchConfigurationDelegate_DefaultBrowserNotFound,
                  e);
            }
          }
        });
      } else {
        throw new CoreException(new Status(
            IStatus.ERROR,
            DartDebugCorePlugin.PLUGIN_ID,
            Messages.BrowserLaunchConfigurationDelegate_DefaultBrowserNotFound));
      }
    } catch (MalformedURLException e) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          DartDebugCorePlugin.PLUGIN_ID,
          Messages.BrowserLaunchConfigurationDelegate_UrlError));
    }
  }

  private LaunchUtils() {

  }

}
