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

package com.google.dart.ui.test.util;

import com.google.dart.ui.test.matchers.WidgetMatcher;
import com.google.dart.ui.test.matchers.WidgetMatchers;

import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

/**
 * Helper for finding {@link Widget}s.
 */
public class WidgetFinder {
  /**
   * Visitor for the {@link Widget}'s hierarchy.
   */
  public static class IWidgetVisitor {
    /**
     * Invoked after visiting all children of this {@link Control}.
     */
    public void endVisit(Widget widget) {
    }

    /**
     * @return <code>true</code> if the children of this {@link Widget} should be visited, and
     *         <code>false</code> if the children of this {@link Widget} should be skipped.
     */
    public boolean visit(Widget widget) {
      return true;
    }
  }

  /**
   * @return the matching {@link Widget}, may be {@code null}.
   */
  @SuppressWarnings("unchecked")
  public static <T extends Widget> T findWidget(Widget start, final WidgetMatcher matcher) {
    final Widget result[] = {null};
    visit(start, new IWidgetVisitor() {
      @Override
      public boolean visit(Widget widget) {
        if (result[0] != null) {
          return false;
        }
        if (matcher.matches(widget)) {
          result[0] = widget;
        }
        return result[0] == null;
      }
    });
    return (T) result[0];
  }

  /**
   * @return the {@link Widget} that has {@link Label} with given text.
   */
  @SuppressWarnings("unchecked")
  public static <T extends Widget> T findWidgetByLabel(Widget start, String labelText) {
    final Widget[] result = {null};
    final WidgetMatcher labelMatcher = WidgetMatchers.withText(labelText);
    visit(start, new IWidgetVisitor() {
      private boolean labelFound;

      @Override
      public boolean visit(Widget widget) {
        if (result[0] != null) {
          return false;
        }
        // may be Label with required text
        if (widget instanceof Label && labelMatcher.matches(widget)) {
          labelFound = true;
          return true;
        }
        // may be Widget after Label
        if (labelFound) {
          result[0] = widget;
          return false;
        }
        // continue
        return true;
      }
    });
    return (T) result[0];
  }

  /**
   * Visits all {@link Widgets}'s starting from given one.
   */
  private static void visit(Widget widget, IWidgetVisitor visitor) {
    // ignore invisible Control's
    if (widget instanceof Control) {
      Control control = (Control) widget;
      if (control.getParent() != null && control.getParent().getLayout() instanceof StackLayout
          && !control.isVisible()) {
        return;
      }
    }
    // visit
    if (visitor.visit(widget)) {
      // Composite
      if (widget instanceof Composite) {
        Composite composite = (Composite) widget;
        Control[] children = composite.getChildren();
        for (int i = 0; i < children.length; i++) {
          Control child = children[i];
          visit(child, visitor);
        }
      }
      // ToolBar
      if (widget instanceof ToolBar) {
        ToolBar toolBar = (ToolBar) widget;
        for (ToolItem toolItem : toolBar.getItems()) {
          visit(toolItem, visitor);
        }
      }
      // TabFolder
      if (widget instanceof TabFolder) {
        TabFolder tabFolder = (TabFolder) widget;
        for (TabItem tabItem : tabFolder.getItems()) {
          visit(tabItem, visitor);
        }
      }
      // TabItem
      if (widget instanceof TabItem) {
        TabItem tabItem = (TabItem) widget;
        Control control = tabItem.getControl();
        if (control != null) {
          visit(control, visitor);
        }
      }
      // Tree
      if (widget instanceof Tree) {
        Tree tree = (Tree) widget;
        for (TreeItem treeItem : tree.getItems()) {
          visit(treeItem, visitor);
        }
      }
      if (widget instanceof TreeItem) {
        TreeItem parent = (TreeItem) widget;
        for (TreeItem treeItem : parent.getItems()) {
          visit(treeItem, visitor);
        }
      }
      // Menu
      if (widget instanceof Menu) {
        Menu menu = (Menu) widget;
        for (MenuItem menuItem : menu.getItems()) {
          visit(menuItem, visitor);
        }
      }
      if (widget instanceof MenuItem) {
        MenuItem menuItem = (MenuItem) widget;
        visit(menuItem.getMenu(), visitor);
      }
      // end
      visitor.endVisit(widget);
    }
  }
}
