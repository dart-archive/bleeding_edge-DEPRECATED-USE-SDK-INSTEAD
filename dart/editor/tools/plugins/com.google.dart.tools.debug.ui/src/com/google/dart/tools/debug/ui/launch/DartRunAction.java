/*
 * Copyright 2012 Dart project authors.
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

package com.google.dart.tools.debug.ui.launch;

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;
import com.google.dart.tools.debug.ui.internal.DartUtil;
import com.google.dart.tools.debug.ui.internal.util.ILaunchShortcutExt;
import com.google.dart.tools.debug.ui.internal.util.LaunchUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A toolbar action to enumerate a launch debug launch configurations.
 */
public class DartRunAction extends DartAbstractAction {

  public DartRunAction(IWorkbenchWindow window) {
    this(window, false);
  }

  public DartRunAction(IWorkbenchWindow window, boolean noMenu) {
    super(window, "Run", noMenu ? IAction.AS_PUSH_BUTTON : IAction.AS_DROP_DOWN_MENU);

    setActionDefinitionId("com.google.dart.tools.debug.ui.run");
    setImageDescriptor(DartDebugUIPlugin.getImageDescriptor("obj16/run_exc.gif"));
  }

  @Override
  public void run() {
    IResource resource = getSelectedResource();

    if (resource == null) {
      chooseAndLaunch(getAllLaunches());

      return;
    }

    // TODO(devoncarew): we should first check if any existing launch configs match the given resource

    // TODO(devoncarew): then check if any config types can launch it

    List<ILaunchShortcut> shortcuts = LaunchUtils.getLaunchShortcuts();

    List<ILaunchShortcut> candidates = new ArrayList<ILaunchShortcut>();

    for (ILaunchShortcut shortcut : shortcuts) {
      if (shortcut instanceof ILaunchShortcutExt) {
        ILaunchShortcutExt handler = (ILaunchShortcutExt) shortcut;

        if (handler.canLaunch(resource)) {
          candidates.add(shortcut);
        }
      }
    }

    ISelection sel = new StructuredSelection(resource);

    if (candidates.size() == 0 && resource instanceof IProject) {
      IProject project = (IProject) resource;

      chooseAndLaunch(getLaunchesFor(project));
    } else {
      if (candidates.size() == 0) {
        MessageDialog.openInformation(getWindow().getShell(), "Unable to Run", "Unable to run "
            + resource.getName() + ".");
      } else if (candidates.size() == 1) {
        launch(candidates.get(0), sel);
      } else {
        Set<ILaunchConfiguration> configs = new LinkedHashSet<ILaunchConfiguration>();

        for (ILaunchShortcut shortcut : candidates) {
          ILaunchShortcutExt handler = (ILaunchShortcutExt) shortcut;

          configs.addAll(Arrays.asList(handler.getAssociatedLaunchConfigurations(resource)));
        }

        if (configs.size() == 0) {
          launch(candidates.get(0), sel);
        } else if (configs.size() > 0) {
          chooseAndLaunch(new ArrayList<ILaunchConfiguration>(configs));
        }
      }
    }
  }

  protected IResource getSelectedResource() {
    IWorkbenchPage page = getWindow().getActivePage();

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

                IResource resource = (IResource) Platform.getAdapterManager().getAdapter(next,
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

    if (page.getActiveEditor() != null) {
      return (IResource) page.getActiveEditor().getEditorInput().getAdapter(IResource.class);
    }

    return null;
  }

  private boolean chooseAndLaunch(List<ILaunchConfiguration> launches) {
    if (launches.size() == 0) {
      return false;
    } else if (launches.size() == 1) {
      launch(launches.get(0));

      return true;
    } else {
      ILaunchConfiguration config = LaunchUtils.chooseConfiguration(launches);

      if (config != null) {
        launch(config);

        return true;
      } else {
        return false;
      }
    }
  }

  private List<ILaunchConfiguration> getAllLaunches() {
    try {
      return Arrays.asList(DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations());
    } catch (CoreException exception) {
      DartUtil.logError(exception);

      return Collections.emptyList();
    }
  }

  private List<ILaunchConfiguration> getLaunchesFor(IProject project) {
    List<ILaunchConfiguration> launches = new ArrayList<ILaunchConfiguration>();

    for (ILaunchConfiguration config : getAllLaunches()) {
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
}
