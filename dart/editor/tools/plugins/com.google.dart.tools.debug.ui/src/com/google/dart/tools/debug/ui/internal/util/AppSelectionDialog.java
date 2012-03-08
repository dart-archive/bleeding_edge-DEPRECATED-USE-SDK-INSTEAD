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
import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.internal.model.DartModelManager;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredResourcesSelectionDialog;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A dialog to choose a main launch config target (a, .dart or .html file).
 */
public class AppSelectionDialog extends FilteredResourcesSelectionDialog {
  private boolean includeHtmlFiles;
  private boolean includeDartFiles;

  private Set<IResource> libraryResources;

  /**
   * Create a new AppSelectionDialog.
   * 
   * @param shell
   * @param container
   */
  public AppSelectionDialog(Shell shell, IContainer container) {
    this(shell, container, true, false);
  }

  public AppSelectionDialog(Shell shell, IContainer container, boolean includeDartFiles,
      boolean includeHtmlFiles) {
    super(shell, false, container, IResource.FILE);

    this.includeDartFiles = includeDartFiles;
    this.includeHtmlFiles = includeHtmlFiles;
    if (includeDartFiles) {
      initializeLibraries();
    }
  }

  @Override
  protected ItemsFilter createFilter() {
    final ItemsFilter delegateFilter = super.createFilter();

    return new ResourceFilter() {
      @Override
      public boolean isConsistentItem(Object item) {
        return delegateFilter.isConsistentItem(item);
      }

      @Override
      public boolean matchItem(Object item) {
        if (!(item instanceof IResource)) {
          return false;
        }

        IResource resource = (IResource) item;

        if (!delegateFilter.matchItem(resource)) {
          return false;
        }

        if (includeDartFiles && DartCore.isDartLikeFileName(resource.getName())) {
          if (libraryResources.contains(resource)) {
            return true;
          }
          return false;

        } else if (includeHtmlFiles && (DartCore.isHTMLLikeFileName(resource.getName()))) {
          return true;
        } else {
          return false;
        }
      }
    };
  }

  private void initializeLibraries() {
    libraryResources = new HashSet<IResource>();
    try {
      List<DartLibrary> libraries = DartModelManager.getInstance().getDartModel().getUnreferencedLibraries();
      List<DartLibrary> bundledLibraries = Arrays.asList(DartModelManager.getInstance().getDartModel().getBundledLibraries());
      libraries.removeAll(bundledLibraries);
      for (DartLibrary library : libraries) {
        if (library instanceof DartLibraryImpl && ((DartLibraryImpl) library).isServerApplication()) {
          libraryResources.add(library.getCorrespondingResource());
        }
      }
    } catch (DartModelException e) {
      DartDebugCorePlugin.logError(e);
    }

  }

}
