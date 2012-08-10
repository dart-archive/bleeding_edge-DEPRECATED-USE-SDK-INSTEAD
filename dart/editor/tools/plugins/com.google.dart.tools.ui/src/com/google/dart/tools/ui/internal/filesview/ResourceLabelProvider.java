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
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * Label provider for resources in the {@link FilesView}.
 */
public class ResourceLabelProvider implements IStyledLabelProvider, ILabelProvider {
  private static final String IGNORE_FILE_ICON = "icons/full/dart16/dart_excl.png";
  private static final String IGNORE_FOLDER_ICON = "icons/full/dart16/flder_obj_excl.png";

  private static final String PACKAGES_FOLDER_ICON = "icons/full/dart16/fldr_obj_pkg.png";

  private static final String LIBRARY_ICON = "icons/full/dart16/dart_library.png";

  private final WorkbenchLabelProvider workbenchLabelProvider = new WorkbenchLabelProvider();

  @Override
  public void addListener(ILabelProviderListener listener) {
    workbenchLabelProvider.addListener(listener);
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

      DartElement dartElement = DartCore.create(resource);

      // Return a different icon for library units.
      if (dartElement instanceof CompilationUnit) {
        if (((CompilationUnit) dartElement).definesLibrary()) {
          return DartToolsPlugin.getImage(LIBRARY_ICON);
        }
      }

      if (element instanceof IFolder) {
        IFolder folder = (IFolder) element;

        String name = folder.getProjectRelativePath().toPortableString();

        if (name.equals("packages")) {
          return DartToolsPlugin.getImage(PACKAGES_FOLDER_ICON);
        }
      }
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

      DartElement dartElement = DartCore.create(resource);

      // Append the library name to library units.
      if (dartElement instanceof CompilationUnit) {
        if (((CompilationUnit) dartElement).definesLibrary()) {
          DartLibrary library = ((CompilationUnit) dartElement).getLibrary();

          string.append(" [" + library.getDisplayName() + "]", StyledString.QUALIFIER_STYLER);
        }
      }

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
    workbenchLabelProvider.removeListener(listener);
  }

}
