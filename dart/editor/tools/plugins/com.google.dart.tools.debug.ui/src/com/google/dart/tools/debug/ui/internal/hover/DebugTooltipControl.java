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

package com.google.dart.tools.debug.ui.internal.hover;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;
import com.google.dart.tools.debug.ui.internal.objectinspector.ObjectInspectorContentProvider;
import com.google.dart.tools.debug.ui.internal.presentation.DartDebugModelPresentation;
import com.google.dart.tools.debug.ui.internal.view.DebuggerView;

import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.PresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.TreeModelViewer;
import org.eclipse.debug.internal.ui.views.variables.details.DefaultDetailPane;
import org.eclipse.debug.internal.ui.views.variables.details.DetailPaneProxy;
import org.eclipse.debug.internal.ui.views.variables.details.IDetailPaneContainer;
import org.eclipse.debug.ui.AbstractDebugView;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.text.AbstractInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;

/**
 * An IInformationControl implementation for Dart debugger tooltips.
 */
@SuppressWarnings("restriction")
public class DebugTooltipControl extends AbstractInformationControl implements
    IInformationControlExtension2 {

  /**
   * Inner class implementing IDetailPaneContainer methods. Handles changes to detail pane and
   * provides limited access to the detail pane proxy.
   */
  private class DetailPaneContainer implements IDetailPaneContainer {

    public DetailPaneContainer() {

    }

    @Override
    public String getCurrentPaneID() {
      return detailPane.getCurrentPaneID();
    }

    @Override
    public IStructuredSelection getCurrentSelection() {
      return (IStructuredSelection) treeViewer.getSelection();
    }

    @Override
    public Composite getParentComposite() {
      return detailPaneComposite;
    }

    @Override
    public IWorkbenchPartSite getWorkbenchPartSite() {
      return null;
    }

    @Override
    public void paneChanged(String newPaneID) {
      if (newPaneID.equals(DefaultDetailPane.ID)) {
        // Use the standard tooltip colors on a mac.
        if (DartCore.isMac()) {
          detailPane.getCurrentControl().setBackground(
              getShell().getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
        }
      }
    }

    @Override
    public void refreshDetailPaneContents() {
      detailPane.display(getCurrentSelection());
    }
  }

  /**
   * Dialog setting key for height
   */
  private static final String HEIGHT = "HEIGHT";

  /**
   * Dialog setting key for width.
   */
  private static final String WIDTH = "WIDTH";

  /**
   * Dialog setting key for tree sash weight
   */
  private static final String SASH_WEIGHT_TREE = "SashWeightTree";

  /**
   * Dialog setting key for details sash weight
   */
  private static final String SASH_WEIGHT_DETAILS = "SashWeightDetails";

  /**
   * Variable to display.
   */
  private IVariable variable;
  private IPresentationContext presentationContext;
  private TreeViewer treeViewer;
  private SashForm sashForm;
  private Composite detailPaneComposite;

  private DetailPaneProxy detailPane;

  /**
   * Constructs a new control in the given shell.
   * 
   * @param parentShell shell
   * @param resize whether resize is supported
   */
  DebugTooltipControl(Shell parentShell, boolean resizeable) {
    super(parentShell, resizeable);

    create();
  }

  @Override
  public Point computeSizeHint() {
    IDialogSettings settings = getDialogSettings(false);

    if (settings != null) {
      int x = getIntSetting(settings, WIDTH);

      if (x > 0) {
        int y = getIntSetting(settings, HEIGHT);

        if (y > 0) {
          return new Point(x, y);
        }
      }
    }

    return super.computeSizeHint();
  }

  @Override
  public void dispose() {
    persistSettings(getShell());
    presentationContext.dispose();

    super.dispose();
  }

  @Override
  public IInformationControlCreator getInformationPresenterControlCreator() {
    return DebugTooltipControlCreator.newControlCreatorResizeable();
  }

  @Override
  public boolean hasContents() {
    return variable != null;
  }

  @Override
  public void setBackgroundColor(Color background) {
    super.setBackgroundColor(background);

    detailPaneComposite.setBackground(background);
    treeViewer.getTree().setBackground(background);
  }

  @Override
  public void setFocus() {
    super.setFocus();

    treeViewer.getTree().setFocus();
  }

  @Override
  public void setInput(Object input) {
    variable = (IVariable) input;
    treeViewer.setInput(new Object[] {variable});

    TreeSelection selection = new TreeSelection(new TreePath(new Object[] {variable}));
    treeViewer.setSelection(selection);
    detailPane.display(selection);
  }

  @Override
  public void setVisible(boolean visible) {
    if (!visible) {
      persistSettings(getShell());
    }

    super.setVisible(visible);
  }

  @Override
  protected void createContent(Composite parent) {
    sashForm = new SashForm(parent, parent.getStyle());
    sashForm.setOrientation(SWT.VERTICAL);

    // update presentation context
    AbstractDebugView view = getViewToEmulate();
    presentationContext = new PresentationContext(IDebugUIConstants.ID_VARIABLE_VIEW);
    if (view != null) {
      // copy over properties
      IPresentationContext copy = ((TreeModelViewer) view.getViewer()).getPresentationContext();
      String[] properties = copy.getProperties();
      for (int i = 0; i < properties.length; i++) {
        String key = properties[i];
        presentationContext.setProperty(key, copy.getProperty(key));
      }
    }

    final DartDebugModelPresentation presentation = new DartDebugModelPresentation();

    treeViewer = new TreeViewer(sashForm, SWT.SINGLE | SWT.V_SCROLL | SWT.FULL_SELECTION);
    treeViewer.setAutoExpandLevel(2);
    treeViewer.setLabelProvider(new LabelProvider() {
      @Override
      public Image getImage(Object element) {
        return presentation.getImage(element);
      }

      @Override
      public String getText(Object element) {
        return presentation.getVariableText((IVariable) element);
      }
    });
    treeViewer.setContentProvider(new ObjectInspectorContentProvider());

    if (view != null) {
      // copy over filters
      StructuredViewer structuredViewer = (StructuredViewer) view.getViewer();
      if (structuredViewer != null) {
        ViewerFilter[] filters = structuredViewer.getFilters();
        for (int i = 0; i < filters.length; i++) {
          treeViewer.addFilter(filters[i]);
        }
      }
    }

    detailPaneComposite = SWTFactory.createComposite(sashForm, 1, 1, GridData.FILL_BOTH);
    Layout layout = detailPaneComposite.getLayout();
    if (layout instanceof GridLayout) {
      GridLayout gl = (GridLayout) layout;
      gl.marginHeight = 0;
      gl.marginWidth = 0;
    }

    detailPane = new DetailPaneProxy(new DetailPaneContainer());
    // Bring up the default pane so the user doesn't see an empty composite.
    detailPane.display(null);

    treeViewer.getTree().addSelectionListener(new SelectionListener() {
      @Override
      public void widgetDefaultSelected(SelectionEvent e) {

      }

      @Override
      public void widgetSelected(SelectionEvent e) {
        detailPane.display((IStructuredSelection) treeViewer.getSelection());
      }
    });

    initSashWeights();

    // Use the standard tooltip colors on a mac.
    if (DartCore.isMac()) {
      setBackgroundColor(getShell().getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
    }
  }

  /**
   * Initializes the sash form weights from the preference store (using default values if no sash
   * weights were stored previously).
   */
  protected void initSashWeights() {
    IDialogSettings settings = getDialogSettings(false);

    if (settings != null) {
      int tree = getIntSetting(settings, SASH_WEIGHT_TREE);

      if (tree > 0) {
        int details = getIntSetting(settings, SASH_WEIGHT_DETAILS);

        if (details > 0) {
          sashForm.setWeights(new int[] {tree, details});
        }
      }
    }
  }

  /**
   * Returns the dialog settings for this hover or <code>null</code> if none
   * 
   * @param create whether to create the settings
   */
  private IDialogSettings getDialogSettings(boolean create) {
    IDialogSettings settings = DartDebugUIPlugin.getDefault().getDialogSettings();
    IDialogSettings section = settings.getSection(this.getClass().getName());

    if (section == null & create) {
      section = settings.addNewSection(this.getClass().getName());
    }

    return section;
  }

  /**
   * Returns an integer value in the given dialog settings or -1 if none.
   * 
   * @param settings dialog settings
   * @param key key
   * @return value or -1 if not present
   */
  private int getIntSetting(IDialogSettings settings, String key) {
    try {
      return settings.getInt(key);
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  /**
   * Attempts to find an appropriate view to emulate, this will either be the variables view or the
   * expressions view.
   * 
   * @return a view to emulate or <code>null</code>
   */
  private AbstractDebugView getViewToEmulate() {
    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

    AbstractDebugView expressionsView = (AbstractDebugView) page.findView(IDebugUIConstants.ID_EXPRESSION_VIEW);
    if (expressionsView != null && expressionsView.isVisible()) {
      return expressionsView;
    }

    AbstractDebugView variablesView = (AbstractDebugView) page.findView(IDebugUIConstants.ID_VARIABLE_VIEW);
    if (variablesView != null && variablesView.isVisible()) {
      return variablesView;
    }

    AbstractDebugView debuggerView = (AbstractDebugView) page.findView(DebuggerView.ID);
    if (debuggerView != null && debuggerView.isVisible()) {
      return debuggerView;
    }

    if (expressionsView != null) {
      return expressionsView;
    }

    return variablesView;
  }

  /**
   * Persists dialog settings.
   * 
   * @param shell
   */
  private void persistSettings(Shell shell) {
    if (shell != null && !shell.isDisposed()) {
      if (isResizable()) {
        IDialogSettings settings = getDialogSettings(true);
        Point size = shell.getSize();
        settings.put(WIDTH, size.x);
        settings.put(HEIGHT, size.y);
        int[] weights = sashForm.getWeights();
        settings.put(SASH_WEIGHT_TREE, weights[0]);
        settings.put(SASH_WEIGHT_DETAILS, weights[1]);
      }
    }
  }

}
