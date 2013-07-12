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

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.dart.tools.internal.corext.refactoring.util.ReflectionUtils;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.junit.Assert;

import java.util.LinkedList;
import java.util.List;

/**
 * Helper for testing SWT UI.
 */
public class UiContext {
  /**
   * Visitor for all {@link Widget}'s.
   */
  public static class IWidgetsVisitor {
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
   * @return the first {@link Widget} with compatible {@link Class}.
   */
  public static <T extends Widget> T findFirstWidget(Widget start, Class<T> classToFind) {
    List<T> widgets = findWidgets(start, classToFind);
    if (!widgets.isEmpty()) {
      return widgets.get(0);
    }
    return null;
  }

  /**
   * @return the {@link List} of {@link Widget}'s with compatible {@link Class}.
   */
  public static <T extends Widget> List<T> findWidgets(Widget start, final Class<T> classToFind) {
    final List<T> widgets = Lists.newArrayList();
    visit(start, new IWidgetsVisitor() {
      @Override
      @SuppressWarnings("unchecked")
      public void endVisit(Widget widget) {
        if (classToFind.isAssignableFrom(widget.getClass())) {
          widgets.add((T) widget);
        }
      }
    });
    //
    return widgets;
  }

  /**
   * Runs the events loop for the given number of milliseconds.
   */
  public static void runEventLoop(int ms) {
    long start = System.currentTimeMillis();
    do {
      while (Display.getDefault().readAndDispatch()) {
        // do nothing
      }
      Thread.yield();
    } while (System.currentTimeMillis() - start < ms);
  }

  /**
   * Runs the events loop one time. Usually just to update UI, such as repaint.
   */
  public static void runEventLoopOnce() {
    while (Display.getDefault().readAndDispatch()) {
      // do nothing
    }
  }

  public static void setChecked(TreeItem treeItem, boolean checked) {
    treeItem.setChecked(checked);
    // notify about check
    Event event = new Event();
    event.detail = SWT.CHECK;
    event.item = treeItem;
    treeItem.getParent().notifyListeners(SWT.Selection, event);
  }

  /**
   * Sets given {@link TreeItem} (and only it) expanded in its {@link Tree}.
   */
  public static void setExpanded(TreeItem treeItem, boolean expanded) {
    treeItem.setExpanded(expanded);
    Event event = new Event();
    event.widget = treeItem.getParent();
    event.item = treeItem;
    treeItem.getParent().notifyListeners(SWT.Expand, event);
  }

  /**
   * Selects items in {@link org.eclipse.swt.widgets.List}.
   */
  public static void setSelection(org.eclipse.swt.widgets.List list, String item) {
    list.setSelection(new String[] {item});
    list.notifyListeners(SWT.Selection, null);
  }

  /**
   * Sets given {@link TreeItem} (and only it) selected in its {@link Tree}.
   */
  public static void setSelection(TreeItem treeItem) {
    Tree tree = treeItem.getParent();
    tree.setSelection(treeItem);
    treeItem.getParent().notifyListeners(SWT.Selection, null);
  }

  /**
   * Visits all {@link Widgets}'s starting from given one.
   */
  public static void visit(Widget widget, IWidgetsVisitor visitor) {
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

  /**
   * @return <code>true</code> if given {@link String}s have same content. Special characters are
   *         ignored.
   */
  private static boolean isSameText(String s1, String s2) {
    if (s1 == null || s2 == null) {
      return false;
    }
    s1 = normalizeTextForComparing(s1);
    s2 = normalizeTextForComparing(s2);
    return s1.equals(s2);
  }

  /**
   * Normalizes given {@link String} by removing special characters.
   */
  private static String normalizeTextForComparing(String s) {
    s = s.trim();
    s = StringUtils.remove(s, '&');
    s = StringUtils.substringBefore(s, "\t");
    return s;
  }

  private final Display display;

  private Shell shell;

  private final LinkedList<Shell> shells = Lists.newLinkedList();

  public UiContext() {
    this.display = Display.getCurrent();
  }

  public UiContext(Display display) {
    this.display = display;
  }

  /**
   * Sends {@link SWT#Selection} event to given {@link ToolItem} with detail.
   */
  public void click(ToolItem toolItem, int detail) {
    Event event = new Event();
    event.detail = detail;
    toolItem.notifyListeners(SWT.Selection, event);
  }

  /**
   * Sends {@link SWT#Selection} event to given {@link Widget}.
   */
  public void click(Widget widget) {
    widget.notifyListeners(SWT.Selection, new Event());
  }

  /**
   * Sends {@link SWT#Selection} event to given {@link Button}.
   */
  public void clickButton(Button button) {
    click(button);
  }

  /**
   * Sends {@link SWT#Selection} event to the {@link Button} with given text.
   */
  public void clickButton(String text) {
    Button button = getButtonByText(text);
    Assert.assertNotNull("Can not find button with text |" + text + "|", button);
    clickButton(button);
  }

  /**
   * @return the first {@link Widget} with compatible {@link Class}.
   */
  public <T extends Widget> T findFirstWidget(Class<T> classToFind) {
    return findFirstWidget(getShell(), classToFind);
  }

  /**
   * @return the {@link Shell} with the given text, may be {@code null}.
   */
  public Shell findShell(String text) {
    for (Shell shell : display.getShells()) {
      if (text.equals(shell.getText())) {
        return shell;
      }
    }
    return null;
  }

  /**
   * @return the {@link List} of {@link Widget}'s with compatible {@link Class}.
   */
  public <T extends Widget> List<T> findWidgets(final Class<T> classToFind) {
    Shell shell = getShell();
    return findWidgets(shell, classToFind);
  }

  /**
   * @return the {@link Menu} widget that active for current {@link Shell}.
   */
  public Menu getActiveMenu() {
    return (Menu) ReflectionUtils.getFieldObject(getShell(), "activeMenu");
  }

  /**
   * @return the {@link Button} with given text.
   */
  public Button getButton(Widget start, final Predicate<String> predicate) {
    final Button[] result = new Button[1];
    visit(start, new IWidgetsVisitor() {
      @Override
      public void endVisit(Widget widget) {
        if (widget instanceof Button) {
          Button button = (Button) widget;
          if (predicate.apply(button.getText()) || predicate.apply(button.getToolTipText())) {
            result[0] = button;
          }
        }
      }
    });
    return result[0];
  }

  /**
   * @return the {@link Button} with given text.
   */
  public Button getButtonByText(String text) {
    return getButtonByText(getShell(), text);
  }

  /**
   * @return the {@link Button} with given text.
   */
  public Button getButtonByText(Widget start, final String text) {
    return getButton(start, new Predicate<String>() {
      @Override
      public boolean apply(String input) {
        return isSameText(input, text);
      }
    });
  }

  /**
   * @return the {@link Button} with given text.
   */
  public Button getButtonByTextPrefix(final String prefix) {
    return getButton(getShell(), new Predicate<String>() {
      @Override
      public boolean apply(String input) {
        return input != null && input.startsWith(prefix);
      }
    });
  }

  /**
   * @return the {@link Control} located in children on its {@link Composite} after given one.
   */
  @SuppressWarnings("unchecked")
  public <T> T getControlAfter(Control reference) {
    Control[] children = reference.getParent().getChildren();
    // get next Control
    int index = ArrayUtils.indexOf(children, reference);
    if (index < children.length - 1) {
      return (T) children[index + 1];
    }
    // not found
    return null;
  }

  /**
   * @return the {@link Control} located in children on its {@link Composite} after {@link Label}
   *         with given text.
   */
  public Control getControlAfterLabel(String text) {
    List<Label> labels = findWidgets(Label.class);
    for (Label label : labels) {
      String labelText = label.getText().trim();
      if (isSameText(labelText, text)) {
        Control[] children = label.getParent().getChildren();
        // get next Control
        int index = ArrayUtils.indexOf(children, label);
        if (index < children.length - 1) {
          return children[index + 1];
        }
        // stop it any case
        break;
      }
    }
    // not found
    return null;
  }

  /**
   * @return the {@link Menu} widget that active for current {@link Display}.
   */
  public Menu getLastPopup() {
    Menu menu = null;
    Menu[] popups = (Menu[]) ReflectionUtils.getFieldObject(display, "popups");
    for (int i = 0; i < popups.length; i++) {
      if (popups[i] != null) {
        menu = popups[i];
      }
    }
    return menu;
  }

  /**
   * @return the {@link MenuItem} with given text.
   */
  public MenuItem getMenuItem(Menu menu, final String text) {
    final MenuItem[] result = new MenuItem[1];
    visit(menu, new IWidgetsVisitor() {
      @Override
      public void endVisit(Widget widget) {
        if (widget instanceof MenuItem) {
          MenuItem item = (MenuItem) widget;
          if (isSameText(item.getText(), text)) {
            result[0] = item;
          }
        }
      }
    });
    return result[0];
  }

  /**
   * @return the {@link Shell} to use as start to searching {@link Widget}'s.
   */
  public Shell getShell() {
    if (this.shell != null) {
      if (this.shell.isDisposed()) {
        this.shell = null;
      } else {
        return this.shell;
      }
    }
    return display.getShells()[0];
  }

  /**
   * @return {@link Shell} with given text, may be <code>null</code>.
   */
  public Shell getShell(String text) {
    for (Shell shell : display.getShells()) {
      if (text.equals(shell.getText())) {
        return shell;
      }
    }
    return null;
  }

  /**
   * @return the {@link TabItem} with given text.
   */
  public TabItem getTabItem(final String text) {
    final TabItem[] result = new TabItem[1];
    visit(getShell(), new IWidgetsVisitor() {
      @Override
      public void endVisit(Widget widget) {
        if (widget instanceof TabItem) {
          TabItem item = (TabItem) widget;
          if (text.equals(item.getText())) {
            result[0] = item;
          }
        }
      }
    });
    return result[0];
  }

  /**
   * @return the {@link Text} widget that has {@link Label} with given text.
   */
  public Text getTextByLabel(final String labelText) {
    final Text[] result = new Text[1];
    visit(getShell(), new IWidgetsVisitor() {
      private boolean labelFound;

      @Override
      public void endVisit(Widget widget) {
        if (widget instanceof Label) {
          Label label = (Label) widget;
          labelFound = isSameText(label.getText(), labelText);
        }
        if (widget instanceof Text && labelFound) {
          result[0] = (Text) widget;
        }
      }
    });
    return result[0];
  }

  /**
   * @return the {@link Text} widget that has given text.
   */
  public Text getTextByText(final String textText) {
    final Text[] result = new Text[1];
    visit(getShell(), new IWidgetsVisitor() {
      @Override
      public void endVisit(Widget widget) {
        if (widget instanceof Text) {
          Text text = (Text) widget;
          if (text.getText().equals(textText)) {
            result[0] = (Text) widget;
          }
        }
      }
    });
    return result[0];
  }

  /**
   * @return the {@link ToolItem} with given text.
   */
  public ToolItem getToolItem(final String text) {
    final ToolItem[] result = new ToolItem[1];
    visit(getShell(), new IWidgetsVisitor() {
      @Override
      public void endVisit(Widget widget) {
        if (widget instanceof ToolItem) {
          ToolItem item = (ToolItem) widget;
          if (text.equals(item.getText()) || text.equals(item.getToolTipText())) {
            result[0] = item;
          }
        }
      }
    });
    return result[0];
  }

  /**
   * @return the {@link TreeItem} with given text.
   */
  public TreeItem getTreeItem(final String text) {
    final TreeItem[] result = new TreeItem[1];
    visit(getShell(), new IWidgetsVisitor() {
      @Override
      public void endVisit(Widget widget) {
        if (widget instanceof TreeItem) {
          TreeItem item = (TreeItem) widget;
          if (text.equals(item.getText())) {
            result[0] = item;
          }
        }
      }
    });
    return result[0];
  }

  /**
   * Specifies that it is expected that current {@link Shell} was closed, so we return to previous.
   */
  public void popShell() {
    shell = shells.removeFirst();
  }

  /**
   * Sends {@link SWT#Selection} event to given {@link Button}.
   */
  public void selectButton(Button button) {
    // if Button in RADIO, deselect all other RADIO Button's
    if ((button.getStyle() & SWT.RADIO) != 0) {
      Control[] children = button.getParent().getChildren();
      for (int i = 0; i < children.length; i++) {
        Control child = children[i];
        if (child instanceof Button && (child.getStyle() & SWT.RADIO) != 0) {
          Button childButton = (Button) child;
          if (childButton != button) {
            selectButton(childButton, false);
          }
        }
      }
    }
    // select needed Button
    button.setFocus();
    selectButton(button, true);
  }

  /**
   * Selects {@link Button} with given text.
   */
  public void selectButton(String text) {
    Button button = getButtonByText(text);
    Assert.assertNotNull("Can not find button with text |" + text + "|", button);
    selectButton(button);
  }

  /**
   * Sends {@link SWT#Selection} event to given {@link Button}.
   */
  public void selectButton(String text, boolean selection) {
    Button button = getButtonByText(text);
    Assert.assertNotNull("Can not find button with text |" + text + "|", button);
    button.setSelection(selection);
    clickButton(button);
  }

  public void selectMenuItem(MenuItem menuItem) {
    selectMenuItem(menuItem, true);
  }

  /**
   * Sends {@link SWT#Selection} event to given {@link MenuItem} as radio item.
   */
  public void selectMenuItem(MenuItem menuItem, boolean selection) {
    menuItem.setSelection(selection);
    click(menuItem);
  }

  /**
   * Activates the specified {@link Shell}.
   */
  public void useShell(Shell shell) {
    this.shell = shell;
  }

  /**
   * Specifies that {@link Shell} with given text should be used to search {@link Widget}'s.
   */
  public Shell useShell(String text) {
    // remember current Shell
    shells.addFirst(getShell());
    // set new Shell
    for (Shell sh : display.getShells()) {
      if (text.equals(sh.getText())) {
        shell = sh;
        return shell;
      }
    }
    throw new IllegalArgumentException("Unable to find Shell: " + text);
  }

  /**
   * Sends {@link SWT#Selection} event to given {@link Button}.
   */
  private void selectButton(Button button, boolean selection) {
    button.setSelection(selection);
    clickButton(button);
  }
}
