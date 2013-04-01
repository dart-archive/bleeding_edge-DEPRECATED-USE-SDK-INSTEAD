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
package com.google.dart.tools.ui.internal.text.functions;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.LightNodeElement;
import com.google.dart.tools.ui.internal.text.editor.LightNodeElements;
import com.google.dart.tools.ui.internal.util.GridDataFactory;
import com.google.dart.tools.ui.internal.util.StringMatcher;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.texteditor.ITextEditor;

import java.util.List;

/**
 * Show outline in light-weight control.
 */
public class DartOutlineInformationControl extends PopupDialog implements IInformationControl {
  /**
   * {@link ViewerFilter} that allows only branches with elements satisfying {@link #stringMatcher}.
   */
  private class NameFilter extends ViewerFilter {
    @Override
    public boolean select(Viewer _viewer, Object parentElement, Object _element) {
      LightNodeElement element = (LightNodeElement) _element;
      // no filter
      if (stringMatcher == null) {
        return true;
      }
      // make be "element" matches
      String elementName = LightNodeElements.LABEL_PROVIDER.getText(element);
      if (elementName != null && stringMatcher.match(elementName)) {
        return true;
      }
      // ...or has matching children
      return hasMatchingChild(element);
    }

    private boolean hasMatchingChild(LightNodeElement element) {
      for (LightNodeElement child : element.getChildren()) {
        if (select(viewer, element, child)) {
          return true;
        }
      }
      return false;
    }
  }
  private class OutlineTreeViewer extends TreeViewer {
    private OutlineTreeViewer(Tree tree) {
      super(tree);
      setUseHashlookup(true);
    }

    @Override
    protected Widget internalGetWidgetToSelect(Object elementOrTreePath) {
      if (elementOrTreePath instanceof TreeItem) {
        return (TreeItem) elementOrTreePath;
      }
      return super.internalGetWidgetToSelect(elementOrTreePath);
    }

    TreeItem findItem2(Object o) {
      return (TreeItem) super.findItem(o);
    }
  }

  /**
   * @return the deepest {@link LightNodeElement} enclosing given offset, may be <code>null</code>.
   */
  private static LightNodeElement findElementEnclosingOffset(List<LightNodeElement> elements,
      int offset) {
    for (LightNodeElement element : elements) {
      if (element.contains(offset)) {
        LightNodeElement deeperElement = findElementEnclosingOffset(element.getChildren(), offset);
        if (deeperElement != null) {
          return deeperElement;
        }
        return element;
      }
    }
    return null;
  }

  private final DartEditor editor;
  private Text filterText;
  private OutlineTreeViewer viewer;

  private StringMatcher stringMatcher;

  public DartOutlineInformationControl(Shell parent, int shellStyle, ITextEditor textEditor) {
    super(parent, shellStyle, true, true, true, true, true, "titleText", "infoText");
    editor = textEditor instanceof DartEditor ? (DartEditor) textEditor : null;
    create();
  }

  @Override
  public void addDisposeListener(DisposeListener listener) {
    getShell().addDisposeListener(listener);
  }

  @Override
  public void addFocusListener(FocusListener listener) {
    getShell().addFocusListener(listener);
  }

  @Override
  public Point computeSizeHint() {
    return getShell().getSize();
  }

  @Override
  public void dispose() {
    close();
  }

  @Override
  public boolean isFocusControl() {
    return viewer.getControl().isFocusControl() || filterText.isFocusControl();
  }

  @Override
  public void removeDisposeListener(DisposeListener listener) {
    getShell().removeDisposeListener(listener);
  }

  @Override
  public void removeFocusListener(FocusListener listener) {
    getShell().removeFocusListener(listener);
  }

  @Override
  public void setBackgroundColor(Color background) {
    applyBackgroundColor(background, getContents());
  }

  @Override
  public void setFocus() {
    getShell().forceFocus();
    filterText.setFocus();
  }

  @Override
  public void setForegroundColor(Color foreground) {
    applyForegroundColor(foreground, getContents());
  }

  @Override
  public void setInformation(String information) {
    // ignore
  }

  @Override
  public void setLocation(Point location) {
    if (!getPersistSize() || getDialogSettings() == null) {
      getShell().setLocation(location);
    }
  }

  @Override
  public void setSize(int width, int height) {
    getShell().setSize(width, height);
  }

  @Override
  public void setSizeConstraints(int maxWidth, int maxHeight) {
    // ignore
  }

  @Override
  public void setVisible(boolean visible) {
    if (visible) {
      open();
    } else {
      saveDialogBounds(getShell());
      getShell().setVisible(false);
    }
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    final Tree tree = new Tree(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
    GridDataFactory.create(tree).hintHeightChars(20).grab().fill();
    if (editor == null) {
      return tree;
    }
    // create TreeViewer
    viewer = new OutlineTreeViewer(tree);
    viewer.addFilter(new NameFilter());
    viewer.setContentProvider(LightNodeElements.newTreeContentProvider());
    viewer.setLabelProvider(LightNodeElements.LABEL_PROVIDER);
    viewer.setInput(editor.getParsedUnit());
    selectElementEnclosingEditorSelection();
    // close on ESC
    tree.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.character == 0x1B) {
          dispose();
        }
      }
    });
    // goto on Enter
    tree.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        gotoSelectedElement();
      }
    });
    // select item under mouse
    tree.addMouseMoveListener(new MouseMoveListener() {
      TreeItem lastItem = null;

      @Override
      public void mouseMove(MouseEvent e) {
        if (tree.equals(e.getSource())) {
          Object o = tree.getItem(new Point(e.x, e.y));
          if (o instanceof TreeItem) {
            if (!o.equals(lastItem)) {
              lastItem = (TreeItem) o;
              tree.setSelection(new TreeItem[] {lastItem});
            }
          }
        }
      }
    });
    // goto after click
    tree.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseUp(MouseEvent e) {
        // has selection
        if (tree.getSelectionCount() < 1) {
          return;
        }
        // first (usually left) button
        if (e.button != 1) {
          return;
        }
        // select element under mouse
        Object targetItem = tree.getItem(new Point(e.x, e.y));
        if (targetItem != null) {
          gotoSelectedElement();
        }
      }
    });
    // done
    return tree;
  }

  protected void createHorizontalSeparator(Composite parent) {
    Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL | SWT.LINE_DOT);
    separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
  }

  @Override
  protected Control createTitleControl(Composite parent) {
    filterText = new Text(parent, SWT.NONE);
    GridDataFactory.create(filterText).grabHorizontal().fillHorizontal().alignVerticalMiddle();
    Dialog.applyDialogFont(filterText);
    filterText.setText("");
    // navigation
    filterText.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.keyCode == 0x0D) {
          gotoSelectedElement();
        }
        if (e.keyCode == SWT.ARROW_DOWN) {
          viewer.getTree().setFocus();
        }
        if (e.keyCode == SWT.ARROW_UP) {
          viewer.getTree().setFocus();
        }
        if (e.character == 0x1B) {
          dispose();
        }
      }
    });
    // filter change
    filterText.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        String text = filterText.getText();
        // treat as prefix
        int length = text.length();
        if (length > 0 && text.charAt(length - 1) != '*') {
          text = text + '*';
        }
        // update filter
        setMatcherString(text);
      }
    });
    return filterText;
  }

  @Override
  protected IDialogSettings getDialogSettings() {
    String sectionName = "com.google.dart.tools.ui.functions.QuickOutline";
    IDialogSettings pluginSettings = DartToolsPlugin.getDefault().getDialogSettings();
    IDialogSettings settings = pluginSettings.getSection(sectionName);
    if (settings == null) {
      settings = pluginSettings.addNewSection(sectionName);
    }
    return settings;
  }

  @Override
  protected boolean hasInfoArea() {
    return false;
  }

  /**
   * Expands {@link #viewer} us much as possible while still in the given time budget.
   */
  private void expandTreeItemsTimeBoxed(long nanoBudget) {
    int numIterations = 10;
    int childrenLimit = 10;
    TreeItem[] rootTreeItems = viewer.getTree().getItems();
    for (int i = 0; i < numIterations; i++) {
      if (nanoBudget < 0) {
        break;
      }
      nanoBudget = expandTreeItemsTimeBoxed(rootTreeItems, childrenLimit, nanoBudget);
      childrenLimit *= 2;
    }
  }

  /**
   * Expands given {@link TreeItem}s if they have not too much children and we have time budget.
   */
  private long expandTreeItemsTimeBoxed(TreeItem[] items, int childrenLimit, long nanoBudget) {
    if (nanoBudget < 0) {
      return -1;
    }
    for (TreeItem item : items) {
      Object itemData = item.getData();
      // prepare LightNodeElement
      if (!(itemData instanceof LightNodeElement)) {
        continue;
      }
      LightNodeElement element = (LightNodeElement) itemData;
      // has children, not too many?
      int numChildren = element.children.size();
      if (numChildren == 0 || numChildren > childrenLimit) {
        continue;
      }
      // expand single item
      {
        long startNano = System.nanoTime();
        viewer.setExpandedState(item, true);
        nanoBudget -= System.nanoTime() - startNano;
      }
      // expand children
      nanoBudget = expandTreeItemsTimeBoxed(item.getItems(), childrenLimit, nanoBudget);
      if (nanoBudget < 0) {
        break;
      }
    }
    return nanoBudget;
  }

  /**
   * @return the first {@link TreeItem} matching {@link #stringMatcher}.
   */
  private TreeItem findFirstMatchingItem(TreeItem[] items) {
    for (TreeItem item : items) {
      // no filter  - first is good
      if (stringMatcher == null) {
        return item;
      }
      // check "element" for filter
      {
        String label = item.getText();
        if (stringMatcher.match(label)) {
          return item;
        }
      }
      // not "element", but may be one of its children
      item = findFirstMatchingItem(item.getItems());
      if (item != null) {
        return item;
      }
    }
    // no matching elements
    return null;
  }

  /**
   * Opens selected {@link LightNodeElement} in the {@link #editor}.
   */
  private void gotoSelectedElement() {
    if (viewer != null) {
      IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
      LightNodeElement element = (LightNodeElement) selection.getFirstElement();
      editor.setSelection(element, true);
      dispose();
    }
  }

  /**
   * Attempts to find {@link LightNodeElement} corresponding to the selection in {@link #editor} and
   * select it in {@link #viewer}.
   */
  private void selectElementEnclosingEditorSelection() {
    // may be small unit, expand as possible
    expandTreeItemsTimeBoxed(50L * 1000000L);
    // prepare "element" to select
    final LightNodeElement element;
    {
      List<LightNodeElement> elements = LightNodeElements.getRootElements(viewer);
      ITextSelection editorSelection = (ITextSelection) editor.getSelectionProvider().getSelection();
      int editorOffset = editorSelection.getOffset();
      element = findElementEnclosingOffset(elements, editorOffset);
    }
    // select "element"
    if (element != null) {
      // select "element" to expect its parents
      viewer.setSelection(new StructuredSelection(element), true);
      // make root of "element" top item 
      {
        LightNodeElement parent = element.getParent();
        while (parent != null) {
          if (parent.getParent() == null) {
            TreeItem parentItem = viewer.findItem2(parent);
            if (parentItem != null) {
              viewer.getTree().setTopItem(parentItem);
            }
            break;
          }
          parent = parent.getParent();
        }
      }
      // schedule "element" selection again for case when parent of "element" has many children
      Display.getCurrent().asyncExec(new Runnable() {
        @Override
        public void run() {
          viewer.setSelection(new StructuredSelection(element), true);
        }
      });
    }
  }

  /**
   * Selects the first {@link TreeItem} which matches the {@link #stringMatcher}.
   */
  private void selectFirstMatch() {
    Tree tree = viewer.getTree();
    TreeItem item = findFirstMatchingItem(tree.getItems());
    if (item != null) {
      viewer.setSelection(new StructuredSelection(item), true);
    } else {
      viewer.setSelection(StructuredSelection.EMPTY);
    }
  }

  /**
   * Sets the patterns to filter out {@link #viewer}.
   * <p>
   * The following characters have special meaning: ? => any character * => any string
   */
  private void setMatcherString(String pattern) {
    // update "stringMatcher"
    if (pattern.length() == 0) {
      stringMatcher = null;
    } else {
      boolean ignoreCase = pattern.toLowerCase().equals(pattern);
      stringMatcher = new StringMatcher(pattern, ignoreCase, false);
    }
    // refresh "viewer"
    viewer.getControl().setRedraw(false);
    try {
      viewer.collapseAll();
      viewer.refresh(false);
      expandTreeItemsTimeBoxed(75L * 1000000L);
      selectFirstMatch();
    } finally {
      viewer.getControl().setRedraw(true);
    }
  }
}
