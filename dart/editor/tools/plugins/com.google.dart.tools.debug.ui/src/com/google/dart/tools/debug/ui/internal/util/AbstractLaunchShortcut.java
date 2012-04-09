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
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.debug.ui.internal.DartUtil;
import com.google.dart.tools.debug.ui.internal.DebugErrorHandler;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.ILaunchShortcut2;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An abstract parent of Dart launch shortcuts.
 */
public abstract class AbstractLaunchShortcut implements ILaunchShortcut2 {
  private String launchTypeLabel;

  /**
   * Create a new AbstractLaunchShortcut.
   * 
   * @param launchTypeLabel
   */
  public AbstractLaunchShortcut(String launchTypeLabel) {
    this.launchTypeLabel = launchTypeLabel;
  }

  public ILaunchConfiguration[] getAssociatedLaunchConfigurations(IResource resource) {
    List<ILaunchConfiguration> results = new ArrayList<ILaunchConfiguration>();

    try {
      ILaunchConfiguration[] configs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(
          getConfigurationType());

      for (int i = 0; i < configs.length; i++) {
        ILaunchConfiguration config = configs[i];

        if (testSimilar(resource, config)) {
          results.add(config);
        }
      }
    } catch (CoreException e) {
      DartUtil.logError(e);
    }

    return results.toArray(new ILaunchConfiguration[results.size()]);
  }

  @Override
  public final IResource getLaunchableResource(IEditorPart editor) {
    IResource resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);

    try {
      if (resource != null) {
        return getLaunchableResource(resource);
      }
    } catch (DartModelException e) {
      DebugErrorHandler.errorDialog(null, "Error Launching " + launchTypeLabel,
          "Unable to locate launchable resource.", e);
      return null;
    }

    return null;
  }

  @Override
  public final IResource getLaunchableResource(ISelection selection) {
    if (!(selection instanceof IStructuredSelection)) {
      return null;
    }

    Object elem = ((IStructuredSelection) selection).getFirstElement();

    Object res = null;
    if (elem instanceof IResource) {
      res = elem;
    } else if (elem instanceof DartElement) {
      res = elem;
    } else if (elem instanceof IAdaptable) {
      res = ((IAdaptable) elem).getAdapter(IResource.class);
    }
    try {
      return getLaunchableResource(res);

    } catch (DartModelException e) {
      DebugErrorHandler.errorDialog(null, "Error Launching " + launchTypeLabel,
          "Unable to locate launchable resource.", e);
      return null;
    }
  }

  @Override
  public final ILaunchConfiguration[] getLaunchConfigurations(IEditorPart editorpart) {
    // let the framework resolve configurations based on resource mapping
    return null;
  }

  @Override
  public final ILaunchConfiguration[] getLaunchConfigurations(ISelection selection) {
    // let the framework resolve configurations based on resource mapping
    return null;
  }

  @Override
  public final void launch(IEditorPart editor, String mode) {
    IResource resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);

    if (resource != null) {
      try {
        resource = getLaunchableResource(resource);

        if (resource != null) {
          launch(resource, mode);

          return;
        }
      } catch (DartModelException e) {
        DebugErrorHandler.errorDialog(null, "Error Launching " + launchTypeLabel,
            "Unable to locate launchable resource.", e);
        return;
      }
    }

    MessageDialog.openWarning(null, "Error Launching " + launchTypeLabel,
        "Unable to locate launchable resource.");
  }

  @Override
  public final void launch(ISelection selection, String mode) {
    launch(getLaunchableResource(selection), mode);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

  /**
   * Returns a configuration from the given collection of configurations that should be launched, or
   * <code>null</code> to cancel. Default implementation opens a selection dialog that allows the
   * user to choose one of the specified launch configurations. Returns the chosen configuration, or
   * <code>null</code> if the user cancels.
   * 
   * @param configList list of configurations to choose from
   * @return configuration to launch or <code>null</code> to cancel
   */
  protected ILaunchConfiguration chooseConfiguration(List<ILaunchConfiguration> configList) {
    IDebugModelPresentation labelProvider = DebugUITools.newDebugModelPresentation();
    ElementListSelectionDialog dialog = new ElementListSelectionDialog(
        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), labelProvider);
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

  /**
   * Find the launch configuration associated with the specified resource
   * 
   * @param resource the resource
   * @return the launch configuration or <code>null</code> if none
   */
  protected final ILaunchConfiguration findConfig(IResource resource) {
    List<ILaunchConfiguration> candidateConfigs = Collections.emptyList();

    try {
      ILaunchConfiguration[] configs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(
          getConfigurationType());

      candidateConfigs = new ArrayList<ILaunchConfiguration>(configs.length);

      for (int i = 0; i < configs.length; i++) {
        ILaunchConfiguration config = configs[i];

        if (testSimilar(resource, config)) {
          candidateConfigs.add(config);
        }
      }
    } catch (CoreException e) {
      DartUtil.logError(e);
    }

    int candidateCount = candidateConfigs.size();

    if (candidateCount == 1) {
      return candidateConfigs.get(0);
    } else if (candidateCount > 1) {
      return chooseConfiguration(candidateConfigs);
    }

    return null;
  }

  /**
   * @return the launch configuration type for this launch shortcut
   */
  protected abstract ILaunchConfigurationType getConfigurationType();

  /**
   * Answer the resource associated with the Dart application to be launched relative to the
   * specified resource.
   * 
   * @param originalResource the original resource or <code>null</code>
   * @return the Dart resource to be launched or <code>null</code>
   */
  protected IResource getLaunchableResource(Object originalResource) throws DartModelException {

    if (originalResource == null) {
      return null;
    }
    DartElement elem = null;
    if (originalResource instanceof IResource) {
      IResource resource = (IResource) originalResource;
      if (!resource.isAccessible()) {
        return null;
      }

      if (DartUtil.isWebPage(resource)) {
        return resource;
      }

      // DartLibrary
      elem = DartCore.create(resource);
    }

    if (originalResource instanceof DartElement) {
      elem = (DartElement) originalResource;
    }

    if (elem == null) {
      return null;
    }

    if (elem instanceof DartProjectImpl) {
      DartLibrary[] libraries = ((DartProjectImpl) elem).getDartLibraries();
      if (libraries.length > 0) {
        Set<IResource> htmlFiles = new HashSet<IResource>();
        for (DartLibrary library : libraries) {
          IResource htmlFile = getHtmlFileFor(library);
          if (htmlFile != null) {
            htmlFiles.add(htmlFile);
          }
        }
        IResource[] files = htmlFiles.toArray(new IResource[htmlFiles.size()]);
        // TODO(keertip): need to handle the case of mutliple html files 
        return files[0];
      }
    }
    DartLibrary parentLibrary = elem.getAncestor(DartLibrary.class);
    return getHtmlFileFor(parentLibrary);
  }

  /**
   * Find or create and launch the given resource.
   * 
   * @param resource
   * @param mode
   */
  protected abstract void launch(IResource resource, String mode);

  /**
   * Return whether the launch configuration is used to launch the given resource.
   * 
   * @param resource
   * @param config
   * @return whether the launch configuration is used to launch the given resource
   */
  protected abstract boolean testSimilar(IResource resource, ILaunchConfiguration config);

  /**
   * @return the html file used to launch the given library
   */
  private IResource getHtmlFileFor(DartLibrary library) throws DartModelException {
    // TODO(devoncarew): we currently return the first html file in the containing folder, or
    // parent folder. We need to make this a bit more rigorous.

    IResource libraryResource = library.getCorrespondingResource();

    return getHtmlFileFor(libraryResource.getParent());
  }

  /**
   * Returns the first html file in this container or parent container. The search terminates after
   * a project container.
   * 
   * @return the first html file in this container or parent container
   */
  private IResource getHtmlFileFor(IContainer container) throws DartModelException {
    try {
      for (IResource resource : container.members()) {
        if (DartUtil.isWebPage(resource)) {
          return resource;
        }
      }
    } catch (CoreException ce) {
      DartUtil.logError(ce);
    }

    if (container instanceof IProject) {
      return null;
    } else {
      return getHtmlFileFor(container.getParent());
    }
  }

}
