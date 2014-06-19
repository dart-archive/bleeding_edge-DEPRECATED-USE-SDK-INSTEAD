/*
 * Copyright (c) 2014, the Dart project authors.
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

import com.google.dart.server.ElementKind;
import com.google.dart.server.Outline;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.DartOutlinePage_NEW;
import com.google.dart.tools.ui.internal.text.editor.LightNodeElements;
import com.google.dart.tools.ui.internal.util.GridDataFactory;
import com.google.dart.tools.ui.internal.util.StringMatcher;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
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

/**
 * Show outline in light-weight control.
 */
public class DartOutlineInformationControl_NEW extends PopupDialog implements IInformationControl {
  /**
   * {@link ViewerFilter} that allows only branches with outlines satisfying {@link #stringMatcher}.
   */
  private class NameFilter extends ViewerFilter {
    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
      Outline outline = (Outline) element;
      // no filter
      if (stringMatcher == null) {
        return true;
      }
      // maybe "outline" matches
      String name = outline.getElement().getName();
      if (name != null && stringMatcher.match(name)) {
        return true;
      }
      // ...or has matching children
      return hasMatchingChild(outline);
    }

    private boolean hasMatchingChild(Outline outline) {
      for (Outline child : outline.getChildren()) {
        if (select(viewer, outline, child)) {
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
   * @return the deepest {@link Outline} enclosing given offset, may be <code>null</code>.
   */
  private static Outline findOutlineEnclosingOffset(Outline[] outlines, int offset) {
    for (Outline outline : outlines) {
      if (outline.containsInclusive(offset)) {
        Outline deeperOutline = findOutlineEnclosingOffset(outline.getChildren(), offset);
        if (deeperOutline != null) {
          return deeperOutline;
        }
        return outline;
      }
    }
    return null;
  }

  private final DartEditor editor;
  private Text filterText;
  private OutlineTreeViewer viewer;

  private StringMatcher stringMatcher;

  public DartOutlineInformationControl_NEW(Shell parent, int shellStyle, ITextEditor textEditor) {
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
  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setText("Outline");
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
    viewer.setComparer(DartOutlinePage_NEW.OUTLINE_COMPARER);
    viewer.setContentProvider(new DartOutlinePage_NEW.OutlineContentProvider());
    viewer.setLabelProvider(new DelegatingStyledCellLabelProvider(
        new DartOutlinePage_NEW.OutlineLabelProvider()));
    viewer.setInput(editor.getOutline());
    selectOutlineEnclosingEditorSelection();
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
        gotoSelectedOutline();
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
          gotoSelectedOutline();
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
          gotoSelectedOutline();
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
   * @return the first {@link TreeItem} matching {@link #stringMatcher}.
   */
  private TreeItem findFirstMatchingItem(TreeItem[] items) {
    for (TreeItem item : items) {
      // no filter  - first is good
      if (stringMatcher == null) {
        return item;
      }
      // check "outline" for filter
      {
        String label = item.getText();
        if (stringMatcher.match(label)) {
          return item;
        }
      }
      // not "outline", but may be one of its children
      item = findFirstMatchingItem(item.getItems());
      if (item != null) {
        return item;
      }
    }
    // no matching outlines
    return null;
  }

  /**
   * Opens selected {@link Outline} in the {@link #editor}.
   */
  private void gotoSelectedOutline() {
    if (viewer != null) {
      IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
      Outline outline = (Outline) selection.getFirstElement();
      editor.setSelection_NEW(outline, true);
      dispose();
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
   * Attempts to find {@link Outline} corresponding to the selection in {@link #editor} and select
   * it in {@link #viewer}.
   */
  private void selectOutlineEnclosingEditorSelection() {
    // may be small unit, expand as possible
    LightNodeElements.expandTreeItemsTimeBoxed(viewer, 50L * 1000000L);
    // prepare "outline" to select
    final Outline outline;
    {
      Outline[] outlines = ((Outline) viewer.getInput()).getChildren();
      ITextSelection editorSelection = (ITextSelection) editor.getSelectionProvider().getSelection();
      int editorOffset = editorSelection.getOffset();
      outline = findOutlineEnclosingOffset(outlines, editorOffset);
    }
    // select "outline"
    if (outline != null) {
      // select "outline" to expect its parents
      viewer.setSelection(new StructuredSelection(outline), true);
      // make root of "outline" top item 
      {
        Outline parent = outline.getParent();
        while (parent != null && parent.getElement().getKind() != ElementKind.COMPILATION_UNIT) {
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
      // schedule "outline" selection again for case when parent of "outline" has many children
      Display.getCurrent().asyncExec(new Runnable() {
        @Override
        public void run() {
          viewer.setSelection(new StructuredSelection(outline), true);
        }
      });
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
      LightNodeElements.expandTreeItemsTimeBoxed(viewer, 75L * 1000000L);
      selectFirstMatch();
    } finally {
      viewer.getControl().setRedraw(true);
    }
  }
}
