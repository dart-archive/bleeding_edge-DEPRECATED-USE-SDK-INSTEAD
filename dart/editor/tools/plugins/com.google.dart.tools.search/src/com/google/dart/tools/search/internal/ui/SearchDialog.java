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
package com.google.dart.tools.search.internal.ui;

import com.google.dart.tools.search.internal.ui.util.ExceptionHandler;
import com.google.dart.tools.search.internal.ui.util.ExtendedDialogWindow;
import com.google.dart.tools.search.ui.IReplacePage;
import com.google.dart.tools.search.ui.ISearchPage;
import com.google.dart.tools.search.ui.ISearchPageContainer;
import com.google.dart.tools.search.ui.ISearchPageScoreComputer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.action.LegacyActionTools;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IPageChangeProvider;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.activities.WorkbenchActivityHelper;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.ui.part.MultiPageEditorPart;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class SearchDialog extends ExtendedDialogWindow implements ISearchPageContainer,
    IPageChangeProvider {

  private class TabFolderLayout extends Layout {
    @Override
    protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
      if (wHint != SWT.DEFAULT && hHint != SWT.DEFAULT) {
        return new Point(wHint, hHint);
      }

      int x = 0;
      int y = 0;
      Control[] children = composite.getChildren();
      for (int i = 0; i < children.length; i++) {
        Point size = children[i].computeSize(SWT.DEFAULT, SWT.DEFAULT, flushCache);
        x = Math.max(x, size.x);
        y = Math.max(y, size.y);
      }

      Point minSize = getMinSize();
      x = Math.max(x, minSize.x);
      y = Math.max(y, minSize.y);

      if (wHint != SWT.DEFAULT) {
        x = wHint;
      }
      if (hHint != SWT.DEFAULT) {
        y = hHint;
      }
      return new Point(x, y);
    }

    @Override
    protected void layout(Composite composite, boolean flushCache) {
      Rectangle rect = composite.getClientArea();

      Control[] children = composite.getChildren();
      for (int i = 0; i < children.length; i++) {
        children[i].setBounds(rect);
      }
    }
  }

  private static final int SEARCH_ID = IDialogConstants.CLIENT_ID + 1;
  private static final int REPLACE_ID = SEARCH_ID + 1;
  private static final int CUSTOMIZE_ID = REPLACE_ID + 1;

  public static String evaluateEnclosingProject(IAdaptable adaptable) {
    IProject project = (IProject) adaptable.getAdapter(IProject.class);
    if (project == null) {
      IResource resource = (IResource) adaptable.getAdapter(IResource.class);
      if (resource != null) {
        project = resource.getProject();
      }
    }
    if (project != null && project.isAccessible()) {
      return project.getName();
    }
    return null;
  }

  public static String[] evaluateEnclosingProject(ISelection selection, IEditorPart activeEditor) {
    // always use the editor if active
    if (activeEditor != null) {
      String name = evaluateEnclosingProject(activeEditor.getEditorInput());
      if (name != null) {
        return new String[] {name};
      }
    } else if (selection instanceof IStructuredSelection) {
      HashSet<String> res = new HashSet<String>();
      for (@SuppressWarnings("rawtypes")
      Iterator iter = ((IStructuredSelection) selection).iterator(); iter.hasNext();) {
        Object curr = iter.next();
        if (curr instanceof IWorkingSet) {
          IWorkingSet workingSet = (IWorkingSet) curr;
          if (workingSet.isAggregateWorkingSet() && workingSet.isEmpty()) {
            IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
            for (int i = 0; i < projects.length; i++) {
              IProject proj = projects[i];
              if (proj.isOpen()) {
                res.add(proj.getName());
              }
            }
          } else {
            IAdaptable[] elements = workingSet.getElements();
            for (int i = 0; i < elements.length; i++) {
              String name = evaluateEnclosingProject(elements[i]);
              if (name != null) {
                res.add(name);
              }
            }
          }
        } else if (curr instanceof IAdaptable) {
          String name = evaluateEnclosingProject((IAdaptable) curr);
          if (name != null) {
            res.add(name);
          }
        }
      }
      if (!res.isEmpty()) {
        return res.toArray(new String[res.size()]);
      }
    }
    return new String[0];
  }

  private ISearchPage fCurrentPage;
  private String fInitialPageId;
  private int fCurrentIndex;
  private List<SearchPageDescriptor> fDescriptors;
  private Point fMinSize;
  private ScopePart[] fScopeParts;
  private boolean fLastEnableState;

  private Button fReplaceButton;
  private ListenerList fPageChangeListeners;

  private final IWorkbenchWindow fWorkbenchWindow;

  private final ISelection fCurrentSelection;

  private final String[] fCurrentEnclosingProject;

  public SearchDialog(IWorkbenchWindow window, String pageId) {
    super(window.getShell());
    fWorkbenchWindow = window;
    fCurrentSelection = window.getSelectionService().getSelection();
    fCurrentEnclosingProject = evaluateEnclosingProject(fCurrentSelection, getActiveEditor());

    fDescriptors = filterByActivities(SearchPlugin.getDefault().getEnabledSearchPageDescriptors(
        pageId));
    fInitialPageId = pageId;
    fPageChangeListeners = null;
    setUseEmbeddedProgressMonitorPart(false);
  }

  @Override
  public void addPageChangedListener(IPageChangedListener listener) {
    if (fPageChangeListeners == null) {
      fPageChangeListeners = new ListenerList();
    }
    fPageChangeListeners.add(listener);
  }

  @Override
  public boolean close() {
    for (int i = 0; i < fDescriptors.size(); i++) {
      SearchPageDescriptor desc = fDescriptors.get(i);
      desc.dispose();
    }
    return super.close();
  }

  /*
   * Overrides method from Window
   */
  @Override
  public void create() {
    super.create();
    if (fCurrentPage != null) {
      fCurrentPage.setVisible(true);
    }
  }

  public IEditorPart getActiveEditor() {
    IWorkbenchPage activePage = fWorkbenchWindow.getActivePage();
    if (activePage != null) {
      IEditorPart activeEditor = activePage.getActiveEditor();
      IWorkbenchPart activePart = activePage.getActivePart();
      if (activeEditor == activePart) {
        return activeEditor;
      }
    }
    return null;
  }

  /*
   * @see com.google.dart.tools.search.ui.ISearchPageContainer#getActiveEditorInput()
   */
  @Override
  public IEditorInput getActiveEditorInput() {
    IEditorPart editor = getActiveEditor();
    if (editor == null) {
      return null;
    }

    // Handle multi-page editors
    if (editor instanceof MultiPageEditorPart) {
      Object page = ((MultiPageEditorPart) editor).getSelectedPage();
      if (page instanceof IEditorPart) {
        editor = (IEditorPart) page;
      } else {
        return null;
      }
    }

    return editor.getEditorInput();
  }

  public String[] getEnclosingProjectNames() {
    return fCurrentEnclosingProject;
  }

  //---- Page Handling -------------------------------------------------------

  /*
   * Implements method from ISearchPageContainer
   */
  @Override
  public IRunnableContext getRunnableContext() {
    return this;
  }

  @Override
  public Object getSelectedPage() {
    return fCurrentPage;
  }

  @Override
  public String[] getSelectedProjectNames() {
    if (getSelectedScope() == SELECTED_PROJECTS_SCOPE) {
      return getEnclosingProjectNames();
    }
    return null;
  }

  /*
   * Implements method from ISearchPageContainer
   */
  @Override
  public int getSelectedScope() {
    if (fScopeParts[fCurrentIndex] == null) {
      // safe code - should not happen
      return ISearchPageContainer.WORKSPACE_SCOPE;
    }

    return fScopeParts[fCurrentIndex].getSelectedScope();
  }

  /*
   * Implements method from ISearchPageContainer
   */
  @Override
  public IWorkingSet[] getSelectedWorkingSets() {
    if (fScopeParts[fCurrentIndex] == null) {
      // safe code - should not happen
      return null;
    }

    return fScopeParts[fCurrentIndex].getSelectedWorkingSets();
  }

  @Override
  public ISelection getSelection() {
    return fCurrentSelection;
  }

  public IWorkbenchWindow getWorkbenchWindow() {
    return fWorkbenchWindow;
  }

  /*
   * Implements method from ISearchPageContainer
   */
  @Override
  public boolean hasValidScope() {
    return getSelectedScope() != WORKING_SET_SCOPE || getSelectedWorkingSets() != null;
  }

  /**
   * Notify that the scope selection has changed
   * <p>
   * Note: This is a special method to be called only from the ScopePart
   * </p>
   */
  public void notifyScopeSelectionChanged() {
    setPerformActionEnabled(fLastEnableState);
  }

  @Override
  public void removePageChangedListener(IPageChangedListener listener) {
    fPageChangeListeners.remove(listener);
  }

  /*
   * @see com.google.dart.tools.search.ui.ISearchPageContainer#allowsActiveEditorAsScope(boolean)
   */
  @Override
  public void setActiveEditorCanProvideScopeSelection(boolean state) {
    if (fScopeParts[fCurrentIndex] != null) {
      fScopeParts[fCurrentIndex].setActiveEditorCanProvideScopeSelection(state);
    }
  }

  /*
   * Overrides method from ExtendedDialogWindow
   */
  @Override
  public void setPerformActionEnabled(boolean state) {
    fLastEnableState = state;
    super.setPerformActionEnabled(state && hasValidScope());
  }

  /*
   * Implements method from ISearchPageContainer
   */
  @Override
  public void setSelectedScope(int scope) {
    if (fScopeParts[fCurrentIndex] != null) {
      fScopeParts[fCurrentIndex].setSelectedScope(scope);
    }
  }

  /*
   * Implements method from ISearchPageContainer
   */
  @Override
  public void setSelectedWorkingSets(IWorkingSet[] workingSets) {
    if (fScopeParts[fCurrentIndex] != null) {
      fScopeParts[fCurrentIndex].setSelectedWorkingSets(workingSets);
    }
  }

  @Override
  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setText(SearchMessages.SearchDialog_title);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, ISearchHelpContextIds.SEARCH_DIALOG);
  }

  @Override
  protected Control createButtonBar(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 0; // create
    layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
    layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
    layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
    layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);

    composite.setLayout(layout);
    composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    // create help control if needed
    if (isHelpAvailable()) {
      createHelpControl(composite);
    }

    Label filler = new Label(composite, SWT.NONE);
    filler.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
    layout.numColumns++;

    fReplaceButton = createActionButton(
        composite,
        REPLACE_ID,
        SearchMessages.SearchDialog_replaceAction,
        true);
    fReplaceButton.setVisible(fCurrentPage instanceof IReplacePage);

    Button searchButton = createActionButton(
        composite,
        SEARCH_ID,
        SearchMessages.SearchDialog_searchAction,
        true);
    searchButton.setEnabled(fDescriptors.size() > 0);
    super.createButtonsForButtonBar(composite); // cancel button

    return composite;
  }

  @Override
  protected Control createPageArea(Composite parent) {
    int numPages = fDescriptors.size();
    fScopeParts = new ScopePart[numPages];

    if (numPages == 0) {
      Label label = new Label(parent, SWT.CENTER | SWT.WRAP);
      label.setText(SearchMessages.SearchDialog_noSearchExtension);
      return label;
    }

    fCurrentIndex = getPreferredPageIndex();
    final SearchPageDescriptor currentDesc = getDescriptorAt(fCurrentIndex);

    Composite composite = new Composite(parent, SWT.NONE);
    composite.setFont(parent.getFont());

    GridLayout layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    composite.setLayout(layout);
    composite.setLayoutData(new GridData(GridData.FILL_BOTH));

    TabFolder folder = new TabFolder(composite, SWT.NONE);
    folder.setLayout(new TabFolderLayout());
    folder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    folder.setFont(composite.getFont());

    for (int i = 0; i < numPages; i++) {
      SearchPageDescriptor descriptor = getDescriptorAt(i);
      if (WorkbenchActivityHelper.filterItem(descriptor)) {
        continue;
      }

      final TabItem item = new TabItem(folder, SWT.NONE);
      item.setData("descriptor", descriptor); //$NON-NLS-1$
      item.setText(descriptor.getLabel());
      item.addDisposeListener(new DisposeListener() {
        @Override
        public void widgetDisposed(DisposeEvent e) {
          item.setData("descriptor", null); //$NON-NLS-1$
          if (item.getImage() != null) {
            item.getImage().dispose();
          }
        }
      });
      ImageDescriptor imageDesc = descriptor.getImage();
      if (imageDesc != null) {
        item.setImage(imageDesc.createImage());
      }

      if (i == fCurrentIndex) {
        Control pageControl = createPageControl(folder, descriptor);
        pageControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        item.setControl(pageControl);
        fCurrentPage = currentDesc.getPage();
      }
    }

    folder.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        turnToPage(event);
      }
    });

    folder.setSelection(fCurrentIndex);

    return composite;
  }

  @Override
  protected IDialogSettings getDialogBoundsSettings() {
    return SearchPlugin.getDefault().getDialogSettingsSection("DialogBounds_SearchDialog"); //$NON-NLS-1$
  }

  @Override
  protected Point getInitialSize() {
    Point requiredSize = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
    Point lastSize = super.getInitialSize();
    if (requiredSize.x > lastSize.x || requiredSize.y > lastSize.y) {
      return requiredSize;
    }
    return lastSize;
  }

  @Override
  protected boolean performAction(int actionID) {
    switch (actionID) {
      case CUSTOMIZE_ID:
        handleCustomizePressed();
        return false;
      case CANCEL:
        return true;
      case SEARCH_ID:
        if (fCurrentPage != null) {
          return fCurrentPage.performAction();
        }
        return true;
      case REPLACE_ID:
        boolean isAutoBuilding = SearchPlugin.setAutoBuilding(false);
        try {
          // safe cast, replace button is only visible when the current page is
          // a replace page.
          return ((IReplacePage) fCurrentPage).performReplace();
        } finally {
          SearchPlugin.setAutoBuilding(isAutoBuilding);
        }
      default:
        return false;
    }
  }

  private Control createPageControl(Composite parent, final SearchPageDescriptor descriptor) {

    // Page wrapper
    final Composite pageWrapper = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    pageWrapper.setLayout(layout);

    applyDialogFont(pageWrapper);

    BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
      @Override
      public void run() {
        SafeRunner.run(new ISafeRunnable() {
          @Override
          public void handleException(Throwable ex) {
            if (ex instanceof CoreException) {
              ExceptionHandler.handle(
                  (CoreException) ex,
                  getShell(),
                  SearchMessages.Search_Error_createSearchPage_title,
                  Messages.format(
                      SearchMessages.Search_Error_createSearchPage_message,
                      descriptor.getLabel()));
            } else {
              ExceptionHandler.displayMessageDialog(
                  ex,
                  getShell(),
                  SearchMessages.Search_Error_createSearchPage_title,
                  Messages.format(
                      SearchMessages.Search_Error_createSearchPage_message,
                      descriptor.getLabel()));
            }
          }

          @Override
          public void run() throws Exception {
            // create page and control
            ISearchPage page = descriptor.createObject(SearchDialog.this);
            if (page != null) {
              page.createControl(pageWrapper);
            }
          }
        });
      }
    });

    ISearchPage page = descriptor.getPage();
    if (page == null || page.getControl() == null) {
      Composite container = new Composite(parent, SWT.NONE);
      Label label = new Label(container, SWT.WRAP);
      label.setText(Messages.format(
          SearchMessages.SearchDialog_error_pageCreationFailed,
          descriptor.getLabel()));
      container.setLayout(new GridLayout());
      label.setLayoutData(new GridData());
      return container;
    }

    page.getControl().setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    // Search scope
    boolean showScope = descriptor.showScopeSection();
    if (showScope) {
      Composite c = new Composite(pageWrapper, SWT.NONE);
      c.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
      c.setLayout(new GridLayout());

      int index = fDescriptors.indexOf(descriptor);
      fScopeParts[index] = new ScopePart(this, descriptor.canSearchInProjects());
      Control part = fScopeParts[index].createPart(c);
      applyDialogFont(part);
      part.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
      fScopeParts[index].setVisible(true);
    }

    return pageWrapper;
  }

  private void destroyImages(List<Image> images) {
    Iterator<Image> iter = images.iterator();
    while (iter.hasNext()) {
      Image image = iter.next();
      if (image != null && !image.isDisposed()) {
        image.dispose();
      }
    }
  }

  private List<SearchPageDescriptor> filterByActivities(List<SearchPageDescriptor> input) {
    ArrayList<SearchPageDescriptor> filteredList = new ArrayList<SearchPageDescriptor>(input.size());
    for (Iterator<SearchPageDescriptor> descriptors = input.iterator(); descriptors.hasNext();) {
      SearchPageDescriptor descriptor = descriptors.next();
      if (!WorkbenchActivityHelper.filterItem(descriptor)) {
        filteredList.add(descriptor);
      }

    }
    return filteredList;
  }

  private SearchPageDescriptor getDescriptorAt(int index) {
    return fDescriptors.get(index);
  }

  private Point getMinSize() {
    if (fMinSize != null) {
      return fMinSize;
    }

    int x = 0;
    int y = 0;
    int length = fDescriptors.size();
    for (int i = 0; i < length; i++) {
      Point size = getDescriptorAt(i).getPreferredSize();
      if (size.x != SWT.DEFAULT) {
        x = Math.max(x, size.x);
      }
      if (size.y != SWT.DEFAULT) {
        y = Math.max(y, size.y);
      }
    }

    fMinSize = new Point(x, y);
    return fMinSize;
  }

  private int getPreferredPageIndex() {
    Object element = null;
    ISelection selection = getSelection();
    if (selection instanceof IStructuredSelection) {
      element = ((IStructuredSelection) selection).getFirstElement();
    }

    if (element == null) {
      element = getActiveEditorInput();
    }

    int result = 0;
    int level = ISearchPageScoreComputer.LOWEST;
    int size = fDescriptors.size();
    for (int i = 0; i < size; i++) {
      SearchPageDescriptor descriptor = fDescriptors.get(i);
      if (fInitialPageId != null && fInitialPageId.equals(descriptor.getId())) {
        return i;
      }

      int newLevel = descriptor.computeScore(element);
      if (newLevel > level) {
        level = newLevel;
        result = i;
      }
    }
    return result;
  }

  private void handleCustomizePressed() {
    List<SearchPageDescriptor> input = SearchPlugin.getDefault().getSearchPageDescriptors();
    input = filterByActivities(input);

    final ArrayList<Image> createdImages = new ArrayList<Image>(input.size());
    ILabelProvider labelProvider = new LabelProvider() {
      @Override
      public Image getImage(Object element) {
        if (element instanceof SearchPageDescriptor) {
          ImageDescriptor imageDesc = ((SearchPageDescriptor) element).getImage();
          if (imageDesc == null) {
            return null;
          }
          Image image = imageDesc.createImage();
          if (image != null) {
            createdImages.add(image);
          }
          return image;
        }
        return null;
      }

      @Override
      public String getText(Object element) {
        if (element instanceof SearchPageDescriptor) {
          return LegacyActionTools.removeMnemonics(((SearchPageDescriptor) element).getLabel());
        }
        return null;
      }
    };

    String message = SearchMessages.SearchPageSelectionDialog_message;

    ListSelectionDialog dialog = new ListSelectionDialog(
        getShell(),
        input,
        new ArrayContentProvider(),
        labelProvider,
        message) {
      @Override
      public void create() {
        super.create();
        final CheckboxTableViewer viewer = getViewer();
        final Button okButton = this.getOkButton();
        viewer.addCheckStateListener(new ICheckStateListener() {
          @Override
          public void checkStateChanged(CheckStateChangedEvent event) {
            okButton.setEnabled(viewer.getCheckedElements().length > 0);
          }
        });
        SelectionListener listener = new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            okButton.setEnabled(viewer.getCheckedElements().length > 0);
          }
        };
        this.getButton(IDialogConstants.SELECT_ALL_ID).addSelectionListener(listener);
        this.getButton(IDialogConstants.DESELECT_ALL_ID).addSelectionListener(listener);
      }
    };
    dialog.setTitle(SearchMessages.SearchPageSelectionDialog_title);
    dialog.setInitialSelections(SearchPlugin.getDefault().getEnabledSearchPageDescriptors(
        fInitialPageId).toArray());
    if (dialog.open() == Window.OK) {
      SearchPageDescriptor.setEnabled(dialog.getResult());
      Display display = getShell().getDisplay();
      close();
      if (display != null && !display.isDisposed()) {
        display.asyncExec(new Runnable() {
          @Override
          public void run() {
            new OpenSearchDialogAction().run();
          }
        });
      }
    }
    destroyImages(createdImages);
  }

  private boolean mustResize(Point currentSize, Point newSize) {
    return currentSize.x < newSize.x || currentSize.y < newSize.y;
  }

  private void notifyPageChanged() {
    if (fPageChangeListeners != null && !fPageChangeListeners.isEmpty()) {
      // Fires the page change event
      final PageChangedEvent event = new PageChangedEvent(this, getSelectedPage());
      Object[] listeners = fPageChangeListeners.getListeners();
      for (int i = 0; i < listeners.length; ++i) {
        final IPageChangedListener l = (IPageChangedListener) listeners[i];
        SafeRunner.run(new SafeRunnable() {
          @Override
          public void run() {
            l.pageChanged(event);
          }
        });
      }
    }
  }

  private void resizeDialogIfNeeded(Point oldSize, Point newSize) {
    if (oldSize == null || newSize == null) {
      return;
    }
    Shell shell = getShell();
    Point shellSize = shell.getSize();
    if (mustResize(oldSize, newSize)) {
      if (newSize.x > oldSize.x) {
        shellSize.x += (newSize.x - oldSize.x);
      }
      if (newSize.y > oldSize.y) {
        shellSize.y += (newSize.y - oldSize.y);
      }
      shell.setSize(shellSize);
      shell.layout(true);
    }
  }

  private void turnToPage(SelectionEvent event) {
    final TabItem item = (TabItem) event.item;
    TabFolder folder = item.getParent();

    SearchPageDescriptor descriptor = (SearchPageDescriptor) item.getData("descriptor"); //$NON-NLS-1$

    if (item.getControl() == null) {
      item.setControl(createPageControl(folder, descriptor));
    }

    Control oldControl = folder.getItem(fCurrentIndex).getControl();
    Point oldSize = oldControl.getSize();
    Control newControl = item.getControl();
    Point newSize = newControl.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
    resizeDialogIfNeeded(oldSize, newSize);

    ISearchPage oldPage = fCurrentPage;
    if (oldPage != null) {
      oldPage.setVisible(false);
    }

    fCurrentPage = descriptor.getPage();
    fCurrentIndex = folder.getSelectionIndex();

    setPerformActionEnabled(fCurrentPage != null);
    if (fCurrentPage != null) {
      fCurrentPage.setVisible(true);
    }
    Control pageControl = fCurrentPage.getControl();
    if (pageControl instanceof Composite) {
      ((Composite) pageControl).layout(false, true);
    }
    fReplaceButton.setVisible(fCurrentPage instanceof IReplacePage);
    notifyPageChanged();
  }
}
