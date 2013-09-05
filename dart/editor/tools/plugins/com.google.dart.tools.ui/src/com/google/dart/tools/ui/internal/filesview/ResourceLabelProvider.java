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

import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.source.SourceKind;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.analysis.model.AnalysisEvent;
import com.google.dart.tools.core.analysis.model.AnalysisListener;
import com.google.dart.tools.core.analysis.model.ProjectManager;
import com.google.dart.tools.core.analysis.model.ResolvedEvent;
import com.google.dart.tools.core.internal.builder.AnalysisWorker;
import com.google.dart.tools.core.utilities.io.FilenameUtils;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Label provider for resources in the {@link FilesView}.
 */
public class ResourceLabelProvider implements IStyledLabelProvider, ILabelProvider,
    AnalysisListener {

  private static final String IGNORE_FILE_ICON = "icons/full/dart16/dart_excl.png"; //$NON-NLS-1$

  private static final String IGNORE_FOLDER_ICON = "icons/full/dart16/flder_obj_excl.png"; //$NON-NLS-1$

  private static final String PACKAGES_FOLDER_ICON = "icons/full/dart16/fldr_obj_pkg.png"; //$NON-NLS-1$
  private static final String LIBRARY_ICON = "icons/full/dart16/dart_library.png"; //$NON-NLS-1$
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

  private boolean disposed;

  @Override
  public void addListener(ILabelProviderListener listener) {
    listeners.add(listener);

    if (listeners.size() == 1) {
      AnalysisWorker.addListener(this);
    }
  }

  @Override
  public void complete(AnalysisEvent event) {
    notifyListeners();
  }

  @Override
  public void dispose() {
    disposed = true;

    workbenchLabelProvider.dispose();

    if (listeners.size() > 0) {
      AnalysisWorker.removeListener(this);
    }
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

        if (DartCoreDebug.EXPERIMENTAL) {
          SourceKind kind = DartCore.getProjectManager().getSourceKind(file);
          if (kind == SourceKind.LIBRARY) {
            return DartToolsPlugin.getImage(LIBRARY_ICON);
          }
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
    return workbenchLabelProvider.getImage(element);
  }

  @Override
  public StyledString getStyledText(Object element) {
    if (element instanceof IResource) {
      IResource resource = (IResource) element;

      // Un-analyzed resources are grey.
      if (!DartCore.isAnalyzed(resource)) {
        return new StyledString(resource.getName(), StyledString.QUALIFIER_STYLER);
      }

      StyledString string = new StyledString(resource.getName());

      try {
        if (resource instanceof IFolder) {
          if (DartCore.isPackagesDirectory((IFolder) resource)) {
            string.append(" [package:]", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
          }

          try {
            String packageVersion = resource.getPersistentProperty(DartCore.PUB_PACKAGE_VERSION);

            if (packageVersion != null) {
              string.append(" [" + packageVersion + "]", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$ //$NON-NLS-2$
              return string;
            }
          } catch (CoreException ce) {
            // ignore
          }
        }

        if (resource instanceof IFile) {
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
          if (DartCoreDebug.EXPERIMENTAL) {
            ProjectManager projectManager = DartCore.getProjectManager();
            SourceKind kind = projectManager.getSourceKind((IFile) resource);

            if (kind == SourceKind.LIBRARY) {
              LibraryElement libraryElement = projectManager.getLibraryElementOrNull((IFile) resource);

              if (libraryElement != null) {
                String name = libraryElement.getName();

                if (name == null || name.length() == 0) {

                  if (libraryElement.getEntryPoint() != null) {
                    name = FilenameUtils.removeExtension(resource.getName());
                  }
                }

                string.append(" [" + name + "]", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$ //$NON-NLS-2$
              }
            }
          }
        }
      } catch (Throwable th) {
        DartToolsPlugin.log(th);
      }

      return string;
    }

    if (element instanceof DartLibraryNode && ((DartLibraryNode) element).getCategory() != null) {
      StyledString string = new StyledString(((DartLibraryNode) element).getLabel());
      string.append(" [" + ((DartLibraryNode) element).getCategory() + "]", //$NON-NLS-1$ //$NON-NLS-2$
          StyledString.QUALIFIER_STYLER);

      return string;
    }

    if (element instanceof DartPackageNode) {
      StyledString string = new StyledString(((DartPackageNode) element).getLabel());
      string.append(" [" + ((DartPackageNode) element).getVersion() + "]", //$NON-NLS-1$ //$NON-NLS-2$
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

    if (listeners.isEmpty()) {
      AnalysisWorker.removeListener(this);
    }
  }

  @Override
  public void resolved(ResolvedEvent event) {
    // ignored
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

  private void notifyListeners() {
    if (disposed) {
      return;
    }

    try {
      for (final ILabelProviderListener listener : listeners) {
        uiExec(new Runnable() {
          @Override
          public void run() {
            if (!disposed) {
              listener.labelProviderChanged(new LabelProviderChangedEvent(
                  ResourceLabelProvider.this));
            }
          }
        });
      }
    } catch (Throwable t) {
      DartToolsPlugin.log(t);
    }
  }

  private void uiExec(Runnable runnable) {
    try {
      Display.getDefault().asyncExec(runnable);
    } catch (SWTException e) {
      // Ignore -- might occur if async events get dispatched after the WS is closed
    }
  }
}
