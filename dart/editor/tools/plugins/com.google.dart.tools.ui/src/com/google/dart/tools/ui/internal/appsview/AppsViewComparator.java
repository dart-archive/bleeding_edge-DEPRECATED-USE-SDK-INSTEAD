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
package com.google.dart.tools.ui.internal.appsview;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartProject;

import org.eclipse.jface.viewers.ViewerComparator;

import java.util.Comparator;

public class AppsViewComparator extends ViewerComparator {

  private static final int PROJECT_SORT = 3;
  private static final int LIBRARY_SORT = 2;
  private static final int COMPUNIT_SORT = 1;
  private static final int DEFAULT_SORT = 0;

  public AppsViewComparator() {
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
    if (element instanceof DartProject) {
      return PROJECT_SORT;
    } else if (element instanceof DartLibrary) {
      return LIBRARY_SORT;
    } else if (element instanceof CompilationUnit) {
      return COMPUNIT_SORT;
    } else {
      return DEFAULT_SORT;
    }
  }

}
