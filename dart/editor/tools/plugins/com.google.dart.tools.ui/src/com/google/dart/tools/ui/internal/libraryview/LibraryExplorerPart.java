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
package com.google.dart.tools.ui.internal.libraryview;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModel;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.ui.DartElementComparator;
import com.google.dart.tools.ui.DartElementLabels;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.ILibrariesViewPart;
import com.google.dart.tools.ui.internal.actions.CollapseAllAction;
import com.google.dart.tools.ui.internal.preferences.MembersOrderPreferenceCache;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;
import com.google.dart.tools.ui.internal.util.SelectionUtil;
import com.google.dart.tools.ui.internal.util.Strings;
import com.google.dart.tools.ui.internal.viewsupport.AppearanceAwareLabelProvider;
import com.google.dart.tools.ui.internal.viewsupport.DartUILabelProvider;
import com.google.dart.tools.ui.internal.viewsupport.DecoratingDartLabelProvider;
import com.google.dart.tools.ui.internal.viewsupport.FilterUpdater;
import com.google.dart.tools.ui.internal.viewsupport.IViewPartInputProvider;
import com.google.dart.tools.ui.internal.viewsupport.ProblemTreeViewer;
import com.google.dart.tools.ui.internal.viewsupport.StatusBarUpdater;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PerformanceStats;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.OpenAndLinkWithEditorHelper;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.part.ISetSelectionTarget;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The ViewPart for the Library Explorer. It listens to part activation events. When selection
 * linking with the editor is enabled the view selection tracks the active editor page. Similarly
 * when a resource is selected in the packages view the corresponding editor is activated.
 * <p>
 * Note: this class was originally copied from the
 * <code>org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart</code>.
 * 
 * @see LibraryExplorerActionGroup
 */
public class LibraryExplorerPart extends ViewPart implements ISetSelectionTarget, IMenuListener,
    IShowInTarget, ILibrariesViewPart, IPropertyChangeListener, IViewPartInputProvider {

  /**
   * Note: the JDT had the additional field "pendingRefreshes" for handling project top level
   * elements. If the LibraryExplorer ever uses projects as the top level element in the future, we
   * should revisit implementing something similar.
   */
  private class LibraryExplorerProblemTreeViewer extends ProblemTreeViewer {

    public LibraryExplorerProblemTreeViewer(Composite parent, int style) {
      super(parent, style);
    }

    @Override
    protected Object[] addAditionalProblemParents(Object[] elements) {
      return elements;
    }

    @Override
    protected boolean evaluateExpandableWithFilters(Object parent) {
      if (parent instanceof DartLibrary || parent instanceof CompilationUnit) {
        return false;
      }
      return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void handleInvalidSelection(ISelection invalidSelection, ISelection newSelection) {
      IStructuredSelection is = (IStructuredSelection) invalidSelection;
      List<Object> ns = null;
      if (newSelection instanceof IStructuredSelection) {
        ns = new ArrayList<Object>(((IStructuredSelection) newSelection).toList());
      } else {
        ns = new ArrayList<Object>();
      }
      boolean changed = false;
      for (Iterator<?> iter = is.iterator(); iter.hasNext();) {
        Object element = iter.next();
        if (element instanceof DartProject) {
          IProject project = ((DartProject) element).getProject();
          if (!project.isOpen() && project.exists()) {
            ns.add(project);
            changed = true;
          }
        } else if (element instanceof IProject) {
          IProject project = (IProject) element;
          if (project.isOpen()) {
            DartProject dartProject = DartCore.create(project);
            if (dartProject != null && dartProject.exists()) {
              ns.add(dartProject);
            }
            changed = true;
          }
        }
      }
      if (changed) {
        newSelection = new StructuredSelection(ns);
        setSelection(newSelection);
      }
      super.handleInvalidSelection(invalidSelection, newSelection);
    }
  }

  /**
   * When CompilationUnit is expanded in the view, the contents of the type are automatically
   * expanded. If this <code>boolean</code> is set to <code>false</code> the contents are *not*
   * automatically expanded.
   * 
   * @see #expansionListener
   * @see #expandMainType
   */
  private static final boolean AUTO_EXPAND_TYPE = true;

  private static final boolean PERF = false;
  private static final String PERF_CREATE_PART_CONTROL = "com.google.dart.tools.ui/perf/explorer/createPartControl"; //$NON-NLS-1$
  private static final String PERF_MAKE_ACTIONS = "com.google.dart.tools.ui/perf/explorer/makeActions"; //$NON-NLS-1$

  public static final int LIBRARIES_AS_ROOTS = 1;

  public static final int PROJECTS_AS_ROOTS = 2;

  private final static String VIEW_ID = DartUI.ID_LIBRARIES;

  // Persistence tags.
  private static final String TAG_ROOT_MODE = "rootMode"; //$NON-NLS-1$

  private static final String TAG_LINK_EDITOR = "linkWithEditor"; //$NON-NLS-1$

  private static final String TAG_MEMENTO = "memento"; //$NON-NLS-1$

  /**
   * Returns the library explorer part of the active perspective. If there isn't any package
   * explorer part <code>null</code> is returned.
   * 
   * @return the library explorer from the active perspective
   */
  public static LibraryExplorerPart getFromActivePerspective() {
    IWorkbenchPage activePage = DartToolsPlugin.getActivePage();
    if (activePage == null) {
      return null;
    }
    IViewPart view = activePage.findView(VIEW_ID);
    if (view instanceof LibraryExplorerPart) {
      return (LibraryExplorerPart) view;
    }
    return null;
  }

  /**
   * Returns the label of a path.
   * 
   * @param path the path
   * @param isOSPath if <code>true</code>, the path represents an OS path, if <code>false</code> it
   *          is a workspace path.
   * @return the label of the path to be used in the UI.
   */
  public static String getPathLabel(IPath path, boolean isOSPath) {
    String label;
    if (isOSPath) {
      label = path.toOSString();
    } else {
      label = path.makeRelative().toString();
    }
    return Strings.markLTR(label, "/\\:."); //$NON-NLS-1$
  }

  /**
   * Makes the Library Explorer part visible in the active perspective. If there isn't a package
   * explorer part registered <code>null</code> is returned. Otherwise the opened view part is
   * returned.
   * 
   * @return the opened Library Explorer
   */
  public static LibraryExplorerPart openInActivePerspective() {
    try {
      return (LibraryExplorerPart) DartToolsPlugin.getActivePage().showView(VIEW_ID);
    } catch (PartInitException pe) {
      return null;
    }
  }

  private boolean showLibrariesNode;

  private boolean linkingEnabled;

  /**
   * This int is set to one of: {@link #LIBRARIES_AS_ROOTS} or {@link #PROJECTS_AS_ROOTS}.
   * <p>
   * The projects mode is not supported as of yet in this view.
   */
  private int rootMode;
  //private DartElementLabelProvider labelProvider;
  private DecoratingDartLabelProvider decoratingLabelProvider;

  private LibraryExplorerContentProvider contentProvider;
  private FilterUpdater filterUpdater;

  private LibraryExplorerActionGroup actionSet;
  private ProblemTreeViewer viewer;

  private Menu contextMenu;

  private IMemento fMemento;

  /**
   * Helper to open and activate editors.
   */
  private OpenAndLinkWithEditorHelper openAndLinkWithEditorHelper;

  private String workingSetLabel;

  private final IDialogSettings dialogSettings;

  private final IPartListener2 linkWithEditorListener = new IPartListener2() {
    @Override
    public void partActivated(IWorkbenchPartReference partRef) {
      if (partRef instanceof IEditorReference) {
        editorActivated(((IEditorReference) partRef).getEditor(true));
      }
    }

    @Override
    public void partBroughtToTop(IWorkbenchPartReference partRef) {
    }

    @Override
    public void partClosed(IWorkbenchPartReference partRef) {
    }

    @Override
    public void partDeactivated(IWorkbenchPartReference partRef) {
    }

    @Override
    public void partHidden(IWorkbenchPartReference partRef) {
    }

    @Override
    public void partInputChanged(IWorkbenchPartReference partRef) {
      IWorkbenchPage activePage = DartToolsPlugin.getActivePage();
      if (partRef instanceof IEditorReference && activePage != null
          && activePage.getActivePartReference() == partRef) {
        editorActivated(((IEditorReference) partRef).getEditor(true));
      }
    }

    @Override
    public void partOpened(IWorkbenchPartReference partRef) {
    }

    @Override
    public void partVisible(IWorkbenchPartReference partRef) {
    }

  };

  /**
   * <p>
   * This listener is attached to the view in the {@link #createPartControl(Composite)}, and remove
   * on {@link #dispose()}.
   * 
   * @see #AUTO_EXPAND_TYPE
   * @see #expandMainType(Object)
   */
  private final ITreeViewerListener expansionListener = new ITreeViewerListener() {
    @Override
    public void treeCollapsed(TreeExpansionEvent event) {
    }

    @Override
    public void treeExpanded(TreeExpansionEvent event) {
      Object element = event.getElement();
      if (AUTO_EXPAND_TYPE && element instanceof CompilationUnit) {
        expandMainType(element);
      }
    }
  };

  /**
   * The only constructor for this class, it is called implicitly from the Eclipse framework. See
   * the contribution for this view in the <code>plugin.xml</code> file.
   */
  public LibraryExplorerPart() {
    // exception: initialize from preference
    dialogSettings = DartToolsPlugin.getDefault().getDialogSettingsSection(getClass().getName());

    if (dialogSettings.get(TAG_LINK_EDITOR) == null) {
      dialogSettings.put(TAG_LINK_EDITOR, true);
    }

    linkingEnabled = dialogSettings.getBoolean(TAG_LINK_EDITOR);

    try {
      rootMode = dialogSettings.getInt(TAG_ROOT_MODE);
    } catch (NumberFormatException e) {
      rootMode = LIBRARIES_AS_ROOTS;
    }
  }

  /**
   * Called by the {@link CollapseAllAction} action. This action is contributed via the
   * {@link LibraryExplorerActionGroup}.
   */
  public void collapseAll() {
    try {
      viewer.getControl().setRedraw(false);
      viewer.collapseToLevel(getViewPartInput(), AbstractTreeViewer.ALL_LEVELS);
    } finally {
      viewer.getControl().setRedraw(true);
    }
  }

  public ISelection convertSelection(ISelection selection) {
    if (!(selection instanceof IStructuredSelection)) {
      return selection;
    }

    Object[] elements = ((IStructuredSelection) selection).toArray();

    boolean changed = false;
    for (int i = 0; i < elements.length; i++) {
      Object convertedElement = convertElement(elements[i]);
      changed = changed || convertedElement != elements[i];
      elements[i] = convertedElement;
    }
    if (changed) {
      return new StructuredSelection(elements);
    } else {
      return selection;
    }
  }

  @Override
  public void createPartControl(Composite parent) {

    final PerformanceStats stats;
    if (PERF) {
      stats = PerformanceStats.getStats(PERF_CREATE_PART_CONTROL, this);
      stats.startRun();
    }
    viewer = createViewer(parent);
    viewer.setUseHashlookup(true);

    setProviders();

    DartToolsPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);

    MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
    menuMgr.setRemoveAllWhenShown(true);
    menuMgr.addMenuListener(this);
    contextMenu = menuMgr.createContextMenu(viewer.getTree());
    viewer.getTree().setMenu(contextMenu);

    // Register viewer with site. This must be done before making the actions.
    IWorkbenchPartSite site = getSite();
    site.registerContextMenu(menuMgr, viewer);
    site.setSelectionProvider(viewer);

    // Call to create and init the action group (LibraryExplorerActionGroup)
    makeActions(); // call before registering for selection changes

    // Set input after filter and sorter has been set. This avoids resorting and refiltering.
    restoreFilterAndSorter();
    viewer.setInput(findInputElement());

    viewer.addDoubleClickListener(new IDoubleClickListener() {
      @Override
      public void doubleClick(DoubleClickEvent event) {
        actionSet.handleDoubleClick(event);
      }
    });

    openAndLinkWithEditorHelper = new OpenAndLinkWithEditorHelper(viewer) {
      @Override
      protected void activate(ISelection selection) {
        try {
          final Object selectedElement = SelectionUtil.getSingleElement(selection);
          if (EditorUtility.isOpenInEditor(selectedElement) != null) {
            EditorUtility.openInEditor(selectedElement, true);
          }
        } catch (PartInitException ex) {
          // ignore if no editor input can be found
        } catch (DartModelException dme) {
          dme.printStackTrace();
        }
      }

      @Override
      protected void linkToEditor(ISelection selection) {
        LibraryExplorerPart.this.linkToEditor(selection);
      }

      @Override
      protected void open(ISelection selection, boolean activate) {
        actionSet.handleOpen(selection, activate);
      }

    };

    IStatusLineManager slManager = getViewSite().getActionBars().getStatusLineManager();
    viewer.addSelectionChangedListener(new StatusBarUpdater(slManager));
    viewer.addTreeListener(expansionListener);

    fillActionBars();

    updateTitle();

    filterUpdater = new FilterUpdater(viewer);
    ResourcesPlugin.getWorkspace().addResourceChangeListener(filterUpdater);

    // Sync'ing the library explorer has to be done here. It can't be done
    // when restoring the link state since the package explorers input isn't
    // set yet.
    setLinkingEnabled(isLinkingEnabled());

    if (PERF) {
      stats.endRun();
    }
  }

  @Override
  public void dispose() {
    XMLMemento memento = XMLMemento.createWriteRoot("libraryExplorer"); //$NON-NLS-1$
    saveState(memento);
    StringWriter writer = new StringWriter();
    try {
      memento.save(writer);
      dialogSettings.put(TAG_MEMENTO, writer.getBuffer().toString());
    } catch (IOException e) {
      // don't do anything. Simply don't store the settings
    }

    if (contextMenu != null && !contextMenu.isDisposed()) {
      contextMenu.dispose();
    }

    getSite().getPage().removePartListener(linkWithEditorListener); // always remove even if we didn't register

    DartToolsPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(this);
    if (viewer != null) {
      viewer.removeTreeListener(expansionListener);
    }

    if (actionSet != null) {
      actionSet.dispose();
    }
    if (filterUpdater != null) {
      ResourcesPlugin.getWorkspace().removeResourceChangeListener(filterUpdater);
    }

    super.dispose();
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Object getAdapter(Class key) {
    if (key.equals(ISelectionProvider.class)) {
      return viewer;
    }
    if (key == IShowInSource.class) {
      return getShowInSource();
    }
    if (key == IShowInTargetList.class) {
      return new IShowInTargetList() {
        @Override
        public String[] getShowInTargetIds() {
          return new String[] {DartUI.ID_LIBRARIES};
        }

      };
    }
    return super.getAdapter(key);
  }

  /**
   * Returns the root mode: Either {@link #PROJECTS_AS_ROOTS} or {@link #WORKING_SETS_AS_ROOTS}.
   * 
   * @return returns the root mode
   */
  public int getRootMode() {
    return rootMode;
  }

  @Override
  public String getTitleToolTip() {
    if (viewer == null) {
      return super.getTitleToolTip();
    }
    return getToolTipText(viewer.getInput());
  }

  /**
   * Returns the TreeViewer.
   * 
   * @return the tree viewer
   */
  @Override
  public TreeViewer getTreeViewer() {
    return viewer;
  }

  @Override
  public Object getViewPartInput() {
    if (viewer != null) {
      return viewer.getInput();
    }
    return null;
  }

  @Override
  public void init(IViewSite site, IMemento memento) throws PartInitException {
    super.init(site, memento);

    if (memento == null) {
      String persistedMemento = dialogSettings.get(TAG_MEMENTO);
      if (persistedMemento != null) {
        try {
          memento = XMLMemento.createReadRoot(new StringReader(persistedMemento));
        } catch (WorkbenchException e) {
          // Don't do anything. Simply don't restore the settings
        }
      }
    }

    fMemento = memento;

    if (memento != null) {
      restoreLinkingEnabled(memento);
      restoreRootMode(memento);
    }

    IWorkbenchSiteProgressService progressService = (IWorkbenchSiteProgressService) getViewSite().getAdapter(
        IWorkbenchSiteProgressService.class);

    if (progressService != null) {
      initProgressService(progressService);
    }
  }

  @Override
  public boolean isLinkingEnabled() {
    return linkingEnabled;
  }

  @Override
  public void menuAboutToShow(IMenuManager menu) {
    DartToolsPlugin.createStandardGroups(menu);

    actionSet.setContext(new ActionContext(getSelection()));
    actionSet.fillContextMenu(menu);
    actionSet.setContext(null);
  }

  @Override
  public void propertyChange(PropertyChangeEvent event) {
    if (viewer == null) {
      return;
    }

    boolean refreshViewer = false;

//    if (PreferenceConstants.SHOW_CU_CHILDREN.equals(event.getProperty())) {
//      boolean showCUChildren = PreferenceConstants.getPreferenceStore().getBoolean(
//          PreferenceConstants.SHOW_CU_CHILDREN);
//      ((StandardJavaElementContentProvider) fViewer.getContentProvider()).setProvideMembers(showCUChildren);
//
//      refreshViewer = true;
//    } else
    if (MembersOrderPreferenceCache.isMemberOrderProperty(event.getProperty())) {
      refreshViewer = true;
    }

    if (refreshViewer) {
      viewer.refresh();
    }
  }

  public void refresh(IStructuredSelection selection) {
    Object[] selectedElements = selection.toArray();
    for (Object object : selectedElements) {
      viewer.refresh(object);
    }
  }

  @Override
  public void saveState(IMemento memento) {
    if (viewer == null && fMemento != null) {
      // part has not been created -> keep the old state
      memento.putMemento(fMemento);
      return;
    }

    memento.putInteger(TAG_ROOT_MODE, rootMode);

    saveLinkingEnabled(memento);

//    if (fActionSet != null) {
//      fActionSet.saveFilterAndSorterState(memento);
//    }
  }

  @Override
  public void selectAndReveal(Object element) {
    selectReveal(new StructuredSelection(element));
  }

  @Override
  public void selectReveal(final ISelection selection) {
    Control ctrl = getTreeViewer().getControl();
    if (ctrl == null || ctrl.isDisposed()) {
      return;
    }
    contentProvider.runPendingUpdates();
    viewer.setSelection(convertSelection(selection), true);
  }

  @Override
  public void setFocus() {
    viewer.getTree().setFocus();
  }

  @Override
  public void setLinkingEnabled(boolean enabled) {
    linkingEnabled = enabled;
    saveDialogSettings();

    IWorkbenchPage page = getSite().getPage();
    if (enabled) {
      page.addPartListener(linkWithEditorListener);

      IEditorPart editor = page.getActiveEditor();
      if (editor != null) {
        editorActivated(editor);
      }
    } else {
      page.removePartListener(linkWithEditorListener);
    }
    openAndLinkWithEditorHelper.setLinkWithEditor(enabled);
  }

  @Override
  public boolean show(ShowInContext context) {
    ISelection selection = context.getSelection();
    if (selection instanceof IStructuredSelection) {
      IStructuredSelection structuredSelection = ((IStructuredSelection) selection);
      if (structuredSelection.size() == 1) {
        int res = tryToReveal(structuredSelection.getFirstElement());
        if (res == IStatus.OK) {
          return true;
        }
        if (res == IStatus.CANCEL) {
          return false;
        }
      } else if (structuredSelection.size() > 1) {
        selectReveal(structuredSelection);
        return true;
      }
    }

    Object input = context.getInput();
    if (input instanceof IEditorInput) {
      Object elementOfInput = getInputFromEditor((IEditorInput) input);
      return elementOfInput != null && (tryToReveal(elementOfInput) == IStatus.OK);
    }

    return false;
  }

  public int tryToReveal(Object element) {
    if (revealElementOrParent(element)) {
      return IStatus.OK;
    }

    // TODO re-visit this code, can we safely remove?
    // try to remove filters
//    CustomFiltersActionGroup filterGroup = fActionSet.getCustomFilterActionGroup();
//    String[] currentFilters = filterGroup.internalGetEnabledFilterIds();
//    String[] newFilters = filterGroup.removeFiltersFor(getVisibleParent(element), element,
//        getTreeViewer().getContentProvider());
//    if (currentFilters.length > newFilters.length) {
//      String message;
//      if (element instanceof IJavaElement) {
//        String elementLabel = JavaElementLabels.getElementLabel((IJavaElement) element,
//            JavaElementLabels.ALL_DEFAULT);
//        message = Messages.format(PackagesMessages.PackageExplorerPart_removeFiltersSpecific,
//            elementLabel);
//      } else {
//        message = PackagesMessages.PackageExplorer_removeFilters;
//      }
//      if (MessageDialog.openQuestion(getSite().getShell(),
//          PackagesMessages.PackageExplorer_filteredDialog_title, message)) {
//        filterGroup.setFilters(newFilters);
//        if (revealElementOrParent(element)) {
//          return IStatus.OK;
//        }
//      } else {
//        return IStatus.CANCEL;
//      }
//    }
//    FrameAction action = fActionSet.getUpAction();
//    while (action.getFrameList().getCurrentIndex() > 0) {
//      // only try to go up if there is a parent frame
//      // fix for bug# 63769 Endless loop after Show in Package Explorer
//      if (action.getFrameList().getSource().getFrame(IFrameSource.PARENT_FRAME, 0) == null) {
//        break;
//      }
//      action.run();
//      if (revealElementOrParent(element)) {
//        return IStatus.OK;
//      }
//    }
    return IStatus.ERROR;
  }

  /**
   * Returns the <code>IShowInSource</code> for this view.
   * 
   * @return the <code>IShowInSource</code>
   */
  protected IShowInSource getShowInSource() {
    return new IShowInSource() {
      @Override
      public ShowInContext getShowInContext() {
        return new ShowInContext(getTreeViewer().getInput(), getTreeViewer().getSelection());
      }
    };
  }

  protected void initProgressService(IWorkbenchSiteProgressService progressService) {
    progressService.showBusyForFamily(ResourcesPlugin.FAMILY_MANUAL_BUILD);
    progressService.showBusyForFamily(ResourcesPlugin.FAMILY_AUTO_BUILD);
  }

  /**
   * An editor has been activated. Set the selection in this Packages Viewer to be the editor's
   * input, if linking is enabled.
   * 
   * @param editor the activated editor
   */
  void editorActivated(IEditorPart editor) {
    IEditorInput editorInput = editor.getEditorInput();
    if (editorInput == null) {
      return;
    }
    Object input = getInputFromEditor(editorInput);
    if (input == null) {
      return;
    }
    if (!inputIsSelected(editorInput)) {
      showInput(input);
    } else {
      getTreeViewer().getTree().showSelection();
    }
  }

  /**
   * A compilation unit was expanded, expand the main type. This method is called from the
   * {@link LibraryExplorerPart#expansionListener}, if {@link #AUTO_EXPAND_TYPE} is
   * <code>true</code>.
   * 
   * @see #AUTO_EXPAND_TYPE
   * @see #expansionListener
   * @param element the element
   */
  void expandMainType(Object element) {
    try {
      Type type = null;
      if (element instanceof CompilationUnit) {
        CompilationUnit cu = (CompilationUnit) element;
        Type[] types = cu.getTypes();
        if (types.length > 0) {
          type = types[0];
        }
      }
      if (type != null) {
        final Type type2 = type;
        Control ctrl = viewer.getControl();
        if (ctrl != null && !ctrl.isDisposed()) {
          ctrl.getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
              Control ctrl2 = viewer.getControl();
              if (ctrl2 != null && !ctrl2.isDisposed()) {
                viewer.expandToLevel(type2, 1);
              }
            }
          });
        }
      }
    } catch (DartModelException e) {
      // no reveal
    }
  }

  /**
   * Returns the name for the given element. Used as the name for the current frame.
   * 
   * @param element the element
   * @return the name of the frame
   */
  String getFrameName(Object element) {
    if (element instanceof DartElement) {
      return ((DartElement) element).getElementName();
    } else {
      return decoratingLabelProvider.getText(element);
    }
  }

  /**
   * Returns the tool tip text for the given element.
   * 
   * @param element the element
   * @return the tooltip
   */
  String getToolTipText(Object element) {
    String result;
    if (!(element instanceof IResource)) {
      if (element instanceof DartElement) {
        result = DartElementLabels.getTextLabel(element, DartElementLabels.ALL_FULLY_QUALIFIED);
      } else if (element instanceof IWorkingSet) {
        result = ((IWorkingSet) element).getLabel();
      } else {
        result = decoratingLabelProvider.getText(element);
      }
    } else {
      IPath path = ((IResource) element).getFullPath();
      if (path.isRoot()) {
        result = LibraryExplorerMessages.LibraryExplorer_title;
      } else {
        result = getPathLabel(path, false);
      }
    }

    if (rootMode == PROJECTS_AS_ROOTS) {
      if (workingSetLabel == null) {
        return result;
      }
//      if (result.length() == 0) {
//        return Messages.format(PackagesMessages.PackageExplorer_toolTip,
//            new String[] {fWorkingSetLabel});
//      }
//      return Messages.format(PackagesMessages.PackageExplorer_toolTip2, new String[] {
//          result, fWorkingSetLabel});
    } else { // Working set mode. During initialization element and action set can be null.
      return result;
    }
    return "";
  }

  boolean isExpandable(Object element) {
    if (viewer == null) {
      return false;
    }
    return viewer.isExpandable(element);
  }

  boolean isLibrariesNodeShown() {
    return showLibrariesNode;
  }

  void setWorkingSetLabel(String workingSetName) {
    workingSetLabel = workingSetName;
    setTitleToolTip(getTitleToolTip());
  }

  boolean showInput(Object input) {
    Object element = null;

    if (input instanceof IFile) {
      element = DartCore.create((IFile) input);
    }

    if (element == null) {
      element = input;
    }

    if (element != null) {
      ISelection newSelection = new StructuredSelection(element);
      if (viewer.getSelection().equals(newSelection)) {
        viewer.reveal(element);
      } else {
        viewer.setSelection(newSelection, true);

        while (element != null && viewer.getSelection().isEmpty()) {
          // Try to select parent in case element is filtered
          element = getParent(element);
          if (element != null) {
            newSelection = new StructuredSelection(element);
            viewer.setSelection(newSelection, true);
          }
        }
      }
      return true;
    }
    return false;
  }

  /**
   * Updates the title text and title tool tip. Called whenever the input of the viewer changes.
   */
  void updateTitle() {
    Object input = viewer.getInput();
    if (input == null || (input instanceof DartModel)) {
      setContentDescription(""); //$NON-NLS-1$
      setTitleToolTip(""); //$NON-NLS-1$
    } else {
      String inputText = DartElementLabels.getTextLabel(input,
          AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS);
      setContentDescription(inputText);
      setTitleToolTip(getToolTipText(input));
    }
  }

  private Object convertElement(Object original) {
    if (original instanceof DartElement) {
      if (original instanceof CompilationUnit) {
        CompilationUnit cu = (CompilationUnit) original;
        DartProject javaProject = cu.getDartProject();
        if (javaProject != null && javaProject.exists()) {
          // could be a working copy of a .java file that is not on classpath
          IResource resource = cu.getResource();
          if (resource != null) {
            return resource;
          }
        }

      }
      return original;
    } else if (original instanceof IResource) {
      DartElement de = DartCore.create((IResource) original);
      if (de != null && de.exists()) {
        DartProject dartProject = de.getDartProject();
        if (dartProject != null && dartProject.exists()) {
          if (dartProject.equals(de)) {
            return de;
          } else {
            // a working copy of a .java file that is not on classpath
            return original;
          }
        }
      }
    } else if (original instanceof IAdaptable) {
      IAdaptable adaptable = (IAdaptable) original;
      DartElement de = (DartElement) adaptable.getAdapter(DartElement.class);
      if (de != null && de.exists()) {
        return de;
      }

      IResource r = (IResource) adaptable.getAdapter(IResource.class);
      if (r != null) {
        de = DartCore.create(r);
        if (de != null && de.exists()) {
          return de;
        } else {
          return r;
        }
      }
    }
    return original;
  }

  private ProblemTreeViewer createViewer(Composite composite) {
    return new LibraryExplorerProblemTreeViewer(composite, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
  }

  /**
   * Call on the action group to fill in the action bars on this view.
   * <p>
   * This method is only called by the {@link #createPartControl(Composite)} method.
   * 
   * @see LibraryExplorerActionGroup#fillActionBars(IActionBars)
   */
  private void fillActionBars() {
    IActionBars actionBars = getViewSite().getActionBars();
    actionSet.fillActionBars(actionBars);
  }

  private Object findInputElement() {

    Object input = getSite().getPage().getInput();
    if (input instanceof IWorkspace) {
      return DartCore.create(((IWorkspace) input).getRoot());
    } else if (input instanceof IContainer) {
      DartElement element = DartCore.create((IContainer) input);
      if (element != null && element.exists()) {
        return element;
      }
      return input;
    }

    //1GERPRT: ITPJUI:ALL - Packages View is empty when shown in Type Hierarchy Perspective
    // we can't handle the input
    // fall back to show the workspace
    return DartCore.create(DartToolsPlugin.getWorkspace().getRoot());
  }

  private Object getInputFromEditor(IEditorInput editorInput) {
    Object input = DartUI.getEditorInputJavaElement(editorInput);
    if (input instanceof CompilationUnit) {
      //CompilationUnit cu = (CompilationUnit) input;
      //if (!cu.getDartProject().isOnClasspath(cu)) { // test needed for Java files in non-source folders (bug 207839)
      //  input = cu.getResource();
      //}
    }
    if (input == null) {
      input = editorInput.getAdapter(IFile.class);
    }
    if (input == null && editorInput instanceof IStorageEditorInput) {
      try {
        input = ((IStorageEditorInput) editorInput).getStorage();
      } catch (CoreException e) {
        // ignore
      }
    }
    return input;
  }

  /**
   * Returns the element's parent.
   * 
   * @param element the element
   * @return the parent or <code>null</code> if there's no parent
   */
  private Object getParent(Object element) {
    if (element instanceof DartElement) {
      return ((DartElement) element).getParent();
    } else if (element instanceof IResource) {
      return ((IResource) element).getParent();
    }
    return null;
  }

  private ISelection getSelection() {
    return viewer.getSelection();
  }

  private Object getVisibleParent(Object object) {
    // Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=19104
    if (object == null) {
      return null;
    }
    if (!(object instanceof DartElement)) {
      return object;
    }
    DartElement element2 = (DartElement) object;
    switch (element2.getElementType()) {
      case DartElement.IMPORT_CONTAINER:
      case DartElement.TYPE:
      case DartElement.METHOD:
      case DartElement.FIELD:
        // select parent cu/classfile
        element2 = element2.getOpenable();
        break;
      case DartElement.DART_MODEL:
        element2 = null;
        break;
    }
    return element2;
  }

  private boolean inputIsSelected(IEditorInput input) {
    IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
    if (selection.size() != 1) {
      return false;
    }

    IEditorInput selectionAsInput;
    try {
      selectionAsInput = EditorUtility.getEditorInput(selection.getFirstElement());
    } catch (DartModelException dme) {
      dme.printStackTrace();
      return false;
    }
    return input.equals(selectionAsInput);
  }

  /**
   * Links to editor (if option enabled)
   * 
   * @param selection the selection
   */
  private void linkToEditor(ISelection selection) {
    Object obj = SelectionUtil.getSingleElement(selection);
    if (obj != null) {
      IEditorPart part = EditorUtility.isOpenInEditor(obj);
      if (part != null) {
        IWorkbenchPage page = getSite().getPage();
        page.bringToTop(part);
        if (obj instanceof DartElement) {
          EditorUtility.revealInEditor(part, (DartElement) obj);
        }
      }
    }
  }

  private void makeActions() {
    if (PERF) {
      final PerformanceStats stats = PerformanceStats.getStats(PERF_MAKE_ACTIONS, this);
      stats.startRun();
      actionSet = new LibraryExplorerActionGroup(this);
      stats.endRun();
    } else {
      actionSet = new LibraryExplorerActionGroup(this);
    }
  }

  private void restoreFilterAndSorter() {
//    fViewer.addFilter(new OutputFolderFilter());
//    setComparator();
//    if (fMemento != null) {
//      fActionSet.restoreFilterAndSorterState(fMemento);
//    }
  }

  private void restoreLinkingEnabled(IMemento memento) {
    Integer val = memento.getInteger(TAG_LINK_EDITOR);
    linkingEnabled = val != null && val.intValue() != 0;
  }

  private void restoreRootMode(IMemento memento) {
    Integer value = memento.getInteger(TAG_ROOT_MODE);
    rootMode = value == null ? PROJECTS_AS_ROOTS : value.intValue();
    //if (rootMode != PROJECTS_AS_ROOTS && rootMode != WORKING_SETS_AS_ROOTS) {
    rootMode = LIBRARIES_AS_ROOTS;
    //}
  }

  private boolean revealAndVerify(Object element) {
    if (element == null) {
      return false;
    }
    selectReveal(new StructuredSelection(element));
    return !getSite().getSelectionProvider().getSelection().isEmpty();
  }

  private boolean revealElementOrParent(Object element) {
    if (revealAndVerify(element)) {
      return true;
    }
    element = getVisibleParent(element);
    if (element != null) {
      if (revealAndVerify(element)) {
        return true;
      }
      if (element instanceof DartElement) {
        IResource resource = ((DartElement) element).getResource();
        if (resource != null) {
          if (revealAndVerify(resource)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private void saveDialogSettings() {
    dialogSettings.put(TAG_ROOT_MODE, rootMode);
    dialogSettings.put(TAG_LINK_EDITOR, linkingEnabled);
  }

  private void saveLinkingEnabled(IMemento memento) {
    memento.putInteger(TAG_LINK_EDITOR, linkingEnabled ? 1 : 0);
  }

  /**
   * Sets any providers (both label and content) needed for the view.
   * <p>
   * This method is only called by {@link #createPartControl(Composite)}
   */
  private void setProviders() {
    //content provider must be set before the label provider
    contentProvider = new LibraryExplorerContentProvider(false, true);
    viewer.setContentProvider(contentProvider);
    viewer.setComparator(new DartElementComparator());
    //labelProvider = new DartElementLabelProvider();
    decoratingLabelProvider = new DecoratingDartLabelProvider(new DartUILabelProvider());
    viewer.setLabelProvider(decoratingLabelProvider);
    //viewer.setLabelProvider(labelProvider);

  }

}
