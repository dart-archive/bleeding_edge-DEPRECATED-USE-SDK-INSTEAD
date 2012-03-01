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
package com.google.dart.tools.ui.callhierarchy;

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.TypeMember;
import com.google.dart.tools.core.search.SearchScope;
import com.google.dart.tools.ui.DartElementLabels;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.IContextMenuConstants;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.actions.CCPActionGroup;
import com.google.dart.tools.ui.actions.DartSearchActionGroup;
import com.google.dart.tools.ui.actions.GenerateActionGroup;
import com.google.dart.tools.ui.actions.OpenEditorActionGroup;
import com.google.dart.tools.ui.internal.callhierarchy.CallHierarchy;
import com.google.dart.tools.ui.internal.callhierarchy.CallLocation;
import com.google.dart.tools.ui.internal.callhierarchy.MethodWrapper;
import com.google.dart.tools.ui.internal.callhierarchy.RealCallers;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.CompositeActionGroup;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;
import com.google.dart.tools.ui.internal.util.DartUIHelp;
import com.google.dart.tools.ui.internal.util.SelectionUtil;
import com.google.dart.tools.ui.internal.viewsupport.SelectionProviderMediator;
import com.google.dart.tools.ui.internal.viewsupport.StatusBarUpdater;

import org.eclipse.core.runtime.Assert;
import org.eclipse.help.IContextProvider;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.OpenAndLinkWithEditorHelper;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.texteditor.ITextEditor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * This is the main view for the call hierarchy. It builds a tree of callers/callees and allows the
 * user to double click an entry to go to the selected method.
 */
public class CallHierarchyViewPart extends ViewPart implements ICallHierarchyViewPart,
    ISelectionChangedListener {

  private final class CallHierarchyOpenEditorHelper extends OpenAndLinkWithEditorHelper {
    public CallHierarchyOpenEditorHelper(StructuredViewer viewer) {
      super(viewer);
    }

    @Override
    protected void activate(ISelection selection) {
      final Object selectedElement = SelectionUtil.getSingleElement(selection);
      if (selectedElement != null) {
        CallHierarchyUI.openInEditor(selectedElement, getSite().getShell(), true);
      }
    }

    @Override
    protected void linkToEditor(ISelection selection) {
      // not supported by this part
    }

    @Override
    protected void open(ISelection selection, boolean activate) {
      if (selection instanceof IStructuredSelection) {
        for (Iterator<?> iter = ((IStructuredSelection) selection).iterator(); iter.hasNext();) {
          boolean noError = CallHierarchyUI.openInEditor(iter.next(), getSite().getShell(),
              OpenStrategy.activateOnOpen());
          if (!noError) {
            return;
          }
        }
      }
    }

  }

  private class CallHierarchySelectionProvider extends SelectionProviderMediator {

    public CallHierarchySelectionProvider(StructuredViewer[] viewers) {
      super(viewers, null);
    }

    @Override
    public ISelection getSelection() {
      ISelection selection = super.getSelection();
      if (!selection.isEmpty()) {
        return CallHierarchyUI.convertSelection(selection);
      }
      return selection;
    }
  }

  static final int VIEW_ORIENTATION_VERTICAL = 0;
  static final int VIEW_ORIENTATION_HORIZONTAL = 1;
  static final int VIEW_ORIENTATION_SINGLE = 2;
  static final int VIEW_ORIENTATION_AUTOMATIC = 3;
  static final int CALL_MODE_CALLERS = 0;
  static final int CALL_MODE_CALLEES = 1;
  static final String GROUP_SEARCH_SCOPE = "MENU_SEARCH_SCOPE"; //$NON-NLS-1$
  static final String ID_CALL_HIERARCHY = "com.google.dart.tools.ui.callhierarchy.view"; //$NON-NLS-1$

  private static final String DIALOGSTORE_VIEWORIENTATION = "CallHierarchyViewPart.orientation"; //$NON-NLS-1$
  private static final String DIALOGSTORE_CALL_MODE = "CallHierarchyViewPart.call_mode"; //$NON-NLS-1$
  private static final String DIALOGSTORE_FIELD_MODE = "CallHierarchyViewPart.field_mode"; //$NON-NLS-1$

  /** The key to be used is <code>DIALOGSTORE_RATIO + fCurrentOrientation</code>. */
  private static final String DIALOGSTORE_RATIO = "CallHierarchyViewPart.ratio"; //$NON-NLS-1$

  private static final String GROUP_FOCUS = "group.focus"; //$NON-NLS-1$
  private static final int PAGE_EMPTY = 0;
  private static final int PAGE_VIEWER = 1;

  static CallHierarchyViewPart findAndShowCallersView(IWorkbenchPartSite site) {
    IWorkbenchPage workbenchPage = site.getPage();
    CallHierarchyViewPart callersView = null;

    try {
      callersView = (CallHierarchyViewPart) workbenchPage.showView(CallHierarchyViewPart.ID_CALL_HIERARCHY);
    } catch (PartInitException e) {
      DartToolsPlugin.log(e);
    }

    return callersView;
  }

  private static String getShortLabel(DartElement member) {
    return DartElementLabels.getElementLabel(member, 0L);
  }

  protected Composite fParent;
  int fOrientation = VIEW_ORIENTATION_AUTOMATIC;

  private Label fNoHierarchyShownLabel;
  private PageBook fPagebook;
  private final IDialogSettings fDialogSettings;
  private int fCurrentOrientation;
  private int fCurrentCallMode;
//  private int fCurrentFieldMode; // TODO change type to MatchKind
  private MethodWrapper[] fCalleeRoots;
  private MethodWrapper[] fCallerRoots;
  private IMemento fMemento;
  private DartElement[] fInputElements;
  private CallHierarchySelectionProvider fSelectionProviderMediator;
  private LocationViewer fLocationViewer;
  private SashForm fHierarchyLocationSplitter;
  private Clipboard fClipboard;
  private SearchScopeActionGroup fSearchScopeActions;
  private ToggleOrientationAction[] fToggleOrientationActions;
  private ToggleCallModeAction[] fToggleCallModeActions;
//  private SelectFieldModeAction[] fToggleFieldModeActions;
  private CallHierarchyFiltersActionGroup fFiltersActionGroup;
  private HistoryDropDownAction fHistoryDropDownAction;
  private RefreshElementAction fRefreshSingleElementAction;
  private RefreshViewAction fRefreshViewAction;
  private OpenLocationAction fOpenLocationAction;
  private LocationCopyAction fLocationCopyAction;
  private FocusOnSelectionAction fFocusOnSelectionAction;
  private CopyCallHierarchyAction fCopyAction;
  private CancelSearchAction fCancelSearchAction;
//  private ExpandWithConstructorsAction fExpandWithConstructorsAction;
  private RemoveFromViewAction fRemoveFromViewAction;
  private ShowSearchInDialogAction fShowSearchInDialogAction;
  private CompositeActionGroup fActionGroups;
  private CallHierarchyViewer fCallHierarchyViewer;
  private boolean fShowCallDetails;
  private IPartListener2 fPartListener;
  private boolean fIsPinned;
  private PinCallHierarchyViewAction fPinViewAction;

  public CallHierarchyViewPart() {
    super();
    fDialogSettings = DartToolsPlugin.getDefault().getDialogSettings();
    fIsPinned = false;
  }

  @Override
  public void createPartControl(Composite parent) {
    fParent = parent;
    addResizeListener(parent);
    fPagebook = new PageBook(parent, SWT.NONE);

    // Page 1: Viewers
    createHierarchyLocationSplitter(fPagebook);
    createCallHierarchyViewer(fHierarchyLocationSplitter);
    createLocationViewer(fHierarchyLocationSplitter);

    // Page 2: Nothing selected
    fNoHierarchyShownLabel = new Label(fPagebook, SWT.TOP + SWT.LEFT + SWT.WRAP);
    fNoHierarchyShownLabel.setText(CallHierarchyMessages.CallHierarchyViewPart_empty); //

    showPage(PAGE_EMPTY);

    PlatformUI.getWorkbench().getHelpSystem().setHelp(fPagebook,
        DartHelpContextIds.CALL_HIERARCHY_VIEW);

    fSelectionProviderMediator = new CallHierarchySelectionProvider(new StructuredViewer[] {
        fCallHierarchyViewer, fLocationViewer});

    IStatusLineManager slManager = getViewSite().getActionBars().getStatusLineManager();
    fSelectionProviderMediator.addSelectionChangedListener(new StatusBarUpdater(slManager));
    getSite().setSelectionProvider(fSelectionProviderMediator);

    fCallHierarchyViewer.initContextMenu(new IMenuListener() {
      @Override
      public void menuAboutToShow(IMenuManager menu) {
        fillCallHierarchyViewerContextMenu(menu);
      }
    }, getSite(), fSelectionProviderMediator);

    fClipboard = new Clipboard(parent.getDisplay());

    makeActions();
    fillViewMenu();
    fillActionBars();
    initDragAndDrop();

    initOrientation();
    initCallMode();
    initFieldMode();

    if (fMemento != null) {
      restoreState(fMemento);
    }
    restoreSplitterRatio();
    addPartListener();
  }

  @Override
  public void dispose() {
    if (fActionGroups != null) {
      fActionGroups.dispose();
    }

    if (fClipboard != null) {
      fClipboard.dispose();
    }

    if (fPartListener != null) {
      getViewSite().getPage().removePartListener(fPartListener);
      fPartListener = null;
    }
    super.dispose();
  }

  @Override
  public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
    if (adapter == IContextProvider.class) {
      return DartUIHelp.getHelpContextProvider(this, DartHelpContextIds.CALL_HIERARCHY_VIEW);
    }
    if (adapter == IShowInSource.class) {
      return new IShowInSource() {
        @Override
        public ShowInContext getShowInContext() {
          return new ShowInContext(null, fSelectionProviderMediator.getSelection());
        }
      };
    }
    if (adapter == IShowInTargetList.class) {
      return new IShowInTargetList() {
        @Override
        public String[] getShowInTargetIds() {
          return new String[] {DartUI.ID_FILE_VIEW, DartUI.ID_LIBRARIES};
        }
      };
    }
    return super.getAdapter(adapter);
  }

  public MethodWrapper[] getCurrentMethodWrappers() {
    if (fCurrentCallMode == CALL_MODE_CALLERS) {
      return fCallerRoots;
    } else {
      return fCalleeRoots;
    }
  }

  /**
   * Gets all history entries.
   * 
   * @return all history entries
   */
  public TypeMember[][] getHistoryEntries() {
    if (getMethodHistory().size() > 0) {
      updateHistoryEntries();
    }
    return getMethodHistory().toArray(new TypeMember[getMethodHistory().size()][]);
  }

  public DartElement[] getInputElements() {
    return fInputElements;
  }

  /**
   * Fetches the search scope with the appropriate include mask.
   * 
   * @param includeMask the include mask
   * @return the search scope with the appropriate include mask
   */
  public SearchScope getSearchScope(int includeMask) {
    return fSearchScopeActions.getSearchScope(includeMask);
  }

  /**
   * Returns the call hierarchy viewer.
   * 
   * @return the call hierarchy viewer
   */
  public CallHierarchyViewer getViewer() {
    return fCallHierarchyViewer;
  }

  /**
   * Goes to the selected entry, without updating the order of history entries.
   * 
   * @param entry the history entry
   */
  public void gotoHistoryEntry(DartElement[] entry) {
    for (Iterator<DartElement[]> iter = getMethodHistory().iterator(); iter.hasNext();) {
      if (Arrays.equals(entry, iter.next())) {
        setInputElements(entry);
        return;
      }
    }
  }

  @Override
  public void init(IViewSite site, IMemento memento) throws PartInitException {
    super.init(site, memento);
    fMemento = memento;
  }

  public void refresh() {
    setCalleeRoots(null);
    setCallerRoots(null);

    updateView();
  }

  @Override
  public void saveState(IMemento memento) {
    if (fPagebook == null) {
      // part has not been created
      if (fMemento != null) { //Keep the old state;
        memento.putMemento(fMemento);
      }

      return;
    }

    fSearchScopeActions.saveState(memento);
  }

  @Override
  public void selectionChanged(SelectionChangedEvent e) {
    if (e.getSelectionProvider() == fCallHierarchyViewer) {
      methodSelectionChanged(e.getSelection());
    }
  }

  @Override
  public void setFocus() {
    fPagebook.setFocus();
  }

  /**
   * Sets the history entries
   * 
   * @param entries the new history entries
   */
  public void setHistoryEntries(TypeMember[][] entries) {
    getMethodHistory().clear();

    for (int i = 0; i < entries.length; i++) {
      getMethodHistory().add(entries[i]);
    }

    updateHistoryEntries();
  }

  public void setInputElements(DartElement[] members) {
    DartElement[] oldMembers = fInputElements;
    fInputElements = members;

    if (members == null || members.length == 0) {
      showPage(PAGE_EMPTY);
      return;
    }

    if (!Arrays.equals(members, oldMembers)) {
      addHistoryEntry(members);
    }

    refresh();
  }

  public void setShowCallDetails(boolean show) {
    fShowCallDetails = show;
    showOrHideCallDetailsView();
  }

  @Override
  public void showBusy(boolean busy) {
    super.showBusy(busy);
    if (!busy) {
      getProgressService().warnOfContentChange();
    }
  }

  protected void fillCallHierarchyViewerContextMenu(IMenuManager menu) {
    DartToolsPlugin.createStandardGroups(menu);

    menu.appendToGroup(IContextMenuConstants.GROUP_SHOW, fRefreshSingleElementAction);
    menu.appendToGroup(IContextMenuConstants.GROUP_SHOW, new Separator(GROUP_FOCUS));

    if (fFocusOnSelectionAction.canActionBeAdded()) {
      menu.appendToGroup(GROUP_FOCUS, fFocusOnSelectionAction);
    }
//    if (fExpandWithConstructorsAction.canActionBeAdded()) {
//      menu.appendToGroup(GROUP_FOCUS, fExpandWithConstructorsAction);
//    }

    if (fRemoveFromViewAction.canActionBeAdded()) {
      menu.appendToGroup(GROUP_FOCUS, fRemoveFromViewAction);
    }

    fActionGroups.setContext(new ActionContext(getSelection()));
    fActionGroups.fillContextMenu(menu);
    fActionGroups.setContext(null);

    if (fCopyAction.canActionBeAdded()) {
      menu.appendToGroup("group.edit" /* ICommonMenuConstants.GROUP_EDIT */, fCopyAction);
    }
  }

  protected void fillLocationViewerContextMenu(IMenuManager menu) {
    DartToolsPlugin.createStandardGroups(menu);

    menu.appendToGroup(IContextMenuConstants.GROUP_SHOW, fOpenLocationAction);
    menu.appendToGroup(IContextMenuConstants.GROUP_SHOW, fRefreshSingleElementAction);
    menu.appendToGroup(IContextMenuConstants.GROUP_REORGANIZE, fLocationCopyAction);
  }

  /**
   * Returns the location viewer.
   * 
   * @return the location viewer
   */
  protected LocationViewer getLocationViewer() {
    return fLocationViewer;
  }

  /**
   * Returns the current selection.
   * 
   * @return selection
   */
  protected ISelection getSelection() {
    StructuredViewer viewerInFocus = fSelectionProviderMediator.getViewerInFocus();
    if (viewerInFocus != null) {
      return viewerInFocus.getSelection();
    }
    return StructuredSelection.EMPTY;
  }

  protected void saveViewSettings() {
    saveSplitterRatio();
    fDialogSettings.put(DIALOGSTORE_VIEWORIENTATION, fOrientation);
  }

  /**
   * Adds the new input elements to the current list.
   * 
   * @param newElements the new input elements to add
   */
  void addInputElements(TypeMember[] newElements) {
    // Caveat: RemoveFromViewAction#run() disposes TreeItems. When we add a previously removed element,
    // we have to consider the real Tree state, not only fInputElements.

    List<DartElement> inputElements = Arrays.asList(fInputElements);
    List<DartElement> treeElements = new ArrayList<DartElement>();
    TreeItem[] treeItems = fCallHierarchyViewer.getTree().getItems();
    for (int i = 0; i < treeItems.length; i++) {
      Object data = treeItems[i].getData();
      if (data instanceof MethodWrapper) {
        treeElements.add(((MethodWrapper) data).getMember());
      }
    }

    List<DartElement> newInput = new ArrayList<DartElement>();
    newInput.addAll(inputElements);
    List<DartElement> addedElements = new ArrayList<DartElement>();

    for (int i = 0; i < newElements.length; i++) {
      TypeMember newElement = newElements[i];
      if (!inputElements.contains(newElement)) {
        newInput.add(newElement);
      }
      if (!treeElements.contains(newElement)) {
        addedElements.add(newElement);
      }
    }
    if (treeElements.size() == 0) {
      updateInputHistoryAndDescription(fInputElements, newElements);
    } else if (newInput.size() > fInputElements.length) {
      updateInputHistoryAndDescription(fInputElements,
          newInput.toArray(new TypeMember[newInput.size()]));
    }
    if (addedElements.size() > 0) {
      updateViewWithAddedElements(addedElements.toArray(new TypeMember[addedElements.size()]));
    }
  }

  /**
   * Cancels the caller/callee search jobs that are currently running.
   */
  void cancelJobs() {
    fCallHierarchyViewer.cancelJobs();
  }

  void computeOrientation() {
    saveSplitterRatio();
    fDialogSettings.put(DIALOGSTORE_VIEWORIENTATION, fOrientation);
    if (fOrientation != VIEW_ORIENTATION_AUTOMATIC) {
      setOrientation(fOrientation);
    } else {
      if (fOrientation == VIEW_ORIENTATION_SINGLE) {
        return;
      }
      Point size = fParent.getSize();
      if (size.x != 0 && size.y != 0) {
        if (size.x > size.y) {
          setOrientation(VIEW_ORIENTATION_HORIZONTAL);
        } else {
          setOrientation(VIEW_ORIENTATION_VERTICAL);
        }
      }
    }
  }

  /**
   * Returns the current call mode.
   * 
   * @return the current call mode: CALL_MODE_CALLERS or CALL_MODE_CALLEES
   */
  int getCallMode() {
    return fCurrentCallMode;
  }

  /**
   * Indicates whether the Call Hierarchy view is pinned.
   * 
   * @return <code>true</code> if the view is pinned, <code>false</code> otherwise
   */
  boolean isPinned() {
    return fIsPinned;
  }

  /**
   * called from ToggleCallModeAction.
   * 
   * @param mode CALL_MODE_CALLERS or CALL_MODE_CALLEES
   */
  void setCallMode(int mode) {
    if (fCurrentCallMode != mode) {
      for (int i = 0; i < fToggleCallModeActions.length; i++) {
        fToggleCallModeActions[i].setChecked(mode == fToggleCallModeActions[i].getMode());
      }

      fCurrentCallMode = mode;
      fDialogSettings.put(DIALOGSTORE_CALL_MODE, mode);

      updateView();
    }
  }

  /**
   * Sets the enablement state of the cancel button.
   * 
   * @param enabled <code>true</code> if cancel should be enabled
   */
  void setCancelEnabled(boolean enabled) {
    fCancelSearchAction.setEnabled(enabled);
  }

  /**
   * called from SelectFieldModeAction.
   * 
   * @param mode IJavaSearchConstants.{REFERENCES,WRITE_ACCESS,READ_ACCESS}
   */
  void setFieldMode(int mode) {
//    if (fCurrentFieldMode != mode) {
//      for (int i = 0; i < fToggleFieldModeActions.length; i++) {
//        fToggleFieldModeActions[i].setChecked(mode == fToggleFieldModeActions[i].getMode());
//      }
//
//      fCurrentFieldMode = mode;
//      fDialogSettings.put(DIALOGSTORE_FIELD_MODE, mode);
//
//      updateView();
//    }
  }

  /**
   * called from ToggleOrientationAction.
   * 
   * @param orientation VIEW_ORIENTATION_HORIZONTAL or VIEW_ORIENTATION_VERTICAL
   */
  void setOrientation(int orientation) {
    if (fCurrentOrientation != orientation) {
      if ((fLocationViewer != null) && !fLocationViewer.getControl().isDisposed()
          && (fHierarchyLocationSplitter != null) && !fHierarchyLocationSplitter.isDisposed()) {
        if (orientation == VIEW_ORIENTATION_SINGLE) {
          setShowCallDetails(false);
        } else {
          if (fCurrentOrientation == VIEW_ORIENTATION_SINGLE) {
            setShowCallDetails(true);
          }

          boolean horizontal = orientation == VIEW_ORIENTATION_HORIZONTAL;
          fHierarchyLocationSplitter.setOrientation(horizontal ? SWT.HORIZONTAL : SWT.VERTICAL);
        }

        fHierarchyLocationSplitter.layout();
      }

      updateCheckedState();

      fCurrentOrientation = orientation;

      restoreSplitterRatio();
    }
  }

  /**
   * Marks the view as pinned.
   * 
   * @param pinned if <code>true</code> the view is marked as pinned
   */
  void setPinned(boolean pinned) {
    fIsPinned = pinned;
  }

  /**
   * Updates the input, history and description for the new input.
   * 
   * @param currentInput the current input
   * @param entry the new input elements
   */
  void updateInputHistoryAndDescription(DartElement[] currentInput, DartElement[] entry) {
    updateHistoryEntries(currentInput, entry);
    fInputElements = entry;
    setContentDescription(getIncludeMask());
  }

  private void addDragAdapters(StructuredViewer viewer) {
//    int ops = DND.DROP_COPY | DND.DROP_LINK;
//
//    Transfer[] transfers = new Transfer[] {
//        LocalSelectionTransfer.getTransfer(), ResourceTransfer.getInstance(),
//        FileTransfer.getInstance()};
//
//    DelegatingDragAdapter dragAdapter = new DelegatingDragAdapter() {
//      @Override
//      public void dragStart(DragSourceEvent event) {
//        IStructuredSelection selection = (IStructuredSelection) fSelectionProviderMediator.getSelection();
//        if (selection.isEmpty()) {
//          event.doit = false;
//          return;
//        }
//        super.dragStart(event);
//      }
//    };
//    dragAdapter.addDragSourceListener(new SelectionTransferDragAdapter(fSelectionProviderMediator));
//    dragAdapter.addDragSourceListener(new EditorInputTransferDragAdapter(fSelectionProviderMediator));
//    dragAdapter.addDragSourceListener(new ResourceTransferDragAdapter(fSelectionProviderMediator));
//    dragAdapter.addDragSourceListener(new FileTransferDragAdapter(fSelectionProviderMediator));
//
//    viewer.addDragSupport(ops, transfers, dragAdapter);
  }

  private void addDropAdapters(StructuredViewer viewer) {
//    Transfer[] transfers = new Transfer[] {
//        LocalSelectionTransfer.getTransfer(), PluginTransfer.getInstance()};
//    int ops = DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK | DND.DROP_DEFAULT;
//
//    DelegatingDropAdapter delegatingDropAdapter = new DelegatingDropAdapter();
//    delegatingDropAdapter.addDropTargetListener(new CallHierarchyTransferDropAdapter(this, viewer));
//    delegatingDropAdapter.addDropTargetListener(new PluginTransferDropAdapter(viewer));
//
//    viewer.addDropSupport(ops, transfers, delegatingDropAdapter);
  }

  /**
   * Adds the entry if new. Inserted at the beginning of the history entries list.
   * 
   * @param entry the entry to add
   */
  private void addHistoryEntry(DartElement[] entry) {
    updateHistoryEntries(entry, entry);
  }

  private void addPartListener() {
    fPartListener = new IPartListener2() {
      /*
       * (non-Javadoc)
       * 
       * @see org.eclipse.ui.IPartListener2#partActivated(org.eclipse.ui.IWorkbenchPartReference)
       */
      @Override
      public void partActivated(IWorkbenchPartReference partRef) {
        if (isThisView(partRef)) {
          CallHierarchyUI.getDefault().callHierarchyViewActivated(CallHierarchyViewPart.this);
        }
      }

      @Override
      public void partBroughtToTop(IWorkbenchPartReference partRef) {
      }

      /*
       * (non-Javadoc)
       * 
       * @see org.eclipse.ui.IPartListener2#partClosed(org.eclipse.ui.IWorkbenchPartReference)
       */
      @Override
      public void partClosed(IWorkbenchPartReference partRef) {
        if (isThisView(partRef)) {
          CallHierarchyUI.getDefault().callHierarchyViewClosed(CallHierarchyViewPart.this);
          saveViewSettings();
        }
      }

      /*
       * (non-Javadoc)
       * 
       * @see org.eclipse.ui.IPartListener2#partDeactivated(org.eclipse.ui.IWorkbenchPartReference)
       */
      @Override
      public void partDeactivated(IWorkbenchPartReference partRef) {
        if (isThisView(partRef)) {
          saveViewSettings();
        }
      }

      @Override
      public void partHidden(IWorkbenchPartReference partRef) {
      }

      @Override
      public void partInputChanged(IWorkbenchPartReference partRef) {
      }

      @Override
      public void partOpened(IWorkbenchPartReference partRef) {
      }

      @Override
      public void partVisible(IWorkbenchPartReference partRef) {
      }
    };
    getViewSite().getPage().addPartListener(fPartListener);
  }

  private void addResizeListener(Composite parent) {
    parent.addControlListener(new ControlListener() {
      @Override
      public void controlMoved(ControlEvent e) {
      }

      @Override
      public void controlResized(ControlEvent e) {
        computeOrientation();
      }
    });
  }

  /**
   * Computes the content description for the call hierarchy computation.
   * 
   * @param includeMask the include mask
   * @return the content description
   */
  private String computeContentDescription(int includeMask) {
    // see also HistoryAction.getElementLabel(TypeMember[])
    String scopeDescription = fSearchScopeActions.getFullDescription(includeMask);

    if (fInputElements.length == 1) {
      DartElement element = fInputElements[0];
      String elementName = DartElementLabels.getElementLabel(element, DartElementLabels.ALL_DEFAULT);
      String[] args = new String[] {elementName, scopeDescription};
      if (fCurrentCallMode == CALL_MODE_CALLERS) {
        switch (element.getElementType()) {
          case DartElement.TYPE:
            return Messages.format(CallHierarchyMessages.CallHierarchyViewPart_callsToConstructors,
                args);
          case DartElement.FIELD:
//            switch (this.fCurrentFieldMode) {
//              case IJavaSearchConstants.READ_ACCESSES:
//                return Messages.format(
//                    CallHierarchyMessages.CallHierarchyViewPart_callsToFieldRead, args);
//              case IJavaSearchConstants.WRITE_ACCESSES:
//                return Messages.format(
//                    CallHierarchyMessages.CallHierarchyViewPart_callsToFieldWrite, args);
//              default: // all references
            return Messages.format(CallHierarchyMessages.CallHierarchyViewPart_callsToField, args);
//            }
          case DartElement.METHOD:
          default:
            return Messages.format(CallHierarchyMessages.CallHierarchyViewPart_callsToMethod, args);
        }
      } else {
        switch (element.getElementType()) {
          case DartElement.TYPE:
            return Messages.format(
                CallHierarchyMessages.CallHierarchyViewPart_callsFromConstructors, args);
          case DartElement.FIELD:
          case DartElement.METHOD:
          default:
            return Messages.format(CallHierarchyMessages.CallHierarchyViewPart_callsFromMethod,
                args);
        }
      }

    } else {
      if (fCurrentCallMode == CALL_MODE_CALLERS) {
        switch (fInputElements.length) {
          case 0:
            Assert.isTrue(false);
            return null;
          case 2:
            return Messages.format(CallHierarchyMessages.CallHierarchyViewPart_callsToMembers_2,
                new String[] {
                    getShortLabel(fInputElements[0]), getShortLabel(fInputElements[1]),
                    scopeDescription});

          default:
            return Messages.format(CallHierarchyMessages.CallHierarchyViewPart_callsToMembers_more,
                new String[] {
                    getShortLabel(fInputElements[0]), getShortLabel(fInputElements[1]),
                    scopeDescription});
        }
      } else {
        switch (fInputElements.length) {
          case 0:
            Assert.isTrue(false);
            return null;
          case 2:
            return Messages.format(CallHierarchyMessages.CallHierarchyViewPart_callsFromMembers_2,
                new String[] {
                    getShortLabel(fInputElements[0]), getShortLabel(fInputElements[1]),
                    scopeDescription});

          default:
            return Messages.format(
                CallHierarchyMessages.CallHierarchyViewPart_callsFromMembers_more, new String[] {
                    getShortLabel(fInputElements[0]), getShortLabel(fInputElements[1]),
                    scopeDescription});
        }
      }
    }
  }

  private void createCallHierarchyViewer(Composite parent) {
    fCallHierarchyViewer = new CallHierarchyViewer(parent, this);

    fCallHierarchyViewer.addSelectionChangedListener(this);
  }

  private void createHierarchyLocationSplitter(Composite parent) {
    fHierarchyLocationSplitter = new SashForm(parent, SWT.NONE);
  }

  private void createLocationViewer(Composite parent) {
    fLocationViewer = new LocationViewer(parent);

    fLocationViewer.initContextMenu(new IMenuListener() {
      @Override
      public void menuAboutToShow(IMenuManager menu) {
        fillLocationViewerContextMenu(menu);
      }
    }, ID_CALL_HIERARCHY, getSite());
  }

  private void fillActionBars() {
    IActionBars actionBars = getActionBars();
    actionBars.setGlobalActionHandler(ActionFactory.REFRESH.getId(), fRefreshSingleElementAction);
    actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(), fRemoveFromViewAction);

    IToolBarManager toolBar = actionBars.getToolBarManager();

    fActionGroups.fillActionBars(actionBars);

    toolBar.add(fRefreshViewAction);
    toolBar.add(fCancelSearchAction);
    for (int i = 0; i < fToggleCallModeActions.length; i++) {
      toolBar.add(fToggleCallModeActions[i]);
    }
    toolBar.add(fHistoryDropDownAction);
    toolBar.add(fPinViewAction);
  }

  private void fillViewMenu() {
    IActionBars actionBars = getViewSite().getActionBars();
    IMenuManager viewMenu = actionBars.getMenuManager();
    viewMenu.add(new Separator());

    for (int i = 0; i < fToggleCallModeActions.length; i++) {
      viewMenu.add(fToggleCallModeActions[i]);
    }

    viewMenu.add(new Separator());

    MenuManager layoutSubMenu = new MenuManager(
        CallHierarchyMessages.CallHierarchyViewPart_layout_menu);
    for (int i = 0; i < fToggleOrientationActions.length; i++) {
      layoutSubMenu.add(fToggleOrientationActions[i]);
    }
    viewMenu.add(layoutSubMenu);

    viewMenu.add(new Separator(IContextMenuConstants.GROUP_SEARCH));

//    MenuManager fieldSubMenu = new MenuManager(
//        CallHierarchyMessages.CallHierarchyViewPart_field_menu);
//    for (int i = 0; i < fToggleFieldModeActions.length; i++) {
//      fieldSubMenu.add(fToggleFieldModeActions[i]);
//    }
//    viewMenu.add(fieldSubMenu);
    viewMenu.add(fShowSearchInDialogAction);
  }

  private IActionBars getActionBars() {
    return getViewSite().getActionBars();
  }

  private MethodWrapper[] getCalleeRoots() {
    if (fCalleeRoots == null) {
      fCalleeRoots = CallHierarchy.getDefault().getCalleeRoots(fInputElements);
    }

    return fCalleeRoots;
  }

  private MethodWrapper[] getCallerRoots() {
//    if (fCallerRoots != null && fCallerRoots.length > 0) {
//      // all caller roots have the same field mode, just check the first:
//      if (fCallerRoots[0].getFieldSearchMode() != fCurrentFieldMode) {
//        fCallerRoots = null; // field mode changed, re-initialize below
//      }
//    }
    if (fCallerRoots == null) {
      fCallerRoots = CallHierarchy.getDefault().getCallerRoots(fInputElements);
//      for (int i = 0; i < fCallerRoots.length; i++) {
//        fCallerRoots[i].setFieldSearchMode(fCurrentFieldMode);
//      }
    }
    return fCallerRoots;
  }

  /**
   * Gets the include mask.
   * 
   * @return the include mask
   */
  private int getIncludeMask() {
    return fShowSearchInDialogAction.getSearchInDialog().getIncludeMask();
  }

  /**
   * Returns the method history.
   * 
   * @return the method history
   */
  private List<DartElement[]> getMethodHistory() {
    return CallHierarchyUI.getDefault().getMethodHistory();
  }

  /**
   * Fetches the progress service for the workbench part site.
   * 
   * @return the progress service for the workbench part site
   */
  private IWorkbenchSiteProgressService getProgressService() {
    IWorkbenchSiteProgressService service = null;
    Object siteService = getSite().getAdapter(IWorkbenchSiteProgressService.class);
    if (siteService != null) {
      service = (IWorkbenchSiteProgressService) siteService;
    }
    return service;
  }

  private void initCallMode() {
    int mode;

    try {
      mode = fDialogSettings.getInt(DIALOGSTORE_CALL_MODE);

      if ((mode < 0) || (mode > 1)) {
        mode = CALL_MODE_CALLERS;
      }
    } catch (NumberFormatException e) {
      mode = CALL_MODE_CALLERS;
    }

    // force the update
    fCurrentCallMode = -1;

    // will fill the main tool bar
    setCallMode(mode);
  }

  private void initDragAndDrop() {
//    addDragAdapters(fCallHierarchyViewer);
//    addDropAdapters(fCallHierarchyViewer);
//    addDropAdapters(fLocationViewer);
//
//    //dnd on empty hierarchy
//    DropTarget dropTarget = new DropTarget(fPagebook, DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK
//        | DND.DROP_DEFAULT);
//    dropTarget.setTransfer(new Transfer[] {LocalSelectionTransfer.getTransfer()});
//    dropTarget.addDropListener(new CallHierarchyTransferDropAdapter(this, fCallHierarchyViewer));
  }

  private void initFieldMode() {
    int mode;

    try {
      mode = fDialogSettings.getInt(DIALOGSTORE_FIELD_MODE);

//      switch (mode) {
//        case IJavaSearchConstants.REFERENCES:
//        case IJavaSearchConstants.READ_ACCESSES:
//        case IJavaSearchConstants.WRITE_ACCESSES:
//          break; // OK
//        default:
//          mode = IJavaSearchConstants.REFERENCES;
//      }
    } catch (NumberFormatException e) {
//      mode = IJavaSearchConstants.REFERENCES;
      mode = 7;
    }

    // force the update
//    fCurrentFieldMode = -1;

    // will fill the main tool bar
    setFieldMode(mode);
  }

  private void initOrientation() {

    try {
      fOrientation = fDialogSettings.getInt(DIALOGSTORE_VIEWORIENTATION);

      if ((fOrientation < 0) || (fOrientation > 3)) {
        fOrientation = VIEW_ORIENTATION_AUTOMATIC;
      }
    } catch (NumberFormatException e) {
      fOrientation = VIEW_ORIENTATION_AUTOMATIC;
    }

    // force the update
    fCurrentOrientation = -1;
    setOrientation(fOrientation);
  }

  /**
   * Tells whether the given part reference references this view.
   * 
   * @param partRef the workbench part reference
   * @return <code>true</code> if the given part reference references this view
   */
  private boolean isThisView(IWorkbenchPartReference partRef) {
    if (!ID_CALL_HIERARCHY.equals(partRef.getId())) {
      return false;
    }
    String partRefSecondaryId = ((IViewReference) partRef).getSecondaryId();
    String thisSecondaryId = getViewSite().getSecondaryId();
    return thisSecondaryId == null && partRefSecondaryId == null || thisSecondaryId != null
        && thisSecondaryId.equals(partRefSecondaryId);
  }

  private void makeActions() {
    fRefreshViewAction = new RefreshViewAction(this);
    fRefreshSingleElementAction = new RefreshElementAction(fCallHierarchyViewer);

    new CallHierarchyOpenEditorHelper(fLocationViewer);
    new CallHierarchyOpenEditorHelper(fCallHierarchyViewer);

    fOpenLocationAction = new OpenLocationAction(this, getSite());
    fLocationCopyAction = fLocationViewer.initCopyAction(getViewSite(), fClipboard);
    fFocusOnSelectionAction = new FocusOnSelectionAction(this);
    fCopyAction = new CopyCallHierarchyAction(this, fClipboard, fCallHierarchyViewer);
    fSearchScopeActions = new SearchScopeActionGroup(this, fDialogSettings);
    fShowSearchInDialogAction = new ShowSearchInDialogAction(this, fCallHierarchyViewer);
    fFiltersActionGroup = new CallHierarchyFiltersActionGroup(this, fCallHierarchyViewer);
    fHistoryDropDownAction = new HistoryDropDownAction(this);
    fHistoryDropDownAction.setEnabled(false);
    fCancelSearchAction = new CancelSearchAction(this);
    setCancelEnabled(false);
//    fExpandWithConstructorsAction = new ExpandWithConstructorsAction(this, fCallHierarchyViewer);
    fRemoveFromViewAction = new RemoveFromViewAction(this, fCallHierarchyViewer);
    fPinViewAction = new PinCallHierarchyViewAction(this);
    fToggleOrientationActions = new ToggleOrientationAction[] {
        new ToggleOrientationAction(this, VIEW_ORIENTATION_VERTICAL),
        new ToggleOrientationAction(this, VIEW_ORIENTATION_HORIZONTAL),
        new ToggleOrientationAction(this, VIEW_ORIENTATION_AUTOMATIC),
        new ToggleOrientationAction(this, VIEW_ORIENTATION_SINGLE)};
    fRemoveFromViewAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_DELETE);
    fToggleCallModeActions = new ToggleCallModeAction[] {
        new ToggleCallModeAction(this, CALL_MODE_CALLERS),
        new ToggleCallModeAction(this, CALL_MODE_CALLEES)};
//    fToggleFieldModeActions = new SelectFieldModeAction[] {
//        new SelectFieldModeAction(this, IJavaSearchConstants.REFERENCES),
//        new SelectFieldModeAction(this, IJavaSearchConstants.READ_ACCESSES),
//        new SelectFieldModeAction(this, IJavaSearchConstants.WRITE_ACCESSES)};
    fActionGroups = new CompositeActionGroup(new ActionGroup[] {
        new OpenEditorActionGroup(this), new CCPActionGroup(this), new GenerateActionGroup(this),
        new DartSearchActionGroup(this), fSearchScopeActions, fFiltersActionGroup});
  }

  private void methodSelectionChanged(ISelection selection) {
    if (selection instanceof IStructuredSelection && ((IStructuredSelection) selection).size() == 1) {
      Object selectedElement = ((IStructuredSelection) selection).getFirstElement();

      if (selectedElement instanceof MethodWrapper) {
        MethodWrapper methodWrapper = (MethodWrapper) selectedElement;

        revealElementInEditor(methodWrapper, fCallHierarchyViewer);
        updateLocationsView(methodWrapper);
      } else {
        updateLocationsView(null);
      }
    } else {
      updateLocationsView(null);
    }
  }

  private void restoreSplitterRatio() {
    String ratio = fDialogSettings.get(DIALOGSTORE_RATIO + fCurrentOrientation);
    if (ratio == null) {
      return;
    }
    int intRatio = Integer.parseInt(ratio);
    fHierarchyLocationSplitter.setWeights(new int[] {intRatio, 1000 - intRatio});
  }

  /**
   * Restores the type hierarchy settings from a memento.
   * 
   * @param memento the memento
   */
  private void restoreState(IMemento memento) {
    fSearchScopeActions.restoreState(memento);
  }

  private void revealElementInEditor(Object elem, Viewer originViewer) {
    // only allow revealing when the type hierarchy is the active pagae
    // no revealing after selection events due to model changes
    if (getSite().getPage().getActivePart() != this) {
      return;
    }

    if (fSelectionProviderMediator.getViewerInFocus() != originViewer) {
      return;
    }

    if (elem instanceof MethodWrapper) {
      CallLocation callLocation = CallHierarchy.getCallLocation(elem);

      if (callLocation != null) {
        IEditorPart editorPart = CallHierarchyUI.isOpenInEditor(callLocation);

        if (editorPart != null) {
          getSite().getPage().bringToTop(editorPart);

          if (editorPart instanceof ITextEditor) {
            ITextEditor editor = (ITextEditor) editorPart;
            editor.selectAndReveal(callLocation.getStart(),
                (callLocation.getEnd() - callLocation.getStart()));
          }
        }
      } else {
        IEditorPart editorPart = CallHierarchyUI.isOpenInEditor(elem);
        getSite().getPage().bringToTop(editorPart);
        EditorUtility.revealInEditor(editorPart, ((MethodWrapper) elem).getMember());
      }
    } else if (elem instanceof DartElement) {
      IEditorPart editorPart = EditorUtility.isOpenInEditor(elem);

      if (editorPart != null) {
        //            getSite().getPage().removePartListener(fPartListener);
        getSite().getPage().bringToTop(editorPart);
        EditorUtility.revealInEditor(editorPart, (DartElement) elem);

        //            getSite().getPage().addPartListener(fPartListener);
      }
    }
  }

  private void saveSplitterRatio() {
    if (fHierarchyLocationSplitter != null && !fHierarchyLocationSplitter.isDisposed()) {
      int[] weigths = fHierarchyLocationSplitter.getWeights();
      int ratio = (weigths[0] * 1000) / (weigths[0] + weigths[1]);
      String key = DIALOGSTORE_RATIO + fCurrentOrientation;
      fDialogSettings.put(key, ratio);
    }
  }

  private void setCalleeRoots(MethodWrapper[] calleeRoots) {
    this.fCalleeRoots = calleeRoots;
  }

  private void setCallerRoots(MethodWrapper[] callerRoots) {
    this.fCallerRoots = callerRoots;
  }

  /**
   * Sets the content description.
   * 
   * @param includeMask the include mask
   */
  private void setContentDescription(int includeMask) {
    setContentDescription(computeContentDescription(includeMask));
  }

  private void showOrHideCallDetailsView() {
    if (fShowCallDetails) {
      fHierarchyLocationSplitter.setMaximizedControl(null);
    } else {
      fHierarchyLocationSplitter.setMaximizedControl(fCallHierarchyViewer.getControl());
    }
  }

  private void showPage(int page) {
    boolean isEmpty = page == PAGE_EMPTY;
    Control control = isEmpty ? (Control) fNoHierarchyShownLabel : fHierarchyLocationSplitter;
    if (isEmpty) {
      setContentDescription(""); //$NON-NLS-1$
      setTitleToolTip(getPartName());
      getViewSite().getActionBars().getStatusLineManager().setMessage(""); //$NON-NLS-1$
      getViewer().clearViewer();
    }
    fPagebook.showPage(control);
    if (fRefreshViewAction != null) {
      fRefreshViewAction.setEnabled(!isEmpty);
    }
    if (fRefreshSingleElementAction != null) {
      fRefreshSingleElementAction.setEnabled(!isEmpty);
    }
  }

  private void updateCheckedState() {
    for (int i = 0; i < fToggleOrientationActions.length; i++) {
      fToggleOrientationActions[i].setChecked(fOrientation == fToggleOrientationActions[i].getOrientation());
    }
  }

  private void updateHistoryEntries() {
    for (int i = getMethodHistory().size() - 1; i >= 0; i--) {
      DartElement[] members = getMethodHistory().get(i);
      for (int j = 0; j < members.length; j++) {
        DartElement member = members[j];
        if (!member.exists()) {
          getMethodHistory().remove(i);
          break;
        }
      }
    }
    fHistoryDropDownAction.setEnabled(!getMethodHistory().isEmpty());
  }

  /**
   * Updates the history with the latest input.
   * 
   * @param currentInput the current input
   * @param entry the new input elements
   */
  private void updateHistoryEntries(DartElement[] currentInput, DartElement[] entry) {
    for (Iterator<DartElement[]> iter = getMethodHistory().iterator(); iter.hasNext();) {
      if (Arrays.equals(currentInput, iter.next())) {
        iter.remove();
      }
    }

    getMethodHistory().add(0, entry);
    fHistoryDropDownAction.setEnabled(true);
  }

  private void updateLocationsView(MethodWrapper methodWrapper) {
    if (methodWrapper != null && methodWrapper.getMethodCall().hasCallLocations()) {
      fLocationViewer.setInput(methodWrapper.getMethodCall().getCallLocations());
    } else {
      fLocationViewer.clearViewer();
    }
  }

  private void updateView() {
    if (fInputElements != null) {
      showPage(PAGE_VIEWER);

      int includeMask = getIncludeMask();
      CallHierarchy.getDefault().setSearchScope(getSearchScope(includeMask));

      // set input to null so that setComparator does not cause a refresh on the old contents:
      fCallHierarchyViewer.setInput(null);
      if (fCurrentCallMode == CALL_MODE_CALLERS) {
        // sort caller hierarchy alphabetically (bug 111423) and make RealCallers the last in 'Expand With Constructors' mode
        fCallHierarchyViewer.setComparator(new ViewerComparator() {
          @Override
          public int category(Object element) {
            return element instanceof RealCallers ? 1 : 0;
          }
        });
        fCallHierarchyViewer.setMethodWrappers(getCallerRoots());
      } else {
        fCallHierarchyViewer.setComparator(null);
        fCallHierarchyViewer.setMethodWrappers(getCalleeRoots());
      }
      setContentDescription(includeMask);
    }
  }

  /**
   * Updates the view with the newly added input elements.
   * 
   * @param newElements the newly added elements
   */
  private void updateViewWithAddedElements(TypeMember[] newElements) {
    setCalleeRoots(null);
    setCallerRoots(null);
    MethodWrapper[] roots;
    if (getCallMode() == CALL_MODE_CALLERS) {
      roots = CallHierarchy.getDefault().getCallerRoots(newElements);
    } else {
      roots = CallHierarchy.getDefault().getCalleeRoots(newElements);
    }
    CallHierarchyViewer hierarchyViewer = getViewer();
    TreeRoot treeRoot = hierarchyViewer.getTreeRoot(roots, true);
    hierarchyViewer.add(treeRoot, roots);
    for (int i = 0; i < roots.length; i++) {
      hierarchyViewer.setExpandedState(roots[i], true);
    }
    hierarchyViewer.setSelection(new StructuredSelection(roots), true);
  }
}
