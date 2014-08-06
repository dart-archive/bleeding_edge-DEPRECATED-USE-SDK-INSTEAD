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
package com.google.dart.tools.ui;

import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.ui.internal.preferences.MembersOrderPreferenceCache;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.ui.model.IWorkbenchAdapter;

import java.io.File;
import java.util.Comparator;

/**
 * Viewer comparator for Dart elements.
 * <p>
 * TODO Add functions, factories.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
public class DartElementComparator extends ViewerComparator {

  private static final int DIRECTORY = 1;
  private static final int FILE = 2;
  private static final int PROJECTS = 3;
  private static final int RESOURCEFOLDERS = 7;
  private static final int DART_LIB = 9;
  private static final int DART_FILE = 10;
  private static final int HTML_FILE = 11;
  private static final int RESOURCES = 12;
  private static final int IMPORTED_LIBS_CONTAINER = 13;
  private static final int IMPORTED_LIB_CONTAINER = 14;
  private static final int IMPORT_CONTAINER = 15;
  // Includes all categories ordered using the OutlineSortOrderPage:
  // types, methods & fields
  private static final int MEMBERSOFFSET = 15;
  private static final int DARTELEMENTS = 50;
  private static final int OTHERS = 51;

  private static Comparator<String> DART_NAME_COMPARATOR = new Comparator<String>() {

    @Override
    public int compare(String arg0, String arg1) {
      if (arg0.length() > 0 && arg1.length() > 0) {
        boolean u0 = arg0.charAt(0) == '_';
        boolean u1 = arg1.charAt(0) == '_';
        if (u0 != u1) {
          if (u1) {
            return -1;
          } else {
            return 1;
          }
        }
      }
      return String.CASE_INSENSITIVE_ORDER.compare(arg0, arg1);
    }

  };

  private MembersOrderPreferenceCache fMemberOrderCache;

  /**
   * Constructor.
   */
  public DartElementComparator() {
    super(DART_NAME_COMPARATOR); // Using String.CASE_INSENSITIVE_ORDER directly sorts _ to top
    DartX.todo("functions, factories");
    fMemberOrderCache = DartToolsPlugin.getDefault().getMemberOrderPreferenceCache();
  }

  @Override
  public int category(Object objectElement) {
    if (objectElement instanceof File) {
      File file = (File) objectElement;
      if (file.isDirectory()) {
        return DIRECTORY;
      } else {
        return FILE;
      }
    } else if (objectElement instanceof IFile) {
      return RESOURCES;
    } else if (objectElement instanceof IProject) {
      return PROJECTS;
    } else if (objectElement instanceof IContainer) {
      return RESOURCEFOLDERS;
    }
    DartX.todo();
//    else if (element instanceof ProjectLibraryRoot) {
//      return PROJECTS;
//    }

    return OTHERS;
  }

  @Override
  @SuppressWarnings("unchecked")
  public int compare(Viewer viewer, Object e1, Object e2) {
    int cat1 = category(e1);
    int cat2 = category(e2);

    if (cat1 != cat2) {
      return cat1 - cat2;
    }

    if (cat1 == PROJECTS || cat1 == DART_LIB || cat1 == RESOURCES || cat1 == RESOURCEFOLDERS
        || cat1 == OTHERS) {
      String name1 = getNonJavaElementLabel(viewer, e1);
      String name2 = getNonJavaElementLabel(viewer, e2);
      if (name1 != null && name2 != null) {
        return getComparator().compare(name1, name2);
      }
      return 0; // can't compare
    }
    // only Dart elements from this point

    String name1 = getElementName(e1);
    String name2 = getElementName(e2);

    if (e1 instanceof Type) { // handle anonymous types
      if (name1.length() == 0) {
        if (name2.length() == 0) {
          try {
            return getComparator().compare(
                ((Type) e1).getSuperclassName(),
                ((Type) e2).getSuperclassName());
          } catch (DartModelException e) {
            return 0;
          }
        } else {
          return 1;
        }
      } else if (name2.length() == 0) {
        return -1;
      }
    }

    int cmp = getComparator().compare(name1, name2);
    if (cmp != 0) {
      return cmp;
    }

    return 0;
  }

  private String getElementName(Object element) {
    return element.toString();
  }

  private int getMemberCategory(int kind) {
    int offset = fMemberOrderCache.getCategoryIndex(kind);
    return offset + MEMBERSOFFSET;
  }

  private String getNonJavaElementLabel(Viewer viewer, Object element) {
    // try to use the workbench adapter for non - JavaScript resources or if not
    // available, use the viewers label provider
    if (element instanceof IResource) {
      return ((IResource) element).getName();
    }
    if (element instanceof IStorage) {
      return ((IStorage) element).getName();
    }
    if (element instanceof IAdaptable) {
      IWorkbenchAdapter adapter = (IWorkbenchAdapter) ((IAdaptable) element).getAdapter(IWorkbenchAdapter.class);
      if (adapter != null) {
        return adapter.getLabel(element);
      }
    }
    if (viewer instanceof ContentViewer) {
      IBaseLabelProvider prov = ((ContentViewer) viewer).getLabelProvider();
      if (prov instanceof ILabelProvider) {
        return ((ILabelProvider) prov).getText(element);
      }
    }
    return null;
  }
}
