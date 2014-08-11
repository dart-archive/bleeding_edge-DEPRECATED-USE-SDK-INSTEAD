/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.ui.internal.viewsupport;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

/**
 * Filter for the methods viewer. Changing a filter property does not trigger a refiltering of the
 * viewer
 */
public class MemberFilter extends ViewerFilter {

  public static final int FILTER_NONPUBLIC = 1;
  public static final int FILTER_STATIC = 2;
  public static final int FILTER_FIELDS = 4;
  public static final int FILTER_LOCALTYPES = 8;

  private int fFilterProperties;

  /**
   * Modifies filter and add a property to filter for
   */
  public final void addFilter(int filter) {
    fFilterProperties |= filter;
  }

  /**
   * Tests if a property is filtered
   */
  public final boolean hasFilter(int filter) {
    return (fFilterProperties & filter) != 0;
  }

  /*
   * @see ViewerFilter#isFilterProperty(java.lang.Object, java.lang.String)
   */
  public boolean isFilterProperty(Object element, Object property) {
    return false;
  }

  /**
   * Modifies filter and remove a property to filter for
   */
  public final void removeFilter(int filter) {
    fFilterProperties &= (-1 ^ filter);
  }

  /*
   * @see ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
   */
  @Override
  public boolean select(Viewer viewer, Object parentElement, Object element) {
    return true;
  }

//  private boolean isLocalType(Type type) {
//    DartElement parent = type.getParent();
//    return parent instanceof TypeMember && !(parent instanceof Type);
//  }
//
//  private boolean isTopLevelType(TypeMember member) {
//    // Type parent= member.getDeclaringType();
//    // return parent == null;
//    return true;
//  }
}
