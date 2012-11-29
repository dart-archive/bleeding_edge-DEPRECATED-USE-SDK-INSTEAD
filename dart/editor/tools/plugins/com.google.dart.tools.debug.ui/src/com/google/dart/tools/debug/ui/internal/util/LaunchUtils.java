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
import com.google.dart.tools.core.internal.model.DartProjectImpl;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.HTMLFile;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.ui.internal.DartUtil;
import com.google.dart.tools.debug.ui.internal.browser.BrowserLaunchShortcut;
import com.google.dart.tools.debug.ui.internal.dartium.DartiumLaunchShortcut;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
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
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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
    for (ILaunchShortcut shortcut : shortcuts) {
      if (shortcut instanceof BrowserLaunchShortcut) {
        return shortcut;
      }
    }
    return null;
  }

  public static ILaunchShortcut getDartiumLaunchShortcut() {
    for (ILaunchShortcut shortcut : shortcuts) {
      if (shortcut instanceof DartiumLaunchShortcut) {
        return shortcut;
      }
    }
    return null;
  }

  /**
   * @return given an IResource, return the corresponding DartLibrary
   */
  public static DartLibrary[] getDartLibraries(IResource resource) {
    DartElement element = DartCore.create(resource);

    if (element instanceof CompilationUnit) {
      CompilationUnit unit = (CompilationUnit) element;

      return new DartLibrary[] {unit.getLibrary()};
    } else if (element instanceof DartLibrary) {
      return new DartLibrary[] {(DartLibrary) element};
    } else if (element instanceof HTMLFile) {
      HTMLFile htmlFile = (HTMLFile) element;

      try {
        return htmlFile.getReferencedLibraries();

      } catch (DartModelException exception) {
        DartUtil.logError(exception);
      }

    } else if (element instanceof DartProjectImpl) {
      try {
        return ((DartProjectImpl) element).getDartLibraries();
      } catch (DartModelException e) {

      }
    }
    return new DartLibrary[] {};

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
      IProject project = (IProject) resource;

      List<ILaunchConfiguration> launches = getLaunchesFor(project);

      if (launches.size() > 0) {
        return chooseLatest(launches);
      }
    }

    List<ILaunchConfiguration> configs = getExistingLaunchesFor(resource);

    if (configs.size() > 0) {
      return chooseLatest(configs);
    }

    // No existing configs - check if the current resource is not launchable.
    if (getApplicableLaunchShortcuts(resource).size() == 0) {
      // Try and locate a launchable library that references this library.
      DartLibrary[] libraries = getDartLibraries(resource);

      if (libraries.length > 0) {
        Set<ILaunchConfiguration> libraryConfigs = new HashSet<ILaunchConfiguration>();

        for (DartLibrary library : libraries) {
          for (DartLibrary referencingLib : library.getReferencingLibraries()) {
            IResource libResource = referencingLib.getCorrespondingResource();

            libraryConfigs.addAll(getExistingLaunchesFor(libResource));
          }
        }

        if (libraryConfigs.size() > 0) {
          return chooseLatest(libraryConfigs);
        }
      }
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

                if (next instanceof DartElement) {
                  next = ((DartElement) next).getResource();
                }

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

  /**
   * @param resource
   * @param config
   * @return whether the given launch config could be used to launch the given resource
   */
  public static boolean isLaunchableWith(IResource resource, ILaunchConfiguration config) {
    DartLaunchConfigWrapper launchWrapper = new DartLaunchConfigWrapper(config);

    DartLibrary[] testLibraries = LaunchUtils.getDartLibraries(resource);
    DartLibrary[] existingLibrary = LaunchUtils.getDartLibraries(launchWrapper.getApplicationResource());

    if (testLibraries.length > 0 & existingLibrary.length > 0) {
      for (DartLibrary testLibrary : testLibraries) {
        if (testLibrary.equals(existingLibrary[0])) {
          return true;
        }

      }
    }
    return false;
  }

  private LaunchUtils() {

  }

}
