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
package com.google.dart.tools.ui.internal.filesview;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.filesview.nodes.old.pkgs.DartPackageNode_OLD;
import com.google.dart.tools.ui.internal.filesview.nodes.old.sdk.DartLibraryNode_OLD;
import com.google.dart.tools.ui.internal.filesview.nodes.server.IDartNode_NEW;
import com.google.dart.tools.ui.internal.filesview.nodes.server.pkgs.DartPackageNode_NEW;
import com.google.dart.tools.ui.internal.filesview.nodes.server.pkgs.InstalledPackagesNode_NEW;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Label provider for resources in the {@link FilesView}.
 */
public class ResourceLabelProvider implements IStyledLabelProvider, ILabelProvider {

  private static final String IGNORE_FILE_ICON = "icons/full/dart16/dart_excl.png"; //$NON-NLS-1$

  private static final String IGNORE_FOLDER_ICON = "icons/full/dart16/flder_obj_excl.png"; //$NON-NLS-1$

  private static final String PACKAGES_FOLDER_ICON = "icons/full/dart16/fldr_obj_pkg.png"; //$NON-NLS-1$
  private static final String BUILD_FILE_ICON = "icons/full/dart16/build_dart.png"; //$NON-NLS-1$
  private static final String PACKAGE_ICON = "icons/full/obj16/package_obj.gif"; //$NON-NLS-1$

  /**
   * Get a resource label provider instance.
   */
  public static ResourceLabelProvider createInstance() {
    return new ResourceLabelProvider();
  }

  private final WorkbenchLabelProvider workbenchLabelProvider = new WorkbenchLabelProvider();

  private List<ILabelProviderListener> listeners = new ArrayList<ILabelProviderListener>();

  @Override
  public void addListener(ILabelProviderListener listener) {
    listeners.add(listener);
  }

  @Override
  public void dispose() {
    workbenchLabelProvider.dispose();
  }

  @Override
  public Image getImage(Object element) {
    if (element instanceof IResource) {

      IResource resource = (IResource) element;

      if (!DartCore.isAnalyzed(resource)) {
        if (resource instanceof IFile) {
          return DartToolsPlugin.getImage(IGNORE_FILE_ICON);
        }

        if (resource instanceof IFolder) {
          return DartToolsPlugin.getImage(IGNORE_FOLDER_ICON);
        }
      }

      if (resource instanceof IFile) {
        IFile file = (IFile) resource;

        if (DartCore.isBuildDart(file)) {
          return DartToolsPlugin.getImage(BUILD_FILE_ICON);
        }
      }

      if (element instanceof IFolder) {
        IFolder folder = (IFolder) element;

        if (DartCore.isPackagesDirectory(folder)) {
          return DartToolsPlugin.getImage(PACKAGES_FOLDER_ICON);
        }

        if (DartCore.isPackagesResource(folder)) {
          return DartToolsPlugin.getImage(PACKAGE_ICON);
        }

      }
    }

    if (element instanceof IFileStore && ((IFileStore) element).getName().equals("lib")) {
      return DartToolsPlugin.getImage(PACKAGE_ICON);
    }

    if (element instanceof IDartNode_NEW) {
      IDartNode_NEW node = (IDartNode_NEW) element;
      ImageDescriptor imageDescriptor = node.getImageDescriptor();
      return DartToolsPlugin.getImage(imageDescriptor);
    }

    return workbenchLabelProvider.getImage(element);
  }

  @Override
  public StyledString getStyledText(Object element) {
    if (element instanceof IResource) {
      IResource resource = (IResource) element;

      // Un-analyzed resources are grey.
      if (!DartCore.isAnalyzed(resource) || resource.isDerived(IResource.CHECK_ANCESTORS)) {
        if (resource instanceof IFolder && DartCore.isBuildDirectory((IFolder) resource)) {
          return new StyledString(
              resource.getName() + " [generated]",
              StyledString.QUALIFIER_STYLER);
        } else {
          return new StyledString(resource.getName(), StyledString.QUALIFIER_STYLER);
        }
      }

      StyledString string = new StyledString(resource.getName());
      try {
        if (resource instanceof IFolder) {
          String packageVersion = resource.getPersistentProperty(DartCore.PUB_PACKAGE_VERSION);

          if (packageVersion != null) {
            string.append(" [" + packageVersion + "]", StyledString.QUALIFIER_STYLER);
            return string;
          }
        } else if (resource instanceof IFile) {
          IFile file = (IFile) resource;

          // If it's a build.dart file, and auto-building is disabled, render the text in grey.
          if (DartCore.isBuildDart(file)
              && DartCore.getPlugin().getDisableDartBasedBuilder(file.getProject())) {
            return new StyledString(file.getName(), StyledString.QUALIFIER_STYLER);
          }

          // If we resource has been remapped by build.dart, display that info as a decoration.
          String remappingPath = DartCore.getResourceRemapping(file);

          if (remappingPath != null) {
            StyledString str = new StyledString(file.getName());
            str.append(
                " [" + getRelativePath(file, remappingPath) + "]",
                StyledString.QUALIFIER_STYLER);
            return str;
          }

          // Append the library name to library units.
          String libraryName = resource.getPersistentProperty(DartCore.LIBRARY_NAME);

          if (libraryName != null) {
            string.append(" [" + libraryName + "]", StyledString.QUALIFIER_STYLER);
            return string;
          }
        }
      } catch (Throwable th) {
        DartToolsPlugin.log(th);
      }

      return string;
    }

    if (element instanceof InstalledPackagesNode_NEW) {
      String label = ((InstalledPackagesNode_NEW) element).getLabel();
      return new StyledString(label);
    }

    if (element instanceof DartPackageNode_NEW) {
      DartPackageNode_NEW node = (DartPackageNode_NEW) element;
      StyledString string = new StyledString(node.getLabel());
      string.append(" [" + node.getVersion() + "]", StyledString.QUALIFIER_STYLER);
      return string;
    }

    if (element instanceof DartLibraryNode_OLD
        && ((DartLibraryNode_OLD) element).getCategory() != null) {
      StyledString string = new StyledString(((DartLibraryNode_OLD) element).getLabel());
      string.append(" [" + ((DartLibraryNode_OLD) element).getCategory() + "]", //$NON-NLS-1$ //$NON-NLS-2$
          StyledString.QUALIFIER_STYLER);

      return string;
    }

    if (element instanceof DartPackageNode_OLD) {
      StyledString string = new StyledString(((DartPackageNode_OLD) element).getLabel());
      string.append(" [" + ((DartPackageNode_OLD) element).getVersion() + "]", //$NON-NLS-1$ //$NON-NLS-2$
          StyledString.QUALIFIER_STYLER);
      return string;
    }

    return workbenchLabelProvider.getStyledText(element);
  }

  @Override
  public String getText(Object element) {
    return workbenchLabelProvider.getText(element);
  }

  @Override
  public boolean isLabelProperty(Object element, String property) {
    return workbenchLabelProvider.isLabelProperty(element, property);
  }

  @Override
  public void removeListener(ILabelProviderListener listener) {
    listeners.remove(listener);
  }

  private String getRelativePath(IFile file, String mappingPath) {
    String parentPath = file.getParent().getFullPath().toPortableString();

    if (mappingPath.startsWith(parentPath)) {
      mappingPath = mappingPath.substring(parentPath.length());
    }

    if (mappingPath.startsWith("/")) {
      mappingPath = mappingPath.substring(1);
    }

    return mappingPath;
  }
}
