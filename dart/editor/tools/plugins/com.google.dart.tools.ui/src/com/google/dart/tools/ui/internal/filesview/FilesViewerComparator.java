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

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ViewerComparator;

import java.util.Comparator;

/**
 * Sorts files alphabetically.
 */
public class FilesViewerComparator extends ViewerComparator {
  private static final int SPECIAL_RESOURCE_SORT = 0;
  private static final int DIRECTORY_SORT = 1;
  private static final int RESOURCE_SORT = 2;
  private static final int FILESTORE_SORT = 3;
  private static final int DEFAULT_SORT = 4;

  public FilesViewerComparator() {
    super(new Comparator<String>() {
      @Override
      public int compare(String arg0, String arg1) {
        // If you do a standard case insensitive sort, strings starting
        // with underscores sort to the top, which is not desired.
        return arg0.toUpperCase().compareTo(arg1.toUpperCase());
      }
    });
  }

  @Override
  public int category(Object element) {
    if (element instanceof IContainer) {
      IContainer container = (IContainer) element;

      if (container.getName().equals(DartCore.PACKAGES_DIRECTORY_NAME)) {
        return SPECIAL_RESOURCE_SORT;
      } else {
        return DIRECTORY_SORT;
      }
    } else if (element instanceof IResource) {
      IResource resource = (IResource) element;

      if (resource.getParent() instanceof IProject) {
        // At the top level of a project, check for pubspec.yaml, pubspec.lock, and build.dart.
        if (resource.getType() == IResource.FILE) {
          String name = resource.getName();

          if (name.equals(DartCore.PUBSPEC_FILE_NAME)
              || name.equals(DartCore.PUBSPEC_LOCK_FILE_NAME)) {
            return SPECIAL_RESOURCE_SORT;
          }
        }
      }

      return RESOURCE_SORT;
    } else if (element instanceof IFileStore) {
      IFileStore fileStore = (IFileStore) element;
      if (fileStore.fetchInfo().isDirectory()) {
        return DIRECTORY_SORT;
      } else {
        return FILESTORE_SORT;
      }
    } else {
      return DEFAULT_SORT;
    }
  }

}
