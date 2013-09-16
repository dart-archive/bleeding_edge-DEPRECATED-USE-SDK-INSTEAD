/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.quickoutline;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlExtension;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.ui.IContentSelectionProvider;
import org.eclipse.wst.sse.ui.internal.SSEUIPlugin;
import org.eclipse.wst.sse.ui.internal.filter.StringMatcher;
import org.eclipse.wst.sse.ui.quickoutline.AbstractQuickOutlineConfiguration;

/**
 * Popup dialog that contains the filtering input and the outline view of the editor's input.
 * <p>
 * Based on {@link org.eclipse.jdt.internal.ui.text.AbstractInformationControl}
 * </p>
 */
public class QuickOutlinePopupDialog extends PopupDialog implements IInformationControl,
    IInformationControlExtension, IInformationControlExtension2, DisposeListener {

  /** Section used for storing the dialog's size and position settings */
  private static final String DIALOG_SECTION = "org.eclipse.wst.sse.ui.quick_outline"; //$NON-NLS-1$

  /** Text field for entering filter patterns */
  private Text fFilterText;

  /** Tree for presenting the information outline */
  private TreeViewer fTreeViewer;

  /** The model to be outlined */
  private IStructuredModel fModel;

  private ILabelProvider fLabelProvider;
  private ITreeContentProvider fContentProvider;

  private IContentSelectionProvider fSelectionProvider;

  private StringPatternFilter fFilter;

  public QuickOutlinePopupDialog(Shell parent, int shellStyle, IStructuredModel model,
      AbstractQuickOutlineConfiguration configuration) {
    super(parent, shellStyle, true, true, true, true, true, null, null);
    fContentProvider = configuration.getContentProvider();
    fLabelProvider = configuration.getLabelProvider();
    fSelectionProvider = configuration.getContentSelectionProvider();
    fModel = model;
    create();
  }

  protected Control createDialogArea(Composite parent) {
    createTreeViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL);
    addListeners(fTreeViewer.getTree());

    installFilter();
    return fTreeViewer.getControl();
  }

  protected Control createTitleControl(Composite parent) {
    createFilterText(parent);

    return fFilterText;
  }

  protected void createTreeViewer(Composite parent, int style) {
    Tree tree = new Tree(parent, style);
    GridData gd = new GridData(GridData.FILL_BOTH);
    gd.heightHint = tree.getItemHeight() * 12;
    tree.setLayoutData(gd);

    fTreeViewer = new TreeViewer(tree);
    fTreeViewer.setContentProvider(fContentProvider);
    fTreeViewer.setLabelProvider(fLabelProvider);
    fTreeViewer.setAutoExpandLevel(2);
    fTreeViewer.setUseHashlookup(true);
    fTreeViewer.setInput(fModel);
  }

  protected void createFilterText(Composite parent) {
    fFilterText = new Text(parent, SWT.NONE);
    Dialog.applyDialogFont(fFilterText);

    GridData data = new GridData(GridData.FILL_HORIZONTAL);
    data.horizontalAlignment = GridData.FILL;
    data.verticalAlignment = GridData.CENTER;
    fFilterText.setLayoutData(data);
    fFilterText.addKeyListener(new KeyListener() {
      public void keyPressed(KeyEvent e) {
        if (e.keyCode == 0x0D) // return
          gotoSelectedElement();
        if (e.keyCode == SWT.ARROW_DOWN)
          fTreeViewer.getTree().setFocus();
        if (e.keyCode == SWT.ARROW_UP)
          fTreeViewer.getTree().setFocus();
        if (e.character == 0x1B) // ESC
          dispose();
      }

      public void keyReleased(KeyEvent e) {
        // do nothing
      }
    });
  }

  protected void installFilter() {
    fFilter = new StringPatternFilter();
    fTreeViewer.addFilter(fFilter);
    fFilterText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        String text = ((Text) e.widget).getText();
        int length = text.length();
        if (length > 0 && text.charAt(length - 1) != '*') {
          text = text + '*';
        }
        setMatcherString(text, true);
      }
    });
  }

  protected void setMatcherString(String pattern, boolean update) {
    fFilter.updatePattern(pattern);
    if (update)
      stringMatcherUpdated();
  }

  /**
   * The string matcher has been modified. The default implementation refreshes the view and selects
   * the first matched element
   */
  protected void stringMatcherUpdated() {
    // refresh viewer to re-filter
    fTreeViewer.getControl().setRedraw(false);
    fTreeViewer.refresh();
    fTreeViewer.expandAll();
    selectFirstMatch();
    fTreeViewer.getControl().setRedraw(true);
  }

  private void selectFirstMatch() {
    TreeItem item = findItem(fTreeViewer.getTree().getItems(), fFilter.getStringMatcher());
    if (item != null) {
      fTreeViewer.getTree().setSelection(item);
    } else {
      fTreeViewer.setSelection(StructuredSelection.EMPTY);
    }
  }

  private TreeItem findItem(TreeItem[] items, StringMatcher matcher) {
    if (matcher == null)
      return items.length > 0 ? items[0] : null;
    int length = items.length;
    TreeItem result = null;
    for (int i = 0; i < length; i++) {
      if (matcher.match(items[i].getText()))
        return items[i];
      if (items[i].getItemCount() > 0) {
        result = findItem(items[i].getItems(), matcher);
        if (result != null)
          return result;
      }

    }
    return result;
  }

  protected IDialogSettings getDialogSettings() {
    IDialogSettings settings = SSEUIPlugin.getDefault().getDialogSettings().getSection(
        DIALOG_SECTION);
    if (settings == null)
      settings = SSEUIPlugin.getDefault().getDialogSettings().addNewSection(DIALOG_SECTION);

    return settings;
  }

  private void gotoSelectedElement() {
    Object element = getSelectedElement();
    dispose();
    ITextEditor editor = getActiveTextEditor();
    if (editor != null) {
      editor.selectAndReveal(((IndexedRegion) element).getStartOffset(),
          ((IndexedRegion) element).getEndOffset() - ((IndexedRegion) element).getStartOffset());
    }
  }

  private ITextEditor getActiveTextEditor() {
    IWorkbench wb = PlatformUI.getWorkbench();
    ITextEditor editor = null;
    if (wb != null) {
      IWorkbenchWindow ww = wb.getActiveWorkbenchWindow();
      IWorkbenchPage page = ww.getActivePage();
      if (page != null) {
        IEditorPart part = page.getActiveEditor();
        if (part instanceof ITextEditor)
          editor = (ITextEditor) part;
        else
          editor = part != null ? (ITextEditor) part.getAdapter(ITextEditor.class) : null;
      }
    }
    return editor;
  }

  private Object getSelectedElement() {
    if (fTreeViewer == null)
      return null;

    return ((IStructuredSelection) fTreeViewer.getSelection()).getFirstElement();
  }

  private void addListeners(final Tree tree) {
    tree.addKeyListener(new KeyListener() {
      public void keyPressed(KeyEvent e) {
        if (e.character == 0x1B) // ESC
          dispose();
      }

      public void keyReleased(KeyEvent e) {
        // do nothing
      }
    });

    tree.addSelectionListener(new SelectionListener() {
      public void widgetSelected(SelectionEvent e) {
        // do nothing
      }

      public void widgetDefaultSelected(SelectionEvent e) {
        gotoSelectedElement();
      }
    });

    /* Mouse hover */
    tree.addMouseMoveListener(new MouseMoveListener() {
      TreeItem fLastItem = null;

      public void mouseMove(MouseEvent e) {
        if (tree.equals(e.getSource())) {
          Object o = tree.getItem(new Point(e.x, e.y));
          if (o instanceof TreeItem) {
            Rectangle clientArea = tree.getClientArea();
            if (!o.equals(fLastItem)) {
              fLastItem = (TreeItem) o;
              tree.setSelection(new TreeItem[] {fLastItem});
            } else if (e.y - clientArea.y < tree.getItemHeight() / 4) {
              // Scroll up
              Point p = tree.toDisplay(e.x, e.y);
              Item item = fTreeViewer.scrollUp(p.x, p.y);
              if (item instanceof TreeItem) {
                fLastItem = (TreeItem) item;
                tree.setSelection(new TreeItem[] {fLastItem});
              }
            } else if (clientArea.y + clientArea.height - e.y < tree.getItemHeight() / 4) {
              // Scroll down
              Point p = tree.toDisplay(e.x, e.y);
              Item item = fTreeViewer.scrollDown(p.x, p.y);
              if (item instanceof TreeItem) {
                fLastItem = (TreeItem) item;
                tree.setSelection(new TreeItem[] {fLastItem});
              }
            }
          }
        }
      }
    });

    tree.addMouseListener(new MouseAdapter() {
      public void mouseUp(MouseEvent e) {

        if (tree.getSelectionCount() < 1)
          return;

        if (e.button != 1)
          return;

        if (tree.equals(e.getSource())) {
          Object o = tree.getItem(new Point(e.x, e.y));
          TreeItem selection = tree.getSelection()[0];
          if (selection.equals(o))
            gotoSelectedElement();
        }
      }
    });
  }

  public void addDisposeListener(DisposeListener listener) {
    getShell().addDisposeListener(listener);
  }

  public void addFocusListener(FocusListener listener) {
    getShell().addFocusListener(listener);
  }

  public Point computeSizeHint() {
    return getShell().getSize();
  }

  public void dispose() {
    close();
  }

  public boolean isFocusControl() {
    return getShell().getDisplay().getActiveShell() == getShell();
  }

  public void removeDisposeListener(DisposeListener listener) {
    getShell().removeDisposeListener(listener);
  }

  public void removeFocusListener(FocusListener listener) {
    getShell().removeFocusListener(listener);
  }

  public void setBackgroundColor(Color background) {
    applyBackgroundColor(background, getContents());
  }

  public void setFocus() {
    getShell().forceFocus();
    fFilterText.setFocus();
  }

  public void setForegroundColor(Color foreground) {
    applyForegroundColor(foreground, getContents());
  }

  public void setInformation(String information) {
    // nothing to do
  }

  public void setLocation(Point location) {
    /*
     * If the location is persisted, it gets managed by PopupDialog - fine. Otherwise, the location
     * is computed in Window#getInitialLocation, which will center it in the parent shell / main
     * monitor, which is wrong for two reasons: - we want to center over the editor / subject
     * control, not the parent shell - the center is computed via the initalSize, which may be also
     * wrong since the size may have been updated since via min/max sizing of
     * AbstractInformationControlManager. In that case, override the location with the one computed
     * by the manager. Note that the call to constrainShellSize in PopupDialog.open will still
     * ensure that the shell is entirely visible.
     */
    if (!getPersistLocation() || getDialogSettings() == null)
      getShell().setLocation(location);
  }

  public void setSize(int width, int height) {
    getShell().setSize(width, height);
  }

  public void setSizeConstraints(int maxWidth, int maxHeight) {
  }

  public void setVisible(boolean visible) {
    if (visible) {
      open();
    } else {
      saveDialogBounds(getShell());
      getShell().setVisible(false);
    }
  }

  public boolean hasContents() {
    return fTreeViewer != null && fTreeViewer.getInput() != null;
  }

  public void setInput(Object input) {
    if (!(input instanceof ISelection)) {
      fTreeViewer.setSelection(new StructuredSelection(input));
    } else {
      if (fSelectionProvider != null) {
        ISelection selection = fSelectionProvider.getSelection(fTreeViewer, (ISelection) input);
        fTreeViewer.setSelection(selection);
      } else {
        fTreeViewer.setSelection((ISelection) input);
      }
    }
  }

  public void widgetDisposed(DisposeEvent e) {
    fTreeViewer = null;
    fFilterText = null;
    fModel = null;
  }

  protected void fillDialogMenu(IMenuManager dialogMenu) {
    // Add custom actions
    super.fillDialogMenu(dialogMenu);
  }

}
