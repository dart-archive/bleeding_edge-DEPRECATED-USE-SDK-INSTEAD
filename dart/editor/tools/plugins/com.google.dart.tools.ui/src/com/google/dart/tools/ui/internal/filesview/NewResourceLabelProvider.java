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
package com.google.dart.tools.ui.internal.filesview;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.builder.ResourceDeltaEvent;
import com.google.dart.tools.core.internal.builder.SourceContainerDeltaEvent;
import com.google.dart.tools.core.internal.builder.SourceDeltaEvent;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Label provider for resources in the {@link FilesView}.
 */
public class NewResourceLabelProvider extends ResourceLabelProvider {
  private static final String IGNORE_FILE_ICON = "icons/full/dart16/dart_excl.png";
  private static final String IGNORE_FOLDER_ICON = "icons/full/dart16/flder_obj_excl.png";

  private static final String PACKAGES_FOLDER_ICON = "icons/full/dart16/fldr_obj_pkg.png";

  @SuppressWarnings("unused")
  private static final String LIBRARY_ICON = "icons/full/dart16/dart_library.png";

  private static final String BUILD_FILE_ICON = "icons/full/dart16/build_dart.png";

  private static final String PACKAGE_ICON = "icons/full/obj16/package_obj.gif";

  private final WorkbenchLabelProvider workbenchLabelProvider = new WorkbenchLabelProvider();

  private List<ILabelProviderListener> listeners = new ArrayList<ILabelProviderListener>();

  @Override
  public void addListener(ILabelProviderListener listener) {
    listeners.add(listener);

    if (listeners.size() == 1) {

      //TODO (pquitslund): add a listener to the delta processor
      //DartCore.getProjectManager().addDeltaListener(this);

    }
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

      if (resource instanceof IFile && resource.getParent() instanceof IProject
          && resource.getName().equals(DartCore.BUILD_DART_FILE_NAME)) {
        return DartToolsPlugin.getImage(BUILD_FILE_ICON);
      }

      try {

//TODO (pquitslund): add support for library icons

//        DartElement dartElement = DartCore.create(resource);
//
//        // Return a different icon for library units.
//        if (dartElement instanceof CompilationUnit) {
//          if (((CompilationUnit) dartElement).definesLibrary()) {
//            return DartToolsPlugin.getImage(LIBRARY_ICON);
//          }
//        }

      } catch (Throwable th) {
        DartToolsPlugin.log(th);
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
            string.append(" [package:]", StyledString.QUALIFIER_STYLER);
          }
          String version = resource.getPersistentProperty(DartCore.PUB_PACKAGE_VERSION);
          if (version != null) {
            string.append(" [" + version + "]", StyledString.QUALIFIER_STYLER);
            return string;
          }
        }

        //TODO (pquitslund): append library name details

//        DartElement dartElement = DartCore.create(resource);
//
//        // Append the library name to library units.
//        if (dartElement instanceof CompilationUnit) {
//          if (((CompilationUnit) dartElement).definesLibrary()) {
//            DartLibrary library = ((CompilationUnit) dartElement).getLibrary();
//
//            string.append(" [" + library.getDisplayName() + "]", StyledString.QUALIFIER_STYLER);
//          }
//        }

      } catch (Throwable th) {
        DartToolsPlugin.log(th);
      }

      return string;
    }

    //TODO (pquitslund): append library name details

//    if (element instanceof DartLibraryNode && ((DartLibraryNode) element).getCategory() != null) {
//      StyledString string = new StyledString(((DartLibraryNode) element).getLabel());
//      string.append(
//          " [" + ((DartLibraryNode) element).getCategory() + "]",
//          StyledString.QUALIFIER_STYLER);
//
//      return string;
//    }

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
  public void packageSourceAdded(SourceDeltaEvent event) {
    notifyListeners();
  }

  @Override
  public void packageSourceChanged(SourceDeltaEvent event) {
    notifyListeners();
  }

  @Override
  public void packageSourceContainerRemoved(SourceContainerDeltaEvent event) {
    notifyListeners();
  }

  @Override
  public void packageSourceRemoved(SourceDeltaEvent event) {
    notifyListeners();
  }

  @Override
  public void pubspecAdded(ResourceDeltaEvent event) {
    notifyListeners();
  }

  @Override
  public void pubspecChanged(ResourceDeltaEvent event) {
    notifyListeners();
  }

  @Override
  public void pubspecRemoved(ResourceDeltaEvent event) {
    notifyListeners();
  }

  @Override
  public void removeListener(ILabelProviderListener listener) {
    listeners.remove(listener);

    if (listeners.isEmpty()) {
      DartCore.removeElementChangedListener(this);
    }

  }

  @Override
  public void sourceAdded(SourceDeltaEvent event) {
    notifyListeners();
  }

  @Override
  public void sourceChanged(SourceDeltaEvent event) {
    notifyListeners();
  }

  @Override
  public void sourceContainerRemoved(SourceContainerDeltaEvent event) {
    notifyListeners();
  }

  @Override
  public void sourceRemoved(SourceDeltaEvent event) {
    notifyListeners();
  }

  private void notifyListeners() {
    try {
      for (ILabelProviderListener listener : listeners) {
        listener.labelProviderChanged(new LabelProviderChangedEvent(this));
      }
    } catch (Throwable t) {
      DartToolsPlugin.log(t);
    }
  }

}
