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
package com.google.dart.tools.ui.internal.view.files;

import org.eclipse.jface.viewers.ViewerComparator;

import java.io.File;
import java.util.Comparator;

/**
 * Sorts directories and files alphabetically.
 */
public class FilesViewerComparator extends ViewerComparator {
  private static final int DIRECTORY = 0;
  private static final int FILE = 1;
  private static final int DEFAULT_SORT = 2;

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
    if (element instanceof File) {
      File f = (File) element;
      if (f.isDirectory()) {
        return DIRECTORY;
      } else {
        return FILE;
      }
    } else {
      return DEFAULT_SORT;
    }
  }
}
