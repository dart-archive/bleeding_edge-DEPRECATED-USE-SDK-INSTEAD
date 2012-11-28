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
package com.google.dart.tools.ui.test.model.internal.views;

import com.google.dart.tools.ui.test.model.internal.views.ViewFinder.IViewMatcher;
import com.google.dart.tools.ui.test.model.internal.workbench.WorkbenchFinder;

import org.eclipse.ui.views.IViewCategory;
import org.eclipse.ui.views.IViewDescriptor;
import org.eclipse.ui.views.IViewRegistry;

/**
 * A helper for navigating and exploring views.
 */
public class ViewExplorer {

  /**
   * Find a descriptor for the view with the given name.
   * 
   * @param name the view name
   * @return the descriptor or {@code null} if no match exists
   */
  public static IViewDescriptor findView(String name) {

    if (name == null) {
      return null;
    }

    for (IViewDescriptor view : getViews()) {
      String label = view.getLabel();
      if (label == null) {
        continue;
      }
      if (name.equals(view.getLabel())) {
        return view;
      }
    }

    return null;
  }

  /**
   * For testing.
   */
  public static void spelunk() {
    IViewDescriptor[] views = getViews();
    for (int i = 0; i < views.length; i++) {
      IViewDescriptor view = views[i];
      //System.out.println(Arrays.toString(view.getCategoryPath()));
      System.out.println(view);
      System.out.println(findCategoryPath(view));
      System.out.println("----");
    }

  }

  static String findCategory(String viewName) {
    return findCategoryPath(findView(viewName));
  }

  static String findCategoryPath(IViewDescriptor view) {

    if (view == null) {
      return null;
    }

    IViewCategory[] categories = getViewRegistry().getCategories();
    for (int i = 0; i < categories.length; i++) {
      IViewCategory category = categories[i];
      IViewDescriptor[] views = category.getViews();
      for (int j = 0; j < views.length; j++) {
        IViewDescriptor candidateView = views[i];
        if (view == candidateView) {
          return category.getLabel();
        }
      }
    }

    return null;
  }

  static IViewDescriptor findMatchInRegistry(IViewMatcher matcher) {

    for (IViewDescriptor view : getViews()) {
      if (matcher.matches(view)) {
        return view;
      }
    }

    return null;
  }

  private static IViewRegistry getViewRegistry() {
    return WorkbenchFinder.getWorkbench().getViewRegistry();
  }

  private static IViewDescriptor[] getViews() {
    return getViewRegistry().getViews();
  }

}
